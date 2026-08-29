# CardWAR — Overview

A short guide to what the game is and how the code is organized.

---

## The game

CardWAR is a two-player card game on a **3×3 board**.

**Setup**
- Each player gets **5 cards**.
- One card is dealt to the **center**, face up. It belongs to nobody.
- Score starts at **5–5**.

**Taking turns**
- Players take turns placing a card from their hand onto an **empty** slot.
- You cannot place on the center (it is already occupied).

**Capturing**
- After you place, look at the **four neighbors** (up, down, left, right — not diagonals).
- If a neighbor has a **lower** rank than your new card, and you do not already own it, it **flips to you**.
- When you capture, **one point moves** from the other player to you.
- Placing without capturing does **not** change the score.

**Winning a match**
- After **8 placements** (one per empty slot), the match ends.
- Whoever has the higher score wins the match.
- A tie gives neither player a match win.

**CPU opponent**
- Turn on **P2 CPU** to play against the computer.
- The CPU's cards are shown **face down** (you cannot see them).
- With CPU off, two people can play on the same device (**hot seat**). Both hands are visible.

**Card ranks (low to high)**  
2, 3, 4, 5, 6, 7, 8, 9, 10, Jack, Queen, King, Ace

---

## How the app is built

The app uses **Jetpack Compose** for the screen and a simple **one-way data flow**:

```
You tap the screen
       ↓
  GameViewModel        (handles taps, sounds, CPU delay)
       ↓
  GameEngine           (applies the rules)
       ↓
  new GameState        (updated board, hands, scores)
       ↓
  GameScreen           (redraws from the new state)
```

**Idea:** the UI never changes the game directly. It sends an **action** ("select this card", "place here", "start new match"). The engine returns a **new state**. The screen just shows that state.

---

## Folder layout

```
app/src/main/java/com/rick/cardwar/

  MainActivity.kt          Opens the app and shows GameScreen

  game/                    All game rules (no Android UI here)
    model/                 Data types: cards, board slots, GameState
    GameEngine.kt          Rules: deal, select, place, score
    Deck.kt                Builds and shuffles the 52-card deck
    ai/CpuAi.kt            CPU picks a move
    CardDrawables.kt       Maps each card to its picture file

  ui/                      Everything on screen
    GameViewModel.kt       Connects taps to the engine; runs CPU turn
    GameScreen.kt          Main layout (hands, board, score, dialogs)
    components/            Reusable pieces (CardFace, PlayerHand, …)
    theme/                 Colors and Material theme (Miku teal / pink)

  audio/
    GameSoundPlayer.kt     Beeps for select, place, capture
```

**Rule of thumb:** if it is about *what happens in the game*, it belongs in `game/`. If it is about *what you see or hear*, it belongs in `ui/` or `audio/`.

---

## Important types

### `GameState`

One object that describes the whole match:

- whose turn it is
- both players' hands
- what is on the board
- scores and match wins
- which card is selected
- whether CPU is on

The UI reads this and draws everything from it.

### `GameAction`

Things the player (or CPU) can ask the engine to do:

| Action | Meaning |
|---|---|
| `StartMatch` | Deal a new game |
| `SelectCard` | Pick a card from your hand |
| `Place` | Put the selected card on a slot |
| `SetCpu` | Turn CPU opponent on or off |

### `GameEngine`

Pure game logic. Given the current state and an action, it returns the **next** state.

- Illegal moves (wrong turn, occupied slot, etc.) return the **unchanged** state.
- No sounds, no delays, no Android — easy to unit test.

### `GameViewModel`

Sits between the UI and the engine:

- Holds the current `GameState` in a `StateFlow` so Compose can observe it.
- Sends actions to `GameEngine`.
- Plays sounds when something actually changed.
- Waits ~400 ms, then lets the CPU move if it is P2's turn.

Sound on/off is stored here, not in `GameState`, because it is a preference, not a game rule.

---

## Screen layout

The game works in **landscape** and **portrait**.

**Landscape:** P1 hand | board | P2 hand  

**Portrait:** P2 hand on top, board in the middle, P1 hand on bottom

**Top bar:** CPU toggle, Start, Rules, Sound  

**Bottom bar:** match wins, current score `[5 - 5]`, whose turn it is

Cards in a hand are **spaced evenly** across the available width or height. The board scales to use the space left in the middle.

---

## Board and cards

The board is a **3×3 grid** defined by `BoardSlot` (nine named positions). Each slot knows its **orthogonal neighbors** — the engine and CPU use the same list, so capture logic is written once.

**`CardFace`** shows a card (or the card back):

- **Empty slot** — back image, neutral border
- **Face-down hand card** (CPU mode) — back image, player-colored border
- **Face-up card** — rank/suit image, owner-colored border

Player 1 uses **teal** borders; Player 2 uses **pink/magenta**.

---

## CPU behavior

Simple strategy:

1. Try to find a move that **captures** one of your cards.
2. If none, pick a **random** legal card and slot.

The CPU does not cheat — it never looks at your hidden hand. It only sees what is already on the board, same as a human would.

---

## Tests

Two test files check that things work without running the app on a phone:

| File | Tests |
|---|---|
| `GameEngineTest` | Deal, captures, scoring, illegal moves, match end |
| `GameViewModelTest` | CPU turn, mute, start cancels pending CPU move |

Run them from Android Studio or with:

```
./gradlew :app:testDebugUnitTest
```

---

## Adding something new

**New rule** (e.g. diagonal capture)
1. Add a `GameAction` variant.
2. Handle it in `GameEngine.reduce`.
3. Add a test in `GameEngineTest`.
4. Wire a button or tap in `GameViewModel` / `GameScreen`.

**New UI piece** (e.g. a hint button)
1. Add a composable in `ui/components/`.
2. Pass in the state and a callback — do not call `GameEngine` from the composable directly.

**New sound**
1. Add a method to `GameSoundPlayer`.
2. Call it from `GameViewModel` when the right thing happens.

If you need `Context` or drawable IDs inside `GameEngine`, the code is probably in the wrong place.
