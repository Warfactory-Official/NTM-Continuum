package com.hbm.blocks.network;

import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.network.TileEntityDroneDock;
import com.hbm.tileentity.network.TileEntityDroneProvider;
import com.hbm.tileentity.network.TileEntityDroneRequester;

/**
 * 1.7 {@code DroneDock}: one block class serving the dock, the provider crate and the requester crate, told apart
 * by which registry object it is (here: an explicit type). Right-click opens the menu unless sneaking.
 */
public class DroneDock extends CompatBlock implements TickingBlock {

    public enum Type {
        DOCK,
        PROVIDER,
        REQUESTER
    }

    private final Type type;

    public DroneDock(Properties properties, Type type) {
        super(properties);
        this.type = type;
    }

    public Type type() {
        return type;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (type) {
            case DOCK -> new TileEntityDroneDock(pos, state);
            case PROVIDER -> new TileEntityDroneProvider(pos, state);
            case REQUESTER -> new TileEntityDroneRequester(pos, state);
        };
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        return DroneCrate.open(level, pos, player);
    }
}
