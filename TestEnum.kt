import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

@Serializable
enum class FetchType { Seasons, Episodes }

@Serializable
data class EnumModel(@ProtoNumber(1) val type: FetchType = FetchType.Episodes)

@Serializable
data class IntModel(@ProtoNumber(1) val type: Int = 1)

fun main() {
    val intModel = IntModel(1)
    val bytes = ProtoBuf.encodeToByteArray(IntModel.serializer(), intModel)
    
    try {
        val enumModel = ProtoBuf.decodeFromByteArray(EnumModel.serializer(), bytes)
        println("Success: $enumModel")
    } catch (e: Exception) {
        println("Crash: ${e.message}")
    }
}
