package packets

import ClientSocket

class GetInfoPacketRq(
) : Packet {

    override fun toBytes(): ByteArray = ByteArray(0)

    companion object {

        fun fromBytes(): GetInfoPacketRq =
            GetInfoPacketRq()

    }


}