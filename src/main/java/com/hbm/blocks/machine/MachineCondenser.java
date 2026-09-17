package com.hbm.blocks.machine;

import com.hbm.interfaces.ILookOverlay;
import dev.golem.core.registry.TickingBlock;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityCondenser;
import net.minecraft.core.BlockPos;
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
public class MachineCondenser extends dev.golem.core.world.CompatBlock implements TickingBlock, ILookOverlay {
    public MachineCondenser(Properties properties) { super(properties); }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TileEntityCondenser(pos, state); }
        /** Look-overlay: green {@code ->} for the incoming spent steam, red {@code <-} for the water it sends. */
    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityCondenser condenser)) return null;

        List<String> text = new ArrayList<>();
        text.add("§a-> §r" + tankLine(condenser.input));
        text.add("§c<- §r" + tankLine(condenser.output));

        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    private static String tankLine(FluidTankNTM tank) {
        return tank.getTankType().getDisplayNameString() + ": " + tank.getFill() + "/" + tank.getMaxFill() + "mB";
    }

}
