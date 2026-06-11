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

        // 1. Setup fonts
        val standardTypeface = if (fontPath != null && File(fontPath).exists()) {
            try {
                Typeface.createFromFile(File(fontPath))
            } catch (e: Exception) {
                Typeface.create(Typeface.create("serif", Typeface.NORMAL), Typeface.NORMAL)
            }
        } else {
            Typeface.create(Typeface.create("serif", Typeface.NORMAL), Typeface.NORMAL)
        }

        val hebrewTypeface = if (hebrewFontPath != null && File(hebrewFontPath).exists()) {
            try {
                Typeface.createFromFile(File(hebrewFontPath))
            } catch (e: Exception) {
                Typeface.create(Typeface.create("cursive", Typeface.NORMAL), Typeface.NORMAL)
            }
        } else {
            Typeface.create(Typeface.create("cursive", Typeface.NORMAL), Typeface.NORMAL)
        }

        // 2. Create high-res canvas bitamp
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

        // Create background-only reference bitmap for exact 40% ink blending matching python
        val bgBitmap = canvasBitmap.copy(Bitmap.Config.ARGB_8888, true)

        // Setup base paint for rendering text
        val baseInkColor = if (inkColor == InkColor.PENCIL) getPencilColor(20) else inkColor.colorVal
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = baseInkColor
            style = Paint.Style.FILL
            textSize = baseFontSize
            // Apply slight blur for soft analog ink pen look matching python's Gaussian radius 0.5
            if (applyPencilBlend) {
                maskFilter = BlurMaskFilter(1.2f, BlurMaskFilter.Blur.NORMAL)
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
                        for (w in words) {
                            if (charIdx >= w.first && charIdx < w.second) {
                                currentWordTuple = w
                                val seed = getWordSeed(w.third, lineIdx, wordIdx)
                                wordRng = java.util.Random(seed)
                                wordIdx++
                                break
                            }
                        }
                    }

                    drift += driftRate + (wordRng.nextFloat() * 0.1f - 0.05f) * imperfectionLevel
                    drift = drift.coerceIn(-4f, 4f)
                    val currentY = yBase + drift.toInt()
                    val vJitter = (wordRng.nextInt(5) - 2) * imperfectionLevel

                    // Letter scale changes: +/-15% base, 10% chance of +/-25% outlier
                    var sizeScale = 1.0f + (wordRng.nextFloat() * 0.3f - 0.15f) * imperfectionLevel
                    if (wordRng.nextFloat() < 0.10f) {
                        sizeScale = 1.0f + (wordRng.nextFloat() * 0.5f - 0.25f) * imperfectionLevel
                    }

                    // Pressure & pen ink variations
                    val useHebrew = isHebrewChar(char)
                    textPaint.typeface = if (useHebrew) hebrewTypeface else standardTypeface

                    if (applyPencilBlend) {
                        val reduce = (20 * imperfectionLevel).toInt()
                        val alphaBase = 255 - reduce - wordRng.nextInt(15)
                        textPaint.alpha = alphaBase.coerceIn(160, 255)

                        // Downstroke letters get slightly bolder paint strokes
                        if ("bdfhijkltpqugyBDFHIJKLTPQUGY".contains(char)) {
                            textPaint.strokeWidth = 1f + (wordRng.nextFloat() * 1.5f * imperfectionLevel)
                            textPaint.style = Paint.Style.FILL_AND_STROKE
                        } else {
                            textPaint.strokeWidth = 1f
                            textPaint.style = Paint.Style.FILL
                        }
                    } else {
                        textPaint.alpha = 255
                        textPaint.strokeWidth = 1f
                        textPaint.style = Paint.Style.FILL
                    }

                    textPaint.textSize = baseFontSize * sizeScale

                    canvas.save()
                    
                    // Slant & tilt rotations matching Python transforms
                    val charRot = (wordRng.nextFloat() * 7f - 3.5f) * imperfectionLevel
                    val charSkew = (wordRng.nextFloat() * 0.16f - 0.08f) * imperfectionLevel
                    
                    val charWidth = textPaint.measureText(char.toString())
                    val pasteX = x - charWidth
                    val specialHebrewShift = if (char == 'י') (baseFontSize * 0.5f) else 0f
                    val pasteY = currentY + vJitter - specialHebrewShift

                    canvas.translate(pasteX, pasteY)
                    canvas.rotate(charRot)
                    
                    val skewMatrix = Matrix()
                    skewMatrix.setSkew(charSkew, 0f)
                    canvas.concat(skewMatrix)

                    canvas.drawText(char.toString(), 0f, 0f, textPaint)
                    canvas.restore()

                    // Horizontal step
                    val spacingVar = (wordRng.nextFloat() * 0.4f - 0.2f) * imperfectionLevel
                    val advance = charWidth * (1.30f + spacingVar) + 4f
                    x -= advance
                    charIdx++
                }
            } else {
                // LTR Path
                var x = (textStartX + redLineX).toFloat()
                var charIdx = 0
                var currentWordTuple: Triple<Int, Int, String>? = null
                var wordRng = java.util.Random(lineIdx.toLong())

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
                        for (w in words) {
                            if (charIdx >= w.first && charIdx < w.second) {
                                currentWordTuple = w
                                val seed = getWordSeed(w.third, lineIdx, wordIdx)
                                wordRng = java.util.Random(seed)
                                wordIdx++
                                break
                            }
                        }
                    }

                    drift += driftRate + (wordRng.nextFloat() * 0.1f - 0.05f) * imperfectionLevel
                    drift = drift.coerceIn(-4f, 4f)
                    val currentY = yBase + drift.toInt()
                    val vJitter = (wordRng.nextInt(5) - 2) * imperfectionLevel

                    var sizeScale = 1.0f + (wordRng.nextFloat() * 0.3f - 0.15f) * imperfectionLevel
                    if (wordRng.nextFloat() < 0.10f) {
                        sizeScale = 1.0f + (wordRng.nextFloat() * 0.5f - 0.25f) * imperfectionLevel
                    }

                    val useHebrew = isHebrewChar(char)
                    textPaint.typeface = if (useHebrew) hebrewTypeface else standardTypeface

                    if (applyPencilBlend) {
                        val reduce = (20 * imperfectionLevel).toInt()
                        val alphaBase = 255 - reduce - wordRng.nextInt(15)
                        textPaint.alpha = alphaBase.coerceIn(160, 255)

                        if ("bdfhijkltpqugyBDFHIJKLTPQUGY".contains(char)) {
                            textPaint.strokeWidth = 1f + (wordRng.nextFloat() * 1.5f * imperfectionLevel)
                            textPaint.style = Paint.Style.FILL_AND_STROKE
                        } else {
                            textPaint.strokeWidth = 1f
                            textPaint.style = Paint.Style.FILL
                        }
                    } else {
                        textPaint.alpha = 255
                        textPaint.strokeWidth = 1f
                        textPaint.style = Paint.Style.FILL
                    }

                    textPaint.textSize = baseFontSize * sizeScale

                    canvas.save()

                    val charRot = (wordRng.nextFloat() * 7f - 3.5f) * imperfectionLevel
                    val charSkew = (wordRng.nextFloat() * 0.16f - 0.08f) * imperfectionLevel

                    canvas.translate(x, currentY + vJitter)
                    canvas.rotate(charRot)

                    val skewMatrix = Matrix()
                    skewMatrix.setSkew(charSkew, 0f)
                    canvas.concat(skewMatrix)

                    canvas.drawText(char.toString(), 0f, 0f, textPaint)
                    canvas.restore()

                    val charWidth = textPaint.measureText(char.toString())
                    val spacingVar = (wordRng.nextFloat() * 0.4f - 0.2f) * imperfectionLevel
                    val advance = charWidth * (1.30f + spacingVar) + 4f
                    x += advance
                    charIdx++
                }
            }
        }

        // 5. High-fidelity Photo post-processing Filters (Affine Shear, Paper Soft warmth, contrast & brightness reduction)
        var processedBitmap = applyPhotoEffects(canvasBitmap, applyLighting)

        // 6. Blend ink depth directly with the background at BLEND_FACTOR=40% matching the PIL code
        if (applyPencilBlend) {
            processedBitmap = blendPixelInk(processedBitmap, bgBitmap, blendFactor = 40)
        }

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

        // 2. Adjust brightness (50%) and contrast (70%) matching python:
        // final_img = ImageEnhance.Brightness(result).enhance(0.5)
        // final_img = ImageEnhance.Contrast(final_img).enhance(0.7)
        if (applyLighting) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            // Contrast adjustment formula: s * color + (1 - s) * 128
            // Brightness scaling: 0.5f on top
            val c = 0.7f
            val bOffset = 128f * (1f - c)
            
            // Build 4x5 color adjustment matrix
            val colorMatrix = ColorMatrix(floatArrayOf(
                c * 0.5f, 0f, 0f, 0f, bOffset * 0.5f,
                0f, c * 0.5f, 0f, 0f, bOffset * 0.5f,
                0f, 0f, c * 0.5f, 0f, bOffset * 0.5f,
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
            val bp = bgPixels[idx]

            // Extract channels
            val tr = (tp shr 16) and 0xFF
            val tg = (tp shr 8) and 0xFF
            val tb = tp and 0xFF

            val br = (bp shr 16) and 0xFF
            val bg = (bp shr 8) and 0xFF
            val bb = bp and 0xFF

            // If the printed pixel noticeably deviates from background template → it is text ink
            val diff = Math.abs(tr - br) + Math.abs(tg - bg) + Math.abs(tb - bb)
            if (diff > 22) {
                // Meticulously blend text pixel toward background to inherit paper texture and vertical ruling line shadows
                val r = (tr * (1f - blendRatio) + br * blendRatio).toInt().coerceIn(0, 255)
                val g = (tg * (1f - blendRatio) + bg * blendRatio).toInt().coerceIn(0, 255)
                val b = (tb * (1f - blendRatio) + bb * blendRatio).toInt().coerceIn(0, 255)

                textPixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val resultBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(textPixels, 0, w, 0, 0, w, h)
        return resultBitmap
    }
}

