package packets

class GetInfoPacketRs(
    packetMetaData: PacketMetaData? = null,
    val clientId: Int
) : AbstractPacket(packetMetaData) {

    override fun toBytes(): ByteArray =
        "clientId:${clientId}".encodeToByteArray()

    companion object {

        fun fromBytes(byteArray: ByteArray): GetInfoPacketRs =
            GetInfoPacketRs(clientId = byteArray.decodeToString().split(":")[1].toInt())
    }
}