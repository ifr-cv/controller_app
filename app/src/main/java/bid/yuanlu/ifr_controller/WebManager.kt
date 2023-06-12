package bid.yuanlu.ifr_controller

import android.annotation.SuppressLint
import android.os.Handler
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.StringJoiner
import java.util.concurrent.ConcurrentSkipListMap

class WebManager {
    private val client = OkHttpClient()
    private val handler = Handler()
    private val values = ConcurrentSkipListMap<Int, String>()
    private var webSocket: WebSocket? = null
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

    fun setUrl(url: String?) {
        if (webSocket != null) webSocket!!.close(1000, null)
        if (!url.isNullOrEmpty() && url.isNotBlank()) {
            webSocket = client.newWebSocket(okhttp3.Request.Builder().url(url).build(), object : WebSocketListener() {
                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    this@WebManager.webSocket = null
                    isConnected = false
                    error = null
                }

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    isConnected = true
                    error = null
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    error = t
                }
            })
        }
        handler.postDelayed({
            val sd = sendData
            if (webSocket != null && sd != null) webSocket!!.send(sd)
        }, 10)
    }
}