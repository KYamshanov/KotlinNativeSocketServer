package packets

class GetInfoPacketRs(
    private val clientId: Int
) : Packet {

    override fun toBytes(): ByteArray =
        "clientId:${clientId}".encodeToByteArray()

    companion object {

        fun fromBytes(byteArray: ByteArray): GetInfoPacketRs =
            GetInfoPacketRs(byteArray.decodeToString().split(":")[1].toInt())
    }
}