import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import platform.posix.WSAGetLastError
import platform.posix.WSAStartup
import platform.posix.system

@ExperimentalCoroutinesApi
fun main(): Unit = runBlocking {
    memScoped {
        alloc<platform.posix.WSAData>() {
            WSAStartup(514, this.ptr).also {
                if (it != 0)
                    throw Exception("Ошибка при настройке WSA ${WSAGetLastError()}")
            }
        }
        var server: ServerSocket? = null
        try {
            launchServer(8081).apply {
                waitClients()
            }.also {
                server = it
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            server?.close()
        }
        system("PAUSE")
    }
}