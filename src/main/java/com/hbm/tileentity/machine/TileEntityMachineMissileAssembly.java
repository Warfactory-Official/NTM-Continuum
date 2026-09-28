package com.hbm.tileentity.machine;

import com.hbm.handler.MissileStruct;
import com.hbm.inventory.container.ContainerMachineMissileAssembly;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModCustomMissileBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.bomb.TileEntityCustomMissileLauncher;
import dev.golem.core.data.DataIn;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code TileEntityMachineMissileAssembly}: five part slots (chip, warhead, fuselage, fins, thruster) and an
 * output. The GUI's button runs {@link #construct}, which fails silently unless every rule below holds: the
 * warhead's bottom matches the fuselage top and weighs no more than the thruster lifts, the thruster matches the
 * fuselage bottom and fuel, and fins, if present, match the fuselage bottom. Fins that do not match block it too.
 */
public class TileEntityMachineMissileAssembly extends TileEntityMachineBase implements MenuProvider {

    /** Client side: the struct the rack draws (1.7 {@code TEMissileMultipartPacket}). */
    public @Nullable MissileStruct load;

    public TileEntityMachineMissileAssembly(BlockPos pos, BlockState state) {
        super(ModCustomMissileBlockEntities.MACHINE_MISSILE_ASSEMBLY.get(), pos, state, 6);
    }

    @Override
    public String getDefaultName() {
        return "container.missileAssembly";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineMissileAssembly te) {
        te.networkPackNT();
    }

    private @Nullable ItemCustomMissilePart part(int slot) {
        return ItemCustomMissilePart.part(inventory.getStackInSlot(slot));
    }

    public int fuselageState() {
        ItemCustomMissilePart part = part(2);
        return part != null && part.type == PartType.FUSELAGE ? 1 : 0;
    }

    public int chipState() {
        ItemCustomMissilePart part = part(0);
        return part != null && part.type == PartType.CHIP ? 1 : 0;
    }

    public int warheadState() {
        ItemCustomMissilePart warhead = part(1), fuselage = part(2), thruster = part(4);
        if (warhead != null && fuselage != null && thruster != null && warhead.type == PartType.WARHEAD
                && fuselage.type == PartType.FUSELAGE && thruster.type == PartType.THRUSTER) {
            float weight = (Float) warhead.attributes[2];
            float thrust = (Float) thruster.attributes[2];
            if (warhead.bottom == fuselage.top && weight <= thrust) return 1;
        }
        return 0;
    }

    /** -1 = no fins (allowed), 1 = fins fit, 0 = fins that do not fit. */
    public int stabilityState() {
        if (inventory.getStackInSlot(3).isEmpty()) return -1;
        ItemCustomMissilePart fins = part(3), fuselage = part(2);
        if (fins != null && fuselage != null && fins.top == fuselage.bottom && fins.type == PartType.FINS) return 1;
        return 0;
    }

    public int thrusterState() {
        ItemCustomMissilePart thruster = part(4), fuselage = part(2);
        if (thruster != null && fuselage != null && thruster.type == PartType.THRUSTER && fuselage.type == PartType.FUSELAGE
                && thruster.top == fuselage.bottom && thruster.attributes[0] == fuselage.attributes[0]) {
            return 1;
        }
        return 0;
    }

    public boolean canBuild() {
        if (inventory.getStackInSlot(5).isEmpty() && chipState() == 1 && warheadState() == 1 && fuselageState() == 1
                && thrusterState() == 1) {
            return stabilityState() != 0;
        }
        return false;
    }

    public void construct() {
        if (!canBuild()) return;

        inventory.setStackInSlot(5, ItemCustomMissile.buildMissile(inventory.getStackInSlot(0), inventory.getStackInSlot(1),
                inventory.getStackInSlot(2), inventory.getStackInSlot(3), inventory.getStackInSlot(4)).copy());

        if (stabilityState() == 1) inventory.setStackInSlot(3, ItemStack.EMPTY);
        inventory.setStackInSlot(0, ItemStack.EMPTY);
        inventory.setStackInSlot(1, ItemStack.EMPTY);
        inventory.setStackInSlot(2, ItemStack.EMPTY);
        inventory.setStackInSlot(4, ItemStack.EMPTY);

        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                ModSounds.MISSILE_ASSEMBLY_2.get(), SoundSource.BLOCKS, 1F, 1F);
        setChanged();
    }

    /** 1.7 AuxButtonPacket to the assembly: any id builds. */
    @Override
    public void handleButtonPacket(int value, int meta) {
        construct();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{0};
    }

    public MissileStruct currentStruct() {
        return new MissileStruct(inventory.getStackInSlot(1), inventory.getStackInSlot(2), inventory.getStackInSlot(3),
                inventory.getStackInSlot(4));
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        TileEntityCustomMissileLauncher.writeStruct(buf, currentStruct());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        load = TileEntityCustomMissileLauncher.readStruct(buf);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        load = currentStruct();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineMissileAssembly(containerId, playerInventory, this);
    }
}
