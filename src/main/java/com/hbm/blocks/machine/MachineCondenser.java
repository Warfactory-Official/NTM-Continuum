package com.hbm.blocks.machine;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.util.BobMathUtil;
import dev.golem.core.registry.TickingBlock;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityCondenser;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Passive steam condenser; the look overlay lists both tanks, as the 1.12.2 {@code printHook} did. */
public class MachineCondenser extends CompatBlock implements TickingBlock, ILookOverlay {
    public MachineCondenser(Properties properties) { super(properties); }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileEntityCondenser(pos, state); }
        /** Look-overlay: green {@code ->} for the incoming spent steam, red {@code <-} for the water it sends. */
    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityCondenser condenser)) return null;

        List<String> text = new ArrayList<>();
        vacuumWarning(level, condenser, text);
        text.add("§a-> §r" + tankLine(condenser.input));
        text.add("§c<- §r" + tankLine(condenser.output));

        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    /**
     * hbm-space's {@code printHook} line for a condenser or cooling tower that is boiling off in a vacuum
     * ({@link TileEntityCondenser#evaporates}); never shown on plain HBM.
     */
    public static void vacuumWarning(Level level, TileEntityCondenser condenser, List<String> text) {
        if (!condenser.evaporates(level, condenser.getBlockPos())) return;
        text.add("&[" + (BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! "
                + Component.translatableWithFallback("atmosphere.noVacuum",
                        "REQUIRES ATMOSPHERE").getString() + " ! ! !");
    }

    private static String tankLine(FluidTankNTM tank) {
        return tank.getTankType().getDisplayNameString() + ": " + tank.getFill() + "/" + tank.getMaxFill() + "mB";
    }

}
