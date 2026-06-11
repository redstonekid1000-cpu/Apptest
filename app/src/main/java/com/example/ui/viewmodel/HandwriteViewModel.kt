package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HandwritingRenderer
import com.example.data.InkColor
import com.example.data.PaperType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class HandwriteViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val renderer = HandwritingRenderer()

    // 1. Core Config States
    private val _textInput = MutableStateFlow(
        "// Let's write some beautiful Kotlin code!\n" +
        "fun main() {\n" +
        "    println(\"Hello, Handwrite!\")\n" +
        "    for (i in 1..4) {\n" +
        "        val smile = \"✍️\".repeat(i)\n" +
        "        println(\"Love the flow! \${'$'}smile\")\n" +
        "    }\n" +
        "}\n\n" +
        "// Support Hebrew Cursive script: \n" +
        "כתב יד מדהים בעברית ובאנגלית ביחד :)\n" +
        "המרת קוד וטקסט לתמונה מרשימה."
    )
    val textInput: StateFlow<String> = _textInput.asStateFlow()

    private val _fontSize = MutableStateFlow(22f)
    val fontSize: StateFlow<Float> = _fontSize.asStateFlow()

    private val _imperfectionLevel = MutableStateFlow(0.45f)
    val imperfectionLevel: StateFlow<Float> = _imperfectionLevel.asStateFlow()

    private val _paperType = MutableStateFlow(PaperType.NOTEBOOK)
    val paperType: StateFlow<PaperType> = _paperType.asStateFlow()

    private val _inkColor = MutableStateFlow(InkColor.BLUE)
    val inkColor: StateFlow<InkColor> = _inkColor.asStateFlow()

    private val _applyLighting = MutableStateFlow(true)
    val applyLighting: StateFlow<Boolean> = _applyLighting.asStateFlow()

    private val _applyPencilBlend = MutableStateFlow(true)
    val applyPencilBlend: StateFlow<Boolean> = _applyPencilBlend.asStateFlow()

    // 2. Network & Font download status
    private val _isDownloadingFonts = MutableStateFlow(false)
    val isDownloadingFonts: StateFlow<Boolean> = _isDownloadingFonts.asStateFlow()

    private val _fontDownloadError = MutableStateFlow<String?>(null)
    val fontDownloadError: StateFlow<String?> = _fontDownloadError.asStateFlow()

    private val _fontsDownloaded = MutableStateFlow(false)
    val fontsDownloaded: StateFlow<Boolean> = _fontsDownloaded.asStateFlow()

    // 3. Render and result states
    private val _isRendering = MutableStateFlow(false)
    val isRendering: StateFlow<Boolean> = _isRendering.asStateFlow()

    private val _generatedBitmap = MutableStateFlow<Bitmap?>(null)
    val generatedBitmap: StateFlow<Bitmap?> = _generatedBitmap.asStateFlow()

    private val _renderError = MutableStateFlow<String?>(null)
    val renderError: StateFlow<String?> = _renderError.asStateFlow()

    // Setup local font storage paths
    private val fontsDir = File(context.cacheDir, "fonts").apply { mkdirs() }
    private val caveatFile = File(fontsDir, "caveat.ttf")
    private val gveretLevinFile = File(fontsDir, "gveret_levin.ttf")

    // Custom uploaded fonts states
    private val _customFontName = MutableStateFlow<String?>(null)
    val customFontName: StateFlow<String?> = _customFontName.asStateFlow()

    private val _customHebrewFontName = MutableStateFlow<String?>(null)
    val customHebrewFontName: StateFlow<String?> = _customHebrewFontName.asStateFlow()

    private val customFontFile = File(fontsDir, "user_custom_font.ttf")
    private val customHebrewFontFile = File(fontsDir, "user_custom_hebrew_font.ttf")

    init {
        // If user already had custom fonts saved, register their custom names
        if (customFontFile.exists()) {
            _customFontName.value = "Custom Font (.ttf)"
        }
        if (customHebrewFontFile.exists()) {
            _customHebrewFontName.value = "Custom Hebrew Font (.ttf)"
        }
        // Automatically check/download required fonts on startup or fall back
        checkAndDownloadFonts()
    }

    fun registerCustomFont(fileUri: android.net.Uri, isHebrew: Boolean, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val destination = if (isHebrew) customHebrewFontFile else customFontFile
                context.contentResolver.openInputStream(fileUri)?.use { input ->
                    FileOutputStream(destination).use { output ->
                        input.copyTo(output)
                    }
                }
                if (isHebrew) {
                    _customHebrewFontName.value = name
                } else {
                    _customFontName.value = name
                }
                withContext(Dispatchers.Main) {
                    triggerRender()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _renderError.value = "Failed to load custom font: ${e.localizedMessage}"
                }
            }
        }
    }

    fun clearCustomFont(isHebrew: Boolean) {
        val file = if (isHebrew) customHebrewFontFile else customFontFile
        if (file.exists()) {
            file.delete()
        }
        if (isHebrew) {
            _customHebrewFontName.value = null
        } else {
            _customFontName.value = null
        }
        triggerRender()
    }

    fun setTextInput(input: String) {
        _textInput.value = input
    }

    fun setFontSize(size: Float) {
        _fontSize.value = size
    }

    fun setImperfectionLevel(level: Float) {
        _imperfectionLevel.value = level
    }

    fun setPaperType(type: PaperType) {
        _paperType.value = type
    }

    fun setInkColor(color: InkColor) {
        _inkColor.value = color
    }

    fun setApplyLighting(apply: Boolean) {
        _applyLighting.value = apply
    }

    fun setApplyPencilBlend(apply: Boolean) {
        _applyPencilBlend.value = apply
    }

    fun checkAndDownloadFonts() {
        if (caveatFile.exists() && gveretLevinFile.exists()) {
            _fontsDownloaded.value = true
            // Run automatic first render so screen is loaded with a beautiful starting preview
            triggerRender()
            return
        }

        viewModelScope.launch {
            _isDownloadingFonts.value = true
            _fontDownloadError.value = null
            try {
                withContext(Dispatchers.IO) {
                    val client = OkHttpClient()

                    // Download Caveat Font if missing
                    if (!caveatFile.exists()) {
                        downloadFontFile(
                            client,
                            "https://raw.githubusercontent.com/google/fonts/main/ofl/caveat/Caveat-Regular.ttf",
                            caveatFile
                        )
                    }

                    // Download Gveret Levin Hebrew Font if missing
                    if (!gveretLevinFile.exists()) {
                        downloadFontFile(
                            client,
                            "https://raw.githubusercontent.com/google/fonts/main/ofl/gveretlevin/GveretLevin-Regular.ttf",
                            gveretLevinFile
                        )
                    }
                }
                _fontsDownloaded.value = true
                triggerRender()
            } catch (e: Exception) {
                _fontDownloadError.value = "Failed to download cursive fonts: ${e.localizedMessage}. Using system cursive fonts fallback."
                _fontsDownloaded.value = true // Support graceful offline fallback
                triggerRender()
            } finally {
                _isDownloadingFonts.value = false
            }
        }
    }

    private fun downloadFontFile(client: OkHttpClient, url: String, targetFile: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            val body = response.body ?: throw Exception("Empty response body")
            FileOutputStream(targetFile).use { outStream ->
                body.byteStream().copyTo(outStream)
            }
        }
    }

    fun triggerRender() {
        val text = _textInput.value
        if (text.isBlank()) return

        val fontSizeSp = _fontSize.value
        val imperfection = _imperfectionLevel.value
        val paper = _paperType.value
        val ink = _inkColor.value
        val lighting = _applyLighting.value
        val blend = _applyPencilBlend.value

        val caveatPath = when {
            customFontFile.exists() -> customFontFile.absolutePath
            caveatFile.exists() -> caveatFile.absolutePath
            else -> null
        }
        val gveretLevinPath = when {
            customHebrewFontFile.exists() -> customHebrewFontFile.absolutePath
            gveretLevinFile.exists() -> gveretLevinFile.absolutePath
            else -> null
        }

        viewModelScope.launch {
            _isRendering.value = true
            _renderError.value = null
            try {
                val bitmapResult = withContext(Dispatchers.Default) {
                    renderer.render(
                        text = text,
                        context = context,
                        fontSizeSp = fontSizeSp,
                        imperfectionLevel = imperfection,
                        paperType = paper,
                        inkColor = ink,
                        applyLighting = lighting,
                        applyPencilBlend = blend,
                        fontPath = caveatPath,
                        hebrewFontPath = gveretLevinPath
                    )
                }
                _generatedBitmap.value = bitmapResult
            } catch (e: Exception) {
                _renderError.value = "Rendering failed: ${e.localizedMessage}"
            } finally {
                _isRendering.value = false
            }
        }
    }
}
