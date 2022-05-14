package packets


class JoinToGamePacket(
    val playerName: String,
    packetData: PacketMetaData? = null
) : AbstractPacket(packetData) {

    override fun toBytes(): ByteArray = playerName.encodeToByteArray()

    companion object {

        fun fromBytes(bytes: ByteArray): JoinToGamePacket =
            JoinToGamePacket(playerName = bytes.decodeToString())

    }
}