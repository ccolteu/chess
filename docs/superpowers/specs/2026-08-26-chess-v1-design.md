# Chess v1 design

Date: 2026-08-26  
App: `~/agentic/chess` — Android, human vs AI

## Goal

One-device chess: human plays White, a built-in engine plays Black. Legal chess only. No online play.

## Architecture

```
app/
  domain/   Board, pieces, GameState, Rules (pure Kotlin)
  engine/   Minimax + alpha-beta
  ui/game/  Compose board + ChessViewModel
```

UI talks only to `ChessViewModel`. Rules have no Android imports.

## UI

- Single screen, portrait-first
- 8×8 board, Unicode pieces, White at the bottom
- Tap piece → highlight legal squares → tap target
- Promotion chooser (Q/R/B/N) for the human; AI always queens
- Status: check / checkmate / stalemate
- “Thinking…” while the engine searches
- New game in the top bar
- Last-move highlight

## Rules

Legal-move generation including pins, castling, en passant, promotion. Detect check, checkmate, stalemate.

## Engine

Minimax + alpha-beta, depth 3, material + piece-square tables. Off main thread. No opening book.

## Out of scope

Two-player, Stockfish, clocks, PGN, undo, flip board, choose color, difficulty slider, sounds.
