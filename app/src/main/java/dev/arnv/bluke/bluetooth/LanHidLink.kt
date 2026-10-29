package dev.arnv.bluke.bluetooth

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.Executors

/**
 * Streams the same HID reports used over Bluetooth to a PC running tools/bluke_lan_receiver.py.
 * Discovery: UDP broadcast "BLUKE?" to [DISCOVERY_PORT], receivers answer "BLUKE! <name>".
 * Session: TCP to [PORT], send "BLUKE <pin>\n", expect "OK\n", then frames of
 * [length = 1 + data.size][reportId][data...].
 */
class LanHidLink(context: Context) {
    data class Host(val name: String, val address: String)

    enum class Failure { WRONG_PIN, UNREACHABLE }

    sealed class State {
        object Idle : State()
        data class Connecting(val host: Host) : State()
        data class Connected(val host: Host) : State()
    }

    companion object {
        const val PORT = 7393
        const val DISCOVERY_PORT = 7394
        private const val SCAN_MILLIS = 1_500
    }

    private val prefs = context.getSharedPreferences("lan_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    private val _hosts = MutableStateFlow<List<Host>>(emptyList())
    val hosts: StateFlow<List<Host>> = _hosts

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _lastFailure = MutableStateFlow<Failure?>(null)
    val lastFailure: StateFlow<Failure?> = _lastFailure

    private val worker = Executors.newSingleThreadExecutor { Thread(it, "lan-link") }
    private val sendExecutor = Executors.newSingleThreadExecutor { Thread(it, "lan-report-sender") }
    @Volatile private var socket: Socket? = null
    @Volatile private var output: OutputStream? = null

    /** PIN last accepted by this receiver, so reconnecting needs no typing. */
    fun savedPin(host: Host): String? = prefs.getString("pin_${host.address}", null)

    fun scan() {
        if (_isScanning.value) return
        _isScanning.value = true
        Thread({
            try {
                _hosts.value = discover()
            } finally {
                _isScanning.value = false
            }
        }, "lan-scan").start()
    }

    fun connect(host: Host, pin: String) {
        _lastFailure.value = null
        _state.value = State.Connecting(host)
        submit { openSession(host, pin) }
    }

    fun disconnect() {
        submit {
            dropSession()
            _state.value = State.Idle
        }
    }

    fun clearFailure() {
        _lastFailure.value = null
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
                    disconnect()
                }
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {
        }
    }

    fun close() {
        dropSession()
        _state.value = State.Idle
        worker.shutdownNow()
        sendExecutor.shutdownNow()
    }

    private fun submit(task: () -> Unit) {
        try {
            worker.submit(task)
        } catch (_: java.util.concurrent.RejectedExecutionException) {
        }
    }

    private fun openSession(host: Host, pin: String) {
        dropSession()
        val s = Socket()
        try {
            s.connect(InetSocketAddress(host.address, PORT), 3_000)
            s.tcpNoDelay = true
            s.soTimeout = 5_000
            val out = s.getOutputStream()
            out.write("BLUKE $pin\n".toByteArray())
            out.flush()
            val reply = s.getInputStream().bufferedReader().readLine()
            if (reply != "OK") {
                s.close()
                _lastFailure.value = Failure.WRONG_PIN
                _state.value = State.Idle
                return
            }
            s.soTimeout = 0
            prefs.edit { putString("pin_${host.address}", pin) }
            socket = s
            output = out
            _state.value = State.Connected(host)
            // The receiver never sends after the handshake; EOF means it went away.
            Thread({
                try { while (s.getInputStream().read() >= 0) Unit } catch (_: Exception) {}
                if (socket === s) disconnect()
            }, "lan-session-watch").start()
        } catch (e: Exception) {
            Log.w("BlukeLan", "Connect to ${host.address} failed", e)
            s.runCatching { close() }
            _lastFailure.value = Failure.UNREACHABLE
            _state.value = State.Idle
        }
    }

    private fun dropSession() {
        val s = socket
        socket = null
        output = null
        s?.runCatching { close() }
    }

    private fun discover(): List<Host> {
        val found = linkedMapOf<String, Host>()
        try {
            DatagramSocket().use { udp ->
                udp.broadcast = true
                udp.soTimeout = 300
                val probe = "BLUKE?".toByteArray()
                for (target in broadcastAddresses()) {
                    runCatching { udp.send(DatagramPacket(probe, probe.size, target, DISCOVERY_PORT)) }
                }
                val buf = ByteArray(256)
                val deadline = System.currentTimeMillis() + SCAN_MILLIS
                while (System.currentTimeMillis() < deadline) {
                    val packet = DatagramPacket(buf, buf.size)
                    try {
                        udp.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    val reply = String(packet.data, 0, packet.length)
                    if (!reply.startsWith("BLUKE! ")) continue
                    val address = packet.address.hostAddress ?: continue
                    found[address] = Host(reply.removePrefix("BLUKE! ").trim().ifEmpty { address }, address)
                }
            }
        } catch (e: Exception) {
            Log.w("BlukeLan", "Discovery failed", e)
        }
        return found.values.toList()
    }

    private fun broadcastAddresses(): List<InetAddress> {
        val subnetBroadcasts = try {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.interfaceAddresses }
                .mapNotNull { it.broadcast }
        } catch (_: Exception) {
            emptyList()
        }
        return (subnetBroadcasts + InetAddress.getByName("255.255.255.255")).distinct()
    }
}
