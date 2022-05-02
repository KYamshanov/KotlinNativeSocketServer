package packets

/**
 * Абстрактрная фабрика обработчика байтов
 */
interface ByteHandlerFactory {

    /**
     * Получить новый экземпляр обработчка
     */
    fun getNewInstance(): ByteHandler

    companion object {

        /**
         * Получить экземпляр фабрики обработчиков
         */
        fun getInstance(): ByteHandlerFactory = ByteHandlerFactoryImpl()
    }

}

/**
 * Обработчик байтов
 */
interface ByteHandler {

    /**
     * Обработать полученные байты
     * @param bytes полученные байты
     * @return Список "Сообщений", если такие были получены
     */
    fun handleReceived(bytes: ByteArray): List<ByteArray>

    /**
     * Подготовить массив байт к отправке
     * @param bytes байты для отправки
     * @return подготовленные байты для отправки
     */
    fun prepareBytesToSend(bytes: ByteArray): ByteArray
}

private class ByteHandlerFactoryImpl : ByteHandlerFactory {
    override fun getNewInstance(): ByteHandler =
        ByteHandlerImpl()

}

private class ByteHandlerImpl : ByteHandler {

    private var cachedBytes = ByteArray(0)

    override fun handleReceived(bytes: ByteArray): List<ByteArray> =
        cachedBytes.mergeByteArray(bytes).let {
            cachedBytes = it
            getMessages()
        }.also {
            if (it.isNotEmpty())
                cachedBytes = ByteArray(0)
        }

    override fun prepareBytesToSend(bytes: ByteArray): ByteArray =
        bytes.mergeByteArray(END_SYMBOLS)


    private fun getMessages(): List<ByteArray> {
        var startMessageIndex = 0
        val messages = mutableListOf<ByteArray>()

        for (index in cachedBytes.indices) {
            if (cachedBytes.isEndMessage(index) && startMessageIndex < index) {
                messages.add(cachedBytes.copyOfRange(startMessageIndex, index))
                startMessageIndex = index + END_SYMBOLS.size
            }
        }
        return messages
    }

    private fun ByteArray.isEndMessage(startIndex: Int): Boolean {
        if (this.size - startIndex < END_SYMBOLS.size) return false
        for (index in startIndex until startIndex + END_SYMBOLS.size) {
            if (END_SYMBOLS[index - startIndex] != this[index])
                return false
        }
        return true
    }

    companion object {

        private val END_SYMBOLS = ByteArray(2).apply {
            this[0] = '\r'.code.toByte()
            this[1] = '\n'.code.toByte()
        }
    }
}