package expo.modules.call

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoCallModule : Module() {
  private val audioManager: AudioManager
    get() {
      val context = appContext.reactContext
        ?: throw IllegalStateException("React context indisponível")
      return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

  private var lastState: String? = null
  private var modeListener: AudioManager.OnModeChangedListener? = null

  // fallback para Android < 12 (sem listener nativo de modo)
  private val handler = Handler(Looper.getMainLooper())
  private val poller = object : Runnable {
    override fun run() {
      emitIfChanged()
      handler.postDelayed(this, 1000)
    }
  }

  private fun currentState(): String = when (audioManager.mode) {
    AudioManager.MODE_IN_CALL,
    AudioManager.MODE_IN_COMMUNICATION -> "active"
    AudioManager.MODE_RINGTONE -> "ringing"
    else -> "idle"
  }

  private fun emitIfChanged() {
    val state = currentState()
    if (state != lastState) {
      lastState = state
      sendEvent("onCallStateChange", mapOf("state" to state))
    }
  }

  override fun definition() = ModuleDefinition {
    Name("ExpoCall")

    Events("onCallStateChange")

    Function("getCallState") {
      currentState()
    }

    OnStartObserving {
      lastState = currentState()

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val listener = AudioManager.OnModeChangedListener { emitIfChanged() }
        modeListener = listener
        audioManager.addOnModeChangedListener(
          appContext.reactContext!!.mainExecutor,
          listener
        )
      } else {
        handler.post(poller)
      }
    }

    OnStopObserving {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        modeListener?.let { audioManager.removeOnModeChangedListener(it) }
        modeListener = null
      } else {
        handler.removeCallbacks(poller)
      }
    }
  }
}
