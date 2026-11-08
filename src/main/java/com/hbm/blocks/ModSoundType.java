package com.hbm.blocks;

import java.util.Random;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;

/**
 * 1.7 {@code ModSoundType.ModEnvelopedSoundType}: the pitch is computed per play. Vanilla reads the sound event
 * before volume and pitch, so the getter called last names the sound being played (1.7's own hack).
 */
public class ModSoundType extends SoundType {

    private final Random random = new Random();
    private final Envelope pitchEnvelope;
    private SubType probableSubType = SubType.PLACE;

    public ModSoundType(float volume, float pitch, SoundEvent breakSound, SoundEvent stepSound, SoundEvent placeSound,
                        SoundEvent hitSound, SoundEvent fallSound, Envelope pitchEnvelope) {
        super(volume, pitch, breakSound, stepSound, placeSound, hitSound, fallSound);
        this.pitchEnvelope = pitchEnvelope;
    }

    @Override
    public SoundEvent getPlaceSound() {
        probableSubType = SubType.PLACE;
        return super.getPlaceSound();
    }

    @Override
    public SoundEvent getBreakSound() {
        probableSubType = SubType.BREAK;
        return super.getBreakSound();
    }

    // 1.7 plays the step sound for steps, mining hits and falls alike
    @Override
    public SoundEvent getStepSound() {
        probableSubType = SubType.STEP;
        return super.getStepSound();
    }

    @Override
    public SoundEvent getHitSound() {
        probableSubType = SubType.STEP;
        return super.getHitSound();
    }

    @Override
    public SoundEvent getFallSound() {
        probableSubType = SubType.STEP;
        return super.getFallSound();
    }

    @Override
    public float getPitch() {
        return pitchEnvelope.compute(super.getPitch(), random, probableSubType);
    }

    @FunctionalInterface
    public interface Envelope {
        float compute(float in, Random rand, SubType type);
    }

    public enum SubType {
        PLACE, BREAK, STEP
    }
}
