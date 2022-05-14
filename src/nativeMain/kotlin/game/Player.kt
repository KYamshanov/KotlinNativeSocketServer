package game

import ClientSocket
import close
import packets.SendMessagePacket

interface Player {
    var selectedItem: GameItem?
    val clientId: Int
    val playerName: String

    fun disconnect()
    fun sendMessage(message: String)
    fun fightWith(second: Player): Player?
}

fun produceCraftPlayer(clientSocket: ClientSocket, playerName: String): Player {
    return CraftPlayer(clientSocket, playerName)
}

private class CraftPlayer(
    private val clientSocket: ClientSocket,
    override val playerName: String
) : Player {

    override var selectedItem: GameItem? = null
    override val clientId: Int
        get() = clientSocket.clientId

    override fun disconnect() {
        clientSocket.socket.close()
    }

    override fun sendMessage(message: String) {
        clientSocket.send(SendMessagePacket(clientId = -1, message = message))
    }

    override fun fightWith(second: Player): Player? =
        this.selectedItem?.fight(second.selectedItem!!)?.let {
            if (it == selectedItem) this
            else second
        }
}