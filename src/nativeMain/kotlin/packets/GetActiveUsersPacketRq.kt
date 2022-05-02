package packets


class GetActiveUsersPacketRq(
) : Packet {

    override fun toBytes(): ByteArray = ByteArray(0)

    companion object {

        fun fromBytes(): GetActiveUsersPacketRq =
            GetActiveUsersPacketRq()

    }


}