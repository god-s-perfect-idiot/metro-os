package com.metro.hub.data

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Xml
import androidx.core.graphics.drawable.toBitmap
import java.io.ByteArrayInputStream
import java.io.StringReader
import org.xmlpull.v1.XmlPullParser

object HubLogoDecoder {
    fun drawableFromLogoXml(resources: Resources, logoXml: String): Drawable? {
        return runCatching {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            parser.setInput(StringReader(logoXml))
            var event = parser.eventType
            while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT) {
                event = parser.next()
            }
            if (event != XmlPullParser.START_TAG) return null
            Drawable.createFromXml(resources, parser, null)
        }.getOrNull()
    }

    fun bitmapFromPngBase64(base64: String): Bitmap? {
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun bitmapFromLogoXml(context: Context, logoXml: String, sizePx: Int = 192): Bitmap? {
        val drawable = drawableFromLogoXml(context.resources, logoXml) ?: return null
        return drawable.toBitmap(width = sizePx, height = sizePx)
    }
}
