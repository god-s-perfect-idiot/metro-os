package com.metro.music.ytmusic.potoken

class PoTokenException(message: String) : Exception(message)

class BadWebViewException(message: String) : Exception(message)

fun buildExceptionForJsError(error: String): Exception =
    if (error.contains("SyntaxError")) BadWebViewException(error) else PoTokenException(error)
