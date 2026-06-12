package com.winasde.apps

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.winasde.apps.data.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class WebViewStateViewModel(application: Application) : AndroidViewModel(application) {
    sealed class AppState {
        data object Loading : AppState()
        data class WebView(val url: String) : AppState()
        data object NormalApp : AppState()
    }

    private val firestoreRepository = FirestoreRepository()
    private val _appState = MutableStateFlow<AppState>(AppState.Loading)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        checkUrl()
    }

    private fun checkUrl() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val prefs = context.getSharedPreferences("webview_prefs", Context.MODE_PRIVATE)
            val webViewPrefs = context.getSharedPreferences("webview_cache", Context.MODE_PRIVATE)
            val cachedUrl = prefs.getString("cached_url", null)
            val cachedFinalUrl = webViewPrefs.getString("cached_final_url", null)
            val fallbackUrl = cachedFinalUrl.takeIf { !it.isNullOrBlank() }
                ?: cachedUrl.takeIf { !it.isNullOrBlank() }

            try {
                val url = withTimeoutOrNull(10_000L) {
                    firestoreRepository.getWebViewUrl()
                }
                when {
                    !url.isNullOrBlank() -> {
                        prefs.edit().putString("cached_url", url).apply()
                        _appState.value = AppState.WebView(url)
                    }
                    !fallbackUrl.isNullOrBlank() -> {
                        _appState.value = AppState.WebView(fallbackUrl)
                    }
                    else -> {
                        _appState.value = AppState.NormalApp
                    }
                }
            } catch (_: Exception) {
                if (!fallbackUrl.isNullOrBlank()) {
                    _appState.value = AppState.WebView(fallbackUrl)
                } else {
                    _appState.value = AppState.NormalApp
                }
            }
        }
    }
}
