import kotlinx.cinterop.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.posix.*
import platform.windows.CHARVar

data class UDPReceivedMessage(
    val bytes: ByteArray,
    val address: sockaddr_in
)

/**
 * UDP эхо сервер
 */
class UdpServer(
    val serverPort: Int
) {

    private var socket: SOCKET? = null

    fun launch() {
        createSocket()
        bind()
    }

    fun send(buffer: ByteArray, recipient: sockaddr_in, flags: Int = 0): Int =
        memScoped { // Создаёт скоуп для автоматического освобождения переменных созданных методом [alloc]
            val array = allocArray<ByteVar>(buffer.size)
            for (index in buffer.indices)
                array[index] = buffer[index]

            val sent = socket?.let {
                sendto(
                    it,
                    array,
                    buffer.size,
                    flags,
                    recipient.ptr.reinterpret(),
                    sizeOf<sockaddr_in>().convert()
                ).also { count ->
                    if (count == -1)
                        println(" Ошибка: ${WSAGetLastError()}")
                }
            } ?: throw Exception("Сокет не создан")
            println("Клиенту UDP были отправлены $sent байт из ${buffer.contentToString()}")
            sent
        }

    fun receiveFrom(bufferSize: Int = 1024, flags: Int = 0): Flow<UDPReceivedMessage> = socket?.let { sock ->
        flow {
            while (true) {
                memScoped {
                    val array = allocArray<ByteVar>(bufferSize)
                    val socketAddress = nativeHeap.alloc<sockaddr_in>()
                    val length = recvfrom(
                        sock, array,
                        bufferSize, flags,
                        socketAddress.ptr.reinterpret(),
                        allocArray<IntVar>(1).apply {
                            this[0] = sizeOf<sockaddr_in>().convert()
                        }.reinterpret()
                    )
                    if (length == 0)
                        throw Exception("От клиента получено 0 байт( ")
                    val byteArray = array.readBytes(length)
                    println("От клиента UDP были получены байты($length) ${byteArray.contentToString()}")
                    UDPReceivedMessage(byteArray, socketAddress)
                }.also {
                    emit(it)
                }
            }
        }
    } ?: throw Exception("Сокет не инициализирован")

    fun close() {
        socket?.close()
    }

    private fun createSocket() {
        socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP).takeIf {
            if (it == INVALID_SOCKET)
                throw Exception("Ошибка создания сокета")
            println("Сокет UDP успешно создан")
            true
        }?.also {
            setsockopt(it, SOL_SOCKET, SO_BROADCAST, "1", sizeOf<CHARVar>().convert()).let { opt ->
                if (opt < 0)
                    throw Exception("ОШибка настройки бродкаста")
            }

            socket = it
        }

    }

    private fun bind(): Unit =
        memScoped { // Создаёт скоуп для автоматического освобождения переменных созданных методом [alloc]
            alloc<sockaddr_in>().apply { //создаёт переменную типа [sockaddr_in]
                sin_family = AF_INET.convert()
                sin_port = posix_htons(serverPort.toShort()).convert()
                sin_addr.S_un.S_addr = ADDR_ANY
            }.let {
                socket?.let { socket ->
                    bind(socket, it.ptr.reinterpret(), sockaddr_in.size.convert())
                }
            }.takeIf {
                if (it != 0)
                    throw Exception("Ошибка при запуске UDP сервера ${WSAGetLastError()}")
                else println("UDP Сервер запущен на порту: $serverPort")
                true
            }
        }
}