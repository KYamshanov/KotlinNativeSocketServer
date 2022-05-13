package packets

interface Packet {

    var packetMetaData: PacketMetaData?

    fun toBytes(): ByteArray
}