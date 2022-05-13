package packets

class GetInfoPacketRq(
    packetData: PacketMetaData? = null
    ) : AbstractPacket(packetData) {


    override fun toBytes(): ByteArray = ByteArray(0)

    companion object {

        fun fromBytes(): GetInfoPacketRq =
            GetInfoPacketRq()

    }


}