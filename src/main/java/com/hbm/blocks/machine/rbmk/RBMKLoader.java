package com.hbm.blocks.machine.rbmk;

import com.hbm.blocks.ITooltipProvider;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code RBMKLoader} ("RBMK Steam Connector"): a plain block that lets a duct carrying the column's hot
 * output attach below the reactor. It connects heatable fluids from above and coolable fluids (or
 * perfluoromethyl) on every other face; the column itself pushes into the ducts around it.
 */
public class RBMKLoader extends CompatBlock implements IFluidConnectorBlockMK2, ITooltipProvider {

    public RBMKLoader(Properties properties) { super(properties); }

    @Override public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        if (dir == ForgeDirection.UP) return type.hasTrait(FT_Heatable.class);
        return type.hasTrait(FT_Coolable.class) || type == Fluids.PERFLUOROMETHYL;
    }

    /**
     * The 1.7 {@code getOutputPos} shared by the boiler and cooler columns: always the top of the column,
     * plus the four sides and the underside of a connector found one or two blocks below the core.
     * {@code belowWhenAbsent} is the outgasser's variant, which also offered its underside without a connector.
     */
    public static DirPos[] outputPositions(Level level, BlockPos core, boolean belowWhenAbsent) {
        int x = core.getX(), y = core.getY(), z = core.getZ();
        DirPos top = new DirPos(x, y + RBMKDials.getColumnHeight(level) + 1, z, Library.POS_Y);
        for (int depth = 1; depth <= 2; depth++) {
            if (level.getBlockState(core.below(depth)).getBlock() instanceof RBMKLoader) {
                int ly = y - depth;
                return new DirPos[]{top,
                        new DirPos(x + 1, ly, z, Library.POS_X), new DirPos(x - 1, ly, z, Library.NEG_X),
                        new DirPos(x, ly, z + 1, Library.POS_Z), new DirPos(x, ly, z - 1, Library.NEG_Z),
                        new DirPos(x, ly - 1, z, Library.NEG_Y)};
            }
        }
        return belowWhenAbsent ? new DirPos[]{top, new DirPos(x, y - 1, z, Library.NEG_Y)} : new DirPos[]{top};
    }
}
