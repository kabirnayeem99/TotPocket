package io.github.kabirnayeem99.totpocket.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kabirnayeem99.totpocket.LocalAppContainer
import io.github.kabirnayeem99.totpocket.ui.components.Emoji
import io.github.kabirnayeem99.totpocket.ui.components.PictureTile
import io.github.kabirnayeem99.totpocket.ui.components.RoundIconButton
import io.github.kabirnayeem99.totpocket.ui.components.SectionCard
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerGrid
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerScaffold
import io.github.kabirnayeem99.totpocket.ui.components.isLandscape
import io.github.kabirnayeem99.totpocket.ui.icons.TotPocketIcons
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketColors
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketTheme

/** Three big cards: Animals, Nature, Flowers. */
@Composable
fun GalleryCategoriesScreen(
    onHome: () -> Unit,
    onOpenCategory: (categoryId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ToddlerScaffold(onHome = onHome, modifier = modifier, background = TotPocketColors.BlueTint) {
        ToddlerGrid(
            items = GalleryCatalog.categories,
            portraitColumns = 1,
            landscapeColumns = 3,
        ) { category, cellModifier ->
            SectionCard(
                onClick = { onOpenCategory(category.id) },
                label = category.name,
                color = TotPocketColors.Blue,
                contentColor = TotPocketColors.OnDark,
                modifier = cellModifier,
            ) {
                Emoji(category.emoji)
            }
        }
    }
}

@Composable
fun SoundGridScreen(
    categoryId: String,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "sound-grid-$categoryId") {
        SoundGridViewModel(GalleryCatalog.category(categoryId), container.soundPlayer)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    SoundGridContent(state = state, onAction = viewModel::onAction, onHome = onHome, modifier = modifier)
}

@Composable
fun SoundGridContent(
    state: SoundGridUiState,
    onAction: (SoundGridAction) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToddlerScaffold(
        onHome = onHome,
        modifier = modifier,
        background = TotPocketColors.BlueTint,
        accessory = { PageDots(page = state.page, count = state.pageCount) },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val grid: @Composable (Modifier) -> Unit = { gridModifier ->
                Box(gridModifier) {
                    ToddlerGrid(items = state.tiles, rows = 3) { item, cellModifier ->
                        PictureTile(
                            onClick = { onAction(SoundGridAction.TileTapped(item.id)) },
                            emoji = item.emoji,
                            contentDescription = item.name,
                            background = TotPocketColors.Surface,
                            highlightColor = TotPocketColors.Blue,
                            highlighted = item.id == state.playingId,
                            modifier = cellModifier,
                        )
                    }
                }
            }
            val previous: @Composable () -> Unit = {
                PageArrow(visible = state.hasPrevious, isNext = false) { onAction(SoundGridAction.PreviousPage) }
            }
            val next: @Composable () -> Unit = {
                PageArrow(visible = state.hasNext, isNext = true) { onAction(SoundGridAction.NextPage) }
            }
            val showArrows = state.pageCount > 1

            if (isLandscape(maxWidth, maxHeight)) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    if (showArrows) {
                        previous()
                        Spacer(Modifier.width(TotPocketDimens.TightSpacing))
                    }
                    grid(Modifier.weight(1f).fillMaxHeight())
                    if (showArrows) {
                        Spacer(Modifier.width(TotPocketDimens.TightSpacing))
                        next()
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    grid(Modifier.weight(1f).fillMaxWidth())
                    if (showArrows) {
                        Spacer(Modifier.height(TotPocketDimens.CardSpacing))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            previous()
                            next()
                        }
                    }
                }
            }
        }
    }
}

/** A big arrow; at the first/last page it disappears instead of greying out. */
@Composable
private fun PageArrow(visible: Boolean, isNext: Boolean, onClick: () -> Unit) {
    if (!visible) {
        Spacer(Modifier.size(TotPocketDimens.MinTouchTarget + 8.dp))
        return
    }
    RoundIconButton(
        onClick = onClick,
        icon = if (isNext) TotPocketIcons.ArrowRight else TotPocketIcons.ArrowLeft,
        contentDescription = if (isNext) "Next pictures" else "Previous pictures",
        color = TotPocketColors.Blue,
        iconTint = TotPocketColors.OnDark,
        size = TotPocketDimens.MinTouchTarget + 8.dp,
    )
}

@Composable
private fun PageDots(page: Int, count: Int) {
    if (count <= 1) return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(if (index == page) 22.dp else 16.dp)
                    .clip(CircleShape)
                    .background(if (index == page) TotPocketColors.Blue else TotPocketColors.Surface)
                    .border(3.dp, TotPocketColors.Outline, CircleShape),
            )
        }
    }
}

@Preview
@Composable
private fun SoundGridContentPreview() {
    val items = GalleryCatalog.Animals.items
    TotPocketTheme {
        SoundGridContent(
            state = SoundGridUiState("Animals", page = 0, pageCount = 2, tiles = items.take(6), playingId = items[1].id),
            onAction = {},
            onHome = {},
        )
    }
}
