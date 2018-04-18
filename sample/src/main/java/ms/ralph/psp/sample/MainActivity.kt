package ms.ralph.psp.sample

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import kotlinx.android.synthetic.main.activity_main.*
import ms.ralph.psp.PluggableSharedPreferences

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sp = applicationContext.getSharedPreferences("test", Context.MODE_PRIVATE)

        val p = PluggableSharedPreferences.Builder(sp)
                .encoder { Base64.encodeToString(it.toByteArray(Charsets.UTF_8), Base64.NO_WRAP) }
                .decoder { Base64.decode(it, Base64.NO_WRAP).toString(Charsets.UTF_8) }
                .build()

        p.edit().putString("key1", "hoge")
                .putInt("key2", 68)
                .putBoolean("key3", true)
                .commit()

        text.text = "original:\n" +
                " key1: hoge\n" +
                " key2: 68\n" +
                " key3: true\n" +
                "\n" +
                "restored:\n" +
                " key1:${p.getString("key1", null)}\n" +
                " key1:${p.getInt("key2", 0)}\n" +
                " key1:${p.getBoolean("key3", false)}\n"
    }
}
