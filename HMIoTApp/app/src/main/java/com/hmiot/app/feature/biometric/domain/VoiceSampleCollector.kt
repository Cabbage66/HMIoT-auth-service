package com.hmiot.app.feature.biometric.domain

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoiceSampleCollector {
    suspend fun collectBase64Wav(seconds: Int = 3, sampleRate: Int = 16000): String = withContext(Dispatchers.IO) {
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding)
        val bufferSize = maxOf(minBufferSize, sampleRate * 2)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioEncoding,
            bufferSize
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord init failed")
        }
        val totalBytes = sampleRate * seconds * 2
        val pcmData = ByteArray(totalBytes)
        var offset = 0
        audioRecord.startRecording()
        try {
            while (offset < totalBytes) {
                val read = audioRecord.read(pcmData, offset, totalBytes - offset)
                if (read > 0) offset += read
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
        val wavBytes = toWav(pcmData, sampleRate)
        Base64.encodeToString(wavBytes, Base64.NO_WRAP)
    }

    private fun toWav(pcm: ByteArray, sampleRate: Int): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val out = ByteArrayOutputStream()
        out.write("RIFF".toByteArray())
        out.write(intToLe(36 + pcm.size))
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        out.write(intToLe(16))
        out.write(shortToLe(1))
        out.write(shortToLe(channels.toShort()))
        out.write(intToLe(sampleRate))
        out.write(intToLe(byteRate))
        out.write(shortToLe((channels * bitsPerSample / 8).toShort()))
        out.write(shortToLe(bitsPerSample.toShort()))
        out.write("data".toByteArray())
        out.write(intToLe(pcm.size))
        out.write(pcm)
        return out.toByteArray()
    }

    private fun intToLe(value: Int): ByteArray = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    private fun shortToLe(value: Short): ByteArray = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
}
