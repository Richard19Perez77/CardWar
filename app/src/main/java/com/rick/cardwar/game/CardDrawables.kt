package com.rick.cardwar.game

import com.rick.cardwar.R
import com.rick.cardwar.game.model.PlayingCard
import com.rick.cardwar.game.model.Rank
import com.rick.cardwar.game.model.Suit

object CardDrawables {
    fun res(rank: Rank, suit: Suit): Int = when (suit) {
        Suit.Hearts -> when (rank) {
            Rank.Two -> R.drawable.twoh
            Rank.Three -> R.drawable.threeh
            Rank.Four -> R.drawable.fourh
            Rank.Five -> R.drawable.fiveh
            Rank.Six -> R.drawable.sixh
            Rank.Seven -> R.drawable.sevenh
            Rank.Eight -> R.drawable.eighth
            Rank.Nine -> R.drawable.nineh
            Rank.Ten -> R.drawable.tenh
            Rank.Jack -> R.drawable.elevenh
            Rank.Queen -> R.drawable.twelveh
            Rank.King -> R.drawable.thirtteenh
            Rank.Ace -> R.drawable.aceh
        }
        Suit.Diamonds -> when (rank) {
            Rank.Two -> R.drawable.twod
            Rank.Three -> R.drawable.threed
            Rank.Four -> R.drawable.fourd
            Rank.Five -> R.drawable.fived
            Rank.Six -> R.drawable.sixd
            Rank.Seven -> R.drawable.sevend
            Rank.Eight -> R.drawable.eightd
            Rank.Nine -> R.drawable.nined
            Rank.Ten -> R.drawable.tend
            Rank.Jack -> R.drawable.elevend
            Rank.Queen -> R.drawable.twelved
            Rank.King -> R.drawable.thirtteend
            Rank.Ace -> R.drawable.aced
        }
        Suit.Spades -> when (rank) {
            Rank.Two -> R.drawable.twos
            Rank.Three -> R.drawable.threes
            Rank.Four -> R.drawable.fours
            Rank.Five -> R.drawable.fives
            Rank.Six -> R.drawable.sixs
            Rank.Seven -> R.drawable.sevens
            Rank.Eight -> R.drawable.eights
            Rank.Nine -> R.drawable.nines
            Rank.Ten -> R.drawable.tens
            Rank.Jack -> R.drawable.elevens
            Rank.Queen -> R.drawable.twelves
            Rank.King -> R.drawable.thirtteens
            Rank.Ace -> R.drawable.aces
        }
        Suit.Clubs -> when (rank) {
            Rank.Two -> R.drawable.twoc
            Rank.Three -> R.drawable.threec
            Rank.Four -> R.drawable.fourc
            Rank.Five -> R.drawable.fivec
            Rank.Six -> R.drawable.sixc
            Rank.Seven -> R.drawable.sevenc
            Rank.Eight -> R.drawable.eightc
            Rank.Nine -> R.drawable.ninec
            Rank.Ten -> R.drawable.tenc
            Rank.Jack -> R.drawable.elevenc
            Rank.Queen -> R.drawable.twelvec
            Rank.King -> R.drawable.thirtteenc
            Rank.Ace -> R.drawable.acec
        }
    }
}

val PlayingCard.drawableRes: Int
    get() = CardDrawables.res(rank, suit)
