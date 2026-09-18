package io.github.kabirnayeem99.totpocket.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens

/** A big coloured card with a glyph and an (adult-facing, optional-to-read) label. */
@Composable
fun SectionCard(
    onClick: () -> Unit,
    label: String,
    color: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    picture: @Composable () -> Unit,
) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = label,
        modifier = modifier,
        color = color,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            picture()
            Spacer(Modifier.height(TotPocketDimens.TightSpacing))
            Text(
                text = label,
                color = contentColor,
                fontSize = TotPocketDimens.LabelSize,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
