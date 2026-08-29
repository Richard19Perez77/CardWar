package com.rick.cardwar.game.model

enum class BoardSlot(val row: Int, val column: Int) {
    TopLeft(0, 0),
    TopCenter(0, 1),
    TopRight(0, 2),
    CenterLeft(1, 0),
    Center(1, 1),
    CenterRight(1, 2),
    BottomLeft(2, 0),
    BottomCenter(2, 1),
    BottomRight(2, 2);

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
