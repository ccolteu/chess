package com.example.chess.data

import android.content.Context
import com.example.chess.domain.Move
import com.example.chess.domain.MoveCodec
import com.example.chess.engine.AiLevel

interface GameStore {
  fun load(): List<Move>

  fun save(moves: List<Move>)

  fun clear()

  fun loadAiLevel(): AiLevel

  fun saveAiLevel(level: AiLevel)
}

class PrefsGameStore(context: Context) : GameStore {
  private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

  override fun load(): List<Move> = MoveCodec.decodeAll(prefs.getString(KEY, "").orEmpty())

  override fun save(moves: List<Move>) {
    if (moves.isEmpty()) {
      prefs.edit().remove(KEY).apply()
    } else {
      prefs.edit().putString(KEY, MoveCodec.encodeAll(moves)).apply()
    }
  }

  override fun clear() {
    prefs.edit().remove(KEY).apply()
  }

  override fun loadAiLevel(): AiLevel = AiLevel.fromStorage(prefs.getString(KEY_LEVEL, null))

  override fun saveAiLevel(level: AiLevel) {
    prefs.edit().putString(KEY_LEVEL, level.name).apply()
  }

  private companion object {
    const val PREFS = "chess_game"
    const val KEY = "saved_moves"
    const val KEY_LEVEL = "ai_level"
  }
}
