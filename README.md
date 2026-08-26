# Chess

Android chess app: you play White, a small built-in engine plays Black.

## Run

Open `~/agentic/chess` in Android Studio, or:

```bash
./gradlew :app:assembleDebug
```

## Tests

```bash
./gradlew test
```

## v1

- Legal moves (castling, en passant, promotion, pins)
- Human vs AI (minimax, depth 3)
- New game, last-move highlight, promotion chooser
