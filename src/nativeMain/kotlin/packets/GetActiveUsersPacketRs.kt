package packets

class GetActiveUsersPacketRs(
    packetData: PacketMetaData? = null,
    val users: List<Int>
) : AbstractPacket(packetData) {

    override fun toBytes(): ByteArray =
        users.joinToString(":").encodeToByteArray()

    companion object {

        fun fromBytes(byteArray: ByteArray): GetActiveUsersPacketRs =
            GetActiveUsersPacketRs(users = byteArray.decodeToString().split(":").map { it.toInt() })
    }
}