package com.martinm.sharedocks

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

object TtsHandler {
    private lateinit var mTextToSpeech: TextToSpeech
    private lateinit var mAudioManager: AudioManager
    var utteranceCount = 0

    fun initialize(context: Context) {
        val initListener = TextToSpeech.OnInitListener {
            if (it == TextToSpeech.SUCCESS) {
                mTextToSpeech.language = Locale.getDefault()
            }

            mTextToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                private val focusGain = if (ConfigurationHandler.getExclusiveAudioEnabled()) {
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                } else {
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                }
                private val mFocusRequest: AudioFocusRequest =
                    AudioFocusRequest.Builder(focusGain).build()

                override fun onDone(p0: String?) {
                    mAudioManager.abandonAudioFocusRequest(mFocusRequest)
                    if (--utteranceCount <= 0) {
                        mTextToSpeech.shutdown()
                    }
                }

                override fun onError(p0: String?) {
                }

                override fun onStart(p0: String?) {
                    mAudioManager.requestAudioFocus(mFocusRequest)
                }
            })
        }
        mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mTextToSpeech = TextToSpeech(context, initListener)
    }

    fun speak(string: String) {
        mTextToSpeech.speak(
            string,
            TextToSpeech.QUEUE_ADD,
            null,
            ""
        )
    }

    fun buildTrackingTTS(context: Context, stationId: String, isAvailable: Boolean): String {
        if (ShareApiHandler.docks[stationId] == null) {
            return ""
        }

        var stationTTS = ConfigurationHandler.getNickname(stationId)

        if (stationTTS == "") {
            stationTTS = ShareApiHandler.docks[stationId]!!.name.replace(
                "/",
                context.getString(R.string.tts_replace_intersection)
            )
        }

        if (isAvailable) {
            return context.getString(R.string.tts_update_available, stationTTS)
        }

        return context.getString(R.string.tts_update_full, stationTTS)
    }
}