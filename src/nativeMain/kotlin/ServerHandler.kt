import packets.Packet

interface ServerHandler {

    fun connectClient(clientSocket: ClientSocket)
    fun disconnectClient(clientId: Int)

    fun handle(clientSocket: ClientSocket, packet: Packet)

}