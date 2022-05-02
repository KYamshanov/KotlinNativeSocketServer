package packets

class EchoPacket(
    private val text: String
) : Packet {

    override fun toBytes(): ByteArray =
        text.encodeToByteArray()

    override fun toString(): String {
        return "EchoPacket(text='$text')"
    }

    companion object {

        fun fromBytes(byteArray: ByteArray): EchoPacket =
            EchoPacket(byteArray.decodeToString())

    }
}