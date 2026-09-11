package com.cc.chess.ui.game

import com.cc.chess.R
import com.cc.chess.domain.Piece
import com.cc.chess.domain.PieceType
import com.cc.chess.domain.Side

/** CBurnett Staunton set by Colin M.L. Burnett (BSD / GPL / GFDL). */
fun pieceDrawableRes(piece: Piece): Int =
  when (piece.side to piece.type) {
    Side.WHITE to PieceType.KING -> R.drawable.piece_wk
    Side.WHITE to PieceType.QUEEN -> R.drawable.piece_wq
    Side.WHITE to PieceType.ROOK -> R.drawable.piece_wr
    Side.WHITE to PieceType.BISHOP -> R.drawable.piece_wb
    Side.WHITE to PieceType.KNIGHT -> R.drawable.piece_wn
    Side.WHITE to PieceType.PAWN -> R.drawable.piece_wp
    Side.BLACK to PieceType.KING -> R.drawable.piece_bk
    Side.BLACK to PieceType.QUEEN -> R.drawable.piece_bq
    Side.BLACK to PieceType.ROOK -> R.drawable.piece_br
    Side.BLACK to PieceType.BISHOP -> R.drawable.piece_bb
    Side.BLACK to PieceType.KNIGHT -> R.drawable.piece_bn
    Side.BLACK to PieceType.PAWN -> R.drawable.piece_bp
    else -> error("Unknown piece $piece")
  }
