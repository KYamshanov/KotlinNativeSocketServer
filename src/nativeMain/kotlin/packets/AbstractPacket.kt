package packets

abstract class AbstractPacket(
    override var packetMetaData: PacketMetaData? = null,
) : Packet