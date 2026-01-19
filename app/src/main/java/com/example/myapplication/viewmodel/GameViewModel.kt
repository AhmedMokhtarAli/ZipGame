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
        val gridSize = 6
        val numbers = com.example.myapplication.logic.LevelGenerator.generate(gridSize)
        
        _gameState.value = GridGameState(
            gridSize = gridSize,
            numberPoints = numbers,
            nextTargetNumber = 1,
            currentPath = emptyList(),
            isWin = false,
            history = emptyList(),
            startTime = System.currentTimeMillis(),
            endTime = 0L,
            isDialogShown = false
        )
    }

    fun onCellTouched(pos: GridPos) {
        val state = _gameState.value
        if (state.isWin) return

        val currentPath = state.currentPath

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

        // 2. Start path if touched number 1 (or any cell to start from number 1)
        if (currentPath.isEmpty()) {
            // Check if this cell contains number 1
            val number1Point = state.numberPoints.firstOrNull { it.number == 1 }
            val isNumber1 = number1Point != null && number1Point.pos.x == pos.x && number1Point.pos.y == pos.y
            
            if (isNumber1) {
                _gameState.value = state.copy(
                    currentPath = listOf(pos),
                    nextTargetNumber = 2,
                    history = emptyList()
                )
            }
            return
        }

        // 3. Extend path (must be adjacent)
        val lastPos = currentPath.last()
        
        // Skip if same as last position
        if (lastPos == pos) return
        
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
            
            // Win condition: all numbers connected AND all cells filled
            val allNumbersConnected = nextTarget == state.numberPoints.maxOf { it.number }
            val allCellsFilled = updatedPath.size == state.gridSize * state.gridSize
            val isWin = allNumbersConnected && allCellsFilled

            _gameState.value = state.copy(
                currentPath = updatedPath,
                nextTargetNumber = nextTarget,
                isWin = isWin,
                endTime = if (isWin) System.currentTimeMillis() else state.endTime
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

    fun markDialogShown() {
        _gameState.value = _gameState.value.copy(isDialogShown = true)
    }

    fun getElapsedTime(): Long {
        val state = _gameState.value
        return if (state.endTime > 0) {
            state.endTime - state.startTime
        } else {
            0L
        }
    }

    private fun isAdjacent(p1: GridPos, p2: GridPos): Boolean {
        return (abs(p1.x - p2.x) == 1 && p1.y == p2.y) ||
               (abs(p1.y - p2.y) == 1 && p1.x == p2.x)
    }
}
