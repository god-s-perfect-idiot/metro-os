plugins {
    // Pinned to metro music host versions so includeBuild composite resolves cleanly.
    id("com.android.library") version "8.7.3" apply false
    kotlin("android") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
}
