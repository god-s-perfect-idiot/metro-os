package com.metro.hub.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.Base64
import android.util.Xml
import androidx.core.graphics.PathParser
import java.io.StringReader
import org.xmlpull.v1.XmlPullParser

/**
 * Decodes Hub catalog logos from Firestore.
 *
 * Inline Android vector XML cannot use [android.graphics.drawable.Drawable.createFromXml]
 * on modern platform versions — those APIs require a binary [XmlBlock] parser and throw
 * ClassCastException on text pull parsers. We rasterize simple `<vector>` / `<group>` /
 * `<path>` documents ourselves instead.
 *
 * Supported (enough for metro-ui-android suite glyphs + sync catalog vectors):
 * - viewport / width / height
 * - nested `<group>` with translate, scale, pivot, rotation
 * - `<path>` fill / stroke / fillType / fillAlpha / strokeAlpha / strokeLineCap
 */
object HubLogoDecoder {
    /**
     * True when [value] is a remote image URL (http/https), not inline vector XML.
     * Firestore `logoXml` may hold either a vector drawable string or a PNG/SVG URL.
     */
    fun isRemoteLogoUrl(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
    }

    fun bitmapFromPngBase64(base64: String): Bitmap? {
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun bitmapFromLogoXml(context: Context, logoXml: String, sizePx: Int = 192): Bitmap? {
        if (isRemoteLogoUrl(logoXml)) return null
        return runCatching { rasterizeVectorXml(logoXml.trim(), sizePx) }.getOrNull()
    }

    private sealed interface VectorNode {
        data class Group(
            val translateX: Float,
            val translateY: Float,
            val scaleX: Float,
            val scaleY: Float,
            val pivotX: Float,
            val pivotY: Float,
            val rotation: Float,
            val children: List<VectorNode>,
        ) : VectorNode

        data class PathNode(
            val pathData: String,
            val fillColor: Int?,
            val strokeColor: Int?,
            val strokeWidth: Float,
            val fillType: Path.FillType,
            val strokeCap: Paint.Cap,
        ) : VectorNode
    }

    private data class ParsedVector(
        val viewportWidth: Float,
        val viewportHeight: Float,
        val roots: List<VectorNode>,
    )

    private fun rasterizeVectorXml(logoXml: String, sizePx: Int): Bitmap? {
        val vector = parseVectorXml(logoXml) ?: return null
        if (vector.viewportWidth <= 0f || vector.viewportHeight <= 0f || vector.roots.isEmpty()) {
            return null
        }
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = minOf(sizePx / vector.viewportWidth, sizePx / vector.viewportHeight)
        val dx = (sizePx - vector.viewportWidth * scale) / 2f
        val dy = (sizePx - vector.viewportHeight * scale) / 2f
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
        }
        for (node in vector.roots) {
            drawNode(canvas, node, fillPaint, strokePaint)
        }
        return bitmap
    }

    private fun drawNode(
        canvas: Canvas,
        node: VectorNode,
        fillPaint: Paint,
        strokePaint: Paint,
    ) {
        when (node) {
            is VectorNode.Group -> {
                canvas.save()
                canvas.concat(node.toMatrix())
                for (child in node.children) {
                    drawNode(canvas, child, fillPaint, strokePaint)
                }
                canvas.restore()
            }
            is VectorNode.PathNode -> {
                val path = PathParser.createPathFromPathData(node.pathData) ?: return
                path.fillType = node.fillType
                node.fillColor?.let { color ->
                    if (Color.alpha(color) > 0) {
                        fillPaint.color = color
                        canvas.drawPath(path, fillPaint)
                    }
                }
                if (node.strokeColor != null &&
                    Color.alpha(node.strokeColor) > 0 &&
                    node.strokeWidth > 0f
                ) {
                    strokePaint.color = node.strokeColor
                    strokePaint.strokeWidth = node.strokeWidth
                    strokePaint.strokeCap = node.strokeCap
                    canvas.drawPath(path, strokePaint)
                }
            }
        }
    }

    private fun VectorNode.Group.toMatrix(): Matrix {
        val matrix = Matrix()
        // Same order as Android VectorDrawable VGroup.updateLocalMatrix().
        matrix.postTranslate(-pivotX, -pivotY)
        matrix.postScale(scaleX, scaleY)
        if (rotation != 0f) {
            matrix.postRotate(rotation)
        }
        matrix.postTranslate(translateX + pivotX, translateY + pivotY)
        return matrix
    }

    private fun parseVectorXml(logoXml: String): ParsedVector? {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(StringReader(logoXml))

        var viewportWidth = 0f
        var viewportHeight = 0f
        val rootChildren = mutableListOf<VectorNode>()
        val groupStack = ArrayDeque<MutableList<VectorNode>>()
        val groupAttrsStack = ArrayDeque<GroupAttrs>()

        fun currentChildren(): MutableList<VectorNode> =
            groupStack.lastOrNull() ?: rootChildren

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "vector" -> {
                        viewportWidth = parser.floatAttr("viewportWidth")
                            ?: parser.floatAttr("width")
                            ?: 0f
                        viewportHeight = parser.floatAttr("viewportHeight")
                            ?: parser.floatAttr("height")
                            ?: 0f
                    }
                    "group" -> {
                        groupAttrsStack.addLast(
                            GroupAttrs(
                                translateX = parser.floatAttr("translateX") ?: 0f,
                                translateY = parser.floatAttr("translateY") ?: 0f,
                                scaleX = parser.floatAttr("scaleX") ?: 1f,
                                scaleY = parser.floatAttr("scaleY") ?: 1f,
                                pivotX = parser.floatAttr("pivotX") ?: 0f,
                                pivotY = parser.floatAttr("pivotY") ?: 0f,
                                rotation = parser.floatAttr("rotation") ?: 0f,
                            ),
                        )
                        groupStack.addLast(mutableListOf())
                    }
                    "path" -> {
                        val pathData = parser.stringAttr("pathData") ?: return null
                        val fillAlpha = parser.floatAttr("fillAlpha") ?: 1f
                        val strokeAlpha = parser.floatAttr("strokeAlpha") ?: 1f
                        currentChildren() += VectorNode.PathNode(
                            pathData = pathData,
                            fillColor = parser.colorAttr("fillColor")?.withAlphaFactor(fillAlpha),
                            strokeColor = parser.colorAttr("strokeColor")?.withAlphaFactor(strokeAlpha),
                            strokeWidth = parser.floatAttr("strokeWidth") ?: 0f,
                            fillType = parser.fillTypeAttr(),
                            strokeCap = parser.strokeCapAttr(),
                        )
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "group" -> {
                        if (groupStack.isEmpty() || groupAttrsStack.isEmpty()) return null
                        val children = groupStack.removeLast()
                        val attrs = groupAttrsStack.removeLast()
                        currentChildren() += VectorNode.Group(
                            translateX = attrs.translateX,
                            translateY = attrs.translateY,
                            scaleX = attrs.scaleX,
                            scaleY = attrs.scaleY,
                            pivotX = attrs.pivotX,
                            pivotY = attrs.pivotY,
                            rotation = attrs.rotation,
                            children = children,
                        )
                    }
                }
            }
            event = parser.next()
        }
        if (viewportWidth <= 0f || viewportHeight <= 0f || rootChildren.isEmpty()) return null
        return ParsedVector(viewportWidth, viewportHeight, rootChildren)
    }

    private data class GroupAttrs(
        val translateX: Float,
        val translateY: Float,
        val scaleX: Float,
        val scaleY: Float,
        val pivotX: Float,
        val pivotY: Float,
        val rotation: Float,
    )

    private fun XmlPullParser.stringAttr(name: String): String? {
        // Prefer android: namespace, then un-namespaced (defensive for pasted XML).
        val androidNs = "http://schemas.android.com/apk/res/android"
        return getAttributeValue(androidNs, name)
            ?: getAttributeValue(null, name)
            ?: getAttributeValue("", name)
    }

    private fun XmlPullParser.floatAttr(name: String): Float? {
        val raw = stringAttr(name)?.trim()?.removeSuffix("dp")?.removeSuffix("dip") ?: return null
        return raw.toFloatOrNull()
    }

    private fun XmlPullParser.colorAttr(name: String): Int? {
        val raw = stringAttr(name)?.trim() ?: return null
        return parseColorValue(raw)
    }

    private fun XmlPullParser.fillTypeAttr(): Path.FillType {
        return when (stringAttr("fillType")?.trim()?.lowercase()) {
            "evenodd" -> Path.FillType.EVEN_ODD
            else -> Path.FillType.WINDING
        }
    }

    private fun XmlPullParser.strokeCapAttr(): Paint.Cap {
        return when (stringAttr("strokeLineCap")?.trim()?.lowercase()) {
            "square" -> Paint.Cap.SQUARE
            "butt" -> Paint.Cap.BUTT
            else -> Paint.Cap.ROUND
        }
    }

    private fun parseColorValue(raw: String): Int? {
        if (raw == "@android:color/transparent" || raw.equals("transparent", ignoreCase = true)) {
            return Color.TRANSPARENT
        }
        if (raw == "@android:color/white") return Color.WHITE
        if (raw == "@android:color/black") return Color.BLACK
        if (!raw.startsWith("#")) return null
        return runCatching { Color.parseColor(raw) }.getOrNull()
    }

    private fun Int.withAlphaFactor(factor: Float): Int {
        val clamped = factor.coerceIn(0f, 1f)
        val alpha = (Color.alpha(this) * clamped).toInt().coerceIn(0, 255)
        return Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))
    }
}
