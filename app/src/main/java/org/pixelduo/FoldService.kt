package org.pixelduo

import android.accessibilityservice.AccessibilityService
import android.content.*
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.*
import android.hardware.display.DisplayManager
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import java.util.concurrent.Executors
import kotlin.math.abs

// Pixel generations can expose either wake-up or non-wake-up hinge sensors.
internal fun SensorManager.hingeSensor(): Sensor? = getDefaultSensor(Sensor.TYPE_HINGE_ANGLE, false)
    ?: getDefaultSensor(Sensor.TYPE_HINGE_ANGLE, true)

class FoldService : AccessibilityService(), SensorEventListener, DisplayManager.DisplayListener,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val handler = Handler(Looper.getMainLooper())
    private val captureExecutor = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("pixelduo", MODE_PRIVATE) }
    private val sensors by lazy { getSystemService(SensorManager::class.java) }
    private val power by lazy { getSystemService(PowerManager::class.java) }
    private val displays by lazy { getSystemService(DisplayManager::class.java) }
    private val wm by lazy { getSystemService(WindowManager::class.java) }
    private var listening = false
    private var started = false
    private var lastAngle = Float.NaN
    private var surface: FoldSurface? = null
    private var generation = 0
    private var capturePending = false
    private var viewport = ""
    private var lastCaptureAt = 0L
    private var inner = false
    private val idle = Runnable { dismiss() }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) { updateListening() }
    }
    private val animationObserver = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) { updateListening() }
    }

    override fun onServiceConnected() {
        started = true
        prefs.registerOnSharedPreferenceChangeListener(this)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_SCREEN_OFF)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        else registerReceiver(receiver, filter)
        contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, animationObserver)
        displays.registerDisplayListener(this, handler)
        updateListening()
    }

    private fun permitted() = FoldMotion.shouldAnimate(prefs.getBoolean("enabled", false),
        power.isInteractive, power.isPowerSaveMode,
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f)

    private fun updateListening() {
        val sensor = sensors.hingeSensor()
        val shouldListen = permitted() && sensor != null
        if (shouldListen && !listening) {
            lastAngle = Float.NaN
            listening = sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME, handler)
        } else if (!shouldListen) {
            sensors.unregisterListener(this)
            listening = false
            lastAngle = Float.NaN
            dismiss()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val angle = FoldMotion.normalize(event.values.firstOrNull() ?: return) ?: return
        if (!permitted()) { updateListening(); return }
        val previous = lastAngle
        lastAngle = angle
        // Ignore the initial on-change sensor value: enabling must not replay an old fold.
        if (!previous.isFinite() || abs(previous - angle) < .15f) return
        refreshViewport()
        if (angle <= FoldMotion.CLOSED || angle >= FoldMotion.OPEN) {
            surface?.update(angle, inner)
            handler.removeCallbacks(idle)
            handler.postDelayed(idle, 120L)
            return
        }
        if (surface == null && !capturePending) capture()
        surface?.update(angle, inner)
        handler.removeCallbacks(idle)
        handler.postDelayed(idle, FoldMotion.IDLE_MS)
    }

    private fun refreshViewport(): Boolean {
        val display = displays.getDisplay(Display.DEFAULT_DISPLAY) ?: return false
        val bounds = wm.maximumWindowMetrics.bounds
        val density = resources.displayMetrics.density
        inner = minOf(bounds.width(), bounds.height()) / density >= 600f
        val key = "${bounds.width()}:${bounds.height()}:${display.rotation}"
        if (key == viewport) return false
        viewport = key
        dismiss()
        return true
    }

    private fun capture() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastCaptureAt < 350L) { show(null); return }
        lastCaptureAt = now
        capturePending = true
        val token = generation
        // One snapshot per transition, never a screen-recording or polling loop.
        takeScreenshot(Display.DEFAULT_DISPLAY, captureExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(result: ScreenshotResult) {
                var bitmap: Bitmap? = null
                try {
                    Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)?.let { hardware ->
                        try {
                            val copy = hardware.copy(Bitmap.Config.ARGB_8888, false)
                            val scale = minOf(1f, 960f / maxOf(copy.width, copy.height))
                            bitmap = Bitmap.createScaledBitmap(copy, maxOf(1, (copy.width * scale).toInt()),
                                maxOf(1, (copy.height * scale).toInt()), true)
                            if (bitmap !== copy) copy.recycle()
                        } finally { hardware.recycle() }
                    }
                } catch (e: RuntimeException) {
                    Log.w("PixelDuo", "Snapshot unavailable; using live shading", e)
                } finally { result.hardwareBuffer.close() }
                val frame = bitmap
                handler.post {
                    if (token != generation || !permitted()) frame?.recycle()
                    else { capturePending = false; show(frame) }
                }
            }
            override fun onFailure(errorCode: Int) {
                handler.post {
                    if (token == generation) {
                        capturePending = false
                        if (permitted()) show(null)
                    }
                }
            }
        })
    }

    private fun show(bitmap: Bitmap?) {
        if (!permitted() || !lastAngle.isFinite() || lastAngle <= FoldMotion.CLOSED || lastAngle >= FoldMotion.OPEN) {
            bitmap?.recycle(); return
        }
        val display = displays.getDisplay(Display.DEFAULT_DISPLAY)
        if (display == null || display.state != Display.STATE_ON) { bitmap?.recycle(); return }
        val view = FoldSurface(this, bitmap, display.rotation)
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT).apply {
            setFitInsetsTypes(0)
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            title = "Pixel Duo fold animation"
        }
        try {
            wm.addView(view, params)
            surface = view
            view.update(lastAngle, inner)
        } catch (e: RuntimeException) {
            view.release()
            Log.w("PixelDuo", "Overlay unavailable", e)
        }
    }

    private fun dismiss() {
        generation++
        capturePending = false
        handler.removeCallbacks(idle)
        surface?.let { view ->
            try { wm.removeViewImmediate(view) }
            catch (e: IllegalArgumentException) { Log.w("PixelDuo", "Overlay already detached", e) }
            finally { view.release() }
        }
        surface = null
    }

    override fun onDisplayChanged(displayId: Int) {
        if (displayId != Display.DEFAULT_DISPLAY || !listening) return
        val changed = refreshViewport()
        if (displays.getDisplay(displayId)?.state != Display.STATE_ON) dismiss()
        else if (changed && lastAngle.isFinite() && lastAngle > FoldMotion.CLOSED && lastAngle < FoldMotion.OPEN) {
            capture()
            handler.postDelayed(idle, FoldMotion.IDLE_MS)
        }
    }
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (started) onDisplayChanged(Display.DEFAULT_DISPLAY)
    }
    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) { updateListening() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* No window text is inspected. */ }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onDisplayAdded(displayId: Int) {}
    override fun onDisplayRemoved(displayId: Int) {}
    override fun onInterrupt() { dismiss() }
    override fun onUnbind(intent: Intent?): Boolean { stop(); return super.onUnbind(intent) }
    override fun onDestroy() { stop(); captureExecutor.shutdown(); super.onDestroy() }
    private fun stop() {
        dismiss()
        sensors.unregisterListener(this)
        listening = false
        if (started) {
            started = false
            prefs.unregisterOnSharedPreferenceChangeListener(this)
            unregisterReceiver(receiver)
            displays.unregisterDisplayListener(this)
            contentResolver.unregisterContentObserver(animationObserver)
        }
    }
}
