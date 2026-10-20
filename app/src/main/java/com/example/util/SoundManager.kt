package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock
import com.example.R

enum class SoundEffect {
    CLICK,
    SAVE,
    EDIT,
    DELETE
}

object SoundManager {
    private var soundPool: SoundPool? = null
    private var clickSoundId: Int = 0
    private var saveSoundId: Int = 0
    private var editSoundId: Int = 0
    private var deleteSoundId: Int = 0
    private var isInitialized: Boolean = false
    private var lastClickTimestampMs: Long = 0L

    @Volatile
    var isSoundEnabled: Boolean = true

    private val _playedEffects = mutableListOf<SoundEffect>()
    val playedEffects: List<SoundEffect>
        get() = synchronized(_playedEffects) { _playedEffects.toList() }

    val clickCount: Int
        get() = synchronized(_playedEffects) { _playedEffects.count { it == SoundEffect.CLICK } }

    val saveCount: Int
        get() = synchronized(_playedEffects) { _playedEffects.count { it == SoundEffect.SAVE } }

    val editCount: Int
        get() = synchronized(_playedEffects) { _playedEffects.count { it == SoundEffect.EDIT } }

    val deleteCount: Int
        get() = synchronized(_playedEffects) { _playedEffects.count { it == SoundEffect.DELETE } }

    fun clearRecordedEffects() {
        synchronized(_playedEffects) {
            _playedEffects.clear()
        }
        lastClickTimestampMs = 0L
    }

    @Synchronized
    fun init(context: Context) {
        if (isInitialized && soundPool != null) return
        val appContext = context.applicationContext
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        clickSoundId = pool.load(appContext, R.raw.click, 1)
        saveSoundId = pool.load(appContext, R.raw.save, 1)
        editSoundId = pool.load(appContext, R.raw.edit, 1)
        deleteSoundId = pool.load(appContext, R.raw.delete, 1)

        soundPool = pool
        isInitialized = true
    }

    fun playClick(debounceMs: Long = 0L): Boolean {
        if (!isSoundEnabled) return false
        if (debounceMs > 0L) {
            val now = SystemClock.uptimeMillis()
            if (now - lastClickTimestampMs < debounceMs) {
                return false
            }
            lastClickTimestampMs = now
        }
        return playEffect(SoundEffect.CLICK, clickSoundId)
    }

    fun playSave(): Boolean = playEffect(SoundEffect.SAVE, saveSoundId)

    fun playEdit(): Boolean = playEffect(SoundEffect.EDIT, editSoundId)

    fun playDelete(): Boolean = playEffect(SoundEffect.DELETE, deleteSoundId)

    private fun playEffect(effect: SoundEffect, soundId: Int): Boolean {
        if (!isSoundEnabled) return false
        synchronized(_playedEffects) {
            _playedEffects.add(effect)
        }
        val pool = soundPool
        if (pool != null && soundId != 0) {
            pool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
        return true
    }

    @Synchronized
    fun release() {
        soundPool?.release()
        soundPool = null
        clickSoundId = 0
        saveSoundId = 0
        editSoundId = 0
        deleteSoundId = 0
        isInitialized = false
    }
}
