package com.example.chess.domain

object MoveCodec {
  fun encodeAll(moves: List<Move>): String = moves.joinToString(";") { encode(it) }

  fun decodeAll(raw: String): List<Move> {
    if (raw.isBlank()) return emptyList()
    return raw.split(";").map { decode(it) }
  }

  private fun encode(move: Move): String =
    listOf(
        move.from.toString(),
        move.to.toString(),
        move.promotion?.name.orEmpty(),
        if (move.isCastling) "C" else "",
        if (move.isEnPassant) "E" else "",
      )
      .joinToString(",")

  private fun decode(token: String): Move {
    val parts = token.split(",")
    return Move(
      from = Square.parse(parts[0]),
      to = Square.parse(parts[1]),
      promotion = parts.getOrNull(2)?.takeIf { it.isNotEmpty() }?.let { PieceType.valueOf(it) },
      isCastling = parts.getOrNull(3) == "C",
      isEnPassant = parts.getOrNull(4) == "E",
    )
  }
}
