package packets


class BroadcastMessagePacket(
    packetData: PacketMetaData? = null, val message: Packet, val packetFactory: PacketFactory
) : AbstractPacket(packetData) {

    override fun toBytes(): ByteArray = packetFactory.serializePacket(message) ?: ByteArray(0)

    companion object {

        fun fromBytes(bytes: ByteArray, packetFactory: PacketFactory): BroadcastMessagePacket =
            packetFactory.producePacket(bytes)
                ?.let { BroadcastMessagePacket(message = it, packetFactory = packetFactory) }
                ?: throw Exception("Нельзя создать пакет из сообщения")

    }
}