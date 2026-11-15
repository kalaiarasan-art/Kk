package com.example.ui.canvas

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.CanvasOpDoc
import com.example.data.model.DrawingTool
import com.example.data.model.SymmetryMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Showpiece 1600x1000 Vector Drawing Engine (§5):
 * - Fixed logical coordinate system: 1600 x 1000
 * - Append-only op-log (STROKE, SHAPE, FILL, UNDO, REDO, CLEAR)
 * - Bitmap checkpoints every 20 ops so multi-level undo/redo and late joiner sync are instant
 * - Deterministic mulberry32 PRNG seeded by stroke seed for spray & pencil grain
 * - Ramer-Douglas-Peucker simplification (epsilon = 0.6 px) & quantized 0.25 px delta codec
 * - Deterministic scanline flood fill encoded as compact run-length spans
 * - Symmetry transforms (Vertical, Horizontal, 4/6/8-fold radial)
 */
object DoodleCanvasEngine {
  const val LOGICAL_WIDTH = 1600f
  const val LOGICAL_HEIGHT = 1000f
  // Raster buffer at 800x500 (exact 2:1 scale of 1600x1000) for ultra-fast 60fps scanline flood fill and checkpoints
  const val RASTER_WIDTH = 800
  const val RASTER_HEIGHT = 500
  const val CHECKPOINT_INTERVAL = 20

  data class TimedPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.65f,
    val dtMs: Int = 16,
  )

  data class ScanlineSpan(
    val y: Int,
    val x1: Int,
    val x2: Int,
  )

  /**
   * Curated 24 Botanical & Studio Colors (§5) + Colorblind-safe Okabe-Ito-inspired option.
   */
  val CURATED_PALETTE: List<Color> = listOf(
    Color(0xFF26231F), // Deep Espresso Ink
    Color(0xFFF7F3EB), // Warm Parchment White
    Color(0xFFC86446), // Terracotta Primary
    Color(0xFF5F7A61), // Sage Botanical
    Color(0xFFDDA15E), // Sunlit Ochre
    Color(0xFF3B6E4C), // Moss Emerald
    Color(0xFFBC4749), // Crimson Berry
    Color(0xFFE07A5F), // Coral Clay
    Color(0xFFF4A261), // Marigold
    Color(0xFFE9C46A), // Golden Honey
    Color(0xFF8AB17D), // Lichen Green
    Color(0xFF2A9D8F), // Teal Verdigris
    Color(0xFF457B9D), // Cerulean Sky
    Color(0xFF1D3557), // Indigo Night
    Color(0xFF6D597A), // Plum Dusk
    Color(0xFFB56576), // Rose Madder
    Color(0xFF7F5539), // Walnut Bark
    Color(0xFFB08968), // Warm Sandstone
    Color(0xFF6B705C), // Olive Slate
    Color(0xFFA5A58D), // Dry Sage
    Color(0xFFFFB5A7), // Soft Peach Blossom
    Color(0xFFA8DADC), // Robin Egg
    Color(0xFFCDB4DB), // Lavender Mist
    Color(0xFF495057), // Charcoal Graphite
  )

  val COLORBLIND_SAFE_PALETTE: List<Color> = listOf(
    Color(0xFF000000), // Black
    Color(0xFFFFFFFF), // White
    Color(0xFFE69F00), // Orange
    Color(0xFF56B4E9), // Sky Blue
    Color(0xFF009E73), // Bluish Green
    Color(0xFFF0E442), // Yellow
    Color(0xFF0072B2), // Blue
    Color(0xFFD55E00), // Vermilion
    Color(0xFFCC79A7), // Reddish Purple
    Color(0xFF332288), // Indigo
    Color(0xFF88CCEE), // Cyan
    Color(0xFF44AA99), // Teal
    Color(0xFF117733), // Forest
    Color(0xFF999933), // Olive
    Color(0xFFDDCC77), // Sand
    Color(0xFFCC6677), // Rose
    Color(0xFF882255), // Wine
    Color(0xFFAA4499), // Magenta
    Color(0xFF661100), // Umber
    Color(0xFF6699CC), // Steel
    Color(0xFF888888), // Mid Grey
    Color(0xFFBBBBBB), // Light Grey
    Color(0xFF444444), // Dark Grey
    Color(0xFFEE8866), // Coral
  )

  /**
   * Mulberry32 deterministic PRNG seeded by stroke ID (§5).
   */
  class Mulberry32(seed: Int) {
    private var state: Int = if (seed == 0) 0x6D2B79F5 else seed

    fun nextFloat(): Float {
      state += 0x6D2B79F5
      var t = state
      t = (t xor (t ushr 15)) * (t or 1)
      t = t xor (t + (t xor (t ushr 7)) * (t or 61))
      val unsigned = ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL)
      return (unsigned.toDouble() / 4294967296.0).toFloat()
    }
  }

  /**
   * Ramer-Douglas-Peucker polyline simplification (epsilon ≈ 0.6 px in 1600x1000 space).
   */
  fun simplifyPoints(points: List<TimedPoint>, epsilon: Float = 0.6f): List<TimedPoint> {
    if (points.size <= 2) return points
    var maxDist = 0f
    var index = 0
    val start = points.first()
    val end = points.last()

    for (i in 1 until points.lastIndex) {
      val d = perpendicularDistance(points[i], start, end)
      if (d > maxDist) {
        index = i
        maxDist = d
      }
    }

    return if (maxDist > epsilon) {
      val left = simplifyPoints(points.subList(0, index + 1), epsilon)
      val right = simplifyPoints(points.subList(index, points.size), epsilon)
      left.dropLast(1) + right
    } else {
      listOf(start, end)
    }
  }

  private fun perpendicularDistance(pt: TimedPoint, lineStart: TimedPoint, lineEnd: TimedPoint): Float {
    val dx = lineEnd.x - lineStart.x
    val dy = lineEnd.y - lineStart.y
    val mag = hypot(dx, dy)
    if (mag < 0.0001f) return hypot(pt.x - lineStart.x, pt.y - lineStart.y)
    val u = ((pt.x - lineStart.x) * dx + (pt.y - lineStart.y) * dy) / (mag * mag)
    val projX = lineStart.x + u.coerceIn(0f, 1f) * dx
    val projY = lineStart.y + u.coerceIn(0f, 1f) * dy
    return hypot(pt.x - projX, pt.y - projY)
  }

  /**
   * Quantizes coordinates to 0.25 px (x4 int) and delta-encodes points into a compact string.
   */
  fun encodeStrokePoints(points: List<TimedPoint>): String {
    if (points.isEmpty()) return ""
    val capped = points.take(1500)
    val sb = StringBuilder(capped.size * 12)
    var prevQx = 0
    var prevQy = 0
    for (i in capped.indices) {
      val p = capped[i]
      val qx = (p.x.coerceIn(0f, LOGICAL_WIDTH) * 4f).roundToInt()
      val qy = (p.y.coerceIn(0f, LOGICAL_HEIGHT) * 4f).roundToInt()
      val dx = qx - prevQx
      val dy = qy - prevQy
      prevQx = qx
      prevQy = qy
      val pressByte = (p.pressure.coerceIn(0.05f, 1f) * 255f).roundToInt()
      val dtByte = p.dtMs.coerceIn(1, 255)
      if (i > 0) sb.append(';')
      sb.append(dx).append(',').append(dy).append(',').append(pressByte).append(',').append(dtByte)
    }
    return sb.toString()
  }

  fun decodeStrokePoints(encoded: String): List<TimedPoint> {
    if (encoded.isBlank()) return emptyList()
    val tokens = encoded.split(';')
    val result = ArrayList<TimedPoint>(tokens.size)
    var qx = 0
    var qy = 0
    for (tok in tokens) {
      val parts = tok.split(',')
      if (parts.size >= 2) {
        qx += parts[0].toIntOrNull() ?: 0
        qy += parts[1].toIntOrNull() ?: 0
        val pByte = parts.getOrNull(2)?.toIntOrNull() ?: 165
        val dt = parts.getOrNull(3)?.toIntOrNull() ?: 16
        result.add(
          TimedPoint(
            x = (qx / 4f).coerceIn(0f, LOGICAL_WIDTH),
            y = (qy / 4f).coerceIn(0f, LOGICAL_HEIGHT),
            pressure = (pByte / 255f).coerceIn(0.05f, 1f),
            dtMs = dt.coerceIn(1, 255)
          )
        )
      }
    }
    return result
  }

  fun encodeFillSpans(spans: List<ScanlineSpan>): String {
    if (spans.isEmpty()) return ""
    val sb = StringBuilder(spans.size * 10)
    for (i in spans.indices) {
      val s = spans[i]
      if (i > 0) sb.append(';')
      sb.append(s.y).append(',').append(s.x1).append(',').append(s.x2)
    }
    // Cap at 64 KB per §5
    return if (sb.length <= 65536) sb.toString() else sb.substring(0, 65536).substringBeforeLast(';')
  }

  fun decodeFillSpans(encoded: String): List<ScanlineSpan> {
    if (encoded.isBlank()) return emptyList()
    return encoded.split(';').mapNotNull { token ->
      val parts = token.split(',')
      if (parts.size == 3) {
        val y = parts[0].toIntOrNull() ?: return@mapNotNull null
        val x1 = parts[1].toIntOrNull() ?: return@mapNotNull null
        val x2 = parts[2].toIntOrNull() ?: return@mapNotNull null
        ScanlineSpan(y, x1, x2)
      } else null
    }
  }

  /**
   * Computes symmetry transformed point lists for a given input stroke in 1600x1000 space.
   */
  fun expandSymmetryPoints(
    points: List<TimedPoint>,
    symmetry: SymmetryMode
  ): List<List<TimedPoint>> {
    if (points.isEmpty() || symmetry == SymmetryMode.NONE) return listOf(points)
    val cx = LOGICAL_WIDTH / 2f
    val cy = LOGICAL_HEIGHT / 2f
    return when (symmetry) {
      SymmetryMode.NONE -> listOf(points)
      SymmetryMode.MIRROR_V -> listOf(
        points,
        points.map { it.copy(x = (LOGICAL_WIDTH - it.x).coerceIn(0f, LOGICAL_WIDTH)) }
      )
      SymmetryMode.MIRROR_H -> listOf(
        points,
        points.map { it.copy(y = (LOGICAL_HEIGHT - it.y).coerceIn(0f, LOGICAL_HEIGHT)) }
      )
      SymmetryMode.RADIAL_4 -> buildRadialTransforms(points, cx, cy, 4)
      SymmetryMode.RADIAL_6 -> buildRadialTransforms(points, cx, cy, 6)
      SymmetryMode.RADIAL_8 -> buildRadialTransforms(points, cx, cy, 8)
    }
  }

  private fun buildRadialTransforms(
    points: List<TimedPoint>,
    cx: Float,
    cy: Float,
    folds: Int
  ): List<List<TimedPoint>> {
    return (0 until folds).map { k ->
      val angle = (2.0 * PI * k) / folds
      val cosA = cos(angle).toFloat()
      val sinA = sin(angle).toFloat()
      points.map { p ->
        val dx = p.x - cx
        val dy = p.y - cy
        p.copy(
          x = (cx + dx * cosA - dy * sinA).coerceIn(0f, LOGICAL_WIDTH),
          y = (cy + dx * sinA + dy * cosA).coerceIn(0f, LOGICAL_HEIGHT)
        )
      }
    }
  }

  /**
   * Resolves the active op-log after processing UNDO, REDO, and CLEAR operations.
   */
  fun resolveEffectiveOps(rawOps: List<CanvasOpDoc>): List<CanvasOpDoc> {
    val active = mutableListOf<CanvasOpDoc>()
    val undone = mutableListOf<CanvasOpDoc>()
    for (op in rawOps.sortedBy { it.seq }) {
      when (op.opType) {
        "CLEAR" -> {
          active.clear()
          undone.clear()
        }
        "UNDO" -> {
          if (active.isNotEmpty()) {
            undone.add(active.removeAt(active.lastIndex))
          }
        }
        "REDO" -> {
          if (undone.isNotEmpty()) {
            active.add(undone.removeAt(undone.lastIndex))
          }
        }
        else -> {
          active.add(op)
          undone.clear()
        }
      }
    }
    return active
  }

  /**
   * Renders a single resolved CanvasOpDoc onto an Android Bitmap Canvas (800x500 raster buffer).
   */
  fun renderOpToBitmapCanvas(canvas: AndroidCanvas, op: CanvasOpDoc) {
    val scaleX = RASTER_WIDTH / LOGICAL_WIDTH
    val scaleY = RASTER_HEIGHT / LOGICAL_HEIGHT
    val baseColor = parseHexColor(op.colorHex)
    val alphaInt = ((op.opacity.coerceIn(1L, 100L) * 255L) / 100L).toInt().coerceIn(5, 255)
    val tool = runCatching { DrawingTool.valueOf(op.tool) }.getOrDefault(DrawingTool.INK_PEN)
    val symmetry = runCatching { SymmetryMode.valueOf(op.symmetryMode) }.getOrDefault(SymmetryMode.NONE)

    if (op.opType == "FILL") {
      val spans = decodeFillSpans(op.encodedData)
      val fillPaint = AndroidPaint().apply {
        color = applyAlpha(baseColor, alphaInt)
        style = AndroidPaint.Style.STROKE
        strokeWidth = 1.5f
        isAntiAlias = false
      }
      for (span in spans) {
        canvas.drawLine(
          span.x1.toFloat(),
          span.y.toFloat(),
          (span.x2 + 1).toFloat(),
          span.y.toFloat(),
          fillPaint
        )
      }
      return
    }

    val decodedPoints = decodeStrokePoints(op.encodedData)
    if (decodedPoints.isEmpty()) return
    val symmetryBranches = expandSymmetryPoints(decodedPoints, symmetry)
    val scaledStrokeSize = (op.sizePx.toFloat() * scaleX).coerceAtLeast(1f)

    for ((branchIdx, branch) in symmetryBranches.withIndex()) {
      when {
        op.opType == "SHAPE" || tool.isShape -> {
          renderShapeBranch(canvas, branch, tool, baseColor, alphaInt, scaledStrokeSize, scaleX, scaleY, fillShape = op.seed == 1L)
        }
        tool == DrawingTool.SPRAY -> {
          renderSprayBranch(canvas, branch, baseColor, alphaInt, scaledStrokeSize, (op.seed.toInt() + branchIdx * 31), scaleX, scaleY)
        }
        tool == DrawingTool.PENCIL -> {
          renderPencilGrainBranch(canvas, branch, baseColor, alphaInt, scaledStrokeSize, (op.seed.toInt() + branchIdx * 17), scaleX, scaleY)
        }
        tool == DrawingTool.CALLIGRAPHY -> {
          renderCalligraphyBranch(canvas, branch, baseColor, alphaInt, scaledStrokeSize, scaleX, scaleY)
        }
        tool == DrawingTool.INK_PEN -> {
          renderPressureRibbonBranch(canvas, branch, baseColor, alphaInt, scaledStrokeSize, scaleX, scaleY)
        }
        tool == DrawingTool.HIGHLIGHTER -> {
          val hlAlpha = (alphaInt * 0.38f).toInt().coerceIn(20, 120)
          renderSmoothQuadraticBranch(canvas, branch, baseColor, hlAlpha, scaledStrokeSize * 1.4f, scaleX, scaleY, isSquareCap = true)
        }
        tool == DrawingTool.MARKER -> {
          val mkAlpha = (alphaInt * 0.78f).toInt().coerceIn(40, 220)
          renderSmoothQuadraticBranch(canvas, branch, baseColor, mkAlpha, scaledStrokeSize * 1.15f, scaleX, scaleY, isSquareCap = false)
        }
        tool == DrawingTool.HARD_ERASER -> {
          val eraserColor = AndroidColor.rgb(0xF7, 0xF3, 0xEB)
          renderSmoothQuadraticBranch(canvas, branch, eraserColor, 255, scaledStrokeSize * 1.25f, scaleX, scaleY, isSquareCap = false)
        }
        tool == DrawingTool.SOFT_ERASER -> {
          val eraserColor = AndroidColor.rgb(0xF7, 0xF3, 0xEB)
          renderSmoothQuadraticBranch(canvas, branch, eraserColor, 115, scaledStrokeSize * 1.45f, scaleX, scaleY, isSquareCap = false)
        }
        else -> {
          renderSmoothQuadraticBranch(canvas, branch, baseColor, alphaInt, scaledStrokeSize, scaleX, scaleY, isSquareCap = false)
        }
      }
    }
  }

  private fun renderSmoothQuadraticBranch(
    canvas: AndroidCanvas,
    points: List<TimedPoint>,
    baseColor: Int,
    alpha: Int,
    strokeWidth: Float,
    scaleX: Float,
    scaleY: Float,
    isSquareCap: Boolean,
  ) {
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
      color = applyAlpha(baseColor, alpha)
      style = AndroidPaint.Style.STROKE
      this.strokeWidth = strokeWidth
      strokeCap = if (isSquareCap) AndroidPaint.Cap.SQUARE else AndroidPaint.Cap.ROUND
      strokeJoin = AndroidPaint.Join.ROUND
    }
    if (points.size == 1) {
      val p = points[0]
      canvas.drawCircle(p.x * scaleX, p.y * scaleY, strokeWidth / 2f, paint.apply { style = AndroidPaint.Style.FILL })
      return
    }
    val path = AndroidPath()
    path.moveTo(points[0].x * scaleX, points[0].y * scaleY)
    for (i in 1 until points.size) {
      val prev = points[i - 1]
      val curr = points[i]
      val midX = ((prev.x + curr.x) * 0.5f) * scaleX
      val midY = ((prev.y + curr.y) * 0.5f) * scaleY
      path.quadTo(prev.x * scaleX, prev.y * scaleY, midX, midY)
    }
    val last = points.last()
    path.lineTo(last.x * scaleX, last.y * scaleY)
    canvas.drawPath(path, paint)
  }

  /**
   * Perfect-freehand style pressure-sensitive variable-width ink stroke.
   */
  private fun renderPressureRibbonBranch(
    canvas: AndroidCanvas,
    points: List<TimedPoint>,
    baseColor: Int,
    alpha: Int,
    baseWidth: Float,
    scaleX: Float,
    scaleY: Float,
  ) {
    if (points.size <= 2) {
      renderSmoothQuadraticBranch(canvas, points, baseColor, alpha, baseWidth, scaleX, scaleY, false)
      return
    }
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
      color = applyAlpha(baseColor, alpha)
      style = AndroidPaint.Style.STROKE
      strokeCap = AndroidPaint.Cap.ROUND
      strokeJoin = AndroidPaint.Join.ROUND
    }
    for (i in 1 until points.size) {
      val p0 = points[i - 1]
      val p1 = points[i]
      val pressureFactor = (0.45f + 0.75f * ((p0.pressure + p1.pressure) * 0.5f)).coerceIn(0.35f, 1.35f)
      paint.strokeWidth = (baseWidth * pressureFactor).coerceAtLeast(1f)
      canvas.drawLine(p0.x * scaleX, p0.y * scaleY, p1.x * scaleX, p1.y * scaleY, paint)
    }
  }

  private fun renderCalligraphyBranch(
    canvas: AndroidCanvas,
    points: List<TimedPoint>,
    baseColor: Int,
    alpha: Int,
    baseWidth: Float,
    scaleX: Float,
    scaleY: Float,
  ) {
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
      color = applyAlpha(baseColor, alpha)
      style = AndroidPaint.Style.FILL
    }
    val nibAngle = PI.toFloat() / 4f // 45-degree nib
    val nibDx = cos(nibAngle) * baseWidth * 0.85f
    val nibDy = sin(nibAngle) * baseWidth * 0.85f

    for (i in points.indices) {
      val p = points[i]
      val sx = p.x * scaleX
      val sy = p.y * scaleY
      if (i > 0) {
        val prev = points[i - 1]
        val px = prev.x * scaleX
        val py = prev.y * scaleY
        val path = AndroidPath().apply {
          moveTo(px - nibDx, py - nibDy)
          lineTo(px + nibDx, py + nibDy)
          lineTo(sx + nibDx, sy + nibDy)
          lineTo(sx - nibDx, sy - nibDy)
          close()
        }
        canvas.drawPath(path, paint)
      } else {
        canvas.drawLine(sx - nibDx, sy - nibDy, sx + nibDx, sy + nibDy, paint.apply { strokeWidth = 2f })
      }
    }
  }

  private fun renderPencilGrainBranch(
    canvas: AndroidCanvas,
    points: List<TimedPoint>,
    baseColor: Int,
    alpha: Int,
    baseWidth: Float,
    seed: Int,
    scaleX: Float,
    scaleY: Float,
  ) {
    val prng = Mulberry32(seed)
    renderSmoothQuadraticBranch(canvas, points, baseColor, (alpha * 0.72f).toInt(), baseWidth * 0.85f, scaleX, scaleY, false)
    val grainPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
      color = applyAlpha(baseColor, (alpha * 0.55f).toInt().coerceIn(20, 200))
      style = AndroidPaint.Style.FILL
    }
    for (p in points) {
      val sx = p.x * scaleX
      val sy = p.y * scaleY
      repeat(3) {
        val ox = (prng.nextFloat() - 0.5f) * baseWidth * 1.1f
        val oy = (prng.nextFloat() - 0.5f) * baseWidth * 1.1f
        canvas.drawCircle(sx + ox, sy + oy, (baseWidth * 0.18f).coerceAtLeast(0.8f), grainPaint)
      }
    }
  }

  private fun renderSprayBranch(
    canvas: AndroidCanvas,
    points: List<TimedPoint>,
    baseColor: Int,
    alpha: Int,
    baseWidth: Float,
    seed: Int,
    scaleX: Float,
    scaleY: Float,
  ) {
    val prng = Mulberry32(seed)
    val dropletPaint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
      color = applyAlpha(baseColor, (alpha * 0.65f).toInt().coerceIn(20, 220))
      style = AndroidPaint.Style.FILL
    }
    val radius = (baseWidth * 1.4f).coerceAtLeast(4f)
    for (p in points) {
      val sx = p.x * scaleX
      val sy = p.y * scaleY
      repeat(10) {
        val angle = prng.nextFloat() * 2f * PI.toFloat()
        val r = sqrt(prng.nextFloat()) * radius
        val dx = cos(angle) * r
        val dy = sin(angle) * r
        canvas.drawCircle(sx + dx, sy + dy, 1.25f, dropletPaint)
      }
    }
  }

  private fun renderShapeBranch(
    canvas: AndroidCanvas,
    points: List<TimedPoint>,
    tool: DrawingTool,
    baseColor: Int,
    alpha: Int,
    strokeWidth: Float,
    scaleX: Float,
    scaleY: Float,
    fillShape: Boolean,
  ) {
    if (points.isEmpty()) return
    val start = points.first()
    val end = points.last()
    val x1 = start.x * scaleX
    val y1 = start.y * scaleY
    val x2 = end.x * scaleX
    val y2 = end.y * scaleY

    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
      color = applyAlpha(baseColor, alpha)
      style = if (fillShape && tool != DrawingTool.LINE) AndroidPaint.Style.FILL_AND_STROKE else AndroidPaint.Style.STROKE
      this.strokeWidth = strokeWidth
      strokeCap = AndroidPaint.Cap.ROUND
      strokeJoin = AndroidPaint.Join.ROUND
    }

    when (tool) {
      DrawingTool.LINE -> canvas.drawLine(x1, y1, x2, y2, paint)
      DrawingTool.RECTANGLE -> canvas.drawRect(min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2), paint)
      DrawingTool.ELLIPSE -> canvas.drawOval(min(x1, x2), min(y1, y2), max(x1, x2), max(y1, y2), paint)
      DrawingTool.TRIANGLE -> {
        val midTopX = (x1 + x2) * 0.5f
        val path = AndroidPath().apply {
          moveTo(midTopX, min(y1, y2))
          lineTo(max(x1, x2), max(y1, y2))
          lineTo(min(x1, x2), max(y1, y2))
          close()
        }
        canvas.drawPath(path, paint)
      }
      else -> canvas.drawLine(x1, y1, x2, y2, paint)
    }
  }

  /**
   * Deterministic Scanline Flood Fill (§5) with tolerance and 1px edge expansion.
   * Returns compact run-length ScanlineSpans in 800x500 raster space.
   */
  fun computeScanlineFloodFill(
    bitmap: Bitmap,
    logicalX: Float,
    logicalY: Float,
    fillColorHex: String,
    tolerance: Int = 36,
  ): List<ScanlineSpan> {
    val startX = ((logicalX / LOGICAL_WIDTH) * RASTER_WIDTH).toInt().coerceIn(0, RASTER_WIDTH - 1)
    val startY = ((logicalY / LOGICAL_HEIGHT) * RASTER_HEIGHT).toInt().coerceIn(0, RASTER_HEIGHT - 1)
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width * height)
    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

    val targetColor = pixels[startY * width + startX]
    val replacementColor = parseHexColor(fillColorHex)
    if (colorDistance(targetColor, replacementColor) <= 8) return emptyList()

    val visited = BooleanArray(width * height)
    val queue = IntArray(width * height)
    var head = 0
    var tail = 0
    val spans = mutableListOf<ScanlineSpan>()

    queue[tail++] = (startY shl 16) or startX

    while (head < tail && spans.size < 4500) {
      val packed = queue[head++]
      val y = packed ushr 16
      val x = packed and 0xFFFF
      val rowOffset = y * width
      if (visited[rowOffset + x] || !matchesTolerance(pixels[rowOffset + x], targetColor, tolerance)) {
        continue
      }

      var left = x
      while (left > 0 && !visited[rowOffset + left - 1] && matchesTolerance(pixels[rowOffset + left - 1], targetColor, tolerance)) {
        left--
      }
      var right = x
      while (right < width - 1 && !visited[rowOffset + right + 1] && matchesTolerance(pixels[rowOffset + right + 1], targetColor, tolerance)) {
        right++
      }

      // Mark visited and expand by 1px at edges to prevent anti-aliased halos (§5)
      val expLeft = (left - 1).coerceAtLeast(0)
      val expRight = (right + 1).coerceAtMost(width - 1)
      for (ix in left..right) {
        visited[rowOffset + ix] = true
      }
      spans.add(ScanlineSpan(y = y, x1 = expLeft, x2 = expRight))

      // Scan above and below
      for (ny in intArrayOf(y - 1, y + 1)) {
        if (ny in 0 until height) {
          val nRow = ny * width
          var inSegment = false
          for (ix in left..right) {
            val match = !visited[nRow + ix] && matchesTolerance(pixels[nRow + ix], targetColor, tolerance)
            if (match && !inSegment) {
              if (tail < queue.size) {
                queue[tail++] = (ny shl 16) or ix
              }
              inSegment = true
            } else if (!match) {
              inSegment = false
            }
          }
        }
      }
    }
    return spans
  }

  private fun matchesTolerance(c1: Int, c2: Int, tolerance: Int): Boolean {
    return colorDistance(c1, c2) <= tolerance
  }

  private fun colorDistance(c1: Int, c2: Int): Int {
    val dr = abs(AndroidColor.red(c1) - AndroidColor.red(c2))
    val dg = abs(AndroidColor.green(c1) - AndroidColor.green(c2))
    val db = abs(AndroidColor.blue(c1) - AndroidColor.blue(c2))
    return maxOf(dr, dg, db)
  }

  fun sampleEyedropperHex(bitmap: Bitmap, logicalX: Float, logicalY: Float): String {
    val sx = ((logicalX / LOGICAL_WIDTH) * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
    val sy = ((logicalY / LOGICAL_HEIGHT) * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
    val pixel = bitmap.getPixel(sx, sy)
    return String.format("#%02X%02X%02X", AndroidColor.red(pixel), AndroidColor.green(pixel), AndroidColor.blue(pixel))
  }

  fun createBlankCanvasBitmap(transparent: Boolean = false): Bitmap {
    val bmp = Bitmap.createBitmap(RASTER_WIDTH, RASTER_HEIGHT, Bitmap.Config.ARGB_8888)
    if (!transparent) {
      bmp.eraseColor(AndroidColor.rgb(0xF7, 0xF3, 0xEB))
    }
    return bmp
  }

  /**
   * Exports the finished drawing to device MediaStore Pictures as a crisp PNG (§5).
   */
  fun saveCanvasToGalleryPng(
    context: Context,
    ops: List<CanvasOpDoc>,
    transparentBackground: Boolean = false,
  ): Result<String> = runCatching {
    val bmp = createBlankCanvasBitmap(transparent = transparentBackground)
    val canvas = AndroidCanvas(bmp)
    val effective = resolveEffectiveOps(ops)
    for (op in effective) {
      renderOpToBitmapCanvas(canvas, op)
    }

    val filename = "DoodleDash_${System.currentTimeMillis()}.png"
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
      put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/DoodleDash")
      }
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
      ?: error("Unable to create MediaStore entry")
    resolver.openOutputStream(uri)?.use { out ->
      bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    filename
  }

  /**
   * Generates bot illustration vector strokes for any secret word so solo / bot turns draw lively sketches!
   */
  fun generateBotIllustrationOps(
    roomId: String,
    authorId: String,
    word: String,
    memberUids: List<String>,
  ): List<CanvasOpDoc> {
    val cx = LOGICAL_WIDTH / 2f
    val cy = LOGICAL_HEIGHT / 2f
    val ops = mutableListOf<CanvasOpDoc>()

    // 1. Central botanical / object silhouette
    val circlePts = (0..36).map { i ->
      val ang = (2.0 * PI * i) / 36.0
      val r = 180f + 25f * sin(ang * 5.0).toFloat()
      TimedPoint(cx + r * cos(ang).toFloat(), cy + r * sin(ang).toFloat(), 0.75f, 16)
    }
    ops.add(
      CanvasOpDoc(
        id = "bot_op_1",
        roomId = roomId,
        authorId = authorId,
        seq = 1L,
        opType = "STROKE",
        tool = DrawingTool.INK_PEN.name,
        colorHex = "#C86446",
        sizePx = 12L,
        opacity = 100L,
        symmetryMode = SymmetryMode.NONE.name,
        seed = word.hashCode().toLong(),
        encodedData = encodeStrokePoints(circlePts),
        memberUids = memberUids
      )
    )

    // 2. Inner core or stem
    val stemPts = listOf(
      TimedPoint(cx, cy + 160f, 0.8f, 16),
      TimedPoint(cx - 20f, cy + 280f, 0.8f, 16),
      TimedPoint(cx + 10f, cy + 390f, 0.8f, 16)
    )
    ops.add(
      CanvasOpDoc(
        id = "bot_op_2",
        roomId = roomId,
        authorId = authorId,
        seq = 2L,
        opType = "STROKE",
        tool = DrawingTool.MARKER.name,
        colorHex = "#3B6E4C",
        sizePx = 16L,
        opacity = 100L,
        symmetryMode = SymmetryMode.MIRROR_V.name,
        seed = (word.hashCode() + 7).toLong(),
        encodedData = encodeStrokePoints(stemPts),
        memberUids = memberUids
      )
    )

    // 3. Sunlit Ochre detail petals / rays
    val petalPts = (0..24).map { i ->
      val ang = (2.0 * PI * i) / 24.0
      TimedPoint(cx + 95f * cos(ang).toFloat(), cy + 95f * sin(ang).toFloat(), 0.7f, 16)
    }
    ops.add(
      CanvasOpDoc(
        id = "bot_op_3",
        roomId = roomId,
        authorId = authorId,
        seq = 3L,
        opType = "STROKE",
        tool = DrawingTool.CALLIGRAPHY.name,
        colorHex = "#DDA15E",
        sizePx = 14L,
        opacity = 95L,
        symmetryMode = SymmetryMode.RADIAL_4.name,
        seed = (word.hashCode() + 19).toLong(),
        encodedData = encodeStrokePoints(petalPts),
        memberUids = memberUids
      )
    )
    return ops
  }

  fun parseHexColor(hex: String): Int {
    return try {
      AndroidColor.parseColor(if (hex.startsWith("#")) hex else "#$hex")
    } catch (_: Exception) {
      AndroidColor.rgb(0x26, 0x23, 0x1F)
    }
  }

  fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%02X%02X%02X", (argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
  }

  private fun applyAlpha(rgbColor: Int, alpha: Int): Int {
    return AndroidColor.argb(
      alpha.coerceIn(0, 255),
      AndroidColor.red(rgbColor),
      AndroidColor.green(rgbColor),
      AndroidColor.blue(rgbColor)
    )
  }
}
