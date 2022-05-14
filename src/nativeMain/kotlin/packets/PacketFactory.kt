package packets

import kotlin.reflect.KClass


interface PacketFactory {

    val packetsProduceData: List<Triple<KClass<out Packet>, Byte, (ByteArray) -> Packet>>

    fun producePacket(byteArray: ByteArray): Packet?

    fun serializePacket(packet: Packet): ByteArray?

    companion object {
        fun getInstance(): PacketFactory =
            PacketFactoryImpl()
    }
}

private class PacketFactoryImpl : PacketFactory {


    override val packetsProduceData: List<Triple<KClass<out Packet>, Byte, (ByteArray) -> Packet>>
        get() = listOf(
            getProducePacketData(0.toByte()) { EchoPacket.fromBytes(it) },
            getProducePacketData(1.toByte()) { GetActiveUsersPacketRq.fromBytes() },
            getProducePacketData(2.toByte()) { GetActiveUsersPacketRs.fromBytes(it) },
            getProducePacketData(3.toByte()) { GetInfoPacketRq.fromBytes() },
            getProducePacketData(4.toByte()) { GetInfoPacketRs.fromBytes(it) },
            getProducePacketData(5.toByte()) { GetServerDataPacketRs.fromBytes(it) },
            getProducePacketData(6.toByte()) { GetServerDataPacketRq.fromBytes() },
            getProducePacketData(7.toByte()) { SendMessagePacket.fromBytes(it) },
            getProducePacketData(8.toByte()) { BroadcastMessagePacket.fromBytes(it, this) },
            getProducePacketData(9.toByte()) { JoinToGamePacket.fromBytes(it) },
            getProducePacketData(10.toByte()) { SelectItemPacket.fromBytes(it) }
        )

    override fun producePacket(byteArray: ByteArray): Packet? =
        packetsProduceData.firstOrNull { it.second == byteArray[0] }?.let {
            it.third(byteArray.copyOfRange(1, byteArray.size))
        }

    override fun serializePacket(packet: Packet): ByteArray? =
        packetsProduceData.firstOrNull { it.first == packet::class }?.second?.toByteArray()
            ?.mergeByteArray(packet.toBytes())
}


private fun Byte.toByteArray() = ByteArray(1) { this }

private inline fun <reified K : Packet> getProducePacketData(byte: Byte, noinline func: (ByteArray) -> K) =
    Triple(K::class, byte, func)