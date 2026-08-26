package com.example.chess.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameHistoryTest {
  @Test
  fun start_cannotUndoAndHasNoRows() {
    val history = GameHistory()
    assertFalse(history.canUndoTurn(aiThinking = false))
    assertTrue(history.rows.isEmpty())
  }

  @Test
  fun formatMove_usesCoordinatesAndCastling() {
    assertEquals("e2-e4", formatMove(Move(Square.parse("e2"), Square.parse("e4"))))
    assertEquals("O-O", formatMove(Move(Square.parse("e1"), Square.parse("g1"), isCastling = true)))
    assertEquals("O-O-O", formatMove(Move(Square.parse("e1"), Square.parse("c1"), isCastling = true)))
    assertEquals(
      "a7-a8=Q",
      formatMove(Move(Square.parse("a7"), Square.parse("a8"), promotion = PieceType.QUEEN)),
    )
  }

  @Test
  fun afterWhiteMove_rowHasOnlyWhiteAndUndoDisabledWhileAiThinks() {
    val history = GameHistory()
    history.apply(move("e2", "e4"))
    assertEquals(listOf(MoveRow(1, "e2-e4", null)), history.rows)
    assertFalse(history.canUndoTurn(aiThinking = true))
  }

  @Test
  fun afterFullTurn_pairsMovesAndUndoRestoresStart() {
    val history = GameHistory()
    history.apply(move("e2", "e4"))
    history.apply(move("e7", "e5"))
    assertEquals(listOf(MoveRow(1, "e2-e4", "e7-e5")), history.rows)
    assertTrue(history.canUndoTurn(aiThinking = false))

    history.undoTurn()
    assertEquals(startingGame().squares, history.current.squares)
    assertEquals(Side.WHITE, history.current.sideToMove)
    assertTrue(history.rows.isEmpty())
    assertFalse(history.canUndoTurn(aiThinking = false))
  }

  @Test
  fun afterWhiteCheckmate_undoTakesBackMatingMove() {
    val history = GameHistory()
    listOf(
        move("e2", "e4"),
        move("e7", "e5"),
        move("d1", "h5"),
        move("b8", "c6"),
        move("f1", "c4"),
        move("g8", "f6"),
        move("h5", "f7"),
      )
      .forEach { history.apply(it) }
    assertEquals(GameStatus.CHECKMATE, history.current.status)
    assertTrue(history.canUndoTurn(aiThinking = false))

    history.undoTurn()
    assertEquals(GameStatus.IN_PROGRESS, history.current.status)
    assertEquals(Side.WHITE, history.current.sideToMove)
    assertEquals(6, history.rows.flatMap { listOfNotNull(it.white, it.black) }.size)
  }

  @Test
  fun replaceWith_replaysMovesAndExposesThem() {
    val history = GameHistory()
    val plies = listOf(move("e2", "e4"), move("e7", "e5"))
    history.replaceWith(plies)
    assertEquals(plies, history.moves)
    assertEquals(Side.WHITE, history.current.sideToMove)
    assertEquals(listOf(MoveRow(1, "e2-e4", "e7-e5")), history.rows)
    assertTrue(history.isResumable)
  }

  @Test
  fun finishedGame_isNotResumable() {
    val history = GameHistory()
    history.replaceWith(
      listOf(
        move("e2", "e4"),
        move("e7", "e5"),
        move("d1", "h5"),
        move("b8", "c6"),
        move("f1", "c4"),
        move("g8", "f6"),
        move("h5", "f7"),
      )
    )
    assertFalse(history.isResumable)
  }

  private fun move(from: String, to: String) = Move(Square.parse(from), Square.parse(to))
}
