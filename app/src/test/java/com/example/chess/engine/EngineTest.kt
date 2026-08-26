package com.example.chess.engine

import com.example.chess.domain.Rules
import com.example.chess.domain.startingGame
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {
  @Test
  fun startPosition_returnsLegalMove() {
    val state = startingGame()
    val move = Engine.chooseMove(state, depth = 2)
    assertNotNull(move)
    assertTrue(Rules.legalMoves(state).contains(move))
  }
}
