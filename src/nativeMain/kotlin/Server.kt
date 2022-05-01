import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import packets.Packet
import packets.PacketFactory
import platform.posix.*

fun launchServer(serverPort: Int): ServerSocket = ServerSocket().apply {
    create()
    bind(serverPort)
    listen()
}

@ExperimentalCoroutinesApi
suspend fun ServerSocket.waitClients(packetHandler: PacketHandler) = coroutineScope {
    launch {
        while (true)
            this@waitClients.accept().let { client ->
                println("T1")
                launch(newSingleThreadContext("Client receive")) {
                    client?.startReceiving()?.collect {
                        packetHandler.handle(client, it)
                    }
                }
            }
    }
}

/**
 * @property socket Сокет клиента
 * @property available Доступность клиента
 */
data class ClientSocket(
    val socket: SOCKET,
    var available: Boolean = true,
    private val packetFactory: PacketFactory
) {

    suspend fun startReceiving(): Flow<Packet> = withContext(Dispatchers.Default) {
        flow {
            var chached = ByteArray(0)
            while (true) {
                chached = chached.putByteArray(receive(1024))
                if (chached.isFullMessage())
                    emit(packetFactory.getPacketFromBytes(chached))
            }
        }
    }



    /**
     *
     * Функция помещает в очередь сокета len количество байт из буфера Buf, затем
     * возвращает управление, практически не задерживая выполнение вызывающего потока. Байты
     * из очереди уйдут при первой возможности
     *  @param buffer Посылаемый буфер
     *  @param flags Флаг параметров передачи. Пишите туда 0.
     *  @return Количество успешно отправленных байт
     */
    private fun send(buffer: ByteArray, flags: Int = 0): Int =
        memScoped { // Создаёт скоуп для автоматического освобождения переменных созданных методом [alloc]
            val array = allocArray<ByteVar>(buffer.size)
            for (index in buffer.indices)
                array[index] = buffer[index]

            send(this@ClientSocket.socket, array, buffer.size, flags)
        }

    /**
     * Функция для приема сообщений
     * @param bufferSize Размер буфера для получения
     * @param flags Флаг параметров передачи. Пишите туда 0.
     * @return Полученный массив байтов
     */
    private fun receive(bufferSize: Int, flags: Int = 0): ByteArray =
        memScoped {
            val array = allocArray<ByteVar>(bufferSize)
            val length = recv(socket, array, bufferSize, flags)
            if (length == 0)
                throw Exception("Соединение с клиентом пропало")
            array.readBytes(length)
        }

    private fun ByteArray.isFullMessage(lastByte: Byte = 10): Boolean =
        this.contains(lastByte)

    private fun ByteArray.putByteArray(appendedByteArray: ByteArray): ByteArray {
        if (this.isEmpty())
            return appendedByteArray
        return mutableListOf<Byte>().apply {
            addAll(this@putByteArray.toTypedArray())
            addAll(appendedByteArray.toTypedArray())
        }.toByteArray()
    }
}

/**
 * Создать сокет
 * @property port Порт на котором будет работать
 */
class ServerSocket() {

    var serverSocket: SOCKET? = null
        private set

    private val _activeClients = mutableListOf<ClientSocket>()
    val activeClients: List<ClientSocket>
        get() = _activeClients.toList()


    /**
     * Создание сокета
     * @param af Определение семейства адресов укажем константу
     * @param type Тип протокол, который мы хотим использовать SOCK_STREAM - TCP
     * @param protocol Протокол
     */
    fun create(af: Int = AF_INET, type: Int = SOCK_STREAM, protocol: Int = platform.windows.IPPROTO_IP): SOCKET? =
        socket(af, type, protocol).takeIf {
            if (it != INVALID_SOCKET) {
                println("Сокет успешно создан.")
                true
            } else {
                throw Exception("Ошибка при создании сокета ${WSAGetLastError()}")
            }
        }?.also {
            serverSocket = it
        }

    /**
     * Функция bind привязывает IP-адрес и порт к прослушивающему сокету сервера,
     * созданному вызовом socket
     * @param serverPort Порт сервера
     */
    fun bind(serverPort: Int) {
        memScoped { // Создаёт скоуп для автоматического освобождения переменных созданных методом [alloc]
            alloc<sockaddr_in>().apply { //создаёт переменную типа [sockaddr_in]
                sin_family = AF_INET.convert()
                sin_port = posix_htons(serverPort.toShort()).convert()
                sin_addr.S_un.S_addr =
                    INADDR_ANY // Клиенты подключаются к серверу и их входящий адрес серверу заранее неизвестен.  Поэтому пишем туда константу INADDR_ANY
            }.let {
                serverSocket?.let { socket ->
                    bind(socket, it.ptr.reinterpret(), sockaddr_in.size.convert())
                }
            }.takeIf {
                if (it != 0)
                    throw Exception("Ошибка при запуске сервера ${WSAGetLastError()}")
                else println("Сервер запущен на порту: $serverPort")
                true
            }
        }
    }

    /**
     * После привязки ([bind]) к прослушивающему сокету адресной информации переведем его врежим прослушивания
     * @param maxConnections Максимальное количество соединений
     */
    fun listen(maxConnections: Int = SOMAXCONN) {
        serverSocket?.let {
            listen(it, maxConnections)
        }?.also {
            if (it != 0)
                throw Exception("Ошибка при прослушивании сокета  ${WSAGetLastError()}")
            println("Прослушивание порта запущено")
        } ?: throw NullPointerException("Server Socket is null")
    }


    /**
     * Ожидает подключения клиента
     *  @return в случае успешного подключения клиента вернет сокет
     */
    suspend fun accept(): ClientSocket? = coroutineScope {
        serverSocket?.let {
            accept(it, null, null)
        }?.takeIf {
            if (it == INVALID_SOCKET) {
                println("Ошибка при подключении клиента  ${WSAGetLastError()}")
                false
            } else {
                println("клиент успешно подключился")
                true
            }
        }?.let {
            println("T5")
            ClientSocket(it)
        }?.also {
            _activeClients.add(it)
        }
    }
}

/**
 * Завершить сервер сокет
 */
fun ServerSocket.close() {
    this.serverSocket?.close()
}

/**
 * Закрыть сокет
 */
fun SOCKET.close() {
    closesocket(this)
}