package com.chauffdoc.app

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Vibrator
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/**
 * Enveloppe native de Chauff'Doc.
 *
 * La page (app/src/main/assets/chauffdoc.html) est la même que la version
 * web. Le magnétomètre du navigateur (Generic Sensor API) n'est pas
 * disponible dans une WebView Android : cette activité lit donc le capteur
 * en natif via SensorManager et transmet les valeurs à la page par un pont
 * JavaScript, sans changer le reste de l'application.
 */
class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var webView: WebView
    private lateinit var sensorManager: SensorManager
    private var magnetometer: Sensor? = null
    private var listening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(Bridge(), "AndroidApp")
        setContentView(webView)

        webView.loadUrl("file:///android_asset/chauffdoc.html")
    }

    override fun onPause() {
        super.onPause()
        stopMagnetometer()
    }

    private fun startMagnetometer() {
        if (magnetometer == null || listening) return
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        listening = true
    }

    private fun stopMagnetometer() {
        if (!listening) return
        sensorManager.unregisterListener(this)
        listening = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        webView.post {
            webView.evaluateJavascript(
                "window.__nativeMag && window.__nativeMag($x,$y,$z);", null
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /** Pont exposé à la page : window.AndroidApp.startMag() / stopMag(). */
    inner class Bridge {
        @JavascriptInterface
        fun startMag() {
            if (magnetometer == null) throw IllegalStateException("no sensor")
            runOnUiThread { startMagnetometer() }
        }

        @JavascriptInterface
        fun stopMag() {
            runOnUiThread { stopMagnetometer() }
        }

        @JavascriptInterface
        fun vibrate(ms: Long) {
            @Suppress("DEPRECATION")
            val v = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            v?.vibrate(ms)
        }
    }
}
