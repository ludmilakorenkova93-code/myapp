package com.winasde.apps

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.TypedValue
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AdvancedWebViewScreen(
    initialUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("webview_cache", Context.MODE_PRIVATE) }
    val cachedUrl = remember { prefs.getString("cached_final_url", "") ?: "" }
    var currentUrl by remember { mutableStateOf(if (cachedUrl.isNotBlank()) cachedUrl else initialUrl) }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var pendingPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val pickMultipleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        filePathCallback?.onReceiveValue(uris.toTypedArray())
        filePathCallback = null
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val result = if (success && cameraImageUri != null) arrayOf(cameraImageUri!!) else emptyArray()
        filePathCallback?.onReceiveValue(result)
        filePathCallback = null
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        filePathCallback?.onReceiveValue(if (uri != null) arrayOf(uri) else emptyArray())
        filePathCallback = null
    }

    val requestWritePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val requestWebViewPermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        val request = pendingPermissionRequest ?: return@rememberLauncherForActivityResult
        pendingPermissionRequest = null

        val grantedResources = request.resources.orEmpty().mapNotNull { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE ->
                    resource.takeIf { grantResults[Manifest.permission.CAMERA] == true }
                PermissionRequest.RESOURCE_AUDIO_CAPTURE ->
                    resource.takeIf { grantResults[Manifest.permission.RECORD_AUDIO] == true }
                else -> null
            }
        }.toTypedArray()

        if (grantedResources.isNotEmpty()) {
            request.grant(grantedResources)
        } else {
            request.deny()
        }
    }

    fun grantWebViewPermissions(ctx: Context, request: PermissionRequest) {
        val resources = request.resources.orEmpty()
        val permissionsToRequest = buildList {
            if (
                PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.CAMERA)
            }
            if (
                PermissionRequest.RESOURCE_AUDIO_CAPTURE in resources &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.RECORD_AUDIO)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            pendingPermissionRequest?.deny()
            pendingPermissionRequest = request
            requestWebViewPermissions.launch(permissionsToRequest.toTypedArray())
            return
        }

        val grantedResources = resources.filter { resource ->
            resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
                resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE
        }.toTypedArray()

        if (grantedResources.isNotEmpty()) {
            request.grant(grantedResources)
        } else {
            request.deny()
        }
    }

    fun startDownload(ctx: Context, url: String, contentDisposition: String?, mimeType: String?, userAgent: String?) {
        try {
            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri)
            val cookies = try { CookieManager.getInstance().getCookie(url) } catch (_: Throwable) { null }
            if (!cookies.isNullOrBlank()) request.addRequestHeader("Cookie", cookies)
            if (!userAgent.isNullOrBlank()) request.addRequestHeader("User-Agent", userAgent)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setAllowedOverMetered(true)
            request.setAllowedOverRoaming(true)
            val filename = URLUtil.guessFileName(url, contentDisposition, mimeType)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            @Suppress("DEPRECATION")
            request.allowScanningByMediaScanner()
            dm.enqueue(request)
        } catch (_: Throwable) {
            try {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Throwable) {
            }
        }
    }

    fun dpToPx(ctx: Context, dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            ctx.resources.displayMetrics
        ).toInt()
    }

    AndroidView(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.systemBars)
            .fillMaxSize(),
        factory = { ctx ->
            val container = FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val wv = WebView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(true)
                settings.allowContentAccess = true
                settings.allowFileAccess = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    settings.safeBrowsingEnabled = true
                }
                CookieManager.getInstance().setAcceptCookie(true)
                try {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                } catch (_: Throwable) {
                }

                setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                    if (Build.VERSION.SDK_INT < 29) {
                        requestWritePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    startDownload(ctx, url, contentDisposition, mimeType, userAgent)
                })

                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) {
                        grantWebViewPermissions(ctx, request)
                    }

                    override fun onPermissionRequestCanceled(request: PermissionRequest) {
                        if (pendingPermissionRequest === request) {
                            pendingPermissionRequest = null
                        }
                        super.onPermissionRequestCanceled(request)
                    }

                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallback_: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        filePathCallback = filePathCallback_
                        val accept = fileChooserParams?.acceptTypes?.joinToString(",") ?: "*/*"
                        val allowMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                        if (accept.contains("image") && fileChooserParams?.isCaptureEnabled == true) {
                            val imageUri = androidx.core.content.FileProvider.getUriForFile(
                                ctx,
                                "${ctx.packageName}.fileprovider",
                                java.io.File(ctx.getExternalFilesDir(null), "camera_${System.currentTimeMillis()}.jpg")
                            )
                            cameraImageUri = imageUri
                            takePhotoLauncher.launch(imageUri)
                        } else {
                            val types = if (accept.isBlank()) arrayOf("*/*") else accept.split(",").toTypedArray()
                            if (allowMultiple) {
                                pickMultipleLauncher.launch(types)
                            } else {
                                openDocumentLauncher.launch(types)
                            }
                        }
                        return true
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        val cleaned = url
                            .replace("&&", "&")
                            .replace("?&", "?")
                            .replace("??", "?")
                        currentUrl = cleaned
                        if (cleaned.startsWith("http")) {
                            prefs.edit().putString("cached_final_url", cleaned).apply()
                        }
                        CookieManager.getInstance().flush()
                        super.onPageFinished(view, url)
                    }

                    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                        try {
                            (view.parent as? ViewGroup)?.removeView(view)
                            view.destroy()
                        } catch (_: Throwable) {
                        }
                        webView = null
                        return true
                    }
                }

                loadUrl(currentUrl)
                webView = this
            }

            container.addView(wv)

            container
        },
        update = { container ->
            val wv = container.getChildAt(0) as? WebView
            if (wv != null && wv.url != currentUrl) {
                wv.loadUrl(currentUrl)
            }
        },
        onRelease = { container ->
            val wv = container.getChildAt(0) as? WebView
            try {
                wv?.stopLoading()
            } catch (_: Throwable) {
            }
            try {
                wv?.destroy()
            } catch (_: Throwable) {
            }
        }
    )

    BackHandler(enabled = true) {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            (context as? Activity)?.moveTaskToBack(true)
        }
    }
}
