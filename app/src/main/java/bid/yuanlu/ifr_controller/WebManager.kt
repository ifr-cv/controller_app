package bid.yuanlu.ifr_controller

import android.annotation.SuppressLint
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.StringJoiner
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentSkipListMap

class WebManager() {

    private val client = OkHttpClient()
    private val timer = Timer()
    private val values = ConcurrentSkipListMap<Int, String>()
    private var webSocket: WebSocket? = null

    var statusCallback: ((Boolean, Throwable?) -> Unit)? = null;
    var isConnected = false
        private set
    var error: Throwable? = null
        private set
    private var sendData: String? = null

    @SuppressLint("DefaultLocale")
    fun setValue(id: Int, x: Int, y: Int) {
        values[id] = String.format("%d %d %d", id, x, y)
        calcSendData()
    }

    @SuppressLint("DefaultLocale")
    fun setValue(id: Int, x: Float, y: Float) {
        values[id] = String.format("%d %f %f", id, x, y)
        calcSendData()
    }

    private fun calcSendData() {
        val sj = StringJoiner(";")
        for (x in values.values) sj.add(x)
        sendData = sj.toString()
    }

    fun doStatusCallback() {
        val sc = statusCallback;
        if (sc != null) sc(webSocket != null && isConnected, error)
    }

    private fun updateStatus(connected: Boolean) {
        Log.d("ifr_cs", "old: $isConnected, new: $connected, ws: $webSocket")
        if (!connected) {
            webSocket?.close(1000, null)
            webSocket = null
        }
        error = null
        if (isConnected != connected) {
            isConnected = connected
            doStatusCallback()
        }
    }

    fun setUrl(url: String?) {
        Log.d("ifr_cs", "url: $url")
        webSocket?.close(1000, null)
        if (!url.isNullOrEmpty() && url.isNotBlank()) {
            try {
                webSocket = client.newWebSocket(okhttp3.Request.Builder().url(url).build(), object : WebSocketListener() {
                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        this@WebManager.webSocket = null
                        updateStatus(false)
                        Log.d("ifr_cs", "web_socket onClosing")
                    }

                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        this@WebManager.webSocket = webSocket
                        updateStatus(true)
                        Log.d("ifr_cs", "web_socket onOpen")
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        error = t
                        Log.d("ifr_cs", "web_socket onFailure: $t, $response")
                    }
                })
            } catch (e: IllegalArgumentException) {
                updateStatus(false)
            }
        } else {
            updateStatus(false)
        }
        Log.d("ifr_cs", "ws: $webSocket, connected: $isConnected")
    }

    fun shutdown() {
        timer.cancel()
        webSocket?.close(1000, null)
    }

    init {
        Log.d("ifr_cs", "init", Throwable("init"))
        timer.schedule(object : TimerTask() {
            override fun run() {
                val sd = sendData
                val ws = webSocket
                Log.d("ifr_cs", "sendData = $sendData, webSocket = $webSocket")
                if (ws != null && sd != null) ws.send(sd)
            }
        }, 0, 1000)
    }
}