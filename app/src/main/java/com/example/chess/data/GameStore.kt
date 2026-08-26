package com.example.chess.data

import android.content.Context
import com.example.chess.domain.Move
import com.example.chess.domain.MoveCodec

interface GameStore {
  fun load(): List<Move>

  fun save(moves: List<Move>)

  fun clear()
}

class PrefsGameStore(context: Context) : GameStore {
  private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

  override fun load(): List<Move> = MoveCodec.decodeAll(prefs.getString(KEY, "").orEmpty())

  override fun save(moves: List<Move>) {
    if (moves.isEmpty()) {
      clear()
    } else {
      prefs.edit().putString(KEY, MoveCodec.encodeAll(moves)).apply()
    }
  }

  override fun clear() {
    prefs.edit().remove(KEY).apply()
  }

  private companion object {
    const val PREFS = "chess_game"
    const val KEY = "saved_moves"
  }
}
