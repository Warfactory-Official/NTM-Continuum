package com.hbm.inventory.container;

import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.gui.CompatContainerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

/** The seven-slot weapon-modification menu, including source-compatible config and transfer behavior. */
public final class ContainerWeaponTable extends CompatContainerMenu {
    public static final int MOD_SLOTS = 7;
    public static final int GUN_SLOT = MOD_SLOTS;
    public static final int PLAYER_START = GUN_SLOT + 1;
    public static final int PLAYER_END = PLAYER_START + 36;

    private final SimpleContainer mods = new SimpleContainer(MOD_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            refreshInstalledMods();
        }
    };
    private final SimpleContainer gun = new SimpleContainer(1) {
        @Override
        public void setItem(int slot, ItemStack stack) {
            if (!owner.level().isClientSide()) config = 0;
            super.setItem(slot, stack);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!getItem(0).isEmpty()) loadInstalledMods();
        }
    };
    private final Player owner;
    private boolean rebuilding;
    private boolean pendingGunTake;
    private int pendingGunConfig;
    private ItemStack[] pendingGunMods;
    private ItemStack clickGunSource;
    private int config;

    public ContainerWeaponTable(int containerId, Inventory inventory, BlockPos ignored) {
        this(containerId, inventory);
    }

    public ContainerWeaponTable(int containerId, Inventory inventory) {
        super(com.hbm.inventory.ModMenus.WEAPON_TABLE.get(), containerId);
        owner = inventory.player;

        for (int i = 0; i < MOD_SLOTS; i++) {
            addSlot(new Slot(mods, i, 44 + 18 * i, 108) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    ItemStack held = gun.getItem(0);
                    return !held.isEmpty() && XWeaponModManager.isApplicable(held, stack, config, true);
                }
            });
        }
        addSlot(new Slot(gun, 0, 8, 108) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return getItem().isEmpty() && stack.getItem() instanceof ItemGunBaseNT;
            }

            @Override
            public ItemStack remove(int amount) {
                if (!getItem().isEmpty()) {
                    pendingGunConfig = config;
                    pendingGunMods = copyModStacks();
                    pendingGunTake = true;
                }
                ItemStack removed = super.remove(amount);
                if (removed.isEmpty()) clearPendingGunTake();
                return removed;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                int outgoingConfig = pendingGunTake ? pendingGunConfig : config;
                ItemStack[] outgoingMods = pendingGunTake ? pendingGunMods : copyModStacks();
                clearPendingGunTake();
                super.onTake(player, stack);
                finishConfig(stack, outgoingConfig, outgoingMods);
                config = 0;
            }
        });
        Compat.addPlayerInventory(this::addSlot, inventory, 8, 158);
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return config;
            }

            @Override
            public void set(int value) {
                if (config == value) return;
                config = value;
                loadInstalledMods();
            }
        });
    }

    public ItemStack previewStack() {
        return gun.getItem(0);
    }

    public int configIndex() {
        return config;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {
        if (action == ClickAction.PICKUP && button == 0 && slotId >= 0 && slotId < slots.size()) {
            Slot slot = slots.get(slotId);
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                if (!slot.hasItem() || !slot.mayPickup(player)) return true;
                ItemStack sourceBeforeTake = slot.getItem();
                ItemStack removed;
                if (slotId >= PLAYER_START && sourceBeforeTake.getItem() instanceof ItemGunBaseNT) {
                    removed = sourceBeforeTake;
                    slot.set(ItemStack.EMPTY);
                    slot.setChanged();
                } else {
                    removed = slot.remove(slot.getItem().getCount());
                }
                if (slotId >= PLAYER_START && sourceBeforeTake.getItem() instanceof ItemGunBaseNT) {
                    clickGunSource = sourceBeforeTake;
                }
                setCarried(removed);
                slot.onTake(player, removed);
            } else {
                if (!slot.mayPlace(carried) || slot.hasItem()) return true;
                slot.set(carried);
                slot.setChanged();
                refreshInstalledMods();
                if (slotId < MOD_SLOTS && owner.level() instanceof ServerLevel level
                        && gun.getItem(0).getItem() instanceof ItemGunBaseNT) {
                    XWeaponModManager.install(level, gun.getItem(0), config, modStacks());
                    if (clickGunSource != null) {
                        XWeaponModManager.install(level, clickGunSource, config, modStacks());
                    }
                }
                setCarried(ItemStack.EMPTY);
            }
            broadcastChanges();
            return true;
        }
        if (action != ClickAction.SWAP || slotId != GUN_SLOT || button < 0 || button >= 9) return false;
        ItemStack source = gun.getItem(0);
        if (source.isEmpty() || !(source.getItem() instanceof ItemGunBaseNT)) return false;

        ItemStack[] outgoingMods = copyModStacks();
        ItemStack destination = player.getInventory().getItem(button);
        ItemStack removed = gun.removeItemNoUpdate(0);
        finishConfig(removed, config, outgoingMods);
        clearConsumedMods(removed, config, outgoingMods);
        player.getInventory().setItem(button, removed);
        if (!destination.isEmpty()) player.drop(destination, false);
        config = 0;
        broadcastChanges();
        return true;
    }

    private void loadInstalledMods() {
        if (rebuilding) return;
        ItemStack stack = gun.getItem(0);
        rebuilding = true;
        mods.clearContent();
        if (!stack.isEmpty()) {
            ItemStack[] installed = XWeaponModManager.getUpgradeItems(stack, config);
            for (int i = 0; i < Math.min(installed.length, MOD_SLOTS); i++) {
                ItemStack mod = installed[i];
                if (mod != null && !mod.isEmpty()) mods.setItem(i, mod.copyWithCount(1));
            }
        }
        rebuilding = false;
    }

    private void refreshInstalledMods() {
        if (rebuilding || owner.level().isClientSide()) return;
        ItemStack stack = gun.getItem(0);
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT)) return;
        if (owner.level() instanceof ServerLevel level) {
            XWeaponModManager.install(level, stack, config, modStacks());
        }
    }

    private ItemStack[] modStacks() {
        ItemStack[] result = new ItemStack[MOD_SLOTS];
        for (int i = 0; i < MOD_SLOTS; i++) result[i] = mods.getItem(i);
        return result;
    }

    private ItemStack[] copyModStacks() {
        ItemStack[] result = new ItemStack[MOD_SLOTS];
        for (int i = 0; i < MOD_SLOTS; i++) result[i] = mods.getItem(i).copy();
        return result;
    }

    private void clearPendingGunTake() {
        pendingGunTake = false;
        pendingGunMods = null;
    }

    private void finishConfig(ItemStack stack, int configIndex, ItemStack[] outgoingMods) {
        if (owner.level().isClientSide() || !(stack.getItem() instanceof ItemGunBaseNT)
                || !(owner.level() instanceof ServerLevel level)) return;
        XWeaponModManager.install(level, stack, configIndex, outgoingMods);
        clearConsumedMods(stack, configIndex, outgoingMods);
    }

    private void clearConsumedMods(ItemStack stack, int configIndex, ItemStack[] consumed) {
        rebuilding = true;
        for (int i = 0; i < MOD_SLOTS; i++) {
            ItemStack mod = mods.getItem(i);
            ItemStack expected = consumed[i];
            if (!mod.isEmpty() && !expected.isEmpty()
                    && ItemStacks.sameItemAndData(mod, expected)
                    && XWeaponModManager.isApplicable(stack, expected, configIndex, false)) {
                int remaining = mod.getCount() - expected.getCount();
                mods.setItem(i, remaining > 0 ? mod.copyWithCount(remaining) : ItemStack.EMPTY);
            }
        }
        rebuilding = false;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        ItemStack stack = gun.getItem(0);
        if (!(stack.getItem() instanceof ItemGunBaseNT weapon) || !weapon.isValidConfigIndex(button)) return false;
        if (button == config) return true;
        if (!player.level().isClientSide()) finishConfig(stack, config, copyModStacks());
        config = button;
        loadInstalledMods();
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (index == GUN_SLOT) {
                int outgoingConfig = config;
                if (!hasPlayerDestination(stack)) return ItemStack.EMPTY;
                if (!owner.level().isClientSide()) {
                    if (!(owner.level() instanceof ServerLevel level)
                            || !(stack.getItem() instanceof ItemGunBaseNT)) return ItemStack.EMPTY;
                    ItemStack[] outgoingMods = copyModStacks();
                    // Install on the live source so the stack copied by the menu move carries all modifiers.
                    XWeaponModManager.install(level, stack, outgoingConfig, outgoingMods);
                    ItemStack finalized = stack.copy();
                    if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
                    clearConsumedMods(finalized, outgoingConfig, outgoingMods);
                } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                    return ItemStack.EMPTY;
                }
                config = 0;
            } else {
                boolean moved = false;
                for (int inventorySlot = 0; inventorySlot < owner.getInventory().getContainerSize(); inventorySlot++) {
                    if (owner.getInventory().getItem(inventorySlot).isEmpty()) {
                        owner.getInventory().setItem(inventorySlot, stack.copy());
                        moved = true;
                        break;
                    }
                }
                if (!moved) return ItemStack.EMPTY;
                slot.set(ItemStack.EMPTY);
                slot.setChanged();
                refreshInstalledMods();
                slot.onTake(player, original);
                return original;
            }
        } else if (stack.getItem() instanceof ItemGunBaseNT) {
            if (!moveItemStackTo(stack, GUN_SLOT, GUN_SLOT + 1, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, MOD_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return stack.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }

    private boolean hasPlayerDestination(ItemStack source) {
        for (int i = PLAYER_START; i < PLAYER_END; i++) {
            Slot target = slots.get(i);
            if (!target.mayPlace(source)) continue;
            ItemStack existing = target.getItem();
            if (existing.isEmpty()) return true;
            if (ItemStacks.sameItemAndData(source, existing)
                    && existing.getCount() < Math.min(target.getMaxStackSize(), source.getMaxStackSize())) return true;
        }
        return false;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!(player.level() instanceof ServerLevel level)) return;
        for (int i = 0; i < MOD_SLOTS; i++) {
            ItemStack mod = mods.removeItemNoUpdate(i);
            if (!mod.isEmpty()) player.drop(mod, false);
        }
        ItemStack stack = gun.removeItemNoUpdate(0);
        if (!stack.isEmpty()) {
            XWeaponModManager.uninstall(level, stack, config);
            player.drop(stack, false);
        }
    }
}
