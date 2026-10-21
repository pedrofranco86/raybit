package com.pelops.assistant

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.net.Uri
import android.provider.Settings
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var history: TextView
    private lateinit var input: EditText
    private var tts: TextToSpeech? = null
    private val prefs by lazy { getSharedPreferences("pelops", MODE_PRIVATE) }
    private val voiceRequest = 700

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 16)
            setBackgroundColor(0xFF101820.toInt())
        }
        root.addView(TextView(this).apply {
            text = "PELOPS"; textSize = 26f; setTextColor(0xFFB9F2FF.toInt()); gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Asistente personal · prototipo"; textSize = 13f; setTextColor(0xFFB8C5CC.toInt())
            gravity = Gravity.CENTER; setPadding(0, 4, 0, 16)
        })
        history = TextView(this).apply {
            textSize = 16f; setTextColor(0xFFF3F6F8.toInt()); setPadding(12, 12, 12, 12)
            text = prefs.getString("history", "Pelops: Estoy disponible. El historial se guarda en este teléfono.\n\n") ?: ""
        }
        root.addView(ScrollView(this).apply { addView(history) }, LinearLayout.LayoutParams(-1, 0, 1f))
        input = EditText(this).apply {
            hint = "Escribe tu mensaje..."; setTextColor(-1); setHintTextColor(0xFFB8C5CC.toInt())
            setSingleLine(true); imeOptions = EditorInfo.IME_ACTION_SEND
            setOnEditorActionListener { _, action, _ -> if (action == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false }
        }
        root.addView(input, LinearLayout.LayoutParams(-1, -2))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        row.addView(button("Enviar") { sendMessage() }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(button("Hablar") { startVoiceInput() }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(button("Leer") { tts?.speak(history.text.toString().takeLast(3000), TextToSpeech.QUEUE_FLUSH, null, "pelops-read") }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(row)
        root.addView(button("Activar burbuja flotante") { enableBubble() })
        setContentView(root)
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; setOnClickListener { action() } }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        if (message.isEmpty()) return
        append("Tú: $message"); input.setText("")
        val reply = localReply(message)
        append("Pelops: $reply")
        tts?.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "pelops-reply")
    }

    private fun append(line: String) {
        history.append("$line\n\n")
        prefs.edit().putString("history", history.text.toString().takeLast(16000)).apply()
    }

    private fun localReply(message: String): String {
        val q = message.lowercase(Locale.ROOT)
        return when {
            q.contains("hola") || q.contains("buenas") -> "Hola. Estoy disponible. Esta versión guarda la conversación y permite usar voz; todavía no está conectada a una IA en la nube."
            q.contains("ayuda") || q.contains("puedes hacer") -> "Puedo guardar el historial local, recibir dictado y leer respuestas en voz alta. La conexión a IA y las acciones automáticas todavía no están configuradas."
            q.contains("memoria") || q.contains("recordar") -> "El historial se guarda en este teléfono. La memoria entre dispositivos y la IA en la nube todavía no están configuradas."
            else -> "Recibí tu mensaje. Este es el prototipo local de Pelops; aún no tengo conectada una IA en la nube para responder con conocimiento general."
        }
    }

    private fun startVoiceInput() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla con Pelops")
            }
            startActivityForResult(intent, voiceRequest)
        } catch (_: Exception) {
            Toast.makeText(this, "El reconocimiento de voz no está disponible.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Legacy callback required for speech recognition")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == voiceRequest && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) { input.setText(spoken); sendMessage() }
        }
    }

    private fun enableBubble() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Autoriza mostrar sobre otras apps y vuelve a pulsar el botón.", Toast.LENGTH_LONG).show()
            return
        }
        startService(Intent(this, FloatingBubbleService::class.java))
        Toast.makeText(this, "Burbuja activada. Tócala para volver a Pelops.", Toast.LENGTH_SHORT).show()
    }

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale("es", "AR") }
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); super.onDestroy() }
}
