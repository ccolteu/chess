package com.cc.chess.engine

import com.cc.chess.domain.GameState
import com.cc.chess.domain.GameStatus
import com.cc.chess.domain.Move
import com.cc.chess.domain.PieceType
import com.cc.chess.domain.Rules
import com.cc.chess.domain.Side
import com.cc.chess.domain.Square
import com.cc.chess.domain.isOver
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
    val budget =
      SearchBudget(
        deadlineNanos =
          if (level.thinkMs <= 0L) Long.MAX_VALUE else System.nanoTime() + level.thinkMs * 1_000_000L,
      )
    val white = state.sideToMove == Side.WHITE
    var ordered = moves
    var chosen = moves.first()
    val startDepth = if (level.thinkMs > 0L) 1 else level.depth
    for (depth in startDepth..level.depth) {
      val scored = searchRoot(state, ordered, depth, level, budget)
      val complete = scored.size == ordered.size
      if (complete) {
        chosen = pickMove(scored, white, level, random)
        ordered = scored.map { it.first }
      }
      if (budget.timedOut || !complete) break
    }
    return chosen
  }

  private fun pickMove(
    scored: List<Pair<Move, Int>>,
    white: Boolean,
    level: AiLevel,
    random: Random,
  ): Move {
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

  private fun searchRoot(
    state: GameState,
    moves: List<Move>,
    depth: Int,
    level: AiLevel,
    budget: SearchBudget,
  ): List<Pair<Move, Int>> {
    val white = state.sideToMove == Side.WHITE
    val pruneRoot = level.topMoves <= 1
    var alpha = -INF
    var beta = INF
    val scored = ArrayList<Pair<Move, Int>>(moves.size)
    for (move in moves) {
      if (budget.expired()) break
      val score = minimax(Rules.apply(state, move), depth - 1, alpha, beta, level.quiescence, budget)
      if (budget.timedOut) break
      scored += move to score
      if (pruneRoot) {
        if (white) alpha = max(alpha, score) else beta = min(beta, score)
      }
    }
    return scored
  }

  private fun minimax(
    state: GameState,
    depth: Int,
    alpha0: Int,
    beta0: Int,
    quiescence: Boolean,
    budget: SearchBudget,
  ): Int {
    if (budget.expired()) return evaluate(state)
    val moves = orderedMoves(state, Rules.legalMoves(state))
    if (moves.isEmpty()) return evaluate(state)
    if (depth == 0) {
      return if (quiescence) quiesce(state, alpha0, beta0, 2, budget) else evaluate(state)
    }
    var alpha = alpha0
    var beta = beta0
    if (state.sideToMove == Side.WHITE) {
      var best = -INF
      for (move in moves) {
        if (budget.expired()) break
        best = max(best, minimax(Rules.apply(state, move), depth - 1, alpha, beta, quiescence, budget))
        alpha = max(alpha, best)
        if (beta <= alpha) break
      }
      return best
    }
    var best = INF
    for (move in moves) {
      if (budget.expired()) break
      best = min(best, minimax(Rules.apply(state, move), depth - 1, alpha, beta, quiescence, budget))
      beta = min(beta, best)
      if (beta <= alpha) break
    }
    return best
  }

  private fun quiesce(state: GameState, alpha0: Int, beta0: Int, remain: Int, budget: SearchBudget): Int {
    if (budget.expired() || state.status.isOver()) return evaluate(state)
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
      var best = if (inCheck) -INF else stand
      for (move in candidates) {
        if (budget.expired()) break
        best = max(best, quiesce(Rules.apply(state, move), alpha, beta, remain - 1, budget))
        alpha = max(alpha, best)
        if (beta <= alpha) break
      }
      return best
    }
    var best = if (inCheck) INF else stand
    for (move in candidates) {
      if (budget.expired()) break
      best = min(best, quiesce(Rules.apply(state, move), alpha, beta, remain - 1, budget))
      beta = min(beta, best)
      if (beta <= alpha) break
    }
    return best
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
      GameStatus.STALEMATE,
      GameStatus.DRAW_REPETITION,
      GameStatus.DRAW_FIFTY,
      GameStatus.DRAW_INSUFFICIENT,
      -> return 0
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

private const val INF = 100_000

private class SearchBudget(private val deadlineNanos: Long) {
  var timedOut: Boolean = false
    private set

  fun expired(): Boolean {
    if (timedOut) return true
    if (deadlineNanos != Long.MAX_VALUE && System.nanoTime() >= deadlineNanos) {
      timedOut = true
      return true
    }
    return false
  }
}
