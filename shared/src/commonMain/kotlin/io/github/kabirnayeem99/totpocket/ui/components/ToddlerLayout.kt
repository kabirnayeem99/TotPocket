package io.github.kabirnayeem99.totpocket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketColors
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens

/** True when the available space is wider than tall. */
fun isLandscape(maxWidth: Dp, maxHeight: Dp): Boolean = maxWidth > maxHeight

/**
 * Standard frame for every screen below Home: the Home button in a fixed corner (top-start in
 * portrait, left column in landscape), an optional [accessory] opposite it, and [content] filling
 * the rest. Nothing scrolls.
 */
@Composable
fun ToddlerScaffold(
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = TotPocketColors.Background,
    accessory: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .padding(TotPocketDimens.ScreenPadding),
    ) {
        if (isLandscape(maxWidth, maxHeight)) {
            Row(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxHeight().width(TotPocketDimens.HomeButtonSize),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HomeButton(onHome)
                    Spacer(Modifier.weight(1f))
                    accessory()
                }
                Spacer(Modifier.width(TotPocketDimens.CardSpacing))
                Box(Modifier.weight(1f).fillMaxHeight(), content = content)
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(TotPocketDimens.HomeButtonSize),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HomeButton(onHome)
                    Spacer(Modifier.weight(1f))
                    accessory()
                }
                Spacer(Modifier.height(TotPocketDimens.CardSpacing))
                Box(Modifier.weight(1f).fillMaxWidth(), content = content)
            }
        }
    }
}

/**
 * Splits the available space evenly between [items]: [portraitColumns] wide in portrait,
 * [landscapeColumns] in landscape, always [rows] rows tall so tiles keep one size across pages.
 */
@Composable
fun <T> ToddlerGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    portraitColumns: Int = 2,
    landscapeColumns: Int = 3,
    rows: Int? = null,
    spacing: Dp = TotPocketDimens.CardSpacing,
    itemContent: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val columns = if (isLandscape(maxWidth, maxHeight)) landscapeColumns else portraitColumns
        val rowCount = rows?.let { if (isLandscape(maxWidth, maxHeight)) (it * portraitColumns + columns - 1) / columns else it }
            ?: ((items.size + columns - 1) / columns)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(spacing)) {
            repeat(rowCount) { row ->
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    repeat(columns) { column ->
                        val item = items.getOrNull(row * columns + column)
                        val cell = Modifier.weight(1f).fillMaxHeight()
                        if (item != null) itemContent(item, cell) else Spacer(cell)
                    }
                }
            }
        }
    }
}
