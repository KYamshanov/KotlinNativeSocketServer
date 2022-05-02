package packets


interface PacketFactory {

    fun producePacket(byteArray: ByteArray): Packet?
    fun serializePacket(packet: Packet): ByteArray?

    companion object {
        fun getInstance(): PacketFactory =
            PacketFactoryImpl()
    }
}

private class PacketFactoryImpl : PacketFactory {

    private val packetProducers: Map<Byte, (ByteArray) -> Packet> = mutableMapOf<Byte, (ByteArray) -> Packet>().apply {
        putPair(PacketIdentifiers.ECHO_PACKET)
        putPair(PacketIdentifiers.SEND_MESSAGE_PACKET)
        putPair(PacketIdentifiers.GETA_ACTIVE_USERS_PACKET_RS)
        putPair(PacketIdentifiers.GETA_ACTIVE_USERS_PACKET_RQ)
        putPair(PacketIdentifiers.GET_INFO_PACKET_RQ)
        putPair(PacketIdentifiers.GET_INFO_PACKET_RS)
    }

    override fun producePacket(byteArray: ByteArray): Packet? =
        packetProducers[byteArray[0]]?.let {
            it(byteArray.copyOfRange(1, byteArray.size))
        }

    override fun serializePacket(packet: Packet): ByteArray? = when (packet) {
        is EchoPacket -> PacketIdentifiers.ECHO_PACKET.first
        is SendMessagePacket -> PacketIdentifiers.SEND_MESSAGE_PACKET.first
        is GetActiveUsersPacketRs -> PacketIdentifiers.GETA_ACTIVE_USERS_PACKET_RS.first
        is GetActiveUsersPacketRq -> PacketIdentifiers.GETA_ACTIVE_USERS_PACKET_RQ.first
        is GetInfoPacketRq -> PacketIdentifiers.GET_INFO_PACKET_RQ.first
        is GetInfoPacketRs -> PacketIdentifiers.GET_INFO_PACKET_RS.first
        else -> null
    }?.toByteArray()?.mergeByteArray(packet.toBytes())
}

private object PacketIdentifiers {

    val ECHO_PACKET: Pair<Byte, (ByteArray) -> Packet> = 1.toByte() to { EchoPacket.fromBytes(it) }
    val SEND_MESSAGE_PACKET: Pair<Byte, (ByteArray) -> Packet> = 2.toByte() to { SendMessagePacket.fromBytes(it) }
    val GETA_ACTIVE_USERS_PACKET_RS: Pair<Byte, (ByteArray) -> Packet> =
        3.toByte() to { GetActiveUsersPacketRs.fromBytes(it) }
    val GETA_ACTIVE_USERS_PACKET_RQ: Pair<Byte, (ByteArray) -> Packet> =
        4.toByte() to { GetActiveUsersPacketRq.fromBytes() }
    val GET_INFO_PACKET_RQ: Pair<Byte, (ByteArray) -> Packet> = 5.toByte() to { GetInfoPacketRq.fromBytes() }
    val GET_INFO_PACKET_RS: Pair<Byte, (ByteArray) -> Packet> = 6.toByte() to { GetInfoPacketRs.fromBytes(it) }

}

private fun Byte.toByteArray() = ByteArray(1) { this }

private fun <K, V> MutableMap<K, V>.putPair(pair: Pair<K, V>) {
    this[pair.first] = pair.second
}