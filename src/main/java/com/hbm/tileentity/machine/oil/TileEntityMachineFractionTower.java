package com.hbm.tileentity.machine.oil;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FractionRecipes;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Fractioning Tower — 1:1 port of the 1.12.2 {@code TileEntityMachineFractionTower}: no power, no GUI,
 * no inventory. 3 tanks × 4000 (0 = feed, 1/2 = the two fractions). When another tower sits 3 blocks
 * above, its tank types are forced to match and feed moves up / fractions pull down (so a stack of
 * towers shares one column). {@link #fractionate()} runs every 10 ticks: −100 mB feed → +both fractions
 * if they fit. The input tank type is set by a fluid-identifier right-click on the bottom segment (the
 * old onBlockActivated, in {@link com.hbm.blocks.machine.MachineFractionTower}).
 */
public class TileEntityMachineFractionTower extends TileEntityLoadedBase implements IFluidStandardTransceiverMK2 {

    public FluidTankNTM[] tanks;

    public TileEntityMachineFractionTower(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_FRACTION_TOWER.get(), pos, state);
        tanks = new FluidTankNTM[3];
        tanks[0] = new FluidTankNTM(Fluids.HEAVYOIL, 4000);
        tanks[1] = new FluidTankNTM(Fluids.BITUMEN, 4000);
        tanks[2] = new FluidTankNTM(Fluids.SMEAR, 4000);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineFractionTower te) {
        BlockEntity stack = level.getBlockEntity(pos.offset(0, 3, 0));

        if (stack instanceof TileEntityMachineFractionTower frac) {
            // make types equal to the segment below
            for (int i = 0; i < 3; i++) frac.tanks[i].setTankType(te.tanks[i].getTankType());

            int oil = Math.min(te.tanks[0].getFill(), frac.tanks[0].getMaxFill() - frac.tanks[0].getFill());
            int left = Math.min(frac.tanks[1].getFill(), te.tanks[1].getMaxFill() - te.tanks[1].getFill());
            int right = Math.min(frac.tanks[2].getFill(), te.tanks[2].getMaxFill() - te.tanks[2].getFill());

            te.tanks[0].setFill(te.tanks[0].getFill() - oil);
            te.tanks[1].setFill(te.tanks[1].getFill() + left);
            te.tanks[2].setFill(te.tanks[2].getFill() + right);
            frac.tanks[0].setFill(frac.tanks[0].getFill() + oil);
            frac.tanks[1].setFill(frac.tanks[1].getFill() - left);
            frac.tanks[2].setFill(frac.tanks[2].getFill() - right);
        }

        te.setupTanks();

        if (level.getGameTime() % 10 == 0) te.fractionate();

        te.sendFluid();
        te.networkPackNT();
    }

    private void sendFluid() {
        for (DirPos dp : getConPos()) {
            this.tryProvide(tanks[1], level, dp);
            this.tryProvide(tanks[2], level, dp);
        }
    }


    private void setupTanks() {
        FractionRecipes.FractionPair pair = FractionRecipes.getFractions(tanks[0].getTankType());
        if (pair != null) {
            tanks[1].setTankType(pair.left().type);
            tanks[2].setTankType(pair.right().type);
        } else {
            tanks[0].setTankType(Fluids.NONE);
            tanks[1].setTankType(Fluids.NONE);
            tanks[2].setTankType(Fluids.NONE);
        }
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z, Library.POS_X),
                new DirPos(x - 2, y, z, Library.NEG_X),
                new DirPos(x, y, z + 2, Library.POS_Z),
                new DirPos(x, y, z - 2, Library.NEG_Z)
        };
    }

    private void fractionate() {
        FractionRecipes.FractionPair pair = FractionRecipes.getFractions(tanks[0].getTankType());
        if (pair == null) return;

        int left = pair.left().fill;
        int right = pair.right().fill;

        if (tanks[0].getFill() >= 100 && hasSpace(left, right)) {
            tanks[0].setFill(tanks[0].getFill() - 100);
            tanks[1].setFill(tanks[1].getFill() + left);
            tanks[2].setFill(tanks[2].getFill() + right);
        }
    }

    private boolean hasSpace(int left, int right) {
        return tanks[1].getFill() + left <= tanks[1].getMaxFill() && tanks[2].getFill() + right <= tanks[2].getMaxFill();
    }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1], tanks[2]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (int i = 0; i < 3; i++) tanks[i].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (int i = 0; i < 3; i++) tanks[i].deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < 3; i++) tanks[i].writeToNBT(output, "tank" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < 3; i++) tanks[i].readFromNBT(input, "tank" + i);
    }
}
