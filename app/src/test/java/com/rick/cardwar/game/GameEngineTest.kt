package com.rick.cardwar.game

import com.rick.cardwar.game.ai.CpuAi
import com.rick.cardwar.game.model.BoardSlot
import com.rick.cardwar.game.model.GameState
import com.rick.cardwar.game.model.GameStatus
import com.rick.cardwar.game.model.PlacedCard
import com.rick.cardwar.game.model.PlayerId
import com.rick.cardwar.game.model.PlayingCard
import com.rick.cardwar.game.model.Rank
import com.rick.cardwar.game.model.Suit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private fun card(id: Int, rank: Rank, suit: Suit = Suit.Hearts) =
        PlayingCard(id = id, rank = rank, suit = suit)

    private fun playingState(
        p1Card: PlayingCard,
        board: Map<BoardSlot, PlacedCard> = emptyMap(),
        p2Hand: List<PlayingCard> = emptyList(),
        placements: Int = 0,
        p1: Int = 5,
        p2: Int = 5,
    ) = GameState(
        status = GameStatus.Playing,
        board = board,
        player1Hand = listOf(p1Card),
        player2Hand = p2Hand,
        currentPlayer = PlayerId.One,
        selectedCardId = p1Card.id,
        p1Score = p1,
        p2Score = p2,
        placementsThisMatch = placements,
    )

    @Test
    fun startMatchDealsCenterAndHands() {
        val state = GameEngine.startMatch(GameState(), Random(1))
        assertEquals(GameStatus.Playing, state.status)
        assertEquals(PlayerId.One, state.currentPlayer)
        assertEquals(5, state.player1Hand.size)
        assertEquals(5, state.player2Hand.size)
        assertEquals(5, state.p1Score)
        assertEquals(5, state.p2Score)
        assertTrue(BoardSlot.Center in state.board)
        assertEquals(PlayerId.None, state.board.getValue(BoardSlot.Center).owner)
        assertEquals(8, state.emptySlots.size)
    }

    @Test
    fun placingHigherCardCapturesOrthogonalLowerOpponent() {
        val ace = card(1, Rank.Ace)
        val two = card(2, Rank.Two, Suit.Spades)
        val state = playingState(
            p1Card = ace,
            board = mapOf(BoardSlot.Center to PlacedCard(two, PlayerId.Two)),
        )

        val next = GameEngine.place(state, BoardSlot.TopCenter)

        assertEquals(PlayerId.One, next.board.getValue(BoardSlot.TopCenter).owner)
        assertEquals(PlayerId.One, next.board.getValue(BoardSlot.Center).owner)
        assertEquals(6, next.p1Score)
        assertEquals(4, next.p2Score)
        assertEquals(listOf(BoardSlot.Center), next.lastCapturedSlots)
        assertTrue(next.player1Hand.isEmpty())
        assertEquals(PlayerId.Two, next.currentPlayer)
    }

    @Test
    fun capturingUnownedCenterTransfersAPoint() {
        val ace = card(1, Rank.Ace)
        val five = card(2, Rank.Five)
        val state = playingState(
            p1Card = ace,
            board = mapOf(BoardSlot.Center to PlacedCard(five, PlayerId.None)),
        )

        val next = GameEngine.place(state, BoardSlot.CenterLeft)

        assertEquals(PlayerId.One, next.board.getValue(BoardSlot.Center).owner)
        assertEquals(6, next.p1Score)
        assertEquals(4, next.p2Score)
    }

    @Test
    fun equalOrLowerValueDoesNotCapture() {
        val five = card(1, Rank.Five)
        val ace = card(2, Rank.Ace, Suit.Clubs)
        val state = playingState(
            p1Card = five,
            board = mapOf(BoardSlot.Center to PlacedCard(ace, PlayerId.Two)),
        )

        val next = GameEngine.place(state, BoardSlot.BottomCenter)

        assertEquals(PlayerId.Two, next.board.getValue(BoardSlot.Center).owner)
        assertEquals(5, next.p1Score)
        assertEquals(5, next.p2Score)
        assertTrue(next.lastCapturedSlots.isEmpty())
    }

    @Test
    fun diagonalNeighborIsNotCaptured() {
        val ace = card(1, Rank.Ace)
        val two = card(2, Rank.Two, Suit.Diamonds)
        val state = playingState(
            p1Card = ace,
            board = mapOf(BoardSlot.Center to PlacedCard(two, PlayerId.Two)),
        )

        val next = GameEngine.place(state, BoardSlot.TopLeft)

        assertEquals(PlayerId.Two, next.board.getValue(BoardSlot.Center).owner)
        assertEquals(5, next.p1Score)
    }

    @Test
    fun cannotPlaceOnOccupiedSlot() {
        val ace = card(1, Rank.Ace)
        val two = card(2, Rank.Two)
        val state = playingState(
            p1Card = ace,
            board = mapOf(BoardSlot.Center to PlacedCard(two, PlayerId.None)),
        )

        val next = GameEngine.place(state, BoardSlot.Center)

        assertEquals(state, next)
    }

    @Test
    fun selectIgnoresOpponentCards() {
        val p1 = card(1, Rank.Ace)
        val p2 = card(2, Rank.Two, Suit.Clubs)
        val state = playingState(p1Card = p1, p2Hand = listOf(p2))

        val next = GameEngine.selectCard(state, p2.id)

        assertEquals(p1.id, next.selectedCardId)
    }

    @Test
    fun matchEndsAfterEightPlacementsAndHigherScoreWins() {
        val ace = card(1, Rank.Ace)
        val two = card(2, Rank.Two)
        val state = playingState(
            p1Card = ace,
            board = mapOf(BoardSlot.TopLeft to PlacedCard(two, PlayerId.Two)),
            placements = 7,
            p1 = 6,
            p2 = 4,
        )

        val next = GameEngine.place(state, BoardSlot.TopCenter)

        assertEquals(GameStatus.Finished, next.status)
        assertEquals(PlayerId.None, next.currentPlayer)
        assertEquals(1, next.p1GamesWon)
        assertEquals(0, next.p2GamesWon)
        assertEquals(7, next.p1Score)
        assertEquals(3, next.p2Score)
    }

    @Test
    fun tiedMatchAwardsNeitherPlayer() {
        val five = card(1, Rank.Five)
        val ace = card(2, Rank.Ace)
        val state = playingState(
            p1Card = five,
            board = mapOf(BoardSlot.Center to PlacedCard(ace, PlayerId.Two)),
            placements = 7,
        )

        val next = GameEngine.place(state, BoardSlot.TopRight)

        assertEquals(GameStatus.Finished, next.status)
        assertEquals(0, next.p1GamesWon)
        assertEquals(0, next.p2GamesWon)
        assertEquals(5, next.p1Score)
        assertEquals(5, next.p2Score)
    }

    @Test
    fun cpuPrefersACapturingMove() {
        val cpuAce = card(10, Rank.Ace, Suit.Spades)
        val cpuTwo = card(11, Rank.Two, Suit.Spades)
        val p1Low = card(1, Rank.Three)
        val state = GameState(
            status = GameStatus.Playing,
            board = mapOf(
                BoardSlot.Center to PlacedCard(p1Low, PlayerId.One),
                BoardSlot.TopLeft to PlacedCard(card(2, Rank.Nine), PlayerId.Two),
            ),
            player2Hand = listOf(cpuTwo, cpuAce),
            currentPlayer = PlayerId.Two,
        )

        val move = CpuAi.chooseMove(state, Random(0))

        assertEquals(cpuAce.id, move!!.cardId)
        assertTrue(
            move.slot in listOf(
                BoardSlot.TopCenter,
                BoardSlot.CenterLeft,
                BoardSlot.CenterRight,
                BoardSlot.BottomCenter,
            ),
        )
    }

    @Test
    fun deckContainsFiftyTwoUniqueCards() {
        val deck = Deck.full()
        assertEquals(52, deck.size)
        assertEquals(52, deck.map { it.id }.toSet().size)
        assertEquals(52, deck.map { it.rank to it.suit }.toSet().size)
    }

    @Test
    fun idleStateIgnoresPlace() {
        val next = GameEngine.place(GameState(), BoardSlot.TopLeft, cardId = 0)
        assertNull(next.lastPlacedSlot)
        assertEquals(GameStatus.Idle, next.status)
    }
}
