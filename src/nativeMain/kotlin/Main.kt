import game.Game
import game.produceGame
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
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.FreezableAtomicReference
import kotlin.native.concurrent.freeze

@ExperimentalCoroutinesApi
fun main(): Unit = runBlocking {

    println("Введите данные о сервере в виде: Название_сервера:Максимальное_количество_игроков")
    val line = /*readLine()*/"TEST:2"?.split(":", limit = 2)!!

    val serverData = ServerData(line[0], line[1].toInt())

    val refGame = AtomicReference<Game?>(null)

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
                    val game = produceGame(it, serverData.maxPlayers)
                    //refGame.value = game.freeze()
                    it.waitClients(ServerHandlerImpl(it, game))
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
                                    GetServerDataPacketRs(serverData, refGame.value?.getStatus() ?: "Игра не запущена")
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