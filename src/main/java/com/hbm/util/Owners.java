package com.hbm.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.BlockPlaces;
import dev.golem.core.world.Protection;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Responsible player of a device, entity or block. Stored as a bare UUID so an offline owner still resolves;
 * null = environment or unknown, never guessed.
 */
public final class Owners {

    /** NBT key shared by every owner-carrying entity and block entity. */
    public static final String KEY = "detonator";

    private Owners() {}

    /** Entity that stores an owner; also resolved by {@link Protection.Source#of}. */
    public interface Carrier extends Protection.Owned {
        void setProtectionOwner(@Nullable UUID owner);
    }

    /** Block entity remembering the player who placed it; set by the placement hook. */
    public interface Placed {
        @Nullable UUID placer();

        void setPlacer(@Nullable UUID placer);

        /** Allocation-free for hot loops when the holder caches it. */
        default Protection.Source placerSource() {
            return Protection.Source.ofPlayer(placer());
        }
    }

    /** Stamps the placing player on every {@link Placed} block entity. */
    public static void init() {
        BlockPlaces.onPlace((placer, level, pos, state) -> {
            if (placer instanceof Player player && level.getBlockEntity(pos) instanceof Placed placed) {
                placed.setPlacer(player.getUUID());
            }
        });
    }

    private static final ThreadLocal<@Nullable UUID> SCOPE = new ThreadLocal<>();

    /** Owner of the bomb being detonated on this thread; {@code null} keeps the enclosing one. Close restores it. */
    public static Scope scope(@Nullable UUID owner) {
        UUID previous = SCOPE.get();
        if (owner != null) SCOPE.set(owner);
        return () -> {
            if (previous == null) SCOPE.remove();
            else SCOPE.set(previous);
        };
    }

    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    /** The detonating player, else the scoped owner of a deferred detonation. Bomb blocks resolve through this. */
    public static @Nullable UUID detonator(@Nullable Entity detonator) {
        UUID id = of(detonator);
        return id != null ? id : SCOPE.get();
    }

    public static Protection.Source sourceOf(@Nullable Entity detonator) {
        return Protection.Source.ofPlayer(detonator(detonator));
    }

    public static @Nullable UUID of(@Nullable Entity entity) {
        return Protection.Source.of(entity).player();
    }

    public static Protection.Source source(@Nullable BlockEntity be) {
        return be instanceof Placed placed ? placed.placerSource() : Protection.Source.NONE;
    }

    /** The detonating player, else the placer of the block entity at {@code pos}; read before the block is removed. */
    public static Protection.Source source(@Nullable Entity detonator, Level level,
                                           BlockPos pos) {
        Protection.Source source = sourceOf(detonator);
        return source.player() != null ? source : source(level.getBlockEntity(pos));
    }

    /** Stamps {@code owner} on a freshly built entity, if it carries one; returns it for chaining. */
    public static <E extends Entity> E give(E entity, @Nullable UUID owner) {
        if (owner != null && entity instanceof Carrier carrier) carrier.setProtectionOwner(owner);
        return entity;
    }

    /** {@link #give} with the owner of {@code parent}. */
    public static <E extends Entity> E inherit(E entity, @Nullable Entity parent) {
        return give(entity, of(parent));
    }

    public static void save(DataOut output, @Nullable UUID owner) {
        if (owner != null) output.store(KEY, UUIDUtil.CODEC, owner);
    }

    public static @Nullable UUID load(DataIn input) {
        return input.read(KEY, UUIDUtil.CODEC).orElse(null);
    }
}
