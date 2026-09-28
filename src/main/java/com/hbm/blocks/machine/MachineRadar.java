package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * 1.7 {@code MachineRadar}: a renderer-drawn single block (the dish spins on top of it) that opens the radar map
 * on a plain right-click and puts out redstone on all six sides, strong and weak alike.
 *
 * <p>Below {@link TileEntityMachineRadarNT#radarAltitude} the click only complains. 1.7 printed that from the
 * client; the message is sent by the server here, which is the same line in the same chat.
 *
 * <p>The radar map is a screen with no menu, opened on the client exactly as 1.7's GUI id 0 was. Common code
 * cannot name a client screen, so the client registrations install {@link #OPEN_SCREEN}.
 */
public class MachineRadar extends CompatBlock implements TickingBlock {

    /** Opens the radar map for a radar core; installed by the client registrations. */
    public static Consumer<TileEntityMachineRadarNT> OPEN_SCREEN = radar -> {};

    public MachineRadar(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineRadarNT(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        return use(level, pos, pos, player);
    }

    /** The shared 1.7 {@code onBlockActivated} of both radars; {@code core} is the radar's own block. */
    static InteractionResult use(Level level, BlockPos clicked, BlockPos core, Player player) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (clicked.getY() < TileEntityMachineRadarNT.radarAltitude) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.literal("[Radar] Error: Radar altitude not sufficient.")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide() && level.getBlockEntity(core) instanceof TileEntityMachineRadarNT radar) {
            OPEN_SCREEN.accept(radar);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof TileEntityMachineRadarNT radar ? radar.getRedPower() : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }
}
