package com.hbm.tileentity.machine.oil;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CrackingRecipes;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.TickPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Catalytic Cracker — 1:1 port of the 1.12.2 {@code TileEntityMachineCatalyticCracker}. A large
 * steam-driven tower (no power, no GUI). 5 tanks: {@code [0] BITUMEN 4k} input (settable via fluid-ID
 * right-click on the block), {@code [1] STEAM 8k}, {@code [2] OIL 4k}, {@code [3] PETROLEUM 4k},
 * {@code [4] SPENTSTEAM 800}. {@link #crack()} runs every 5 ticks in 2 loops: −100 input −200 steam →
 * +left +right +2 spentsteam; sends every 10 ticks.
 *
 * <p>Deferred (§20): the Forge fluid-capability wrapper ({@code NTMFluidHandlerWrapper}).
 */
public class TileEntityMachineCatalyticCracker extends TileEntityLoadedBase implements IFluidStandardTransceiverMK2 {

    public FluidTankNTM[] tanks;

    public TileEntityMachineCatalyticCracker(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CATALYTIC_CRACKER.get(), pos, state);
        tanks = new FluidTankNTM[5];
        tanks[0] = new FluidTankNTM(Fluids.BITUMEN, 4000);
        tanks[1] = new FluidTankNTM(Fluids.STEAM, 8000);
        tanks[2] = new FluidTankNTM(Fluids.OIL, 4000);
        tanks[3] = new FluidTankNTM(Fluids.PETROLEUM, 4000);
        tanks[4] = new FluidTankNTM(Fluids.SPENTSTEAM, 800); // 1.7.10: 800 (1.12.2 used 4,000)
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCatalyticCracker te) {
        te.setupTanks();
        for (DirPos dp : te.getConPos()) {
            te.trySubscribe(te.tanks[0].getTankType(), level, dp);
            te.trySubscribe(te.tanks[1].getTankType(), level, dp);
        }

        if (TickPhase.every(level, pos, 5)) te.crack();

        if (TickPhase.every(level, pos, 10)) {
            for (DirPos dp : te.getConPos()) {
                for (int i = 2; i <= 4; i++) {
                    if (te.tanks[i].getFill() > 0) te.tryProvide(te.tanks[i], level, dp);
                }
            }
            te.networkPackNT();
        }
    }


    public DirPos[] getConPos() {
        int meta = MachineCore.facingOrdinal(getBlockState(), 2);
        ForgeDirection dir = ForgeDirection.getOrientation(meta);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x + dir.offsetX * 4 + rot.offsetX, y, z + dir.offsetZ * 4 + rot.offsetZ, dir),
                new DirPos(x + dir.offsetX * 4 - rot.offsetX * 2, y, z + dir.offsetZ * 4 - rot.offsetZ * 2, dir),
                new DirPos(x - dir.offsetX * 4 + rot.offsetX, y, z - dir.offsetZ * 4 + rot.offsetZ, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 4 - rot.offsetX * 2, y, z - dir.offsetZ * 4 - rot.offsetZ * 2, dir.getOpposite()),
                new DirPos(x + dir.offsetX * 2 + rot.offsetX * 3, y, z + dir.offsetZ * 2 + rot.offsetZ * 3, rot),
                new DirPos(x + dir.offsetX * 2 - rot.offsetX * 4, y, z + dir.offsetZ * 2 - rot.offsetZ * 4, rot),
                new DirPos(x - dir.offsetX * 2 + rot.offsetX * 3, y, z - dir.offsetZ * 2 + rot.offsetZ * 3, rot.getOpposite()),
                new DirPos(x - dir.offsetX * 2 - rot.offsetX * 4, y, z - dir.offsetZ * 2 - rot.offsetZ * 4, rot.getOpposite())
        };
    }

    public void setupTanks() {
        CrackingRecipes.CrackingRecipe rec = CrackingRecipes.getCracking(tanks[0].getTankType());
        if (rec != null) {
            tanks[1].setTankType(Fluids.STEAM);
            tanks[2].setTankType(rec.left().type);
            tanks[3].setTankType(rec.right().type);
            tanks[4].setTankType(Fluids.SPENTSTEAM);
        } else {
            for (int i = 2; i < 5; i++) tanks[i].setTankType(Fluids.NONE); // 1.7: only the output tanks
        }
    }

    private void crack() {
        CrackingRecipes.CrackingRecipe rec = CrackingRecipes.getCracking(tanks[0].getTankType());
        if (rec == null) return;

        int left = rec.left().fill;
        int right = rec.right().fill;

        for (int i = 0; i < 2; i++) {
            if (tanks[0].getFill() >= 100 && tanks[1].getFill() >= 200 && hasSpace(left, right)) {
                tanks[0].setFill(tanks[0].getFill() - 100);
                tanks[1].setFill(tanks[1].getFill() - 200);
                tanks[2].setFill(tanks[2].getFill() + left);
                tanks[3].setFill(tanks[3].getFill() + right);
                tanks[4].setFill(tanks[4].getFill() + 2);
            }
        }
    }

    private boolean hasSpace(int left, int right) {
        return tanks[2].getFill() + left <= tanks[2].getMaxFill()
                && tanks[3].getFill() + right <= tanks[3].getMaxFill()
                && tanks[4].getFill() + 2 <= tanks[4].getMaxFill();
    }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[2], tanks[3], tanks[4]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0], tanks[1]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (int i = 0; i < 5; i++) tanks[i].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (int i = 0; i < 5; i++) tanks[i].deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < 5; i++) tanks[i].writeToNBT(output, "tank" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < 5; i++) tanks[i].readFromNBT(input, "tank" + i);
    }
}
