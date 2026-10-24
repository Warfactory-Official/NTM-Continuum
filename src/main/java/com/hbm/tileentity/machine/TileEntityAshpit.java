package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.inventory.container.ContainerAshpit;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ModIngredientItems;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockState;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import com.hbm.tileentity.IConfigurableMachine;

/** Five-output ash collector for firebox ash and chimney fly ash/soot. */
public class TileEntityAshpit extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

    public static final int SLOT_COUNT = 5;
    private static final int[] ALL_SLOTS = {0, 1, 2, 3, 4};

    public int playersUsing;
    public float doorAngle;
    public float prevDoorAngle;
    public boolean isFull;

    public int ashLevelWood;
    public int ashLevelCoal;
    public int ashLevelMisc;
    public int ashLevelFly;
    public int ashLevelSoot;

    public static int thresholdWood = 2_000;
    public static int thresholdCoal = 2_000;
    public static int thresholdMisc = 2_000;
    public static int thresholdFly = 2_000;
    public static int thresholdSoot = 8_000;

    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "ashpit"; }

        @Override public void readIfPresent(JsonObject obj) {
            thresholdWood = IConfigurableMachine.grab(obj, "I:thresholdWood", thresholdWood);
            thresholdCoal = IConfigurableMachine.grab(obj, "I:thresholdCoal", thresholdCoal);
            thresholdMisc = IConfigurableMachine.grab(obj, "I:thresholdMisc", thresholdMisc);
            thresholdFly = IConfigurableMachine.grab(obj, "I:thresholdFly", thresholdFly);
            thresholdSoot = IConfigurableMachine.grab(obj, "I:thresholdSoot", thresholdSoot);
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("I:thresholdWood").value(thresholdWood);
            writer.name("I:thresholdCoal").value(thresholdCoal);
            writer.name("I:thresholdMisc").value(thresholdMisc);
            writer.name("I:thresholdFly").value(thresholdFly);
            writer.name("I:thresholdSoot").value(thresholdSoot);
        }
    };

    public TileEntityAshpit(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ASHPIT.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public String getDefaultName() {
        return "container.ashpit";
    }

    @Override
    protected boolean syncFullTagOnChunkLoad() {
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   TileEntityAshpit ashpit) {
        if (ashpit.processAsh(ashpit.ashLevelWood, EnumAshType.WOOD, thresholdWood)) ashpit.ashLevelWood -= thresholdWood;
        if (ashpit.processAsh(ashpit.ashLevelCoal, EnumAshType.COAL, thresholdCoal)) ashpit.ashLevelCoal -= thresholdCoal;
        if (ashpit.processAsh(ashpit.ashLevelMisc, EnumAshType.MISC, thresholdMisc)) ashpit.ashLevelMisc -= thresholdMisc;
        if (ashpit.processAsh(ashpit.ashLevelFly, EnumAshType.FLY, thresholdFly)) ashpit.ashLevelFly -= thresholdFly;
        if (ashpit.processAsh(ashpit.ashLevelSoot, EnumAshType.SOOT, thresholdSoot)) ashpit.ashLevelSoot -= thresholdSoot;

        ashpit.isFull = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (!ashpit.inventory.getStackInSlot(i).isEmpty()) ashpit.isFull = true;
        }

        ashpit.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                   TileEntityAshpit ashpit) {
        ashpit.prevDoorAngle = ashpit.doorAngle;
        float swingSpeed = ashpit.doorAngle / 10F + 3F;
        if (ashpit.playersUsing > 0) ashpit.doorAngle += swingSpeed;
        else ashpit.doorAngle -= swingSpeed;
        ashpit.doorAngle = Mth.clamp(ashpit.doorAngle, 0F, 135F);
    }

    protected boolean processAsh(int level, EnumAshType type, int threshold) {
        if (level < threshold) return false;

        ItemStack ash = new ItemStack(ModIngredientItems.ash(type));
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack existing = inventory.getStackInSlot(i);
            if (existing.isEmpty()) {
                inventory.setStackInSlot(i, ash.copy());
                // This extra subtraction names the wood counter for every type.
                ashLevelWood -= threshold;
                return true;
            }
            if (existing.is(ash.getItem()) && existing.getCount() < existing.getMaxStackSize()) {
                existing.grow(1);
                inventory.setStackInSlot(i, existing);
                return true;
            }
        }
        return false;
    }

    public void setPlayersUsing(int count) {
        playersUsing = Math.max(0, count);
    }

    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return false; }
    @Override public int[] getSlotsForFace(Direction side) { return ALL_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getDefaultName());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerAshpit(id, inventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(playersUsing);
        buf.writeBoolean(isFull);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        playersUsing = buf.readInt();
        isFull = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("ashLevelWood", ashLevelWood);
        output.putInt("ashLevelCoal", ashLevelCoal);
        output.putInt("ashLevelMisc", ashLevelMisc);
        output.putInt("ashLevelFly", ashLevelFly);
        output.putInt("ashLevelSoot", ashLevelSoot);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        ashLevelWood = input.getIntOr("ashLevelWood", 0);
        ashLevelCoal = input.getIntOr("ashLevelCoal", 0);
        ashLevelMisc = input.getIntOr("ashLevelMisc", 0);
        ashLevelFly = input.getIntOr("ashLevelFly", 0);
        ashLevelSoot = input.getIntOr("ashLevelSoot", 0);
    }
}
