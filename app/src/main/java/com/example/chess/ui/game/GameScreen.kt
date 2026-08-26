package com.example.chess.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chess.domain.Piece
import com.example.chess.domain.PieceType
import com.example.chess.domain.Side
import com.example.chess.domain.Square
import com.example.chess.theme.ChessTheme

private val LightSq = Color(0xFFF0D9B5)
private val DarkSq = Color(0xFFB58863)
private val Selected = Color(0x66F6F669)
private val LastMove = Color(0x66BACA44)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(modifier: Modifier = Modifier, viewModel: ChessViewModel = viewModel()) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text("Chess") },
        actions = { TextButton(onClick = viewModel::newGame) { Text("New game") } },
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
        modifier = Modifier.padding(bottom = 16.dp),
      )
      ChessBoard(
        pieces = state.pieces,
        selected = state.selected,
        legalTargets = state.legalTargets,
        lastMove = state.lastMove?.let { setOf(it.from, it.to) }.orEmpty(),
        onSquareClick = viewModel::onSquareClicked,
        enabled = !state.isAiThinking && !state.gameOver,
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
              Text(glyph(Piece(type, Side.WHITE)), fontSize = 28.sp)
            }
          }
        }
      },
      confirmButton = {},
    )
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
            Box(
              modifier =
                Modifier.size(tile)
                  .background(if (light) LightSq else DarkSq)
                  .then(
                    when {
                      selected == square -> Modifier.background(Selected)
                      square in lastMove -> Modifier.background(LastMove)
                      else -> Modifier
                    }
                  )
                  .clickable(enabled = enabled) { onSquareClick(square) },
              contentAlignment = Alignment.Center,
            ) {
              val piece = pieces[square.index]
              if (piece != null) {
                Text(glyph(piece), fontSize = (tile.value * 0.55f).sp, fontWeight = FontWeight.Bold)
              } else if (square in legalTargets) {
                Box(Modifier.size(tile * 0.28f).background(Color(0x66000000), CircleShape))
              }
              if (piece != null && square in legalTargets) {
                Box(Modifier.fillMaxSize().border(3.dp, Color(0x88000000)))
              }
            }
          }
        }
      }
    }
  }
}

private fun glyph(piece: Piece): String =
  when (piece.side to piece.type) {
    Side.WHITE to PieceType.KING -> "♔"
    Side.WHITE to PieceType.QUEEN -> "♕"
    Side.WHITE to PieceType.ROOK -> "♖"
    Side.WHITE to PieceType.BISHOP -> "♗"
    Side.WHITE to PieceType.KNIGHT -> "♘"
    Side.WHITE to PieceType.PAWN -> "♙"
    Side.BLACK to PieceType.KING -> "♚"
    Side.BLACK to PieceType.QUEEN -> "♛"
    Side.BLACK to PieceType.ROOK -> "♜"
    Side.BLACK to PieceType.BISHOP -> "♝"
    Side.BLACK to PieceType.KNIGHT -> "♞"
    Side.BLACK to PieceType.PAWN -> "♟"
    else -> ""
  }

@Preview(showBackground = true)
@Composable
private fun GameScreenPreview() {
  ChessTheme { GameScreen() }
}
