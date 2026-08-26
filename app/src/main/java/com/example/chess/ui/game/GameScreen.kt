package com.example.chess.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chess.domain.MoveRow
import com.example.chess.domain.Piece
import com.example.chess.domain.PieceType
import com.example.chess.domain.Side
import com.example.chess.domain.Square
import com.example.chess.engine.AiLevel
import com.example.chess.theme.Brass
import com.example.chess.theme.ChessTheme
import com.example.chess.theme.Cream
import com.example.chess.theme.Ink
import com.example.chess.theme.LegalDot
import com.example.chess.theme.OakSquare
import com.example.chess.theme.Parchment
import com.example.chess.theme.PieceCream
import com.example.chess.theme.PieceEspresso
import com.example.chess.theme.SquareWash
import com.example.chess.theme.WalnutBackground
import com.example.chess.theme.WalnutRail
import com.example.chess.theme.WalnutSquare

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val viewModel: ChessViewModel = viewModel(factory = ChessViewModel.factory(context))
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val actionColors = ButtonDefaults.textButtonColors(contentColor = Brass, disabledContentColor = Brass.copy(alpha = 0.35f))
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = WalnutBackground,
    topBar = {
      TopAppBar(
        title = { Text("Chess", style = MaterialTheme.typography.titleLarge, color = Cream) },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = WalnutBackground,
            titleContentColor = Cream,
            actionIconContentColor = Brass,
          ),
        actions = {
          AiLevelMenu(level = state.aiLevel, onPick = viewModel::setAiLevel, colors = actionColors)
          TextButton(onClick = viewModel::undo, enabled = state.canUndo, colors = actionColors) { Text("Undo") }
          TextButton(onClick = viewModel::requestNewGame, colors = actionColors) { Text("New game") }
        },
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = state.statusText,
        style = MaterialTheme.typography.titleMedium,
        color = Cream,
        modifier = Modifier.padding(bottom = 16.dp),
      )
      BoardFrame {
        ChessBoard(
          pieces = state.pieces,
          selected = state.selected,
          legalTargets = state.legalTargets,
          lastMove = state.lastMove?.let { setOf(it.from, it.to) }.orEmpty(),
          onSquareClick = viewModel::onSquareClicked,
          enabled = !state.askResume && !state.isAiThinking && !state.gameOver,
        )
      }
      MoveList(
        rows = state.moveRows,
        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 16.dp),
      )
    }
  }
  if (state.promotionMove != null) {
    AlertDialog(
      onDismissRequest = viewModel::onPromotionDismissed,
      title = { Text("Promote pawn") },
      text = {
        Row {
          listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { type ->
            TextButton(onClick = { viewModel.onPromotionPicked(type) }) {
              Image(
                painter = painterResource(pieceDrawableRes(Piece(type, Side.WHITE))),
                contentDescription = type.name,
                modifier = Modifier.size(36.dp),
                colorFilter = ColorFilter.tint(PieceCream, BlendMode.Modulate),
              )
            }
          }
        }
      },
      confirmButton = {},
    )
  }
  if (state.askConfirmNewGame) {
    AlertDialog(
      onDismissRequest = viewModel::dismissNewGameConfirm,
      title = { Text("Start a new game?") },
      text = { Text("This will erase the game in progress.") },
      confirmButton = { TextButton(onClick = viewModel::confirmNewGame) { Text("New game") } },
      dismissButton = { TextButton(onClick = viewModel::dismissNewGameConfirm) { Text("Cancel") } },
    )
  }
  if (state.askResume) {
    AlertDialog(
      onDismissRequest = {},
      title = { Text("Resume game?") },
      text = { Text("A game was in progress. Do you want to resume it or start a new one?") },
      confirmButton = { TextButton(onClick = viewModel::resumeSavedGame) { Text("Resume") } },
      dismissButton = { TextButton(onClick = viewModel::startNewGameFromPrompt) { Text("New game") } },
    )
  }
}

@Composable
private fun AiLevelMenu(
  level: AiLevel,
  onPick: (AiLevel) -> Unit,
  colors: ButtonColors,
) {
  var open by remember { mutableStateOf(false) }
  Box {
    TextButton(onClick = { open = true }, colors = colors) { Text(level.label) }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      AiLevel.entries.forEach { option ->
        DropdownMenuItem(
          text = { Text(option.label) },
          onClick = {
            onPick(option)
            open = false
          },
        )
      }
    }
  }
}

@Composable
private fun BoardFrame(content: @Composable () -> Unit) {
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .shadow(12.dp, RoundedCornerShape(10.dp), ambientColor = Color.Black.copy(alpha = 0.55f), spotColor = Color.Black.copy(alpha = 0.7f))
        .background(WalnutRail, RoundedCornerShape(10.dp))
        .padding(12.dp)
        .border(2.dp, Brass, RoundedCornerShape(4.dp))
        .padding(4.dp),
  ) {
    content()
  }
}

@Composable
private fun MoveList(rows: List<MoveRow>, modifier: Modifier = Modifier) {
  val listState = rememberLazyListState()
  LaunchedEffect(rows.size, rows.lastOrNull()?.black) {
    if (rows.isNotEmpty()) {
      listState.animateScrollToItem(rows.lastIndex)
    }
  }
  Surface(
    modifier = modifier.shadow(6.dp, RoundedCornerShape(8.dp)),
    color = Parchment,
    contentColor = Ink,
    shape = RoundedCornerShape(8.dp),
    border = BorderStroke(1.dp, Brass.copy(alpha = 0.55f)),
  ) {
    if (rows.isEmpty()) {
      Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopStart) {
        Text("Moves will appear here", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else {
      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
      ) {
        items(rows, key = { it.number }) { row ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
          ) {
            Text(
              text = "${row.number}.",
              fontFamily = FontFamily.Monospace,
              color = Ink,
              maxLines = 1,
              modifier = Modifier.width(36.dp),
            )
            Text(row.white, fontFamily = FontFamily.Monospace, color = Ink, modifier = Modifier.weight(1f))
            Text(row.black.orEmpty(), fontFamily = FontFamily.Monospace, color = Ink, modifier = Modifier.weight(1f))
          }
        }
      }
    }
  }
}

@Composable
private fun ChessBoard(
  pieces: List<Piece?>,
  selected: Square?,
  legalTargets: Set<Square>,
  lastMove: Set<Square>,
  onSquareClick: (Square) -> Unit,
  enabled: Boolean,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
    val tile = maxWidth / 8
    Column {
      for (displayRank in 7 downTo 0) {
        Row {
          for (file in 0..7) {
            val square = Square(file, displayRank)
            val light = (file + displayRank) % 2 == 1
            val marked = selected == square || square in lastMove
            Box(
              modifier =
                Modifier.size(tile)
                  .drawBehind { drawWoodSquare(light, selected == square) }
                  .then(if (marked) Modifier.border(2.dp, Brass) else Modifier)
                  .clickable(enabled = enabled) { onSquareClick(square) },
              contentAlignment = Alignment.Center,
            ) {
              val piece = pieces[square.index]
              if (piece != null) {
                PieceGlyph(piece = piece, size = tile * 0.9f)
              } else if (square in legalTargets) {
                Box(Modifier.size(tile * 0.26f).background(LegalDot, CircleShape))
              }
              if (piece != null && square in legalTargets) {
                Box(Modifier.fillMaxSize().border(3.dp, Ink.copy(alpha = 0.55f)))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PieceGlyph(piece: Piece, size: Dp) {
  Image(
    painter = painterResource(pieceDrawableRes(piece)),
    contentDescription = null,
    modifier = Modifier.size(size),
    colorFilter =
      ColorFilter.tint(if (piece.side == Side.WHITE) PieceCream else PieceEspresso, BlendMode.Modulate),
  )
}

private fun DrawScope.drawWoodSquare(light: Boolean, selected: Boolean) {
  val base = if (light) OakSquare else WalnutSquare
  drawRect(base)
  val streak = if (light) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.14f)
  val step = size.width / 8f
  if (light) {
    for (i in 0..7) {
      drawRect(color = streak, topLeft = Offset(i * step + 1.2f, 0f), size = Size(1.1f, size.height))
    }
  } else {
    for (i in 0..7) {
      drawRect(color = streak, topLeft = Offset(0f, i * step + 1.2f), size = Size(size.width, 1.1f))
    }
  }
  if (selected) {
    drawRect(SquareWash)
  }
}

@Preview(showBackground = true)
@Composable
private fun GameScreenPreview() {
  ChessTheme { GameScreen() }
}
