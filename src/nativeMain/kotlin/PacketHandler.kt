import packets.Packet

interface PacketHandler {

    fun handle(clientSocket: ClientSocket,packet: Packet)

}