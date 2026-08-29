package com.rick.cardwar.game.model

enum class BoardSlot {
    TopLeft,
    TopCenter,
    TopRight,
    CenterLeft,
    Center,
    CenterRight,
    BottomLeft,
    BottomCenter,
    BottomRight;

    val orthogonalNeighbors: List<BoardSlot>
        get() = when (this) {
            TopLeft -> listOf(TopCenter, CenterLeft)
            TopCenter -> listOf(TopLeft, TopRight, Center)
            TopRight -> listOf(TopCenter, CenterRight)
            CenterLeft -> listOf(TopLeft, Center, BottomLeft)
            Center -> listOf(TopCenter, CenterLeft, CenterRight, BottomCenter)
            CenterRight -> listOf(TopRight, Center, BottomRight)
            BottomLeft -> listOf(CenterLeft, BottomCenter)
            BottomCenter -> listOf(Center, BottomLeft, BottomRight)
            BottomRight -> listOf(CenterRight, BottomCenter)
        }

    companion object {
        val rows: List<List<BoardSlot>> = listOf(
            listOf(TopLeft, TopCenter, TopRight),
            listOf(CenterLeft, Center, CenterRight),
            listOf(BottomLeft, BottomCenter, BottomRight),
        )
    }
}
