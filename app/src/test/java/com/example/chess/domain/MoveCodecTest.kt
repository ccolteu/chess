package com.example.chess.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MoveCodecTest {
  @Test
  fun roundTripsQuietMovePromotionCastlingAndEnPassant() {
    val moves =
      listOf(
        Move(Square.parse("e2"), Square.parse("e4")),
        Move(Square.parse("a7"), Square.parse("a8"), promotion = PieceType.QUEEN),
        Move(Square.parse("e1"), Square.parse("g1"), isCastling = true),
        Move(Square.parse("e5"), Square.parse("d6"), isEnPassant = true),
      )
    assertEquals(moves, MoveCodec.decodeAll(MoveCodec.encodeAll(moves)))
  }

  @Test
  fun emptyString_isEmptyList() {
    assertEquals(emptyList<Move>(), MoveCodec.decodeAll(""))
  }
}
