package com.example.chess.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
  @Test
  fun startingPosition_whiteHas20Moves() {
    val state = startingGame()
    assertEquals(20, Rules.legalMoves(state).size)
  }

  @Test
  fun e4_thenBlackHas20Moves() {
    val afterE4 = Rules.apply(startingGame(), move("e2", "e4"))
    assertEquals(Side.BLACK, afterE4.sideToMove)
    assertEquals(20, Rules.legalMoves(afterE4).size)
  }

  @Test
  fun pinnedQueen_cannotLeaveFile() {
    val state = Fen.parse("4q3/8/8/8/8/8/4Q3/4K3 w - - 0 1")
    val moves = Rules.legalMoves(state).filter { it.from == Square.parse("e2") }
    assertTrue(moves.isNotEmpty())
    assertTrue(moves.all { it.to.file == 4 })
  }

  @Test
  fun castling_kingsideWhenClear() {
    val state = Fen.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
    val castle = Rules.legalMoves(state).firstOrNull { it.from == Square.parse("e1") && it.to == Square.parse("g1") }
    assertTrue(castle != null && castle.isCastling)
  }

  @Test
  fun cannotCastle_throughCheck() {
    val state = Fen.parse("4k3/8/8/8/8/8/4r3/R3K2R w KQ - 0 1")
    val moves = Rules.legalMoves(state)
    assertFalse(moves.any { it.to == Square.parse("g1") && it.isCastling })
  }

  @Test
  fun enPassant_onlyImmediately() {
    var state = Fen.parse("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1")
    val ep = Rules.legalMoves(state).firstOrNull { it.isEnPassant }
    assertTrue(ep != null)
    assertEquals(Square.parse("d6"), ep!!.to)
    state = Rules.apply(state, ep)
    assertEquals(null, state.pieceAt(Square.parse("d5")))
  }

  @Test
  fun promotion_requiresChoice() {
    val state = Fen.parse("4k3/P7/8/8/8/8/8/4K3 w - - 0 1")
    val promotions = Rules.legalMoves(state).filter { it.from == Square.parse("a7") }
    assertEquals(setOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT), promotions.map { it.promotion }.toSet())
  }

  @Test
  fun scholarsMate_isCheckmate() {
    var state = startingGame()
    state = Rules.apply(state, move("e2", "e4"))
    state = Rules.apply(state, move("e7", "e5"))
    state = Rules.apply(state, move("d1", "h5"))
    state = Rules.apply(state, move("b8", "c6"))
    state = Rules.apply(state, move("f1", "c4"))
    state = Rules.apply(state, move("g8", "f6"))
    state = Rules.apply(state, move("h5", "f7"))
    assertEquals(GameStatus.CHECKMATE, state.status)
    assertTrue(Rules.legalMoves(state).isEmpty())
  }

  @Test
  fun stalemate_hasNoLegalMoves() {
    val state = Rules.withStatus(Fen.parse("7k/5Q2/8/8/8/8/8/4K3 b - - 0 1"))
    assertEquals(GameStatus.STALEMATE, state.status)
    assertTrue(Rules.legalMoves(state).isEmpty())
  }

  private fun move(from: String, to: String) = Move(Square.parse(from), Square.parse(to))
}
