package com.example.chess.engine

enum class AiLevel(
  val label: String,
  val depth: Int,
  val topMoves: Int,
  val quiescence: Boolean,
) {
  EASY(label = "Easy", depth = 2, topMoves = 3, quiescence = false),
  MEDIUM(label = "Medium", depth = 3, topMoves = 1, quiescence = false),
  HARD(label = "Hard", depth = 4, topMoves = 1, quiescence = true),
  ;

  companion object {
    fun fromStorage(raw: String?): AiLevel = entries.find { it.name == raw } ?: MEDIUM
  }
}
