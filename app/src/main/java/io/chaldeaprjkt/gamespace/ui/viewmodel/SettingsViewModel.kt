package io.chaldeaprjkt.gamespace.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemProperties
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.data.UserGame
import io.chaldeaprjkt.gamespace.utils.GameModeUtils
import javax.inject.Inject

data class RegisteredGame(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val mode: Int
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSettings: AppSettings,
    private val systemSettings: SystemSettings,
    private val gameModeUtils: GameModeUtils
) : ViewModel() {

    var callOverlayEnabled by mutableStateOf(appSettings.callOverlayEnabled)
        private set

    var danmakuNotification by mutableStateOf(appSettings.danmakuNotification)
        private set

    var callsMode by mutableIntStateOf(appSettings.callsMode)
        private set

    var ringerMode by mutableIntStateOf(appSettings.ringerMode)
        private set

    var noAutoBrightness by mutableStateOf(appSettings.noAutoBrightness)
        private set

    var noThreeScreenshot by mutableStateOf(appSettings.noThreeScreenshot)
        private set

    var stayAwake by mutableStateOf(systemSettings.stayAwake)
        private set

    var menuOpacity by mutableFloatStateOf(appSettings.menuOpacity.toFloat())
        private set

    var registeredGames by mutableStateOf<List<RegisteredGame>>(emptyList())
        private set

    var quickStartApps by mutableStateOf(appSettings.quickStartApps)
        private set

    var bypassChargeEnabled by mutableStateOf(systemSettings.bypassChargeEnabled)
        private set

    var iconIdleAlpha by mutableFloatStateOf(appSettings.iconIdleAlpha.toFloat())
        private set

    var autoDnd by mutableStateOf(appSettings.autoDnd)
        private set

    var notificationStyle by mutableStateOf(appSettings.notificationStyle)
        private set

    var slidingPillShowIcon by mutableStateOf(appSettings.slidingPillShowIcon)
        private set
    var slidingPillShowSender by mutableStateOf(appSettings.slidingPillShowSender)
        private set
    var slidingPillShowMessage by mutableStateOf(appSettings.slidingPillShowMessage)
        private set
    var slidingPillCapsuleMode by mutableStateOf(appSettings.slidingPillCapsuleMode)
        private set
    var slidingPillAnimationType by mutableStateOf(appSettings.slidingPillAnimationType)
        private set
    var slidingPillPosition by mutableStateOf(appSettings.slidingPillPosition)
        private set
    var slidingPillSize by mutableStateOf(appSettings.slidingPillSize)
        private set
    var slidingPillLaneCount by mutableIntStateOf(appSettings.slidingPillLaneCount)
        private set
    var slidingPillFontSize by mutableIntStateOf(appSettings.slidingPillFontSize)
        private set
    var slidingPillMaxWidth by mutableIntStateOf(appSettings.slidingPillMaxWidth)
        private set
    var slidingPillOverflowMode by mutableStateOf(appSettings.slidingPillOverflowMode)
        private set
    var slidingPillAnimationSpeed by mutableIntStateOf(appSettings.slidingPillAnimationSpeed)
        private set
    var slidingPillBackgroundOpacity by mutableIntStateOf(appSettings.slidingPillBackgroundOpacity)
        private set
    var slidingPillShadowStrength by mutableIntStateOf(appSettings.slidingPillShadowStrength)
        private set
    var slidingPillSlideAcross by mutableStateOf(appSettings.slidingPillSlideAcross)
        private set

    val isBypassSupported = Build.MANUFACTURER.equals("Google", ignoreCase = true) 
            || SystemProperties.getBoolean("persist.sys.ax_chg_bypass", false)

    init {
        loadRegisteredGames()
    }

    fun updateCallOverlay(enabled: Boolean) {
        callOverlayEnabled = enabled
        appSettings.callOverlayEnabled = enabled
    }

    fun updateDanmakuNotification(enabled: Boolean) {
        danmakuNotification = enabled
        appSettings.danmakuNotification = enabled
    }

    fun updateCallsMode(mode: Int) {
        callsMode = mode
        appSettings.callsMode = mode
    }

    fun updateRingerMode(mode: Int) {
        ringerMode = mode
        appSettings.ringerMode = mode
    }

    fun updateNoAutoBrightness(disabled: Boolean) {
        noAutoBrightness = disabled
        appSettings.noAutoBrightness = disabled
    }

    fun updateNoThreeScreenshot(disabled: Boolean) {
        noThreeScreenshot = disabled
        appSettings.noThreeScreenshot = disabled
    }

    fun updateStayAwake(enabled: Boolean) {
        stayAwake = enabled
        systemSettings.stayAwake = enabled
    }

    fun updateMenuOpacity(opacity: Float) {
        menuOpacity = opacity
        appSettings.menuOpacity = opacity.toInt()
    }

    fun updateQuickStartApps(apps: String) {
        quickStartApps = apps
        appSettings.quickStartApps = apps
    }

    fun updateBypassChargeEnabled(enabled: Boolean) {
        bypassChargeEnabled = enabled
        systemSettings.bypassChargeEnabled = enabled
    }

    fun updateIconIdleAlpha(alpha: Float) {
        iconIdleAlpha = alpha
        appSettings.iconIdleAlpha = alpha.toInt()
    }

    fun updateAutoDnd(enabled: Boolean) {
        autoDnd = enabled
        appSettings.autoDnd = enabled
    }

    fun updateNotificationStyle(style: String) {
        notificationStyle = style
        appSettings.notificationStyle = style
    }

    fun updateSlidingPillShowIcon(enabled: Boolean) { slidingPillShowIcon = enabled; appSettings.slidingPillShowIcon = enabled }
    fun updateSlidingPillShowSender(enabled: Boolean) { slidingPillShowSender = enabled; appSettings.slidingPillShowSender = enabled }
    fun updateSlidingPillShowMessage(enabled: Boolean) { slidingPillShowMessage = enabled; appSettings.slidingPillShowMessage = enabled }
    fun updateSlidingPillCapsuleMode(enabled: Boolean) { slidingPillCapsuleMode = enabled; appSettings.slidingPillCapsuleMode = enabled }
    fun updateSlidingPillAnimationType(type: String) { slidingPillAnimationType = type; appSettings.slidingPillAnimationType = type }
    fun updateSlidingPillPosition(pos: String) { slidingPillPosition = pos; appSettings.slidingPillPosition = pos }
    fun updateSlidingPillSize(size: String) { slidingPillSize = size; appSettings.slidingPillSize = size }
    fun updateSlidingPillLaneCount(count: Int) { slidingPillLaneCount = count; appSettings.slidingPillLaneCount = count }
    fun updateSlidingPillFontSize(size: Int) { slidingPillFontSize = size; appSettings.slidingPillFontSize = size }
    fun updateSlidingPillMaxWidth(width: Int) { slidingPillMaxWidth = width; appSettings.slidingPillMaxWidth = width }
    fun updateSlidingPillOverflowMode(mode: String) { slidingPillOverflowMode = mode; appSettings.slidingPillOverflowMode = mode }
    fun updateSlidingPillAnimationSpeed(speed: Int) { slidingPillAnimationSpeed = speed; appSettings.slidingPillAnimationSpeed = speed }
    fun updateSlidingPillBackgroundOpacity(opacity: Int) { slidingPillBackgroundOpacity = opacity; appSettings.slidingPillBackgroundOpacity = opacity }
    fun updateSlidingPillShadowStrength(strength: Int) { slidingPillShadowStrength = strength; appSettings.slidingPillShadowStrength = strength }
    fun updateSlidingPillSlideAcross(enabled: Boolean) { slidingPillSlideAcross = enabled; appSettings.slidingPillSlideAcross = enabled }

    fun refreshSlidingPillSettings() {
        slidingPillShowIcon = appSettings.slidingPillShowIcon
        slidingPillShowSender = appSettings.slidingPillShowSender
        slidingPillShowMessage = appSettings.slidingPillShowMessage
        slidingPillCapsuleMode = appSettings.slidingPillCapsuleMode
        slidingPillAnimationType = appSettings.slidingPillAnimationType
        slidingPillPosition = appSettings.slidingPillPosition
        slidingPillSize = appSettings.slidingPillSize
        slidingPillLaneCount = appSettings.slidingPillLaneCount
        slidingPillFontSize = appSettings.slidingPillFontSize
        slidingPillMaxWidth = appSettings.slidingPillMaxWidth
        slidingPillOverflowMode = appSettings.slidingPillOverflowMode
        slidingPillAnimationSpeed = appSettings.slidingPillAnimationSpeed
        slidingPillBackgroundOpacity = appSettings.slidingPillBackgroundOpacity
        slidingPillShadowStrength = appSettings.slidingPillShadowStrength
        slidingPillSlideAcross = appSettings.slidingPillSlideAcross
    }

    fun loadRegisteredGames() {
        val pm = context.packageManager
        val flags = PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())

        registeredGames = systemSettings.userGames
            .mapNotNull { game ->
                try {
                    val appInfo = pm.getApplicationInfo(game.packageName, flags)
                    RegisteredGame(
                        packageName = game.packageName,
                        label = appInfo.loadLabel(pm).toString(),
                        icon = appInfo.loadIcon(pm),
                        mode = game.mode
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
    }

    fun registerGame(packageName: String) {
        val games = systemSettings.userGames.toMutableList()
        if (!games.any { it.packageName == packageName }) {
            games.add(UserGame(packageName))
            systemSettings.userGames = games
        }
        loadRegisteredGames()
    }

    fun unregisterGame(packageName: String) {
        gameModeUtils.setAngleDriverChoice(packageName, GameModeUtils.DRIVER_CHOICE_DEFAULT)
        val games = systemSettings.userGames.toMutableList()
        games.removeIf { it.packageName == packageName }
        systemSettings.userGames = games
        loadRegisteredGames()
    }

    fun updateGameMode(packageName: String, mode: Int) {
        gameModeUtils.setGameModeFor(packageName, systemSettings, mode)
        loadRegisteredGames()
    }

    val gameModeOptions = listOf(1 to "Standard", 2 to "Performance", 3 to "Battery")

    val angleFeatureAvailable: Boolean
        get() = gameModeUtils.findAnglePackage()?.isEnabled == true && gameModeUtils.isVulkanSupported()

    fun getAngleDriverChoice(packageName: String): String = gameModeUtils.getAngleDriverChoice(packageName)

    fun updateAngleDriverChoice(packageName: String, choice: String) {
        gameModeUtils.setAngleDriverChoice(packageName, choice)
    }

    val angleDriverOptions = listOf(
        GameModeUtils.DRIVER_CHOICE_DEFAULT to "Default",
        GameModeUtils.DRIVER_CHOICE_ANGLE to "ANGLE",
        GameModeUtils.DRIVER_CHOICE_NATIVE to "Native"
    )
}
