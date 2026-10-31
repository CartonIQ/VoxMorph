package com.cartoniq.voxmorph.domain

import androidx.annotation.StringRes
import com.cartoniq.voxmorph.R

enum class VoiceCategory { MALE, FEMALE, EFFECTS }
enum class SourceType { RECORDING, TTS }
enum class EffectType { NONE, ROBOT, ALIEN, GHOST, ECHO, MEGAPHONE, CHIPMUNK, MONSTER, DEEP, RADIO, TELEPHONE }

/** One immutable voice/effect profile. The catalog below is the single source of truth. */
data class VoiceProfile(
    val id: String,
    val category: VoiceCategory,
    val index: Int,
    @StringRes val nameRes: Int,
    val isFree: Boolean,
    val pitchSemitones: Float,
    /** Vocal-tract (formant) shift in semitones, independent of pitch. Negative = bigger/deeper voice. */
    val formantSemitones: Float,
    val tilt: Float,
    val effect: EffectType,
    val hue: Float,
    val glyph: String = ""
)

object VoiceCatalog {
    private val malePitch = floatArrayOf(-2f, -3f, -4f, -5f, -6f, -1f, -3.5f, -7f, -2.5f, -4.5f)
    private val maleFormant = floatArrayOf(-1.5f, -2f, -2.5f, -3.5f, -4f, -0.8f, -2.2f, -5f, -1.8f, -3f)
    private val maleTilt = floatArrayOf(0f, 0.2f, -0.2f, -0.4f, 0.3f, 0.4f, -0.1f, -0.5f, 0.1f, 0.5f)
    private val femalePitch = floatArrayOf(3f, 4f, 5f, 2f, 6f, 3.5f, 5.5f, 7f, 4.5f, 2.5f)
    private val femaleFormant = floatArrayOf(1.5f, 2f, 2.8f, 1f, 3.2f, 1.8f, 3f, 3.8f, 2.4f, 1.3f)
    private val femaleTilt = floatArrayOf(0f, 0.2f, 0.3f, -0.2f, 0.4f, 0.1f, 0.5f, 0.25f, -0.1f, 0.35f)

    val FREE_IDS = setOf("man_1", "woman_1", "robot")

    val all: List<VoiceProfile> = buildList {
        for (i in 1..10) add(
            VoiceProfile("man_$i", VoiceCategory.MALE, i, R.string.voice_man, "man_$i" in FREE_IDS,
                malePitch[i - 1], maleFormant[i - 1], maleTilt[i - 1], EffectType.NONE, 200f + i * 7f)
        )
        for (i in 1..10) add(
            VoiceProfile("woman_$i", VoiceCategory.FEMALE, i, R.string.voice_woman, "woman_$i" in FREE_IDS,
                femalePitch[i - 1], femaleFormant[i - 1], femaleTilt[i - 1], EffectType.NONE, 290f + i * 6f)
        )
        fun fx(id: String, name: Int, e: EffectType, pitch: Float, formant: Float, tilt: Float, hue: Float, glyph: String) =
            add(VoiceProfile(id, VoiceCategory.EFFECTS, 0, name, id in FREE_IDS, pitch, formant, tilt, e, hue, glyph))
        fx("robot", R.string.fx_robot, EffectType.ROBOT, 0f, 0f, 0.2f, 195f, "🤖")
        fx("alien", R.string.fx_alien, EffectType.ALIEN, 4f, 5f, 0.3f, 125f, "👽")
        fx("ghost", R.string.fx_ghost, EffectType.GHOST, -2f, -1f, -0.1f, 235f, "👻")
        fx("echo", R.string.fx_echo, EffectType.ECHO, 0f, 0f, 0f, 185f, "🔊")
        fx("megaphone", R.string.fx_megaphone, EffectType.MEGAPHONE, 0f, 0f, 0.2f, 30f, "📢")
        fx("chipmunk", R.string.fx_chipmunk, EffectType.CHIPMUNK, 9f, 8f, 0.4f, 25f, "🐿️")
        fx("monster", R.string.fx_monster, EffectType.MONSTER, -8f, -6f, -0.2f, 285f, "👹")
        fx("deep", R.string.fx_deep, EffectType.DEEP, -7f, -5f, -0.5f, 215f, "🐋")
        fx("radio", R.string.fx_radio, EffectType.RADIO, 0f, 0f, 0f, 200f, "📻")
        fx("telephone", R.string.fx_telephone, EffectType.TELEPHONE, 0f, 0f, 0f, 210f, "☎️")
    }

    private val byId = all.associateBy { it.id }

    fun get(id: String): VoiceProfile = byId[id] ?: all.first()
    fun of(category: VoiceCategory): List<VoiceProfile> = all.filter { it.category == category }
    fun defaultFree(category: VoiceCategory): VoiceProfile = when (category) {
        VoiceCategory.MALE -> get("man_1")
        VoiceCategory.FEMALE -> get("woman_1")
        VoiceCategory.EFFECTS -> get("robot")
    }
}
