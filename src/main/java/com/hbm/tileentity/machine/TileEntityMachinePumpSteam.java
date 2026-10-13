package com.hbm.tileentity.machine;

import com.hbm.api.environment.Environment;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.HbmBlockTags;
import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Steam-powered groundwater pump ported from the legacy pump base and steam implementation. */
public class TileEntityMachinePumpSteam extends TileEntityMachineBase
        implements IFluidStandardTransceiverMK2, PumpAnimationState {

    public static final int GROUND_HEIGHT = 70;
    public static final int GROUND_DEPTH = 4;
    public static final int STEAM_SPEED = 1_000;
    public static final int STEAM_COST = 100;
    public static final int NON_WATER_DEBUFF = 100;

    public final FluidTankNTM water = new FluidTankNTM(Fluids.WATER, STEAM_SPEED * 100);
    public final FluidTankNTM steam = new FluidTankNTM(Fluids.STEAM, 1_000);
    public final FluidTankNTM lps = new FluidTankNTM(Fluids.SPENTSTEAM, 10);
    public boolean isOn;
    public float rotor;
    public float lastRotor;
    public boolean onGround;
    public int groundCheckDelay;

    public TileEntityMachinePumpSteam(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PUMP_STEAM.get(), pos, state, 0);
    }

    @Override
    public String getDefaultName() {
        return "block.hbm.machine_pump_steam";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePumpSteam pump) {
        // The legacy steam subtype subscribes steam and sends spent steam before the base sends water.
        for (DirPos connection : pump.getConPos()) {
            pump.trySubscribe(pump.steam.getTankType(), level, connection);
            if (pump.lps.getFill() > 0) pump.tryProvide(pump.lps, level, connection);
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

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePumpSteam pump) {
        pump.lastRotor = pump.rotor;
        if (pump.isOn) pump.rotor += 10F;
        if (pump.rotor >= 360F) {
            pump.rotor -= 360F;
            pump.lastRotor -= 360F;
            play(level, pos, ModSounds.STEAM_ENGINE_OPERATE.get(), 0.5F, 0.75F);
            play(level, pos, SoundEvents.GENERIC_SPLASH, 1F, 0.5F);
        }
    }

    private static void play(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                sound, SoundSource.BLOCKS, volume, pitch, false);
    }

    private boolean canOperate() {
        return steam.getFill() >= STEAM_COST
                && lps.getMaxFill() - lps.getFill() > 0
                && water.getFill() < water.getMaxFill();
    }

    private void operate() {
        steam.setFill(steam.getFill() - STEAM_COST);
        lps.setFill(lps.getFill() + 1);
        int pumpSpeed = water.getTankType() == Fluids.WATER ? STEAM_SPEED : STEAM_SPEED / NON_WATER_DEBUFF;
        water.setFill(Math.min(water.getFill() + pumpSpeed, water.getMaxFill()));
    }

    private boolean checkGround(Level level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight()) return false;
        // hbm-space TileEntityMachinePumpBase.checkGround: no groundwater in orbit, and the pump lifts the body's
        // water table, whatever fluid that is (com.hbm.api.environment.Environment; plain HBM: water)
        FluidType table = Environment.groundwater(level, pos);
        if (table == Fluids.NONE) return false;
        water.setTankType(table);

        int validBlocks = 0;
        int invalidBlocks = 0;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y >= -GROUND_DEPTH; y--) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos groundPos = pos.offset(x, y, z);
                    BlockState groundState = level.getBlockState(groundPos);
                    if (y == -1 && !Compat.isSolidRender(groundState, level, groundPos)) return false;
                    if (groundState.is(HbmBlockTags.PUMP_GROUND)) validBlocks++;
                    else invalidBlocks++;
                }
            }
        }
        return validBlocks >= invalidBlocks;
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

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{steam}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{water, lps}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{water, steam, lps}; }
    @Override public float getRotor() { return rotor; }
    @Override public float getLastRotor() { return lastRotor; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeBoolean(onGround);
        water.serialize(buf);
        steam.serialize(buf);
        lps.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        onGround = buf.readBoolean();
        water.deserialize(buf);
        steam.deserialize(buf);
        lps.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("isOn", isOn);
        output.putBoolean("onGround", onGround);
        output.putInt("groundCheckDelay", groundCheckDelay);
        water.writeToNBT(output, "water");
        steam.writeToNBT(output, "steam");
        lps.writeToNBT(output, "lps");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        isOn = input.getBooleanOr("isOn", false);
        onGround = input.getBooleanOr("onGround", false);
        groundCheckDelay = input.getIntOr("groundCheckDelay", 0);
        water.readFromNBT(input, "water");
        steam.readFromNBT(input, "steam");
        lps.readFromNBT(input, "lps");
    }
}
