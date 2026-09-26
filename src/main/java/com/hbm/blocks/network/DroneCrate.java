package com.hbm.blocks.network;

import net.minecraft.core.Direction;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.fluidmk2.IFluidConnectorMK2;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.api.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.hbm.blocks.ModDroneContent;
import com.hbm.tileentity.network.TileEntityDroneCrate;

/** 1.7 {@code DroneCrate}: the transport drones' loading bay; right-click opens it unless the linker is held. */
public class DroneCrate extends CompatBlock implements TickingBlock, ILookOverlay, IFluidConnectorBlockMK2 {

    public DroneCrate(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDroneCrate(pos, state);
    }

    /** 1.7: the linker's own use wins over the GUI. */
    static boolean holdsLinker(Player player) {
        return player.getMainHandItem().getItem() == ModDroneContent.DRONE_LINKER.get()
                || player.getOffhandItem().getItem() == ModDroneContent.DRONE_LINKER.get();
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (holdsLinker(player)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return open(level, pos, player);
    }

    static InteractionResult open(Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            Menus.openWithPos(serverPlayer, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityDroneCrate tile) || tile.next == null) return null;
        List<String> text = new ArrayList<>();
        text.add("Next waypoint: " + tile.next.getX() + " / " + tile.next.getY() + " / " + tile.next.getZ());
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, Direction side) {
        ForgeDirection dir = ForgeDirection.of(side);
        return world.getBlockEntity(new BlockPos(x, y, z)) instanceof IFluidConnectorMK2 connector
                && connector.canConnect(type, dir.toDirection());
    }
}
