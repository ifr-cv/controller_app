package bid.yuanlu.ifr_controller

import android.util.Log
import java.util.Timer
import java.util.TimerTask

class WebManager() {


    private val timer = Timer()
    val dataPack = DataPack()
    private var connector: Connector = Connector.SocketConnector()

    var isConnected = false
        private set

    fun doStatusCallback() {
        connector.doStatusCallback()
    }

    fun setStatusCallback(cb: ((Boolean, Throwable?) -> Unit)?) {
        connector.statusCallback = cb
    }

    fun setUrl(url: String?) {
        connector.connect(url)
    }

    fun shutdown() {
        timer.cancel()
        connector.close(1000, null)
    }

    init {
        Log.d("ifr_cs_fieldInfo", dataPack.fieldInfoString)
        timer.schedule(object : TimerTask() {
            override fun run() {
                connector.send(dataPack)
            }
        }, 0, 1)
    }
}