package com.example.chaintorquenative.ui.screens

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

private val ViewerBg      = Color(0xFF0F172A)
private val ViewerSurface = Color(0xFF1E293B)
private val ViewerAccent  = Color(0xFF6366F1)
private val ViewerGradient = Color(0xFF1E1B4B)

// JS bridge — @JavascriptInterface runs on a background thread,
// so we MUST post back to the main thread before touching Compose state.
private class ModelViewerBridge(
    private val mainHandler: Handler,
    private val onModelLoaded: () -> Unit,
    private val onModelError:  () -> Unit
) {
    @JavascriptInterface fun onLoad()  { mainHandler.post { onModelLoaded() } }
    @JavascriptInterface fun onError() { mainHandler.post { onModelError()  } }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelViewerScreen(
    modelUrl: String,
    title: String = "3D Model",
    onBack: () -> Unit
) {
    val context    = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val scope       = rememberCoroutineScope()

    var isLoading  by remember { mutableStateOf(true) }
    var loadError  by remember { mutableStateOf(false) }
    var modelReady by remember { mutableStateOf(false) }  // true once model-viewer fires 'load'
    var reloadKey  by remember { mutableStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val sanitizedUrl = modelUrl.trim()
    val hasModel     = sanitizedUrl.isNotBlank()
    val viewerHtml   = remember(sanitizedUrl) { buildModelViewerHtml(sanitizedUrl) }

    val bridge = remember {
        ModelViewerBridge(
            mainHandler   = mainHandler,
            onModelLoaded = { isLoading = false; loadError = false; modelReady = true },
            onModelError  = { isLoading = false; loadError = true  }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ViewerGradient, ViewerBg)))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top App Bar ──────────────────────────────────────────────────
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "Pinch to zoom  •  Drag to rotate",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            reloadKey++
                            isLoading = true
                            loadError = false
                            modelReady = false
                        }
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Reload",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ViewerSurface
                )
            )

            // ── Viewer Body ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    !hasModel -> NoModelPlaceholder()

                    loadError -> ModelLoadError(
                        onRetry = {
                            reloadKey++
                            isLoading = true
                            loadError = false
                            webViewRef?.reload()
                        }
                    )

                    else -> {
                        // Launch Scene Viewer directly!
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ViewInAr,
                                contentDescription = "3D Viewer",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Ready to View",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Launch the 3D viewer to see this model in 3D or AR.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            val localContext = LocalContext.current
                            Button(
                                onClick = {
                                    val sceneViewerIntent = Intent(Intent.ACTION_VIEW)
                                    val uri = Uri.parse("https://arvr.google.com/scene-viewer/1.0").buildUpon()
                                        .appendQueryParameter("file", modelUrl)
                                        .appendQueryParameter("mode", "3d_preferred")
                                        .appendQueryParameter("title", title)
                                        .build()
                                    sceneViewerIntent.data = uri
                                    sceneViewerIntent.setPackage("com.google.ar.core")
                                    try {
                                        localContext.startActivity(sceneViewerIntent)
                                    } catch (e: ActivityNotFoundException) {
                                        // Fallback if AR Core isn't installed
                                        sceneViewerIntent.setPackage(null)
                                        localContext.startActivity(sceneViewerIntent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ViewerAccent),
                                modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
                            ) {
                                Text("Open 3D Viewer", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── AR + Hint Bar ─────────────────────────────────────────────
            if (hasModel && !loadError) {
                Surface(modifier = Modifier.fillMaxWidth(), color = ViewerSurface) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // AR Button — native, launches Google Scene Viewer directly (no WebView JS)
                        Button(
                            onClick = {
                                val sceneViewerUri = Uri.parse(
                                    "https://arvr.google.com/scene-viewer/1.0" +
                                    "?file=${Uri.encode(sanitizedUrl)}" +
                                    "&mode=ar_preferred" +
                                    "&title=${Uri.encode(title)}"
                                )
                                val intent = Intent(Intent.ACTION_VIEW, sceneViewerUri).apply {
                                    setPackage("com.google.android.googlequicksearchbox")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: ActivityNotFoundException) {
                                    // Scene Viewer not installed — try without package restriction
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, sceneViewerUri))
                                    } catch (e2: ActivityNotFoundException) {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW,
                                                Uri.parse("market://details?id=com.google.ar.core"))
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ViewerAccent)
                        ) {
                            Icon(Icons.Filled.ViewInAr, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View in AR", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        // Gesture hints
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            HintChip("👆 Drag", "Rotate")
                            HintChip("🤏 Pinch", "Zoom")
                            HintChip("✌️ Two-finger", "Pan")
                        }
                    }
                }
            }
        }
    }
}

// ── Helper Composables ───────────────────────────────────────────────────────

@Composable
private fun HintChip(gesture: String, action: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = gesture,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.75f)
        )
        Text(
            text = action,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun NoModelPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ViewerBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("📦", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No 3D model available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This listing does not have\na 3D model file attached.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModelLoadError(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ViewerBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("⚠️", style = MaterialTheme.typography.displayMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Failed to load 3D model",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Check your connection and\nmake sure the file is still available.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = ViewerAccent)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── HTML Builder ─────────────────────────────────────────────────────────────

private fun buildModelViewerHtml(modelUrl: String): String = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    html, body { width: 100%; height: 100%; overflow: hidden; background: #0F172A; }
    model-viewer {
      width: 100%;
      height: 100%;
      --poster-color: #0F172A;
      background-color: #0F172A;
    }
    #loading-bar {
      position: fixed; top: 0; left: 0;
      width: 0%; height: 3px;
      background: #6366F1;
      transition: width 0.3s ease;
      z-index: 10;
    }
  </style>
</head>
<body>
  <div id="loading-bar"></div>
  <!-- Non-module IIFE build works reliably in Android WebView loadDataWithBaseURL -->
  <script type="module" src="https://ajax.googleapis.com/ajax/libs/model-viewer/3.4.0/model-viewer.min.js"></script>

  <model-viewer
    id="viewer"
    src="$modelUrl"
    alt="3D model"
    camera-controls
    touch-action="pan-y"
    auto-rotate
    auto-rotate-delay="1200"
    rotation-per-second="20deg"
    shadow-intensity="1"
    shadow-softness="0.8"
    exposure="1.0"
    tone-mapping="commerce"
    environment-image="neutral"
    ar
    ar-modes="scene-viewer webxr"
    ar-scale="auto"
    ar-placement="floor"
    xr-environment
  ></model-viewer>

  <script>
    var viewer = document.getElementById('viewer');
    var bar    = document.getElementById('loading-bar');

    viewer.addEventListener('progress', function(e) {
      bar.style.width = (e.detail.totalProgress * 100) + '%';
    });

    viewer.addEventListener('load', function() {
      bar.style.width = '100%';
      setTimeout(function() { bar.style.opacity = '0'; }, 400);
      if (window.Android) { Android.onLoad(); }
    });

    viewer.addEventListener('error', function() {
      bar.style.backgroundColor = '#EF4444';
      bar.style.width = '100%';
      if (window.Android) { Android.onError(); }
    });
  </script>
</body>
</html>
""".trimIndent()
