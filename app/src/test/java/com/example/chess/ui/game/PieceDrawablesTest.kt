package com.example.chess.ui.game

import com.example.chess.R
import com.example.chess.domain.Piece
import com.example.chess.domain.PieceType
import com.example.chess.domain.Side
import org.junit.Assert.assertEquals
import org.junit.Test

class PieceDrawablesTest {
  @Test
  fun mapsEveryPieceToCburnettDrawable() {
    val expected =
      mapOf(
        Piece(PieceType.KING, Side.WHITE) to R.drawable.piece_wk,
        Piece(PieceType.QUEEN, Side.WHITE) to R.drawable.piece_wq,
        Piece(PieceType.ROOK, Side.WHITE) to R.drawable.piece_wr,
        Piece(PieceType.BISHOP, Side.WHITE) to R.drawable.piece_wb,
        Piece(PieceType.KNIGHT, Side.WHITE) to R.drawable.piece_wn,
        Piece(PieceType.PAWN, Side.WHITE) to R.drawable.piece_wp,
        Piece(PieceType.KING, Side.BLACK) to R.drawable.piece_bk,
        Piece(PieceType.QUEEN, Side.BLACK) to R.drawable.piece_bq,
        Piece(PieceType.ROOK, Side.BLACK) to R.drawable.piece_br,
        Piece(PieceType.BISHOP, Side.BLACK) to R.drawable.piece_bb,
        Piece(PieceType.KNIGHT, Side.BLACK) to R.drawable.piece_bn,
        Piece(PieceType.PAWN, Side.BLACK) to R.drawable.piece_bp,
      )
    expected.forEach { (piece, res) -> assertEquals(piece.toString(), res, pieceDrawableRes(piece)) }
  }
}
