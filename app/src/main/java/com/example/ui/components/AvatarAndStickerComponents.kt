package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AvatarConfig
import com.example.data.model.StickerDef
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.abs

private val AVATAR_BG_COLORS = listOf(
  Color(0xFFDCE8DC), // Sage Mist
  Color(0xFFF7DDD4), // Soft Terracotta
  Color(0xFFFAE8CE), // Sunlit Ochre
  Color(0xFFD8E2DC), // Lichen
  Color(0xFFEAD7D1), // Rose Clay
  Color(0xFFD4E4EA), // Sky Dew
  Color(0xFFE2D9E8), // Lavender
  Color(0xFFE9EDC9), // Sprout
)

private val SKIN_TONES = listOf(
  Color(0xFFF9E4D4),
  Color(0xFFF1C6A7),
  Color(0xFFE0A982),
  Color(0xFFC6865A),
  Color(0xFF8D5524),
  Color(0xFF5C3818),
  Color(0xFFA3B18A), // Botanical Dryad Sage
  Color(0xFFE07A5F), // Terracotta Spirit
)

private val HAIR_COLORS = listOf(
  Color(0xFF26231F), // Espresso
  Color(0xFF7F5539), // Chestnut
  Color(0xFFC86446), // Auburn Terracotta
  Color(0xFFDDA15E), // Golden Wheat
  Color(0xFF3B6E4C), // Moss Green
  Color(0xFF6D597A), // Plum
  Color(0xFFF7F3EB), // Silver Parchment
  Color(0xFF457B9D), // Indigo
)

/**
 * Shared layered vector `<AvatarView />` (§6) rendered from compact JSON config (<300B).
 */
@Composable
fun AvatarView(
  avatarJson: String,
  modifier: Modifier = Modifier,
  size: Dp = 48.dp,
) {
  val config = AvatarConfig.fromJson(avatarJson)
  val bgColor = AVATAR_BG_COLORS[config.bgColorIndex.mod(AVATAR_BG_COLORS.size)]
  val skinColor = SKIN_TONES[config.skinToneIndex.mod(SKIN_TONES.size)]
  val hairColor = HAIR_COLORS[config.hairColorIndex.mod(HAIR_COLORS.size)]
  val inkColor = Color(0xFF26231F)

  Box(
    modifier = modifier
      .size(size)
      .clip(CircleShape)
      .background(bgColor)
      .border(1.5.dp, Color(0xFF26231F).copy(alpha = 0.18f), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height
      val cx = w / 2f
      val cy = h * 0.54f
      val headR = w * 0.30f

      // 1. Shoulders / Collar
      drawArc(
        color = hairColor.copy(alpha = 0.85f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - headR * 1.15f, cy + headR * 0.65f),
        size = Size(headR * 2.3f, headR * 1.4f)
      )

      // 2. Face Shape
      when (config.faceShape.mod(4)) {
        0 -> drawCircle(color = skinColor, radius = headR, center = Offset(cx, cy))
        1 -> drawRoundRect(
          color = skinColor,
          topLeft = Offset(cx - headR * 0.92f, cy - headR * 0.95f),
          size = Size(headR * 1.84f, headR * 1.9f),
          cornerRadius = CornerRadius(headR * 0.45f, headR * 0.45f)
        )
        2 -> drawOval(
          color = skinColor,
          topLeft = Offset(cx - headR * 0.85f, cy - headR * 1.05f),
          size = Size(headR * 1.7f, headR * 2.1f)
        )
        else -> drawCircle(color = skinColor, radius = headR * 0.96f, center = Offset(cx, cy))
      }

      // 3. Brows
      val eyeY = cy - headR * 0.10f
      val eyeOffsetX = headR * 0.36f
      val browY = eyeY - headR * 0.24f
      val browTilt = when (config.browStyle.mod(4)) {
        1 -> -headR * 0.08f
        2 -> headR * 0.08f
        else -> 0f
      }
      drawLine(
        color = inkColor,
        start = Offset(cx - eyeOffsetX - headR * 0.14f, browY - browTilt),
        end = Offset(cx - eyeOffsetX + headR * 0.14f, browY + browTilt),
        strokeWidth = w * 0.035f,
        cap = StrokeCap.Round
      )
      drawLine(
        color = inkColor,
        start = Offset(cx + eyeOffsetX - headR * 0.14f, browY + browTilt),
        end = Offset(cx + eyeOffsetX + headR * 0.14f, browY - browTilt),
        strokeWidth = w * 0.035f,
        cap = StrokeCap.Round
      )

      // 4. Eyes
      when (config.eyeStyle.mod(5)) {
        0 -> {
          drawCircle(inkColor, radius = w * 0.042f, center = Offset(cx - eyeOffsetX, eyeY))
          drawCircle(inkColor, radius = w * 0.042f, center = Offset(cx + eyeOffsetX, eyeY))
        }
        1 -> { // Wink
          drawCircle(inkColor, radius = w * 0.042f, center = Offset(cx - eyeOffsetX, eyeY))
          drawArc(
            color = inkColor,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx + eyeOffsetX - headR * 0.14f, eyeY - headR * 0.08f),
            size = Size(headR * 0.28f, headR * 0.20f),
            style = Stroke(width = w * 0.035f, cap = StrokeCap.Round)
          )
        }
        2 -> { // Starry / Sparkle
          drawCircle(inkColor, radius = w * 0.048f, center = Offset(cx - eyeOffsetX, eyeY))
          drawCircle(Color.White, radius = w * 0.018f, center = Offset(cx - eyeOffsetX - 1f, eyeY - 1f))
          drawCircle(inkColor, radius = w * 0.048f, center = Offset(cx + eyeOffsetX, eyeY))
          drawCircle(Color.White, radius = w * 0.018f, center = Offset(cx + eyeOffsetX - 1f, eyeY - 1f))
        }
        else -> {
          drawCircle(inkColor, radius = w * 0.038f, center = Offset(cx - eyeOffsetX, eyeY))
          drawCircle(inkColor, radius = w * 0.038f, center = Offset(cx + eyeOffsetX, eyeY))
        }
      }

      // 5. Mouth
      val mouthY = cy + headR * 0.30f
      when (config.mouthStyle.mod(5)) {
        0 -> drawArc(
          color = inkColor,
          startAngle = 15f,
          sweepAngle = 150f,
          useCenter = false,
          topLeft = Offset(cx - headR * 0.26f, mouthY - headR * 0.12f),
          size = Size(headR * 0.52f, headR * 0.30f),
          style = Stroke(width = w * 0.038f, cap = StrokeCap.Round)
        )
        1 -> drawArc(
          color = Color(0xFFBC4749),
          startAngle = 0f,
          sweepAngle = 180f,
          useCenter = true,
          topLeft = Offset(cx - headR * 0.28f, mouthY - headR * 0.08f),
          size = Size(headR * 0.56f, headR * 0.34f)
        )
        2 -> drawCircle(color = inkColor, radius = w * 0.038f, center = Offset(cx, mouthY + headR * 0.05f))
        else -> drawLine(
          color = inkColor,
          start = Offset(cx - headR * 0.20f, mouthY + headR * 0.05f),
          end = Offset(cx + headR * 0.22f, mouthY - headR * 0.02f),
          strokeWidth = w * 0.038f,
          cap = StrokeCap.Round
        )
      }

      // 6. Hair / Botanical Hat
      val topY = cy - headR * 0.92f
      when (config.hairHatStyle.mod(6)) {
        0 -> { // Botanical Leaf Sprout
          drawLine(
            color = Color(0xFF3B6E4C),
            start = Offset(cx, topY),
            end = Offset(cx, topY - headR * 0.38f),
            strokeWidth = w * 0.04f,
            cap = StrokeCap.Round
          )
          drawOval(
            color = Color(0xFF5F7A61),
            topLeft = Offset(cx, topY - headR * 0.44f),
            size = Size(headR * 0.45f, headR * 0.25f)
          )
          drawOval(
            color = Color(0xFF3B6E4C),
            topLeft = Offset(cx - headR * 0.45f, topY - headR * 0.38f),
            size = Size(headR * 0.45f, headR * 0.24f)
          )
        }
        1 -> { // Artist Beret
          drawOval(
            color = hairColor,
            topLeft = Offset(cx - headR * 0.95f, topY - headR * 0.22f),
            size = Size(headR * 1.9f, headR * 0.60f)
          )
          drawLine(
            color = hairColor,
            start = Offset(cx, topY - headR * 0.22f),
            end = Offset(cx + headR * 0.12f, topY - headR * 0.38f),
            strokeWidth = w * 0.04f,
            cap = StrokeCap.Round
          )
        }
        2 -> { // Sunhat
          drawOval(
            color = Color(0xFFDDA15E),
            topLeft = Offset(cx - headR * 1.25f, topY - headR * 0.05f),
            size = Size(headR * 2.5f, headR * 0.38f)
          )
          drawArc(
            color = Color(0xFFC86446),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - headR * 0.75f, topY - headR * 0.42f),
            size = Size(headR * 1.5f, headR * 0.75f)
          )
        }
        3 -> { // Curly Crown
          for (i in -2..2) {
            drawCircle(
              color = hairColor,
              radius = headR * 0.26f,
              center = Offset(cx + i * headR * 0.32f, topY + abs(i) * headR * 0.06f)
            )
          }
        }
        4 -> { // Cozy Beanie
          drawArc(
            color = hairColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - headR * 0.95f, topY - headR * 0.35f),
            size = Size(headR * 1.9f, headR * 1.0f)
          )
          drawCircle(color = Color(0xFFF7F3EB), radius = headR * 0.16f, center = Offset(cx, topY - headR * 0.38f))
        }
        else -> { // Flower Crown
          for (i in -2..2) {
            val flowerColor = if (i % 2 == 0) Color(0xFFC86446) else Color(0xFFDDA15E)
            drawCircle(
              color = flowerColor,
              radius = headR * 0.18f,
              center = Offset(cx + i * headR * 0.30f, topY + headR * 0.05f)
            )
          }
        }
      }

      // 7. Accessory
      when (config.accessoryStyle.mod(5)) {
        1 -> { // Round Glasses
          drawCircle(
            color = inkColor,
            radius = headR * 0.22f,
            center = Offset(cx - eyeOffsetX, eyeY),
            style = Stroke(width = w * 0.028f)
          )
          drawCircle(
            color = inkColor,
            radius = headR * 0.22f,
            center = Offset(cx + eyeOffsetX, eyeY),
            style = Stroke(width = w * 0.028f)
          )
          drawLine(
            color = inkColor,
            start = Offset(cx - eyeOffsetX + headR * 0.22f, eyeY),
            end = Offset(cx + eyeOffsetX - headR * 0.22f, eyeY),
            strokeWidth = w * 0.025f
          )
        }
        2 -> { // Monocle
          drawCircle(
            color = Color(0xFFDDA15E),
            radius = headR * 0.24f,
            center = Offset(cx + eyeOffsetX, eyeY),
            style = Stroke(width = w * 0.032f)
          )
        }
        3 -> { // Rosy Paint Cheeks
          drawCircle(
            color = Color(0xFFE07A5F).copy(alpha = 0.55f),
            radius = headR * 0.16f,
            center = Offset(cx - eyeOffsetX * 1.15f, eyeY + headR * 0.26f)
          )
          drawCircle(
            color = Color(0xFFE07A5F).copy(alpha = 0.55f),
            radius = headR * 0.16f,
            center = Offset(cx + eyeOffsetX * 1.15f, eyeY + headR * 0.26f)
          )
        }
      }
    }
  }
}

/**
 * Animated vector sticker badge (§6) — renders bundled sticker IDs with zero server image bandwidth.
 */
@Composable
fun StickerBadgeCard(
  sticker: StickerDef,
  modifier: Modifier = Modifier,
  compact: Boolean = false,
  onClick: (() -> Unit)? = null,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "sticker_pulse")
  val scale by infiniteTransition.animateFloat(
    initialValue = 0.97f,
    targetValue = if (sticker.isAnimated) 1.04f else 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "sticker_scale"
  )

  val primary = runCatching { Color(android.graphics.Color.parseColor(sticker.primaryHex)) }
    .getOrDefault(MaterialTheme.colorScheme.primary)
  val accent = runCatching { Color(android.graphics.Color.parseColor(sticker.accentHex)) }
    .getOrDefault(MaterialTheme.colorScheme.tertiary)

  Card(
    modifier = modifier
      .scale(scale)
      .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = primary.copy(alpha = 0.14f)),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, primary.copy(alpha = 0.5f))
  ) {
    Row(
      modifier = Modifier.padding(horizontal = if (compact) 10.dp else 12.dp, vertical = if (compact) 6.dp else 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Text(
        text = sticker.emojiArt,
        fontSize = if (compact) 18.sp else 22.sp
      )
      Column {
        Surface(
          color = primary,
          shape = RoundedCornerShape(6.dp)
        ) {
          Text(
            text = sticker.badgeText,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            fontSize = 10.sp
          )
        }
        if (!compact) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = sticker.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}

/**
 * Deterministic vector QR-style room invite matrix (§4 & §10) generated directly on Canvas from the 6-char room code.
 */
@Composable
fun RoomCodeQrMatrix(
  roomCode: String,
  modifier: Modifier = Modifier,
  size: Dp = 136.dp,
) {
  val ink = MaterialTheme.colorScheme.onSurface
  val bg = MaterialTheme.colorScheme.surface
  val accent = MaterialTheme.colorScheme.primary

  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(16.dp))
      .background(bg)
      .border(2.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
      .padding(10.dp),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val grid = 13
      val cell = this.size.width / grid
      val hash = roomCode.hashCode()

      fun isFinder(r: Int, c: Int): Boolean {
        val topL = r in 0..3 && c in 0..3
        val topR = r in 0..3 && c in (grid - 4) until grid
        val botL = r in (grid - 4) until grid && c in 0..3
        return topL || topR || botL
      }

      for (r in 0 until grid) {
        for (c in 0 until grid) {
          if (isFinder(r, c)) {
            val isBorder = r == 0 || r == 3 || r == grid - 4 || r == grid - 1 ||
              c == 0 || c == 3 || c == grid - 4 || c == grid - 1
            val isCenter = (r in 1..2 || r in (grid - 3)..(grid - 2)) &&
              (c in 1..2 || c in (grid - 3)..(grid - 2))
            if (isBorder || isCenter) {
              drawRoundRect(
                color = accent,
                topLeft = Offset(c * cell, r * cell),
                size = Size(cell * 0.9f, cell * 0.9f),
                cornerRadius = CornerRadius(2f, 2f)
              )
            }
          } else {
            val bit = ((hash ushr ((r * grid + c) % 28)) xor (r * 31 + c * 17)) and 1
            if (bit == 1) {
              drawRoundRect(
                color = ink,
                topLeft = Offset(c * cell, r * cell),
                size = Size(cell * 0.85f, cell * 0.85f),
                cornerRadius = CornerRadius(2f, 2f)
              )
            }
          }
        }
      }
    }
  }
}
