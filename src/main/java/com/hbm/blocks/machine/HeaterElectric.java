package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityHeaterElectric;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Electric heating unit (1.12.2 {@code heater_electric}): a dummyable power→heat converter; a heat
 * consumer sits on its core. No GUI — the setting (0–10) is cycled by right-clicking (sneak = down; the
 * old screwdriver/hand-drill tools aren't ported), and the look-overlay shows heat/consumption/output.
 * Cables connect at the single rear power port.
 */
public class HeaterElectric extends BlockDummyable implements IEnergyConnectorBlock, ILookOverlay {

    public HeaterElectric(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHeaterElectric(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 2, 1, 1};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        this.makeExtra(world, x, y, z);
    }

    /** Cables connect only at the heater's rear power port. */
    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityHeaterElectric te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    /** Right-click cycles the heat setting up, sneak-right-click cycles it down (replaces IToolable). */
    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (findCoreTE(level, pos) instanceof TileEntityHeaterElectric te) {
                if (player.isShiftKeyDown()) te.toggleSettingDown();
                else te.toggleSettingUp();
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    sp.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Setting: " + te.setting + " (" + te.getHeatGen() + " TU/t, " + te.getConsumption() + " HE/t)"), true);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? createTickerHelper(type, ModBlockEntities.HEATER_ELECTRIC.get(), TileEntityHeaterElectric::clientTick)
                : createTickerHelper(type, ModBlockEntities.HEATER_ELECTRIC.get(), TileEntityHeaterElectric::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityHeaterElectric heater)) return null;

        List<String> text = new ArrayList<>();
        text.add(String.format("%,d", heater.heatEnergy) + " TU");
        text.add("§7Setting: " + heater.setting);
        text.add("§a-> §r" + heater.getConsumption() + " HE/t");
        text.add("§c<- §r" + heater.getHeatGen() + " TU/t");
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }
}
