package com.example.chess.ui.game

import com.example.chess.data.GameStore
import com.example.chess.engine.AiLevel
import com.example.chess.domain.Move
import com.example.chess.domain.Square
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChessViewModelTest {
  private val dispatcher = StandardTestDispatcher()

  @Before
  fun setMain() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun reset() {
    Dispatchers.resetMain()
  }

  @Test
  fun savedUnfinishedGame_asksToResume() {
    val store = MemoryGameStore(listOf(move("e2", "e4"), move("e7", "e5")))
    val vm = ChessViewModel(store, chooseAiMove = { _, _ -> null })
    assertTrue(vm.uiState.value.askResume)
    assertTrue(vm.uiState.value.moveRows.isEmpty())
  }

  @Test
  fun resume_restoresMoves() {
    val store = MemoryGameStore(listOf(move("e2", "e4"), move("e7", "e5")))
    val vm = ChessViewModel(store, chooseAiMove = { _, _ -> null })
    vm.resumeSavedGame()
    assertFalse(vm.uiState.value.askResume)
    assertEquals(1, vm.uiState.value.moveRows.size)
    assertEquals("e2-e4", vm.uiState.value.moveRows[0].white)
    assertEquals("e7-e5", vm.uiState.value.moveRows[0].black)
  }

  @Test
  fun decline_clearsSaveAndStartsFresh() {
    val store = MemoryGameStore(listOf(move("e2", "e4"), move("e7", "e5")))
    val vm = ChessViewModel(store, chooseAiMove = { _, _ -> null })
    vm.startNewGameFromPrompt()
    assertFalse(vm.uiState.value.askResume)
    assertTrue(vm.uiState.value.moveRows.isEmpty())
    assertTrue(store.load().isEmpty())
  }

  @Test
  fun requestNewGame_onEmptyBoard_doesNotAsk() {
    val vm = ChessViewModel(MemoryGameStore(), chooseAiMove = { _, _ -> null })
    vm.requestNewGame()
    assertFalse(vm.uiState.value.askConfirmNewGame)
    assertTrue(vm.uiState.value.moveRows.isEmpty())
  }

  @Test
  fun requestNewGame_withMoves_asksAndKeepsPosition() {
    val store = MemoryGameStore(listOf(move("e2", "e4"), move("e7", "e5")))
    val vm = ChessViewModel(store, chooseAiMove = { _, _ -> null })
    vm.resumeSavedGame()
    vm.requestNewGame()
    assertTrue(vm.uiState.value.askConfirmNewGame)
    assertEquals(1, vm.uiState.value.moveRows.size)
    assertEquals(listOf(move("e2", "e4"), move("e7", "e5")), store.load())
  }

  @Test
  fun dismissNewGameConfirm_keepsGame() {
    val store = MemoryGameStore(listOf(move("e2", "e4"), move("e7", "e5")))
    val vm = ChessViewModel(store, chooseAiMove = { _, _ -> null })
    vm.resumeSavedGame()
    vm.requestNewGame()
    vm.dismissNewGameConfirm()
    assertFalse(vm.uiState.value.askConfirmNewGame)
    assertEquals(1, vm.uiState.value.moveRows.size)
  }

  @Test
  fun confirmNewGame_clearsGameAndSave() {
    val store = MemoryGameStore(listOf(move("e2", "e4"), move("e7", "e5")))
    val vm = ChessViewModel(store, chooseAiMove = { _, _ -> null })
    vm.resumeSavedGame()
    vm.requestNewGame()
    vm.confirmNewGame()
    assertFalse(vm.uiState.value.askConfirmNewGame)
    assertTrue(vm.uiState.value.moveRows.isEmpty())
    assertTrue(store.load().isEmpty())
  }

  @Test
  fun setAiLevel_persistsAndShowsInUi() {
    val store = MemoryGameStore()
    val vm = ChessViewModel(store, chooseAiMove = { _, _ -> null })
    assertEquals(AiLevel.MEDIUM, vm.uiState.value.aiLevel)
    vm.setAiLevel(AiLevel.HARD)
    assertEquals(AiLevel.HARD, vm.uiState.value.aiLevel)
    assertEquals(AiLevel.HARD, store.loadAiLevel())
  }

  @Test
  fun newGame_keepsAiLevel() {
    val store = MemoryGameStore(listOf(move("e2", "e4"), move("e7", "e5")), AiLevel.EASY)
    val vm = ChessViewModel(store, chooseAiMove = { _, _ -> null })
    vm.resumeSavedGame()
    vm.confirmNewGame()
    assertEquals(AiLevel.EASY, vm.uiState.value.aiLevel)
    assertEquals(AiLevel.EASY, store.loadAiLevel())
  }

  @Test
  fun playing_persistsMoves() = runTest(dispatcher) {
    val store = MemoryGameStore()
    val vm = ChessViewModel(store, chooseAiMove = { _, _ -> move("e7", "e5") }, computeDispatcher = dispatcher)
    vm.onSquareClicked(Square.parse("e2"))
    vm.onSquareClicked(Square.parse("e4"))
    advanceUntilIdle()
    assertEquals(listOf(move("e2", "e4"), move("e7", "e5")), store.load())
  }

  @Test
  fun thinkTimes_accumulateThenClearOnUndoAndNewGame() = runTest(dispatcher) {
    val clock = ArrayDeque(listOf(0L, 800L, 2_400L, 3_000L, 3_300L, 3_900L, 4_000L))
    val store = MemoryGameStore()
    val vm =
      ChessViewModel(
        store,
        chooseAiMove = { _, _ -> move("e7", "e5") },
        computeDispatcher = dispatcher,
        nowMs = { clock.removeFirst() },
      )
    vm.onSquareClicked(Square.parse("e2"))
    vm.onSquareClicked(Square.parse("e4"))
    advanceUntilIdle()
    assertEquals(1_600L, vm.uiState.value.cpuThinkMs)
    assertEquals(800L, vm.uiState.value.playerThinkMs)
    assertEquals(800L, store.loadPlayerMs())
    assertEquals(1_600L, store.loadCpuMs())
    vm.undo()
    assertEquals(0L, vm.uiState.value.cpuThinkMs)
    assertEquals(0L, vm.uiState.value.playerThinkMs)
    vm.onSquareClicked(Square.parse("e2"))
    vm.onSquareClicked(Square.parse("e4"))
    advanceUntilIdle()
    assertEquals(600L, vm.uiState.value.cpuThinkMs)
    assertEquals(300L, vm.uiState.value.playerThinkMs)
    vm.newGame()
    assertEquals(0L, vm.uiState.value.cpuThinkMs)
    assertEquals(0L, vm.uiState.value.playerThinkMs)
    assertEquals(0L, store.loadPlayerMs())
    assertEquals(0L, store.loadCpuMs())
  }

  @Test
  fun resume_restoresThinkTimes() {
    val store =
      MemoryGameStore(
        initial = listOf(move("e2", "e4"), move("e7", "e5")),
        playerMs = 12_000L,
        cpuMs = 9_000L,
      )
    val vm = ChessViewModel(store, chooseAiMove = { _, _ -> null })
    vm.resumeSavedGame()
    assertEquals(12_000L, vm.uiState.value.playerThinkMs)
    assertEquals(9_000L, vm.uiState.value.cpuThinkMs)
  }

  private fun move(from: String, to: String) = Move(Square.parse(from), Square.parse(to))
}

private class MemoryGameStore(
  initial: List<Move> = emptyList(),
  initialLevel: AiLevel = AiLevel.MEDIUM,
  playerMs: Long = 0L,
  cpuMs: Long = 0L,
) : GameStore {
  private var moves = initial.toList()
  private var level = initialLevel
  private var savedPlayerMs = playerMs
  private var savedCpuMs = cpuMs

  override fun load(): List<Move> = moves

  override fun save(moves: List<Move>) {
    this.moves = moves.toList()
  }

  override fun clear() {
    moves = emptyList()
    savedPlayerMs = 0L
    savedCpuMs = 0L
  }

  override fun loadAiLevel(): AiLevel = level

  override fun saveAiLevel(level: AiLevel) {
    this.level = level
  }

  override fun loadPlayerMs(): Long = savedPlayerMs

  override fun loadCpuMs(): Long = savedCpuMs

  override fun saveClocks(playerMs: Long, cpuMs: Long) {
    savedPlayerMs = playerMs
    savedCpuMs = cpuMs
  }
}
