package com.hbm.blocks.generic;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.deco.TileEntityPlushie;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockPlushie}: cloth, hardness 0, full-cube bounds. A right-click squishes it: the client sets an
 * 11-tick squish timer the renderer turns into a wobble, the server plays the squeaky toy at 0.25 - or, for
 * Hundun, the magnificent howl at volume 100. The creative list skips {@link PlushieType#NONE}.
 */
public class BlockPlushie extends BlockTrinket {

    public BlockPlushie(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPlushie(pos, state);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityPlushie plushie)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) {
            plushie.squishTimer = 11;
        } else if (plushie.type() == PlushieType.HUNDUN) {
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModSounds.HUNDUNS_MAGNIFICENT_HOWL.get(), SoundSource.BLOCKS, squishVolume(plushie.type()), 1F);
        } else {
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModSounds.SQUEAKY_TOY.get(), SoundSource.BLOCKS, squishVolume(plushie.type()), 1F);
        }
        return InteractionResult.SUCCESS;
    }

    /** The base mod's loud Hundun; forks may supply their authored squish volume. */
    protected float squishVolume(PlushieType type) {
        return type == PlushieType.HUNDUN ? 100F : 0.25F;
    }

    public boolean isPressable() {
        return false;
    }

    public SoundEvent pressSound() {
        return ModSounds.PRESS_OPERATE.get();
    }

    public interface Plushie {
        float scaleX();

        default float scaleY() {
            return scaleX();
        }

        default float scaleZ() {
            return scaleX();
        }
    }

    public enum PlushieType implements Plushie {
        NONE("NONE", null),
        YOMI("Yomi", "Hi! Can I be your rabbit friend?"),
        NUMBERNINE("Number Nine", "None of y'all deserve coal."),
        HUNDUN("Hundun", "混沌"),
        DERG("Dragon", "Squeeze him."); // blerg

        public static final PlushieType[] VALUES = values();

        public final String label;
        public final String inscription;

        PlushieType(String label, String inscription) {
            this.label = label;
            this.inscription = inscription;
        }

        public static PlushieType of(int ordinal) {
            return VALUES[Math.floorMod(ordinal, VALUES.length)];
        }

        @Override
        public float scaleX() {
            return switch (this) {
                case YOMI -> 0.5F;
                case NUMBERNINE -> 0.75F;
                default -> 1F;
            };
        }
    }
}
