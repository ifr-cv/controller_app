package bid.yuanlu.ifr_controller

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL


abstract class Connector {
    var statusCallback: ((Boolean, Throwable?) -> Unit)? = null
    abstract fun close(code: Int, reason: String?): Boolean
    abstract fun connect(url: String?)
    abstract fun isConnected(): Boolean
    abstract fun send(dp: DataPack)
    var error: Throwable? = null
        protected set


    abstract fun doStatusCallback()


    class WebsocketConnector : Connector() {
        private val client = OkHttpClient()
        private var webSocket: WebSocket? = null
        private var isConnectedSocket = false

        override fun close(code: Int, reason: String?): Boolean {
            val r = webSocket?.close(code, reason) ?: false
            webSocket = null
            return r
        }

        override fun isConnected(): Boolean {
            return webSocket != null && isConnectedSocket
        }

        override fun send(dp: DataPack) {
            webSocket?.send(dp.dataString)
        }

        override fun doStatusCallback() {
            val sc = statusCallback
            if (sc != null) sc(isConnected(), error)
        }

        private fun updateStatus(connected: Boolean) {
            Log.d("ifr_cs", "old: $isConnectedSocket, new: $connected, ws: $webSocket")
            if (!connected) {
                close(1000, null)
            }
            error = null
            if (isConnectedSocket != connected) {
                isConnectedSocket = connected
                doStatusCallback()
            }
        }

        override fun connect(url: String?) {
            Log.d("ifr_cs", "url: $url")
            close(1000, null)
            if (!url.isNullOrEmpty() && url.isNotBlank()) {
                try {
                    webSocket = client.newWebSocket(okhttp3.Request.Builder().url(url).build(), object : WebSocketListener() {
                        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                            this@WebsocketConnector.webSocket = null
                            updateStatus(false)
                            Log.d("ifr_cs", "web_socket onClosing")
                        }

                        override fun onOpen(webSocket: WebSocket, response: Response) {
                            this@WebsocketConnector.webSocket = webSocket
                            updateStatus(true)
                            Log.d("ifr_cs", "web_socket onOpen")
                        }

                        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                            error = t
                            Log.d("ifr_cs", "web_socket onFailure: $t, $response")
                        }
                    })
                } catch (e: Throwable) {
                    error = e
                    updateStatus(false)
                }
            } else {
                error = IllegalArgumentException("Empty Url: $url")
                updateStatus(false)
            }
        }
    }


    class SocketConnector : Connector() {
        private var socket: Socket? = null
        private var os: OutputStream? = null
        private var bs: ByteArray? = null
        private var isConnectedSocket = false
        private var thread: Thread? = null

        override fun close(code: Int, reason: String?): Boolean {
            val r = socket != null && !(socket?.isClosed ?: true)
            socket?.close()
            socket = null
            return r
        }

        override fun isConnected(): Boolean {
            return socket?.isConnected ?: false
        }

        override fun doStatusCallback() {
            val sc = statusCallback
            if (sc != null) sc(isConnected(), error)
        }

        private fun updateStatus(connected: Boolean) {
            Log.d("ifr_cs", "old: $isConnectedSocket, new: $connected, socket: $socket")
            if (!connected) close(1000, null)
            error = null
            if (isConnectedSocket != connected) {
                isConnectedSocket = connected
                doStatusCallback()
            }
        }

        override fun connect(url: String?) {
            close(1000, null)
            if (!url.isNullOrEmpty() && url.isNotBlank()) {
                try {
                    val url1 = URL(if (url.contains("://")) url else "http://$url")
                    thread = Thread {
                        try {
                            socket = Socket(url1.host, if (url1.port == -1) url1.defaultPort else url1.port)
                            os = socket?.getOutputStream()
                        } catch (e: Throwable) {
                            error = e
                        }
                        updateStatus(socket?.isConnected ?: false)
                    }
                    thread?.start()
                } catch (e: Throwable) {
                    error = e
                    updateStatus(false)
                }
            } else {
                error = IllegalArgumentException("Empty Url: $url")
                updateStatus(false)
            }
        }

        override fun send(dp: DataPack) {
            try {
                bs = dp.getData(bs)
                os?.write(bs!!)
            } catch (e: Throwable) {
                error = e
                updateStatus(socket?.isConnected ?: false)
            }
        }
    }

    class UDPSocketConnector : Connector() {
        private var socket: DatagramSocket? = null
        private var packet: DatagramPacket? = null
        private var bs: ByteArray? = null
        private var isConnectedSocket = false
        private var thread: Thread? = null

        override fun close(code: Int, reason: String?): Boolean {
            val r = socket != null && !(socket?.isClosed ?: true)
            socket?.close()
            socket = null
            packet = null
            return r
        }

        override fun isConnected(): Boolean {
            return isNotClosed()
        }

        override fun doStatusCallback() {
            val sc = statusCallback
            if (sc != null) sc(isConnected(), error)
        }

        private fun updateStatus(connected: Boolean) {
            Log.d("ifr_cs", "old: $isConnectedSocket, new: $connected, socket: $socket")
            if (!connected) close(1000, null)
            error = null
            if (isConnectedSocket != connected) {
                isConnectedSocket = connected
                doStatusCallback()
            }
        }

        private fun isNotClosed(): Boolean {
            val s = socket
            return if (s != null) !s.isClosed else false
        }

        override fun connect(url: String?) {
            close(1000, null)
            if (!url.isNullOrEmpty() && url.isNotBlank()) {
                try {
                    val url1 = URL(if (url.contains("://")) url else "http://$url")
                    thread = Thread {
                        try {
                            val add = InetSocketAddress(url1.host, if (url1.port == -1) url1.defaultPort else url1.port)
                            socket = DatagramSocket()
                            packet = DatagramPacket(ByteArray(1), 1, add)
                        } catch (e: Throwable) {
                            error = e
                        }
                        updateStatus(isNotClosed())
                    }
                    thread?.start()
                } catch (e: Throwable) {
                    error = e
                    updateStatus(false)
                }
            } else {
                error = IllegalArgumentException("Empty Url: $url")
                updateStatus(false)
            }
        }

        override fun send(dp: DataPack) {
            try {
                bs = dp.getData(bs)
                packet?.setData(bs!!, 0, bs!!.size)
                socket?.send(packet!!)
            } catch (e: Throwable) {
                error = e
                updateStatus(isNotClosed())
            }
        }
    }
}