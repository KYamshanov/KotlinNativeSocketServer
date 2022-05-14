import game.Game
import game.produceCraftPlayer
import packets.*

class ServerHandlerImpl(
    private val serverSocket: ServerSocket,
    private val game: Game
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
        is GetInfoPacketRq -> clientSocket.send(GetInfoPacketRs(clientId = clientSocket.clientId))
        is GetActiveUsersPacketRq -> clientSocket.send(GetActiveUsersPacketRs(users = activeClients.keys.toList()))
        is GetServerDataPacketRq -> clientSocket.send(GetServerDataPacketRs(serverSocket.serverData,game.getStatus()))
        is BroadcastMessagePacket -> activeClients.values.filter { it.clientId != clientSocket.clientId }.forEach {
            it.send(packet.message)
        }
        is JoinToGamePacket -> game.joinPlayer(produceCraftPlayer(clientSocket,packet.playerName))
        is SelectItemPacket -> game.selectItem(clientSocket.clientId, packet.gameItem)
        else -> {}
    }

    private fun SendMessagePacket.handleSendMessagePacket(sender: ClientSocket) {
        activeClients[this.clientId]?.send(
            SendMessagePacket(
                clientId = sender.clientId,
                message = this.message
            )
        ) ?: run {
            println("При отправке SendMessagePacket не найден получатель ${this.clientId}")
        }
    }

}