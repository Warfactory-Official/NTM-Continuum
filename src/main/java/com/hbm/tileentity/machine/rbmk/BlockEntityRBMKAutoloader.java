package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ICopiable;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerRBMKAutoloader;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModDoorSounds;
import com.hbm.tileentity.ModRBMKBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code TileEntityRBMKAutoloader}: a nine-tall piston tower standing on an RBMK fuel channel. Every
 * second, while retracted, it checks whether the rod below is cold enough and either empty or depleted
 * below the configured cycle percentage; if so it lowers its piston (four blocks at 0.005 per tick), swaps
 * the spent rod into its output grid, inserts a fresh one and rises again. Slots 0-8 take fresh rods,
 * 9-17 receive spent ones.
 */
public class BlockEntityRBMKAutoloader extends TileEntityMachineBase implements MenuProvider, IControlReceiver,
        ICopiable, WorldlyContainer, IRBMKTicking {

    public static final int SLOTS = 18;
    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17};
    private static final int[] NO_SLOTS = {};
    public static double speed = 0.005D;

    public double piston, renderPiston, lastPiston;
    private double syncPiston;
    private int turnProgress;
    private boolean isRetracting = true;
    private int delay = 0;
    public int cycle = 50;
    private @Nullable AudioWrapper audioLift;

    public BlockEntityRBMKAutoloader(BlockPos pos, BlockState state) {
        super(ModRBMKBlockEntities.RBMK_AUTOLOADER.get(), pos, state, SLOTS);
    }

    @Override public String getDefaultName() { return "container.rbmkAutoloader"; }

    @Override public void tickServer() {
        if (delay > 0) delay--;

        if (delay <= 0 && isRetracting && piston > 0D) {
            piston -= speed;
            if (piston <= 0) { piston = 0; delay = 40; }
        }

        // check for connected fuel rod and decide whether to begin working
        if (isRetracting && level.getGameTime() % 20 == 0 && hasFuel() && hasSpace()) {
            BlockEntityRBMKRod rod = rodBelow();
            if (rod != null && coldEnoughForAutoloader(rod)) {
                ItemStack loaded = rod.inventory.getStackInSlot(0);
                if (loaded.isEmpty() || (loaded.getItem() instanceof ItemRBMKRod && ItemRBMKRod.getEnrichment(loaded) * 100 < cycle)) {
                    isRetracting = false;
                }
            }
        }

        if (delay <= 0 && !isRetracting && piston < 1D) {
            piston += speed;
            if (piston >= 1) { piston = 1; delay = 40; }
        }

        // once the piston is fully extended
        if (!isRetracting && piston >= 1D) {
            piston = 1D;
            BlockEntityRBMKRod rod = rodBelow();
            if (rod != null) {
                // try to take out the old fuel rod
                if (!rod.inventory.getStackInSlot(0).isEmpty() && hasSpace()) {
                    for (int i = 9; i < 18; i++) {
                        if (inventory.getStackInSlot(i).isEmpty()) {
                            inventory.setStackInSlot(i, rod.inventory.getStackInSlot(0).copy());
                            rod.inventory.setStackInSlot(0, ItemStack.EMPTY);
                            break;
                        }
                    }
                }
                // if there's space, try and insert a new fuel rod
                if (rod.inventory.getStackInSlot(0).isEmpty()) {
                    for (int i = 0; i < 9; i++) {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if (!stack.isEmpty() && stack.getItem() instanceof ItemRBMKRod && ItemRBMKRod.getEnrichment(stack) * 100 >= cycle) {
                            rod.inventory.setStackInSlot(0, stack.copy());
                            inventory.setStackInSlot(i, ItemStack.EMPTY);
                            break;
                        }
                    }
                }
                rod.markChanged();
                isRetracting = true;
                delay = 40;
            }
        }

        networkPackNT();
    }

    @Override public void tickClient() {
        lastPiston = renderPiston;
        if (turnProgress > 0) {
            renderPiston = renderPiston + (syncPiston - renderPiston) / (double) turnProgress;
            --turnProgress;
        } else {
            renderPiston = syncPiston;
        }

        float x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        if (renderPiston > 0.01 && renderPiston < 0.99) {
            if (audioLift == null || !audioLift.isPlaying()) {
                audioLift = AudioSystem.getLoopedSound(ModDoorSounds.WGH_START.get(), SoundSource.BLOCKS, x, y, z,
                        getVolume(0.75F), 25F, 1.0F, 5);
                if (audioLift != null) audioLift.startSound();
            }
            if (audioLift != null) {
                audioLift.updateVolume(getVolume(0.75F));
                audioLift.keepAlive();
            }
        } else if (audioLift != null) {
            audioLift.stopSound();
            audioLift = null;
            level.playLocalSound(x, y, z, ModDoorSounds.WGH_STOP.get(), SoundSource.BLOCKS, getVolume(2F), 1F, false);
        }

        if (renderPiston > 0.99) {
            for (int i = 0; i < 3; i++) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "tower");
                data.putFloat("lift", 0F);
                data.putFloat("base", 0.25F);
                data.putFloat("max", 1.5F);
                data.putInt("life", 70 + level.getRandom().nextInt(30));
                data.putBoolean("noWind", true);
                // The original wrote "alphaMod", a key the tower particle never reads; kept as-is.
                data.putFloat("alphaMod", 2F);
                data.putFloat("strafe", 0.05F);
                HbmEffectNT.Tower.summonParticle(level, x + 0.5 + level.getRandom().nextGaussian() * 0.125, y + 0.25,
                        z + 0.5 + level.getRandom().nextGaussian() * 0.125, data);
            }
        }
    }

    /** 1.7 {@code TileEntityRBMKRod.coldEnoughForAutoloader}: a loaded rod's hull must be at or below 1000°C. */
    private static boolean coldEnoughForAutoloader(BlockEntityRBMKRod rod) {
        ItemStack stack = rod.inventory.getStackInSlot(0);
        return !(stack.getItem() instanceof ItemRBMKRod) || ItemRBMKRod.getHullHeat(stack) <= 1_000;
    }

    private @Nullable BlockEntityRBMKRod rodBelow() {
        BlockPos below = worldPosition.below();
        if (!(level.getBlockState(below).getBlock() instanceof RBMKBase)) return null;
        BlockPos core = MultiblockSurface.coreOfAny(level, below);
        BlockEntity tile = core == null ? null : level.getBlockEntity(core);
        return tile instanceof BlockEntityRBMKRod rod ? rod : null;
    }

    public boolean hasFuel() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ItemRBMKRod && ItemRBMKRod.getEnrichment(stack) * 100 >= cycle) return true;
        }
        return false;
    }

    public boolean hasSpace() {
        for (int i = 9; i < 18; i++) if (inventory.getStackInSlot(i).isEmpty()) return true;
        return false;
    }

    @Override public boolean isItemValidForSlot(int i, ItemStack stack) {
        return stack.getItem() instanceof ItemRBMKRod && ItemRBMKRod.getEnrichment(stack) * 100 >= cycle && i < 9;
    }

    // --- WorldlyContainer: only while the piston is fully up; input slots take fresh rods, output is taken ---
    @Override public int[] getSlotsForFace(Direction side) { return piston <= 0 ? ALL_SLOTS : NO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 9; }

    @Override public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(piston);
        buf.writeInt(cycle);
    }

    @Override public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        syncPiston = buf.readDouble();
        cycle = buf.readInt();
        turnProgress = 2;
    }

    @Override protected void saveData(DataOut output) {
        super.saveData(output);
        output.putDouble("piston", piston);
        output.putBoolean("ret", isRetracting);
        output.putInt("delay", delay);
        output.putInt("cycle", cycle);
    }

    @Override protected void loadData(DataIn input) {
        super.loadData(input);
        piston = input.getDoubleOr("piston", 0D);
        isRetracting = input.getBooleanOr("ret", false);
        delay = input.getIntOr("delay", 0);
        cycle = input.getIntOr("cycle", 0);
    }

    @Override public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerRBMKAutoloader menu && menu.loader() == this && menu.stillValid(player);
    }

    @Override public void receiveControl(CompoundTag data) {
        if (Tags.contains(data, "minus") && cycle > 5) cycle -= 5;
        if (Tags.contains(data, "plus") && cycle < 95) cycle += 5;
        cycle = Mth.clamp(cycle, 5, 95);
        markChanged();
    }

    @Override public CompoundTag getSettings(Level world, BlockPos pos) {
        CompoundTag data = new CompoundTag();
        data.putInt("cycle", cycle);
        return data;
    }

    @Override public void pasteSettings(CompoundTag nbt, int index, Level world, Player player, BlockPos pos) {
        if (Tags.contains(nbt, "cycle")) cycle = Mth.clamp(Tags.getIntOr(nbt, "cycle", cycle), 5, 95);
    }

    @Override public Component getDisplayName() { return Component.translatable(getName()); }

    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerRBMKAutoloader(id, inventory, this);
    }

    @Override public void setRemoved() {
        super.setRemoved();
        if (audioLift != null) {
            audioLift.stopSound();
            audioLift = null;
        }
    }
}
