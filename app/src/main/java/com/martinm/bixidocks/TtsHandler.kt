package com.martinm.bixidocks

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

object TtsHandler {
    private lateinit var mTextToSpeech: TextToSpeech
    private lateinit var mAudioManager: AudioManager

    private val initListener = TextToSpeech.OnInitListener {
        if (it == TextToSpeech.SUCCESS) {
            mTextToSpeech.language = Locale.getDefault()
        }
    }

    fun initialize(context: Context) {
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


}