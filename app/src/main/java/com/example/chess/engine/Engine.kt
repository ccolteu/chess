package com.example.chess.engine

import com.example.chess.domain.GameState
import com.example.chess.domain.Move
import com.example.chess.domain.PieceType
import com.example.chess.domain.Rules
import com.example.chess.domain.Side
import com.example.chess.domain.Square
import kotlin.math.max
import kotlin.math.min

object Engine {
  const val DEPTH = 3

  fun chooseMove(state: GameState, depth: Int = DEPTH): Move? {
    val moves = Rules.legalMoves(state)
    if (moves.isEmpty()) return null
    var best = moves.first()
    var bestScore = if (state.sideToMove == Side.WHITE) Int.MIN_VALUE else Int.MAX_VALUE
    for (move in moves) {
      val child = Rules.apply(state, move)
      val score = minimax(child, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE)
      if (state.sideToMove == Side.WHITE && score > bestScore) {
        bestScore = score
        best = move
      } else if (state.sideToMove == Side.BLACK && score < bestScore) {
        bestScore = score
        best = move
      }
    }
    return best
  }

  private fun minimax(state: GameState, depth: Int, alpha0: Int, beta0: Int): Int {
    val moves = Rules.legalMoves(state)
    if (depth == 0 || moves.isEmpty()) return evaluate(state)
    var alpha = alpha0
    var beta = beta0
    if (state.sideToMove == Side.WHITE) {
      var best = Int.MIN_VALUE
      for (move in moves) {
        best = max(best, minimax(Rules.apply(state, move), depth - 1, alpha, beta))
        alpha = max(alpha, best)
        if (beta <= alpha) break
      }
      return best
    } else {
      var best = Int.MAX_VALUE
      for (move in moves) {
        best = min(best, minimax(Rules.apply(state, move), depth - 1, alpha, beta))
        beta = min(beta, best)
        if (beta <= alpha) break
      }
      return best
    }
  }

  private fun evaluate(state: GameState): Int {
    when (state.status) {
      com.example.chess.domain.GameStatus.CHECKMATE ->
        return if (state.sideToMove == Side.WHITE) -30_000 else 30_000
      com.example.chess.domain.GameStatus.STALEMATE -> return 0
      else -> Unit
    }
    var score = 0
    for (i in 0 until 64) {
      val piece = state.squares[i] ?: continue
      val sq = Square.fromIndex(i)
      val pstIndex = if (piece.side == Side.WHITE) i else (7 - sq.rank) * 8 + sq.file
      val value = material(piece.type) + pieceSquare(piece.type, pstIndex)
      score += if (piece.side == Side.WHITE) value else -value
    }
    return score
  }

  private fun material(type: PieceType): Int =
    when (type) {
      PieceType.PAWN -> 100
      PieceType.KNIGHT -> 320
      PieceType.BISHOP -> 330
      PieceType.ROOK -> 500
      PieceType.QUEEN -> 900
      PieceType.KING -> 0
    }

  private fun pieceSquare(type: PieceType, index: Int): Int =
    when (type) {
      PieceType.PAWN -> PAWN_PST[index]
      PieceType.KNIGHT -> KNIGHT_PST[index]
      PieceType.BISHOP -> 0
      PieceType.ROOK -> 0
      PieceType.QUEEN -> 0
      PieceType.KING -> 0
    }

  // White's perspective, rank 1 first (matches Square.index).
  private val PAWN_PST =
    intArrayOf(
      0, 0, 0, 0, 0, 0, 0, 0,
      5, 10, 10, -20, -20, 10, 10, 5,
      5, -5, -10, 0, 0, -10, -5, 5,
      0, 0, 0, 20, 20, 0, 0, 0,
      5, 5, 10, 25, 25, 10, 5, 5,
      10, 10, 20, 30, 30, 20, 10, 10,
      50, 50, 50, 50, 50, 50, 50, 50,
      0, 0, 0, 0, 0, 0, 0, 0,
    )

  private val KNIGHT_PST =
    intArrayOf(
      -50, -40, -30, -30, -30, -30, -40, -50,
      -40, -20, 0, 5, 5, 0, -20, -40,
      -30, 5, 10, 15, 15, 10, 5, -30,
      -30, 0, 15, 20, 20, 15, 0, -30,
      -30, 5, 15, 20, 20, 15, 5, -30,
      -30, 0, 10, 15, 15, 10, 0, -30,
      -40, -20, 0, 0, 0, 0, -20, -40,
      -50, -40, -30, -30, -30, -30, -40, -50,
    )
}
