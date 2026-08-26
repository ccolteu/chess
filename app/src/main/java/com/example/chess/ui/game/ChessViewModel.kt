package com.example.chess.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.content.Context
import com.example.chess.data.GameStore
import com.example.chess.data.PrefsGameStore
import com.example.chess.domain.GameHistory
import com.example.chess.domain.GameState
import com.example.chess.domain.GameStatus
import com.example.chess.domain.Move
import com.example.chess.domain.MoveRow
import com.example.chess.domain.Piece
import com.example.chess.domain.PieceType
import com.example.chess.domain.Rules
import com.example.chess.domain.Side
import com.example.chess.domain.Square
import com.example.chess.engine.AiLevel
import com.example.chess.engine.Engine
import kotlinx.coroutines.CoroutineDispatcher
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
  val moveRows: List<MoveRow> = emptyList(),
  val canUndo: Boolean = false,
  val askResume: Boolean = false,
  val askConfirmNewGame: Boolean = false,
  val aiLevel: AiLevel = AiLevel.MEDIUM,
)

class ChessViewModel(
  private val store: GameStore,
  private val chooseAiMove: (GameState, AiLevel) -> Move? = { state, level -> Engine.chooseMove(state, level) },
  private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
  private val history = GameHistory()
  private var pendingSaved: List<Move> = emptyList()
  private var aiLevel: AiLevel = store.loadAiLevel()
  private val game: GameState
    get() = history.current
  private val _ui = MutableStateFlow(toUi())
  val uiState: StateFlow<GameUiState> = _ui

  init {
    val saved = store.load()
    if (saved.isNotEmpty()) {
      val probe = GameHistory().also { it.replaceWith(saved) }
      if (probe.isResumable) {
        pendingSaved = saved
        _ui.value = toUi().copy(askResume = true)
      } else {
        store.clear()
      }
    }
  }

  fun onSquareClicked(square: Square) {
    val ui = _ui.value
    if (ui.askResume || ui.isAiThinking || ui.gameOver || ui.promotionMove != null) return
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

  fun requestNewGame() {
    if (history.moves.isEmpty()) {
      newGame()
    } else {
      _ui.update { it.copy(askConfirmNewGame = true) }
    }
  }

  fun confirmNewGame() {
    newGame()
  }

  fun dismissNewGameConfirm() {
    _ui.update { it.copy(askConfirmNewGame = false) }
  }

  fun newGame() {
    history.reset()
    pendingPromotions = emptyList()
    pendingSaved = emptyList()
    persist()
    _ui.value = toUi()
  }

  fun resumeSavedGame() {
    if (pendingSaved.isEmpty()) return
    history.replaceWith(pendingSaved)
    pendingSaved = emptyList()
    persist()
    _ui.value = toUi()
    if (game.sideToMove == Side.BLACK && !isOver()) startAi()
  }

  fun startNewGameFromPrompt() {
    pendingSaved = emptyList()
    newGame()
  }

  fun undo() {
    if (!_ui.value.canUndo) return
    history.undoTurn()
    pendingPromotions = emptyList()
    persist()
    _ui.value = toUi()
  }

  fun setAiLevel(level: AiLevel) {
    if (level == aiLevel) return
    aiLevel = level
    store.saveAiLevel(level)
    _ui.update { it.copy(aiLevel = level) }
  }

  private fun playHuman(move: Move) {
    history.apply(move)
    persist()
    _ui.value = toUi()
    if (!isOver()) startAi()
  }

  private fun startAi() {
    _ui.value = toUi(aiThinking = true).copy(statusText = "Thinking…", selected = null, legalTargets = emptySet())
    viewModelScope.launch {
      val snapshot = game
      val move = withContext(computeDispatcher) { chooseAiMove(snapshot, aiLevel) }
      if (move != null) {
        history.apply(move)
        persist()
      }
      _ui.value = toUi()
    }
  }

  private fun persist() {
    store.save(history.moves)
  }

  private fun isOver(): Boolean =
    game.status == GameStatus.CHECKMATE || game.status == GameStatus.STALEMATE

  private fun toUi(aiThinking: Boolean = false): GameUiState {
    val text =
      when (game.status) {
        GameStatus.CHECKMATE -> if (game.sideToMove == Side.WHITE) "Checkmate — you lose" else "Checkmate — you win"
        GameStatus.STALEMATE -> "Stalemate — draw"
        GameStatus.CHECK -> if (game.sideToMove == Side.WHITE) "Check — your turn" else "Check"
        GameStatus.IN_PROGRESS -> if (game.sideToMove == Side.WHITE) "Your turn" else "Black to move"
      }
    return GameUiState(
      pieces = game.squares,
      lastMove = history.lastMove,
      statusText = text,
      gameOver = isOver(),
      isAiThinking = aiThinking,
      moveRows = history.rows,
      canUndo = history.canUndoTurn(aiThinking),
      aiLevel = aiLevel,
    )
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
      initializer { ChessViewModel(PrefsGameStore(context.applicationContext)) }
    }
  }
}
