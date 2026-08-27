package com.example.chess.domain

private val STARTING_COUNTS =
  mapOf(
    PieceType.QUEEN to 1,
    PieceType.ROOK to 2,
    PieceType.BISHOP to 2,
    PieceType.KNIGHT to 2,
    PieceType.PAWN to 8,
  )

private val CAPTURE_ORDER = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT, PieceType.PAWN)

/** Pieces of [side] that started the game and are no longer on the board. */
fun capturedOf(side: Side, squares: List<Piece?>): List<Piece> {
  val remaining = squares.filterNotNull().filter { it.side == side }.groupingBy { it.type }.eachCount()
  return CAPTURE_ORDER.flatMap { type ->
    val gone = ((STARTING_COUNTS[type] ?: 0) - (remaining[type] ?: 0)).coerceAtLeast(0)
    List(gone) { Piece(type, side) }
  }
}
