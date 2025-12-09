package dev.ewio.util

import dev.ewio.util.GL

// Ermittelt den ersten StackTraceFrame außerhalb des Logging-Pakets und formatiert "Datei:funktion:zeile"
private fun callerInfo(): String {
    val st = Throwable().stackTrace
    for (el in st) {
        val cn = el.className
        if (!cn.startsWith("dev.ewio.util") && !cn.startsWith("java.lang")) {
            val file = el.fileName ?: "UnknownFile"
            val method = el.methodName ?: "UnknownMethod"
            val line = if (el.lineNumber >= 0) el.lineNumber else -1
            return "$file:$method:$line"
        }
    }
    // Fallback
    val el = st.getOrNull(1)
    return if (el != null) "${el.fileName}:${el.methodName}:${el.lineNumber}" else "Unknown:Unknown:-1"
}

fun logInfo(message: String) {
    if(GL.level == LogLevel.INFO) GL.logger.info("[${callerInfo()}] $message")
}

fun logWarning(message: String) {
    if(GL.level == LogLevel.INFO || GL.level == LogLevel.WARNING ) GL.logger.warning("[${callerInfo()}][WARN] $message")
}

fun logSevere(message: String) {
    if(GL.level == LogLevel.INFO || GL.level == LogLevel.WARNING || GL.level == LogLevel.SEVERE) GL.logger.severe("[${callerInfo()}][SEVERE] $message")
}
