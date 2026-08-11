package io.chaldeaprjkt.gamespace.gamebar

import android.animation.TimeAnimator
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Handler
import android.text.TextUtils
import android.os.Looper
import android.os.UserHandle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.utils.dp
import io.chaldeaprjkt.gamespace.utils.statusbarHeight
import java.util.LinkedList
import javax.inject.Inject

data class SlidingPillState(
    val pill: SlidingPillNotification,
    var view: View? = null,
    var lane: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
    var phase: PillPhase = PillPhase.QUEUED,
    var enterProgress: Float = 0f,
    var displayTimeRemaining: Long = 0L,
    var exitProgress: Float = 0f,
    var startX: Float = 0f,
    var targetX: Float = 0f,
    var currentX: Float = 0f,
    var currentY: Float = 0f,
)

enum class PillPhase { QUEUED, ENTERING, DISPLAYING, EXITING, REMOVED }

@ServiceScoped
class SlidingPillService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings,
) : SlidingPillNotificationCallback {

    private var notificationListener: SlidingPillNotificationListener? = null

    private val windowManager: WindowManager = context.getSystemService(WindowManager::class.java)!!
    private val handler = Handler(Looper.getMainLooper())

    private var container: FrameLayout? = null

    private val activePills = mutableListOf<SlidingPillState>()
    private val pillQueue = LinkedList<SlidingPillNotification>()

    private var isInitialized = false
    private var displayWidth = 0

    private var timeAnimator: TimeAnimator? = null

    private val iconCache = mutableMapOf<String, Drawable?>()
    private val accentCache = mutableMapOf<String, Int>()
    private val appLabelCache = mutableMapOf<String, String>()

    private val pillPool = mutableListOf<View>()

    override val slidingPillNotificationsEnabled: Boolean
        get() = appSettings.danmakuNotification && isStyleActive

    private val isStyleActive: Boolean
        get() = appSettings.notificationStyle == AppSettings.NOTIFICATION_STYLE_SLIDING_PILL

    fun init() {
        if (isInitialized) return
        notificationListener = SlidingPillNotificationListener().apply { callback = this@SlidingPillService }
        updateDisplayMetrics()
        registerListener()
        isInitialized = true
    }

    fun updateConfiguration(newConfig: Configuration) {
        updateDisplayMetrics()
    }

    fun destroy() {
        unregisterListener()
        stopAnimation()
        removeContainer()
        activePills.clear()
        pillQueue.clear()
        pillPool.clear()
        iconCache.clear()
        accentCache.clear()
        appLabelCache.clear()
        isInitialized = false
    }

    private fun registerListener() {
        notificationListener?.registerAsSystemService(
            context,
            ComponentName(context, SlidingPillService::class.java),
            UserHandle.USER_CURRENT
        )
    }

    private fun unregisterListener() {
        notificationListener?.unregisterAsSystemService()
    }

    private fun updateDisplayMetrics() {
        val bounds = windowManager.maximumWindowMetrics.bounds
        displayWidth = bounds.width()
    }

    override fun onNotificationReceived(pill: SlidingPillNotification) {
        if (!slidingPillNotificationsEnabled) return
        handler.post {
            removePillByKey(pill.key)
            enqueuePill(pill)
        }
    }

    override fun onNotificationRemoved(pill: SlidingPillNotification) {
        handler.post { removePillByKey(pill.key) }
    }

    override fun getAppLabel(packageName: String): String {
        return appLabelCache.getOrPut(packageName) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                context.packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) { packageName }
        }
    }

    override fun loadAppIcon(packageName: String): Drawable? {
        return iconCache.getOrPut(packageName) {
            try {
                context.packageManager.getApplicationIcon(packageName)
            } catch (_: Exception) { null }
        }
    }

    override fun loadAppAccentColor(packageName: String): Int {
        return accentCache.getOrPut(packageName) { computeAccentColor(packageName) }
    }

    private fun computeAccentColor(packageName: String): Int {
        val icon = loadAppIcon(packageName) ?: return Color.TRANSPARENT
        return try {
            val bitmap = android.graphics.Bitmap.createBitmap(
                icon.intrinsicWidth.coerceAtLeast(64),
                icon.intrinsicHeight.coerceAtLeast(64),
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            icon.setBounds(0, 0, canvas.width, canvas.height)
            icon.draw(canvas)
            var r = 0L; var g = 0L; var b = 0L; var count = 0L
            for (x in 0 until bitmap.width step 4) {
                for (y in 0 until bitmap.height step 4) {
                    val pixel = bitmap.getPixel(x, y)
                    val alpha = Color.alpha(pixel)
                    if (alpha > 128) {
                        r += Color.red(pixel)
                        g += Color.green(pixel)
                        b += Color.blue(pixel)
                        count++
                    }
                }
            }
            bitmap.recycle()
            if (count > 0) Color.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
            else Color.TRANSPARENT
        } catch (_: Exception) { Color.TRANSPARENT }
    }

    private fun enqueuePill(pill: SlidingPillNotification) {
        val activeCount = activePills.count {
            it.phase != PillPhase.REMOVED && it.phase != PillPhase.EXITING
        }
        if (activeCount < getMaxLanes()) {
            showPill(pill)
        } else {
            if (pillQueue.size < MAX_QUEUE_SIZE) pillQueue.add(pill)
        }
    }

    private fun showPill(pill: SlidingPillNotification) {
        val lane = findAvailableLane() ?: return
        ensureContainer()

        val state = SlidingPillState(
            pill = pill, lane = lane, phase = PillPhase.ENTERING
        )

        val pillHeight = getPillHeight()
        val vMargin = appSettings.slidingPillVerticalMargin.dp
        val laneTop = getNotificationAreaTop() + lane * (pillHeight + vMargin)
        state.height = pillHeight
        state.currentY = laneTop.toFloat()

        val view = obtainView(pill, state)
        state.view = view
        state.width = (view.layoutParams?.width ?: 200)
        state.height = pillHeight

        val hMargin = getHorizontalMargin()
        val entryDir = getEntryDirection()
        val slideAcross = appSettings.slidingPillSlideAcross
        when {
            entryDir == "fade" -> {
                state.startX = hMargin.toFloat()
                state.targetX = state.startX
            }
            entryDir.contains("right_left") -> {
                state.startX = displayWidth.toFloat()
                state.targetX = if (slideAcross) -state.width.toFloat()
                    else (displayWidth - state.width - hMargin).toFloat()
            }
            entryDir.contains("left_right") -> {
                state.startX = -state.width.toFloat()
                state.targetX = if (slideAcross) displayWidth.toFloat()
                    else hMargin.toFloat()
            }
            else -> {
                state.startX = displayWidth.toFloat()
                state.targetX = if (slideAcross) -state.width.toFloat()
                    else (displayWidth - state.width - hMargin).toFloat()
            }
        }
        state.currentX = state.startX
        state.displayTimeRemaining = DISPLAY_DURATION_MS

        view.alpha = 0f
        positionView(state)
        container?.addView(view)
        activePills.add(state)
        ensureAnimationRunning()
    }

    private fun findAvailableLane(): Int? {
        val maxLanes = getMaxLanes()
        val usedLanes = activePills
            .filter { it.phase != PillPhase.REMOVED && it.phase != PillPhase.EXITING }
            .map { it.lane }.toSet()
        for (i in 0 until maxLanes) {
            if (i !in usedLanes) return i
        }
        val exiting = activePills.filter { it.phase == PillPhase.EXITING }
        if (exiting.isNotEmpty()) return exiting.first().lane
        return null
    }

    private fun getMaxLanes(): Int = DEFAULT_LANE_COUNT

    private fun getEntryDirection(): String = appSettings.slidingPillAnimationType

    private fun getHorizontalMargin(): Int = appSettings.slidingPillHorizontalMargin.dp

    private fun getPillHeight(): Int {
        val lineHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, DEFAULT_TEXT_SIZE_SP, context.resources.displayMetrics
        )
        val lines = 2
        return (lineHeight.toInt() * lines + 24.dp).coerceAtLeast(48.dp)
    }

    private fun getNotificationAreaTop(): Int {
        val sbHeight = context.statusbarHeight
        return sbHeight + 4.dp
    }

    private fun obtainView(pill: SlidingPillNotification, state: SlidingPillState): View {
        val view = pillPool.lastOrNull()?.also { pillPool.removeLast() }
            ?: createPillView()
        bindPillView(view, pill, state)
        return view
    }

    private fun createPillView(): FrameLayout {
        val iconView = ImageView(context).apply {
            id = ICON_VIEW_ID
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val textView = TextView(context).apply {
            id = TEXT_VIEW_ID
            val initialMaxLines = 2
            maxLines = initialMaxLines
            includeFontPadding = false
        }
        return FrameLayout(context).apply {
            addView(iconView)
            addView(textView)
            isFocusable = false
            isClickable = false
            setWillNotDraw(false)
        }
    }

    private fun bindPillView(view: View, pill: SlidingPillNotification, state: SlidingPillState) {
        val frame = view as FrameLayout
        val iconView = frame.findViewById<ImageView>(ICON_VIEW_ID) ?: return
        val textView = frame.findViewById<TextView>(TEXT_VIEW_ID) ?: return

        val showSender = appSettings.slidingPillShowSender
        val showMessage = appSettings.slidingPillShowMessage
        val fontColor = if (appSettings.slidingPillFontColor == -1) Color.WHITE
            else appSettings.slidingPillFontColor
        val opacity = (appSettings.slidingPillBackgroundOpacity.coerceIn(0, 100) * 255 / 100)
            .coerceIn(0, 255)
        val contentColor = (fontColor and 0x00FFFFFF) or (opacity shl 24)

        val pillHeight = getPillHeight()
        val iconSize = (pillHeight * 0.65f).toInt().coerceAtLeast(16.dp)
        val iconPadding = (pillHeight - iconSize) / 2
        val contentPadding = 10.dp

        if (pill.appIcon != null) {
            iconView.visibility = View.VISIBLE
            iconView.setImageDrawable(pill.appIcon)
            iconView.imageAlpha = opacity
            iconView.layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                leftMargin = contentPadding
            }
        } else {
            iconView.visibility = View.GONE
            iconView.layoutParams = FrameLayout.LayoutParams(0, 0)
        }

        val sb = StringBuilder()
        if (showSender && !pill.sender.isNullOrBlank()) sb.append(pill.sender)
        if (showMessage && !pill.message.isNullOrBlank()) {
            if (sb.isNotEmpty()) sb.append(": ")
            sb.append(pill.message)
        }
        val displayText = sb.toString()
        textView.text = displayText
        textView.setTextColor(contentColor)
        textView.setShadowLayer(2.dp.toFloat(), 0f, 1.dp.toFloat(), Color.argb(100, 0, 0, 0))
        textView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
            val iconW = if (pill.appIcon != null) iconSize + contentPadding * 2 else 0
            leftMargin = iconW + 6.dp
        }

        val messageWidth = measureTextWidth(displayText, DEFAULT_TEXT_SIZE_SP)
        val iconW = if (pill.appIcon != null) iconSize + contentPadding * 2 else 0
        val textLeftMargin = 6.dp
        val contentWidth = iconW + textLeftMargin + messageWidth + contentPadding * 2
        val finalWidth = contentWidth.coerceIn(
            appSettings.slidingPillMinWidth.dp,
            (appSettings.slidingPillMaxWidth.dp).coerceAtMost(displayWidth - getHorizontalMargin() * 2)
        )
        state.width = finalWidth
        state.height = pillHeight
        frame.layoutParams = FrameLayout.LayoutParams(finalWidth, pillHeight)
        frame.setPadding(contentPadding, 0, contentPadding, 0)

        val availableTextWidth = finalWidth - iconW - textLeftMargin - contentPadding * 2
        textView.layoutParams = (textView.layoutParams as FrameLayout.LayoutParams).also {
            it.width = availableTextWidth.coerceAtLeast(0)
        }
        val baseMaxLines = 2
        textView.maxLines = baseMaxLines
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.setHorizontallyScrolling(false)
        textView.isHorizontalFadingEdgeEnabled = false

        updatePillAppearance(frame, pill, pillHeight)
        frame.tag = pill.key
    }

    private fun measureTextWidth(text: String, fontSizeSp: Float): Int {
        val paint = Paint().apply {
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, fontSizeSp, context.resources.displayMetrics
            )
        }
        return paint.measureText(text).toInt()
    }

    private fun updatePillAppearance(frame: FrameLayout, pill: SlidingPillNotification, pillHeight: Int) {
        val useMaterialYou = appSettings.slidingPillUseMaterialYou
        val useGradient = appSettings.slidingPillUseGradient

        val bgColor = when {
            useMaterialYou -> Color.parseColor("#CC1A1A2E")
            useGradient -> appSettings.slidingPillGradientStart
            else -> appSettings.slidingPillBackgroundColor
        }

        val opacity = (appSettings.slidingPillBackgroundOpacity.coerceIn(0, 100) * 255 / 100).coerceIn(0, 255)
        val colorWithAlpha = Color.argb(
            opacity, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor)
        )

        val cornerRadius = (pillHeight / 2f)

        val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(colorWithAlpha)
            setCornerRadius(cornerRadius)
            if (appSettings.slidingPillBorderWidth > 0) {
                setStroke(
                    appSettings.slidingPillBorderWidth.dp,
                    appSettings.slidingPillBorderColor
                )
            }
        }

        if (useGradient) {
            val sc = appSettings.slidingPillGradientStart
            val ec = appSettings.slidingPillGradientEnd
            bgDrawable.colors = intArrayOf(
                Color.argb(opacity, Color.red(sc), Color.green(sc), Color.blue(sc)),
                Color.argb(opacity, Color.red(ec), Color.green(ec), Color.blue(ec))
            )
            bgDrawable.orientation = android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM
        }

        frame.background = bgDrawable
        frame.elevation = 4.dp.toFloat()
    }

    private fun positionView(state: SlidingPillState) {
        state.view?.translationX = state.currentX
        state.view?.translationY = state.currentY
    }

    private fun ensureContainer() {
        if (container != null) return
        val containerView = FrameLayout(context).apply {
            isFocusable = false
            isClickable = false
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            preferMinimalPostProcessing = true
        }
        windowManager.addView(containerView, params)
        container = containerView
    }

    private fun ensureAnimationRunning() {
        if (timeAnimator != null) return
        timeAnimator = TimeAnimator().apply {
            setTimeListener { _, _, deltaTime ->
                onAnimationFrame(deltaTime)
            }
            start()
        }
    }

    private fun stopAnimation() {
        timeAnimator?.let {
            it.end()
            it.cancel()
        }
        timeAnimator = null
    }

    private fun onAnimationFrame(deltaNs: Long) {
        val deltaMs = if (deltaNs > 1_000_000L) deltaNs / 1_000_000L else deltaNs
        if (deltaMs <= 0L) return
        val speed = (appSettings.slidingPillAnimationSpeed * 1000).coerceAtLeast(50).toFloat()
        val frameDelta = (deltaMs.coerceAtMost(speed.toLong() / 2)).coerceAtLeast(1L)

        val toRemove = mutableListOf<SlidingPillState>()

        for (s in activePills) {
            when (s.phase) {
                PillPhase.ENTERING -> {
                    s.enterProgress += frameDelta / speed
                    if (s.enterProgress >= 1f) {
                        s.enterProgress = 1f
                        val isFade = getEntryDirection() == "fade"
                        if (isFade || !appSettings.slidingPillSlideAcross) {
                            s.phase = PillPhase.DISPLAYING
                            s.displayTimeRemaining = DISPLAY_DURATION_MS
                        } else {
                            s.phase = PillPhase.REMOVED
                            toRemove.add(s)
                        }
                    }
                    val isFade = getEntryDirection() == "fade"
                    if (isFade) {
                        s.view?.alpha = s.enterProgress
                    } else {
                        s.currentX = lerp(s.startX, s.targetX, s.enterProgress)
                        s.view?.alpha = 1f
                    }
                    positionView(s)
                }
                PillPhase.DISPLAYING -> {
                    s.displayTimeRemaining -= deltaMs
                    if (s.displayTimeRemaining <= 0) {
                        s.phase = PillPhase.EXITING
                        s.exitProgress = 0f
                    }
                }
                PillPhase.EXITING -> {
                    s.exitProgress += frameDelta / speed
                    if (s.exitProgress >= 1f) {
                        s.phase = PillPhase.REMOVED
                        toRemove.add(s)
                    } else {
                        val isFade = getEntryDirection() == "fade"
                        if (isFade) {
                            s.view?.alpha = 1f - s.exitProgress
                        } else {
                            val exitX = lerp(s.targetX,
                                if (getEntryDirection().contains("right_left")) displayWidth.toFloat()
                                else -s.width.toFloat(),
                                s.exitProgress)
                            s.currentX = exitX
                            s.view?.alpha = 1f
                        }
                        positionView(s)
                    }
                }
                else -> {}
            }
        }

        for (s in toRemove) {
            s.view?.let { v ->
                container?.removeView(v)
                v.alpha = 1f
                v.translationX = 0f
                v.translationY = 0f
                pillPool.add(v)
            }
            activePills.remove(s)
        }

        if (toRemove.isNotEmpty()) {
            while (pillQueue.isNotEmpty()) {
                val activeCount = activePills.count {
                    it.phase != PillPhase.REMOVED && it.phase != PillPhase.EXITING
                }
                if (activeCount >= getMaxLanes()) break
                val next = pillQueue.poll() ?: break
                showPill(next)
            }
        }

        if (activePills.isEmpty() && pillQueue.isNotEmpty()) {
            val next = pillQueue.poll()
            if (next != null) showPill(next)
        }

        if (activePills.isEmpty()) stopAnimation()
    }

    private fun removePillByKey(key: String) {
        val s = activePills.find { it.pill.key == key }
        if (s != null) {
            s.view?.let { v ->
                container?.removeView(v)
                v.alpha = 1f
                v.translationX = 0f
                v.translationY = 0f
                pillPool.add(v)
            }
            s.phase = PillPhase.REMOVED
            activePills.remove(s)
        }
        pillQueue.removeAll { it.key == key }
    }

    private fun removeContainer() {
        container?.let {
            runCatching { windowManager.removeViewImmediate(it) }
            container = null
        }
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

    companion object {
        private const val DEFAULT_TEXT_SIZE_SP = 14f
        private const val DEFAULT_LANE_COUNT = 3
        private const val DISPLAY_DURATION_MS = 3000L
        private const val MAX_QUEUE_SIZE = 50
        private val ICON_VIEW_ID = View.generateViewId()
        private val TEXT_VIEW_ID = View.generateViewId()
    }
}
