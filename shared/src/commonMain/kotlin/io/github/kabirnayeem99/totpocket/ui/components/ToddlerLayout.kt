package io.github.kabirnayeem99.totpocket.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens

/** True when the available space is wider than tall. */
fun isLandscape(maxWidth: Dp, maxHeight: Dp): Boolean = maxWidth > maxHeight

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
