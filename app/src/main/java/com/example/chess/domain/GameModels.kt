package com.example.chess.domain

enum class Side {
  WHITE,
  BLACK,
}

fun Side.opposite(): Side = if (this == Side.WHITE) Side.BLACK else Side.WHITE

enum class PieceType {
  KING,
  QUEEN,
  ROOK,
  BISHOP,
  KNIGHT,
  PAWN,
}

data class Piece(val type: PieceType, val side: Side)

data class Square(val file: Int, val rank: Int) {
  init {
    require(file in 0..7 && rank in 0..7) { "Invalid square $file,$rank" }
  }

  val index: Int
    get() = rank * 8 + file

  override fun toString(): String = "${'a' + file}${rank + 1}"

  companion object {
    fun fromIndex(index: Int): Square = Square(index % 8, index / 8)

    fun parse(algebraic: String): Square {
      require(algebraic.length == 2)
      return Square(algebraic[0] - 'a', algebraic[1] - '1')
    }
  }
}

data class Move(
  val from: Square,
  val to: Square,
  val promotion: PieceType? = null,
  val isCastling: Boolean = false,
  val isEnPassant: Boolean = false,
)

data class CastlingRights(
  val whiteKing: Boolean = true,
  val whiteQueen: Boolean = true,
  val blackKing: Boolean = true,
  val blackQueen: Boolean = true,
)

enum class GameStatus {
  IN_PROGRESS,
  CHECK,
  CHECKMATE,
  STALEMATE,
}

data class GameState(
  val squares: List<Piece?>,
  val sideToMove: Side,
  val castling: CastlingRights,
  val enPassant: Square?,
  val halfmoveClock: Int,
  val fullmoveNumber: Int,
  val status: GameStatus,
) {
  init {
    require(squares.size == 64)
  }

  fun pieceAt(square: Square): Piece? = squares[square.index]
}

fun startingGame(): GameState = Fen.parse(Fen.START)
