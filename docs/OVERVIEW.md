# CardWAR — Overview

A short guide to what the game is and how the code is organized. Written for beginners — no Android experience required to follow the big ideas.

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

The app uses **Jetpack Compose** to draw the screen. Compose is a way to describe UI in Kotlin — you write functions that say *what* to show, and the framework redraws when data changes.

The code follows **one-way data flow** (sometimes called UDF — unidirectional data flow):

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

**The key idea:** the UI never changes the game directly. It sends an **action** ("select this card", "place here", "start new match"). The engine returns a **new state**. The screen just shows that state.

Think of it like a vending machine: you press a button (action), the machine decides what happens (engine), and the display updates (UI).

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
    components/            Reusable screen pieces (see below)
    theme/                 Colors and Material theme (Miku teal / pink)

  audio/
    GameSounds.kt          Interface for sound effects
    GameSoundPlayer.kt     Plays system tones on a real device
```

**Rule of thumb:** if it is about *what happens in the game*, it belongs in `game/`. If it is about *what you see or hear*, it belongs in `ui/` or `audio/`.

Card images live in `app/src/main/res/drawable-nodpi/` (52 PNGs at 186×240). A script in `tools/process-cards.ps1` can regenerate them from source JPGs.

---

## Architecture in plain terms

| Layer | Job | Knows about Android UI? |
|---|---|---|
| `game/` | Rules and data | No |
| `GameViewModel` | Turn taps into actions; hold state for the screen | A little (ViewModel lifecycle) |
| `ui/` | Draw everything; forward taps upward | Yes (Compose) |
| `audio/` | Beep on select / place / capture | Yes (ToneGenerator) |

**Why split it this way?**
- Game rules can be **unit tested** on your computer without a phone.
- UI can be changed (colors, layout) without risking broken rules.
- CPU and human both go through the same `GameEngine` — no special-case cheating.

---

## Important classes

### `GameState`

One object that describes the whole match at a moment in time:

| Field | Meaning |
|---|---|
| `status` | Idle, Playing, or Finished |
| `board` | Which cards sit on which slots, and who owns them |
| `player1Hand` / `player2Hand` | Cards still in each hand |
| `currentPlayer` | Whose turn it is |
| `selectedCardId` | Card picked from hand, waiting to be placed |
| `p1Score` / `p2Score` | Current score (starts at 5 each) |
| `p1GamesWon` / `p2GamesWon` | Match wins across the session |
| `cpuOpponent` | Whether Player 2 is the computer |
| `placementsThisMatch` | How many cards have been placed (match ends at 8) |

The UI reads this and draws everything from it. When anything changes, a **new** `GameState` is created — the old one is never edited in place.

`GameState` is marked `@Immutable` so Compose can skip redrawing parts of the screen that did not change.

Helper: `handOf(player)` returns that player's hand list.

---

### `GameAction`

A fixed list of things the player (or CPU) can ask the engine to do:

| Action | Meaning |
|---|---|
| `StartMatch` | Deal a new game |
| `SelectCard` | Pick a card from your hand |
| `Place` | Put a card on a board slot |
| `SetCpu` | Turn CPU opponent on or off |

These live in `GameEngine.kt` as a `sealed interface` — meaning the compiler knows every possible action. That makes the `when` block in the engine exhaustive and safe to extend.

---

### `GameEngine`

Pure game logic. Given the current state and an action, it returns the **next** state.

```kotlin
GameEngine.reduce(state, action) → newState
```

- Illegal moves (wrong turn, occupied slot, etc.) return the **same object** — not a copy. Callers detect rejection with `newState === oldState`.
- No sounds, no delays, no Android APIs — easy to unit test.

Main functions: `startMatch`, `selectCard`, `place`. The public entry point is always `reduce`.

---

### `GameViewModel`

Sits between the UI and the engine:

- Holds the current `GameState` in a `StateFlow`. A **StateFlow** is like a box the screen watches — when the value inside changes, Compose redraws.
- Every tap calls `dispatch(action)`, which runs `GameEngine.reduce` and updates the flow.
- Plays sounds when something actually changed (select, place, capture).
- Waits ~400 ms, then lets the CPU move if it is P2's turn.

Sound on/off is stored here (`soundEnabled`), not in `GameState`, because it is a **preference**, not a game rule.

The screen calls simple methods: `startGame()`, `selectCard(id)`, `tapSlot(slot)`, `setCpuEnabled(on)`, `toggleSound()`.

---

### Model types (`game/model/`)

Small data classes and enums. No logic beyond simple helpers.

| Type | What it is |
|---|---|
| `PlayingCard` | One card: id, rank, suit |
| `Rank` | 2 through Ace, each with a numeric value for comparisons |
| `Suit` | Hearts, Diamonds, Clubs, Spades |
| `PlayerId` | `One`, `Two`, or `None` (center card / unowned) |
| `BoardSlot` | One of nine named positions on the 3×3 grid |
| `PlacedCard` | A card on the board plus who owns it |
| `GameStatus` | `Idle`, `Playing`, `Finished` |
| `GameRules` | Constants: hand size (5), starting score (5), placements per match (8) |

**`BoardSlot`** is worth a closer look. Each slot knows its **orthogonal neighbors** (up/down/left/right, not diagonals). Both capture logic and CPU AI use the same list, so neighbor rules are written once.

---

### `Deck` and `CardDrawables`

- **`Deck`** — builds all 52 cards and shuffles them.
- **`CardDrawables.kt`** — maps a `PlayingCard` to its PNG resource id (e.g. `R.drawable.ace_of_spades`). This is the one bridge from game data to Android resources; it stays outside `GameEngine` so the engine stays testable.

---

### `CpuAi`

Simple strategy:

1. Try to find a move that **captures** a Player 1 card.
2. If none, pick a **random** legal card and slot.

The CPU does not cheat — it never looks at your hidden hand. It only sees what is already on the board, same as a human would.

Returns a `CpuMove` (card id + slot). The ViewModel sends that as `GameAction.Place(slot, cardId)`.

---

## Screen and UI classes

### `MainActivity`

The app entry point. Sets the theme and shows `GameScreen()`. That is all it does — no game logic here.

---

### `GameScreen`

The main layout. Contains:

- Background board image
- Top **HUD** (CPU, Start, Rules, Sound)
- Middle **play area** (hands + board) — switches between landscape and portrait
- Bottom **score bar** (wins, score, whose turn)

`GameScreenContent` is the testable/previewable version that takes state and callbacks as parameters. The real screen gets those from `GameViewModel`.

Small helpers at the bottom of the file (private extensions on `GameState`):

- `acceptsTouchFrom(player)` — is it this player's turn and are they human?
- `playableBy` — who can drop the selected card on the board right now?
- `isCpu(player)` — is this hand controlled by the computer?

These keep turn/CPU rules out of the composables themselves.

---

### `GameHud`

Top bar: CPU checkbox, Start, Rules, Sound toggle. Four equal slots so buttons stay aligned when labels change width.

---

### Layout: how the play area fits on screen

The play area is built in two orientations:

**Landscape** (tablet / phone on its side)

```
┌──────────────────────────────────────┐
│  P1 hand  │   3×3 board   │  P2 hand │
│  (3+2)    │               │  (3+2)   │
└──────────────────────────────────────┘
```

**Portrait** (phone upright)

```
┌──────────────┐
│  P2 hand     │  opponent on top
├──────────────┤
│  3×3 board   │  largest section
├──────────────┤
│  P1 hand     │  you on bottom (thumb reach)
└──────────────┘
```

**`PlayAreaLayout`** — computes card sizes once per orientation so board and hands stay in proportion. Returns a `PlayAreaSizes` with `boardCard` and `handCard` sizes.

- **Landscape:** one size for everything (board rows and hand rows both hold 3 cards across).
- **Portrait:** board cards can be larger than hand cards (board has 3 per row; hands have 5).

**`HandSlot`** — where a hand sits relative to the board. Each slot knows:
- how many cards per row (`Above`/`Below` = 5, `Left`/`Right` = 3)
- which way a selected card **leans** toward the board (visual hint)

**`PlayerHand`** — draws a hand inside a **turn panel**. When it is that player's turn, the panel gets a dark scrim (`HudScrim`) and a violet outline (`TurnHighlight`) — same treatment as the top/bottom bars.

**`BoardGrid`** — the 3×3 grid. Empty slots highlight in the active player's color when a card is selected and ready to place (`playableBy`). Uses `BoardSlot.rows` to lay out the grid.

**`CardFace`** — one card (or card back). Border color shows owner (teal P1, pink P2) or selection. Three visual modes:
- **Empty slot** — back image, faint border
- **Face-down hand card** (CPU) — back image, player-colored border
- **Face-up card** — rank/suit image, owner-colored border

---

### Theme (`ui/theme/`)

| File | Purpose |
|---|---|
| `Color.kt` | Miku teal/pink palette, player borders, `HudScrim`, `TurnHighlight` |
| `Theme.kt` | Material 3 light/dark schemes |
| `Type.kt` | Font styles for HUD and score bar |

Player 1 = **teal**. Player 2 = **pink/magenta**. Turn highlight = **violet** (same for both players, so it reads clearly against the teal background art).

---

### Audio

| File | Purpose |
|---|---|
| `GameSounds` | Interface: `playSelected`, `playPlaced`, `playTurned`, `release` |
| `GameSoundPlayer` | Uses Android `ToneGenerator` for short beeps |

The interface exists so tests can pass a silent fake instead of needing a real device.

---

## Data flow example: placing a card

1. You tap a card in your hand → `GameScreen` calls `viewModel.selectCard(id)`.
2. ViewModel sends `GameAction.SelectCard(id)` to `GameEngine.reduce`.
3. Engine sets `selectedCardId` in a new `GameState`.
4. ViewModel updates `StateFlow`; Compose redraws. Empty board slots glow your color.
5. You tap an empty slot → `viewModel.tapSlot(slot)`.
6. ViewModel sends `GameAction.Place(slot)`.
7. Engine moves the card, checks captures, updates score, switches turn.
8. ViewModel plays a sound and updates state. If CPU is on and it is P2's turn, a delayed job runs `CpuAi.chooseMove` and dispatches `Place` automatically.

The UI never called `GameEngine` itself — only the ViewModel did.

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
1. Add a `GameAction` variant in `GameEngine.kt`.
2. Handle it in `GameEngine.reduce`.
3. Add a test in `GameEngineTest`.
4. Wire a button or tap in `GameViewModel` / `GameScreen`.

**New UI piece** (e.g. a hint button)
1. Add a composable in `ui/components/`.
2. Pass in state and a callback — do **not** call `GameEngine` from the composable directly.

**New sound**
1. Add a method to `GameSounds`.
2. Implement it in `GameSoundPlayer`.
3. Call it from `GameViewModel.playFeedback` when the right thing happens.

**Layout change** (e.g. bigger board on small phones)
1. Adjust constants or math in `PlayAreaLayout.kt`.
2. Check both `@Preview` sizes in `GameScreen.kt` (800×480 landscape, 360×800 portrait).

If you need `Context` or drawable IDs inside `GameEngine`, the code is probably in the wrong place.

---

## Quick reference: who talks to whom

```
MainActivity
    └── GameScreen
            ├── GameViewModel ──→ GameEngine
            │        └── GameSoundPlayer
            ├── GameHud
            ├── PlayerHand ──→ CardFace
            ├── BoardGrid  ──→ CardFace
            └── ScoreBar

GameEngine ──→ Deck, BoardSlot, GameState, GameRules
GameViewModel ──→ CpuAi (when CPU is on)
CardFace ──→ CardDrawables (for card images)
```

The UI layer only **reads** `GameState` and **sends** actions upward. The engine only **reads** state and **returns** new state. Nothing in `game/` imports from `ui/`.
