package com.example.myapplication.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.myapplication.model.GridGameState
import com.example.myapplication.model.GridPos
import com.example.myapplication.model.NumberPoint
import kotlin.math.abs

class GameViewModel : ViewModel() {

    private val _gameState = mutableStateOf(GridGameState())
    val gameState: State<GridGameState> = _gameState

    init {
        loadLevel()
    }

    private fun loadLevel() {
        // Hardcoded sample level based on the image provided
        // Image shows numbers up to 8 on a grid (looks like 5x7 or 6x6)
        // Let's go with 6x6 for now
        val numbers = listOf(
            NumberPoint(GridPos(0, 0), 1),
            NumberPoint(GridPos(5, 5), 2),
            NumberPoint(GridPos(1, 5), 3),
            NumberPoint(GridPos(1, 2), 4),
            NumberPoint(GridPos(5, 4), 5),
            NumberPoint(GridPos(3, 4), 6),
            NumberPoint(GridPos(5, 1), 7),
            NumberPoint(GridPos(4, 2), 8)
        )
        
        _gameState.value = GridGameState(
            gridSize = 6,
            numberPoints = numbers,
            nextTargetNumber = 1
        )
    }

    fun onCellTouched(pos: GridPos) {
        val state = _gameState.value
        if (state.isWin) return

        val currentPath = state.currentPath
        
        // 0. Smoothness: If same cell as last, skip
        if (currentPath.lastOrNull() == pos) return

        // 1. Backtracking: If cell is already in path, truncate path
        if (pos in currentPath) {
            val index = currentPath.indexOf(pos)
            val updatedPath = currentPath.subList(0, index + 1).toList()
            
            // Re-calculate target number based on the truncated path
            val nextTarget = calculateNextTarget(updatedPath, state.numberPoints)
            
            _gameState.value = state.copy(
                currentPath = updatedPath,
                nextTargetNumber = nextTarget,
                isWin = false
            )
            return
        }

        // 2. Start path if touched the next target number
        if (currentPath.isEmpty()) {
            val target = state.numberPoints.find { it.number == 1 }
            if (target != null && target.pos == pos) {
                _gameState.value = state.copy(
                    currentPath = listOf(pos),
                    nextTargetNumber = 1,
                    history = state.history + listOf(currentPath)
                )
            }
            return
        }

        // 3. Normal extension
        val lastPos = currentPath.last()
        if (isAdjacent(lastPos, pos)) {
            val hitNumber = state.numberPoints.find { it.pos == pos }
            var nextTarget = state.nextTargetNumber

            if (hitNumber != null) {
                // In Zip game, you can ONLY cross a number if it is the NEXT sequence number
                if (hitNumber.number == nextTarget + 1) {
                    nextTarget++
                } else if (hitNumber.number > 1) {
                    return // Invalid number hit
                }
            }

            val updatedPath = currentPath + pos
            val isWin = nextTarget == state.numberPoints.maxOf { it.number }

            _gameState.value = state.copy(
                currentPath = updatedPath,
                nextTargetNumber = nextTarget,
                isWin = isWin
            )
        }
    }

    private fun calculateNextTarget(path: List<GridPos>, numberPoints: List<NumberPoint>): Int {
        var reached = 1
        path.forEach { pos ->
            val node = numberPoints.find { it.pos == pos }
            if (node != null && node.number == reached + 1) {
                reached++
            }
        }
        return reached
    }

    fun undo() {
        val state = _gameState.value
        if (state.history.isNotEmpty()) {
            val lastPath = state.history.last()
            // We also need to revert the target number if the path we undid hit a number
            // This simplification might need refinement
            _gameState.value = state.copy(
                currentPath = lastPath,
                history = state.history.dropLast(1)
                // nextTargetNumber logic for undo would be more complex
            )
        }
    }

    fun reset() {
        loadLevel()
    }

    private fun isAdjacent(p1: GridPos, p2: GridPos): Boolean {
        return (abs(p1.x - p2.x) == 1 && p1.y == p2.y) ||
               (abs(p1.y - p2.y) == 1 && p1.x == p2.x)
    }
}
