package packets

interface Packet {

    fun toBytes(): ByteArray
}