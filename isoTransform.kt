package org.example.myvisa.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Converts 3D (x, y, z) to 2D isometric (isoX, isoY).
 */
private fun isoTransform(x: Float, y: Float, z: Float, angleDeg: Float): Offset {
    val rad = angleDeg * (PI.toFloat() / 180f)
    val isoX = (x - y) * cos(rad)
    val isoY = (x + y) * sin(rad) - z
    return Offset(isoX, isoY)
}

/**
 * Draws a single isometric “cube” with:
 *  - Top face: Gray, drawn last with a small corner radius in 2D
 *  - Side faces: Black
 *  - [width], [depth], [height] for 3D size
 *  - [angleDegrees] is typically 30
 *  - [topCornerRadiusPx] small so it doesn’t distort
 *
 * If the top face still looks black or distorted:
 * 1) Ensure [topColor] != black
 * 2) Keep [topCornerRadiusPx] small
 * 3) Possibly enlarge the Canvas or reduce width/depth/height
 */
@Composable
fun IsometricRoundedCube(
    modifier: Modifier = Modifier,
    // 3D position of bottom-left-back corner
    x: Float = 0f,
    y: Float = 0f,
    z: Float = 0f,
    // Dimensions of the box
    width: Float = 3f,
    depth: Float = 3f,
    height: Float = 2f,
    // Isometric angle (usually 30)
    angleDegrees: Float = 30f,
    // Rounded corner radius (in px) for the top face
    topCornerRadiusPx: Float = 6f,

    // Colors
    topColor: Color = Color.Gray,
    sideColor: Color = Color.Black,

    // Outline settings (optional)
    outlineColor: Color = Color.Black,
    outlineWidthPx: Float = 0f
) {
    val scale = 40f  // Adjust if shape is too large or too small

    // We'll just use a 200.dp Canvas by default
    Canvas(modifier = modifier.size(200.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)

        // ---- 3D -> 2D corner transforms ----
        val topLeftBack3D   = isoTransform(x,         y,         z + height, angleDegrees)
        val topRightBack3D  = isoTransform(x + width, y,         z + height, angleDegrees)
        val topLeftFront3D  = isoTransform(x,         y + depth, z + height, angleDegrees)
        val topRightFront3D = isoTransform(x + width, y + depth, z + height, angleDegrees)

        val bottomLeftBack3D   = isoTransform(x,         y,         z, angleDegrees)
        val bottomRightBack3D  = isoTransform(x + width, y,         z, angleDegrees)
        val bottomLeftFront3D  = isoTransform(x,         y + depth, z, angleDegrees)
        val bottomRightFront3D = isoTransform(x + width, y + depth, z, angleDegrees)

        // Shift to center & scale
        val topLeftBack    = center + (topLeftBack3D * scale)
        val topRightBack   = center + (topRightBack3D * scale)
        val topLeftFront   = center + (topLeftFront3D * scale)
        val topRightFront  = center + (topRightFront3D * scale)

        val bottomLeftBack   = center + (bottomLeftBack3D * scale)
        val bottomRightBack  = center + (bottomRightBack3D * scale)
        val bottomLeftFront  = center + (bottomLeftFront3D * scale)
        val bottomRightFront = center + (bottomRightFront3D * scale)

        // 1) Draw BOTTOM face (straight edges)
        drawQuadFace(
            listOf(
                bottomLeftBack,
                bottomRightBack,
                bottomRightFront,
                bottomLeftFront
            ),
            sideColor,
            outlineColor,
            outlineWidthPx
        )
        // 2) Draw LEFT face
        drawQuadFace(
            listOf(
                bottomLeftBack,
                topLeftBack,
                topLeftFront,
                bottomLeftFront
            ),
            sideColor,
            outlineColor,
            outlineWidthPx
        )
        // 3) Draw RIGHT face
        drawQuadFace(
            listOf(
                bottomRightBack,
                topRightBack,
                topRightFront,
                bottomRightFront
            ),
            sideColor,
            outlineColor,
            outlineWidthPx
        )
        // 4) Draw FRONT face
        drawQuadFace(
            listOf(
                bottomLeftFront,
                topLeftFront,
                topRightFront,
                bottomRightFront
            ),
            sideColor,
            outlineColor,
            outlineWidthPx
        )
        // 5) Draw BACK face
        drawQuadFace(
            listOf(
                bottomLeftBack,
                topLeftBack,
                topRightBack,
                bottomRightBack
            ),
            sideColor,
            outlineColor,
            outlineWidthPx
        )

        // 6) Finally, draw the TOP face with round corners *last*
        drawRoundedTopFace(
            topLeftBack,
            topRightBack,
            topRightFront,
            topLeftFront,
            cornerRadius = topCornerRadiusPx,
            color = topColor,
            outlineColor = outlineColor,
            outlineWidthPx = outlineWidthPx
        )
    }
}

/**
 * Draw a straight-edged quadrilateral face.
 */
private fun DrawScope.drawQuadFace(
    points: List<Offset>,
    fillColor: Color,
    outlineColor: Color,
    outlineWidthPx: Float
) {
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        lineTo(points[1].x, points[1].y)
        lineTo(points[2].x, points[2].y)
        lineTo(points[3].x, points[3].y)
        close()
    }
    drawPath(path, fillColor)
    if (outlineWidthPx > 0f) {
        drawPath(path, outlineColor, style = Stroke(width = outlineWidthPx))
    }
}

/**
 * Draw the top face with corner arcs in 2D.
 * Doing this last ensures no side face overwrites the top.
 */
private fun DrawScope.drawRoundedTopFace(
    p1: Offset,
    p2: Offset,
    p3: Offset,
    p4: Offset,
    cornerRadius: Float,
    color: Color,
    outlineColor: Color,
    outlineWidthPx: Float
) {
    // p1 -> p2 -> p3 -> p4. We'll do arcs at each corner.
    val path = Path().apply {
        // Move near p1
        moveTo(p1.x + cornerRadius, p1.y)

        // Arc around p1
        arcTo(
            rect = Rect(
                left = p1.x,
                top = p1.y - cornerRadius,
                right = p1.x + cornerRadius * 2,
                bottom = p1.y + cornerRadius
            ),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        // line to p2 minus radius
        lineTo(p2.x - cornerRadius, p2.y)
        // Arc p2
        arcTo(
            Rect(
                left = p2.x - cornerRadius * 2,
                top = p2.y - cornerRadius,
                right = p2.x,
                bottom = p2.y + cornerRadius
            ),
            startAngleDegrees = 270f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        // line to p3 plus radius in y
        lineTo(p3.x, p3.y + cornerRadius)
        // Arc p3
        arcTo(
            Rect(
                left = p3.x - cornerRadius,
                top = p3.y,
                right = p3.x + cornerRadius,
                bottom = p3.y + cornerRadius * 2
            ),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        // line to p4 minus radius in y
        lineTo(p4.x, p4.y - cornerRadius)
        // Arc p4
        arcTo(
            Rect(
                left = p4.x - cornerRadius,
                top = p4.y - cornerRadius * 2,
                right = p4.x + cornerRadius,
                bottom = p4.y
            ),
            startAngleDegrees = 90f,
            sweepAngleDegrees = 90f,
            forceMoveTo = false
        )
        close()
    }

    drawPath(path, color)
    if (outlineWidthPx > 0f) {
        drawPath(path, outlineColor, style = Stroke(outlineWidthPx))
    }
}
