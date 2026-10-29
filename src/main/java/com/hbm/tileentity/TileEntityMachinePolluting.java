package com.hbm.tileentity;

import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import com.hbm.module.portmanager.ModulePortManFluidAdaptive;

/**
 * The 1.7 {@code TileEntityMachinePolluting}: a machine that buffers what it emits as smoke fluid in three
 * tanks ({@code SMOKE}, {@code SMOKE_LEADED}, {@code SMOKE_POISON}, 100 mB per pollution unit) and offers
 * them to a chimney. Whatever does not fit in a tank vents straight into the region with a hiss.
 */
public abstract class TileEntityMachinePolluting extends TileEntityMachineBase implements IFluidStandardSenderMK2 {

    public FluidTankNTM smoke;
    public FluidTankNTM smoke_leaded;
    public FluidTankNTM smoke_poison;

    protected ModulePortManFluidAdaptive moduleSmokePorts;

    public TileEntityMachinePolluting(BlockEntityType<?> type, BlockPos pos, BlockState state, int scount, int buffer) {
        super(type, pos, state, scount);
        smoke = new FluidTankNTM(Fluids.SMOKE, buffer);
        smoke_leaded = new FluidTankNTM(Fluids.SMOKE_LEADED, buffer);
        smoke_poison = new FluidTankNTM(Fluids.SMOKE_POISON, buffer);

        moduleSmokePorts = new ModulePortManFluidAdaptive(this).setOutputTanks(smoke, smoke_leaded, smoke_poison);
    }

    public void pollute(PollutionType type, float amount) {
        FluidTankNTM tank = type == PollutionType.SOOT ? smoke : type == PollutionType.HEAVYMETAL ? smoke_leaded : smoke_poison;

        int fluidAmount = (int) Math.ceil(amount * 100);
        // 1.7 overfills the tank and vents the excess; FluidTankNTM.setFill clamps, so the excess is counted first
        int fill = tank.getFill() + fluidAmount;
        tank.setFill(fill);

        if (fill > tank.getMaxFill()) {
            int overflow = fill - tank.getMaxFill();
            PollutionHandler.incrementPollution(level, worldPosition, type, overflow / 100F);

            // 1.7 NTMSounds.VANILLA_HISS, "random.fizz"
            if (level.getRandom().nextInt(3) == 0) {
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.1F, 1.5F);
            }
        }
    }

    /** {@code amount} of the fluid burned or spilled, scaled by each entry of its pollution table. */
    public void pollute(FluidType type, FluidReleaseType release, float amount) {
        FT_Polluting trait = type.getTrait(FT_Polluting.class);
        if (trait == null) return;
        if (release == FluidReleaseType.VOID) return;

        Map<PollutionType, Float> map = release == FluidReleaseType.BURN ? trait.burnMap : trait.releaseMap;

        for (Map.Entry<PollutionType, Float> entry : map.entrySet()) {
            pollute(entry.getKey(), entry.getValue() * amount);
        }
    }

    public FluidTankNTM[] getSmokeTanks() {
        return new FluidTankNTM[]{smoke, smoke_leaded, smoke_poison};
    }

    @Override
    public void setRemoved() {
        if (level != null) moduleSmokePorts.destroy();
        super.setRemoved();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        smoke.readFromNBT(input, "smoke0");
        smoke_leaded.readFromNBT(input, "smoke1");
        smoke_poison.readFromNBT(input, "smoke2");
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        smoke.writeToNBT(output, "smoke0");
        smoke_leaded.writeToNBT(output, "smoke1");
        smoke_poison.writeToNBT(output, "smoke2");
    }
}
