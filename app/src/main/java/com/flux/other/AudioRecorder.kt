package com.flux.other

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var tempFile: File? = null
    private var isRecording = false

    fun hasMicPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Start recording into a temp cache file
    fun startRecording() {
        if (isRecording) return
        tempFile = File(
            context.cacheDir,
            "temp_audio_${System.currentTimeMillis()}.m4a"
        )

        val mediaRecorder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

        recorder = mediaRecorder.apply {

            setAudioSource(MediaRecorder.AudioSource.MIC)

            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)

            setOutputFile(tempFile!!.absolutePath)

            prepare()
            start()
        }

        isRecording = true
    }

    // Stop recording and return temp file
    fun stopRecording(): File? {
        if (!isRecording) return null

        return try {

            recorder?.apply {
                stop()
                release()
            }

            val file = tempFile

            recorder = null
            tempFile = null
            isRecording = false

            file

        } catch (_: Exception) {

            // Cleanup corrupted file
            tempFile?.delete()

            recorder = null
            tempFile = null
            isRecording = false

            null
        }
    }

    // Delete temp recording without saving
    fun deleteRecording() {

        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
            // Ignore stop failure if not started
        }

        tempFile?.delete()

        recorder = null
        tempFile = null
        isRecording = false
    }

    // Release resources (lifecycle safety)
    fun release() {
        try {
            recorder?.release()
        } catch (_: Exception) {}

        recorder = null
        tempFile = null
        isRecording = false
    }

    // For waveform UI
    fun getAmplitude(): Int {
        return recorder?.maxAmplitude ?: 0
    }
}