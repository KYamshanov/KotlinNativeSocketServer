package packets


class GetActiveUsersPacketRq(
    packetData: PacketMetaData? = null
) : AbstractPacket(packetData) {

    override fun toBytes(): ByteArray = ByteArray(0)

    companion object {

        fun fromBytes(): GetActiveUsersPacketRq =
            GetActiveUsersPacketRq()

    }


}