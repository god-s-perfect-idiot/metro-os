package com.metro.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.metro.music.data.LocalArtworkFetcher
import com.zemer.cipher.CipherDeobfuscator
import com.zemer.cipher.ZemerCipher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class MusicApp : Application(), ImageLoaderFactory {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG && Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        // Metrolist/zemer stream unlock — cipher WebView + remote player_configs.
        ZemerCipher.initialize(applicationContext, debugLogging = BuildConfig.DEBUG)
        appScope.launch {
            runCatching { CipherDeobfuscator.prewarm() }
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(LocalArtworkFetcher.Factory()) }
        .build()
}
