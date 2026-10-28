package dev.arnv.bluke.bluetooth

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.random.Random

/**
 * Streams the same HID reports used over Bluetooth to one PC receiver on the local network
 * (tools/bluke_lan_receiver.py). The receiver connects in and must send "BLUKE <pin>\n" first.
 * Frame: [length = 1 + data.size][reportId][data...].
 */
class LanHidServer {
    sealed class State {
        object Off : State()
        data class Listening(val addresses: List<String>, val port: Int, val pin: String) : State()
        data class Connected(val addresses: List<String>, val port: Int, val pin: String, val client: String) : State()
    }

    companion object {
        const val PORT = 7393
    }

    private val _state = MutableStateFlow<State>(State.Off)
    val state: StateFlow<State> = _state

    private val sendExecutor = Executors.newSingleThreadExecutor { Thread(it, "lan-report-sender") }
    @Volatile private var server: ServerSocket? = null
    @Volatile private var client: Socket? = null
    @Volatile private var output: OutputStream? = null

    @Synchronized
    fun start() {
        if (server != null) return
        val socket = try {
            ServerSocket(PORT)
        } catch (e: Exception) {
            Log.e("BlukeLan", "Failed to listen on $PORT", e)
            return
        }
        server = socket
        val pin = "%06d".format(Random.nextInt(1_000_000))
        val addresses = localAddresses()
        _state.value = State.Listening(addresses, PORT, pin)
        Thread({ acceptLoop(socket, addresses, pin) }, "lan-accept").start()
    }

    @Synchronized
    fun stop() {
        server?.runCatching { close() }
        server = null
        dropClient()
        _state.value = State.Off
    }

    fun send(reportId: Int, report: ByteArray) {
        if (output == null) return
        val frame = ByteArray(report.size + 2)
        frame[0] = (report.size + 1).toByte()
        frame[1] = reportId.toByte()
        report.copyInto(frame, 2)
        try {
            sendExecutor.submit {
                val out = output ?: return@submit
                try {
                    out.write(frame)
                    out.flush()
                } catch (e: Exception) {
                    Log.w("BlukeLan", "Receiver write failed", e)
                    dropClient()
                }
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
        }
    }

    fun close() {
        stop()
        sendExecutor.shutdownNow()
    }

    private fun acceptLoop(socket: ServerSocket, addresses: List<String>, pin: String) {
        while (!socket.isClosed) {
            val incoming = try { socket.accept() } catch (_: Exception) { break }
            try {
                incoming.tcpNoDelay = true
                incoming.soTimeout = 5_000
                val hello = incoming.getInputStream().bufferedReader().readLine()
                if (hello != "BLUKE $pin") {
                    incoming.close()
                    Thread.sleep(1_000) // Throttle PIN guessing; the accept loop is single-threaded.
                    continue
                }
                incoming.soTimeout = 0
                val out = incoming.getOutputStream()
                out.write("OK\n".toByteArray())
                out.flush()
                dropClient() // Newest authenticated receiver replaces the previous one.
                client = incoming
                output = out
                _state.value = State.Connected(addresses, PORT, pin, incoming.inetAddress.hostAddress ?: "?")
                // Block until the receiver disconnects so the UI reflects it promptly.
                Thread({
                    try { while (incoming.getInputStream().read() >= 0) Unit } catch (_: Exception) {}
                    if (client === incoming) {
                        dropClient()
                        if (server === socket) _state.value = State.Listening(addresses, PORT, pin)
                    }
                }, "lan-client-watch").start()
            } catch (e: Exception) {
                Log.w("BlukeLan", "Receiver handshake failed", e)
                incoming.runCatching { close() }
            }
        }
    }

    private fun dropClient() {
        val c = client
        client = null
        output = null
        c?.runCatching { close() }
    }

    private fun localAddresses(): List<String> = try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .mapNotNull { it.hostAddress }
    } catch (_: Exception) {
        emptyList()
    }
}
