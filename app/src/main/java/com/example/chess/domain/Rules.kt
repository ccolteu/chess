package com.example.chess.domain

object Rules {
  fun legalMoves(state: GameState): List<Move> {
    if (state.status == GameStatus.CHECKMATE || state.status == GameStatus.STALEMATE) return emptyList()
    return pseudoLegalMoves(state).filter { move -> !leavesKingInCheck(state, move) }
  }

  fun apply(state: GameState, move: Move): GameState {
    val next = applyUnchecked(state, move)
    return withStatus(next)
  }

  fun withStatus(state: GameState): GameState {
    val legal = pseudoLegalMoves(state).filter { move -> !leavesKingInCheck(state, move) }
    val inCheck = isSquareAttacked(state.squares, kingSquare(state.squares, state.sideToMove), state.sideToMove.opposite())
    val status =
      when {
        legal.isEmpty() && inCheck -> GameStatus.CHECKMATE
        legal.isEmpty() -> GameStatus.STALEMATE
        inCheck -> GameStatus.CHECK
        else -> GameStatus.IN_PROGRESS
      }
    return state.copy(status = status)
  }

  private fun applyUnchecked(state: GameState, move: Move): GameState {
    val board = state.squares.toMutableList()
    val moving = board[move.from.index] ?: error("No piece on ${move.from}")
    var captured = board[move.to.index]
    board[move.from.index] = null

    if (move.isEnPassant) {
      val capRank = if (moving.side == Side.WHITE) move.to.rank - 1 else move.to.rank + 1
      val capSq = Square(move.to.file, capRank)
      captured = board[capSq.index]
      board[capSq.index] = null
    }

    if (move.isCastling) {
      board[move.to.index] = moving
      if (move.to.file == 6) {
        val rookFrom = Square(7, move.from.rank)
        val rookTo = Square(5, move.from.rank)
        board[rookTo.index] = board[rookFrom.index]
        board[rookFrom.index] = null
      } else {
        val rookFrom = Square(0, move.from.rank)
        val rookTo = Square(3, move.from.rank)
        board[rookTo.index] = board[rookFrom.index]
        board[rookFrom.index] = null
      }
    } else {
      val placed =
        if (move.promotion != null) Piece(move.promotion, moving.side) else moving
      board[move.to.index] = placed
    }

    val castling = updateCastling(state.castling, move, moving)
    val ep =
      if (moving.type == PieceType.PAWN && kotlin.math.abs(move.to.rank - move.from.rank) == 2) {
        Square(move.from.file, (move.from.rank + move.to.rank) / 2)
      } else {
        null
      }
    val resetClock = moving.type == PieceType.PAWN || captured != null
    val half = if (resetClock) 0 else state.halfmoveClock + 1
    val full = if (state.sideToMove == Side.BLACK) state.fullmoveNumber + 1 else state.fullmoveNumber
    return GameState(
      squares = board,
      sideToMove = state.sideToMove.opposite(),
      castling = castling,
      enPassant = ep,
      halfmoveClock = half,
      fullmoveNumber = full,
      status = GameStatus.IN_PROGRESS,
    )
  }

  private fun updateCastling(rights: CastlingRights, move: Move, moving: Piece): CastlingRights {
    var r = rights
    if (moving.type == PieceType.KING && moving.side == Side.WHITE) {
      r = r.copy(whiteKing = false, whiteQueen = false)
    }
    if (moving.type == PieceType.KING && moving.side == Side.BLACK) {
      r = r.copy(blackKing = false, blackQueen = false)
    }
    fun loseRook(square: Square) {
      when (square) {
        Square(0, 0) -> r = r.copy(whiteQueen = false)
        Square(7, 0) -> r = r.copy(whiteKing = false)
        Square(0, 7) -> r = r.copy(blackQueen = false)
        Square(7, 7) -> r = r.copy(blackKing = false)
      }
    }
    loseRook(move.from)
    loseRook(move.to)
    return r
  }

  private fun leavesKingInCheck(state: GameState, move: Move): Boolean {
    val next = applyUnchecked(state, move)
    val king = kingSquare(next.squares, state.sideToMove)
    return isSquareAttacked(next.squares, king, state.sideToMove.opposite())
  }

  private fun kingSquare(board: List<Piece?>, side: Side): Square {
    val idx = board.indexOfFirst { it?.type == PieceType.KING && it.side == side }
    require(idx >= 0) { "Missing $side king" }
    return Square.fromIndex(idx)
  }

  private fun pseudoLegalMoves(state: GameState): List<Move> {
    val moves = mutableListOf<Move>()
    for (i in 0 until 64) {
      val piece = state.squares[i] ?: continue
      if (piece.side != state.sideToMove) continue
      val from = Square.fromIndex(i)
      when (piece.type) {
        PieceType.PAWN -> addPawnMoves(state, from, piece, moves)
        PieceType.KNIGHT -> addKnightMoves(state, from, piece, moves)
        PieceType.BISHOP -> addSlideMoves(state, from, piece, moves, BISHOP_DIRS)
        PieceType.ROOK -> addSlideMoves(state, from, piece, moves, ROOK_DIRS)
        PieceType.QUEEN -> addSlideMoves(state, from, piece, moves, QUEEN_DIRS)
        PieceType.KING -> addKingMoves(state, from, piece, moves)
      }
    }
    return moves
  }

  private fun addPawnMoves(state: GameState, from: Square, piece: Piece, moves: MutableList<Move>) {
    val dir = if (piece.side == Side.WHITE) 1 else -1
    val startRank = if (piece.side == Side.WHITE) 1 else 6
    val promoRank = if (piece.side == Side.WHITE) 7 else 0
    val one = from.offset(0, dir)
    if (one != null && state.pieceAt(one) == null) {
      addPawnArrival(from, one, promoRank, moves)
      if (from.rank == startRank) {
        val two = from.offset(0, dir * 2)
        if (two != null && state.pieceAt(two) == null) {
          moves += Move(from, two)
        }
      }
    }
    for (df in intArrayOf(-1, 1)) {
      val cap = from.offset(df, dir) ?: continue
      val target = state.pieceAt(cap)
      if (target != null && target.side != piece.side) {
        addPawnArrival(from, cap, promoRank, moves)
      } else if (cap == state.enPassant) {
        moves += Move(from, cap, isEnPassant = true)
      }
    }
  }

  private fun addPawnArrival(from: Square, to: Square, promoRank: Int, moves: MutableList<Move>) {
    if (to.rank == promoRank) {
      for (promo in PROMOTIONS) {
        moves += Move(from, to, promotion = promo)
      }
    } else {
      moves += Move(from, to)
    }
  }

  private fun addKnightMoves(state: GameState, from: Square, piece: Piece, moves: MutableList<Move>) {
    for ((df, dr) in KNIGHT_DIRS) {
      val to = from.offset(df, dr) ?: continue
      val target = state.pieceAt(to)
      if (target == null || target.side != piece.side) {
        moves += Move(from, to)
      }
    }
  }

  private fun addSlideMoves(
    state: GameState,
    from: Square,
    piece: Piece,
    moves: MutableList<Move>,
    dirs: Array<IntArray>,
  ) {
    for ((df, dr) in dirs) {
      var f = from.file + df
      var r = from.rank + dr
      while (f in 0..7 && r in 0..7) {
        val to = Square(f, r)
        val target = state.pieceAt(to)
        if (target == null) {
          moves += Move(from, to)
        } else {
          if (target.side != piece.side) moves += Move(from, to)
          break
        }
        f += df
        r += dr
      }
    }
  }

  private fun addKingMoves(state: GameState, from: Square, piece: Piece, moves: MutableList<Move>) {
    for (df in -1..1) {
      for (dr in -1..1) {
        if (df == 0 && dr == 0) continue
        val to = from.offset(df, dr) ?: continue
        val target = state.pieceAt(to)
        if (target == null || target.side != piece.side) {
          moves += Move(from, to)
        }
      }
    }
    addCastling(state, from, piece, moves)
  }

  private fun addCastling(state: GameState, from: Square, piece: Piece, moves: MutableList<Move>) {
    val rank = if (piece.side == Side.WHITE) 0 else 7
    if (from != Square(4, rank)) return
    val enemy = piece.side.opposite()
    if (isSquareAttacked(state.squares, from, enemy)) return
    val canKing = if (piece.side == Side.WHITE) state.castling.whiteKing else state.castling.blackKing
    val canQueen = if (piece.side == Side.WHITE) state.castling.whiteQueen else state.castling.blackQueen
    if (canKing && state.pieceAt(Square(5, rank)) == null && state.pieceAt(Square(6, rank)) == null) {
      if (!isSquareAttacked(state.squares, Square(5, rank), enemy) &&
        !isSquareAttacked(state.squares, Square(6, rank), enemy)
      ) {
        moves += Move(from, Square(6, rank), isCastling = true)
      }
    }
    if (canQueen &&
      state.pieceAt(Square(3, rank)) == null &&
      state.pieceAt(Square(2, rank)) == null &&
      state.pieceAt(Square(1, rank)) == null
    ) {
      if (!isSquareAttacked(state.squares, Square(3, rank), enemy) &&
        !isSquareAttacked(state.squares, Square(2, rank), enemy)
      ) {
        moves += Move(from, Square(2, rank), isCastling = true)
      }
    }
  }

  private fun isSquareAttacked(board: List<Piece?>, square: Square, by: Side): Boolean {
    val pawnDir = if (by == Side.WHITE) -1 else 1
    for (df in intArrayOf(-1, 1)) {
      val pawnSq = square.offset(df, pawnDir)
      if (pawnSq != null) {
        val p = board[pawnSq.index]
        if (p?.type == PieceType.PAWN && p.side == by) return true
      }
    }
    for ((df, dr) in KNIGHT_DIRS) {
      val n = square.offset(df, dr) ?: continue
      val p = board[n.index]
      if (p?.type == PieceType.KNIGHT && p.side == by) return true
    }
    if (slideHits(board, square, by, BISHOP_DIRS, setOf(PieceType.BISHOP, PieceType.QUEEN))) return true
    if (slideHits(board, square, by, ROOK_DIRS, setOf(PieceType.ROOK, PieceType.QUEEN))) return true
    for (df in -1..1) {
      for (dr in -1..1) {
        if (df == 0 && dr == 0) continue
        val k = square.offset(df, dr) ?: continue
        val p = board[k.index]
        if (p?.type == PieceType.KING && p.side == by) return true
      }
    }
    return false
  }

  private fun slideHits(
    board: List<Piece?>,
    from: Square,
    by: Side,
    dirs: Array<IntArray>,
    types: Set<PieceType>,
  ): Boolean {
    for ((df, dr) in dirs) {
      var f = from.file + df
      var r = from.rank + dr
      while (f in 0..7 && r in 0..7) {
        val p = board[Square(f, r).index]
        if (p != null) {
          if (p.side == by && p.type in types) return true
          break
        }
        f += df
        r += dr
      }
    }
    return false
  }

  private fun Square.offset(df: Int, dr: Int): Square? {
    val f = file + df
    val r = rank + dr
    return if (f in 0..7 && r in 0..7) Square(f, r) else null
  }

  private val KNIGHT_DIRS =
    arrayOf(1 to 2, 2 to 1, 2 to -1, 1 to -2, -1 to -2, -2 to -1, -2 to 1, -1 to 2)
  private val BISHOP_DIRS = arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1))
  private val ROOK_DIRS = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
  private val QUEEN_DIRS = BISHOP_DIRS + ROOK_DIRS
  private val PROMOTIONS = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
}
