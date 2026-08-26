package com.example.chess.domain

data class MoveRow(val number: Int, val white: String, val black: String?)

class GameHistory(initial: GameState = startingGame()) {
  private val positions = mutableListOf(Rules.withStatus(initial))
  private val notations = mutableListOf<String>()
  private val applied = mutableListOf<Move>()

  val current: GameState
    get() = positions.last()

  val lastMove: Move?
    get() = applied.lastOrNull()

  val moves: List<Move>
    get() = applied.toList()

  val isResumable: Boolean
    get() = applied.isNotEmpty() && !isOver(current)

  val rows: List<MoveRow>
    get() =
      notations.chunked(2).mapIndexed { index, pair ->
        MoveRow(number = index + 1, white = pair[0], black = pair.getOrNull(1))
      }

  fun canUndoTurn(aiThinking: Boolean): Boolean {
    if (aiThinking || notations.isEmpty()) return false
    return notations.size % 2 == 0 || isOver(current)
  }

  fun apply(move: Move): GameState {
    val next = Rules.apply(current, move)
    notations += formatMove(move)
    applied += move
    positions += next
    return next
  }

  fun undoTurn(): GameState {
    val count = if (notations.size % 2 == 0) 2 else 1
    repeat(count) {
      notations.removeAt(notations.lastIndex)
      applied.removeAt(applied.lastIndex)
      positions.removeAt(positions.lastIndex)
    }
    return current
  }

  fun reset() {
    val start = Rules.withStatus(startingGame())
    positions.clear()
    positions += start
    notations.clear()
    applied.clear()
  }

  fun replaceWith(moves: List<Move>) {
    reset()
    moves.forEach { apply(it) }
  }

  private fun isOver(state: GameState): Boolean =
    state.status == GameStatus.CHECKMATE || state.status == GameStatus.STALEMATE
}

fun formatMove(move: Move): String {
  if (move.isCastling) return if (move.to.file == 6) "O-O" else "O-O-O"
  val promo =
    when (move.promotion) {
      PieceType.QUEEN -> "=Q"
      PieceType.ROOK -> "=R"
      PieceType.BISHOP -> "=B"
      PieceType.KNIGHT -> "=N"
      null -> ""
      else -> ""
    }
  return "${move.from}-${move.to}$promo"
}
