import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.decodeFromByteArray

@Serializable
data class TestBackup(
    @ProtoNumber(205) var customGenre: List<String>? = null
)

fun main() {
    try {
        val b = TestBackup()
        val bytes = ProtoBuf.encodeToByteArray(TestBackup.serializer(), b)
        val decoded = ProtoBuf.decodeFromByteArray(TestBackup.serializer(), bytes)
        println("Success")
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
