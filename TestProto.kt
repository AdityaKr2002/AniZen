import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

@Serializable
data class OldModel(
    @ProtoNumber(1) val id: Int,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val unknown: String = "unknown"
)

@Serializable
data class NewModel(
    @ProtoNumber(1) val id: Int,
    @ProtoNumber(2) val name: String
)

@Serializable
data class RequiredModel(
    @ProtoNumber(1) val id: Int,
    @ProtoNumber(2) val name: String,
    @ProtoNumber(3) val req: String
)

fun main() {
    val old = OldModel(1, "test")
    val bytes = ProtoBuf.encodeToByteArray(OldModel.serializer(), old)
    
    try {
        val new = ProtoBuf.decodeFromByteArray(NewModel.serializer(), bytes)
        println("Unknown fields ignored: $new")
    } catch (e: Exception) {
        println("Failed on unknown field: ${e.message}")
    }

    val newModel = NewModel(1, "test")
    val bytes2 = ProtoBuf.encodeToByteArray(NewModel.serializer(), newModel)

    try {
        val req = ProtoBuf.decodeFromByteArray(RequiredModel.serializer(), bytes2)
        println("Missing fields ignored: $req")
    } catch (e: Exception) {
        println("Failed on missing field: ${e.message}")
    }
}
