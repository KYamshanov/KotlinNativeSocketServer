package packets


class GetServerDataPacketRs(
    val serverData: ServerData,
    val status: String,
    packetData: PacketMetaData? = null
) : AbstractPacket(packetData) {

    override fun toBytes(): ByteArray =
        "${serverData.serverName}:${serverData.maxPlayers}:${serverData}".encodeToByteArray()

    companion object {

        fun fromBytes(byteArray: ByteArray) =
            byteArray.decodeToString().split(":", limit = 3).let {
                GetServerDataPacketRs(ServerData(it[0], it[1].toInt()), it[2])
            }
    }


}