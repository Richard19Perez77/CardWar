# CardWAR

A two-player capture card game for Android. Place cards on a 3×3 board, flip weaker neighbors, and win the match by outscoring your opponent after eight turns.

This project is a **Compose rewrite** of an older **Eclipse-era Java** game. The original lived in a single giant `MainActivity`, targeted tablets in landscape only, and carried a few rule bugs in the capture logic. It was rebuilt here with **[Cursor](https://cursor.com)** — I described what I wanted, iterated on the results, and shaped the final app through that back-and-forth.

---

## How to play

**Setup**
- Each player gets **5 cards**.
- One card is dealt to the **center**, face up. It belongs to nobody.
- Score starts at **5–5**.

**On your turn**
- Pick a card from your hand, then tap an **empty** slot to place it.
- You cannot place on the center — it is already occupied.

**Capturing**
- After you place, check the **four neighbors** (up, down, left, right — not diagonals).
- If a neighbor has a **lower** rank than your new card, and you do not already own it, it **flips to you**.
- Each capture moves **one point** from the other player to you.
- Placing without capturing does **not** change the score.

**End of match**
- After **8 placements** (one per empty slot), the match ends.
- Higher score wins the match. Ties award neither player.

**Card ranks (low to high)**  
2 → Ace (Jack, Queen, King in between)

**Modes**
- **Hot seat** — two people on one device. Both hands are visible.
- **P2 CPU** — play against the computer. The CPU's cards are face down; it does not peek at your hand.

Tap **Start** to deal. Use **Rules** for a quick in-app reminder.

---

## What's new in this version

Compared to the old Eclipse build, this rewrite:

- Runs on **phones and tablets**, **portrait and landscape**
- Separates **game rules** from the UI (testable, easier to maintain)
- Fixes capture bugs from the original (wrong owner/slot on flips)
- Keeps the original scoring: start **5–5**, points move **only on captures**
- Uses a **Hatsune Miku–inspired** teal/pink theme for the chrome
- Shows **teal** (P1) and **magenta** (P2) card borders so ownership is clear
- Spaces hands **evenly** across the screen instead of stacking them left
- Hides the CPU opponent's cards when **P2 CPU** is on
- Uses simple **system tones** for sound effects (no bundled audio files)

That list reflects both what the port needed and **suggestions I made while building it in Cursor** — layout tweaks, theme colors, score bar layout, face-down CPU cards, dropping unused credits/art, and focusing the architecture on a small, readable codebase rather than copying the old monolith line-for-line.

---

## Screenshots

*(Add screenshots here if you like.)*

---

## Requirements

- Android **7.0+** (API 24)
- Android Studio with a recent SDK (project targets API 37)

---

## Build and run

Open the project in Android Studio and run the **app** configuration on a device or emulator.

From the command line:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

---

## Project docs

For a bit more on how the code is laid out (without a deep architecture write-up), see **[docs/OVERVIEW.md](docs/OVERVIEW.md)**.

---

## License

*(Add a license if you plan to publish this.)*
