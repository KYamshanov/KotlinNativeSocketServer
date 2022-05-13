package packets

class EchoPacket(
    packetData: PacketMetaData? = null,
    val text: String
) : AbstractPacket(packetData) {

    override fun toBytes(): ByteArray =
        text.encodeToByteArray()

    override fun toString(): String {
        return "EchoPacket(text='$text')"
    }

    companion object {

        fun fromBytes(byteArray: ByteArray): EchoPacket =
            EchoPacket(text = byteArray.decodeToString())

    }
}