package com.hbm.tileentity.machine;

import com.hbm.api.environment.Environment;
import dev.golem.core.data.Tags;

import com.hbm.api.tile.IHeatSource;
import com.hbm.lib.ForgeDirection;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.tileentity.MachineCore;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.TileEntityMachinePolluting;
import com.hbm.util.TickPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Base for solid-fuel heaters (1.12.2 {@code TileEntityFireboxBase}): two fuel slots burn furnace fuels
 * (times/heat scaled by {@link ModuleBurnTime}) into a {@code heatEnergy} reservoir that an
 * {@link IHeatSource} consumer above drains. Burn time only ticks down while the reservoir isn't full;
 * with no fire the reservoir passively decays by {@code max(heat/1000, 1)}. While burning below full heat it
 * puts 1.7's {@code SOOT_PER_SECOND * 3} into its smoke tanks each second and offers them to the ring of
 * cells around its footprint. Deferred from the original: the ashpit and the muffler.
 */
public abstract class TileEntityFireboxBase extends TileEntityMachinePolluting implements MenuProvider, IHeatSource {

    public int maxBurnTime;
    public int burnTime;
    public int burnHeat;
    public boolean wasOn = false;

    public int heatEnergy;

    /** Number of players with the GUI open (server-authoritative, synced); drives the door swing. */
    public int playersUsing;

    /** Client-only interpolated door swing (0 closed .. 135 fully open), like the 1.12.2 renderer. */
    public float doorAngle;
    public float prevDoorAngle;

    public TileEntityFireboxBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, 50);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFireboxBase te) {
        for (int i = 2; i < 6; i++) {
            ForgeDirection dir = ForgeDirection.getOrientation(i);
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);

            for (int j = -1; j <= 1; j++) {
                te.sendSmoke(pos.getX() + dir.offsetX * 2 + rot.offsetX * j, pos.getY(),
                        pos.getZ() + dir.offsetZ * 2 + rot.offsetZ * j, dir);
            }
        }

        te.wasOn = false;
        // hbm-space's canOperate: whether the fire got air this tick (com.hbm.api.environment.Environment). Plain HBM
        // always breathes, which is 1.7. Without air a new fuel item still loads (hbm-space asks before loading but
        // does not gate it), but a burning fire neither burns down nor stays lit.
        boolean canOperate;

        if (te.burnTime <= 0) {
            canOperate = Environment.breatheAir(level, pos, 0);
            for (int i = 0; i < 2; i++) {
                ItemStack fuel = te.inventory.getStackInSlot(i);
                if (fuel.isEmpty()) continue;

                int baseTime = te.getModule().getBurnTime(level, fuel);
                if (baseTime <= 0) continue;

                te.maxBurnTime = te.burnTime = (int) (baseTime * te.getTimeMult());
                te.burnHeat = te.getModule().getBurnHeat(te.getBaseHeat(), fuel);

                // Captured BEFORE the shrink: once the stack empties its item is AIR, and asking AIR
                // for a crafting remainder silently returns nothing (lava bucket -> no empty bucket).
                ItemStack remainder = Compat.craftingRemainder(fuel); // e.g. lava bucket -> empty bucket
                fuel.shrink(1);
                if (fuel.isEmpty()) {
                    te.inventory.setStackInSlot(i, remainder);
                }

                te.wasOn = true;
                te.setChanged();
                break;
            }
        } else {
            if (te.heatEnergy < te.getMaxHeat()) {
                // hbm-space: the firebox consumes 1 mB of oxygen every 5 ticks, the heating oven every tick
                canOperate = Environment.breatheAir(level, pos,
                        TickPhase.every(level, pos, 500 / te.getBaseHeat()) ? 1 : 0);
                if (canOperate) {
                    te.burnTime--;
                    if (TickPhase.every(level, pos, 20L)) te.pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 3);
                }
            } else {
                canOperate = Environment.breatheAir(level, pos, 0);
            }

            if (canOperate) {
                te.wasOn = true;

                if (level.getRandom().nextInt(15) == 0 && !te.muffled) {
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS,
                            1.0F, 0.5F + level.getRandom().nextFloat() * 0.5F);
                }
            }
        }

        if (te.wasOn) {
            te.heatEnergy = Math.min(te.heatEnergy + te.burnHeat, te.getMaxHeat());
        } else {
            te.heatEnergy = Math.max(te.heatEnergy - Math.max(te.heatEnergy / 1000, 1), 0);
            // hbm-space keeps the burn heat of a fire that only lacks air, so it resumes at the same heat
            if (canOperate) te.burnHeat = 0;
        }

        te.networkPackNT();
    }

    /** Client: the door swing + flame particles at the fire door while burning (the old client update). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityFireboxBase te) {
        // Door swing: eases open while someone has the GUI open, eases shut otherwise (1.12.2 verbatim).
        te.prevDoorAngle = te.doorAngle;
        float swingSpeed = (te.doorAngle / 10F) + 3F;
        if (te.playersUsing > 0) te.doorAngle += swingSpeed;
        else te.doorAngle -= swingSpeed;
        te.doorAngle = net.minecraft.util.Mth.clamp(te.doorAngle, 0F, 135F);

        if (te.wasOn && TickPhase.every(level, pos, 5)) {
            ForgeDirection dir = MachineCore.facing(state);
            double x = pos.getX() + 0.5 + dir.offsetX;
            double y = pos.getY() + 0.25;
            double z = pos.getZ() + 0.5 + dir.offsetZ;
            level.addParticle(ParticleTypes.FLAME,
                    x + level.getRandom().nextDouble() * 0.5 - 0.25,
                    y + level.getRandom().nextDouble() * 0.25,
                    z + level.getRandom().nextDouble() * 0.5 - 0.25, 0, 0, 0);
        }
    }

    /** Classifies fuel for the ash stream used by the firebox and brick furnace. */
    public static com.hbm.items.ItemEnums.EnumAshType getAshFromFuel(ItemStack stack) {
        if (stack.isEmpty()) return com.hbm.items.ItemEnums.EnumAshType.MISC;
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.PLANKS) || stack.is(ItemTags.SAPLINGS)
                || path.contains("wood")) {
            return com.hbm.items.ItemEnums.EnumAshType.WOOD;
        }
        if (path.contains("coal") || path.contains("coke") || path.contains("lignite")) {
            return com.hbm.items.ItemEnums.EnumAshType.COAL;
        }
        return com.hbm.items.ItemEnums.EnumAshType.MISC;
    }

    public abstract ModuleBurnTime getModule();
    public abstract int getBaseHeat();
    public abstract double getTimeMult();
    public abstract int getMaxHeat();

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{0, 1};
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return level != null && getModule().getBurnTime(level, stack) > 0;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[0];
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return this.getSmokeTanks();
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return dir != ForgeDirection.UNKNOWN && dir != ForgeDirection.DOWN;
    }

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    @Override
    public void useUpHeat(int heat) {
        this.heatEnergy = Math.max(0, this.heatEnergy - heat);
    }

    // --- sync + persistence (1.12.2 keys) ---

    /** Called from the menu (server side) when a player opens/closes the GUI — drives the door. */
    public void setPlayersUsing(int count) {
        this.playersUsing = Math.max(0, count);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(maxBurnTime);
        buf.writeInt(burnTime);
        buf.writeInt(burnHeat);
        buf.writeInt(heatEnergy);
        buf.writeBoolean(wasOn);
        buf.writeInt(playersUsing);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        maxBurnTime = buf.readInt();
        burnTime = buf.readInt();
        burnHeat = buf.readInt();
        heatEnergy = buf.readInt();
        wasOn = buf.readBoolean();
        playersUsing = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("maxBurnTime", maxBurnTime);
        output.putInt("burnTime", burnTime);
        output.putInt("burnHeat", burnHeat);
        output.putInt("heatEnergy", heatEnergy);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        maxBurnTime = input.getIntOr("maxBurnTime", 0);
        burnTime = input.getIntOr("burnTime", 0);
        burnHeat = input.getIntOr("burnHeat", 0);
        heatEnergy = input.getIntOr("heatEnergy", 0);
    }
}
