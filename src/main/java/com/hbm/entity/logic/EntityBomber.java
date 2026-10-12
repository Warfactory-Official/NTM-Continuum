package com.hbm.entity.logic;

import com.hbm.entity.ModNtmEntities;
import com.hbm.config.GeneralConfig;
import dev.golem.core.data.SynchedData;
import com.hbm.entity.projectile.EntityBombletZeta;
import com.hbm.entity.projectile.EntityBoxcar;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 EntityBomber.
 */
public class EntityBomber extends EntityPlaneBase {

    private static final EntityDataAccessor<Byte> STYLE =
            SynchedEntityData.defineId(EntityBomber.class, EntityDataSerializers.BYTE);
    /**
     * An addon's airframe by name (ADDON-API-INVENTORY H-72, C-25): empty for HBM's numbered styles; otherwise the
     * renderer draws the skin registered under this name ({@code RenderBomber.registerSkin}) and {@link #LOOP_SOUND}
     * may give it its own engine loop. Synced, saved as {@code skin}.
     */
    private static final EntityDataAccessor<String> SKIN =
            SynchedEntityData.defineId(EntityBomber.class, EntityDataSerializers.STRING);

    /** The engine loop a bomber plays (client); the first non-null answer wins, else 1.7's by style. HBM registers none. */
    @FunctionalInterface
    public interface LoopSound {
        net.minecraft.sounds.@org.jspecify.annotations.Nullable SoundEvent sound(EntityBomber bomber);
    }

    public static final dev.golem.core.hook.Hook<LoopSound> LOOP_SOUND = dev.golem.core.hook.Hook.notifying();

    private int bombStart = 75;
    private int bombStop = 125;
    private int bombRate = 3;
    /** The {@link BomberPayload} name, saved as such (1.7 saved the numeric {@code type}). */
    private String payload = BomberPayload.CARPET.name();
    private AudioWrapper audio;

    public EntityBomber(EntityType<? extends EntityBomber> type, Level level) {
        super(type, level);
    }

    public static EntityBomber statFacCarpet(Level level, double x, double y, double z) {
        return BomberPayload.CARPET.create(level, x, y, z);
    }

    public static EntityBomber statFacNapalm(Level level, double x, double y, double z) {
        return BomberPayload.NAPALM.create(level, x, y, z);
    }

    public static EntityBomber statFacChlorine(Level level, double x, double y, double z) {
        return BomberPayload.CHLORINE.create(level, x, y, z);
    }

    public static EntityBomber statFacOrange(Level level, double x, double y, double z) {
        return BomberPayload.ORANGE.create(level, x, y, z);
    }

    public static EntityBomber statFacABomb(Level level, double x, double y, double z) {
        return BomberPayload.ABOMB.create(level, x, y, z);
    }

    public static EntityBomber statFacStinger(Level level, double x, double y, double z) {
        return BomberPayload.STINGER.create(level, x, y, z);
    }

    public static EntityBomber statFacBoxcar(Level level, double x, double y, double z) {
        return BomberPayload.BOXCAR.create(level, x, y, z);
    }

    public static EntityBomber statFacPC(Level level, double x, double y, double z) {
        return BomberPayload.PC.create(level, x, y, z);
    }

    /**
     * 1.7 {@code statFac*}'s shared body: a 200-tick bomber that drops {@code payload} every {@code rate} ticks
     * between {@code start} and {@code stop}, approaching (x, y, z) from a random side ({@code fac}, which also rolls
     * the airframe style). A payload's factory may change the timer, style or motion afterwards.
     */
    public static EntityBomber create(Level level, double x, double y, double z, int start, int stop, int rate,
                                      String payload) {
        EntityBomber bomber = new EntityBomber(ModNtmEntities.BOMBER.get(), level);
        bomber.timer = 200;
        bomber.bombStart = start;
        bomber.bombStop = stop;
        bomber.bombRate = rate;
        bomber.fac(x, y, z);
        bomber.payload = payload;
        return bomber;
    }

    /** Overwrites the airframe {@code fac} rolled (1.7 {@code dataWatcher.updateObject(16, style)}). */
    public EntityBomber setStyle(int style) {
        entityData.set(STYLE, (byte) style);
        return this;
    }

    public String payload() {
        return payload;
    }

    /** The drop window and rate: {start, stop, rate} in ticks. */
    public int[] timing() {
        return new int[] {bombStart, bombStop, bombRate};
    }

    @Override
    protected void defineData(SynchedData builder) {
        super.defineData(builder);
        builder.define(STYLE, (byte) 0);
        builder.define(SKIN, "");
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        if (level().isClientSide()) {
            tickClientAudio();
            return;
        }
        if (health > 0 && tickCount > bombStart && tickCount < bombStop && tickCount % bombRate == 0) {
            bomb();
        }
    }

    private void tickClientAudio() {
        if (health > 0) {
            if (audio == null || !audio.isPlaying()) {
                net.minecraft.sounds.SoundEvent loop = null;
                for (LoopSound listener : LOOP_SOUND) {
                    loop = listener.sound(this);
                    if (loop != null) break;
                }
                if (loop == null) loop = getStyle() <= 4 ? ModSounds.ENTITY_BOMBER_SMALL_LOOP.get() : ModSounds.ENTITY_BOMBER_LOOP.get();
                audio = AudioSystem.getLoopedSound(
                        loop,
                        SoundSource.AMBIENT, (float) getX(), (float) getY(), (float) getZ(), 2F, 250F, 1F, 20);
                if (audio != null) audio.startSound();
            }
            if (audio != null) {
                audio.keepAlive();
                audio.updatePosition((float) getX(), (float) getY(), (float) getZ());
            }
        } else if (audio != null && audio.isPlaying()) {
            audio.stopSound();
            audio = null;
        }
    }

    private void bomb() {
        BomberPayload drop = BomberPayload.byName(payload);
        if (drop != null) drop.drop().drop(this);
    }

    /**
     * 1.7's bomblet branch (types 0, 1, 2 and 4): a whistle and one {@link EntityBombletZeta} of {@code kind}
     * leaving the bay; {@code scatter} adds the carpet bomber's sideways spread (1.7 {@code type == 0}).
     */
    public void dropBomblet(EntityBombletZeta.Kind kind, boolean scatter) {
        level().playSound(null, getX() + 0.5F, getY() + 0.5F, getZ() + 0.5F, ModSounds.BOMBER_WHISTLE.get(),
                SoundSource.PLAYERS,
                10F, 0.9F + random.nextFloat() * 0.2F);
        EntityBombletZeta zeta = new EntityBombletZeta(ModNtmEntities.BOMBLET_ZETA.get(), level());
        // 1.7 rotates FIRST, on a still-zeroed motion, so the bomblet leaves the bay at atan2(0, 0)
        zeta.rotation();
        zeta.setKind(kind);
        zeta.setPos(getX() + random.nextDouble() - 0.5D, getY() - random.nextDouble(),
                getZ() + random.nextDouble() - 0.5D);
        Vec3 motion = getDeltaMovement();
        zeta.setDeltaMovement(scatter ? motion.x + random.nextGaussian() * 0.15D : motion.x, motion.y,
                scatter ? motion.z + random.nextGaussian() * 0.15D : motion.z);
        level().addFreshEntity(zeta);
    }

    /** 1.7's boxcar branch (type 6): one falling {@link EntityBoxcar} out of the bay. */
    public void dropBoxcar() {
        EntityBoxcar rocket = new EntityBoxcar(ModNtmEntities.BOXCAR.get(), level());
        rocket.setPos(getX() + random.nextDouble() - 0.5D, getY() - random.nextDouble(),
                getZ() + random.nextDouble() - 0.5D);
        level().addFreshEntity(rocket);
    }

    /** 1.7's gas-release hiss (types 3 and 7). */
    public void fizz() {
        double x = getX(), y = getY(), z = getZ();
        level().playSound(null, x + 0.5D, y + 0.5D, z + 0.5D, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS,
                5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
    }

    private void fac(double x, double y, double z) {
        Vec3 vector = new Vec3(random.nextDouble() - 0.5D, 0D, random.nextDouble() - 0.5D).normalize();
        vector = vector.scale(GeneralConfig.enableBomberShortMode ? 1D : 2D);
        setPos(x - vector.x * 100D, y + 50D, z - vector.z * 100D);
        setDeltaMovement(vector.x, 0D, vector.z);
        rotation();
        forceSpawnChunk();

        int style = switch (random.nextInt(7)) {
            case 0, 1 -> 1;
            case 2, 3 -> 2;
            case 4 -> 5;
            case 5 -> 6;
            default -> 7;
        };
        if (random.nextInt(100) == 0) style = switch (random.nextInt(4)) {
            case 0 -> 0;
            case 1 -> 3;
            case 2 -> 4;
            default -> 8;
        };
        entityData.set(STYLE, (byte) style);
    }

    public int getStyle() {
        return entityData.get(STYLE);
    }

    /** Names the airframe (an addon's skin, see {@link #SKIN}); empty restores HBM's numbered style. */
    public EntityBomber setSkin(String skin) {
        entityData.set(SKIN, skin);
        return this;
    }

    public String getSkin() {
        return entityData.get(SKIN);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        bombStart = input.getIntOr("bombStart", 75);
        bombStop = input.getIntOr("bombStop", 125);
        bombRate = input.getIntOr("bombRate", 3);
        // the payload by name; a pre-2026-10-08 numeric type falls back to the carpet bomber with one WARN
        if (input.getIntOr("type", Integer.MIN_VALUE) != Integer.MIN_VALUE) com.hbm.util.SavedIds.warnNumeric("bomber payload", "type");
        String saved = input.getStringOr("payload", BomberPayload.CARPET.name());
        payload = BomberPayload.byName(saved) != null ? saved : BomberPayload.CARPET.name();
        entityData.set(STYLE, input.getByteOr("style", (byte) 0));
        entityData.set(SKIN, input.getStringOr("skin", ""));
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("bombStart", bombStart);
        output.putInt("bombStop", bombStop);
        output.putInt("bombRate", bombRate);
        output.putString("payload", payload);
        output.putByte("style", entityData.get(STYLE));
        if (!entityData.get(SKIN).isEmpty()) output.putString("skin", entityData.get(SKIN));
    }
}
