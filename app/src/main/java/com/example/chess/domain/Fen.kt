package com.example.chess.domain

object Fen {
  const val START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

  fun parse(fen: String): GameState {
    val parts = fen.trim().split(Regex("\\s+"))
    require(parts.size >= 4) { "Bad FEN: $fen" }
    val board = MutableList<Piece?>(64) { null }
    val ranks = parts[0].split("/")
    require(ranks.size == 8)
    ranks.forEachIndexed { rankFromTop, row ->
      val rank = 7 - rankFromTop
      var file = 0
      for (ch in row) {
        when {
          ch.isDigit() -> file += ch.digitToInt()
          else -> {
            board[Square(file, rank).index] = pieceFromFen(ch)
            file += 1
          }
        }
      }
      require(file == 8) { "Bad FEN rank: $row" }
    }
    val side = if (parts[1] == "w") Side.WHITE else Side.BLACK
    val cr = parts[2]
    val castling =
      CastlingRights(
        whiteKing = 'K' in cr,
        whiteQueen = 'Q' in cr,
        blackKing = 'k' in cr,
        blackQueen = 'q' in cr,
      )
    val ep = if (parts[3] == "-") null else Square.parse(parts[3])
    val half = parts.getOrNull(4)?.toInt() ?: 0
    val full = parts.getOrNull(5)?.toInt() ?: 1
    return GameState(board, side, castling, ep, half, full, GameStatus.IN_PROGRESS)
  }

  private fun pieceFromFen(ch: Char): Piece {
    val side = if (ch.isUpperCase()) Side.WHITE else Side.BLACK
    val type =
      when (ch.lowercaseChar()) {
        'k' -> PieceType.KING
        'q' -> PieceType.QUEEN
        'r' -> PieceType.ROOK
        'b' -> PieceType.BISHOP
        'n' -> PieceType.KNIGHT
        'p' -> PieceType.PAWN
        else -> error("Bad FEN piece $ch")
      }
    return Piece(type, side)
  }
}
