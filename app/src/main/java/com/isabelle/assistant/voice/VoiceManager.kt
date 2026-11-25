package com.isabelle.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** Reconhecimento de voz (pt-BR) e fala. Use sempre na thread principal. */
class VoiceManager(private val ctx: Context) {

    private var rec: SpeechRecognizer? = null
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(ctx) {
        if (it == TextToSpeech.SUCCESS) {
            tts_ready()
        }
    }

    private fun tts_ready() {
        tts.language = Locale("pt", "BR")
        ready = true
    }

    fun listen(onPartial: (String) -> Unit = {}, onFinal: (String) -> Unit, onErr: (Int) -> Unit = {}) {
        stopListening()
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) { onErr(-1); return }
        val r = SpeechRecognizer.createSpeechRecognizer(ctx)
        rec = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(t: Int, b: Bundle?) {}
            override fun onPartialResults(b: Bundle?) {
                b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onPartial)
            }
            override fun onResults(b: Bundle?) {
                val t = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (t.isNullOrBlank()) onErr(SpeechRecognizer.ERROR_NO_MATCH) else onFinal(t)
            }
            override fun onError(e: Int) = onErr(e)
        })
        r.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        )
    }

    fun stopListening() {
        rec?.destroy(); rec = null
    }

    fun speak(text: String, finished: () -> Unit = {}) {
        if (!ready) { finished(); return }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) = finished()
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) = finished()
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "isabelle")
    }

    fun shutdown() {
        stopListening(); tts.stop(); tts.shutdown()
    }
}
