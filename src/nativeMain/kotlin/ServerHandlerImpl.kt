import packets.*

class ServerHandlerImpl(
    private val serverSocket: ServerSocket
) : ServerHandler {

    private val activeClients = mutableMapOf<Int, ClientSocket>()

    override fun connectClient(clientSocket: ClientSocket) {
        activeClients[clientSocket.clientId] = clientSocket
        println("Клиент ${clientSocket.clientId} успешно подключился")
    }

    override fun disconnectClient(clientId: Int) {
        activeClients.remove(clientId)
        println("Клиент $clientId отключился")
    }


    override fun handle(clientSocket: ClientSocket, packet: Packet): Unit = when (packet) {
        is EchoPacket -> clientSocket.send(packet)
        is SendMessagePacket -> {
            packet.handleSendMessagePacket(clientSocket)
        }
        is GetInfoPacketRq -> clientSocket.send(GetInfoPacketRs(clientSocket.clientId))
        is GetActiveUsersPacketRq -> clientSocket.send(GetActiveUsersPacketRs(activeClients.keys.toList()))
        else -> {}
    }

    private fun SendMessagePacket.handleSendMessagePacket(sender: ClientSocket) {
        activeClients[this.clientId]?.send(
            SendMessagePacket(
                sender.clientId,
                this.message
            )
        ) ?: run {
            println("При отправке SendMessagePacket не найден получатель ${this.clientId}")
        }
    }

}