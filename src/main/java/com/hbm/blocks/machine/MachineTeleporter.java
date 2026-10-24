package com.hbm.blocks.machine;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModRadarUtilityBlockEntities;
import net.minecraft.world.level.BlockGetter;
import com.hbm.tileentity.machine.TileEntityMachineTeleporter;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 1.7 {@code MachineTeleporter}: a plain cube ({@code teleporter_top/_bottom/_side}) that sends whatever stands
 * on it to the exit its telelinker gave it, for a million HE a trip. Looking at it shows the charge and exit.
 */
public class MachineTeleporter extends CompatBlock implements TickingBlock, ILookOverlay, ICapabilityBlock, IFluidConnectorBlockMK2 {

    private static final MachineCaps POWER = MachineCaps.of(ModRadarUtilityBlockEntities.TELEPORTER).powerIn();
    private static final MachineCaps CROSSING_FUEL = POWER.fluidIn();

    @Override public MachineCaps caps() { return TileEntityMachineTeleporter.hasCrossingFuel() ? CROSSING_FUEL : POWER; }

    @Override
    public boolean canConnect(FluidType type, BlockGetter level, int x, int y, int z, ForgeDirection direction) {
        if (!(level.getBlockEntity(new BlockPos(x, y, z)) instanceof TileEntityMachineTeleporter tele)) return false;
        var tank = tele.getCrossingTank();
        return tank != null && (type == Fluids.NONE || tank.joins(type));
    }

    public MachineTeleporter(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineTeleporter(pos, state);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityMachineTeleporter tele)) return null;

        List<String> text = new ArrayList<>();

        var target = tele.target;
        if (target == null) {
            text.add(ChatFormatting.RED + "No destination set!");
        } else {
            text.add((tele.power >= TileEntityMachineTeleporter.consumption ? ChatFormatting.GREEN : ChatFormatting.RED)
                    + String.format(Locale.US, "%,d", tele.power) + " / "
                    + String.format(Locale.US, "%,d", TileEntityMachineTeleporter.maxPower));
            var tank = tele.getCrossingTank();
            if (tank != null && !level.dimension().equals(target.dimension())) {
                text.add(tank.getTankType().getDisplayNameString() + ": " + tank.getFill() + "/" + tank.getMaxFill() + "mB");
            }
            text.add("Destination: " + target.pos().getX() + " / " + target.pos().getY() + " / " + target.pos().getZ()
                    + " (D: " + dev.golem.core.Ids.keyId(target.dimension()) + ")");
        }

        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
