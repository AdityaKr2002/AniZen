import okhttp3.Headers

fun main() {
    val headers = Headers.Builder().add("User-Agent", "Test").build()
    headers.forEach { (key, value) ->
        println("$key: $value")
    }
}
