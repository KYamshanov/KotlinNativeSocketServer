package packets

fun ByteArray.mergeByteArray(appendedByteArray: ByteArray): ByteArray {
    if (this.isEmpty())
        return appendedByteArray
    return mutableListOf<Byte>().apply {
        addAll(this@mergeByteArray.toTypedArray())
        addAll(appendedByteArray.toTypedArray())
    }.toByteArray()
}