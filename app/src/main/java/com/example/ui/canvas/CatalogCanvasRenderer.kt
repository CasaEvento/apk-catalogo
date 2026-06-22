package com.example.ui.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.example.data.model.CompanyConfig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object CatalogCanvasRenderer {

    /**
     * Renders the complete 1080x1080 catalog page technical sheet onto any Android Canvas.
     */
    fun drawCatalogOnCanvas(
        canvas: Canvas,
        width: Int,
        height: Int,
        productBitmap: Bitmap?,
        logoBitmap: Bitmap?,
        companyConfig: CompanyConfig,
        productName: String,
        productCode: String,
        dimHeight: String,
        dimWidth: String,
        dimDepth: String,
        material: String,
        finish: String,
        presentation: String
    ) {
        val pColor = companyConfig.getPrimaryColor()
        val sColor = companyConfig.getSecondaryColor()
        val ctaColor = companyConfig.getCtaColor()
        val textColor = companyConfig.getTextColor()

        // 1. BACKGROUND RENDER
        val bgPaint = Paint().apply {
            color = pColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle geometric background accent
        val accentPaint = Paint().apply {
            color = sColor
            style = Paint.Style.FILL
            alpha = 80 // Semi-transparent
        }
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(width.toFloat() * 0.45f, 0f)
            lineTo(width.toFloat() * 0.35f, height.toFloat())
            lineTo(0f, height.toFloat())
            close()
        }
        canvas.drawPath(path, accentPaint)

        // 2. HEADER BRANDING BANNER
        val titlePaint = Paint().apply {
            color = textColor
            textSize = height * 0.042f // ~45sp
            style = Paint.Style.FILL
            typeface = Typeface.create("Roboto", Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = textColor
            textSize = height * 0.02f // ~21sp
            style = Paint.Style.FILL
            typeface = Typeface.create("Roboto", Typeface.NORMAL)
            alpha = 180
            isAntiAlias = true
        }

        canvas.drawText(
            companyConfig.name.uppercase(),
            width * 0.06f,
            height * 0.07f,
            titlePaint
        )
        canvas.drawText(
            "FICHA TÉCNICA DE PRODUCTO",
            width * 0.06f,
            height * 0.10f,
            subtitlePaint
        )

        // Draw branding logo if present
        if (logoBitmap != null) {
            val logoSize = (height * 0.09f).toInt()
            val destRect = Rect(
                (width * 0.85f).toInt(),
                (height * 0.03f).toInt(),
                (width * 0.85f + logoSize).toInt(),
                (height * 0.03f + logoSize).toInt()
            )
            val logoPaint = Paint().apply { isFilterBitmap = true }
            canvas.drawBitmap(logoBitmap, null, destRect, logoPaint)
        }

        // Decorative line under title
        val linePaint = Paint().apply {
            color = ctaColor
            strokeWidth = height * 0.005f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawLine(width * 0.06f, height * 0.115f, width * 0.94f, height * 0.115f, linePaint)

        // 3. PRODUCT STAGING DISPLAY (Center-Right Area)
        val stageCenterX = width * 0.55f
        val stageCenterY = height * 0.45f
        val stageRadius = height * 0.23f

        // Staging circle
        val stagePaint = Paint().apply {
            color = sColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(stageCenterX, stageCenterY, stageRadius, stagePaint)

        // Staging outline
        val stageOutlinePaint = Paint().apply {
            color = ctaColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawCircle(stageCenterX, stageCenterY, stageRadius, stageOutlinePaint)

        // Draw product bitmap inside staging area
        var pLeft = stageCenterX - stageRadius * 0.85f
        var pTop = stageCenterY - stageRadius * 0.85f
        var pRight = stageCenterX + stageRadius * 0.85f
        var pBottom = stageCenterY + stageRadius * 0.85f

        if (productBitmap != null) {
            val srcH = productBitmap.height
            val srcW = productBitmap.width
            val srcAspect = srcW.toFloat() / srcH.toFloat()

            // Adapt aspect ratio to fit boundaries beautifully without deformation
            val boundSize = stageRadius * 1.7f
            if (srcAspect > 1f) {
                // Wider than tall
                val targetH = boundSize / srcAspect
                pTop = stageCenterY - targetH / 2f
                pBottom = stageCenterY + targetH / 2f
                pLeft = stageCenterX - boundSize / 2f
                pRight = stageCenterX + boundSize / 2f
            } else {
                // Taller than wide
                val targetW = boundSize * srcAspect
                pLeft = stageCenterX - targetW / 2f
                pRight = stageCenterX + targetW / 2f
                pTop = stageCenterY - boundSize / 2f
                pBottom = stageCenterY + boundSize / 2f
            }

            val pPaint = Paint().apply { isFilterBitmap = true }
            canvas.drawBitmap(productBitmap, null, RectF(pLeft, pTop, pRight, pBottom), pPaint)
        } else {
            // Draw visual placeholder text if no image
            val textPaint = Paint().apply {
                color = textColor
                textSize = height * 0.024f
                style = Paint.Style.FILL
                alpha = 150
                typeface = Typeface.create("Roboto", Typeface.ITALIC)
                isAntiAlias = true
            }
            val placeholderStr = "[ Captura Fotográfica ]"
            val textWidth = textPaint.measureText(placeholderStr)
            canvas.drawText(placeholderStr, stageCenterX - textWidth / 2f, stageCenterY + 10f, textPaint)
        }

        // 4. DOUBLE ANNOTATED METRIC RULES (LAS COTAS)
        val cotaPaint = Paint().apply {
            color = ctaColor
            strokeWidth = 4f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val cotaTextPaint = Paint().apply {
            color = textColor
            textSize = height * 0.022f // ~24sp
            style = Paint.Style.FILL
            typeface = Typeface.create("Roboto", Typeface.BOLD)
            isAntiAlias = true
        }

        // --- COTA ANCHO (Horizontal rule placed under the product staging) ---
        val hCotaY = stageCenterY + stageRadius + 30f
        val hCotaStartX = stageCenterX - stageRadius * 0.8f
        val hCotaEndX = stageCenterX + stageRadius * 0.8f

        // Draw tick markers
        canvas.drawLine(hCotaStartX, hCotaY - 15f, hCotaStartX, hCotaY + 15f, cotaPaint)
        canvas.drawLine(hCotaEndX, hCotaY - 15f, hCotaEndX, hCotaY + 15f, cotaPaint)
        // Draw primary cota line
        canvas.drawLine(hCotaStartX, hCotaY, hCotaEndX, hCotaY, cotaPaint)
        // Draw ending arrows
        drawCotaArrow(canvas, hCotaStartX, hCotaY, 180f, hCotaY, cotaPaint)
        drawCotaArrow(canvas, hCotaEndX, hCotaY, 0f, hCotaY, cotaPaint)

        // Draw width text
        val widthStr = if (dimWidth.isEmpty()) "0 cm" else "$dimWidth cm"
        val widthTextW = cotaTextPaint.measureText(widthStr)
        // Erase background behind text to keep it readable
        val textBgPaint = Paint().apply {
            color = pColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            (hCotaStartX + hCotaEndX) / 2f - widthTextW / 2f - 10f,
            hCotaY - 20f,
            (hCotaStartX + hCotaEndX) / 2f + widthTextW / 2f + 10f,
            hCotaY + 20f,
            textBgPaint
        )
        canvas.drawText(
            widthStr,
            (hCotaStartX + hCotaEndX) / 2f - widthTextW / 2f,
            hCotaY + 8f,
            cotaTextPaint
        )

        // --- COTA ALTO (Vertical rule placed left of the product staging) ---
        val vCotaX = stageCenterX - stageRadius - 30f
        val vCotaStartY = stageCenterY - stageRadius * 0.8f
        val vCotaEndY = stageCenterY + stageRadius * 0.8f

        // Draw tick markers
        canvas.drawLine(vCotaX - 15f, vCotaStartY, vCotaX + 15f, vCotaStartY, cotaPaint)
        canvas.drawLine(vCotaX - 15f, vCotaEndY, vCotaX + 15f, vCotaEndY, cotaPaint)
        // Draw primary cota line
        canvas.drawLine(vCotaX, vCotaStartY, vCotaX, vCotaEndY, cotaPaint)
        // Draw ending arrows
        drawCotaArrow(canvas, vCotaX, vCotaStartY, 270f, vCotaX, cotaPaint)
        drawCotaArrow(canvas, vCotaX, vCotaEndY, 90f, vCotaX, cotaPaint)

        // Draw height text
        val heightStr = if (dimHeight.isEmpty()) "0 cm" else "$dimHeight cm"
        val heightTextW = cotaTextPaint.measureText(heightStr)

        canvas.save()
        // Rotate text vertically to match blueprint detailing style
        canvas.translate(vCotaX - 20f, (vCotaStartY + vCotaEndY) / 2f)
        canvas.rotate(-90f)

        canvas.drawRect(
            -heightTextW / 2f - 10f,
            -18f,
            heightTextW / 2f + 10f,
            18f,
            textBgPaint
        )
        canvas.drawText(
            heightStr,
            -heightTextW / 2f,
            6f,
            cotaTextPaint
        )
        canvas.restore()


        // 5. TECHNICAL INFORMATION DATA BADGE (Left Side Column)
        val cardLeft = width * 0.06f
        val cardTop = height * 0.17f
        val cardWidth = width * 0.38f
        val cardHeight = height * 0.77f

        // Info Badge Base Card shape
        val badgePaint = Paint().apply {
            color = sColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val badgeOutline = Paint().apply {
            color = ctaColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        val cardRect = RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight)
        canvas.drawRoundRect(cardRect, 20f, 20f, badgePaint)
        canvas.drawRoundRect(cardRect, 20f, 20f, badgeOutline)

        // Structured Info Text elements inside the card
        val labelPaint = Paint().apply {
            color = textColor
            textSize = height * 0.016f // ~18sp
            style = Paint.Style.FILL
            alpha = 180
            typeface = Typeface.create("Roboto", Typeface.NORMAL)
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            color = textColor
            textSize = height * 0.021f // ~23sp
            style = Paint.Style.FILL
            typeface = Typeface.create("Roboto", Typeface.BOLD)
            isAntiAlias = true
        }

        // Draw metadata rows
        var currY = cardTop + 50f
        val lineSpacing = height * 0.09f

        // Product Name (Primary Hero)
        val nameHeroPaint = Paint().apply {
            color = textColor
            textSize = height * 0.026f // ~28sp
            style = Paint.Style.FILL
            typeface = Typeface.create("Roboto", Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("PRODUCTO", cardLeft + 30f, currY, labelPaint)
        currY += 35f
        val maxNameLen = cardWidth - 60f
        val clippedName = if (nameHeroPaint.measureText(productName) > maxNameLen) {
            productName.take(18) + "..."
        } else productName.ifEmpty { "NUEVO MODELO" }
        canvas.drawText(clippedName, cardLeft + 30f, currY, nameHeroPaint)

        currY += lineSpacing - 10f

        // Code
        canvas.drawText("CÓDIGO DE RECONOCIMIENTO", cardLeft + 30f, currY, labelPaint)
        currY += 30f
        canvas.drawText(productCode.ifEmpty { "S/N" }, cardLeft + 30f, currY, valuePaint)

        currY += lineSpacing

        // Material
        canvas.drawText("MATERIAL DETECTADO", cardLeft + 30f, currY, labelPaint)
        currY += 30f
        val clippedMaterial = if (valuePaint.measureText(material) > maxNameLen) {
            material.take(20) + "..."
        } else material.ifEmpty { "Cargando..." }
        canvas.drawText(clippedMaterial, cardLeft + 30f, currY, valuePaint)

        currY += lineSpacing

        // Finish (Acabado)
        canvas.drawText("TIPO DE ACABADO", cardLeft + 30f, currY, labelPaint)
        currY += 30f
        val finishUpper = finish.uppercase().ifEmpty { "MATE" }
        // Highlight background for finish text
        val finishBadgePaint = Paint().apply {
            color = ctaColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val fTextW = valuePaint.measureText(finishUpper)
        val fBadgeWidth = fTextW + 30f
        val fBadgeHeight = height * 0.038f
        val fRect = RectF(
            cardLeft + 30f,
            currY - fBadgeHeight*0.75f,
            cardLeft + 30f + fBadgeWidth,
            currY + fBadgeHeight*0.35f
        )
        canvas.drawRoundRect(fRect, 10f, 10f, finishBadgePaint)
        canvas.drawText(finishUpper, cardLeft + 45f, currY, valuePaint)

        currY += lineSpacing

        // Dimensions box (Alto x Ancho x Profundidad)
        canvas.drawText("DIMENSIONES GENERALES", cardLeft + 30f, currY, labelPaint)
        currY += 30f
        val dimStr = "${dimHeight.ifEmpty { "0" }} x ${dimWidth.ifEmpty { "0" }} x ${dimDepth.ifEmpty { "0" }} cm"
        canvas.drawText(dimStr, cardLeft + 30f, currY, valuePaint)

        currY += lineSpacing

        // Presentation
        canvas.drawText("PRESENTACIÓN COMERCIAL", cardLeft + 30f, currY, labelPaint)
        currY += 30f
        canvas.drawText(presentation.ifEmpty { "Unidad estándar" }, cardLeft + 30f, currY, valuePaint)
    }

    /**
     * Helper to draw double arrows correctly at the given endpoints on the metric rule.
     */
    private fun drawCotaArrow(
        canvas: Canvas,
        x: Float,
        y: Float,
        angleDeg: Float,
        refSize: Float,
        paint: Paint
    ) {
        val arrowSize = 20f
        val rad = Math.toRadians(angleDeg.toDouble())
        val backRad1 = Math.toRadians((angleDeg + 145f).toDouble())
        val backRad2 = Math.toRadians((angleDeg - 145f).toDouble())

        val xAction1 = x + arrowSize * cos(backRad1).toFloat()
        val yAction1 = y + arrowSize * sin(backRad1).toFloat()

        val xAction2 = x + arrowSize * cos(backRad2).toFloat()
        val yAction2 = y + arrowSize * sin(backRad2).toFloat()

        canvas.drawLine(x, y, xAction1, yAction1, paint)
        canvas.drawLine(x, y, xAction2, yAction2, paint)
    }

    /**
     * Generates a fully autonomous 1080x1080 Bitmap using the canvas logic.
     */
    fun createCatalogBitmap(
        productBitmap: Bitmap?,
        logoBitmap: Bitmap?,
        companyConfig: CompanyConfig,
        productName: String,
        productCode: String,
        dimHeight: String,
        dimWidth: String,
        dimDepth: String,
        material: String,
        finish: String,
        presentation: String
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawCatalogOnCanvas(
            canvas = canvas,
            width = 1080,
            height = 1080,
            productBitmap = productBitmap,
            logoBitmap = logoBitmap,
            companyConfig = companyConfig,
            productName = productName,
            productCode = productCode,
            dimHeight = dimHeight,
            dimWidth = dimWidth,
            dimDepth = dimDepth,
            material = material,
            finish = finish,
            presentation = presentation
        )
        return bitmap
    }
}
