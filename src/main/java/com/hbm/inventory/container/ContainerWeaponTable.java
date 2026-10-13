package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import dev.golem.core.gui.CompatContainerMenu;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The seven-slot weapon modification table, ported from the original {@code ContainerWeaponTable}.
 *
 * <p>The original keeps the gun in the table <b>live</b>: every change to a modifier slot re-installs the
 * whole modifier row on the gun ({@code refreshInstalledMods}), so whatever copy of the gun leaves the
 * table (by pickup, shift-click, hotbar swap or throw) already carries its modifiers. Taking the gun
 * out then only consumes the modifier slots it applies to and resets the configuration index. Nothing
 * here tracks stack identity across clicks; vanilla's own click handling is left alone.
 *
 * <p>Modifier installation is server-side only. The client sees the gun's components through ordinary
 * slot synchronisation, which is all its {@code mayPlace} checks need.
 */
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
            super.setItem(slot, stack);
            // The original putStack: a new gun always starts on configuration 0 and shows its mods.
            // Server only: the client sees this same call for every slot sync and must not reset the
            // configuration index it was just sent.
            if (owner.level().isClientSide()) return;
            config = 0;
            if (!stack.isEmpty()) loadInstalledMods(stack);
        }
    };
    private final Player owner;
    /** True while the table itself rewrites the modifier slots, so that does not re-install. */
    private boolean rebuilding;
    private int config;

    public ContainerWeaponTable(int containerId, Inventory inventory, BlockPos ignored) {
        this(containerId, inventory);
    }

    public ContainerWeaponTable(int containerId, Inventory inventory) {
        super(ModMenus.WEAPON_TABLE.get(), containerId);
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

            /** The original onPickupFromSlot: consume the modifiers the outgoing gun took with it. */
            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                consumeApplicableMods(stack, config);
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
                config = value;
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

    /** The original's 999_999 click: switch the edited configuration of a multi-config gun. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        ItemStack stack = gun.getItem(0);
        if (!(stack.getItem() instanceof ItemGunBaseNT weapon) || !weapon.isValidConfigIndex(button)) return false;
        if (player.level().isClientSide()) return true;
        refreshInstalledMods();
        consumeApplicableMods(stack, config);
        config = button;
        loadInstalledMods(stack);
        broadcastChanges();
        return true;
    }

    /** Shows the gun's installed modifiers for the current configuration in the modifier slots. */
    private void loadInstalledMods(ItemStack stack) {
        ItemStack[] installed = XWeaponModManager.getUpgradeItems(stack, config);
        rebuilding = true;
        for (int i = 0; i < Math.min(installed.length, MOD_SLOTS); i++) {
            ItemStack mod = installed[i];
            if (mod != null && !mod.isEmpty()) mods.setItem(i, mod.copyWithCount(1));
        }
        rebuilding = false;
    }

    /** The original refreshInstalledMods: the gun in the table always reflects the modifier row. */
    private void refreshInstalledMods() {
        if (rebuilding) return;
        ItemStack stack = gun.getItem(0);
        if (!(stack.getItem() instanceof ItemGunBaseNT) || !(owner.level() instanceof ServerLevel level)) return;
        XWeaponModManager.install(level, stack, config, modStacks());
    }

    /** Clears every modifier slot the given gun accepts; those modifiers now live on the gun. */
    private void consumeApplicableMods(ItemStack stack, int configIndex) {
        if (!(stack.getItem() instanceof ItemGunBaseNT) || owner.level().isClientSide()) return;
        rebuilding = true;
        for (int i = 0; i < MOD_SLOTS; i++) {
            ItemStack mod = mods.getItem(i);
            if (!mod.isEmpty() && XWeaponModManager.isApplicable(stack, mod, configIndex, false)) {
                mods.setItem(i, ItemStack.EMPTY);
            }
        }
        rebuilding = false;
    }

    private ItemStack[] modStacks() {
        ItemStack[] result = new ItemStack[MOD_SLOTS];
        for (int i = 0; i < MOD_SLOTS; i++) result[i] = mods.getItem(i);
        return result;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index < PLAYER_START) {
                if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
                // The move empties the slot stack; hand onTake the copy so the gun's modifiers are consumed.
                slot.onTake(player, copy);
            } else if (stack.getItem() instanceof ItemGunBaseNT) {
                if (!moveItemStackTo(stack, GUN_SLOT, GUN_SLOT + 1, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, MOD_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return copy;
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
