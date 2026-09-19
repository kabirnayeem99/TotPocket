package io.github.kabirnayeem99.totpocket.parent

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.kabirnayeem99.totpocket.online.DownloadResult
import io.github.kabirnayeem99.totpocket.online.DownloadedPhoto
import io.github.kabirnayeem99.totpocket.online.OnlinePhoto
import io.github.kabirnayeem99.totpocket.online.OnlinePhotoSearch
import io.github.kabirnayeem99.totpocket.online.PhotoDownloads
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** What the grown-up is looking at full-screen: a search result, or a photo already added. */
@Immutable
sealed interface AddPhotosDetail {
    data class Result(val photo: OnlinePhoto) : AddPhotosDetail
    data class Added(val photo: DownloadedPhoto) : AddPhotosDetail
}

@Immutable
data class AddPhotosUiState(
    val query: String = "",
    val searching: Boolean = false,
    val results: List<OnlinePhoto> = emptyList(),
    /** A search finished, so an empty [results] means nothing was found. */
    val searched: Boolean = false,
    val added: List<DownloadedPhoto> = emptyList(),
    val leftToday: Int = 0,
    val detail: AddPhotosDetail? = null,
    val saving: Boolean = false,
    /** One line about the last thing that happened, e.g. "Added to Photos". */
    val notice: String? = null,
) {
    fun isAdded(photo: OnlinePhoto): Boolean = added.any { it.sourceId == photo.id }
}

sealed interface AddPhotosAction {
    data class QueryChanged(val text: String) : AddPhotosAction
    data object Search : AddPhotosAction
    data class OpenResult(val photo: OnlinePhoto) : AddPhotosAction
    data class OpenAdded(val photo: DownloadedPhoto) : AddPhotosAction
    data object CloseDetail : AddPhotosAction
    data object AddOpened : AddPhotosAction
    data object RemoveOpened : AddPhotosAction
}

/**
 * The grown-ups' "Add photos" screen: search Wikimedia Commons, look at a photo full-size, and add
 * it to Photos — at most [io.github.kabirnayeem99.totpocket.online.DownloadQuota.LIMIT] a day.
 * Added photos can be removed again (that doesn't give the download back).
 */
class AddPhotosViewModel(
    private val search: OnlinePhotoSearch,
    private val downloads: PhotoDownloads,
) : ViewModel() {

    private val screen = MutableStateFlow(AddPhotosUiState())
    private var searchJob: Job? = null

    val state: StateFlow<AddPhotosUiState> = combine(screen, downloads.photos, downloads.leftToday) { screen, added, left ->
        screen.copy(added = added, leftToday = left)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddPhotosUiState())

    init {
        downloads.refresh()
    }

    fun onAction(action: AddPhotosAction) {
        when (action) {
            is AddPhotosAction.QueryChanged -> screen.update { it.copy(query = action.text) }
            AddPhotosAction.Search -> runSearch()
            is AddPhotosAction.OpenResult -> screen.update { it.copy(detail = AddPhotosDetail.Result(action.photo), notice = null) }
            is AddPhotosAction.OpenAdded -> screen.update { it.copy(detail = AddPhotosDetail.Added(action.photo), notice = null) }
            AddPhotosAction.CloseDetail -> screen.update { it.copy(detail = null) }
            AddPhotosAction.AddOpened -> addOpened()
            AddPhotosAction.RemoveOpened -> removeOpened()
        }
    }

    private fun runSearch() {
        val query = screen.value.query.trim()
        if (query.isEmpty()) return
        searchJob?.cancel()
        screen.update { it.copy(searching = true, notice = null) }
        searchJob = viewModelScope.launch {
            try {
                val found = search.search(query)
                screen.update { it.copy(searching = false, searched = true, results = found) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                screen.update { it.copy(searching = false, notice = "Couldn't reach Wikimedia Commons. Check the internet and try again.") }
            }
        }
    }

    private fun addOpened() {
        val photo = (screen.value.detail as? AddPhotosDetail.Result)?.photo ?: return
        if (screen.value.saving) return
        screen.update { it.copy(saving = true) }
        viewModelScope.launch {
            val notice = try {
                when (downloads.download(photo)) {
                    DownloadResult.Added -> "Added to Photos, in \"New photos\"."
                    DownloadResult.AlreadyAdded -> "This photo is already added."
                    DownloadResult.LimitReached -> "That's today's photos. You can add more tomorrow."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Couldn't download the photo. Check the internet and try again."
            }
            screen.update { it.copy(saving = false, detail = null, notice = notice) }
        }
    }

    private fun removeOpened() {
        val photo = (screen.value.detail as? AddPhotosDetail.Added)?.photo ?: return
        viewModelScope.launch {
            downloads.remove(photo.sourceId)
            screen.update { it.copy(detail = null, notice = "Removed from Photos.") }
        }
    }
}
