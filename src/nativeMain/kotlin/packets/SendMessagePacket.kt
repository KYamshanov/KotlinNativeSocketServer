package packets

class SendMessagePacket(
    val clientId: Int,
    val message: String
) : Packet {

    override fun toBytes(): ByteArray =
        "$clientId:$message".encodeToByteArray()

    override fun toString(): String {
        return "SendMessagePacket(clientId=$clientId, message='$message')"
    }

    companion object {

        fun fromBytes(byteArray: ByteArray): SendMessagePacket =
            byteArray.decodeToString().let {
                val parts = it.split(":", limit = 2)
                SendMessagePacket(parts[0].toInt(), parts[1])
            }
    }
}