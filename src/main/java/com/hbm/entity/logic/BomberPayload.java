package com.hbm.entity.logic;

import com.hbm.api.registry.NamedRegistry;
import com.hbm.entity.projectile.EntityBombletZeta;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * What an {@link EntityBomber} drops: 1.7 {@code ItemBombCaller}'s eight payloads ({@code EntityBomber.type} 0..7),
 * as a registry of records keyed by name. The bomber saves the payload's name (a numeric {@code type} would break the
 * numeric-id rule, AGENTS.md section 8), so an addon's payload ({@code "ntmspace:airliner"}) loads
 * back as itself.
 *
 * <p>A payload is the bomber's factory (the 1.7 {@code statFac*}: run timing and airframe style) and its drop, the
 * branch of 1.7 {@code EntityBomber.onUpdate} that runs every {@code bombRate} ticks; HBM's eight are 1.7's literal
 * values. The designator item names the payload ({@code ItemBombCaller}), so a new payload is a new designator item
 * and one {@link #register} call at mod construction.
 */
public record BomberPayload(String name, Factory factory, Drop drop) {

    /** 1.7 {@code statFac*}: a configured bomber flying at the target; not yet in the world. */
    @FunctionalInterface
    public interface Factory {
        EntityBomber create(Level level, double x, double y, double z);
    }

    /** One drop of the payload (1.7 {@code EntityBomber.onUpdate}'s per-type branch), on the server. */
    @FunctionalInterface
    public interface Drop {
        void drop(EntityBomber bomber);
    }

    public static final NamedRegistry<BomberPayload> REGISTRY = new NamedRegistry<>("bomber payload", BomberPayload::name);

    // 1.7 type 0..7. The statFac* values are 1.7's.
    public static final BomberPayload CARPET = register("carpet",
            (level, x, y, z) -> EntityBomber.create(level, x, y, z, 50, 100, 2, "carpet"),
            bomber -> bomber.dropBomblet(EntityBombletZeta.HE, true));
    public static final BomberPayload NAPALM = register("napalm",
            (level, x, y, z) -> EntityBomber.create(level, x, y, z, 50, 100, 5, "napalm"),
            bomber -> bomber.dropBomblet(EntityBombletZeta.INCENDIARY, false));
    public static final BomberPayload CHLORINE = register("chlorine",
            (level, x, y, z) -> EntityBomber.create(level, x, y, z, 50, 100, 4, "chlorine"),
            bomber -> bomber.dropBomblet(EntityBombletZeta.CHLORINE, false));
    public static final BomberPayload ORANGE = register("orange",
            (level, x, y, z) -> EntityBomber.create(level, x, y, z, 75, 125, 1, "orange"),
            bomber -> {
                bomber.fizz();
                ExplosionChaos.spawnPoisonCloud(bomber.level(), bomber.getX(), bomber.getY() - 1F, bomber.getZ(), 10, 0.5D, 3);
            });
    // 1.7: these four overwrite the style fac() rolled: the payload picks the airframe, not the random table
    public static final BomberPayload ABOMB = register("abomb",
            (level, x, y, z) -> {
                EntityBomber bomber = EntityBomber.create(level, x, y, z, 60, 70, 65, "abomb");
                int style = switch (level.getRandom().nextInt(3)) {
                    case 0 -> 5;
                    case 1 -> 6;
                    default -> 7;
                };
                if (level.getRandom().nextInt(100) == 0) style = 8;
                bomber.setStyle(style);
                return bomber;
            },
            bomber -> bomber.dropBomblet(EntityBombletZeta.NUKE, false));
    public static final BomberPayload STINGER = register("stinger",
            (level, x, y, z) -> EntityBomber.create(level, x, y, z, 50, 150, 10, "stinger").setStyle(4),
            bomber -> {
                // 1.7: VT-stinger payload branch, empty
            });
    public static final BomberPayload BOXCAR = register("boxcar",
            (level, x, y, z) -> EntityBomber.create(level, x, y, z, 50, 150, 10, "boxcar").setStyle(6),
            bomber -> {
                bomber.level().playSound(null, bomber.getX() + 0.5F, bomber.getY() + 0.5F, bomber.getZ() + 0.5F,
                        ModSounds.MISSILE_TAKE_OFF.get(), SoundSource.PLAYERS, 10F, 0.9F + bomber.level().getRandom().nextFloat() * 0.2F);
                bomber.dropBoxcar();
            });
    public static final BomberPayload PC = register("pc",
            (level, x, y, z) -> EntityBomber.create(level, x, y, z, 75, 125, 1, "pc").setStyle(6),
            bomber -> {
                bomber.fizz();
                Level level = bomber.level();
                ExplosionChaos.spawnPoisonCloud(level, bomber.getX(),
                        level.getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(bomber.getX()), Mth.floor(bomber.getZ())) + 2,
                        bomber.getZ(), 10, 1D, 2);
            });

    /**
     * The payloads in 1.7's {@code type} order 0..7. Only to name what an old numeric value meant in a log line; a
     * numeric value is never read back as a payload.
     */
    static final String[] LEGACY_ORDER = {"carpet", "napalm", "chlorine", "orange", "abomb", "stinger", "boxcar", "pc"};

    /** Registers a payload. HBM's names are bare ({@code "carpet"}); an addon uses {@code "<namespace>:<path>"}. */
    public static BomberPayload register(String name, Factory factory, Drop drop) {
        return REGISTRY.register(new BomberPayload(name, factory, drop));
    }

    /** The payload saved as {@code name}, or null. */
    public static BomberPayload byName(String name) {
        return REGISTRY.get(name);
    }

    /** A configured bomber with this payload, flying at (x, y, z); add it to the world yourself. */
    public EntityBomber create(Level level, double x, double y, double z) {
        return factory.create(level, x, y, z);
    }
}
