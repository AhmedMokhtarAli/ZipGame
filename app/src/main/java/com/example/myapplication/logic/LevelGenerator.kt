package com.example.myapplication.logic

import com.example.myapplication.model.GridPos
import com.example.myapplication.model.NumberPoint
import kotlin.random.Random

/**
 * Procedural generator for Zip puzzle levels.
 * Uses a Hamiltonian path (visits every cell exactly once) to ensure
 * the solution requires filling the entire grid.
 */
object LevelGenerator {

    fun generate(gridSize: Int): List<NumberPoint> {
        val path = generateHamiltonianPath(gridSize) ?: return emptyList()
        
        // Place numbers at intervals along the complete path
        val numbers = mutableListOf<NumberPoint>()
        
        if (path.isEmpty()) return emptyList()

        // Always start at position 1
        numbers.add(NumberPoint(path.first(), 1))

        // Calculate spacing based on grid size to get 6-10 numbers
        val totalCells = gridSize * gridSize
        val targetNumbers = when {
            gridSize <= 4 -> 4
            gridSize <= 6 -> 6
            else -> 8
        }
        
        val stepSize = totalCells / targetNumbers

        var currentIndex = stepSize
        var nextNumber = 2
        
        while (currentIndex < path.size - stepSize / 2 && nextNumber < targetNumbers) {
            numbers.add(NumberPoint(path[currentIndex], nextNumber))
            nextNumber++
            currentIndex += stepSize
        }

        // Always end at the last cell
        if (path.last() !in numbers.map { it.pos }) {
            numbers.add(NumberPoint(path.last(), nextNumber))
        }

        return numbers
    }

    /**
     * Generate a Hamiltonian path using backtracking.
     * This ensures every cell is visited exactly once.
     */
    private fun generateHamiltonianPath(size: Int): List<GridPos>? {
        val totalCells = size * size
        val visited = mutableSetOf<GridPos>()
        val path = mutableListOf<GridPos>()
        
        // Start from a random corner
        val startX = if (Random.nextBoolean()) 0 else size - 1
        val startY = if (Random.nextBoolean()) 0 else size - 1
        val start = GridPos(startX, startY)
        
        if (findPath(start, visited, path, size, totalCells)) {
            return path
        }
        
        // If failed from random corner, try systematic search from (0,0)
        visited.clear()
        path.clear()
        if (findPath(GridPos(0, 0), visited, path, size, totalCells)) {
            return path
        }
        
        return null
    }

    /**
     * Backtracking search for Hamiltonian path
     */
    private fun findPath(
        current: GridPos,
        visited: MutableSet<GridPos>,
        path: MutableList<GridPos>,
        size: Int,
        targetLength: Int
    ): Boolean {
        visited.add(current)
        path.add(current)
        
        if (path.size == targetLength) {
            return true // Found complete path!
        }
        
        // Try neighbors in random order for variety
        val neighbors = getNeighbors(current, size).filter { it !in visited }.shuffled()
        
        for (next in neighbors) {
            if (findPath(next, visited, path, size, targetLength)) {
                return true
            }
        }
        
        // Backtrack
        visited.remove(current)
        path.removeAt(path.size - 1)
        return false
    }

    private fun getNeighbors(pos: GridPos, size: Int): List<GridPos> {
        return listOf(
            GridPos(pos.x + 1, pos.y),
            GridPos(pos.x - 1, pos.y),
            GridPos(pos.x, pos.y + 1),
            GridPos(pos.x, pos.y - 1)
        ).filter { it.x in 0 until size && it.y in 0 until size }
    }
}
