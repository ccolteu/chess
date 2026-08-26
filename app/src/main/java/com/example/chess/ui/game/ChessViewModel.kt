package com.example.chess.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.domain.GameStatus
import com.example.chess.domain.Move
import com.example.chess.domain.Piece
import com.example.chess.domain.PieceType
import com.example.chess.domain.Rules
import com.example.chess.domain.Side
import com.example.chess.domain.Square
import com.example.chess.domain.startingGame
import com.example.chess.engine.Engine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class GameUiState(
  val pieces: List<Piece?>,
  val selected: Square? = null,
  val legalTargets: Set<Square> = emptySet(),
  val lastMove: Move? = null,
  val statusText: String = "Your turn",
  val isAiThinking: Boolean = false,
  val promotionMove: Move? = null,
  val gameOver: Boolean = false,
)

class ChessViewModel : ViewModel() {
  private var game = Rules.withStatus(startingGame())
  private val _ui = MutableStateFlow(toUi())
  val uiState: StateFlow<GameUiState> = _ui

  fun onSquareClicked(square: Square) {
    val ui = _ui.value
    if (ui.isAiThinking || ui.gameOver || ui.promotionMove != null) return
    if (game.sideToMove != Side.WHITE) return

    val legal = Rules.legalMoves(game)
    if (ui.selected != null) {
      val candidates = legal.filter { it.from == ui.selected && it.to == square }
      when {
        candidates.size > 1 -> {
          _ui.update { it.copy(promotionMove = candidates.first().copy(promotion = null), selected = null, legalTargets = emptySet()) }
          pendingPromotions = candidates
          return
        }
        candidates.size == 1 -> {
          playHuman(candidates.first())
          return
        }
      }
    }
    val piece = game.pieceAt(square)
    if (piece?.side == Side.WHITE) {
      val targets = legal.filter { it.from == square }.map { it.to }.toSet()
      _ui.update { it.copy(selected = square, legalTargets = targets) }
    } else {
      _ui.update { it.copy(selected = null, legalTargets = emptySet()) }
    }
  }

  private var pendingPromotions: List<Move> = emptyList()

  fun onPromotionPicked(type: PieceType) {
    val move = pendingPromotions.firstOrNull { it.promotion == type } ?: return
    pendingPromotions = emptyList()
    _ui.update { it.copy(promotionMove = null) }
    playHuman(move)
  }

  fun onPromotionDismissed() {
    pendingPromotions = emptyList()
    _ui.update { it.copy(promotionMove = null) }
  }

  fun newGame() {
    game = Rules.withStatus(startingGame())
    pendingPromotions = emptyList()
    _ui.value = toUi()
  }

  private fun playHuman(move: Move) {
    game = Rules.apply(game, move)
    _ui.value = toUi(last = move)
    if (!isOver()) startAi()
  }

  private fun startAi() {
    _ui.update { it.copy(isAiThinking = true, statusText = "Thinking…", selected = null, legalTargets = emptySet()) }
    viewModelScope.launch {
      val snapshot = game
      val move = withContext(Dispatchers.Default) { Engine.chooseMove(snapshot) }
      if (move != null) {
        game = Rules.apply(game, move)
      }
      _ui.value = toUi(last = move ?: _ui.value.lastMove).copy(isAiThinking = false)
    }
  }

  private fun isOver(): Boolean =
    game.status == GameStatus.CHECKMATE || game.status == GameStatus.STALEMATE

  private fun toUi(last: Move? = null): GameUiState {
    val text =
      when (game.status) {
        GameStatus.CHECKMATE -> if (game.sideToMove == Side.WHITE) "Checkmate — you lose" else "Checkmate — you win"
        GameStatus.STALEMATE -> "Stalemate — draw"
        GameStatus.CHECK -> if (game.sideToMove == Side.WHITE) "Check — your turn" else "Check"
        GameStatus.IN_PROGRESS -> if (game.sideToMove == Side.WHITE) "Your turn" else "Black to move"
      }
    return GameUiState(
      pieces = game.squares,
      lastMove = last,
      statusText = text,
      gameOver = isOver(),
    )
  }
}
