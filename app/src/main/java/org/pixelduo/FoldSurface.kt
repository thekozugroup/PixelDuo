package org.pixelduo

import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.*
import kotlin.math.abs

/** A half-resolution hardware surface, paced by vsync only while the angle changes. */
internal class FoldSurface(context: Context, private var bitmap: Bitmap?, private val rotation: Int) :
    SurfaceView(context), SurfaceHolder.Callback {
    private val choreographer = Choreographer.getInstance()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var shader: RuntimeShader? = null
    private val levels = mutableListOf<Bitmap>()
    private var target = 180f
    private var rendered = Float.NaN
    private var inner = true
    private var queued = false
    private var released = false
    private var lastFrame = 0L
    private val frame = Choreographer.FrameCallback { now ->
        queued = false
        if (!released && holder.surface.isValid) {
            rendered = if (rendered.isFinite()) FoldMotion.smooth(rendered, target,
                if (lastFrame == 0L) 16f else (now - lastFrame) / 1_000_000f) else target
            lastFrame = now
            drawFrame()
            if (abs(rendered - target) > .05f) requestFrame()
        }
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        holder.addCallback(this)
        bitmap?.let { image ->
            try {
                shader = RuntimeShader(DuoGlassShader.source).apply {
                    setInputShader("content", BitmapShader(image, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
                    var level = image
                    for (i in 1..7) {
                        level = Bitmap.createScaledBitmap(level, maxOf(1, level.width / 2), maxOf(1, level.height / 2), true)
                        levels.add(level)
                        val map = BitmapShader(level, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                            setFilterMode(BitmapShader.FILTER_MODE_LINEAR)
                            setLocalMatrix(Matrix().apply { setScale(image.width.toFloat() / level.width, image.height.toFloat() / level.height) })
                        }
                        setInputShader("mip$i", map)
                    }
                    setFloatUniform("texSize", image.width.toFloat(), image.height.toFloat())
                    setFloatUniform("aaStrength", .35f)
                    setFloatUniform("sampleScale", 1f, 1f)
                    setFloatUniform("sampleOffset", 0f, 0f)
                    setFloatUniform("intensity", 1f)
                    setFloatUniform("blurStrength", .25f)
                    setFloatUniform("seamOffset", .05f)
                    setFloatUniform("reflectedCover", 0f)
                    setFloatUniform("windowReveal", 1f)
                    setFloatUniform("startupEasing", 1f)
                    setFloatUniform("horizontal", if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) 1f else 0f)
                    setFloatUniform("reverse", if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_180) 1f else 0f)
                }
            } catch (e: RuntimeException) {
                shader = null
                Log.w("PixelDuo", "Glass shader unavailable; using live shading", e)
            }
        }
    }

    fun update(angle: Float, expanded: Boolean) {
        target = angle
        inner = expanded
        requestFrame()
    }
    private fun requestFrame() {
        if (!released && !queued && holder.surface.isValid) {
            queued = true
            choreographer.postFrameCallback(frame)
        }
    }
    override fun surfaceCreated(holder: SurfaceHolder) {
        val rate = display?.supportedModes?.filter { it.physicalWidth == display?.mode?.physicalWidth &&
            it.physicalHeight == display?.mode?.physicalHeight }?.maxOfOrNull { it.refreshRate }?.coerceAtMost(120f) ?: 60f
        holder.surface.setFrameRate(rate, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT, Surface.CHANGE_FRAME_RATE_ONLY_IF_SEAMLESS)
        requestFrame()
    }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) { requestFrame() }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) holder.setFixedSize(maxOf(1, w / 2), maxOf(1, h / 2))
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        choreographer.removeFrameCallback(frame)
        queued = false
    }

    private fun drawFrame() {
        if (width <= 0 || height <= 0) return
        var canvas: Canvas? = null
        try {
            canvas = holder.lockHardwareCanvas()
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            canvas.scale(canvas.width.toFloat() / width, canvas.height.toFloat() / height)
            val progress = FoldMotion.strength(rendered, inner)
            if (progress <= .001f) return
            val program = shader
            if (program != null) {
                program.setFloatUniform("origin", 0f, 0f)
                program.setFloatUniform("extent", width.toFloat(), height.toFloat())
                program.setFloatUniform("progress", progress)
                program.setFloatUniform("foldRadians", if (inner) progress * (Math.PI.toFloat() / 2f)
                    else Math.PI.toFloat() - rendered * Math.PI.toFloat() / 180f)
                program.setFloatUniform("inner", if (inner) 1f else 0f)
                program.setFloatUniform("fallback", 0f)
                program.setFloatUniform("earlyStretch", if (inner) .9f else 2.7f)
                program.setFloatUniform("endStretch", if (inner) .45f else 1.25f)
                program.setFloatUniform("verticalCompression", if (inner) .6f else 1.15f)
                paint.shader = program
            } else {
                val horizontal = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
                val reverse = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_180
                val alpha = (progress * progress * (3f - 2f * progress) * 190).toInt()
                val colors = intArrayOf(Color.argb(alpha, 0, 0, 0), Color.TRANSPARENT)
                if (reverse) colors.reverse()
                paint.shader = LinearGradient(0f, 0f, if (horizontal) 0f else width.toFloat(),
                    if (horizontal) height.toFloat() else 0f, colors, null, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        } catch (e: RuntimeException) {
            Log.w("PixelDuo", "Frame unavailable", e)
        } finally { if (canvas != null) holder.unlockCanvasAndPost(canvas) }
    }

    fun release() {
        if (released) return
        released = true
        choreographer.removeFrameCallback(frame)
        queued = false
        paint.shader = null
        shader = null
        levels.distinct().filter { it !== bitmap }.forEach { it.recycle() }
        levels.clear()
        bitmap?.recycle()
        bitmap = null
    }
}
