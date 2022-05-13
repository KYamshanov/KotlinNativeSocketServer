package packets

class SendMessagePacket(
    packetMetaData: PacketMetaData? = null,
    val clientId: Int,
    val message: String
) : AbstractPacket(packetMetaData) {

    override fun toBytes(): ByteArray =
        "$clientId:$message".encodeToByteArray()

    override fun toString(): String {
        return "SendMessagePacket(clientId=$clientId, message='$message')"
    }

    companion object {

        fun fromBytes(byteArray: ByteArray): SendMessagePacket =
            byteArray.decodeToString().let {
                val parts = it.split(":", limit = 2)
                SendMessagePacket(clientId = parts[0].toInt(), message = parts[1])
            }
    }
}