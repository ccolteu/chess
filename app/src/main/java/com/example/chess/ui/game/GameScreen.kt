package com.example.chess.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.example.chess.R
import com.example.chess.domain.MoveRow
import com.example.chess.domain.Piece
import com.example.chess.domain.PieceType
import com.example.chess.domain.Side
import com.example.chess.domain.Square
import com.example.chess.engine.AiLevel
import com.example.chess.theme.Brass
import com.example.chess.theme.ChessTheme
import com.example.chess.theme.Cream
import com.example.chess.theme.Ink
import com.example.chess.theme.LegalDot
import com.example.chess.theme.NotepadBinding
import com.example.chess.theme.NotepadMargin
import com.example.chess.theme.NotepadPaper
import com.example.chess.theme.NotepadRule
import com.example.chess.theme.PencilLead
import com.example.chess.theme.PieceCream
import com.example.chess.theme.PieceOxblood
import com.example.chess.theme.SquareWash
import com.example.chess.theme.WalnutBackground
import com.example.chess.theme.WalnutRail

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val viewModel: ChessViewModel = viewModel(factory = ChessViewModel.factory(context))
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  LaunchedEffect(state.clockRunning) {
    if (state.clockRunning == null) return@LaunchedEffect
    while (true) {
      delay(1_000)
      viewModel.persistClocks()
    }
  }
  Box(modifier = modifier.fillMaxSize().background(WalnutBackground)) {
    Image(
      painter = painterResource(R.drawable.cloth_oak_table),
      contentDescription = null,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.Crop,
    )
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val landscape = maxWidth > maxHeight
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
      ) {
        if (landscape) {
          LandscapePlay(
            state = state,
            viewModel = viewModel,
            edgeInset = ScreenInnerPad,
            modifier = Modifier.fillMaxSize().padding(horizontal = SafeEdgePad),
          )
        } else {
          PortraitPlay(
            state = state,
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = SafeEdgePad),
          )
        }
      }
    }
  }
  GameDialogs(state = state, viewModel = viewModel)
}

private val SafeEdgePad = 36.dp
private val ScreenInnerPad = 8.dp
private val BoardChromePad = 12.dp
private val BoardFrameInset = 18.dp
private val ControlChromeGap = 10.dp

private fun boardPieceSize(innerBoard: Dp): Dp = innerBoard / 8 * 0.9f

@Composable
private fun PortraitPlay(state: GameUiState, viewModel: ChessViewModel, modifier: Modifier = Modifier) {
  BoxWithConstraints(modifier) {
    val pieceSize = boardPieceSize(maxWidth - BoardFrameInset * 2)
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.End) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = BoardChromePad),
        horizontalAlignment = Alignment.End,
      ) {
        HudBar(state = state, viewModel = viewModel, modifier = Modifier.padding(bottom = ControlChromeGap))
        SeatStrip(
          icon = R.drawable.ic_seat_cpu,
          iconDescription = "CPU",
          captures = state.cpuCaptures,
          committedMs = state.cpuThinkMs,
          running = state.clockRunning == Side.BLACK,
          startedAt = state.clockStartedAt,
          pieceSize = pieceSize,
          modifier = Modifier.fillMaxWidth().padding(bottom = ControlChromeGap),
          contentPadding = 0.dp,
        )
      }
      PlayBoard(state = state, viewModel = viewModel, modifier = Modifier.fillMaxWidth())
      Column(
        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = BoardChromePad),
        horizontalAlignment = Alignment.End,
      ) {
        SeatStrip(
          icon = R.drawable.ic_seat_player,
          iconDescription = "You",
          captures = state.playerCaptures,
          committedMs = state.playerThinkMs,
          running = state.clockRunning == Side.WHITE,
          startedAt = state.clockStartedAt,
          pieceSize = pieceSize,
          modifier = Modifier.fillMaxWidth().padding(top = ControlChromeGap),
          contentPadding = 0.dp,
        )
        MoveList(rows = state.moveRows, modifier = Modifier.fillMaxWidth().weight(1f).padding(top = ControlChromeGap))
      }
    }
  }
}

@Composable
private fun LandscapePlay(
  state: GameUiState,
  viewModel: ChessViewModel,
  edgeInset: Dp,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier) {
    val pieceSize = boardPieceSize(maxHeight - edgeInset * 2 - BoardFrameInset * 2)
    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.Top) {
      PlayBoard(
        state = state,
        viewModel = viewModel,
        modifier =
          Modifier
            .padding(start = edgeInset, top = edgeInset, bottom = edgeInset)
            .fillMaxHeight()
            .aspectRatio(1f, matchHeightConstraintsFirst = true),
      )
      Column(
        modifier =
          Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(start = edgeInset, end = edgeInset, top = edgeInset, bottom = edgeInset),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(ControlChromeGap),
      ) {
        HudBar(state = state, viewModel = viewModel)
        SeatStrip(
          icon = R.drawable.ic_seat_cpu,
          iconDescription = "CPU",
          captures = state.cpuCaptures,
          committedMs = state.cpuThinkMs,
          running = state.clockRunning == Side.BLACK,
          startedAt = state.clockStartedAt,
          pieceSize = pieceSize,
          modifier = Modifier.fillMaxWidth(),
          contentPadding = 0.dp,
        )
        SeatStrip(
          icon = R.drawable.ic_seat_player,
          iconDescription = "You",
          captures = state.playerCaptures,
          committedMs = state.playerThinkMs,
          running = state.clockRunning == Side.WHITE,
          startedAt = state.clockStartedAt,
          pieceSize = pieceSize,
          modifier = Modifier.fillMaxWidth(),
          contentPadding = 0.dp,
        )
        MoveList(rows = state.moveRows, modifier = Modifier.fillMaxWidth().weight(1f))
      }
    }
  }
}

@Composable
private fun HudBar(state: GameUiState, viewModel: ChessViewModel, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = "Chess",
      style =
        hudLabelStyle.copy(
          fontSize = 26.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 0.8.sp,
          lineHeight = 30.sp,
        ),
      color = Cream,
    )
    GameActions(state = state, viewModel = viewModel)
  }
}

@Composable
private fun GameActions(state: GameUiState, viewModel: ChessViewModel) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
    HudButton(onClick = viewModel::requestNewGame, label = "New game")
    AiLevelMenu(level = state.aiLevel, onPick = viewModel::setAiLevel)
    HudButton(onClick = viewModel::undo, label = "Undo", enabled = state.canUndo)
  }
}

@Composable
private fun SeatIcon(@DrawableRes icon: Int, contentDescription: String, size: Dp) {
  Box(
    modifier = Modifier.size(size).hudChrome(),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(icon),
      contentDescription = contentDescription,
      modifier = Modifier.size(size * 0.53f),
      colorFilter = ColorFilter.tint(Brass),
    )
  }
}

@Composable
private fun SeatStrip(
  @DrawableRes icon: Int,
  iconDescription: String,
  captures: List<Piece>,
  committedMs: Long,
  running: Boolean,
  startedAt: Long,
  pieceSize: Dp,
  modifier: Modifier = Modifier,
  contentPadding: Dp = 8.dp,
) {
  val timeText = formatThinkTime(tickingMs(committedMs, running, startedAt))
  val stripHeight = max(pieceSize, 34.dp)
  Row(
    modifier = modifier.height(stripHeight).padding(horizontal = contentPadding),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    SeatIcon(icon = icon, contentDescription = iconDescription, size = stripHeight)
    LazyRow(
      modifier = Modifier.weight(1f).height(pieceSize).padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      itemsIndexed(captures) { _, piece ->
        PieceGlyph(piece = piece, size = pieceSize)
      }
    }
    SeatClock(timeText = timeText, running = running, height = stripHeight)
  }
}

private val ClockWidth = 108.dp

@Composable
private fun SeatClock(timeText: String, running: Boolean, height: Dp) {
  Box(
    modifier =
      Modifier
        .height(height)
        .width(ClockWidth)
        .hudChrome()
        .padding(start = 10.dp, end = 12.dp),
    contentAlignment = Alignment.CenterEnd,
  ) {
    Text(
      text = timeText,
      style =
        TextStyle(
          fontFamily = FontFamily.Monospace,
          fontFeatureSettings = "tnum, lnum",
          fontWeight = FontWeight.SemiBold,
          fontSize = 20.sp,
          letterSpacing = 0.4.sp,
          lineHeight = 24.sp,
        ),
      color = if (running) Brass else Cream.copy(alpha = 0.7f),
      textAlign = TextAlign.End,
      maxLines = 1,
    )
  }
}

internal fun formatThinkTime(ms: Long): String {
  val totalSec = (ms / 1000).coerceAtLeast(0)
  return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}

@Composable
private fun tickingMs(committed: Long, running: Boolean, startedAt: Long): Long {
  var now by remember { mutableStateOf(System.currentTimeMillis()) }
  LaunchedEffect(running, startedAt) {
    now = System.currentTimeMillis()
    if (!running) return@LaunchedEffect
    while (true) {
      delay(100)
      now = System.currentTimeMillis()
    }
  }
  if (!running) return committed
  return committed + (now - startedAt).coerceAtLeast(0L)
}

@Composable
private fun PlayBoard(state: GameUiState, viewModel: ChessViewModel, modifier: Modifier = Modifier) {
  BoardFrame(modifier = modifier) {
    ChessBoard(
      pieces = state.pieces,
      selected = state.selected,
      legalTargets = state.legalTargets,
      lastMove = state.lastMove?.let { setOf(it.from, it.to) }.orEmpty(),
      onSquareClick = viewModel::onSquareClicked,
      enabled = !state.askResume && !state.isAiThinking && !state.gameOver,
    )
  }
}

@Composable
private fun GameDialogs(state: GameUiState, viewModel: ChessViewModel) {
  if (state.promotionMove != null) {
    ChoiceDialog(onDismissRequest = viewModel::onPromotionDismissed) {
      Text(
        text = "Promote pawn",
        style = hudDialogTitleStyle,
        color = Cream,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { type ->
          Image(
            painter = painterResource(pieceDrawableRes(Piece(type, Side.WHITE))),
            contentDescription = type.name,
            modifier = Modifier.size(36.dp).clickable { viewModel.onPromotionPicked(type) },
            colorFilter = ColorFilter.tint(PieceCream, BlendMode.Modulate),
          )
        }
      }
    }
  }
  if (state.askConfirmNewGame) {
    TableDialog(
      title = "Start a new game?",
      text = "This will erase the game in progress.",
      confirmLabel = "New game",
      onConfirm = viewModel::confirmNewGame,
      dismissLabel = "Cancel",
      onDismiss = viewModel::dismissNewGameConfirm,
      onDismissRequest = viewModel::dismissNewGameConfirm,
    )
  }
  if (state.askResume) {
    TableDialog(
      title = "Resume game?",
      text = "A game was in progress. Do you want to resume it or start a new one?",
      confirmLabel = "Resume",
      onConfirm = viewModel::resumeSavedGame,
      dismissLabel = "New game",
      onDismiss = viewModel::startNewGameFromPrompt,
    )
  }
}

@Composable
private fun AiLevelMenu(level: AiLevel, onPick: (AiLevel) -> Unit) {
  HudChoiceMenu(anchorLabel = level.label) { close ->
    AiLevel.entries.forEach { option ->
      HudTextAction(
        label = option.label,
        selected = option == level,
        onClick = {
          onPick(option)
          close()
        },
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

private val hudButtonShape = RoundedCornerShape(4.dp)

private val hudLabelStyle =
  TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    letterSpacing = 0.6.sp,
    lineHeight = 14.sp,
  )

private val hudStatusStyle =
  TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    letterSpacing = 0.3.sp,
    lineHeight = 16.sp,
  )

private val hudDialogTitleStyle =
  TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    letterSpacing = 0.4.sp,
    lineHeight = 20.sp,
  )

private fun Modifier.hudChrome(): Modifier =
  shadow(4.dp, hudButtonShape).border(BorderStroke(1.dp, Brass), hudButtonShape).background(WalnutRail, hudButtonShape)

@Composable
private fun HudButton(onClick: () -> Unit, label: String, modifier: Modifier = Modifier, enabled: Boolean = true) {
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.defaultMinSize(minWidth = 0.dp, minHeight = 0.dp).height(34.dp).shadow(4.dp, hudButtonShape),
    shape = hudButtonShape,
    border = BorderStroke(1.dp, Brass),
    colors =
      ButtonDefaults.buttonColors(
        containerColor = WalnutRail,
        contentColor = Cream,
        disabledContainerColor = WalnutRail,
        disabledContentColor = Cream.copy(alpha = 0.5f),
      ),
    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
  ) {
    Text(text = label, style = hudLabelStyle, maxLines = 1, textAlign = TextAlign.Center)
  }
}

@Composable
private fun HudTextAction(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  selected: Boolean = false,
  enabled: Boolean = true,
) {
  Text(
    text = label,
    style = hudLabelStyle.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold, fontSize = 13.sp),
    color = if (selected) Brass else Cream,
    textAlign = TextAlign.Center,
    maxLines = 1,
    modifier = modifier.clickable(enabled = enabled, onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp),
  )
}

@Composable
private fun ChoiceDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
  Dialog(onDismissRequest = onDismissRequest) {
    Column(
      modifier =
        Modifier
          .widthIn(min = 168.dp)
          .border(BorderStroke(1.dp, Brass), hudButtonShape)
          .background(WalnutRail, hudButtonShape)
          .padding(vertical = 8.dp, horizontal = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      content()
    }
  }
}

@Composable
private fun HudChoiceMenu(anchorLabel: String, content: @Composable (close: () -> Unit) -> Unit) {
  var open by remember { mutableStateOf(false) }
  HudButton(onClick = { open = true }, label = anchorLabel)
  if (open) {
    ChoiceDialog(onDismissRequest = { open = false }) { content { open = false } }
  }
}

@Composable
private fun TableDialog(
  title: String,
  text: String,
  confirmLabel: String,
  onConfirm: () -> Unit,
  dismissLabel: String? = null,
  onDismiss: (() -> Unit)? = null,
  onDismissRequest: () -> Unit = {},
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = Modifier.border(BorderStroke(1.dp, Brass), hudButtonShape),
    shape = hudButtonShape,
    containerColor = WalnutRail,
    titleContentColor = Cream,
    textContentColor = Cream,
    tonalElevation = 0.dp,
    title = {
      Text(text = title, style = hudDialogTitleStyle, color = Cream, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    },
    text = {
      Text(text = text, style = hudStatusStyle, color = Cream, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    },
    confirmButton = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (dismissLabel != null && onDismiss != null) {
          HudTextAction(label = dismissLabel, onClick = onDismiss)
        }
        HudTextAction(label = confirmLabel, onClick = onConfirm, selected = true)
      }
    },
  )
}

@Composable
private fun BoardFrame(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
  Box(
    modifier =
      modifier
        .shadow(12.dp, RoundedCornerShape(10.dp), ambientColor = Color.Black.copy(alpha = 0.55f), spotColor = Color.Black.copy(alpha = 0.7f))
        .background(WalnutRail, RoundedCornerShape(10.dp))
        .padding(BoardChromePad)
        .border(2.dp, Brass, RoundedCornerShape(4.dp))
        .padding(4.dp),
  ) {
    content()
  }
}

@Composable
private fun MoveList(rows: List<MoveRow>, modifier: Modifier = Modifier) {
  val listState = rememberLazyListState()
  LaunchedEffect(rows.size, rows.lastOrNull()?.black) {
    if (rows.isNotEmpty()) {
      listState.animateScrollToItem(rows.lastIndex)
    }
  }
  Box(modifier) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .shadow(4.dp, RectangleShape, ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.4f))
          .border(BorderStroke(1.dp, Brass), RectangleShape)
          .background(NotepadPaper, RectangleShape),
    ) {
    Box(
      modifier = Modifier.fillMaxWidth().height(16.dp).background(NotepadBinding),
      contentAlignment = Alignment.CenterStart,
    ) {
      Row(
        modifier = Modifier.padding(start = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        repeat(9) {
          Box(Modifier.size(7.dp).background(NotepadPaper.copy(alpha = 0.55f), CircleShape))
        }
      }
    }
    Box(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxWidth()
          .clip(RectangleShape)
          .drawBehind { drawNotepadSheet() },
    ) {
      if (rows.isEmpty()) {
        Text(
          text = "MOVES WILL APPEAR HERE",
          style = PencilWriting.copy(color = PencilLead.copy(alpha = 0.45f)),
          modifier = Modifier.padding(start = 44.dp, top = 10.dp, end = 12.dp),
        )
      } else {
        LazyColumn(
          state = listState,
          modifier = Modifier.fillMaxSize().padding(start = 44.dp, end = 10.dp, top = 4.dp, bottom = 8.dp),
        ) {
          items(rows, key = { it.number }) { row ->
            Row(
              modifier = Modifier.fillMaxWidth().height(22.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "${row.number}.".uppercase(),
                style = PencilWriting.copy(color = PencilLead.copy(alpha = 0.72f)),
                maxLines = 1,
                modifier = Modifier.width(28.dp),
              )
              Text(
                text = row.white.uppercase(),
                style = PencilWriting,
                modifier = Modifier.weight(1f),
                maxLines = 1,
              )
              Text(
                text = row.black.orEmpty().uppercase(),
                style = PencilWriting,
                modifier = Modifier.weight(1f),
                maxLines = 1,
              )
            }
          }
        }
      }
    }
    }
    NotepadPencil(
      modifier =
        Modifier
          .align(Alignment.CenterEnd)
          .padding(end = 28.dp)
          .graphicsLayer { rotationZ = 14f },
    )
  }
}

private val PencilHand = FontFamily(Font(R.font.architects_daughter_regular))

private val PencilWriting =
  TextStyle(
    fontFamily = PencilHand,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 22.sp,
    letterSpacing = 0.3.sp,
    color = PencilLead,
  )

@Composable
private fun NotepadPencil(modifier: Modifier = Modifier) {
  Canvas(
    modifier =
      modifier
        .size(width = 11.dp, height = 124.dp)
        .shadow(3.dp, RoundedCornerShape(1.dp), ambientColor = Color.Black.copy(alpha = 0.35f), spotColor = Color.Black.copy(alpha = 0.4f)),
  ) {
    val w = size.width
    val h = size.height
    val woodEnd = h * 0.16f
    val barrelEnd = h * 0.82f
    val ferruleEnd = h * 0.93f
    val yellow = Color(0xFFF4C430)
    val yellowShade = Color(0xFFD4A41A)
    val wood = Color(0xFFE6C392)
    val graphite = Color(0xFF3A3A3A)
    val ferrule = Color(0xFFC8CDD2)
    val ferruleDark = Color(0xFF8E949A)
    val eraser = Color(0xFFE39AAB)
    val eraserDark = Color(0xFFC47B8C)
    val point =
      Path().apply {
        moveTo(w * 0.5f, 0f)
        lineTo(w, woodEnd)
        lineTo(0f, woodEnd)
        close()
      }
    drawPath(point, wood)
    val lead =
      Path().apply {
        moveTo(w * 0.5f, 0f)
        lineTo(w * 0.68f, woodEnd * 0.42f)
        lineTo(w * 0.32f, woodEnd * 0.42f)
        close()
      }
    drawPath(lead, graphite)
    drawRect(yellow, topLeft = Offset(0f, woodEnd), size = Size(w, barrelEnd - woodEnd))
    drawRect(yellowShade, topLeft = Offset(0f, woodEnd), size = Size(w * 0.18f, barrelEnd - woodEnd))
    drawRect(Color.White.copy(alpha = 0.28f), topLeft = Offset(w * 0.28f, woodEnd + 2f), size = Size(w * 0.16f, barrelEnd - woodEnd - 4f))
    drawRect(ferrule, topLeft = Offset(0f, barrelEnd), size = Size(w, ferruleEnd - barrelEnd))
    val band = (ferruleEnd - barrelEnd) / 5f
    drawRect(ferruleDark, topLeft = Offset(0f, barrelEnd + band), size = Size(w, band * 0.45f))
    drawRect(ferruleDark, topLeft = Offset(0f, ferruleEnd - band * 1.4f), size = Size(w, band * 0.45f))
    val worn =
      Path().apply {
        val r = w * 0.22f
        moveTo(0f, ferruleEnd)
        lineTo(w, ferruleEnd)
        lineTo(w, h - r)
        quadraticTo(w, h, w - r * 0.85f, h)
        lineTo(r * 0.85f, h)
        quadraticTo(0f, h, 0f, h - r)
        close()
      }
    drawPath(worn, eraser)
    drawRect(eraserDark, topLeft = Offset(w * 0.12f, h - 2.2f), size = Size(w * 0.76f, 1.6f))
  }
}

private fun DrawScope.drawNotepadSheet() {
  drawRect(NotepadPaper)
  val grain = Color.White.copy(alpha = 0.07f)
  val step = size.width / 14f
  for (i in 0..13) {
    drawRect(color = grain, topLeft = Offset(i * step + 1.2f, 0f), size = Size(1.1f, size.height))
  }
  val lineStep = 22.dp.toPx()
  var y = 22.dp.toPx()
  while (y < size.height) {
    drawLine(color = NotepadRule, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.2f)
    y += lineStep
  }
  val marginX = 36.dp.toPx()
  drawLine(color = NotepadMargin, start = Offset(marginX, 0f), end = Offset(marginX, size.height), strokeWidth = 2.2f)
}

@Composable
private fun ChessBoard(
  pieces: List<Piece?>,
  selected: Square?,
  legalTargets: Set<Square>,
  lastMove: Set<Square>,
  onSquareClick: (Square) -> Unit,
  enabled: Boolean,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
    val tile = maxWidth / 8
    Image(
      painter = painterResource(R.drawable.board_walnut_oak),
      contentDescription = null,
      modifier = Modifier.fillMaxSize(),
      contentScale = ContentScale.FillBounds,
    )
    Column(modifier = Modifier.fillMaxSize()) {
      for (displayRank in 7 downTo 0) {
        Row {
          for (file in 0..7) {
            val square = Square(file, displayRank)
            val marked = selected == square || square in lastMove
            Box(
              modifier =
                Modifier.size(tile)
                  .then(if (selected == square) Modifier.background(SquareWash) else Modifier)
                  .then(if (marked) Modifier.border(2.dp, Brass) else Modifier)
                  .clickable(enabled = enabled) { onSquareClick(square) },
              contentAlignment = Alignment.Center,
            ) {
              val piece = pieces[square.index]
              if (piece != null) {
                PieceGlyph(piece = piece, size = tile * 0.9f)
              } else if (square in legalTargets) {
                Box(Modifier.size(tile * 0.26f).background(LegalDot, CircleShape))
              }
              if (piece != null && square in legalTargets) {
                Box(Modifier.fillMaxSize().border(3.dp, Ink.copy(alpha = 0.55f)))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PieceGlyph(piece: Piece, size: Dp) {
  Image(
    painter = painterResource(pieceDrawableRes(Piece(piece.type, Side.WHITE))),
    contentDescription = null,
    modifier = Modifier.size(size),
    colorFilter =
      ColorFilter.tint(if (piece.side == Side.WHITE) PieceCream else PieceOxblood, BlendMode.Modulate),
  )
}

@Preview(showBackground = true)
@Composable
private fun GameScreenPreview() {
  ChessTheme { GameScreen() }
}
