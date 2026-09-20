package com.hbm.entity.item;

import dev.golem.core.data.Tags;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;


/**
 * 1.7 {@code entity/item/EntityFireworks}: the shell the fireworks box lobs.
 *
 * <p>26.2: 1.7's burst was a hand-rolled {@code ParticleLetter} plus 50 {@code EntityFireworkSparkFX}, sent
 * over an aux packet. Vanilla now models exactly this - {@code Level.createFireworks} with a coloured
 * {@code FireworkExplosion} - so the burst rides that instead of a bespoke particle and packet pair.
 * The glyph half of 1.7's effect (the box could spell out a character) has no vanilla analogue and is the
 * one thing not carried over.
 */
public class EntityFireworks extends CompatEntity {

    private int color;
    private int character;

    public EntityFireworks(EntityType<? extends EntityFireworks> type, Level level) {
        super(type, level);
    }

    public EntityFireworks(Level level, double x, double y, double z, int color, int character) {
        this(ModNtmEntities.FIREWORK_BALL.get(), level);
        this.snapTo(x, y, z, 0.0F, 0.0F);
        this.color = color;
        this.character = character;
    }

    public int getColor() {
        return color;
    }

    public int getCharacter() {
        return character;
    }

    @Override
    protected void defineData(SynchedData builder) {
    }

    // 1.7 left Entity's default: not damageable
    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        // 1.7 moveEntity(0, 3, 0): a flat climb, no gravity, but it does collide - a ceiling stops the shell
        move(MoverType.SELF, new Vec3(0.0D, 3.0D, 0.0D));

        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.FLAME,
                    getX(), getY(), getZ(), 0.0D, -0.3D, 0.0D);
            level().addParticle(ParticleTypes.SMOKE,
                    getX(), getY(), getZ(), 0.0D, -0.2D, 0.0D);
            return;
        }

        // 1.7: ticksExisted advanced twice a server tick - World's own bump plus onUpdate's - so its > 30 fired
        // on tick 16. tickCount advances once, so the same climb is 16 moves, not 31.
        if (tickCount < 16) return;

        level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.AMBIENT, 20F, 1F + random.nextFloat() * 0.2F);

        dev.golem.core.world.Compat.fireworkBurst(level(), getX(), getY(), getZ(), color);

        discard();
    }

    @Override
    protected void loadData(DataIn input) {
        character = input.getIntOr("char", 0);
        color = input.getIntOr("color", 0);
        tickCount = input.getIntOr("ticksExisted", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("char", character);
        output.putInt("color", color);
        output.putInt("ticksExisted", tickCount);
    }
}
