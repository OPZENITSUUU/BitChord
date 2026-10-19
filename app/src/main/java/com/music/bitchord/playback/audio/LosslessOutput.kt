package com.music.bitchord.playback.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.music.bitchord.data.TrackLog
import com.music.bitchord.playback.audio.bluetooth.BluetoothAudioTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Whether the phone has a route that is eligible for an addon requiring
 * lossless output — not proof that the complete source-to-speaker path is
 * bit-perfect. System mixing, DSP and source format are reported separately.
 *
 * Wired, USB, HDMI and dock routes are treated as potentially capable; the
 * active output negotiation still determines the actual PCM format. Bluetooth
 * is accepted only when Android explicitly reports aptX Lossless and a 16-bit,
 * 44.1-kHz configuration. A codec or sample-rate label that merely advertises
 * Hi-Res does not establish bit-exact transport:
 *
 *  - **Wired:** 3.5mm headphones or headset, analog/digital line out, aux.
 *  - **USB:** USB-C headphones, USB DACs and USB audio accessories.
 *  - **HDMI:** HDMI, ARC and eARC (a TV, receiver or soundbar).
 *  - **Dock:** digital and analog docks.
 *  - **Bluetooth A2DP only when aptX Lossless is explicitly named and the
 *    negotiated configuration is 16-bit/44.1-kHz.** LDAC, LHDC and Samsung
 *    UHQ may advertise high-resolution input support, but this code has no
 *    bit-exact transport evidence for those modes and therefore does not use
 *    them to satisfy a lossless-only source gate. Other or unknown codecs are
 *    likewise rejected conservatively.
 *
 * The phone's own speaker, earpiece, hearing aids, casting and BLE Audio do
 * not count either. Bluetooth is only counted when Android names the codec,
 * which needs the Nearby devices permission on Android 12+; without it the
 * codec is unknown, and an unknown codec is reported as such rather than
 * guessed — see [State.bluetoothCodecUnknown].
 */
object LosslessOutput {

    enum class Kind { WIRED, USB, HDMI, DOCK, BLUETOOTH }

    data class State(
        val capable: Boolean = false,
        /** What qualified, when something did. */
        val via: Kind? = null,
        /** The Bluetooth codec in use, when Android named one. */
        val bluetoothCodec: String? = null,
        /** A Bluetooth output is connected but its codec could not be read — usually a missing permission. */
        val bluetoothCodecUnknown: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /** The answer every gate reads. False until [init] has looked. */
    val capable: Boolean get() = _state.value.capable

    private var audioManager: AudioManager? = null
    private var tracker: BluetoothAudioTracker? = null

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()
    }

    fun init(context: Context) {
        if (audioManager != null) return
        val app = context.applicationContext
        val manager = app.getSystemService(AudioManager::class.java) ?: return
        audioManager = manager
        manager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        // Its own tracker rather than the playback service's: the gate has to
        // be right on the sources screen with nothing playing, when that one
        // is not running.
        val bluetooth = BluetoothAudioTracker(app).also { it.start() }
        tracker = bluetooth
        // Made here, not as a field: plain-JVM tests read [capable] through
        // the source registry, and have no main dispatcher to build one on.
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            .launch { bluetooth.telemetry.collect { recompute() } }
        recompute()
    }

    /** Asks Bluetooth again — after the Nearby devices permission is granted, say. */
    fun refresh() {
        tracker?.refreshCurrentDevice()
        recompute()
    }

    private fun recompute() {
        val outputs = audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
            .filter { it.isSink }
            .map { it.type }
            .toSet()
        val telemetry = tracker?.telemetry?.value
        val named = telemetry?.takeIf { it.hasNamedCodec }
        val next = decide(outputs, named?.codecName, named?.sampleRateHz, named?.bitDepth)
        if (next != _state.value) {
            // The rate is here for codecs we have never seen a phone name —
            // Wi-Fi earbuds, vendor codecs — so a pasted log shows what to match.
            TrackLog.d(TAG, "lossless output: $next (outputs=$outputs, codecRate=${named?.sampleRateHz})")
            _state.value = next
        }
    }

    /**
     * The decision itself, from the connected output types and the Bluetooth
     * codec Android named (null if it named none), with the sample rate that
     * codec is configured for. Pure, for tests.
     */
    internal fun decide(
        outputTypes: Set<Int>,
        bluetoothCodec: String?,
        bluetoothSampleRateHz: Int? = null,
        bluetoothBitDepth: Int? = null,
    ): State {
        val cabled = when {
            outputTypes.any { it in WIRED } -> Kind.WIRED
            outputTypes.any { it in USB } -> Kind.USB
            outputTypes.any { it in HDMI } -> Kind.HDMI
            outputTypes.any { it in DOCK } -> Kind.DOCK
            else -> null
        }
        if (cabled != null) return State(capable = true, via = cabled, bluetoothCodec = bluetoothCodec)
        if (AudioDeviceInfo.TYPE_BLUETOOTH_A2DP !in outputTypes) return State()
        if (bluetoothCodec == null) return State(bluetoothCodecUnknown = true)
        val lossless = isLosslessBluetoothCodec(bluetoothCodec, bluetoothSampleRateHz, bluetoothBitDepth)
        return State(
            capable = lossless,
            via = Kind.BLUETOOTH.takeIf { lossless },
            bluetoothCodec = bluetoothCodec,
        )
    }

    /**
     * aptX Lossless only, when the negotiated PCM is the CD-quality mode
     * Qualcomm documents as 16-bit/44.1-kHz. A high-rate LDAC/LHDC/SSC
     * configuration is not evidence of bit-exact transport. Unknown rate or
     * bit depth is a refusal, not an optimistic guess.
     */
    internal fun isLosslessBluetoothCodec(
        name: String,
        sampleRateHz: Int? = null,
        bitDepth: Int? = null,
    ): Boolean {
        val codec = name.uppercase(Locale.ROOT)
        return "APTX" in codec && "LOSSLESS" in codec &&
            sampleRateHz == 44_100 && bitDepth == 16
    }

    // AudioDeviceInfo constants, by value so the newer ones compile into older
    // API levels as the plain ints they are; a phone that predates a type
    // simply never reports it.
    private val WIRED = setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_LINE_ANALOG,
        AudioDeviceInfo.TYPE_LINE_DIGITAL,
        AudioDeviceInfo.TYPE_AUX_LINE,
    )
    private val USB = setOf(
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_ACCESSORY,
    )
    private val HDMI = setOf(
        AudioDeviceInfo.TYPE_HDMI,
        AudioDeviceInfo.TYPE_HDMI_ARC,
        29, // TYPE_HDMI_EARC, API 31
    )
    private val DOCK = setOf(
        AudioDeviceInfo.TYPE_DOCK,
        31, // TYPE_DOCK_ANALOG, API 34
    )

    private const val TAG = "BitChord"
}
