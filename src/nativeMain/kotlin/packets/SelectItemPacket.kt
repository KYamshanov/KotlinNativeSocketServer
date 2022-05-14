package packets

import game.GameItem


class SelectItemPacket(
    val gameItem: GameItem, packetData: PacketMetaData? = null
) : AbstractPacket(packetData) {

    override fun toBytes(): ByteArray = ByteArray(1) { gameItem.ordinal.toByte() }

    companion object {
        fun fromBytes(bytes: ByteArray): SelectItemPacket = SelectItemPacket(GameItem.values()[bytes[0].toInt()])
    }
}