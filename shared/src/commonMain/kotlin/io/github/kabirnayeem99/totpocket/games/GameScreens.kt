package io.github.kabirnayeem99.totpocket.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kabirnayeem99.totpocket.LocalAppContainer
import io.github.kabirnayeem99.totpocket.games.shapematch.Point
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeKind
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeMatchAction
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeMatchEngine
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeMatchPhase
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeMatchUiState
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeMatchViewModel
import io.github.kabirnayeem99.totpocket.games.shapematch.ShapeRound
import io.github.kabirnayeem99.totpocket.games.shapematch.displayName
import io.github.kabirnayeem99.totpocket.games.shapematch.fill
import io.github.kabirnayeem99.totpocket.games.shapematch.outline
import io.github.kabirnayeem99.totpocket.ui.components.SectionCard
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerGrid
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerScaffold
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketColors
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ---------------------------------------------------------------- picker

private enum class Game(val label: String) { Shapes("Shapes") }

@Composable
fun GamePickerScreen(
    onHome: () -> Unit,
    onOpenShapeMatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToddlerScaffold(onHome = onHome, modifier = modifier, background = TotPocketColors.YellowTint) {
        ToddlerGrid(items = Game.entries, portraitColumns = 1, landscapeColumns = 1) { game, cellModifier ->
            SectionCard(
                onClick = {
                    when (game) {
                        Game.Shapes -> onOpenShapeMatch()
                    }
                },
                label = game.label,
                color = TotPocketColors.Yellow,
                contentColor = TotPocketColors.OnLight,
                modifier = cellModifier,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf(ShapeKind.Circle, ShapeKind.Triangle, ShapeKind.Square).forEach { SolidShape(it, 72.dp) }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- shape match

@Composable
fun ShapeMatchScreen(onHome: () -> Unit, modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ShapeMatchViewModel(container.soundPlayer, container.random) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ShapeMatchContent(state, viewModel::onAction, onHome, modifier)
}

/**
 * Outlined holes on top, loose shapes below. Drag a shape near its own hole and it snaps in;
 * let go anywhere else and it springs back, silently. No score, no timer, no failure.
 */
@Composable
fun ShapeMatchContent(
    state: ShapeMatchUiState,
    onAction: (ShapeMatchAction) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToddlerScaffold(onHome = onHome, modifier = modifier, background = TotPocketColors.YellowTint) {
        if (state.phase == ShapeMatchPhase.Done) {
            AllDone()
        } else {
            key(state.round) {
                ShapeBoard(state, onAction)
            }
        }
    }
}

@Composable
private fun ShapeBoard(state: ShapeMatchUiState, onAction: (ShapeMatchAction) -> Unit) {
    val holeCentres = remember { mutableMapOf<ShapeKind, Point>() }
    val tolerancePx = with(LocalDensity.current) { ShapeMatchEngine.SnapToleranceDp.dp.toPx() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val count = state.round.holes.size
        val slot = minOf(maxWidth / count - 24.dp, maxHeight / 2 - 32.dp, 150.dp)
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.round.holes.forEach { kind ->
                    Hole(
                        kind = kind,
                        filled = kind in state.placed,
                        celebrating = state.phase == ShapeMatchPhase.Celebrating,
                        size = slot,
                        modifier = Modifier.onGloballyPositioned {
                            val centre = it.boundsInRoot().center
                            holeCentres[kind] = Point(centre.x, centre.y)
                        },
                    )
                }
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.round.tray.forEach { kind ->
                    if (kind in state.placed) {
                        Spacer(Modifier.size(slot))
                    } else {
                        DraggableShape(
                            kind = kind,
                            size = slot,
                            snapTargetFor = { drop ->
                                ShapeMatchEngine.resolveDrop(kind, drop, holeCentres, tolerancePx)
                                    ?.let { holeCentres[it] }
                            },
                            onDropped = { drop ->
                                onAction(ShapeMatchAction.ShapeDropped(kind, drop, holeCentres.toMap(), tolerancePx))
                            },
                        )
                    }
                }
            }
        }
    }
}

/** An empty outline with a faint hint of the shape's colour, or — once matched — the shape itself. */
@Composable
private fun Hole(kind: ShapeKind, filled: Boolean, celebrating: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(filled) {
        if (filled) {
            scale.snapTo(1.2f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    LaunchedEffect(celebrating) {
        if (celebrating) {
            scale.animateTo(1.12f, spring(stiffness = Spring.StiffnessMediumLow))
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    val shape = kind.outline()
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .background(if (filled) kind.fill() else kind.fill().copy(alpha = 0.18f), shape)
            .border(TotPocketDimens.CardOutline, TotPocketColors.Outline, shape)
            .semantics { contentDescription = "${kind.displayName()} hole" },
    )
}

@Composable
private fun DraggableShape(
    kind: ShapeKind,
    size: Dp,
    snapTargetFor: (drop: Point) -> Point?,
    onDropped: (drop: Point) -> Unit,
) {
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentSnapTarget by rememberUpdatedState(snapTargetFor)
    val currentOnDropped by rememberUpdatedState(onDropped)
    // Written in layout, read only inside gesture callbacks — never during composition.
    val restingCentre = remember { floatArrayOf(0f, 0f) }
    var dragging by remember { mutableStateOf(false) }

    Box(
        Modifier
            .zIndex(if (dragging) 1f else 0f)
            // Measured outside the offset below, so this is where the shape rests, not where it's dragged.
            .onGloballyPositioned {
                val centre = it.boundsInRoot().center
                restingCentre[0] = centre.x
                restingCentre[1] = centre.y
            }
            .offset { IntOffset(offset.value.x.roundToInt(), offset.value.y.roundToInt()) }
            .graphicsLayer {
                val lift = if (dragging) 1.1f else 1f
                scaleX = lift
                scaleY = lift
            }
            .pointerInput(kind) {
                detectDragGestures(
                    onDragStart = { dragging = true },
                    onDrag = { change, amount ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + amount) }
                    },
                    onDragEnd = {
                        dragging = false
                        val drop = Point(restingCentre[0] + offset.value.x, restingCentre[1] + offset.value.y)
                        val target = currentSnapTarget(drop)
                        scope.launch {
                            if (target != null) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                offset.animateTo(
                                    Offset(target.x - restingCentre[0], target.y - restingCentre[1]),
                                    spring(stiffness = Spring.StiffnessMedium),
                                )
                            } else {
                                offset.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                            currentOnDropped(drop)
                        }
                    },
                    onDragCancel = {
                        dragging = false
                        scope.launch { offset.animateTo(Offset.Zero) }
                    },
                )
            },
    ) {
        SolidShape(kind, size)
    }
}

@Composable
private fun SolidShape(kind: ShapeKind, size: Dp) {
    val shape = kind.outline()
    Box(
        Modifier
            .size(size)
            .background(kind.fill(), shape)
            .border(TotPocketDimens.CardOutline, TotPocketColors.Outline, shape)
            .semantics { contentDescription = kind.displayName() },
    )
}

/** The natural stopping point: one calm star and nothing else to do but go Home. */
@Composable
private fun AllDone() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(240.dp)
                .background(TotPocketColors.Surface, CircleShape)
                .border(TotPocketDimens.CardOutline, TotPocketColors.Outline, CircleShape)
                .semantics { contentDescription = "All done" },
            contentAlignment = Alignment.Center,
        ) {
            Text("🌟", fontSize = 120.sp)
        }
    }
}

@Preview
@Composable
private fun ShapeMatchContentPreview() {
    val round = ShapeRound(
        holes = listOf(ShapeKind.Circle, ShapeKind.Star, ShapeKind.Heart),
        tray = listOf(ShapeKind.Heart, ShapeKind.Circle, ShapeKind.Star),
    )
    TotPocketTheme {
        ShapeMatchContent(
            ShapeMatchUiState(round, placed = setOf(ShapeKind.Star), phase = ShapeMatchPhase.Playing),
            onAction = {},
            onHome = {},
        )
    }
}
