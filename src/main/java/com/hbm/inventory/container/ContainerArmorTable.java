package com.hbm.inventory.container;

import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.ModMenus;
import com.hbm.items.armor.JetpackBase;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Armors;
import dev.golem.core.world.CompatEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The armor table menu. Installed modules are mirrored into the armor component on every slot change. */
public final class ContainerArmorTable extends AbstractContainerMenu {

    public static final int MODULE_SLOTS = ArmorModHandler.MOD_SLOTS;
    public static final int ARMOR_SLOT = MODULE_SLOTS;
    private static final int EQUIPMENT_START = ARMOR_SLOT + 1;
    private static final int PLAYER_START = EQUIPMENT_START + 4;
    private static final int PLAYER_END = PLAYER_START + 36;

    public final SimpleContainer upgrades = new SimpleContainer(MODULE_SLOTS);
    public final SimpleContainer armor = new SimpleContainer(1);
    public ContainerArmorTable(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenus.ARMOR_TABLE.get(), containerId);

        addSlot(new UpgradeSlot(ArmorModHandler.helmet_only, 48, 27));
        addSlot(new UpgradeSlot(ArmorModHandler.plate_only, 84, 27));
        addSlot(new UpgradeSlot(ArmorModHandler.legs_only, 120, 27));
        addSlot(new UpgradeSlot(ArmorModHandler.boots_only, 156, 45));
        addSlot(new UpgradeSlot(ArmorModHandler.servos, 156, 81));
        addSlot(new UpgradeSlot(ArmorModHandler.cladding, 120, 99));
        addSlot(new UpgradeSlot(ArmorModHandler.kevlar, 84, 99));
        addSlot(new UpgradeSlot(ArmorModHandler.extra, 48, 99));
        addSlot(new UpgradeSlot(ArmorModHandler.battery, 30, 63));

        addSlot(new ArmorTargetSlot(66, 63));

        EquipmentSlot[] equipment = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < equipment.length; i++) addSlot(new EquipmentTargetSlot(
                playerInventory, 39 - i, equipment[i], 5, 36 + i * 18));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 30, 140);
        loadModules();
    }

    private void loadModules() {
        upgrades.clearContent();
        ItemStack target = armor.getItem(0);
        if (target.isEmpty()) return;
        ItemStack[] modules = ArmorModHandler.pryMods(target);
        for (int i = 0; i < MODULE_SLOTS; i++) upgrades.setItem(i, modules[i]);
    }

    private void syncModules() {
        ItemStack target = armor.getItem(0);
        if (target.isEmpty()) return;
        ArmorModHandler.clearMods(target);
        for (int i = 0; i < MODULE_SLOTS; i++) {
            ItemStack module = upgrades.getItem(i);
            if (!module.isEmpty()) ArmorModHandler.applyMod(target, module);
        }
    }

    private void applyModulesTo(ItemStack target) {
        for (int i = 0; i < MODULE_SLOTS; i++) {
            ItemStack module = upgrades.getItem(i);
            if (!module.isEmpty()) ArmorModHandler.applyMod(target, module);
        }
    }

    @Override
    public boolean stillValid(Player player) {
        // The legacy container stayed valid unconditionally; the table has no block entity
        // and can be safely closed by the client at any time without losing stack-owned state.
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide()) return;

        ItemStack target = armor.getItem(0);
        if (!target.isEmpty()) {
            ItemStack[] modules = ArmorModHandler.pryMods(target);
            ArmorModHandler.clearMods(target);
            armor.setItem(0, ItemStack.EMPTY);
            player.drop(target, false);
            for (ItemStack module : modules) if (!module.isEmpty()) player.drop(module, false);
            upgrades.clearContent();
        } else {
            for (int i = 0; i < MODULE_SLOTS; i++) {
                ItemStack module = upgrades.getItem(i);
                if (!module.isEmpty()) player.drop(module.copy(), false);
                upgrades.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index <= ARMOR_SLOT) {
            // 1.7: the armour piece goes onto the player first, modules straight into the inventory
            if (index != ARMOR_SLOT || !moveItemStackTo(source, EQUIPMENT_START, PLAYER_START, false))
                if (!moveItemStackTo(source, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (Armors.isArmor(source) && !(source.getItem() instanceof JetpackBase)) {
            if (!moveItemStackTo(source, ARMOR_SLOT, ARMOR_SLOT + 1, false)) return ItemStack.EMPTY;
        } else {
            int moduleSlot = ArmorModHandler.getModuleSlot(source);
            if (moduleSlot < 0 || moduleSlot >= MODULE_SLOTS || !slots.get(moduleSlot).mayPlace(source)) return ItemStack.EMPTY;
            if (!moveItemStackTo(source, moduleSlot, moduleSlot + 1, false)) return ItemStack.EMPTY;
        }

        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (source.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, source);
        return original;
    }

    private final class UpgradeSlot extends Slot {
        private final int moduleIndex;

        UpgradeSlot(int index, int x, int y) {
            super(upgrades, index, x, y);
            this.moduleIndex = index;
        }

        @Override
        public int getMaxStackSize() { return 1; }

        @Override
        public boolean mayPlace(ItemStack stack) {
            ItemStack target = armor.getItem(0);
            return !target.isEmpty() && ArmorModHandler.getModuleSlot(stack) == moduleIndex
                    && ArmorModHandler.isApplicable(target, stack);
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            super.setByPlayer(stack);
            syncModules();
        }
    }

    private final class ArmorTargetSlot extends Slot {
        ArmorTargetSlot(int x, int y) {
            super(armor, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // 1.7: any ItemArmor - vanilla's and other mods' armour take modules too
            return Armors.isArmor(stack);
        }

        @Override
        public int getMaxStackSize() { return 1; }

        @Override
        public void setByPlayer(ItemStack stack) {
            super.setByPlayer(stack);
            loadModules();
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            applyModulesTo(stack);
            upgrades.clearContent();
            super.onTake(player, stack);
        }
    }

    private static final class EquipmentTargetSlot extends Slot {
        private final EquipmentSlot equipmentSlot;
        private final Player player;

        EquipmentTargetSlot(Inventory inventory, int index, EquipmentSlot equipmentSlot, int x, int y) {
            super(inventory, index, x, y);
            this.equipmentSlot = equipmentSlot;
            this.player = inventory.player;
        }

        @Override
        public int getMaxStackSize() { return 1; }

        @Override
        public boolean mayPlace(ItemStack stack) {
            // 1.7 isValidArmor(stack, slot, player): whatever vanilla lets the player wear there (any armour, a pumpkin
            // or head on the head, an elytra on the chest), as the vanilla inventory's armour slots decide it.
            return player.getEquipmentSlotForItem(stack) == equipmentSlot;
        }

        @Override
        public boolean mayPickup(Player player) {
            return (player.isCreative() || !CompatEnchantments.preventsArmorChange(getItem()))
                    && super.mayPickup(player);
        }

    }
}
