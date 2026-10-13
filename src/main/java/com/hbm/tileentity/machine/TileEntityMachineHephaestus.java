package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.sound.ModPowerGenSounds;
import com.hbm.tileentity.ModPowerGenBlockEntities;

/**
 * The geothermal heat exchanger, 1:1 with 1.7 {@code TileEntityMachineHephaestus}. Every tick it scans one
 * 15x15 layer below itself (ten layers, one per tick, cycling on world time) for lava, volcanic lava and
 * volcanic fissures, and heats one {@code FT_Heatable} step of the input fluid with the ten-layer sum.
 */
public class TileEntityMachineHephaestus extends TileEntityLoadedBase implements IFluidStandardTransceiverMK2 {

    public final FluidTankNTM input;
    public final FluidTankNTM output;
    /** The client's copy of {@link #getTotalHeat()}. */
    public int bufferedHeat;

    public float rot;
    public float prevRot;

    private final int[] heat = new int[10];
    private long fissureScanTime;

    private @Nullable AudioWrapper audio;

    private static @Nullable Block oreVolcano;
    private static boolean oreVolcanoLooked;

    public TileEntityMachineHephaestus(BlockPos pos, BlockState state) {
        super(ModPowerGenBlockEntities.HEPHAESTUS.get(), pos, state);
        this.input = new FluidTankNTM(Fluids.OIL, 24_000);
        this.output = new FluidTankNTM(Fluids.HOTOIL, 24_000);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineHephaestus heatex) {
        heatex.setupTanks();

        if (level.getGameTime() % 20 == 0 && heatex.input.getTankType() != Fluids.NONE) {
            for (DirPos connection : heatex.getConPos()) heatex.trySubscribe(heatex.input.getTankType(), level, connection);
        }

        int height = (int) (level.getGameTime() % 10);
        int range = 7;
        int y = pos.getY() - 1 - height;

        heatex.heat[height] = 0;

        if (y >= level.getMinY()) {
            BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    heatex.heat[height] += heatex.heatFromBlock(scan.set(pos.getX() + x, y, pos.getZ() + z));
                }
            }
        }

        heatex.heatFluid();

        if (heatex.output.getFill() > 0) {
            for (DirPos connection : heatex.getConPos()) {
                heatex.tryProvide(heatex.output, level, connection);
            }
        }
        heatex.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineHephaestus heatex) {
        heatex.prevRot = heatex.rot;

        if (heatex.bufferedHeat > 0) {
            heatex.rot += 0.5F;

            RandomSource rand = level.getRandom();
            if (rand.nextInt(7) == 0) {
                double x = rand.nextGaussian() * 2;
                double y = rand.nextGaussian() * 3;
                double z = rand.nextGaussian() * 2;
                level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5 + x, pos.getY() + 6 + y, pos.getZ() + 0.5 + z,
                        0, 0, 0);
            }

            if (heatex.audio == null) {
                heatex.audio = AudioSystem.getLoopedSound(ModPowerGenSounds.HEPHAESTUS_LOOP.get(), SoundSource.BLOCKS,
                        pos.getX(), pos.getY() + 5F, pos.getZ(), 0.75F, 10F, 1.0F);
                if (heatex.audio != null) heatex.audio.startSound();
            }
        } else {
            if (heatex.audio != null) {
                heatex.audio.stopSound();
                heatex.audio = null;
            }
        }

        if (heatex.rot >= 360F) {
            heatex.prevRot -= 360F;
            heatex.rot -= 360F;
        }
    }

    /** 1.7 {@code invalidate}/{@code onChunkUnload}: the loop dies with the tile. */
    @Override
    public void setRemoved() {
        super.setRemoved();
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    protected void heatFluid() {
        FluidType type = input.getTankType();
        FT_Heatable trait = type.getTrait(FT_Heatable.class);
        if (trait != null) {
            int heat = this.getTotalHeat();
            HeatingStep step = trait.getFirstStep();

            int inputOps = input.getFill() / step.amountReq;
            int outputOps = (output.getMaxFill() - output.getFill()) / step.amountProduced;
            int heatOps = heat / step.heatReq;
            int ops = Math.min(Math.min(inputOps, outputOps), heatOps);

            input.setFill(input.getFill() - step.amountReq * ops);
            output.setFill(output.getFill() + step.amountProduced * ops);
            setChanged();
        }
    }

    protected void setupTanks() {
        FluidType type = input.getTankType();
        FT_Heatable trait = type.getTrait(FT_Heatable.class);
        if (trait != null && trait.getEfficiency(HeatingType.HEATEXCHANGER) > 0) {
            output.setTankType(trait.getFirstStep().typeProduced);
            return;
        }
        input.setTankType(Fluids.NONE);
        output.setTankType(Fluids.NONE);
    }

    /**
     * 1.7: vanilla lava (still or flowing) 5, {@code volcanic_lava_block} 150, {@code ore_volcano} 300 and it
     * marks the fissure scan time. The fissure ore is not in the port yet, so it is looked up by id once and
     * counts for nothing until it exists.
     */
    protected int heatFromBlock(BlockPos at) {
        Block b = level.getBlockState(at).getBlock();

        if (b == Blocks.LAVA) return 5;
        if (b == ModFluidNetBlocks.volcanic_lava_block.get()) return 150;

        Block fissure = oreVolcano();
        if (fissure != null && b == fissure) {
            this.fissureScanTime = level.getGameTime();
            return 300;
        }
        return 0;
    }

    private static @Nullable Block oreVolcano() {
        if (!oreVolcanoLooked) {
            oreVolcanoLooked = true;
            oreVolcano = BuiltInRegistries.BLOCK.getOptional(Library.id("ore_volcano")).orElse(null);
        }
        return oreVolcano;
    }

    public int getTotalHeat() {
        boolean fissure = level.getGameTime() - this.fissureScanTime < 20;
        int total = 0;
        for (int h : this.heat) total += h;
        if (fissure) total *= 3;
        return total;
    }

    /** 1.7: the eight ring blocks at y and y+11, two out from the core on each horizontal axis. */
    public DirPos[] getConPos() {
        BlockPos p = getBlockPos();
        return new DirPos[]{
                new DirPos(p.getX() + 2, p.getY(), p.getZ(), Library.POS_X),
                new DirPos(p.getX() - 2, p.getY(), p.getZ(), Library.NEG_X),
                new DirPos(p.getX(), p.getY(), p.getZ() + 2, Library.POS_Z),
                new DirPos(p.getX(), p.getY(), p.getZ() - 2, Library.NEG_Z),
                new DirPos(p.getX() + 2, p.getY() + 11, p.getZ(), Library.POS_X),
                new DirPos(p.getX() - 2, p.getY() + 11, p.getZ(), Library.NEG_X),
                new DirPos(p.getX(), p.getY() + 11, p.getZ() + 2, Library.POS_Z),
                new DirPos(p.getX(), p.getY() + 11, p.getZ() - 2, Library.NEG_Z)
        };
    }

    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{input, output}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{output}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{input}; }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN && dir != ForgeDirection.UP && dir != ForgeDirection.DOWN;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        input.serialize(buf);
        output.serialize(buf);
        buf.writeInt(getTotalHeat());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        input.deserialize(buf);
        output.deserialize(buf);
        bufferedHeat = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        this.input.writeToNBT(output, "0");
        this.output.writeToNBT(output, "1");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.input.readFromNBT(input, "0");
        this.output.readFromNBT(input, "1");
    }

    public float getRot(float partialTicks) {
        return prevRot + (rot - prevRot) * partialTicks;
    }
}
