package com.example.chess.engine

import com.example.chess.domain.Fen
import com.example.chess.domain.Rules
import com.example.chess.domain.Square
import com.example.chess.domain.startingGame
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
  @Test
  fun easy_startPosition_returnsLegalMove() {
    val state = startingGame()
    val move = Engine.chooseMove(state, AiLevel.EASY, Random(0))
    assertNotNull(move)
    assertTrue(Rules.legalMoves(state).contains(move))
  }

  @Test
  fun medium_startPosition_returnsLegalMove() {
    val state = startingGame()
    val move = Engine.chooseMove(state, AiLevel.MEDIUM)
    assertNotNull(move)
    assertTrue(Rules.legalMoves(state).contains(move))
  }

  @Test
  fun hard_takesHangingQueen() {
    val state = Rules.withStatus(Fen.parse("4k3/8/2n5/8/3Q4/8/8/4K3 b - - 0 1"))
    val move = Engine.chooseMove(state, AiLevel.HARD)
    assertNotNull(move)
    assertEquals(Square.parse("c6"), move!!.from)
    assertEquals(Square.parse("d4"), move.to)
  }
}
