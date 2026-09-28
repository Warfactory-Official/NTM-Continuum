package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.blocks.ModSecurityBlocks;
import com.hbm.lib.ModDamageSources;
import com.hbm.sound.ModSecuritySounds;
import com.hbm.tileentity.deco.TileEntityTrappedBrick;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code TrappedBrick}, the trapped enargite brick. 1.7 kept the fifteen traps as the metadata of one block;
 * the port flattens them the usual way, {@code brick_jungle_trap_<trap>}, one block per {@link Trap}.
 *
 * <p>The {@link TrapType#ON_STEP} traps spring when a player walks on the brick ({@link #stepOn}), the
 * {@link TrapType#DETECTOR} traps from {@link TileEntityTrappedBrick}'s detection box; either way the brick then
 * clicks and turns into a plain enargite brick. 1.7's enum spells {@code MAGIC_CONVERSTION} with the typo, and
 * the registry id keeps it ({@code brick_jungle_trap_magic_converstion}) so the name is the original's.
 */
public class TrappedBrick extends CompatBlock {

    public enum TrapType {
        ON_STEP,
        DETECTOR
    }

    public enum Trap {
        FALLING_ROCKS(TrapType.DETECTOR),
        FIRE(TrapType.ON_STEP),
        ARROW(TrapType.DETECTOR),
        SPIKES(TrapType.ON_STEP),
        MINE(TrapType.ON_STEP),
        WEB(TrapType.ON_STEP),
        FLAMING_ARROW(TrapType.DETECTOR),
        PILLAR(TrapType.DETECTOR),
        RAD_CONVERSION(TrapType.ON_STEP),
        MAGIC_CONVERSTION(TrapType.ON_STEP),
        SLOWNESS(TrapType.ON_STEP),
        WEAKNESS(TrapType.ON_STEP),
        POISON_DART(TrapType.DETECTOR),
        ZOMBIE(TrapType.DETECTOR),
        SPIDERS(TrapType.DETECTOR);

        public final TrapType type;

        Trap(TrapType type) {
            this.type = type;
        }

        public static Trap get(int i) {
            if (i >= 0 && i < values().length) return values()[i];
            return FIRE;
        }

        public String id() {
            return "brick_jungle_trap_" + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public final Trap trap;

    public TrappedBrick(Properties properties, Trap trap) {
        super(properties);
        this.trap = trap;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide() || trap.type != TrapType.ON_STEP || !(entity instanceof Player player)) return;
        ServerLevel server = (ServerLevel) level;
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        BlockPos above = pos.above();

        switch (trap) {
            case FIRE -> {
                if (level.getBlockState(above).canBeReplaced()) level.setBlockAndUpdate(above, BaseFireBlock.getState(level, above));
            }
            case SPIKES -> {
                if (level.getBlockState(above).canBeReplaced())
                    level.setBlockAndUpdate(above, ModSecurityBlocks.spikes.get().defaultBlockState());
                for (Entity e : level.getEntitiesOfClass(Entity.class, new AABB(x, y + 1, z, x + 1, y + 2, z + 1))) {
                    Compat.hurt(e, server, ModDamageSources.source(level, ModDamageSources.SPIKES), 10);
                }
                level.playSound(null, x + 0.5, y + 1.5, z + 0.5, ModSecuritySounds.SLICER.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            // 1.7 createExplosion(null, ..., 1F, false): a small block-breaking blast, no fire
            case MINE -> level.explode(null, x + 0.5, y + 1.5, z + 0.5, 1F, false, Level.ExplosionInteraction.BLOCK);
            case WEB -> {
                if (level.getBlockState(above).canBeReplaced()) level.setBlockAndUpdate(above, Blocks.COBWEB.defaultBlockState());
            }
            case RAD_CONVERSION -> convert(level, pos, ModDungeonBlocks.brick_jungle_ooze.get());
            case MAGIC_CONVERSTION -> convert(level, pos, ModDungeonBlocks.brick_jungle_mystic.get());
            case SLOWNESS -> Effects.add(player, MobEffects.SLOWNESS, 300, 2);
            case WEAKNESS -> Effects.add(player, MobEffects.WEAKNESS, 300, 2);
            default -> { }
        }

        spring(level, pos);
    }

    /** 1.7: every other plain, cracked or magmatic brick within a 7-cube turns into {@code into}, a coin flip each. */
    private static void convert(Level level, BlockPos pos, Block into) {
        for (int a = -3; a <= 3; a++) {
            for (int b = -3; b <= 3; b++) {
                for (int c = -3; c <= 3; c++) {
                    if (level.getRandom().nextBoolean()) continue;
                    BlockPos at = pos.offset(a, b, c);
                    Block bl = level.getBlockState(at).getBlock();
                    if (bl == ModBlocks.brick_jungle.get() || bl == ModBlocks.brick_jungle_cracked.get()
                            || bl == ModDungeonBlocks.brick_jungle_lava.get()) {
                        level.setBlockAndUpdate(at, into.defaultBlockState());
                    }
                }
            }
        }
    }

    /** 1.7: {@code random.click} at 0.3 / 0.6, then the brick becomes a plain enargite brick. */
    public static void spring(Level level, BlockPos pos) {
        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3F, 0.6F);
        level.setBlockAndUpdate(pos, ModBlocks.brick_jungle.get().defaultBlockState());
    }

    /** The detector traps: 1.7 {@code createNewTileEntity} answered a {@link TileEntityTrappedBrick} for these only. */
    public static class Detector extends TrappedBrick implements TickingBlock {

        public Detector(Properties properties, Trap trap) {
            super(properties, trap);
        }

        @Override
        public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new TileEntityTrappedBrick(pos, state);
        }
    }
}
