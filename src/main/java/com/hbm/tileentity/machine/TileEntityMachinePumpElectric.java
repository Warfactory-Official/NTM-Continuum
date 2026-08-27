package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Electric groundwater pump logic ported from the legacy pump base and electric implementation. */
public class TileEntityMachinePumpElectric extends TileEntityMachineBase
        implements IEnergyReceiverMK2, IFluidStandardSenderMK2 {

    public static final long MAX_POWER = 10_000;
    public static final int POWER_COST = 1_000;
    public static final int GROUND_HEIGHT = 70;
    public static final int GROUND_DEPTH = 4;
    public static final int ELECTRIC_SPEED = 10_000;
    public static final int NON_WATER_DEBUFF = 100;

    public long power;
    public final FluidTankNTM water = new FluidTankNTM(Fluids.WATER, ELECTRIC_SPEED * 100);
    public boolean isOn;
    public float rotor;
    public float lastRotor;
    public boolean onGround;
    public int groundCheckDelay;

    public TileEntityMachinePumpElectric(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PUMP_ELECTRIC.get(), pos, state, 0);
    }

    @Override
    public String getDefaultName() {
        return "block.hbm.machine_pump_electric";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePumpElectric pump) {
        if (level.getGameTime() % 20 == 0) {
            for (DirPos connection : pump.getConPos()) pump.trySubscribe(level, connection);
        }

        for (DirPos connection : pump.getConPos()) {
            if (pump.water.getFill() > 0) pump.tryProvide(pump.water, level, connection);
        }

        if (pump.groundCheckDelay > 0) pump.groundCheckDelay--;
        else pump.onGround = pump.checkGround(level, pos);

        pump.isOn = false;
        if (pump.canOperate() && pos.getY() <= GROUND_HEIGHT && pump.onGround) {
            pump.isOn = true;
            pump.operate();
        }

        pump.setChanged();
        pump.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePumpElectric pump) {
        pump.lastRotor = pump.rotor;
        if (pump.isOn) pump.rotor += 10F;
        if (pump.rotor >= 360F) {
            pump.rotor -= 360F;
            pump.lastRotor -= 360F;
            play(level, pos, com.hbm.sound.ModSounds.STEAM_ENGINE_OPERATE.get(), 0.5F, 0.75F);
            play(level, pos, net.minecraft.sounds.SoundEvents.GENERIC_SPLASH, 1F, 0.5F);
        }
    }

    private static void play(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                sound, SoundSource.BLOCKS, volume, pitch, false);
    }

    private boolean canOperate() {
        return power >= POWER_COST && water.getFill() < water.getMaxFill();
    }

    private void operate() {
        power -= POWER_COST;
        int pumpSpeed = water.getTankType() == Fluids.WATER ? ELECTRIC_SPEED : ELECTRIC_SPEED / NON_WATER_DEBUFF;
        water.setFill(Math.min(water.getFill() + pumpSpeed, water.getMaxFill()));
    }

    private boolean checkGround(Level level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight()) return false;

        int validBlocks = 0;
        int invalidBlocks = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y >= -GROUND_DEPTH; y--) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos groundPos = pos.offset(x, y, z);
                    BlockState groundState = level.getBlockState(groundPos);
                    Block block = groundState.getBlock();
                    if (y == -1 && !Compat.isSolidRender(groundState, level, groundPos)) return false;
                    if (isValidGround(block)) validBlocks++;
                    else invalidBlocks++;
                }
            }
        }
        return validBlocks >= invalidBlocks;
    }

    private static boolean isValidGround(Block block) {
        return block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.SAND
                || block == Blocks.MYCELIUM || block == ModFalloutBlocks.waste_earth.get()
                || block == ModFallingBlocks.dirt_dead.get() || block == ModFallingBlocks.dirt_oily.get()
                || block == ModFallingBlocks.sand_dirty.get() || block == ModFallingBlocks.sand_dirty_red.get();
    }

    public DirPos[] getConPos() {
        BlockPos pos = getBlockPos();
        return new DirPos[]{
                new DirPos(pos.east(2), Library.POS_X),
                new DirPos(pos.west(2), Library.NEG_X),
                new DirPos(pos.south(2), Library.POS_Z),
                new DirPos(pos.north(2), Library.NEG_Z)
        };
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return MAX_POWER; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{water}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{water}; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeBoolean(onGround);
        water.serialize(buf);
        buf.writeLong(power);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        onGround = buf.readBoolean();
        water.deserialize(buf);
        power = buf.readLong();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putBoolean("isOn", isOn);
        output.putBoolean("onGround", onGround);
        output.putInt("groundCheckDelay", groundCheckDelay);
        water.writeToNBT(output, "water");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        isOn = input.getBooleanOr("isOn", false);
        onGround = input.getBooleanOr("onGround", false);
        groundCheckDelay = input.getIntOr("groundCheckDelay", 0);
        water.readFromNBT(input, "water");
    }
}
