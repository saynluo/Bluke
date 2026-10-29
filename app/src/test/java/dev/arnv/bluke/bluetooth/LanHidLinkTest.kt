package dev.arnv.bluke.bluetooth

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.DataInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Plays the PC receiver's side of the protocol against the real phone-side client. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LanHidLinkTest {
    private val link = LanHidLink(ApplicationProvider.getApplicationContext<Context>())
    private val server = ServerSocket(LanHidLink.PORT, 1, InetAddress.getLoopbackAddress())
    private val host = LanHidLink.Host("test-pc", "127.0.0.1")

    @After
    fun tearDown() {
        link.close()
        server.close()
    }

    private fun waitFor(timeoutMillis: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "timed out" }
            Thread.sleep(20)
        }
    }

    @Test
    fun correctPinConnectsAndStreamsFramedReports() {
        val frames = AtomicReference<ByteArray>()
        val receiver = Thread {
            server.accept().use { conn ->
                val input = DataInputStream(conn.getInputStream())
                // Read byte-wise so no buffered reader swallows the frames that follow.
                val hello = generateSequence { input.read().takeIf { it >= 0 && it != '\n'.code } }
                    .map { it.toChar() }.joinToString("")
                assertEquals("BLUKE 123456", hello)
                conn.getOutputStream().write("OK\n".toByteArray())
                val buf = ByteArray(10 + 6)
                input.readFully(buf)
                frames.set(buf)
            }
        }.apply { start() }

        link.connect(host, "123456")
        waitFor { link.state.value is LanHidLink.State.Connected }
        link.send(1, byteArrayOf(0x02, 0, 0x04, 0, 0, 0, 0, 0))
        link.send(2, byteArrayOf(1, 5, -5, 0))
        receiver.join(TimeUnit.SECONDS.toMillis(5))

        assertArrayEquals(
            byteArrayOf(9, 1, 0x02, 0, 0x04, 0, 0, 0, 0, 0, 5, 2, 1, 5, -5, 0),
            frames.get()
        )
        assertEquals("123456", link.savedPin(host))
    }

    @Test
    fun wrongPinReportsFailureAndStaysIdle() {
        Thread {
            server.accept().use { conn ->
                conn.getInputStream().bufferedReader().readLine()
                conn.getOutputStream().write("NO\n".toByteArray())
            }
        }.start()

        link.connect(host, "000000")
        waitFor { link.lastFailure.value != null }
        assertEquals(LanHidLink.Failure.WRONG_PIN, link.lastFailure.value)
        assertTrue(link.state.value is LanHidLink.State.Idle)
    }
}
