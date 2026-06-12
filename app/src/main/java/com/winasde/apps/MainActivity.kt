package com.winasde.apps

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.winasde.apps.data.DrawingType
import com.winasde.apps.data.PlayerPosition
import com.winasde.apps.data.PlayerProfile
import com.winasde.apps.data.TacticalDrawing
import com.winasde.apps.data.TeamScheme
import com.winasde.apps.data.analyze
import com.winasde.apps.ui.theme.FootballSchemesTheme
import kotlin.math.roundToInt
import java.util.UUID

private enum class AppTab {
    Schemes,
    Players,
    QrScanner
}

private enum class DrawingTool {
    Move,
    Arrow,
    Zone,
    Marker
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FootballSchemesTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot(
    webViewStateViewModel: WebViewStateViewModel = viewModel()
) {
    val appState by webViewStateViewModel.appState.collectAsStateWithLifecycle()

    when (val state = appState) {
        WebViewStateViewModel.AppState.Loading -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        is WebViewStateViewModel.AppState.WebView -> {
            AdvancedWebViewScreen(initialUrl = state.url)
        }
        WebViewStateViewModel.AppState.NormalApp -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                FootballSchemesApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FootballSchemesApp(
    viewModel: SchemeViewModel = viewModel()
) {
    val schemes by viewModel.schemes.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(AppTab.Schemes) }
    var drawingTool by remember { mutableStateOf(DrawingTool.Move) }
    val selectedScheme = schemes.firstOrNull { it.id == selectedId } ?: schemes.first()

    Scaffold(
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tacktic Win", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Схемы, игроки, QR и тактический анализ",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.createScheme()
                            selectedId = null
                        },
                        contentPadding = PaddingValues(horizontal = 14.dp)
                    ) {
                        Text("Новая")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppTabs(selectedTab = selectedTab, onSelect = { selectedTab = it })
            when (selectedTab) {
                AppTab.Schemes -> SchemesWorkspace(
                    schemes = schemes,
                    players = players,
                    selectedScheme = selectedScheme,
                    drawingTool = drawingTool,
                    onDrawingToolChange = { drawingTool = it },
                    onSelectScheme = { selectedId = it },
                    onCreateScheme = viewModel::createScheme,
                    onRename = { viewModel.renameScheme(selectedScheme, it) },
                    onFormationChange = { viewModel.changeFormation(selectedScheme, it) },
                    onMovePlayer = { number, x, y -> viewModel.movePlayer(selectedScheme, number, x, y) },
                    onAddDrawing = { viewModel.addDrawing(selectedScheme, it) },
                    onClearDrawings = { viewModel.clearDrawings(selectedScheme) },
                    onNotesChange = { viewModel.updateNotes(selectedScheme, it) },
                    onDelete = {
                        viewModel.deleteScheme(selectedScheme.id)
                        selectedId = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
                AppTab.Players -> PlayersDatabaseScreen(
                    players = players,
                    onAddPlayer = viewModel::savePlayer,
                    onDeletePlayer = viewModel::deletePlayer,
                    modifier = Modifier.fillMaxSize()
                )
                AppTab.QrScanner -> QrScannerScreen(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AppTabs(
    selectedTab: AppTab,
    onSelect: (AppTab) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedTab == AppTab.Schemes,
            onClick = { onSelect(AppTab.Schemes) },
            label = { Text("Схемы") }
        )
        FilterChip(
            selected = selectedTab == AppTab.Players,
            onClick = { onSelect(AppTab.Players) },
            label = { Text("Игроки") }
        )
        FilterChip(
            selected = selectedTab == AppTab.QrScanner,
            onClick = { onSelect(AppTab.QrScanner) },
            label = { Text("QR") }
        )
    }
}

@Composable
private fun SchemesWorkspace(
    schemes: List<TeamScheme>,
    players: List<PlayerProfile>,
    selectedScheme: TeamScheme,
    drawingTool: DrawingTool,
    onDrawingToolChange: (DrawingTool) -> Unit,
    onSelectScheme: (String) -> Unit,
    onCreateScheme: () -> Unit,
    onRename: (String) -> Unit,
    onFormationChange: (String) -> Unit,
    onMovePlayer: (Int, Float, Float) -> Unit,
    onAddDrawing: (TacticalDrawing) -> Unit,
    onClearDrawings: () -> Unit,
    onNotesChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        if (maxWidth >= 900.dp) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SchemeList(
                    schemes = schemes,
                    selectedId = selectedScheme.id,
                    onSelect = onSelectScheme,
                    onCreate = onCreateScheme,
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                )
                EditorPanel(
                    scheme = selectedScheme,
                    players = players,
                    drawingTool = drawingTool,
                    onDrawingToolChange = onDrawingToolChange,
                    onRename = onRename,
                    onFormationChange = onFormationChange,
                    onMovePlayer = onMovePlayer,
                    onAddDrawing = onAddDrawing,
                    onClearDrawings = onClearDrawings,
                    modifier = Modifier.weight(1f)
                )
                AnalysisPanel(
                    scheme = selectedScheme,
                    onNotesChange = onNotesChange,
                    onDelete = onDelete,
                    modifier = Modifier.width(320.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SchemeList(
                    schemes = schemes,
                    selectedId = selectedScheme.id,
                    onSelect = onSelectScheme,
                    onCreate = onCreateScheme,
                    modifier = Modifier.fillMaxWidth()
                )
                EditorPanel(
                    scheme = selectedScheme,
                    players = players,
                    drawingTool = drawingTool,
                    onDrawingToolChange = onDrawingToolChange,
                    onRename = onRename,
                    onFormationChange = onFormationChange,
                    onMovePlayer = onMovePlayer,
                    onAddDrawing = onAddDrawing,
                    onClearDrawings = onClearDrawings,
                    modifier = Modifier.fillMaxWidth()
                )
                AnalysisPanel(
                    scheme = selectedScheme,
                    onNotesChange = onNotesChange,
                    onDelete = onDelete,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SchemeList(
    schemes: List<TeamScheme>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Схемы",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onCreate) {
                    Text("Добавить")
                }
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                items(schemes, key = { it.id }) { scheme ->
                    SchemeListItem(
                        scheme = scheme,
                        selected = scheme.id == selectedId,
                        onClick = { onSelect(scheme.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SchemeListItem(
    scheme: TeamScheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = if (selected) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = colors,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = scheme.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = scheme.formation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditorPanel(
    scheme: TeamScheme,
    players: List<PlayerProfile>,
    drawingTool: DrawingTool,
    onDrawingToolChange: (DrawingTool) -> Unit,
    onRename: (String) -> Unit,
    onFormationChange: (String) -> Unit,
    onMovePlayer: (Int, Float, Float) -> Unit,
    onAddDrawing: (TacticalDrawing) -> Unit,
    onClearDrawings: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = scheme.name,
                onValueChange = onRename,
                label = { Text("Название схемы") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            FormationSelector(
                selected = scheme.formation,
                onSelect = onFormationChange
            )
            DrawingToolbar(
                selectedTool = drawingTool,
                onSelect = onDrawingToolChange,
                onClear = onClearDrawings
            )
            SoccerPitch(
                players = scheme.players,
                playerProfiles = players,
                drawings = scheme.drawings,
                drawingTool = drawingTool,
                onMovePlayer = onMovePlayer,
                onAddDrawing = onAddDrawing,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FormationSelector(
    selected: String,
    onSelect: (String) -> Unit
) {
    val formations = listOf("4-3-3", "4-4-2", "3-5-2", "4-2-3-1")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Формация",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            formations.forEach { formation ->
                FilterChip(
                    selected = formation == selected,
                    onClick = { onSelect(formation) },
                    label = { Text(formation) }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun DrawingToolbar(
    selectedTool: DrawingTool,
    onSelect: (DrawingTool) -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Визуальные инструменты",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selectedTool == DrawingTool.Move, { onSelect(DrawingTool.Move) }, label = { Text("Двигать") })
            FilterChip(selectedTool == DrawingTool.Arrow, { onSelect(DrawingTool.Arrow) }, label = { Text("Стрелка") })
            FilterChip(selectedTool == DrawingTool.Zone, { onSelect(DrawingTool.Zone) }, label = { Text("Зона") })
            FilterChip(selectedTool == DrawingTool.Marker, { onSelect(DrawingTool.Marker) }, label = { Text("Метка") })
            TextButton(onClick = onClear) {
                Text("Очистить")
            }
        }
    }
}

@Composable
private fun SoccerPitch(
    players: List<PlayerPosition>,
    playerProfiles: List<PlayerProfile>,
    drawings: List<TacticalDrawing>,
    drawingTool: DrawingTool,
    onMovePlayer: (Int, Float, Float) -> Unit,
    onAddDrawing: (TacticalDrawing) -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var drawingStart by remember { mutableStateOf<Offset?>(null) }
    var drawingCurrent by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2D7A43))
            .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
            .onSizeChanged { size = it }
            .pointerInput(drawingTool, size) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (drawingTool != DrawingTool.Move && size.width > 0 && size.height > 0) {
                            drawingStart = offset
                            drawingCurrent = offset
                        }
                    },
                    onDragEnd = {
                        val start = drawingStart
                        val end = drawingCurrent
                        if (drawingTool != DrawingTool.Move && start != null && end != null) {
                            val type = when (drawingTool) {
                                DrawingTool.Arrow -> DrawingType.Arrow
                                DrawingTool.Zone -> DrawingType.Zone
                                DrawingTool.Marker -> DrawingType.Marker
                                DrawingTool.Move -> DrawingType.Marker
                            }
                            onAddDrawing(
                                TacticalDrawing(
                                    id = UUID.randomUUID().toString(),
                                    type = type,
                                    startX = (start.x / size.width).coerceIn(0f, 1f),
                                    startY = (start.y / size.height).coerceIn(0f, 1f),
                                    endX = (end.x / size.width).coerceIn(0f, 1f),
                                    endY = (end.y / size.height).coerceIn(0f, 1f)
                                )
                            )
                        }
                        drawingStart = null
                        drawingCurrent = null
                    }
                ) { change, dragAmount ->
                    if (drawingTool != DrawingTool.Move && drawingStart != null) {
                        change.consume()
                        drawingCurrent = change.position + dragAmount
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val line = Color.White.copy(alpha = 0.78f)
            val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            drawLine(line, Offset(0f, size.height / 2f), Offset(size.width.toFloat(), size.height / 2f), stroke.width)
            drawCircle(line, radius = size.width * 0.14f, center = Offset(size.width / 2f, size.height / 2f), style = stroke)
            drawCircle(line, radius = 4.dp.toPx(), center = Offset(size.width / 2f, size.height / 2f))
            drawRect(
                color = line,
                topLeft = Offset(size.width * 0.18f, 0f),
                size = Size(size.width * 0.64f, size.height * 0.16f),
                style = stroke
            )
            drawRect(
                color = line,
                topLeft = Offset(size.width * 0.18f, size.height * 0.84f),
                size = Size(size.width * 0.64f, size.height * 0.16f),
                style = stroke
            )
            drawRect(
                color = line.copy(alpha = 0.42f),
                topLeft = Offset(size.width * 0.34f, 0f),
                size = Size(size.width * 0.32f, size.height * 0.07f),
                style = stroke
            )
            drawRect(
                color = line.copy(alpha = 0.42f),
                topLeft = Offset(size.width * 0.34f, size.height * 0.93f),
                size = Size(size.width * 0.32f, size.height * 0.07f),
                style = stroke
            )

            drawings.forEach { drawing ->
                val start = Offset(drawing.startX * size.width, drawing.startY * size.height)
                val end = Offset(drawing.endX * size.width, drawing.endY * size.height)
                when (drawing.type) {
                    DrawingType.Arrow -> {
                        val color = Color(0xFFFFD166)
                        drawLine(color, start, end, strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
                        drawCircle(color, radius = 7.dp.toPx(), center = end)
                    }
                    DrawingType.Zone -> {
                        val left = minOf(start.x, end.x)
                        val top = minOf(start.y, end.y)
                        val width = kotlin.math.abs(end.x - start.x).coerceAtLeast(28.dp.toPx())
                        val height = kotlin.math.abs(end.y - start.y).coerceAtLeast(28.dp.toPx())
                        drawRoundRect(
                            color = Color(0xFF4FC3F7).copy(alpha = 0.26f),
                            topLeft = Offset(left, top),
                            size = Size(width, height)
                        )
                        drawRoundRect(
                            color = Color(0xFFB3E5FC),
                            topLeft = Offset(left, top),
                            size = Size(width, height),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                    DrawingType.Marker -> {
                        drawCircle(Color(0xFFFF7043), radius = 12.dp.toPx(), center = end)
                        drawCircle(Color.White, radius = 5.dp.toPx(), center = end)
                    }
                }
            }
        }

        players.forEach { player ->
            PlayerMarker(
                player = player,
                profile = playerProfiles.firstOrNull { it.number == player.number },
                pitchSize = size,
                onMove = onMovePlayer,
                canDrag = drawingTool == DrawingTool.Move
            )
        }
    }
}

@Composable
private fun PlayerMarker(
    player: PlayerPosition,
    profile: PlayerProfile?,
    pitchSize: IntSize,
    onMove: (Int, Float, Float) -> Unit,
    canDrag: Boolean
) {
    val density = LocalDensity.current
    val markerSize = 42.dp
    val markerPx = with(density) { markerSize.toPx() }
    var dragX by remember(player.number, player.x) { mutableStateOf(player.x) }
    var dragY by remember(player.number, player.y) { mutableStateOf(player.y) }

    if (pitchSize.width == 0 || pitchSize.height == 0) return

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (dragX * pitchSize.width - markerPx / 2f).roundToInt(),
                    y = (dragY * pitchSize.height - markerPx / 2f).roundToInt()
                )
            }
            .size(markerSize)
            .clip(CircleShape)
            .background(if (player.role == "GK") Color(0xFFFFD166) else Color(0xFFEAF4FF))
            .border(2.dp, Color(0xFF123524), CircleShape)
            .pointerInput(player.number, pitchSize, canDrag) {
                detectDragGestures(
                    onDragEnd = {
                        if (canDrag) {
                            onMove(player.number, dragX, dragY)
                        }
                    }
                ) { change, dragAmount ->
                    if (canDrag) {
                        change.consume()
                        dragX = (dragX + dragAmount.x / pitchSize.width).coerceIn(0.06f, 0.94f)
                        dragY = (dragY + dragAmount.y / pitchSize.height).coerceIn(0.06f, 0.96f)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = player.number.toString(),
                color = Color(0xFF102016),
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                lineHeight = 14.sp
            )
            Text(
                text = profile?.name?.take(6) ?: player.role,
                color = Color(0xFF365140),
                fontWeight = FontWeight.Bold,
                fontSize = if (profile == null) 8.sp else 7.sp,
                lineHeight = 8.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AnalysisPanel(
    scheme: TeamScheme,
    onNotesChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val analysis = scheme.analyze()
    var showDeleteDialog by remember { mutableStateOf(false) }

    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Анализ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            AssistChip(onClick = {}, label = { Text(analysis.balanceText) })
            AssistChip(onClick = {}, label = { Text(analysis.pressureText) })
            AnalysisMetric("Атака", analysis.attackingPlayers, "игроков выше линии")
            AnalysisMetric("Центр", analysis.midfieldPlayers, "игроков в средней зоне")
            AnalysisMetric("Оборона", analysis.defensivePlayers, "игроков ниже линии")
            AnalysisMetric("Фланги", analysis.leftSidePlayers + analysis.rightSidePlayers, "игроков широко")
            OutlinedTextField(
                value = scheme.notes,
                onValueChange = onNotesChange,
                label = { Text("Заметки к схеме") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { showDeleteDialog = true }) {
                Text("Удалить схему", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить схему?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun AnalysisMetric(
    title: String,
    value: Int,
    description: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlayersDatabaseScreen(
    players: List<PlayerProfile>,
    onAddPlayer: (String, Int, String, String, Int, String) -> Unit,
    onDeletePlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var numberText by remember { mutableStateOf("10") }
    var position by remember { mutableStateOf("Полузащитник") }
    var strongFoot by remember { mutableStateOf("Правая") }
    var ageText by remember { mutableStateOf("18") }
    var notes by remember { mutableStateOf("") }

    BoxWithConstraints(modifier = modifier) {
        if (maxWidth >= 840.dp) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PlayerFormCard(
                    name = name,
                    numberText = numberText,
                    position = position,
                    strongFoot = strongFoot,
                    ageText = ageText,
                    notes = notes,
                    onNameChange = { name = it },
                    onNumberChange = { numberText = it.filter(Char::isDigit).take(2) },
                    onPositionChange = { position = it },
                    onStrongFootChange = { strongFoot = it },
                    onAgeChange = { ageText = it.filter(Char::isDigit).take(2) },
                    onNotesChange = { notes = it },
                    onSubmit = {
                        if (name.isNotBlank()) {
                            onAddPlayer(
                                name,
                                numberText.toIntOrNull() ?: 1,
                                position,
                                strongFoot,
                                ageText.toIntOrNull() ?: 18,
                                notes
                            )
                            name = ""
                            notes = ""
                        }
                    },
                    modifier = Modifier.width(360.dp)
                )
                PlayersListCard(
                    players = players,
                    onDeletePlayer = onDeletePlayer,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PlayerFormCard(
                    name = name,
                    numberText = numberText,
                    position = position,
                    strongFoot = strongFoot,
                    ageText = ageText,
                    notes = notes,
                    onNameChange = { name = it },
                    onNumberChange = { numberText = it.filter(Char::isDigit).take(2) },
                    onPositionChange = { position = it },
                    onStrongFootChange = { strongFoot = it },
                    onAgeChange = { ageText = it.filter(Char::isDigit).take(2) },
                    onNotesChange = { notes = it },
                    onSubmit = {
                        if (name.isNotBlank()) {
                            onAddPlayer(
                                name,
                                numberText.toIntOrNull() ?: 1,
                                position,
                                strongFoot,
                                ageText.toIntOrNull() ?: 18,
                                notes
                            )
                            name = ""
                            notes = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                PlayersListCard(
                    players = players,
                    onDeletePlayer = onDeletePlayer,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun PlayerFormCard(
    name: String,
    numberText: String,
    position: String,
    strongFoot: String,
    ageText: String,
    notes: String,
    onNameChange: (String) -> Unit,
    onNumberChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onStrongFootChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "База игроков",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Имя игрока") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = numberText,
                    onValueChange = onNumberChange,
                    label = { Text("Номер") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = ageText,
                    onValueChange = onAgeChange,
                    label = { Text("Возраст") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = position,
                onValueChange = onPositionChange,
                label = { Text("Позиция") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Правая", "Левая", "Обе").forEach { foot ->
                    FilterChip(
                        selected = strongFoot == foot,
                        onClick = { onStrongFootChange(foot) },
                        label = { Text(foot) }
                    )
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Заметки") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить игрока")
            }
        }
    }
}

@Composable
private fun PlayersListCard(
    players: List<PlayerProfile>,
    onDeletePlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Состав",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (players.isEmpty()) {
                Text(
                    text = "Пока нет игроков. Добавь первого игрока вручную или позже через QR.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(players, key = { it.id }) { player ->
                        PlayerRow(player = player, onDelete = { onDeletePlayer(player.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(
    player: PlayerProfile,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.number.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(player.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${player.position} • ${player.age} лет • ${player.strongFoot}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (player.notes.isNotBlank()) {
                    Text(
                        text = player.notes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(onClick = onDelete) {
                Text("Удалить")
            }
        }
    }
}

@Composable
private fun QrScannerScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "QR-сканирование",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF101612)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val frameWidth = size.width * 0.68f
                    val frameHeight = frameWidth
                    val left = (size.width - frameWidth) / 2f
                    val top = (size.height - frameHeight) / 2f
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.18f),
                        topLeft = Offset(left, top),
                        size = Size(frameWidth, frameHeight),
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawLine(
                        color = Color(0xFF9BD5A7),
                        start = Offset(left + 18.dp.toPx(), top + frameHeight / 2f),
                        end = Offset(left + frameWidth - 18.dp.toPx(), top + frameHeight / 2f),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (hasCameraPermission) "Камера разрешена" else "Нужно разрешение камеры",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Следующий шаг: подключить распознавание QR и автодобавление игрока.",
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 28.dp)
                    )
                }
            }
            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                enabled = !hasCameraPermission
            ) {
                Text(if (hasCameraPermission) "Разрешение получено" else "Разрешить камеру")
            }
        }
    }
}
