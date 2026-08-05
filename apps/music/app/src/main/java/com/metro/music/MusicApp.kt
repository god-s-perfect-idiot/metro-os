package com.metro.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.metro.music.data.LocalArtworkFetcher

class MusicApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(LocalArtworkFetcher.Factory()) }
        .build()
}
