# CardWAR — Design and Architecture Overview

This document explains how the Compose port is structured, which standards it follows, and why those choices were made. It reflects the app as it exists after the port, not the original Eclipse/Java tablet build.

## What the game is

CardWAR is a two-player capture game on a 3×3 board. Each player is dealt five cards. The center starts face-up and **unowned**; it cannot be placed on. Players alternate placing onto the eight empty slots. After a place, any **orthogonal** neighbor you do not already own, with a **strictly lower** rank, flips to you and a point moves. Score starts **5–5** and **only changes on captures**. After eight placements the match ends; the higher score takes the match win. Ties award neither player.

Optional **P2 CPU** plays the second seat. With the CPU on, P2's hand is dealt **face down**, because the CPU never reads P1's cards and showing its hand only leaked information one way. With the CPU off both hands stay face up for hot-seat play, matching the original.

---

## Why the original was rewritten, not transcribed

The Java app was a single `MainActivity` (~1,800 lines) with:

- One `onClick` handler per card and per slot
- Capture logic copy-pasted eight times, with real owner/slot bugs
- Pixel/`DisplayMetrics` density switches to fake a tablet layout
- Forced landscape and `requiresSmallestWidthDp="600"` so phones were filtered out

That shape does not port cleanly to Compose. Compose wants **state in, UI out**. The Java switches encoded one rule eight times. The port encodes that rule **once** and lets the UI observe the result.

We kept the **intended rules** (including 5–5 capture scoring and unowned center) and **fixed accidental bugs** (wrong owner written on a flip, wrong slot painted, CPU neighbor mistakes). That was an explicit product choice: faithful game, not bit-perfect Java.

---

## Architecture standards

### 1. Unidirectional data flow (UDF)

```
User / CPU  →  GameViewModel  →  GameEngine.reduce(state, action)  →  new GameState
                     ↑                         │
                     └──── StateFlow ──────────┘
                              │
                         GameScreen (Compose)
```

- **`GameState`** is an immutable data class, annotated `@Immutable` so Compose can skip subtrees when the instance has not changed. Nothing in the UI mutates cards or scores in place.
- **`GameAction`** is a sealed interface (`StartMatch`, `SelectCard`, `Place`, `SetCpu`). Every rule change is an action.
- **`GameEngine.reduce`** is a pure function: `(GameState, GameAction) → GameState`. No Android types, no Compose, no sounds.
- **`reduce` is the only way state changes.** The ViewModel has a single private `dispatch(action)`; it never calls `place`/`selectCard` directly and never writes fields on the state. Rule constants live in `GameRules` so the engine and `GameState` defaults cannot drift apart.

**Rejected moves return the same instance.** When an action is illegal the engine does `return state` rather than building an equal copy. That makes `after === before` a reliable "nothing happened" signal, so `dispatch` can decide whether to play a sound or hand the turn to the CPU without diffing fields.

This is the same idea as Redux / MVI, kept small. It is the right fit for a turn-based game: the next board is a function of the previous board plus one move. Time-travel, tests, and CPU play all become “feed an action, assert the state.”

**Why not LiveData + mutable fields?** Mutable `Card.player` and `ImageButton` updates were how the original drifted into bugs. Immutable state makes illegal combinations harder (a selected card that is not in the current hand, a place onto an occupied slot) because the engine simply returns the old state.

### 2. Separation of game, UI, and platform

| Layer | Package | Allowed to know |
|---|---|---|
| Domain | `game.model`, `game.GameEngine`, `game.Deck`, `game.ai` | Kotlin only |
| Presentation | `ui`, `ui.components`, `ui.theme` | Compose + `GameState` |
| Platform | `audio`, `MainActivity` | Android framework |

**`MainActivity`** only calls `setContent`. It does not deal cards, play sounds, or own score. That is the Android recommended “single activity, thin host” pattern.

**`GameViewModel`** is the only bridge: it holds `StateFlow<GameState>`, runs the CPU on a coroutine delay, and asks `GameSounds` to beep. Sound enablement is **not** in `GameState` because it is a device preference, not a rule.

It is a plain `ViewModel`, not an `AndroidViewModel`. Nothing it owns needs a `Context` once sounds moved to `ToneGenerator`, and audio is injected as the **`GameSounds`** interface so the whole ViewModel — CPU scheduling included — is testable on the JVM.

**`CardDrawables`** maps rank+suit → `R.drawable`. That is Android, so it lives next to the deck mapping but is **not** used by `GameEngine` or unit tests. Tests construct `PlayingCard(id, rank, suit)` directly.

### 3. Package-by-feature, then by role

```
com.rick.cardwar
  MainActivity.kt                 // Compose host
  audio/
    GameSounds.kt                 // interface the ViewModel depends on
    GameSoundPlayer.kt            // ToneGenerator SFX (Android)
  game/
    model/                        // Rank, Suit, PlayerId, PlayingCard, BoardSlot, GameState, GameRules
    Deck.kt
    GameEngine.kt
    CardDrawables.kt              // resource IDs only
    ai/CpuAi.kt
  ui/
    GameViewModel.kt
    GameScreen.kt                 // layout + dialogs
    components/                   // CardFace, PlayerHand, BoardGrid, GameHud
    theme/                        // Miku color scheme
```

**Why this instead of `mvp/` or a full Clean Architecture (`domain/data/presentation`)?** There is no network, database, or repository. Inventing use-case classes around four actions would add files without adding meaning. The engine *is* the domain. ViewModel *is* the application service. That matches Google’s guidance for small apps: skip layers you cannot justify.

**Why model types are tiny files (`Rank.kt`, `Suit.kt`, …)?** Each type has a single reason to change (rank table, neighbor graph, match flags). `BoardSlot.orthogonalNeighbors` is the capture graph; keeping it on the enum means the engine never hard-codes “top-left checks top-center and center-left” in eight functions again.

### 4. Single source of truth for layout slots

`BoardSlot` is a 3×3 enum with `row`, `column`, and `orthogonalNeighbors`. The board UI iterates `BoardSlot.rows`. The engine iterates neighbors. CPU uses the same graph. There is no parallel set of view IDs (`slotTopLeft`, `slottl`, …).

### 5. Test the rules where they live

Both suites are plain JVM unit tests — no Robolectric, no Compose, no device.

`GameEngineTest` covers deal, orthogonal capture, no diagonal capture, equal rank does not flip, occupied center cannot be placed, unowned center *does* move a point, eight placements end the match, ties award no wins, CPU prefers a capturing move, and `List.removeAt(0)` deal (not Java 21 `removeFirst()`).

`GameViewModelTest` covers the parts the engine cannot see: that an illegal tap changes nothing and stays silent, that the CPU actually moves after the human (driven by `StandardTestDispatcher` + `advanceUntilIdle` instead of a real 400ms wait), that it stays put while disabled, that a new **Start** cancels a pending CPU move, and that mute suppresses every effect. `GameSounds` is faked to count calls.

If a capture feels wrong in the UI, the failing case belongs in `GameEngineTest` first. If *turn flow* feels wrong, it belongs in `GameViewModelTest`.

### 6. Compose UI standards

- **Stateless screens where possible.** `GameScreenContent(state, callbacks)` is previewable without a `ViewModel`. `GameScreen()` is the thin wrapper that `viewModel()` + `collectAsStateWithLifecycle()`.
- **State hoisting.** Hands and the grid do not remember selection; they display `selectedCardId` from `GameState`.
- **`BoxWithConstraints` for adaptive layout**, not `DisplayMetrics` density buckets. Landscape vs portrait is `maxWidth > maxHeight`, so multi-window and foldables get the same rule as rotation.
- **No `weight()` outside `RowScope`/`ColumnScope`.** HUD slots are `RowScope.HudSlot` so equal columns compile. Score uses `Modifier.weight(1f)` *inside* the `Row`.
- **Lifecycle-aware collection** (`collectAsStateWithLifecycle`) so the UI does not collect in the background after the activity stops.

### 7. ViewModel + coroutines for “time”

CPU is not in `GameEngine`. The engine is synchronous. The ViewModel waits ~400ms, then `CpuAi.chooseMove` + `GameEngine.place`. That delay is UX, not a rule. Cancelling `cpuJob` on a new **Start** or on `onCleared` prevents a stale CPU move from applying after the player already dealt again.

---

## Design decisions (product and UI)

### All devices, both orientations

The original locked landscape and hid phones. This build does **not** set `screenOrientation` and does **not** use `requiresSmallestWidthDp`. `android:appCategory="game"` is set so large-screen Android versions that ignore orientation locks still treat this as a game.

| Orientation | Layout |
|---|---|
| Landscape | P1 hand \| 3×3 board \| P2 hand |
| Portrait | P2 hand (top), board, P1 hand (bottom) |

Portrait puts P1 nearest the typical holding edge, as if sitting at a table.

### Hands: fill the strip, small gaps, no overlap by default

The first Compose hands **fanned/overlapped** (~20% offset) and **left-aligned**, which wasted width and let a selected card’s `zIndex` cover neighbors.

Current rule: size cards so **n cards + 6dp gaps** fill the main axis (width in portrait, height in landscape), then constrain by the cross axis. Extra space **centers** the group. Selection only changes border color and a small nudge toward the board — it does not raise `zIndex`.

Overlap remains a valid idea on a tiny screen, but with even sizing the five cards already fit on phones without stacking.

### Board is the hero

The 3×3 uses leftover space after HUD, score, and hands. Card size is `min(availW/3, availH/3)` at the 93:120 art ratio. Grid cells **never** overlap. Empty cells show the card back; occupied cells show face + owner border.

### Player colors (after theme work)

Rules chrome is Hatsune Miku teal. **Ownership borders** must still contrast with the board *and* with each other:

| Seat | Color | Hex |
|---|---|---|
| Player 1 | Dark teal | `#137A7F` |
| Player 1 selected | Miku teal | `#39C5BB` |
| Player 2 | Magenta | `#E12885` |
| Player 2 selected | Bright pink | `#FF5AAD` |

Borders are **6dp** (8dp selected) so ownership is readable at phone size. Neutral/center uses a dark unowned border. Rules copy says TEAL / PINK, not blue / red.

### Miku theme for chrome, not for rules

Material 3 schemes use Crypton’s `#39C5BB` plus ink/mint/grey. Dynamic color is **off** so a wallpaper-derived purple does not fight the board. The activity forces **dark** scheme because the table art is dark; light scheme still exists for completeness.

HUD buttons and the CPU checkbox use `MaterialTheme.colorScheme`, not hardcoded navy.

### Top bar: four equal columns

**P2 CPU | Start | Rules | Sound**, each in `weight(1f)` centered in its slot. Horizontal scroll was removed; it collapsed the row to content width and prevented even spacing.

Credits were removed when the licensed card art went away.

### Score bar: three columns

- Start: `Player 1 Wins = N` (teal)
- Center: `[p1 - p2]`
- End: `Player 2 Wins = N` (magenta)

Turn / idle / match-over text stays centered on the line below. That matches how you read a table: your record, the current fight, their record.

### Sound: system tones, no assets

The original had two MP3s and **no music**. Those files are gone. `ToneGenerator` on `STREAM_MUSIC` plays:

- Select — `TONE_PROP_BEEP`
- Place without capture — `TONE_PROP_BEEP2`
- Capture — `TONE_PROP_ACK`

The in-app mute still gates all three. No `res/raw`, no `SoundPool.load` crash on missing IDs.

**Why not `AudioManager.playSoundEffect`?** Those follow the system “touch sounds” setting and can be globally off, which would make the game mute look broken. Tones always play when the app toggle is on (unless music volume is zero).

### Icons and launcher

Sound uses **volume-up / volume-off** Material Symbols checked in as two local vector drawables, tinted with primary teal.

**Why not `material-icons-extended`?** Google stopped publishing that library and removed it from recent Material 3 releases; it also shipped every Material icon, which measurably slows builds. The documented replacement is to copy the individual vector XML you need, which for two icons is ~20 lines.

Launcher vectors were restored after drawables were deleted: teal field + white card/spade. Adaptive icons live in `mipmap-anydpi-v26`, with per-density `webp` bitmaps as the API 24–25 fallback. An earlier layer-list in plain `mipmap-anydpi` was removed: `anydpi` outranks density folders, so it silently shadowed those bitmaps on exactly the versions it was meant to help.

### Java 21 `List.removeFirst()`

Kotlin compiles `removeFirst()` to a Java 21 `List` method. Older ART throws `NoSuchMethodError`. Deal uses **`removeAt(0)`**, which exists on all API levels we support.

---

## CPU

Greedy then random, same idea as the original:

1. Shuffle remaining P2 cards.
2. For each card, try empty slots in random order; take the first move that would capture a **P1** card (not merely the unowned center).
3. If none, random legal place.

Neighbors come from `BoardSlot`, so the old CPU bugs (checking the wrong adjacent slot) cannot recur without failing the unit test.

---

## What we deliberately did not add

- **Portrait-only or landscape-only lock** — the layout adapts.
- **Clean “territory” scoring** (1 point per owned cell) — rejected; 5–5 capture transfer is the original design.
- **Saving a match across process death** — `ViewModel` survives rotation, so the only loss is a background kill mid-match. A `SavedStateHandle` round-trip of `GameState` would fix it if that ever matters.
- **Placing on center** — still illegal; center is occupied at deal.
- **Diagonal captures** — not in the original rule.
- **Networking / save game** — out of scope for the port.
- **Full Clean Architecture / Hilt** — one screen, no I/O.

---

## How to extend it safely

1. **New rule** — add a `GameAction`, handle it in `GameEngine.reduce`, add a `GameEngineTest` case, then wire a ViewModel method that calls `dispatch`. Return the same instance for anything the rules reject.
2. **New widget** — take `GameState` slices + lambdas; do not call the engine from a composable.
3. **New SFX** — add it to the `GameSounds` interface and `GameSoundPlayer`; the ViewModel decides *when*.
4. **New tunable** — put the number in `GameRules`, not in the engine body and not in a `GameState` default.

If a change needs `Context`, `R.drawable`, or `ToneGenerator` inside `GameEngine`, it is in the wrong layer.
