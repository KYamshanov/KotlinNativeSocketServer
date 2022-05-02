package packets

class GetActiveUsersPacketRs(
    private val users: List<Int>
) : Packet {

    override fun toBytes(): ByteArray =
        users.joinToString(":").encodeToByteArray()

    companion object {

        fun fromBytes(byteArray: ByteArray): GetActiveUsersPacketRs =
            GetActiveUsersPacketRs(byteArray.decodeToString().split(":").map { it.toInt() })
    }
}