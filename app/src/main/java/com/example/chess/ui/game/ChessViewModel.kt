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
import com.example.chess.domain.capturedOf
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
  val isAiThinking: Boolean = false,
  val promotionMove: Move? = null,
  val gameOver: Boolean = false,
  val moveRows: List<MoveRow> = emptyList(),
  val canUndo: Boolean = false,
  val askResume: Boolean = false,
  val askConfirmNewGame: Boolean = false,
  val aiLevel: AiLevel = AiLevel.MEDIUM,
  val cpuCaptures: List<Piece> = emptyList(),
  val playerCaptures: List<Piece> = emptyList(),
  val cpuThinkMs: Long = 0L,
  val playerThinkMs: Long = 0L,
  val clockRunning: Side? = null,
  val clockStartedAt: Long = 0L,
)

class ChessViewModel(
  private val store: GameStore,
  private val chooseAiMove: (GameState, AiLevel) -> Move? = { state, level -> Engine.chooseMove(state, level) },
  private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
  private val nowMs: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
  private val history = GameHistory()
  private var pendingSaved: List<Move> = emptyList()
  private var pendingPromotions: List<Move> = emptyList()
  private var playerBaseMs = 0L
  private var cpuBaseMs = 0L
  private val playerLegs = mutableListOf<Long>()
  private val cpuLegs = mutableListOf<Long>()
  private var running: Side? = null
  private var turnStartedAt = 0L
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
        playerBaseMs = store.loadPlayerMs()
        cpuBaseMs = store.loadCpuMs()
        _ui.value = toUi().copy(askResume = true)
      } else {
        store.clear()
      }
    }
    if (pendingSaved.isEmpty()) {
      startRunning(Side.WHITE)
      _ui.value = toUi()
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
    resetClocks()
    startRunning(Side.WHITE)
    persist()
    _ui.value = toUi()
  }

  fun resumeSavedGame() {
    if (pendingSaved.isEmpty()) return
    history.replaceWith(pendingSaved)
    pendingSaved = emptyList()
    when {
      game.sideToMove == Side.BLACK && !isOver() -> startRunning(Side.BLACK)
      isOver() -> startRunning(null)
      else -> startRunning(Side.WHITE)
    }
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
    val removing = if (history.moves.size % 2 == 0) 2 else 1
    history.undoTurn()
    pendingPromotions = emptyList()
    if (removing >= 2 && cpuLegs.isNotEmpty()) cpuLegs.removeAt(cpuLegs.lastIndex)
    if (playerLegs.isNotEmpty()) playerLegs.removeAt(playerLegs.lastIndex)
    startRunning(Side.WHITE)
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
    val now = nowMs()
    commitRunning(now)
    history.apply(move)
    if (isOver()) {
      startRunning(null, now)
      persist()
      _ui.value = toUi()
      return
    }
    startRunning(Side.BLACK, now)
    persist()
    _ui.value = toUi()
    startAi()
  }

  private fun startAi() {
    _ui.value = toUi(aiThinking = true).copy(selected = null, legalTargets = emptySet())
    viewModelScope.launch {
      val snapshot = game
      val move = withContext(computeDispatcher) { chooseAiMove(snapshot, aiLevel) }
      val now = nowMs()
      commitRunning(now)
      if (move != null) history.apply(move)
      if (!isOver() && game.sideToMove == Side.WHITE) startRunning(Side.WHITE, now) else startRunning(null, now)
      persist()
      _ui.value = toUi()
    }
  }

  private fun resetClocks() {
    playerBaseMs = 0L
    cpuBaseMs = 0L
    playerLegs.clear()
    cpuLegs.clear()
    running = null
    turnStartedAt = 0L
  }

  private fun startRunning(side: Side?, now: Long = nowMs()) {
    running = side
    turnStartedAt = now
  }

  private fun commitRunning(now: Long) {
    val extra = if (running != null) (now - turnStartedAt).coerceAtLeast(0) else 0L
    when (running) {
      Side.WHITE -> playerLegs += extra
      Side.BLACK -> cpuLegs += extra
      null -> {}
    }
    running = null
  }

  private fun committedPlayerMs(): Long = playerBaseMs + playerLegs.sum()

  private fun committedCpuMs(): Long = cpuBaseMs + cpuLegs.sum()

  private fun snapshotClocks(): Pair<Long, Long> {
    val extra = if (running != null) (nowMs() - turnStartedAt).coerceAtLeast(0) else 0L
    return when (running) {
      Side.WHITE -> committedPlayerMs() + extra to committedCpuMs()
      Side.BLACK -> committedPlayerMs() to committedCpuMs() + extra
      null -> committedPlayerMs() to committedCpuMs()
    }
  }

  fun persistClocks() {
    val (player, cpu) = snapshotClocks()
    store.saveClocks(player, cpu)
  }

  private fun persist() {
    store.save(history.moves)
    store.saveClocks(committedPlayerMs(), committedCpuMs())
  }

  override fun onCleared() {
    persistClocks()
    super.onCleared()
  }

  private fun isOver(): Boolean =
    game.status == GameStatus.CHECKMATE || game.status == GameStatus.STALEMATE

  private fun toUi(aiThinking: Boolean = false): GameUiState {
    return GameUiState(
      pieces = game.squares,
      lastMove = history.lastMove,
      gameOver = isOver(),
      isAiThinking = aiThinking,
      moveRows = history.rows,
      canUndo = history.canUndoTurn(aiThinking),
      aiLevel = aiLevel,
      cpuCaptures = capturedOf(Side.WHITE, game.squares),
      playerCaptures = capturedOf(Side.BLACK, game.squares),
      cpuThinkMs = committedCpuMs(),
      playerThinkMs = committedPlayerMs(),
      clockRunning = running,
      clockStartedAt = turnStartedAt,
    )
  }

  companion object {
    fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
      initializer { ChessViewModel(PrefsGameStore(context.applicationContext)) }
    }
  }
}
