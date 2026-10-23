package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.client.sound.ArcFurnaceSoundHandler;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.container.ContainerMachineArcFurnaceLarge;
import com.hbm.inventory.recipes.ArcFurnaceRecipes;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.items.machine.ItemArcElectrode;
import com.hbm.items.machine.ItemArcElectrodeBurnt;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.sound.ModDoorSounds;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.CrucibleUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.MachineAudio;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.items.ModMachineItems;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TilePort.PortDef;

/** Large Arc Furnace core: the old 20-cell grid, five-cell queue, three electrodes and liquid store. */
public class TileEntityMachineArcFurnaceLarge extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IControlReceiver, WorldlyContainer, IUpgradeInfoProvider {

    public static final long MAX_POWER = 2_500_000L;
    public static final int PROCESSING_TIME = 400;
    public static final int MAX_LIQUID = MaterialShapes.BLOCK.quantity * 128;
    public static final long maxPower = MAX_POWER;
    public static final int maxLiquid = MAX_LIQUID;
    public static final byte ELECTRODE_NONE = 0;
    public static final byte ELECTRODE_FRESH = 1;
    public static final byte ELECTRODE_USED = 2;
    public static final byte ELECTRODE_DEPLETED = 3;

    public long power;
    public float progress;
    public boolean isProgressing;
    public boolean hasMaterial;
    public boolean liquidMode;
    public int delay;
    public int upgrade;
    public float lid;
    private AudioWrapper audioLid;
    public float prevLid;
    public int approachNum;
    public float syncLid;
    public final byte[] electrodes = new byte[3];
    public List<Mats.MaterialStack> liquids = new ArrayList<>();

    /** Synced stream state; the server timer is not persisted. */
    public int pourColor = -1;
    public float pourLen;
    private int pourTimer;
    private static final int STREAM_LIFETIME = 15;

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineArcFurnaceLarge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ARC_FURNACE.get(), pos, state, 30);
        ArcFurnaceRecipes.register();
    }

    @Override
    protected NTMItemHandler createInventory(int slotCount) {
        return new NTMItemHandler(slotCount) {
            @Override public int getSlotLimit(int slot) { return slot < 5 ? 1 : super.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                return TileEntityMachineArcFurnaceLarge.this.isItemValidForSlot(slot, stack);
            }
            @Override protected void onContentsChanged(int slot) {
                TileEntityMachineArcFurnaceLarge.this.setChanged();
                TileEntityMachineArcFurnaceLarge.this.onSlotContentsChanged(slot, getStackInSlot(slot));
            }
        };
    }

    @Override public String getDefaultName() { return "container.machineArcFurnaceLarge"; }
    @Override public Component getDisplayName() { return Component.translatable(getDefaultName()); }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  TileEntityMachineArcFurnaceLarge furnace) {
        furnace.setupPowerPorts(furnace.getPorts());
        furnace.updateAllPorts();
        furnace.receivePower();

        furnace.upgradeManager.checkSlots(furnace.inventory, 4, 4);
        furnace.upgrade = furnace.upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.SPEED);
        furnace.power = Library.chargeTEFromItems(furnace.inventory, 3, furnace.power, MAX_POWER);

        if (furnace.lid > 0F) furnace.loadIngredients();

        furnace.isProgressing = false;
        if (furnace.power > 0) {
            boolean ingredients = furnace.hasIngredients();
            boolean electrodes = furnace.hasElectrodes();
            int consumption = (int) (1_000 * Math.pow(5, furnace.upgrade));

            if (ingredients && electrodes && furnace.delay <= 0 && furnace.liquids.isEmpty()) {
                if (furnace.lid > 0F) {
                    furnace.lid -= (float) (1F / (60F / (furnace.upgrade * 0.5F + 1F)));
                    if (furnace.lid < 0F) furnace.lid = 0F;
                    furnace.progress = 0F;
                } else if (furnace.power >= consumption) {
                    int duration = PROCESSING_TIME / (furnace.upgrade * 2 + 1);
                    furnace.progress += 1F / duration;
                    furnace.isProgressing = true;
                    furnace.power -= consumption;
                    if (furnace.progress >= 1F) {
                        furnace.process();
                        furnace.progress = 0F;
                        furnace.delay = (int) (120F / (furnace.upgrade * 0.5F + 1F));
                        furnace.setChanged();
                        PollutionHandler.incrementPollution(level, pos, PollutionType.SOOT, 10F);
                    }
                }
            } else {
                if (furnace.delay > 0) furnace.delay--;
                furnace.progress = 0F;
                if (furnace.lid < 1F) {
                    furnace.lid += (float) (1F / (60F / (furnace.upgrade * 0.5F + 1F)));
                    if (furnace.lid > 1F) furnace.lid = 1F;
                }
            }
            furnace.hasMaterial = ingredients;
        }

        furnace.decideElectrodeState();
        if (!furnace.hasMaterial) furnace.hasMaterial = furnace.hasIngredients();

        if (!furnace.liquids.isEmpty() && furnace.lid > 0F) {
            ForgeDirection dir = furnace.facing();
            Vec3[] impact = new Vec3[1];
            double sx = pos.getX() + 0.5D + dir.offsetX * 2.875D;
            double sy = pos.getY() + 1.25D;
            double sz = pos.getZ() + 0.5D + dir.offsetZ * 2.875D;
            Mats.MaterialStack poured = CrucibleUtil.pourFullStack(level, sx, sy, sz, 6D, true,
                    furnace.liquids, MaterialShapes.INGOT.quantity, impact);
            if (poured != null && impact[0] != null) {
                furnace.pourColor = poured.material.moltenColor;
                furnace.pourLen = Math.max(1F, pos.getY() + 1F - (float) (Math.ceil(impact[0].y) - 0.875D));
                furnace.pourTimer = STREAM_LIFETIME;
            }
        }

        furnace.liquids.removeIf(stack -> stack.amount <= 0);
        if (furnace.pourTimer > 0) furnace.pourTimer--;
        else furnace.pourColor = -1;
        furnace.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state,
                                  TileEntityMachineArcFurnaceLarge furnace) {
        float previous = furnace.prevLid;
        furnace.prevLid = furnace.lid;
        if (furnace.approachNum > 0) {
            furnace.lid += (furnace.syncLid - furnace.lid) / furnace.approachNum;
            furnace.approachNum--;
        } else {
            furnace.lid = furnace.syncLid;
        }
        if (furnace.lid != previous && furnace.lid > previous
                && !(previous == 0F && furnace.lid == 1F)
                && level.players().stream().anyMatch(player -> player.distanceToSqr(
                pos.getX() + 0.5D, pos.getY() + 4D, pos.getZ() + 0.5D) < 2_500D)) {
            CompoundTag data = new CompoundTag();
            data.putFloat("lift", 0.01F);
            data.putFloat("base", 0.5F);
            data.putFloat("max", 2F);
            data.putInt("life", 70 + level.getRandom().nextInt(30));
            data.putBoolean("noWind", true);
            data.putFloat("alphaMod", previous / furnace.lid);
            data.putInt("color", 0);
            data.putFloat("strafe", 0.05F);
            for (int i = 0; i < 3; i++) {
                HbmEffectNT.Tower.summonParticle(level,
                        pos.getX() + 0.5D + level.getRandom().nextGaussian() * 0.5D,
                        pos.getY() + 4D,
                        pos.getZ() + 0.5D + level.getRandom().nextGaussian() * 0.5D, data);
            }
        }
        furnace.audioLid = MachineAudio.tick(furnace.audioLid, furnace.lid != furnace.prevLid,
                ModDoorSounds.WGH_START.get(), SoundSource.BLOCKS, pos, furnace.getVolume(0.75F), 15F, 1.0F, 5);

        if ((furnace.lid == 1F || furnace.lid == 0F) && furnace.lid != furnace.prevLid
                && !(furnace.prevLid == 0F && furnace.lid == 1F)) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    ModDoorSounds.WGH_STOP.get(), SoundSource.BLOCKS, furnace.getVolume(1F), 1F, false);
        }

        ArcFurnaceSoundHandler.tickLoop(furnace);
    }

    /** Moves queue slots 25..29 into the 20-cell grid 5..24, capped by the active recipe. */
    public void loadIngredients() {
        boolean changed = false;
        for (int queue = 25; queue < 30; queue++) {
            ItemStack queued = inventory.getStackInSlot(queue);
            if (queued.isEmpty()) continue;
            ArcFurnaceRecipes.ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(queued, liquidMode, level);
            if (recipe == null) continue;

            int max = getMaxInputSize();
            int recipeMax = liquidMode ? max : queued.getMaxStackSize() / recipe.solidOutput.getCount();
            max = Math.min(max, recipeMax);

            for (int slot = 5; slot < 25 && !queued.isEmpty(); slot++) {
                ItemStack ingredient = inventory.getStackInSlot(slot);
                if (ingredient.isEmpty() || !ItemStacks.sameItemAndData(queued, ingredient)) continue;
                int toMove = Math.min(Math.min(ingredient.getMaxStackSize() - ingredient.getCount(), queued.getCount()),
                        Math.max(0, max - ingredient.getCount()));
                if (toMove > 0) {
                    ingredient.grow(toMove);
                    inventory.setStackInSlot(slot, ingredient);
                    inventory.extractItem(queue, toMove, false);
                    queued = inventory.getStackInSlot(queue);
                    changed = true;
                }
            }

            for (int slot = 5; slot < 25 && !queued.isEmpty(); slot++) {
                if (!inventory.getStackInSlot(slot).isEmpty()) continue;
                int toMove = Math.min(max, queued.getCount());
                inventory.setStackInSlot(slot, queued.copyWithCount(toMove));
                inventory.extractItem(queue, toMove, false);
                queued = inventory.getStackInSlot(queue);
                changed = true;
            }
        }
        if (changed) setChanged();
    }

    public void decideElectrodeState() {
        for (int slot = 0; slot < 3; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof ItemArcElectrodeBurnt) {
                electrodes[slot] = ELECTRODE_DEPLETED;
            } else if (stack.getItem() instanceof ItemArcElectrode electrode) {
                electrodes[slot] = isProgressing || ItemArcElectrode.getDurability(stack) > 0
                        ? ELECTRODE_USED : ELECTRODE_FRESH;
            } else {
                electrodes[slot] = ELECTRODE_NONE;
            }
        }
    }

    public void process() {
        for (int slot = 5; slot < 25; slot++) {
            ItemStack input = inventory.getStackInSlot(slot);
            if (input.isEmpty()) continue;
            ArcFurnaceRecipes.ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(input, liquidMode, level);
            if (recipe == null) continue;

            if (!liquidMode && recipe.solidOutput != null) {
                int count = input.getCount();
                inventory.setStackInSlot(slot, recipe.solidOutput.copyWithCount(recipe.solidOutput.getCount() * count));
            }

            if (liquidMode && recipe.fluidOutput.length > 0) {
                while (!inventory.getStackInSlot(slot).isEmpty()) {
                    int current = getStackAmount(liquids);
                    int addition = getStackAmount(recipe.fluidOutput);
                    if (current + addition > MAX_LIQUID) break;
                    inventory.extractItem(slot, 1, false);
                    for (Mats.MaterialStack output : recipe.fluidOutput) addToStack(output);
                }
            }
        }

        for (int slot = 0; slot < 3; slot++) {
            ItemStack electrode = inventory.getStackInSlot(slot);
            if (electrode.getItem() instanceof ItemArcElectrode && ItemArcElectrode.damage(electrode)) {
                ItemArcElectrode.ElectrodeType type = ((ItemArcElectrode) electrode.getItem()).type;
                inventory.setStackInSlot(slot, new ItemStack(ModMachineItems.arcElectrodeBurnt(type)));
            }
        }
    }

    public boolean hasIngredients() {
        for (int slot = 5; slot < 25; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            ArcFurnaceRecipes.ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(stack, liquidMode, level);
            if (recipe != null && (liquidMode ? recipe.fluidOutput.length > 0 : recipe.solidOutput != null)) return true;
        }
        return false;
    }

    /** 1.7: what is left of {@code is} once AE2 fills the electrode slots and the grid; unmodulated it only predicts. */
    public ItemStack distributeInput(ItemStack is, boolean modulate) {
        is = is.copy();
        if (is.isEmpty()) return ItemStack.EMPTY;
        if (is.getItem() instanceof ItemArcElectrode) {
            for (int i = 0; i < 3; i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    ItemStack split = is.split(1);
                    if (modulate) inventory.setStackInSlot(i, split);
                }
                if (is.isEmpty()) return ItemStack.EMPTY;
            }
            return is;
        }
        ArcFurnaceRecipes.ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(is, liquidMode, level);
        if (recipe == null) return is;
        int maxStackSize = liquidMode ? 64 : recipe.solidOutput.getMaxStackSize() / recipe.solidOutput.getCount();
        maxStackSize = Math.min(maxStackSize, Math.min(is.getMaxStackSize(), getMaxInputSize()));
        for (int i = 5; i < 25; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (slot.isEmpty()) {
                if (is.getCount() <= maxStackSize) {
                    if (modulate) inventory.setStackInSlot(i, is);
                    return ItemStack.EMPTY;
                }
                ItemStack split = is.split(maxStackSize);
                if (modulate) inventory.setStackInSlot(i, split);
            } else if (ItemStacks.sameItemAndData(is, slot) && slot.getCount() < maxStackSize) {
                int room = maxStackSize - slot.getCount();
                if (is.getCount() <= room) {
                    if (modulate) inventory.setStackInSlot(i, ItemStacks.copyWithCount(slot, slot.getCount() + is.getCount()));
                    return ItemStack.EMPTY;
                }
                is.shrink(room);
                if (modulate) inventory.setStackInSlot(i, ItemStacks.copyWithCount(slot, maxStackSize));
            }
        }
        return is;
    }

    /** 1.7: the part of {@code is} AE2 may take, burnt electrodes from 0..2 and finished items from the grid. */
    public ItemStack collectRequested(ItemStack is, boolean modulate) {
        int req = is.getCount();
        if (req == 0) return ItemStack.EMPTY;
        if (!(is.getItem() instanceof ItemArcElectrode)) {
            for (int i = 0; i < 3 && req > 0; i++) req = take(i, is, req, modulate);
        }
        if (ArcFurnaceRecipes.getOutput(is, liquidMode, level) == null) {
            for (int i = 5; i < 25 && req > 0; i++) req = take(i, is, req, modulate);
        }
        return req == is.getCount() ? ItemStack.EMPTY : ItemStacks.copyWithCount(is, is.getCount() - req);
    }

    private int take(int slot, ItemStack is, int req, boolean modulate) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty() || !ItemStacks.sameItemAndData(is, stack)) return req;
        int taken = Math.min(req, stack.getCount());
        if (modulate) {
            inventory.setStackInSlot(slot, taken == stack.getCount() ? ItemStack.EMPTY
                    : ItemStacks.copyWithCount(stack, stack.getCount() - taken));
        }
        return req - taken;
    }

    /** 1.7: the stack AE2 lists for {@code slot}, never fresh electrodes, the lid slots or unprocessed grid input. */
    public ItemStack getAvailableItemFromSlot(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty() || slot == 3 || slot == 4) return ItemStack.EMPTY;
        if (slot < 3 && stack.getItem() instanceof ItemArcElectrode) return ItemStack.EMPTY;
        if (slot > 4 && ArcFurnaceRecipes.getOutput(stack, liquidMode, level) != null) return ItemStack.EMPTY;
        return stack;
    }

    public boolean hasElectrodes() {
        for (int slot = 0; slot < 3; slot++) {
            if (!(inventory.getStackInSlot(slot).getItem() instanceof ItemArcElectrode)) return false;
        }
        return true;
    }

    public int getMaxInputSize() {
        return switch (upgrade) {
            case 1 -> 4;
            case 2 -> 8;
            case 3 -> 16;
            default -> 1;
        };
    }

    public void addToStack(Mats.MaterialStack addition) {
        for (Mats.MaterialStack stack : liquids) {
            if (stack.material == addition.material) {
                stack.amount += addition.amount;
                return;
            }
        }
        liquids.add(addition.copy());
    }

    public static int getStackAmount(List<Mats.MaterialStack> stack) {
        int amount = 0;
        for (Mats.MaterialStack material : stack) amount += material.amount;
        return amount;
    }

    public static int getStackAmount(Mats.MaterialStack[] stack) {
        int amount = 0;
        for (Mats.MaterialStack material : stack) amount += material.amount;
        return amount;
    }

    protected PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = facing();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + dir.offsetX * 2 + rot.offsetX, y, z + dir.offsetZ * 2 + rot.offsetZ, dir),
                    PortDef.make(x + dir.offsetX * 2 - rot.offsetX, y, z + dir.offsetZ * 2 - rot.offsetZ, dir),
                    PortDef.make(x + rot.offsetX * 2 + dir.offsetX, y, z + rot.offsetZ * 2 + dir.offsetZ, rot),
                    PortDef.make(x + rot.offsetX * 2 - dir.offsetX, y, z + rot.offsetZ * 2 - dir.offsetZ, rot),
                    PortDef.make(x - rot.offsetX * 2 + dir.offsetX, y, z - rot.offsetZ * 2 + dir.offsetZ, rot.getOpposite()),
                    PortDef.make(x - rot.offsetX * 2 - dir.offsetX, y, z - rot.offsetZ * 2 - dir.offsetZ, rot.getOpposite()),
            };
        }
        return cachedPorts;
    }

    private ForgeDirection facing() {
        return MachineCore.facing(getBlockState());
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return MAX_POWER; }
    public long getPowerScaled(long scale) { return power * scale / MAX_POWER; }
    public int getProgressScaled(int scale) { return (int) (progress * scale / 1F); }

    @Override public int[] getSlotsForFace(Direction side) {
        int[] slots = new int[28];
        int at = 0;
        slots[at++] = 0; slots[at++] = 1; slots[at++] = 2;
        for (int slot = 5; slot < 30; slot++) slots[at++] = slot;
        return slots;
    }

    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot < 3 ? stack.getItem() instanceof ItemArcElectrode : slot >= 25 && isItemValidForSlot(slot, stack);
    }

    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot < 3) return lid >= 1F && !(stack.getItem() instanceof ItemArcElectrode);
        if (slot > 4 && slot < 25) return lid > 0F && ArcFurnaceRecipes.getOutput(stack, liquidMode, level) == null;
        if (slot >= 25) return ArcFurnaceRecipes.getOutput(stack, liquidMode, level) == null;
        return false;
    }

    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 3) return stack.getItem() instanceof ItemArcElectrode;
        if (slot == 3) return Library.isBattery(stack);
        if (slot == 4) return Library.isMachineUpgrade(stack);
        if (slot > 4) {
            ArcFurnaceRecipes.ArcFurnaceRecipe recipe = ArcFurnaceRecipes.getOutput(stack, liquidMode, level);
            return recipe != null && (liquidMode ? recipe.fluidOutput.length > 0 : recipe.solidOutput != null);
        }
        return false;
    }

    @Override public int[] getAccessibleSlotsFromSide(int side) { return getSlotsForFace(Direction.UP); }
    @Override protected int[] getUpgradeSlotRange() { return new int[]{4, 4}; }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerMachineArcFurnaceLarge(id, inventory, this);
    }
    @Override public boolean hasPermission(Player player) { return stillValid(player); }
    @Override public void receiveControl(CompoundTag data) {
        if (Tags.getBooleanOr(data, "liquid", false)) {
            liquidMode = !liquidMode;
            setChanged();
        }
    }

    @Override public boolean canProvideInfo(ItemMachineUpgrade.UpgradeType type, int level, boolean extendedInfo) {
        return type == ItemMachineUpgrade.UpgradeType.SPEED;
    }
    @Override public void provideInfo(ItemMachineUpgrade.UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_arc_furnace.get()));
        if (type == ItemMachineUpgrade.UpgradeType.SPEED) {
            info.add(Component.literal("Delay: -" + (100 - 100 / (level * 2 + 1)) + "%"));
            info.add(Component.literal("Consumption: +" + ((int) Math.pow(5, level) * 100 - 100) + "%"));
        }
    }
    @Override public HashMap<ItemMachineUpgrade.UpgradeType, Integer> getValidUpgrades() {
        HashMap<ItemMachineUpgrade.UpgradeType, Integer> valid = new HashMap<>();
        valid.put(ItemMachineUpgrade.UpgradeType.SPEED, 3);
        return valid;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeFloat(progress);
        buf.writeFloat(lid);
        buf.writeBoolean(isProgressing);
        buf.writeBoolean(liquidMode);
        buf.writeBoolean(hasMaterial);
        for (byte electrode : electrodes) buf.writeByte(electrode);
        buf.writeShort(liquids.size());
        for (Mats.MaterialStack liquid : liquids) {
            buf.writeInt(liquid.material.id);
            buf.writeInt(liquid.amount);
        }
        buf.writeInt(pourColor);
        buf.writeFloat(pourLen);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readFloat();
        syncLid = buf.readFloat();
        isProgressing = buf.readBoolean();
        liquidMode = buf.readBoolean();
        hasMaterial = buf.readBoolean();
        for (int i = 0; i < electrodes.length; i++) electrodes[i] = buf.readByte();
        int count = buf.readShort() & 0xFFFF;
        List<Mats.MaterialStack> synced = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            NTMMaterial material = Mats.matById.get(buf.readInt());
            int amount = buf.readInt();
            if (material != null) synced.add(new Mats.MaterialStack(material, amount));
        }
        liquids = synced;
        pourColor = buf.readInt();
        pourLen = buf.readFloat();
        if (syncLid != 0F && syncLid != 1F) approachNum = 2;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putBoolean("liquidMode", liquidMode);
        output.putFloat("progress", progress);
        output.putFloat("lid", lid);
        output.putInt("delay", delay);
        output.putInt("count", liquids.size());
        for (int i = 0; i < liquids.size(); i++) {
            Mats.write(output, "m" + i, liquids.get(i).material); // by name, never the numeric id
            output.putInt("a" + i, liquids.get(i).amount);
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        liquidMode = input.getBooleanOr("liquidMode", false);
        progress = input.getFloatOr("progress", 0F);
        lid = input.getFloatOr("lid", 0F);
        delay = input.getIntOr("delay", 0);
        liquids.clear();
        int count = input.getIntOr("count", 0);
        for (int i = 0; i < count; i++) {
            NTMMaterial material = Mats.read(input, "m" + i);
            if (material != null) liquids.add(new Mats.MaterialStack(material, input.getIntOr("a" + i, 0)));
        }
    }

    /** Molten contents leave as scraps, then the slots spill (1.7 {@code BlockDummyable.breakBlock}). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            for (Mats.MaterialStack liquid : liquids) {
                ItemScraps.spawnMats(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        liquid.copy());
            }
            liquids.clear();
        }
        super.preRemoveSideEffects(pos, state);
    }
}
