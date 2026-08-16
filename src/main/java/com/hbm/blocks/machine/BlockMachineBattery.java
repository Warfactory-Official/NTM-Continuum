package com.hbm.blocks.machine;

import com.hbm.client.ILookOverlay;
import com.hbm.lib.Library;
import com.hbm.platform.Services;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A machine block backed by a {@link TileEntityMachineBattery}. Plain {@link Block} + {@link EntityBlock}
 * (not BaseEntityBlock) so it renders with its normal block model. Opens its menu on right-click via
 * the cross-loader platform service. Implements {@link ILookOverlay} to show its charge/throughput when
 * looked at.
 */
public class BlockMachineBattery extends dev.golem.core.world.CompatBlock implements EntityBlock, ILookOverlay {

    public BlockMachineBattery(Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineBattery(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.MACHINE_BATTERY.get(), TileEntityMachineBattery::serverTick);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
                Services.PLATFORM.openMenu(serverPlayer, provider, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    /** Look-overlay: charge / max, throughput (±HE/s), and a colour-coded fill percentage. */
    @Override
    public void printHook(GuiGraphicsExtractor graphics, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityMachineBattery battery)) return;

        List<String> text = new ArrayList<>();
        text.add(Library.getShortNumber(battery.power) + "/" + Library.getShortNumber(TileEntityMachineBattery.MAX_POWER) + " HE");
        if (battery.delta == 0) text.add("§e-- §r0HE/s");
        else if (battery.delta > 0) text.add("§a-> §r" + Library.getShortNumber(battery.delta) + "HE/s");
        else text.add("§c<- §r" + Library.getShortNumber(-battery.delta) + "HE/s");
        double frac = (double) battery.power / (double) TileEntityMachineBattery.MAX_POWER;
        text.add("&[" + Library.getColorProgress(frac) + "&]    " + Library.getPercentage(frac) + "%");

        ILookOverlay.printGeneric(graphics, this.getName().getString(), 0xffff00, 0x404000, text);
    }
}
