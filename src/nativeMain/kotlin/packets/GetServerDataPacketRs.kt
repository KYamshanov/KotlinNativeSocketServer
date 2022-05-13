package packets


class GetServerDataPacketRs(
    val serverData: ServerData,
    packetData: PacketMetaData? = null
) : AbstractPacket(packetData) {

    override fun toBytes(): ByteArray = "${serverData.serverName}:${serverData.maxPlayers}".encodeToByteArray()

    companion object {

        fun fromBytes(byteArray: ByteArray) =
            GetServerDataPacketRs(byteArray.decodeToString().split(":", limit = 2).let {
                ServerData(it[0], it[1].toInt())
            })
    }


}