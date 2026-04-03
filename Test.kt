fun isValidFatFilenameChar(c: Char): Boolean {
    if (0x00.toChar() <= c && c <= 0x1f.toChar()) {
        return false
    }
    return when (c) {
        '"', '*', '/', ':', '<', '>', '?', '\\', '|', 0x7f.toChar() -> false
        else -> true
    }
}
fun main() {
    val name = "Bullet • Point.mp4"
    val sb = StringBuilder(name.length)
    name.forEach { c ->
        if (isValidFatFilenameChar(c)) {
            sb.append(c)
        } else {
            sb.append('_')
        }
    }
    println("Original: $name")
    println("Valid: ${sb.toString()}")
}
