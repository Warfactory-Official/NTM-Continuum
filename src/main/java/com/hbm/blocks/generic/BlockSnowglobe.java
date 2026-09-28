package com.hbm.blocks.generic;

import com.hbm.tileentity.deco.TileEntitySnowglobe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * 1.7 {@code BlockSnowglobe}: glass material, hardness 0, an 8x5x8-pixel box; a right-click opens
 * {@code GUIScreenSnowglobe} on the client. The creative list skips {@link SnowglobeType#NONE}.
 */
public class BlockSnowglobe extends BlockTrinket {

    /** Client hook: opens {@code GUIScreenSnowglobe}. Installed by the client registrations. */
    public static Consumer<TileEntitySnowglobe> OPEN_SCREEN = globe -> {};

    private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 5.0D, 12.0D);

    public BlockSnowglobe(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySnowglobe(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntitySnowglobe globe) {
            OPEN_SCREEN.accept(globe);
        }
        return InteractionResult.SUCCESS;
    }

    public enum SnowglobeType {
        NONE("NONE", null),
        RIVETCITY("Rivet City", "Welcome to Rivet City. Please wait while the bridge extends."),
        TENPENNYTOWER("Tenpenny Tower", "Tenpenny Tower is the brainchild of Allistair Tenpenny, a British refugee who came to the Capital Wasteland seeking his fortune."),
        LUCKY38("Lucky 38", "My guess? Leads to a big cashout at some casino - and if the \"38\" on it is any indication... well... Lucky 38 it is."),
        SIERRAMADRE("Sierra Madre", "It's the moment you've been waiting for, the reason we're all here - the Gala Event, the Grand Opening of the Sierra Madre Casino."),
        PRYDWEN("The Prydwen", "People of the Commonwealth. Do not interfere. Our intentions are peaceful. We are the Brotherhood of Steel.");

        public static final SnowglobeType[] VALUES = values();

        public final String label;
        public final String inscription;

        SnowglobeType(String label, String inscription) {
            this.label = label;
            this.inscription = inscription;
        }

        public static SnowglobeType of(int ordinal) {
            return VALUES[Math.floorMod(ordinal, VALUES.length)];
        }
    }
}
