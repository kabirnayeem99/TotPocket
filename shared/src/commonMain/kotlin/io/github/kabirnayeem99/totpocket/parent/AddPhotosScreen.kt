package io.github.kabirnayeem99.totpocket.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kabirnayeem99.totpocket.LocalAppContainer
import io.github.kabirnayeem99.totpocket.PlatformBackHandler
import io.github.kabirnayeem99.totpocket.media.ImageSource
import io.github.kabirnayeem99.totpocket.media.MediaImage
import io.github.kabirnayeem99.totpocket.online.DownloadQuota
import io.github.kabirnayeem99.totpocket.ui.components.AppScaffold
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerButton

@Composable
fun AddPhotosScreen(onBack: () -> Unit, onHome: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { AddPhotosViewModel(container.onlinePhotoSearch, container.photoDownloads) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    AddPhotosContent(state, viewModel::onAction, onBack, onHome)
}

/**
 * Grown-ups only (behind the PIN). A search box, today's allowance, the results, and the photos
 * already added. Tapping any photo opens it big, with its author and licence, to add or remove it.
 */
@Composable
fun AddPhotosContent(state: AddPhotosUiState, onAction: (AddPhotosAction) -> Unit, onBack: () -> Unit, onHome: () -> Unit) {
    val detail = state.detail
    if (detail != null) {
        PlatformBackHandler(enabled = true) { onAction(AddPhotosAction.CloseDetail) }
        AddPhotosDetailContent(detail, state, onAction, onHome)
        return
    }
    AppScaffold(title = "Add photos", style = SettingsChrome, onBack = onBack, onHome = onHome) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { SearchBox(state, onAction) }
            if (state.searching) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
                }
            } else if (state.searched && state.results.isEmpty()) {
                header("No photos found. Try other words, in English.")
            }
            if (state.results.isNotEmpty()) {
                header("Results — tap one to look at it")
                items(state.results, key = { "result:${it.id}" }) { photo ->
                    Thumb(ImageSource.Url(photo.thumbUrl), photo.title, marked = state.isAdded(photo)) {
                        onAction(AddPhotosAction.OpenResult(photo))
                    }
                }
            }
            if (state.added.isNotEmpty()) {
                header("Added photos (${state.added.size}) — tap one to remove it")
                items(state.added.asReversed(), key = { "added:${it.sourceId}" }) { added ->
                    Thumb(added.photo.thumb, added.photo.title, marked = false) { onAction(AddPhotosAction.OpenAdded(added)) }
                }
            }
        }
    }
}

@Composable
private fun SearchBox(state: AddPhotosUiState, onAction: (AddPhotosAction) -> Unit) {
    val focus = LocalFocusManager.current
    val search = {
        focus.clearFocus()
        onAction(AddPhotosAction.Search)
    }
    Group {
        Label(
            "${state.leftToday} of ${DownloadQuota.LIMIT} photos left today",
            "Photos come from Wikimedia Commons. Look at each one before adding it — search results aren't checked for children.",
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onAction(AddPhotosAction.QueryChanged(it)) },
                placeholder = { Text("e.g. elephant, mango, boat") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() }),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, cursorColor = Accent),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            SmallButton("Search", Accent, onClick = search)
        }
        state.notice?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = DarkText, fontSize = 14.sp)
        }
    }
}

private fun LazyGridScope.header(text: String) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(text, color = GreyText, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 2.dp))
    }
}

@Composable
private fun Thumb(source: ImageSource, title: String, marked: Boolean, onClick: () -> Unit) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = title,
        modifier = Modifier.aspectRatio(1f),
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        outline = if (marked) Accent else Color.Transparent,
        outlineWidth = 3.dp,
        playTapSound = false,
        pressedScale = 0.95f,
        minSize = 80.dp,
    ) {
        MediaImage(source, maxPx = 330, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)))
    }
}

/** One photo on black, big enough to judge, with its credit and the one thing you can do with it. */
@Composable
private fun AddPhotosDetailContent(detail: AddPhotosDetail, state: AddPhotosUiState, onAction: (AddPhotosAction) -> Unit, onHome: () -> Unit) {
    val (image, title, credit) = when (detail) {
        is AddPhotosDetail.Result -> Triple(ImageSource.Url(detail.photo.imageUrl), detail.photo.title, "Photo: ${detail.photo.author} · ${detail.photo.license}")
        is AddPhotosDetail.Added -> Triple(detail.photo.photo.image, detail.photo.photo.title, detail.photo.photo.credit.orEmpty())
    }
    AppScaffold(title = "", style = SettingsChrome, onBack = { onAction(AddPhotosAction.CloseDetail) }, onHome = onHome) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MediaImage(
                image,
                maxPx = 1080,
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black),
                contentScale = ContentScale.Fit,
                placeholder = Color.Black,
            )
            Text(title, color = DarkText, fontSize = 18.sp, fontWeight = FontWeight.Medium, maxLines = 2)
            Text(credit, color = GreyText, fontSize = 13.sp, maxLines = 2)
            when (detail) {
                is AddPhotosDetail.Result -> when {
                    state.isAdded(detail.photo) -> Text("Already added.", color = GreyText, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    state.saving -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
                    state.leftToday <= 0 -> Text("That's today's ${DownloadQuota.LIMIT} photos. You can add more tomorrow.", color = GreyText, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    else -> WideButton("Add to Photos (${state.leftToday} left today)", Accent) { onAction(AddPhotosAction.AddOpened) }
                }
                is AddPhotosDetail.Added -> WideButton("Remove from Photos", Color(0xFFF23B3B)) { onAction(AddPhotosAction.RemoveOpened) }
            }
        }
    }
}

@Composable
private fun SmallButton(text: String, background: Color, onClick: () -> Unit) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = text,
        shape = RoundedCornerShape(14.dp),
        color = background,
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        playTapSound = false,
        pressedScale = 0.96f,
        minSize = 56.dp,
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp))
    }
}
