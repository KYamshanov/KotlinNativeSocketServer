import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import packets.ByteHandlerFactory
import packets.Packet
import packets.PacketFactory
import platform.posix.*

fun launchServer(serverPort: Int): ServerSocket = ServerSocket().apply {
    create()
    bind(serverPort)
    listen()
}

@ExperimentalCoroutinesApi
suspend fun ServerSocket.waitClients(packetHandler: ServerHandler) = coroutineScope {
    val packetFlow = MutableSharedFlow<Pair<ClientSocket, Packet>>(replay = 1)
    val connectedUsersFlow = MutableSharedFlow<ClientSocket>(replay = 1)
    val disconnectedUsersFlow = MutableSharedFlow<ClientSocket>(replay = 1)

    launch(newSingleThreadContext("accept thread")) {
        while (true) {
            this@waitClients.accept()?.let { client ->
                connectedUsersFlow.emit(client)
                launch(newSingleThreadContext("Receiving thread")) {
                    client.startReceiving()
                        .catch {
                            it.printStackTrace()
                            currentCoroutineContext().cancel()
                            disconnectedUsersFlow.emit(client)
                        }.collect { packet ->
                            packetFlow.emit(client to packet)
                        }
                }
            }
        }
    }
    launch {
        packetFlow.collect {
            packetHandler.handle(it.first, it.second)
        }
    }
    launch {
        disconnectedUsersFlow.collect {
            packetHandler.disconnectClient(it.clientId)
        }
    }
    launch {
        connectedUsersFlow.collect {
            packetHandler.connectClient(it)
        }
    }
}

/**
 * @property socket Сокет клиента
 * @property packetFactory Фабрика пакетов
 * @property byteHandlerFactory Фабрика обработчика байтов
 */
data class ClientSocket(
    val socket: SOCKET,
    private val packetFactory: PacketFactory,
    private val byteHandlerFactory: ByteHandlerFactory
) {

    /**
     * Id клиента
     */
    val clientId = rand()
    private val available: Boolean = true


    init {
        println("Создан клиент $clientId")
    }

    fun startReceiving(): Flow<Packet> = flow {
        val byteHandler = byteHandlerFactory.getNewInstance()
        while (true) {
            if (!available) return@flow
            val messages = byteHandler.handleReceived(receive(1024))
            for (message in messages)
                packetFactory.producePacket(message)?.let {
                    emit(it)
                }
        }
    }.onEach {
        println("От Клиента $clientId был получен пакет: ${it::class.simpleName} $it")
    }

    /**
     * Отправить пакет
     */
    fun send(packet: Packet) {
        println("Клиенту $clientId был отправлен пакет $packet")
        val byteHandler = byteHandlerFactory.getNewInstance()
        packetFactory.serializePacket(packet)?.let {
            send(byteHandler.prepareBytesToSend(it))
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

            val sent = send(this@ClientSocket.socket, array, buffer.size, flags)
            println("Клиенту $clientId были отправлены $sent байт из ${buffer.contentToString()}")
            sent
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
            val byteArray = array.readBytes(length)
            println("От клиента $clientId были получены байты($length) ${byteArray.contentToString()}")
            byteArray
        }

    private fun ByteArray.isFullMessage(lastByte: Byte = 10): Boolean =
        this.contains(lastByte)
}

/**
 * Создать сокет
 * @property port Порт на котором будет работать
 */
class ServerSocket() {

    var serverSocket: SOCKET? = null
        private set

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
                true
            }
        }?.let {
            ClientSocket(it, PacketFactory.getInstance(), ByteHandlerFactory.getInstance())
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