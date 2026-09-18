package com.hbm.tileentity.machine;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.HeatRecipes;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.sounds.SoundSource;
import com.hbm.sound.ModSounds;
import com.hbm.sound.MachineAudio;
import com.hbm.sound.AudioWrapper;

/**
 * Heat Boiler — 1:1 port of the 1.12.2 {@code TileEntityHeatBoiler} ({@code heat_boiler}): the
 * OIL→HOTOIL (and water→steam) converter. No GUI/inventory — readouts via the block's look-overlay,
 * typing via a fluid identifier right-click. Pulls heat from the {@link IHeatSource} below (diffusion
 * 0.1, cap 12.8M TU, passive cooling otherwise), converts via {@link HeatRecipes} first-step data
 * (output tank auto-resized to {@code inMax·produced/req}), and — old boiler-burst behavior — when the
 * output tank is completely full ({@code outputOps == 0}) it explodes: the 3×3×2 upper body is cleared
 * and a strength-5 explosion fires ({@code level.explode} stands in for the unported ExplosionVNT;
 * the burst husk renders via {@code boiler_burst.obj}).
 *
 * <p>Skips (§20): TomSaveData sun-fire heating, the boilerGroan ambience, the boiler audio loop,
 * IPersistentNBT/IConfigurableMachine/IFluidCopiable. Port seam: heat is polled through a
 * multiblock cell below by resolving its core (old used proxy TEs).
 */
public class TileEntityHeatBoiler extends TileEntityLoadedBase implements IFluidStandardTransceiverMK2, IRORValueProvider {

    public static final int maxHeat = 12_800_000;
    public static final double diffusion = 0.1D;
    public static final boolean canExplode = true;

    public FluidTankNTM[] tanks;
    public int heat;
    public boolean isOn;
    public boolean hasExploded = false;
    private AudioWrapper audio;
    private int audioTime;

    public TileEntityHeatBoiler(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEAT_BOILER.get(), pos, state);
        tanks = new FluidTankNTM[2];
        this.tanks[0] = new FluidTankNTM(Fluids.WATER, 16_000);
        this.tanks[1] = new FluidTankNTM(Fluids.STEAM, 16_000 * 100);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityHeatBoiler te) {
        if (!te.hasExploded) {
            te.setupTanks();
            te.tryPullHeat();

            te.isOn = false;
            te.tryConvert();

            if (te.tanks[1].getFill() > 0) {
                for (DirPos dp : te.getConPos()) {
                    te.tryProvide(te.tanks[1], level, dp);
                }
            }
        }
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityHeatBoiler te) {
        if (te.isOn) te.audioTime = 20;
        if (te.audioTime > 0) te.audioTime--;
        te.audio = MachineAudio.tick(te.audio, te.audioTime > 0 && !te.hasExploded, ModSounds.BOILER.get(),
                SoundSource.BLOCKS, pos, te.getVolume(0.125F), 10F, 1.0F, 20);
    }

    public DirPos[] getConPos() {
        int meta = MachineCore.facingOrdinal(getBlockState(), 2);
        ForgeDirection dir = ForgeDirection.getOrientation(meta).getRotation(ForgeDirection.UP);
        BlockPos pos = worldPosition;
        return new DirPos[]{
                new DirPos(pos.getX() + dir.offsetX * 2, pos.getY(), pos.getZ() + dir.offsetZ * 2, dir),
                new DirPos(pos.getX() - dir.offsetX * 2, pos.getY(), pos.getZ() - dir.offsetZ * 2, dir.getOpposite()),
                new DirPos(pos.getX(), pos.getY() + 4, pos.getZ(), Library.POS_Y)
        };
    }

    protected void setupTanks() {
        HeatRecipes.HeatStep step = HeatRecipes.getBoilerStep(tanks[0].getTankType());
        if (step != null) {
            tanks[1].setTankType(step.output());
            tanks[1].changeTankSize(tanks[0].getMaxFill() * step.amountProduced() / step.amountReq());
            return;
        }
        tanks[0].setTankType(Fluids.NONE);
        tanks[1].setTankType(Fluids.NONE);
    }

    protected void tryConvert() {
        HeatRecipes.HeatStep step = HeatRecipes.getBoilerStep(tanks[0].getTankType());
        if (step == null) return;

        int heatReq = Math.max(step.heatReq(), 1); // BOILER efficiency is 1.0 for every entry (folded in)
        int inputOps = this.tanks[0].getFill() / step.amountReq();
        int outputOps = (this.tanks[1].getMaxFill() - this.tanks[1].getFill()) / step.amountProduced();
        int heatOps = this.heat / heatReq;

        int ops = Math.min(inputOps, Math.min(outputOps, heatOps));

        this.tanks[0].setFill(this.tanks[0].getFill() - step.amountReq() * ops);
        this.tanks[1].setFill(this.tanks[1].getFill() + step.amountProduced() * ops);
        this.heat -= heatReq * ops;

        if (ops > 0 && this.level.getRandom().nextInt(400) == 0) {
            this.level.playSound(null, this.worldPosition.above(2), ModSounds.BOILER_GROAN.get(),
                    SoundSource.BLOCKS, 0.5F, 1.0F);
        }

        if (ops > 0) {
            this.isOn = true;
        }

        if (outputOps == 0 && canExplode) {
            this.hasExploded = true;
            BlockPos base = this.worldPosition;
            BlockMultiblockCore.withoutTeardown(() -> {
                for (int x = base.getX() - 1; x <= base.getX() + 1; x++) {
                    for (int y = base.getY() + 2; y <= base.getY() + 3; y++) {
                        for (int z = base.getZ() - 1; z <= base.getZ() + 1; z++) {
                            this.level.removeBlock(new BlockPos(x, y, z), false);
                        }
                    }
                }
                this.level.removeBlock(this.worldPosition.above(), false);

                // ExplosionVNT (strength 5, range-mod 3) unported — vanilla stand-in, documented §20.
                // ExplosionInteraction.NONE: the old boiler-burst ExplosionVNT set only entity/player
                // processors and NO BlockAllocator, i.e. it damaged entities but destroyed no blocks. A
                // block-destroying interaction here blows up the boiler's own cells → one self-drop per
                // destroyed cell ("drops multiple of itself") and, with teardown suppressed, orphans.
                this.level.explode(null, base.getX() + 0.5D, base.getY() + 2D, base.getZ() + 0.5D, 5F, Level.ExplosionInteraction.NONE);
            });
        }
    }

    protected void tryPullHeat() {
        if (this.heat >= maxHeat) return;
        BlockPos below = worldPosition.below();
        BlockEntity con = level.getBlockEntity(below);

        // Port seam: the heater below may expose only a dummy cell here — resolve to its core.
        if (!(con instanceof IHeatSource)) {
            con = MachineCore.at(level, below, level.getBlockState(below));
        }

        if (con instanceof IHeatSource source) {
            int diff = source.getHeatStored() - this.heat;
            if (diff == 0) return;

            if (diff > 0) {
                diff = (int) Math.ceil(diff * diffusion);
                source.useUpHeat(diff);
                this.heat += diff;
                if (this.heat > maxHeat) this.heat = maxHeat;
                return;
            }
        }
        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    // --- IFluidStandardTransceiverMK2 ---

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    // --- sync + persistence (old serialize order: hasExploded gate first) ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(hasExploded);
        if (!this.hasExploded) {
            buf.writeInt(this.heat);
            this.tanks[0].serialize(buf);
            this.tanks[1].serialize(buf);
            buf.writeBoolean(this.isOn);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.hasExploded = buf.readBoolean();
        if (!this.hasExploded) {
            this.heat = buf.readInt();
            this.tanks[0].deserialize(buf);
            this.tanks[1].deserialize(buf);
            this.isOn = buf.readBoolean();
        }
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "water");
        tanks[1].writeToNBT(output, "steam");
        output.putInt("heat", heat);
        output.putBoolean("exploded", hasExploded);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "water");
        tanks[1].readFromNBT(input, "steam");
        heat = input.getIntOr("heat", 0);
        hasExploded = input.getBooleanOr("exploded", false);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "input", PREFIX_VALUE + "output"};
    }

    @Override
    public String provideRORValue(String name) {
        if (hasExploded) {
            if ((PREFIX_VALUE + "input").equals(name) || (PREFIX_VALUE + "output").equals(name)) return "0";
            return null;
        }
        if ((PREFIX_VALUE + "input").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "output").equals(name)) return "" + tanks[1].getFill();
        return null;
    }
}
