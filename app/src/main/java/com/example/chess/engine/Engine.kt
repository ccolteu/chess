package com.example.chess.engine

import com.example.chess.domain.GameState
import com.example.chess.domain.GameStatus
import com.example.chess.domain.Move
import com.example.chess.domain.PieceType
import com.example.chess.domain.Rules
import com.example.chess.domain.Side
import com.example.chess.domain.Square
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object Engine {
  fun chooseMove(
    state: GameState,
    level: AiLevel = AiLevel.MEDIUM,
    random: Random = Random.Default,
  ): Move? {
    val moves = orderedMoves(state, Rules.legalMoves(state))
    if (moves.isEmpty()) return null
    val white = state.sideToMove == Side.WHITE
    val scored =
      moves.map { move ->
        val child = Rules.apply(state, move)
        move to minimax(child, level.depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, level.quiescence)
      }
    val sorted = if (white) scored.sortedByDescending { it.second } else scored.sortedBy { it.second }
    val bestScore = sorted.first().second
    val window =
      if (level.topMoves <= 1) {
        listOf(sorted.first())
      } else {
        sorted.filter { kotlin.math.abs(it.second - bestScore) <= 80 }.take(level.topMoves)
      }
    return window[random.nextInt(window.size)].first
  }

  private fun minimax(state: GameState, depth: Int, alpha0: Int, beta0: Int, quiescence: Boolean): Int {
    val moves = orderedMoves(state, Rules.legalMoves(state))
    if (moves.isEmpty()) return evaluate(state)
    if (depth == 0) {
      return if (quiescence) quiesce(state, alpha0, beta0, 4) else evaluate(state)
    }
    var alpha = alpha0
    var beta = beta0
    if (state.sideToMove == Side.WHITE) {
      var best = Int.MIN_VALUE
      for (move in moves) {
        best = max(best, minimax(Rules.apply(state, move), depth - 1, alpha, beta, quiescence))
        alpha = max(alpha, best)
        if (beta <= alpha) break
      }
      return best
    } else {
      var best = Int.MAX_VALUE
      for (move in moves) {
        best = min(best, minimax(Rules.apply(state, move), depth - 1, alpha, beta, quiescence))
        beta = min(beta, best)
        if (beta <= alpha) break
      }
      return best
    }
  }

  private fun quiesce(state: GameState, alpha0: Int, beta0: Int, remain: Int): Int {
    if (state.status == GameStatus.CHECKMATE || state.status == GameStatus.STALEMATE) return evaluate(state)
    val legal = Rules.legalMoves(state)
    if (legal.isEmpty()) return evaluate(state)
    if (remain == 0) return evaluate(state)

    val inCheck = state.status == GameStatus.CHECK
    val stand = evaluate(state)
    var alpha = alpha0
    var beta = beta0
    val candidates =
      if (inCheck) {
        orderedMoves(state, legal)
      } else {
        orderedMoves(state, legal.filter { isCapture(state, it) })
      }
    if (!inCheck) {
      if (state.sideToMove == Side.WHITE) {
        if (stand >= beta) return stand
        alpha = max(alpha, stand)
      } else {
        if (stand <= alpha) return stand
        beta = min(beta, stand)
      }
    }
    if (candidates.isEmpty()) return stand

    if (state.sideToMove == Side.WHITE) {
      var best = if (inCheck) Int.MIN_VALUE else stand
      for (move in candidates) {
        best = max(best, quiesce(Rules.apply(state, move), alpha, beta, remain - 1))
        alpha = max(alpha, best)
        if (beta <= alpha) break
      }
      return best
    } else {
      var best = if (inCheck) Int.MAX_VALUE else stand
      for (move in candidates) {
        best = min(best, quiesce(Rules.apply(state, move), alpha, beta, remain - 1))
        beta = min(beta, best)
        if (beta <= alpha) break
      }
      return best
    }
  }

  private fun orderedMoves(state: GameState, moves: List<Move>): List<Move> =
    moves.sortedByDescending { captureScore(state, it) }

  private fun isCapture(state: GameState, move: Move): Boolean =
    move.isEnPassant || state.pieceAt(move.to) != null

  private fun captureScore(state: GameState, move: Move): Int {
    val victimType =
      when {
        move.isEnPassant -> PieceType.PAWN
        else -> state.pieceAt(move.to)?.type ?: return -1
      }
    val attacker = state.pieceAt(move.from)?.type ?: return 0
    return material(victimType) * 16 - material(attacker)
  }

  private fun evaluate(state: GameState): Int {
    when (state.status) {
      GameStatus.CHECKMATE ->
        return if (state.sideToMove == Side.WHITE) -30_000 else 30_000
      GameStatus.STALEMATE -> return 0
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
