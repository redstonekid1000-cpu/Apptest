package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import java.io.File
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt

enum class PaperType {
    NOTEBOOK,
    GRID,
    LEGAL_PAD,
    BLANK_PARCHMENT
}

enum class InkColor(val colorVal: Int) {
    BLUE(0xFF1E3A8A.toInt()),     // Fine blue ink
    BLACK(0xFF1F2937.toInt()),    // Deep black gel
    RED(0xFF991B1B.toInt()),      // Edit red
    PENCIL(0xFF4B5563.toInt())    // Charcoal pencil grey
}

class HandwritingRenderer {

    // Target sizes at ~150 DPI for rendering to match PAGE_SIZES["A4"] = (1240, 1754)
    val width = 1240
    val height = 1754

    // Pencil charcoal calculation matching TEXT_BRIGHTNESS = 20
    private fun getPencilColor(brightness: Int = 20): Int {
        val base = (30 + brightness * 2.25).toInt().coerceIn(0, 255)
        return Color.argb(255, base, base, base)
    }

    private fun isHebrewChar(c: Char): Boolean {
        return c in '\u0590'..'\u05FF'
    }

    private fun isRtlLine(line: String): Boolean {
        return line.any { isHebrewChar(it) }
    }

    private fun getWordSeed(word: String, lineIdx: Int, wordIdx: Int): Long {
        val seedStr = "${lineIdx}_${wordIdx}_$word"
        return Math.abs(seedStr.hashCode().toLong())
    }

    fun render(
        text: String,
        context: Context,
        fontSizeSp: Float = 24f,
        imperfectionLevel: Float = 0.5f,
        paperType: PaperType = PaperType.NOTEBOOK,
        inkColor: InkColor = InkColor.BLUE,
        applyLighting: Boolean = true,
        applyPencilBlend: Boolean = true,
        fontPath: String? = null,
        hebrewFontPath: String? = null
    ): Bitmap {
        // Compute scaled font sizing to fit A4 layout gracefully
        val baseFontSize = fontSizeSp * (width / 500f) * 0.45f
        val charHeight = (baseFontSize * 1.8f).toInt()
        val notebookLineSpacing = charHeight
        
        val scale = baseFontSize / 32.0f
        val textStartX = (60 * scale).toInt()
        val textStartY = (100 * scale).toInt()
        val redLineX = (48 * scale).toInt()

        // 1. Setup fonts (catching Throwable to avoid native crashes on faulty TTF files)
        val standardTypeface = if (fontPath != null && File(fontPath).exists()) {
            try {
                Typeface.createFromFile(File(fontPath))
            } catch (e: Throwable) {
                Typeface.create(Typeface.create("serif", Typeface.NORMAL), Typeface.NORMAL)
            }
        } else {
            Typeface.create(Typeface.create("serif", Typeface.NORMAL), Typeface.NORMAL)
        }

        val hebrewTypeface = if (hebrewFontPath != null && File(hebrewFontPath).exists()) {
            try {
                Typeface.createFromFile(File(hebrewFontPath))
            } catch (e: Throwable) {
                Typeface.create(Typeface.create("cursive", Typeface.NORMAL), Typeface.NORMAL)
            }
        } else {
            Typeface.create(Typeface.create("cursive", Typeface.NORMAL), Typeface.NORMAL)
        }

        // 2. Create high-res canvas background image (paper and lines)
        val canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)

        // 3. Render physical ruling lines & paper type backgrounds
        drawBackground(canvas, paperType, textStartY, notebookLineSpacing, redLineX)

        if (paperType != PaperType.BLANK_PARCHMENT) {
            drawPaperTexture(canvas, java.util.Random(42))
        }

        // Apply lighting and shadows gradient on BLANK canvas before drawing text matching python script
        if (applyLighting) {
            applyLightingAndShadows(canvas)
        }

        // 4. Create separate transparent bitmap specifically to draw custom text characters elegantly
        val textBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val textCanvas = Canvas(textBitmap)

        val isCustomFontUsed = (fontPath != null && fontPath.contains("user_custom_font")) || 
                               (hebrewFontPath != null && hebrewFontPath.contains("user_custom_hebrew_font"))

        // Setup base paint for rendering text
        val baseInkColor = if (inkColor == InkColor.PENCIL) getPencilColor(20) else inkColor.colorVal
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = baseInkColor
            style = Paint.Style.FILL
            textSize = baseFontSize
            // Apply slight blur for soft analog ink pen look matching python's Gaussian radius 0.5
            // Use extremely sharp settings for custom fonts so they do not blur off
            if (applyPencilBlend && !isCustomFontUsed) {
                maskFilter = BlurMaskFilter(1.2f, BlurMaskFilter.Blur.NORMAL)
            } else if (applyPencilBlend && isCustomFontUsed) {
                maskFilter = BlurMaskFilter(0.4f, BlurMaskFilter.Blur.NORMAL)
            }
        }

        // Measure average character width
        val sampleText = "abcdefghijklmnopqrstuvwxyz0123456789., "
        var totalW = 0f
        for (ch in sampleText) {
            val bounds = Rect()
            textPaint.getTextBounds(ch.toString(), 0, 1, bounds)
            totalW += bounds.width()
        }
        val avgCharW = Math.max(12f, totalW / sampleText.length)
        val hebrewAvgCharW = avgCharW * 1.1f

        val lines = text.split("\n")

        // 4. Draft text content onto page
        for (lineIdx in lines.indices) {
            val line = lines[lineIdx]
            val lineSine = sin(lineIdx * 0.6f) * 2.5f * imperfectionLevel
            val globalRand = java.util.Random(lineIdx.toLong() + text.hashCode())
            val lineRand = (globalRand.nextFloat() * 4f - 2f) * imperfectionLevel
            val yBase = textStartY + lineIdx * notebookLineSpacing + (lineSine + lineRand).toInt()

            // Stop printing if bottom padding cutoff is violated
            if (yBase > height - (100 * scale).toInt() - charHeight) {
                break
            }

            val isRtl = isRtlLine(line)

            // Split line into word groups for seeding deterministic randomness
            val words = mutableListOf<Triple<Int, Int, String>>()
            var i = 0
            while (i < line.length) {
                if (line[i] == ' ' || line[i] == '\t') {
                    i++
                    continue
                }
                val start = i
                val sb = StringBuilder()
                while (i < line.length && line[i] != ' ' && line[i] != '\t') {
                    sb.append(line[i])
                    i++
                }
                words.add(Triple(start, i, sb.toString()))
            }

            var wordIdx = 0
            val driftRate = (globalRand.nextFloat() * 0.3f - 0.15f) * imperfectionLevel
            var drift = 0f

            if (isRtl) {
                var x = (width - textStartX).toFloat()
                var charIdx = 0
                var currentWordTuple: Triple<Int, Int, String>? = null
                var wordRng = java.util.Random(lineIdx.toLong())

                var wordSizeScale = 1.0f
                var wordRot = 0f
                var wordSkew = 0f
                var wordVOffset = 0f
                var wordAdvanceMult = 0.95f

                while (charIdx < line.length) {
                    val char = line[charIdx]

                    if (char == ' ') {
                        while (charIdx < line.length && line[charIdx] == ' ') {
                            charIdx++
                        }
                        x -= hebrewAvgCharW * (1.6f + globalRand.nextFloat() * 0.4f)
                        drift = 0f
                        currentWordTuple = null
                        continue
                    }

                    if (char == '\t') {
                        x -= hebrewAvgCharW * 4f
                        charIdx++
                        continue
                    }

                    // Seeding word-based randomizer for consistent spacing/slant
                    if (currentWordTuple == null) {
                        var matchedWord = false
                        for (w in words) {
                            if (charIdx >= w.first && charIdx < w.second) {
                                currentWordTuple = w
                                val seed = getWordSeed(w.third, lineIdx, wordIdx)
                                wordRng = java.util.Random(seed)
                                wordIdx++
                                matchedWord = true
                                break
                            }
                        }
                        if (matchedWord) {
                            // Unified parent traits for the entire word to maintain graphic cohesion
                            wordSizeScale = 1.0f + (wordRng.nextFloat() * 0.12f - 0.06f) * imperfectionLevel
                            wordRot = (wordRng.nextFloat() * 3.5f - 1.75f) * imperfectionLevel
                            wordSkew = (wordRng.nextFloat() * 0.08f - 0.04f) * imperfectionLevel
                            wordVOffset = (wordRng.nextFloat() * 2.5f - 1.25f) * imperfectionLevel
                            wordAdvanceMult = 0.92f + (wordRng.nextFloat() * 0.06f - 0.03f) * imperfectionLevel
                        } else {
                            wordSizeScale = 1.0f
                            wordRot = 0f
                            wordSkew = 0f
                            wordVOffset = 0f
                            wordAdvanceMult = 0.95f
                        }
                    }

                    drift += driftRate + (wordRng.nextFloat() * 0.1f - 0.05f) * imperfectionLevel
                    drift = drift.coerceIn(-4f, 4f)
                    val currentY = yBase + drift.toInt()
                    
                    // Fine character-level jitter applied on top of unified word traits
                    val localSizeVar = (wordRng.nextFloat() * 0.03f - 0.015f) * imperfectionLevel
                    val sizeScale = wordSizeScale + localSizeVar

                    val localRotVar = (wordRng.nextFloat() * 1.5f - 0.75f) * imperfectionLevel
                    val charRot = wordRot + localRotVar

                    val localSkewVar = (wordRng.nextFloat() * 0.02f - 0.01f) * imperfectionLevel
                    val charSkew = wordSkew + localSkewVar

                    val localVVar = (wordRng.nextFloat() * 1.5f - 0.75f) * imperfectionLevel
                    val vJitter = (wordVOffset + localVVar).toInt()

                    // Pressure & pen ink variations
                    val useHebrew = isHebrewChar(char)
                    textPaint.typeface = if (useHebrew) hebrewTypeface else standardTypeface

                    if (applyPencilBlend) {
                        val reduce = if (isCustomFontUsed) (5 * imperfectionLevel).toInt() else (20 * imperfectionLevel).toInt()
                        val alphaBase = 255 - reduce - wordRng.nextInt(15)
                        textPaint.alpha = alphaBase.coerceIn(if (isCustomFontUsed) 220 else 160, 255)

                        // Downstroke letters get slightly bolder paint strokes
                        if (isCustomFontUsed) {
                            textPaint.strokeWidth = 1.2f + (wordRng.nextFloat() * 0.4f * imperfectionLevel)
                            textPaint.style = Paint.Style.FILL_AND_STROKE
                        } else if ("bdfhijkltpqugyBDFHIJKLTPQUGY".contains(char)) {
                            textPaint.strokeWidth = 1f + (wordRng.nextFloat() * 1.0f * imperfectionLevel)
                            textPaint.style = Paint.Style.FILL_AND_STROKE
                        } else {
                            textPaint.strokeWidth = 1f
                            textPaint.style = Paint.Style.FILL
                        }
                    } else {
                        textPaint.alpha = 255
                        if (isCustomFontUsed) {
                            textPaint.strokeWidth = 1.2f
                            textPaint.style = Paint.Style.FILL_AND_STROKE
                        } else {
                            textPaint.strokeWidth = 1f
                            textPaint.style = Paint.Style.FILL
                        }
                    }

                    textPaint.textSize = baseFontSize * sizeScale

                    textCanvas.save()
                    
                    val charWidth = textPaint.measureText(char.toString())
                    val pasteX = x - charWidth
                    val specialHebrewShift = if (char == 'י') (if (isCustomFontUsed) 0f else baseFontSize * 0.1f) else 0f
                    val pasteY = currentY + vJitter - specialHebrewShift

                    textCanvas.translate(pasteX, pasteY)
                    textCanvas.rotate(charRot)
                    
                    val skewMatrix = Matrix()
                    skewMatrix.setSkew(charSkew, 0f)
                    textCanvas.concat(skewMatrix)

                    textCanvas.drawText(char.toString(), 0f, 0f, textPaint)
                    textCanvas.restore()

                    // Horizontal step with a beautifully compact, slightly overlapping or connected cursive margin
                    val spacingVar = (wordRng.nextFloat() * 0.04f - 0.02f) * imperfectionLevel
                    val advance = charWidth * (wordAdvanceMult + spacingVar)
                    x -= advance
                    charIdx++
                }
            } else {
                // LTR Path
                var x = (textStartX + redLineX).toFloat()
                var charIdx = 0
                var currentWordTuple: Triple<Int, Int, String>? = null
                var wordRng = java.util.Random(lineIdx.toLong())

                var wordSizeScale = 1.0f
                var wordRot = 0f
                var wordSkew = 0f
                var wordVOffset = 0f
                var wordAdvanceMult = 1.02f

                while (charIdx < line.length) {
                    val char = line[charIdx]

                    if (char == ' ') {
                        while (charIdx < line.length && line[charIdx] == ' ') {
                            charIdx++
                        }
                        x += avgCharW * (1.6f + globalRand.nextFloat() * 0.4f)
                        drift = 0f
                        currentWordTuple = null
                        continue
                    }

                    if (char == '\t') {
                        x += avgCharW * 4f
                        charIdx++
                        continue
                    }

                    if (currentWordTuple == null) {
                        var matchedWord = false
                        for (w in words) {
                            if (charIdx >= w.first && charIdx < w.second) {
                                currentWordTuple = w
                                val seed = getWordSeed(w.third, lineIdx, wordIdx)
                                wordRng = java.util.Random(seed)
                                wordIdx++
                                matchedWord = true
                                break
                            }
                        }
                        if (matchedWord) {
                            // Unified parent traits for the entire LTR word
                            wordSizeScale = 1.0f + (wordRng.nextFloat() * 0.12f - 0.06f) * imperfectionLevel
                            wordRot = (wordRng.nextFloat() * 3.5f - 1.75f) * imperfectionLevel
                            wordSkew = (wordRng.nextFloat() * 0.08f - 0.04f) * imperfectionLevel
                            wordVOffset = (wordRng.nextFloat() * 2.5f - 1.25f) * imperfectionLevel
                            wordAdvanceMult = 1.02f + (wordRng.nextFloat() * 0.06f - 0.03f) * imperfectionLevel
                        } else {
                            wordSizeScale = 1.0f
                            wordRot = 0f
                            wordSkew = 0f
                            wordVOffset = 0f
                            wordAdvanceMult = 1.02f
                        }
                    }

                    drift += driftRate + (wordRng.nextFloat() * 0.1f - 0.05f) * imperfectionLevel
                    drift = drift.coerceIn(-4f, 4f)
                    val currentY = yBase + drift.toInt()
                    
                    // Fine character-level jitter applied on top of unified word traits
                    val localSizeVar = (wordRng.nextFloat() * 0.03f - 0.015f) * imperfectionLevel
                    val sizeScale = wordSizeScale + localSizeVar

                    val localRotVar = (wordRng.nextFloat() * 1.5f - 0.75f) * imperfectionLevel
                    val charRot = wordRot + localRotVar

                    val localSkewVar = (wordRng.nextFloat() * 0.02f - 0.01f) * imperfectionLevel
                    val charSkew = wordSkew + localSkewVar

                    val localVVar = (wordRng.nextFloat() * 1.5f - 0.75f) * imperfectionLevel
                    val vJitter = (wordVOffset + localVVar).toInt()

                    val useHebrew = isHebrewChar(char)
                    textPaint.typeface = if (useHebrew) hebrewTypeface else standardTypeface

                    if (applyPencilBlend) {
                        val reduce = if (isCustomFontUsed) (5 * imperfectionLevel).toInt() else (20 * imperfectionLevel).toInt()
                        val alphaBase = 255 - reduce - wordRng.nextInt(15)
                        textPaint.alpha = alphaBase.coerceIn(if (isCustomFontUsed) 220 else 160, 255)

                        if (isCustomFontUsed) {
                            textPaint.strokeWidth = 1.2f + (wordRng.nextFloat() * 0.4f * imperfectionLevel)
                            textPaint.style = Paint.Style.FILL_AND_STROKE
                        } else if ("bdfhijkltpqugyBDFHIJKLTPQUGY".contains(char)) {
                            textPaint.strokeWidth = 1f + (wordRng.nextFloat() * 1.0f * imperfectionLevel)
                            textPaint.style = Paint.Style.FILL_AND_STROKE
                        } else {
                            textPaint.strokeWidth = 1f
                            textPaint.style = Paint.Style.FILL
                        }
                    } else {
                        textPaint.alpha = 255
                        if (isCustomFontUsed) {
                            textPaint.strokeWidth = 1.2f
                            textPaint.style = Paint.Style.FILL_AND_STROKE
                        } else {
                            textPaint.strokeWidth = 1f
                            textPaint.style = Paint.Style.FILL
                        }
                    }

                    textPaint.textSize = baseFontSize * sizeScale

                    textCanvas.save()

                    textCanvas.translate(x, (currentY + vJitter).toFloat())
                    textCanvas.rotate(charRot)

                    val skewMatrix = Matrix()
                    skewMatrix.setSkew(charSkew, 0f)
                    textCanvas.concat(skewMatrix)

                    textCanvas.drawText(char.toString(), 0f, 0f, textPaint)
                    textCanvas.restore()

                    val charWidth = textPaint.measureText(char.toString())
                    val spacingVar = (wordRng.nextFloat() * 0.04f - 0.02f) * imperfectionLevel
                    val advance = charWidth * (wordAdvanceMult + spacingVar)
                    x += advance
                    charIdx++
                }
            }
        }

        // 5. Meticulously blend transparent text ink directly with the background paper textures & lines
        val blendedBitmap = if (applyPencilBlend) {
            val actualBlendFactor = if (isCustomFontUsed) 15 else 40
            blendPixelInk(textBitmap, canvasBitmap, blendFactor = actualBlendFactor)
        } else {
            blendPixelInk(textBitmap, canvasBitmap, blendFactor = 0)
        }

        // 6. High-fidelity Photo post-processing Filters (Affine Shear, Paper Soft warmth, contrast & brightness reduction)
        val processedBitmap = applyPhotoEffects(blendedBitmap, applyLighting)

        return processedBitmap
    }

    private fun drawBackground(canvas: Canvas, paperType: PaperType, startY: Int, lineSpacing: Int, redLineX: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Page base tone
        val baseColor = when (paperType) {
            PaperType.NOTEBOOK -> 0xFFFAF8F0.toInt()       // Off-white ivory paper
            PaperType.GRID -> 0xFFFDFBF7.toInt()           // Bright sketchbook grid
            PaperType.LEGAL_PAD -> 0xFFFEFCD7.toInt()      // Warm yellow canary legal tablet
            PaperType.BLANK_PARCHMENT -> 0xFFF3EAE2.toInt() // Soft vintage antique parchment
        }
        canvas.drawColor(baseColor)

        // Draw notebook lines / grid lines
        when (paperType) {
            PaperType.NOTEBOOK -> {
                paint.apply {
                    color = 0xFF4F83CC.toInt() // Real pen ink notebook blue line
                    strokeWidth = 1.5f
                    alpha = 110 // Semi-opalescent notebooks
                    style = Paint.Style.STROKE
                }
                var y = startY + lineSpacing
                while (y < height - 100) {
                    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
                    y += lineSpacing
                }

                paint.apply {
                    color = 0xFFE04343.toInt() // Hot margin red lines
                    strokeWidth = 2.5f
                    alpha = 160
                }
                canvas.drawLine(redLineX.toFloat() + 30f, 0f, redLineX.toFloat() + 30f, height.toFloat(), paint)
            }
            PaperType.GRID -> {
                paint.apply {
                    color = 0xFFCCD7E8.toInt() // Grid slate lines
                    strokeWidth = 1f
                    alpha = 120
                    style = Paint.Style.STROKE
                }
                var x = 0
                while (x < width) {
                    canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), paint)
                    x += lineSpacing
                }
                var y = 0
                while (y < height) {
                    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
                    y += lineSpacing
                }
            }
            PaperType.LEGAL_PAD -> {
                paint.apply {
                    color = 0xFFCCD39B.toInt() // Faint yellow-green ruling
                    strokeWidth = 1.5f
                    alpha = 120
                    style = Paint.Style.STROKE
                }
                var y = startY + lineSpacing
                while (y < height - 100) {
                    canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
                    y += lineSpacing
                }

                // Dual margining lines
                paint.apply {
                    color = 0xFFF25A5A.toInt() // Pinkish line on yellow legal pads
                    strokeWidth = 1.2f
                    alpha = 140
                }
                canvas.drawLine((redLineX + 26).toFloat(), 0f, (redLineX + 26).toFloat(), height.toFloat(), paint)
                canvas.drawLine((redLineX + 32).toFloat(), 0f, (redLineX + 32).toFloat(), height.toFloat(), paint)
            }
            PaperType.BLANK_PARCHMENT -> {
                // Outer subtle organic parchment border
                paint.apply {
                    color = 0x1A6B4A23 // Vintage brown border shadow
                    strokeWidth = 12f
                    style = Paint.Style.STROKE
                }
                canvas.drawRect(25f, 25f, (width - 25).toFloat(), (height - 25).toFloat(), paint)
            }
        }
    }

    private fun drawPaperTexture(canvas: Canvas, random: java.util.Random) {
        val paint = Paint().apply {
            style = Paint.Style.FILL
        }

        // Draw 30,000 transparent microdots to match `_add_paper_texture` noise
        for (i in 0..30000) {
            val rx = random.nextFloat() * width
            val ry = random.nextFloat() * height
            val isDark = random.nextFloat() < 0.35f
            if (isDark) {
                paint.color = Color.argb(4, 90, 80, 70) // Organic wood fiber specks
                canvas.drawRect(rx, ry, rx + 1.2f, ry + 1.2f, paint)
            } else {
                paint.color = Color.argb(6, 255, 255, 255) // White reflecting flecks
                canvas.drawRect(rx, ry, rx + 1.8f, ry + 1.8f, paint)
            }
        }
    }

    private fun applyLightingAndShadows(canvas: Canvas) {
        // Linear gradients to emulate linear and dual-corner light fallout
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }

        // Warm corner gradient (Vignette) from top-left
        val shadowColor = Color.argb(38, 20, 14, 2)
        val lightColor = Color.argb(0, 255, 255, 255)
        overlayPaint.shader = LinearGradient(
            0f, 0f, width * 0.85f, height * 0.85f,
            shadowColor, lightColor, Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // Secondary bottom-right vignette falloff
        overlayPaint.shader = LinearGradient(
            width.toFloat(), height.toFloat(), width * 0.15f, height * 0.15f,
            Color.argb(32, 12, 10, 4), Color.argb(0, 255, 255, 255), Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
    }

    private fun applyPhotoEffects(input: Bitmap, applyLighting: Boolean): Bitmap {
        // Create matching target bitmap
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // 1. Subtle Perspective Warp using affine skew matrix
        val matrix = Matrix()
        // Random 1-3 degree tilt shear
        val shearFactor = 0.02f
        matrix.postSkew(shearFactor, 0f)
        canvas.drawBitmap(input, matrix, null)

        // 2. Adjust brightness (94%) and contrast (84%) to be extremely bright, clear and legible
        if (applyLighting) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            // Contrast adjustment formula: s * color + (1 - s) * 128
            // Brightness scaling: 0.94f for beautiful high-quality scanning lookup
            val c = 0.84f
            val brightness = 0.94f
            val scale = c * brightness
            val bOffset = 128f * (1f - c) * brightness
            
            // Build 4x5 color adjustment matrix
            val colorMatrix = ColorMatrix(floatArrayOf(
                scale, 0f, 0f, 0f, bOffset,
                0f, scale, 0f, 0f, bOffset,
                0f, 0f, scale, 0f, bOffset,
                0f, 0f, 0f, 1f, 0f
            ))
            
            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            
            // Redraw on itself with the color filter adjustment
            val temp = output.copy(Bitmap.Config.ARGB_8888, true)
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            canvas.drawBitmap(temp, 0f, 0f, paint)
        }

        return output
    }

    private fun blendPixelInk(textBitmap: Bitmap, bgBitmap: Bitmap, blendFactor: Int): Bitmap {
        val w = textBitmap.width
        val h = textBitmap.height
        val totalPixels = w * h

        val textPixels = IntArray(totalPixels)
        val bgPixels = IntArray(totalPixels)

        textBitmap.getPixels(textPixels, 0, w, 0, 0, w, h)
        bgBitmap.getPixels(bgPixels, 0, w, 0, 0, w, h)

        val blendRatio = blendFactor / 100f

        for (idx in 0 until totalPixels) {
            val tp = textPixels[idx]
            val alpha = (tp shr 24) and 0xFF

            if (alpha > 0) {
                // Extract channels from text ink
                val tr = (tp shr 16) and 0xFF
                val tg = (tp shr 8) and 0xFF
                val tb = tp and 0xFF

                // Extract channels from background paper
                val bp = bgPixels[idx]
                val br = (bp shr 16) and 0xFF
                val bg = (bp shr 8) and 0xFF
                val bb = bp and 0xFF

                // Blend ink over background based on alpha and blendFactor
                val inkStrength = (alpha / 255f) * (1f - blendRatio)
                
                val r = (tr * inkStrength + br * (1f - inkStrength)).toInt().coerceIn(0, 255)
                val g = (tg * inkStrength + bg * (1f - inkStrength)).toInt().coerceIn(0, 255)
                val b = (tb * inkStrength + bb * (1f - inkStrength)).toInt().coerceIn(0, 255)

                bgPixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val resultBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(bgPixels, 0, w, 0, 0, w, h)
        return resultBitmap
    }
}

