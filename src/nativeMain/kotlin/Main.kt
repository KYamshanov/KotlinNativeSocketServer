import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import packets.*
import platform.posix.WSAGetLastError
import platform.posix.WSAStartup
import platform.posix.system

@ExperimentalCoroutinesApi
fun main(): Unit = runBlocking {

    println("Введите данные о сервере в виде: Название_сервера:Максимальное_количество_игроков")
    val line = /*readLine()?*/"Сервер-1:15".split(":", limit = 2)

    val serverData = ServerData(line[0], line[1].toInt())

    memScoped {
        alloc<platform.posix.WSAData>() {
            WSAStartup(514, this.ptr).also {
                if (it != 0)
                    throw Exception("Ошибка при настройке WSA ${WSAGetLastError()}")
            }
        }
        launch(newSingleThreadContext("TCP server")) {
            var server: ServerSocket? = null
            try {
                launchServer(8081, serverData).also {
                    server = it
                    it.waitClients(ServerHandlerImpl(it))
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                server?.close()
            }
        }
        launch(newSingleThreadContext("UDP server")) {
            var server: UdpServer? = null
            try {
                UdpServer(8080).also {
                    server = it
                    it.launch()
                    val byteHandler = ByteHandlerFactory.getInstance().getNewInstance()
                    val packetFactory = PacketFactory.getInstance()
                    it.receiveFrom().collect { message ->
                        byteHandler.handleReceived(message.bytes).mapNotNull { bytes ->
                            packetFactory.producePacket(bytes)
                        }.forEach { packet ->
                            when (packet) {
                                is GetServerDataPacketRq -> packetFactory.serializePacket(
                                    GetServerDataPacketRs(serverData)
                                )?.let { bytes -> byteHandler.prepareBytesToSend(bytes) }?.also { bytes ->
                                    server?.send(bytes, message.address)
                                }
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                server?.close()
            }
        }
        system("PAUSE")
    }
}