# CardWAR — Design and Architecture Overview

This document explains how the Compose port is structured, which standards it follows, and why those choices were made. It reflects the app **as it exists now** — after the port from the original Eclipse/Java tablet build and a follow-up modernization pass — not the legacy source tree.

## Contents

1. [What the game is](#what-the-game-is)
2. [Why the original was rewritten](#why-the-original-was-rewritten-not-transcribed)
3. [Architecture standards](#architecture-standards)
4. [Design decisions (product and UI)](#design-decisions-product-and-ui)
5. [Build and toolchain](#build-and-toolchain)
6. [CPU](#cpu)
7. [What we deliberately did not add](#what-we-deliberately-did-not-add)
8. [How to extend it safely](#how-to-extend-it-safely)

---

## What the game is

CardWAR is a two-player capture game on a 3×3 board. Each player is dealt five cards. The center starts face-up and **unowned**; it cannot be placed on. Players alternate placing onto the eight empty slots. After a place, any **orthogonal** neighbor you do not already own, with a **strictly lower** rank, flips to you and a point moves. Score starts **5–5** and **only changes on captures**. After eight placements the match ends; the higher score takes the match win. Ties award neither player.

Optional **P2 CPU** plays the second seat. With the CPU on, P2's hand is shown **face down**, because the CPU never reads P1's cards and showing its hand only leaked information one way. With the CPU off, both hands stay face up for hot-seat play, matching the original.

Rule constants (`HandSize`, `StartingScore`, `PlacementsPerMatch`) live in `game/model/GameRules.kt` so the engine, default `GameState`, and tests cannot drift apart.

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
User / CPU  →  GameViewModel.dispatch(action)
                      ↓
              GameEngine.reduce(state, action)  →  new GameState
                      ↑                                    │
                      └────────── StateFlow ───────────────┘
                                      │
                               GameScreen (Compose)
```

- **`GameState`** is an immutable data class, annotated `@Immutable` so Compose can skip subtrees when the instance has not changed. Nothing in the UI mutates cards or scores in place.
- **`GameAction`** is a sealed interface (`StartMatch`, `SelectCard`, `Place`, `SetCpu`), defined in `GameEngine.kt` next to the reducer.
- **`GameEngine.reduce`** is a pure function: `(GameState, GameAction) → GameState`. No Android types, no Compose, no sounds.
- **`reduce` is the only way game state changes.** The ViewModel has a single private `dispatch(action)`; it never writes `_state.value` except through `reduce`, and never calls `place` / `selectCard` / `startMatch` directly.

**Rejected moves return the same instance.** When an action is illegal the engine does `return state` rather than building an equal copy. That makes `after === before` a reliable "nothing happened" signal in `dispatch`, so the ViewModel can skip sound effects and CPU scheduling without diffing fields. (JVM tests often use structural `assertEquals` on the returned state; the identity rule is what the ViewModel relies on at runtime.)

This is the same idea as Redux / MVI, kept small. It is the right fit for a turn-based game: the next board is a function of the previous board plus one move. Time-travel, tests, and CPU play all become "feed an action, assert the state."

**Why not LiveData + mutable fields?** Mutable `Card.player` and `ImageButton` updates were how the original drifted into bugs. Immutable state makes illegal combinations harder (a selected card that is not in the current hand, a place onto an occupied slot) because the engine simply returns the old state.

**Ephemeral fields on `GameState`.** `lastCapturedSlots` and `lastPlacedSlot` are set on every successful place so the ViewModel can choose the right sound. They are not used for animations today; they ride along in the snapshot because they are cheap to copy and useful for tests.

### 2. Separation of game, UI, and platform

| Layer | Package | Allowed to know |
|---|---|---|
| Domain | `game.model`, `game.GameEngine`, `game.Deck`, `game.ai` | Kotlin only |
| Presentation | `ui`, `ui.components`, `ui.theme` | Compose + `GameState` |
| Platform | `audio`, `MainActivity` | Android framework |

**`MainActivity`** only calls `enableEdgeToEdge()`, then `setContent`. It does not deal cards, play sounds, or own score. That is the Android recommended "single activity, thin host" pattern.

**`GameViewModel`** is the only bridge: it holds `StateFlow<GameState>`, runs the CPU on a coroutine delay, and asks `GameSounds` to beep. Sound enablement is **not** in `GameState` because it is a device preference, not a rule.

It is a plain `ViewModel`, not an `AndroidViewModel`. Nothing it owns needs a `Context` once sounds moved to `ToneGenerator`, and audio is injected as the **`GameSounds`** interface so the whole ViewModel — CPU scheduling included — is testable on the JVM.

**`CardDrawables`** maps rank+suit → `R.drawable` via a `PlayingCard.drawableRes` extension. That is Android-specific, so it lives in `game/` but is **not** used by `GameEngine` or unit tests. Tests construct `PlayingCard(id, rank, suit)` directly.

### 3. Package-by-feature, then by role

```
com.rick.cardwar
  MainActivity.kt                 // Compose host
  audio/
    GameSounds.kt                 // interface the ViewModel depends on
    GameSoundPlayer.kt            // ToneGenerator SFX (Android)
  game/
    model/                        // Rank, Suit, PlayerId, PlayingCard, BoardSlot,
                                  // GameState, GameRules, GameStatus
    Deck.kt
    GameEngine.kt                 // GameAction + reduce + place/select/start
    CardDrawables.kt              // resource IDs only
    ai/CpuAi.kt
  ui/
    GameViewModel.kt
    GameScreen.kt                 // layout, dialogs, previews
    components/                   // CardFace, PlayerHand, BoardGrid, GameHud
    theme/                        // Miku color scheme
```

**Why this instead of `mvp/` or a full Clean Architecture (`domain/data/presentation`)?** There is no network, database, or repository. Inventing use-case classes around four actions would add files without adding meaning. The engine *is* the domain. ViewModel *is* the application service. That matches Google's guidance for small apps: skip layers you cannot justify.

**Why model types are tiny files (`Rank.kt`, `Suit.kt`, …)?** Each type has a single reason to change (rank table, neighbor graph, match flags). `BoardSlot.orthogonalNeighbors` is the capture graph; keeping it on the enum means the engine never hard-codes "top-left checks top-center and center-left" in eight functions again.

### 4. Single source of truth for layout slots

`BoardSlot` is a 3×3 enum with `row`, `column`, and `orthogonalNeighbors`. The board UI iterates `BoardSlot.rows`. The engine iterates neighbors. CPU uses the same graph. There is no parallel set of view IDs (`slotTopLeft`, `slottl`, …).

### 5. Who can act — engine vs UI

The engine and the UI split responsibility deliberately:

| Concern | Where it lives | Behavior |
|---|---|---|
| "Is it P1's or P2's turn to place?" | `GameEngine.place` | Allows placement when `currentPlayer` is `One` or `Two`. The helper is named `isHumanSeat`, which is legacy wording — it means **active seat**, not "human-controlled". CPU moves use the same `Place` action with an explicit `cardId`. |
| "Can the human tap P2's cards?" | `GameScreen` | `player2Active = playing && currentPlayer == Two && !cpuOpponent`. P2 hand is not clickable while CPU is on. |
| "Can the human tap the board during CPU's turn?" | `GameScreen` | `boardInteractive = playing && !cpuThinking`, where `cpuThinking = cpuOpponent && currentPlayer == Two`. |
| "Can the human see P2's cards?" | `PlayerHand` | `faceDown = cpuOpponent` for P2 only. Face-down cards still show that player's border color. |

So: the engine knows turns; the UI knows which seat is human-operated and what to hide.

### 6. Test the rules where they live

Both game suites are plain JVM unit tests — no Robolectric, no Compose, no device:

| File | What it covers |
|---|---|
| `app/src/test/.../game/GameEngineTest.kt` | Deal, orthogonal capture, no diagonal capture, equal rank does not flip, occupied center cannot be placed, unowned center *does* move a point, eight placements end the match, ties award no wins, CPU prefers a capturing move, deck uniqueness |
| `app/src/test/.../ui/GameViewModelTest.kt` | Illegal tap changes nothing and stays silent; CPU moves after the human (`StandardTestDispatcher` + `advanceUntilIdle`, no real 400 ms wait); CPU idle while disabled; new **Start** cancels a pending CPU job; mute suppresses every effect via a fake `GameSounds` |

`app/src/androidTest/.../ExampleInstrumentedTest.kt` is the default Android Studio boilerplate (package name only) and is not part of the game test strategy.

If a capture feels wrong in the UI, add a case to `GameEngineTest` first. If *turn flow* feels wrong, add one to `GameViewModelTest`.

### 7. Compose UI standards

- **Stateless screens where possible.** `GameScreenContent(state, callbacks)` is previewable without a `ViewModel`. `GameScreen()` is the thin wrapper that uses `viewModel()` + `collectAsStateWithLifecycle()`.
- **State hoisting.** Hands and the grid do not remember selection; they display `selectedCardId` from `GameState`.
- **`BoxWithConstraints` for adaptive layout**, not `DisplayMetrics` density buckets. Landscape vs portrait is `maxWidth > maxHeight`, so multi-window and foldables get the same rule as rotation.
- **No `weight()` outside `RowScope`/`ColumnScope`.** HUD slots are `RowScope.HudSlot` so equal columns compile. Score uses `Modifier.weight(1f)` *inside* the `Row`.
- **Lifecycle-aware collection** (`collectAsStateWithLifecycle`) so the UI does not collect in the background after the activity stops.
- **Safe drawing insets.** `windowInsetsPadding(WindowInsets.safeDrawing)` keeps HUD and score clear of system bars. With `targetSdk 37`, edge-to-edge is the platform default; `enableEdgeToEdge()` in `MainActivity` remains as explicit insurance on older behavior.
- **Previews use real dealt state.** `previewState()` in `GameScreen.kt` runs `GameEngine.startMatch` with a fixed seed and one placement so Compose previews show cards on the board, not an empty idle shell.

### 8. ViewModel + coroutines for "time"

CPU logic is not in `GameEngine`. The engine is synchronous. After a human place (or enabling CPU mid-turn), the ViewModel:

1. Cancels any pending `cpuJob`.
2. If `cpuOpponent && currentPlayer == Two`, launches a coroutine.
3. Waits **400 ms** (`CPU_DELAY`) for readability.
4. Re-checks the snapshot (player may have hit **Start** during the delay).
5. Calls `CpuAi.chooseMove`, then `dispatch(GameAction.Place(slot, cardId))`.

That delay is UX, not a rule. Cancelling `cpuJob` on **Start** or `onCleared` prevents a stale CPU move from applying after the player already dealt again. The CPU path does not call `scheduleCpuTurn` again — after it places, the turn returns to P1.

---

## Design decisions (product and UI)

### All devices, both orientations

The original locked landscape and hid phones. This build does **not** set `screenOrientation` and does **not** use `requiresSmallestWidthDp`. `android:appCategory="game"` is set so large-screen Android versions that ignore orientation locks still treat this as a game (harmless no-op below API 26).

| Orientation | Layout |
|---|---|
| Landscape | P1 hand \| 3×3 board \| P2 hand |
| Portrait | P2 hand (top), board, P1 hand (bottom) |

Portrait puts P1 nearest the typical holding edge, as if sitting at a table.

### Hands: fill the strip, small gaps, no overlap by default

The first Compose hands **fanned/overlapped** (~20% offset) and **left-aligned**, which wasted width and let a selected card's `zIndex` cover neighbors.

Current rule: size cards so **n cards + 6dp gaps** fill the main axis (width in portrait, height in landscape), then constrain by the cross axis. Extra space **centers** the group. Selection only changes border color and a small nudge toward the board — it does not raise `zIndex`.

With five cards at 93×120, phones usually fit without stacking. Overlap remains an option if a future device proves too narrow.

### Board is the hero

The 3×3 uses leftover space after HUD, score, and hands. Card size is `min(availW/3, availH/3)` at the 93:120 art ratio. Grid cells **never** overlap.

`CardFace` distinguishes three visual modes:

| `card` | `owner` | Meaning | Border |
|---|---|---|---|
| null | `None` | Empty board slot | Dashed empty-slot border |
| null | `One` / `Two` | Face-down hand card (CPU mode) | That player's color |
| non-null | any | Face-up card | Owner / selected color |

Empty cells and face-down cards both draw the card-back art; only empty slots use the neutral empty border.

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

Material 3 schemes use Crypton's `#39C5BB` plus ink/mint/grey. Dynamic color is **off** (`dynamicColor = false` in `MainActivity` and previews) so a wallpaper-derived purple does not fight the board. The activity forces **dark** scheme because the table art is dark; a light scheme still exists in `Theme.kt` for completeness.

HUD buttons and the CPU checkbox use `MaterialTheme.colorScheme`, not hardcoded navy. Compose theme colors live in `ui/theme/Color.kt`; the XML `values/colors.xml` only exposes `miku_ink` for the launch window background.

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

**Why not `AudioManager.playSoundEffect`?** Those follow the system "touch sounds" setting and can be globally off, which would make the game mute look broken. Tones play when the app toggle is on (unless music stream volume is zero).

### Icons and launcher

Sound uses **volume-up / volume-off** Material Symbols checked in as `drawable/ic_volume_up.xml` and `ic_volume_off.xml`, tinted with `MaterialTheme.colorScheme.primary`.

**Why not `material-icons-extended`?** Google stopped publishing that library and removed it from recent Material 3 releases; it also shipped every Material icon, which measurably slows builds. The documented replacement is to copy the individual vector XML you need.

Launcher: teal field + white card/spade vectors in `drawable/`. Adaptive icons live in `mipmap-anydpi-v26`; per-density `webp` bitmaps serve API 24–25. A layer-list in plain `mipmap-anydpi` was removed because `anydpi` outranks density folders and silently shadowed those bitmaps on the versions it was meant to help.

---

## Build and toolchain

| Setting | Value | Notes |
|---|---|---|
| Gradle | 9.5.0 | Wrapper-pinned |
| AGP | 9.3.2 | Bundles Kotlin; no separate `kotlin.android` plugin |
| Kotlin (compose plugin) | 2.4.10 | Must track the Kotlin version AGP expects; verified by building |
| Compose BOM | 2026.08.00 | Single pin for all Compose artifacts |
| kotlinx-coroutines-test | 1.11.0 | JVM tests for ViewModel CPU scheduling |
| `compileSdk` / `targetSdk` | 37 | Compose 1.12+ expects this pairing |
| `minSdk` | 24 | Original reach without extra desugaring |
| `sourceCompatibility` / `targetCompatibility` | **17** | Raised from 11 in the project template |
| Release optimization | **on** | R8 + resource shrinking via `optimization { enable = true }` |

### Three different "Java versions" are in play

This trips people up, so it is worth stating plainly:

1. **The JDK that runs Gradle** — whatever Android Studio points `JAVA_HOME` at (often 17–21; can be newer). Affects the build only.
2. **`sourceCompatibility` / `targetCompatibility`** — bytecode level **17**. This is "which Java language/API the compiler targets." It does *not* let you call arbitrary newer JDK library methods on device.
3. **ART on the device**, bounded by `minSdk 24`. This decides which library methods actually exist at runtime.

Raising #2 from 11 to 17 is free because D8 desugars language features down to API 24. Changing #1 does not change the app binary.

### Java 21 `List.removeFirst()`

Point 3 is exactly why dealing uses **`removeAt(0)`** in `GameEngine.startMatch`. Kotlin compiles `removeFirst()` to the Java 21 `List` method, which older ART does not have, so it crashed with `NoSuchMethodError` at runtime even though it compiled cleanly under a modern JDK. `removeAt(0)` exists on every API level we support.

### Card art lives in `drawable-nodpi`

The 54 card PNGs plus `board.png` and `cardbw.png` are 93×120 (cards) or similar fixed sizes. In a bare `drawable/` folder Android treats bitmaps as mdpi and **upscales them at decode time** — roughly 9× the memory on a 3× device — for no visual gain, since Compose scales them to the slot size anyway. `drawable-nodpi` decodes at native size. Vector/XML assets (launcher, volume icons) stay in `drawable/`.

The launch theme is `android:Theme.Material.NoActionBar` with `@color/miku_ink` as `windowBackground`, so there is no white flash before the dark board draws.

---

## CPU

Greedy then random, same idea as the original:

1. Shuffle P2's remaining cards and empty slots (random order each check).
2. Take the first `(card, slot)` pair that would capture a **P1-owned** neighbor (not merely the unowned center).
3. If none, pick a random legal `(card, slot)`.

Neighbors come from `BoardSlot.orthogonalNeighbors`, so the old CPU bugs (checking the wrong adjacent slot) cannot recur without failing `GameEngineTest`.

The ViewModel passes the chosen move as `GameAction.Place(slot, cardId)` — no card selection step — because the CPU never uses `selectedCardId`.

---

## What we deliberately did not add

- **Portrait-only or landscape-only lock** — the layout adapts.
- **Clean "territory" scoring** (1 point per owned cell) — rejected; 5–5 capture transfer is the original design.
- **Saving a match across process death** — `ViewModel` survives rotation, so the only loss is a background kill mid-match. A `SavedStateHandle` round-trip of `GameState` would fix it if that ever matters.
- **Placing on center** — still illegal; center is occupied at deal.
- **Diagonal captures** — not in the original rule.
- **Networking / save game** — out of scope for the port.
- **Full Clean Architecture / Hilt** — one screen, no I/O; dependency injection would add ceremony without a second implementation to swap.

---

## How to extend it safely

1. **New rule** — add a `GameAction`, handle it in `GameEngine.reduce`, add a `GameEngineTest` case, then expose a ViewModel method that calls `dispatch`. Return the **same instance** for anything the rules reject.
2. **New widget** — take `GameState` slices + lambdas; do not call the engine from a composable.
3. **New SFX** — add it to the `GameSounds` interface and `GameSoundPlayer`; the ViewModel decides *when* in `playFeedback`.
4. **New tunable** — put the number in `GameRules`, not in the engine body and not duplicated in a `GameState` default.
5. **Hide or lock a seat** — UI flags in `GameScreen` / `PlayerHand` first; only touch `GameState` if the rule itself changes (e.g. a new game mode).

If a change needs `Context`, `R.drawable`, or `ToneGenerator` inside `GameEngine`, it is in the wrong layer.
