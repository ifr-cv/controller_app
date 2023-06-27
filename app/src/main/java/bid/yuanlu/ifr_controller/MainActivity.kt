package bid.yuanlu.ifr_controller

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import bid.yuanlu.ifr_controller.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    var webManager: WebManager? = null

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        val storage = getSharedPreferences("settings", Context.MODE_PRIVATE)
        storage.edit {
            putString("url", storage.getString("url", ""))
            putBoolean("vibrateBtn", storage.getBoolean("vibrateBtn", true))
            putBoolean("vibrateJoystick", storage.getBoolean("vibrateJoystick", true))
            putBoolean("vibrateSeek", storage.getBoolean("vibrateSeek", true))
            putBoolean("is_red_team", storage.getBoolean("is_red_team", true))
        }

        webManager?.shutdown()
        webManager = WebManager()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


//        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
//        vibrator.vibrate(longArrayOf(500, 1500, 1000, 500), 1)

//        binding.fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAnchorView(R.id.fab)
//                .setAction("Action", null).show()
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 膨胀菜单；这会将项目添加到操作栏（如果存在）。
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 处理操作栏项目单击此处。只要您在 AndroidManifest.xml 中指定父活动，操作栏就会自动处理 HomeUp 按钮上的点击。
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webManager?.shutdown()
    }
}