package ms.ralph.psp.sample

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import kotlinx.android.synthetic.main.activity_main.text
import ms.ralph.psp.PluggableSharedPreferences

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sp = applicationContext.getSharedPreferences("test", Context.MODE_PRIVATE)

        val p = PluggableSharedPreferences.Builder(sp)
            .keyEncoder { Base64.encodeToString(it.toByteArray(Charsets.UTF_8), Base64.NO_WRAP) }
            .keyDecoder { Base64.decode(it, Base64.NO_WRAP).toString(Charsets.UTF_8) }
            .valueEncoder { _, v ->
                Base64.encodeToString(
                    v.toByteArray(Charsets.UTF_8),
                    Base64.NO_WRAP
                )
            }
            .valueDecoder { _, v -> Base64.decode(v, Base64.NO_WRAP).toString(Charsets.UTF_8) }
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
