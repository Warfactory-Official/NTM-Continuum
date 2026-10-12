package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.entity.projectile.EntityZirnoxDebris;
import com.hbm.entity.projectile.EntityZirnoxDebris.DebrisType;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.container.ContainerReactorZirnox;
import com.hbm.items.ModItems;
import com.hbm.items.ModZirnoxItems;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.items.machine.ZirnoxFuel;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.sound.ModSounds;
import com.hbm.Constants;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/** Server simulation for the 24-channel carbon-dioxide moderated ZIRNOX reactor. */
public class TileEntityReactorZirnox extends TileEntityMachineBase implements IFluidStandardTransceiverMK2, WorldlyContainer, MenuProvider, IRORValueProvider, IRORInteractive {

    public static final int MAX_HEAT = 100_000;
    public static final int MAX_PRESSURE = 100_000;
    private static final int[][] NEIGHBOURS = {
            {1, 7}, {0, 2, 8}, {1, 9}, {4, 10}, {3, 5, 11}, {4, 6, 12}, {5, 13},
            {0, 8, 14}, {1, 7, 9, 15}, {2, 8, 16}, {3, 11, 17}, {4, 10, 12, 18},
            {5, 11, 13, 19}, {6, 12, 20}, {7, 15, 21}, {8, 14, 16, 22}, {9, 15, 23},
            {10, 18}, {11, 17, 19}, {12, 18, 20}, {13, 19}, {14, 22}, {15, 21, 23}, {16, 22}
    };
    private static final int[] ROD_SLOTS = java.util.stream.IntStream.range(0, 24).toArray();

    public int heat;
    public int pressure;
    public boolean isOn;
    public boolean redstonePowered;
    public final FluidTankNTM steam = new FluidTankNTM(Fluids.SUPERHOTSTEAM, 8_000);
    public final FluidTankNTM carbonDioxide = new FluidTankNTM(Fluids.CARBONDIOXIDE, 16_000);
    public final FluidTankNTM water = new FluidTankNTM(Fluids.WATER, 32_000);

    public TileEntityReactorZirnox(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ZIRNOX.get(), pos, state, 28);
    }

    @Override public String getDefaultName() { return "container.zirnox"; }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerReactorZirnox(containerId, playerInventory, this);
    }

    /** Menu button 0 toggles the core and button 1 vents a fixed 1000 mB of moderator. */
    @Override
    public void handleButtonPacket(int value, int meta) {
        if (value == 0) {
            if (redstonePowered) return;
            isOn = !isOn;
        } else if (value == 1) {
            carbonDioxide.setFill(Math.max(0, carbonDioxide.getFill() - 1_000));
        } else {
            return;
        }
        setChanged();
        networkPackNT();
    }

    public void setRedstonePowered(boolean powered) {
        if (!powered && redstonePowered) isOn = false;
        redstonePowered = powered;
        if (powered) isOn = true;
        setChanged();
    }

    @Override
    public boolean isItemValidForSlot(int slot, net.minecraft.world.item.ItemStack stack) {
        if (slot < 24) return stack.getItem() instanceof ItemZirnoxRod;
        if (slot == 24) return providesFluid(stack, Fluids.CARBONDIOXIDE);
        if (slot == 25) return providesFluid(stack, Fluids.WATER);
        return false;
    }

    private static boolean providesFluid(net.minecraft.world.item.ItemStack stack, com.hbm.inventory.fluid.FluidType fluid) {
        return stack.getItem() instanceof IFillableItem fillable && fillable.providesFluid(fluid, stack)
                || stack.getItem() instanceof ItemFluidBucket && ItemFluidBucket.getFluid(stack) == fluid;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityReactorZirnox te) {
        if (level.getGameTime() % 20 == 0) {
            for (DirPos port : te.getConPos()) {
                te.trySubscribe(te.water.getTankType(), level, port);
                te.trySubscribe(te.carbonDioxide.getTankType(), level, port);
            }
        }
        if (te.redstonePowered) te.isOn = true;
        te.loadTank(24, 26, te.carbonDioxide, Fluids.CARBONDIOXIDE);
        te.loadTank(25, 27, te.water, Fluids.WATER);
        if (te.isOn) te.decayRods();

        te.pressure = te.carbonDioxide.getFill() * 2
                + (int) ((float) te.heat * ((float) te.carbonDioxide.getFill() / te.carbonDioxide.getMaxFill()));
        if (te.heat > 0 && te.heat < MAX_HEAT) {
            if (te.water.getFill() > 0 && te.carbonDioxide.getFill() > 0 && te.steam.getFill() < te.steam.getMaxFill()) {
                // 1.7.10 generateSteam: only above 10,256 heat, factor 25 * mult with mult = 7.5F ("was 5
                // originally" — the 125 factor the port carried was the pre-7.5 value).
                if (te.heat > 10_256) {
                    float mult = 7.5F;
                    int cycle = (int) (((te.heat - 10_256F) / MAX_HEAT)
                            * Math.min(te.carbonDioxide.getFill() / 14_000F, 1F) * 25F * mult);
                    te.water.setFill(Math.max(0, te.water.getFill() - cycle));
                    te.steam.setFill(Math.min(te.steam.getMaxFill(), te.steam.getFill() + cycle));
                }
                te.heat -= (int) ((float) te.heat * te.pressure / 1_000_000F);
            } else {
                te.heat -= 10; // 1.7: no clamp; heat may dip below 0, which ends this branch until reheated
            }

            if (level.getGameTime() % 100 == 0)
                com.hbm.saveddata.satellites.SatelliteRayScan.reportEvent(level, pos.getX(), pos.getY(), pos.getZ(),
                        com.hbm.saveddata.satellites.SatelliteRayScan.RayEvent.INFO_NUCLEAR, 200);
        }
        for (DirPos port : te.getConPos()) {
            te.tryProvide(te.steam, level, port);
        }
        if (te.heat > MAX_HEAT || te.pressure > MAX_PRESSURE) {
            te.meltdown(level, pos, state);
            return;
        }
        te.setChanged();
        te.networkPackNT();
    }

    private void loadTank(int inputSlot, int outputSlot, FluidTankNTM tank, FluidType expected) {
        ItemStack input = inventory.getStackInSlot(inputSlot);
        if (input.isEmpty()) return;

        // 1.7 FluidLoaderInfinite.emptyItem: an infinite item feeds the tank from its slot and is never moved or consumed
        if (com.hbm.tileentity.machine.MachineFluidContainerIO.loadInfinite(tank, input)) {
            setChanged();
            return;
        }
        if (input.getItem() instanceof IFillableItem fillable) {
            int held = fillable.getFill(input);
            if (held <= 0 || fillable.getFirstFluidType(input) != expected) return;
            int addable = tank.fill(expected, held, false);
            if (addable <= 0) return;
            ItemStack result = dev.golem.core.world.ItemStacks.copyWithCount(input, 1);
            int taken = fillable.tryEmpty(expected, addable, result);
            if (taken <= 0 || !inventory.insertItem(outputSlot, result.copy(), true).isEmpty()) return;
            tank.fill(expected, taken, true);
            inventory.extractItem(inputSlot, 1, false);
            inventory.insertItem(outputSlot, result, false);
            setChanged();
        } else if (input.getItem() instanceof ItemFluidBucket && ItemFluidBucket.getFluid(input) == expected) {
            if (tank.fill(expected, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) return;
            ItemStack result = new ItemStack(Items.BUCKET);
            if (!inventory.insertItem(outputSlot, result.copy(), true).isEmpty()) return;
            tank.fill(expected, ItemFluidBucket.VOLUME, true);
            inventory.extractItem(inputSlot, 1, false);
            inventory.insertItem(outputSlot, result, false);
            setChanged();
        }
    }

    private void decayRods() {
        for (int slot = 0; slot < 24; slot++) {
            net.minecraft.world.item.ItemStack stack = inventory.getStackInSlot(slot);
            // 1.7: while on, a bred meteorite sword in a rod slot becomes the irradiated one
            if (stack.is(com.hbm.items.ModAbilityToolItems.meteorite_sword_bred.get())) {
                inventory.setStackInSlot(slot, new net.minecraft.world.item.ItemStack(
                        com.hbm.items.ModAbilityToolItems.meteorite_sword_irradiated.get()));
                continue;
            }
            if (!(stack.getItem() instanceof ItemZirnoxRod rod)) continue;
            com.hbm.api.data.ReactorDataMaps.ZirnoxFuel fuel = rod.stats();
            int operations = fuel.breeding() ? 0 : 1;
            for (int neighbour : NEIGHBOURS[slot]) {
                net.minecraft.world.item.ItemStack near = inventory.getStackInSlot(neighbour);
                if (near.getItem() instanceof ItemZirnoxRod other && !other.stats().breeding()) operations++;
            }
            for (int i = 0; i < operations; i++) {
                heat += fuel.heat();
                int life = rod.getLife(stack) + 1;
                rod.setLife(stack, life);
                if (life > fuel.maxLife()) {
                    // 1.7: thorium-232 breeds into a thorium rod, lithium into a tritium rod, every other rod depletes
                    inventory.setStackInSlot(slot, new net.minecraft.world.item.ItemStack(
                            com.hbm.api.data.ReactorDataMaps.itemOf(fuel.depletesInto())));
                    break;
                }
            }
        }
    }

    private void meltdown(Level level, BlockPos pos, BlockState state) {
        isOn = false;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            inventory.setStackInSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
        }

        // Remove the upper live shell and core before forming the two-block-tall wreck.
        BlockMultiblockCore.withoutTeardown(() -> {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = 2; dy <= 5; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        BlockPos shell = pos.offset(dx, dy, dz);
                        if (level.getBlockState(shell).getBlock() == ModMachineBlocks.machine_zirnox.get()) {
                            level.removeBlock(shell, false);
                        }
                    }
                }
            }
            level.removeBlock(pos, false);
        });

        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D,
                ModSounds.RBMK_EXPLOSION.get(), SoundSource.BLOCKS, 10.0F, 1.0F);
        level.explode(null, pos.getX() + 0.5D, pos.getY() + 3.0D, pos.getZ() + 0.5D, 12.0F,
                Level.ExplosionInteraction.BLOCK);

        ModMachineBlocks.zirnox_destroyed.get().formWreck(level, pos,
                MachineCore.facing(state).toDirection());
        spawnDebris(level, pos, DebrisType.EXCHANGER, 2);
        spawnDebris(level, pos, DebrisType.CONCRETE, 20);
        spawnDebris(level, pos, DebrisType.BLANK, 20);
        spawnDebris(level, pos, DebrisType.ELEMENT, 10);
        spawnDebris(level, pos, DebrisType.GRAPHITE, 10);
        spawnDebris(level, pos, DebrisType.SHRAPNEL, 10);
        ExplosionNukeGeneric.waste(level, pos, 35); // 1.7 TileEntityReactorZirnox.meltdown:398
        grantMeltdownAdvancement(level, pos);
    }

    private static void spawnDebris(Level level, BlockPos pos, DebrisType type, int count) {
        for (int i = 0; i < count; i++) {
            EntityZirnoxDebris debris = new EntityZirnoxDebris(level,
                    pos.getX() + 0.5D, pos.getY() + 4.0D, pos.getZ() + 0.5D, type);
            double x = level.getRandom().nextGaussian() * 0.75D;
            double z = level.getRandom().nextGaussian() * 0.75D;
            double y = 0.01D + level.getRandom().nextDouble() * 1.25D;

            if (type == DebrisType.CONCRETE) {
                x *= 0.25D;
                y += level.getRandom().nextDouble();
                z *= 0.25D;
            } else if (type == DebrisType.EXCHANGER) {
                x += 0.5D;
                y *= 0.1D;
                z += 0.5D;
            }

            debris.setDeltaMovement(x, y, z);
            level.addFreshEntity(debris);
        }
    }

    private static void grantMeltdownAdvancement(Level level, BlockPos pos) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class,
                new net.minecraft.world.phys.AABB(pos).inflate(100.0D))) {
            // 1.7 achZIRNOXBoom: every player in the core's box grown by 100
            com.hbm.advancement.HbmCriteria.detonation(player, com.hbm.advancement.DetonationTrigger.Kind.ZIRNOX);
            // 1.7: the same players are marked for BossSpawnHandler's radiation beasts
            if (com.hbm.config.MobConfig.enableElementals) com.hbm.extprop.HbmPlayerProps.getData(player).radMark = true;
        }
    }

    public List<DirPos> getConPos() {
        ForgeDirection facing = MachineCore.facing(getBlockState());
        ForgeDirection rot = facing.getRotation(ForgeDirection.UP);
        return List.of(
                new DirPos(worldPosition.offset(rot.offsetX * 3, 1, rot.offsetZ * 3), rot),
                new DirPos(worldPosition.offset(rot.offsetX * 3, 3, rot.offsetZ * 3), rot),
                new DirPos(worldPosition.offset(-rot.offsetX * 3, 1, -rot.offsetZ * 3), rot.getOpposite()),
                new DirPos(worldPosition.offset(-rot.offsetX * 3, 3, -rot.offsetZ * 3), rot.getOpposite()));
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "heat", PREFIX_VALUE + "pressure", PREFIX_VALUE + "water",
                PREFIX_VALUE + "steam", PREFIX_VALUE + "co2", PREFIX_VALUE + "state",
                PREFIX_FUNCTION + "setState" + NAME_SEPARATOR + "active (0 or 1)",
                PREFIX_FUNCTION + "ventCO2"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "heat").equals(name)) return "" + (int) Math.round(heat * 1.0E-5D * 780.0D + 20.0D);
        if ((PREFIX_VALUE + "pressure").equals(name)) return "" + (int) Math.round(pressure * 1.0E-5D * 30.0D);
        if ((PREFIX_VALUE + "water").equals(name)) return "" + water.getFill();
        if ((PREFIX_VALUE + "steam").equals(name)) return "" + steam.getFill();
        if ((PREFIX_VALUE + "co2").equals(name)) return "" + carbonDioxide.getFill();
        if ((PREFIX_VALUE + "state").equals(name)) return isOn ? "1" : "0";
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setState").equals(name) && params.length > 0) {
            if (redstonePowered) return null;
            isOn = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "ventCO2").equals(name)) {
            carbonDioxide.setFill(Math.max(carbonDioxide.getFill() - 1_000, 0));
            setChanged();
        }
        return null;
    }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{water, carbonDioxide}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{steam}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{water, steam, carbonDioxide}; }
    @Override public int[] getSlotsForFace(Direction side) { return ROD_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, net.minecraft.world.item.ItemStack stack, Direction side) { return isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, net.minecraft.world.item.ItemStack stack, Direction side) { return slot < 24 && !(stack.getItem() instanceof ItemZirnoxRod); }

    @Override public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf); buf.writeInt(heat); buf.writeInt(pressure); buf.writeBoolean(isOn); buf.writeBoolean(redstonePowered);
        steam.serialize(buf); carbonDioxide.serialize(buf); water.serialize(buf);
    }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf); heat = buf.readInt(); pressure = buf.readInt(); isOn = buf.readBoolean(); redstonePowered = buf.readBoolean();
        steam.deserialize(buf); carbonDioxide.deserialize(buf); water.deserialize(buf);
    }
    @Override protected void saveData(DataOut tag) {
        super.saveData(tag); tag.putInt("heat", heat); tag.putInt("pressure", pressure); tag.putBoolean("isOn", isOn); tag.putBoolean("redstonePowered", redstonePowered);
        steam.writeToNBT(tag, "steam"); carbonDioxide.writeToNBT(tag, "carbondioxide"); water.writeToNBT(tag, "water");
    }
    @Override protected void loadData(DataIn tag) {
        super.loadData(tag); heat = tag.getIntOr("heat", 0); pressure = tag.getIntOr("pressure", 0); isOn = tag.getBooleanOr("isOn", false); redstonePowered = tag.getBooleanOr("redstonePowered", false);
        steam.readFromNBT(tag, "steam"); carbonDioxide.readFromNBT(tag, "carbondioxide"); water.readFromNBT(tag, "water");
    }
}
