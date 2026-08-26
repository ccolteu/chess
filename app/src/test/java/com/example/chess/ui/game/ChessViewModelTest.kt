package com.example.chess.ui.game

import com.example.chess.data.GameStore
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
    val vm = ChessViewModel(store, chooseAiMove = { null })
    assertTrue(vm.uiState.value.askResume)
    assertTrue(vm.uiState.value.moveRows.isEmpty())
  }

  @Test
  fun resume_restoresMoves() {
    val store = MemoryGameStore(listOf(move("e2", "e4"), move("e7", "e5")))
    val vm = ChessViewModel(store, chooseAiMove = { null })
    vm.resumeSavedGame()
    assertFalse(vm.uiState.value.askResume)
    assertEquals(1, vm.uiState.value.moveRows.size)
    assertEquals("e2-e4", vm.uiState.value.moveRows[0].white)
    assertEquals("e7-e5", vm.uiState.value.moveRows[0].black)
  }

  @Test
  fun decline_clearsSaveAndStartsFresh() {
    val store = MemoryGameStore(listOf(move("e2", "e4"), move("e7", "e5")))
    val vm = ChessViewModel(store, chooseAiMove = { null })
    vm.startNewGameFromPrompt()
    assertFalse(vm.uiState.value.askResume)
    assertTrue(vm.uiState.value.moveRows.isEmpty())
    assertTrue(store.load().isEmpty())
  }

  @Test
  fun requestNewGame_onEmptyBoard_doesNotAsk() {
    val vm = ChessViewModel(MemoryGameStore(), chooseAiMove = { null })
    vm.requestNewGame()
    assertFalse(vm.uiState.value.askConfirmNewGame)
    assertTrue(vm.uiState.value.moveRows.isEmpty())
  }

  @Test
  fun requestNewGame_withMoves_asksAndKeepsPosition() {
    val store = MemoryGameStore(listOf(move("e2", "e4"), move("e7", "e5")))
    val vm = ChessViewModel(store, chooseAiMove = { null })
    vm.resumeSavedGame()
    vm.requestNewGame()
    assertTrue(vm.uiState.value.askConfirmNewGame)
    assertEquals(1, vm.uiState.value.moveRows.size)
    assertEquals(listOf(move("e2", "e4"), move("e7", "e5")), store.load())
  }

  @Test
  fun dismissNewGameConfirm_keepsGame() {
    val store = MemoryGameStore(listOf(move("e2", "e4"), move("e7", "e5")))
    val vm = ChessViewModel(store, chooseAiMove = { null })
    vm.resumeSavedGame()
    vm.requestNewGame()
    vm.dismissNewGameConfirm()
    assertFalse(vm.uiState.value.askConfirmNewGame)
    assertEquals(1, vm.uiState.value.moveRows.size)
  }

  @Test
  fun confirmNewGame_clearsGameAndSave() {
    val store = MemoryGameStore(listOf(move("e2", "e4"), move("e7", "e5")))
    val vm = ChessViewModel(store, chooseAiMove = { null })
    vm.resumeSavedGame()
    vm.requestNewGame()
    vm.confirmNewGame()
    assertFalse(vm.uiState.value.askConfirmNewGame)
    assertTrue(vm.uiState.value.moveRows.isEmpty())
    assertTrue(store.load().isEmpty())
  }

  @Test
  fun playing_persistsMoves() = runTest(dispatcher) {
    val store = MemoryGameStore()
    val vm = ChessViewModel(store, chooseAiMove = { move("e7", "e5") }, computeDispatcher = dispatcher)
    vm.onSquareClicked(Square.parse("e2"))
    vm.onSquareClicked(Square.parse("e4"))
    advanceUntilIdle()
    assertEquals(listOf(move("e2", "e4"), move("e7", "e5")), store.load())
  }

  private fun move(from: String, to: String) = Move(Square.parse(from), Square.parse(to))
}

private class MemoryGameStore(initial: List<Move> = emptyList()) : GameStore {
  private var moves = initial.toList()

  override fun load(): List<Move> = moves

  override fun save(moves: List<Move>) {
    this.moves = moves.toList()
  }

  override fun clear() {
    moves = emptyList()
  }
}
