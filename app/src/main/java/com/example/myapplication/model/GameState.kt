package com.example.myapplication.model

/**
 * Represents a point on the grid.
 */
data class GridPos(val x: Int, val y: Int)

/**
 * Represents a target number point on the grid.
 */
data class NumberPoint(
    val pos: GridPos,
    val number: Int
)

/**
 * Represents the state of the grid-based Zip puzzle.
 */
data class GridGameState(
    val gridSize: Int = 5,
    val numberPoints: List<NumberPoint> = emptyList(),
    val currentPath: List<GridPos> = emptyList(),
    val nextTargetNumber: Int = 1,
    val score: Int = 0,
    val isWin: Boolean = false,
    val history: List<List<GridPos>> = emptyList() // For Undo functionality
)
