package com.example.myapplication.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.GridGameState
import com.example.myapplication.model.GridPos
import com.example.myapplication.model.NumberPoint
import com.example.myapplication.viewmodel.GameViewModel

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.gameState
    
    // Live timer state
    var currentTime by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    
    // Update timer every 100ms while game is not won
    androidx.compose.runtime.LaunchedEffect(state.isWin) {
        if (!state.isWin) {
            while (true) {
                kotlinx.coroutines.delay(100)
                currentTime = System.currentTimeMillis()
            }
        }
    }
    
    // Calculate elapsed time
    val elapsedTime = if (state.isWin) {
        state.endTime - state.startTime
    } else {
        currentTime - state.startTime
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Score / Title can go here
            Text(
                text = "ZIP PUZZLE",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF333333)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Live Timer Display
            TimerDisplay(elapsedTime = elapsedTime, isWin = state.isWin)
            
            Spacer(modifier = Modifier.height(24.dp))

            // The Main Game Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(8.dp)
            ) {
                GameGrid(state, onCellTouched = { viewModel.onCellTouched(it) })
            }

            Spacer(modifier = Modifier.weight(1f))

            // Footer Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GameActionButton("Undo", onClick = { viewModel.undo() })
                GameActionButton("Reset", onClick = { viewModel.reset() })
            }
        }

        if (state.isWin && !state.isDialogShown) {
            WinOverlay(
                elapsedTime = viewModel.getElapsedTime(),
                onRestart = { viewModel.reset() },
                onDismiss = { viewModel.markDialogShown() }
            )
        }
    }
}

@Composable
fun GameGrid(state: GridGameState, onCellTouched: (GridPos) -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    var sizePx by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0f) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { sizePx = it.width.toFloat() }
    ) {
        val gridSize = state.gridSize
        val cellSizePx = sizePx / gridSize
        val cellSizeDp = with(density) { cellSizePx.toDp() }

        if (sizePx > 0) {
            // 1. Drawing Layer (Grid & Path)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(gridSize, sizePx) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val currentCellSize = sizePx / gridSize
                                event.changes.forEach { change ->
                                    val x = (change.position.x / currentCellSize).toInt().coerceIn(0, gridSize - 1)
                                    val y = (change.position.y / currentCellSize).toInt().coerceIn(0, gridSize - 1)
                                    val cell = GridPos(x, y)
                                    onCellTouched(cell)
                                    change.consume()
                                }
                            }
                        }
                    }
            ) {
                // Draw Grid Lines
                for (i in 0..gridSize) {
                    val pos = i * cellSizePx
                    drawLine(Color(0xFFEEEEEE), Offset(pos, 0f), Offset(pos, size.height), 2f)
                    drawLine(Color(0xFFEEEEEE), Offset(0f, pos), Offset(size.width, pos), 2f)
                }

                // Draw Path
                if (state.currentPath.size >= 2) {
                    val path = Path().apply {
                        val first = state.currentPath.first()
                        moveTo(first.x * cellSizePx + cellSizePx / 2, first.y * cellSizePx + cellSizePx / 2)
                        state.currentPath.drop(1).forEach {
                            lineTo(it.x * cellSizePx + cellSizePx / 2, it.y * cellSizePx + cellSizePx / 2)
                        }
                    }
                    // Use green color when solved, otherwise use orange
                    val pathColor = if (state.isWin) Color(0xFF4CAF50) else Color(0xFFFF5722)
                    drawPath(path, pathColor.copy(alpha = 0.2f), style = Stroke(cellSizePx * 0.8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(path, pathColor, style = Stroke(cellSizePx * 0.45f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }

            // 2. Numbers Layer (Nodes)
            state.numberPoints.forEach { point ->
                Box(
                    modifier = Modifier
                        .size(cellSizeDp)
                        .offset(x = cellSizeDp * point.pos.x, y = cellSizeDp * point.pos.y),
                    contentAlignment = Alignment.Center
                ) {
                    NumberNode(
                        number = point.number,
                        isTarget = point.number == state.nextTargetNumber
                    )
                }
            }
        }
    }
}

@SuppressLint("Range")
@Composable
fun NumberNode(number: Int, isTarget: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize(0.7f)
            .background(Color.Black, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isTarget) {
            // Highlight ring for the next target
            Surface(
                modifier = Modifier.fillMaxSize(1.2f),
                shape = CircleShape,
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF5722).copy(alpha = 0.5f))
            ) {}
        }
        Text(
            text = number.toString(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun TimerDisplay(elapsedTime: Long, isWin: Boolean) {
    val seconds = (elapsedTime / 1000) % 60
    val minutes = (elapsedTime / 1000) / 60
    val timeText = if (minutes > 0) {
        String.format("%02d:%02d", minutes, seconds)
    } else {
        String.format("%02d", seconds)
    }
    
    Text(
        text = timeText,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = if (isWin) Color(0xFF4CAF50) else Color(0xFF333333)
    )
}

@Composable
fun GameActionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(120.dp).height(48.dp)
    ) {
        Text(text, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun WinOverlay(elapsedTime: Long, onRestart: () -> Unit, onDismiss: () -> Unit) {
    // Format elapsed time
    val seconds = (elapsedTime / 1000) % 60
    val minutes = (elapsedTime / 1000) / 60
    val timeText = if (minutes > 0) {
        String.format("%d:%02d", minutes, seconds)
    } else {
        String.format("%d seconds", seconds)
    }
    
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("EXCELLENT!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Board Cleared", color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Time: $timeText",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onRestart()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("PLAY AGAIN")
                }
            }
        }
    }
}

private fun getCell(offset: Offset, cellSizePx: Float): GridPos {
    val x = (offset.x / cellSizePx).toInt().coerceIn(0, 5) // Max is gridSize - 1
    val y = (offset.y / cellSizePx).toInt().coerceIn(0, 5) // Max is gridSize - 1
    return GridPos(x, y)
}
