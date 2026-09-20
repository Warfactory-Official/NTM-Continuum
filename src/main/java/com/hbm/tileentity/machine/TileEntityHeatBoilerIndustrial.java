package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.tileentity.MachineCore;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.HeatRecipes;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundSource;
import com.hbm.sound.ModSounds;
import com.hbm.sound.MachineAudio;
import com.hbm.sound.AudioWrapper;

/** Industrial boiler, ported from {@code TileEntityHeatBoilerIndustrial}. */
public class TileEntityHeatBoilerIndustrial extends TileEntityLoadedBase implements IFluidStandardTransceiverMK2, IRORValueProvider {
    public static final int maxHeat = 12_800_000;
    public static final double diffusion = 0.1D;

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.WATER, 64_000),
            new FluidTankNTM(Fluids.STEAM, 6_400_000)
    };
    public int heat;
    public boolean isOn;
    private AudioWrapper audio;
    private int audioTime;

    public TileEntityHeatBoilerIndustrial(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_INDUSTRIAL_BOILER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityHeatBoilerIndustrial te) {
        te.setupTanks();
        te.tryPullHeat(level);
        te.isOn = false;
        te.tryConvert(level);

        if (te.tanks[1].getFill() > 0) {
            for (DirPos connection : te.getConPos()) te.tryProvide(te.tanks[1], level, connection);
        }
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  TileEntityHeatBoilerIndustrial te) {
        if (te.isOn) te.audioTime = 20;
        if (te.audioTime > 0) te.audioTime--;
        te.audio = MachineAudio.tick(te.audio, te.audioTime > 0, ModSounds.BOILER.get(),
                SoundSource.BLOCKS, pos, te.getVolume(0.125F), 10F, 1.0F, 20);
    }

    private void setupTanks() {
        HeatRecipes.HeatStep step = HeatRecipes.getBoilerStep(tanks[0].getTankType());
        if (step != null) {
            tanks[1].setTankType(step.output());
            tanks[1].changeTankSize(tanks[0].getMaxFill() * step.amountProduced() / step.amountReq());
        } else {
            tanks[0].setTankType(Fluids.NONE);
            tanks[1].setTankType(Fluids.NONE);
        }
    }

    private void tryConvert(Level level) {
        HeatRecipes.HeatStep step = HeatRecipes.getBoilerStep(tanks[0].getTankType());
        if (step == null) return;
        int inputOps = tanks[0].getFill() / step.amountReq();
        int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.amountProduced();
        int heatOps = heat / Math.max(step.heatReq(), 1);
        int ops = Math.min(inputOps, Math.min(outputOps, heatOps));
        tanks[0].setFill(tanks[0].getFill() - step.amountReq() * ops);
        tanks[1].setFill(tanks[1].getFill() + step.amountProduced() * ops);
        heat -= step.heatReq() * ops;

        if (ops > 0 && level.getRandom().nextInt(400) == 0) {
            level.playSound(null, worldPosition.above(2), ModSounds.BOILER_GROAN.get(),
                    SoundSource.BLOCKS, 0.5F, 1.0F);
        }

        isOn = ops > 0;
    }

    private void tryPullHeat(Level level) {
        if (heat >= maxHeat) return;
        BlockPos below = worldPosition.below();
        BlockEntity sourceEntity = level.getBlockEntity(below);
        if (!(sourceEntity instanceof IHeatSource)) {
            sourceEntity = MachineCore.at(level, below, level.getBlockState(below));
        }
        if (sourceEntity instanceof IHeatSource source) {
            int diff = source.getHeatStored() - heat;
            if (diff > 0) {
                diff = Math.min((int) Math.ceil(diff * diffusion), maxHeat - heat);
                source.useUpHeat(diff);
                heat += diff;
                return;
            }
        }
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }


    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z, Library.POS_X),
                new DirPos(x - 2, y, z, Library.NEG_X),
                new DirPos(x, y, z + 2, Library.POS_Z),
                new DirPos(x, y, z - 2, Library.NEG_Z),
                new DirPos(x, y + 5, z, Library.POS_Y)
        };
    }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(heat);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        heat = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        isOn = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "water");
        tanks[1].writeToNBT(output, "steam");
        output.putInt("heat", heat);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "water");
        tanks[1].readFromNBT(input, "steam");
        heat = input.getIntOr("heat", 0);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "input", PREFIX_VALUE + "output"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "input").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "output").equals(name)) return "" + tanks[1].getFill();
        return null;
    }
}
