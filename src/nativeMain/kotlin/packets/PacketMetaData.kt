package packets

import platform.posix.sockaddr_in

data class PacketMetaData(
    val sender : sockaddr_in? = null,
    val buffer: ByteArray? = null
)
