package com.hbm.items.block;

import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.config.ServerConfig;
import com.hbm.inventory.container.ContainerCrateTungsten;
import com.hbm.inventory.container.ContainerSafe;
import com.hbm.inventory.container.MenuCrate;
import com.hbm.inventory.ModHeldCrateMenus;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import com.hbm.tileentity.machine.storage.CrateType;
import dev.golem.core.gui.CompatContainerMenu;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 1.7 {@code ItemBlockStorageCrate.onItemUse}/{@code onItemRightClick}/{@code InventoryCrate} under
 * {@code /ntmserver CRATE_OPEN_HELD} (on by default): a crate in hand opens its own inventory on right-click and is
 * placed only while sneaking. A single crate only; a locked one needs a matching key anywhere in the main inventory;
 * a crate with spiders lets them out once. The contents live on the stack (the same carried inventory a placed and
 * broken crate carries), written back on every change; the hotbar slot holding the crate is locked while it is open.
 * The mass storage never opened from the hand in 1.7 and does not here (it has its own item).
 */
public final class CrateHeldOpening {

    /** 1.7 {@code InventoryCrate} "stacklock": a random value that keeps anything from merging into the crate. */
    public static final String STACKLOCK = "stacklock";

    private CrateHeldOpening() {}

    public static void stamp(ItemStack crate, Level level) {
        ItemStacks.setCarriedLong(crate, STACKLOCK, level.getRandom().nextLong());
    }

    /** Which crate a stack is, for the held menus; null when it is none of the five. */
    public enum Kind { IRON, STEEL, DESH, TUNGSTEN, SAFE }

    public static @Nullable Kind kindOf(ItemStack stack) {
        if (stack.getItem() instanceof ItemBlockStorageCrate crate) {
            return switch (crate.crateType()) {
                case IRON -> Kind.IRON;
                case STEEL -> Kind.STEEL;
                case DESH -> Kind.DESH;
            };
        }
        if (stack.is(ModDecoCrateBlocks.crate_tungsten.get().asItem())) return Kind.TUNGSTEN;
        if (stack.is(ModDecoCrateBlocks.safe.get().asItem())) return Kind.SAFE;
        return null;
    }

    public static int slots(Kind kind) {
        return switch (kind) {
            case IRON -> CrateType.IRON.slots;
            case STEEL -> CrateType.STEEL.slots;
            case DESH -> CrateType.DESH.slots;
            case TUNGSTEN -> 27;
            case SAFE -> 15;
        };
    }

    public static CrateType crateType(Kind kind) {
        return switch (kind) {
            case STEEL -> CrateType.STEEL;
            case DESH -> CrateType.DESH;
            default -> CrateType.IRON;
        };
    }

    /** 1.7 {@code onItemUse}: with the key on, placing needs sneaking; the click goes on to {@link #use}. */
    public static @Nullable InteractionResult beforePlace(UseOnContext context) {
        Player player = context.getPlayer();
        if (ServerConfig.CRATE_OPEN_HELD.get() && player != null && !player.isShiftKeyDown()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        return null;
    }

    /** 1.7 {@code onItemRightClick}. */
    public static InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!ServerConfig.CRATE_OPEN_HELD.get()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        ItemStack stack = player.getItemInHand(hand);
        Kind kind = kindOf(stack);
        if (kind == null || hand != InteractionHand.MAIN_HAND) return InteractionResult.TRY_WITH_EMPTY_HAND; // 1.7 used getHeldItem
        if (!level.isClientSide() && stack.getCount() == 1 && player instanceof ServerPlayer serverPlayer) {
            int lock = ItemStacks.carriedInt(stack, "lock");
            if (lock != 0) {
                for (int i = 0; i < 36; i++) { // 1.7 mainInventory
                    ItemStack item = player.getInventory().getItem(i);
                    if (item.getItem() instanceof TileEntityLockableBase.KeyPins key && key.getPins(item) == lock) {
                        spawnSpiders(player, level, stack);
                        open(serverPlayer, stack, kind);
                        break;
                    }
                }
                return InteractionResult.SUCCESS; // return early if it was locked
            }
            spawnSpiders(player, level, stack);
            open(serverPlayer, stack, kind);
        }
        return InteractionResult.SUCCESS;
    }

    private static void open(ServerPlayer player, ItemStack stack, Kind kind) {
        int heldSlot = Compat.selectedSlot(player);
        Component title = ItemStacks.customName(stack) != null ? stack.getHoverName() : stack.getItem().getName(stack);
        Menus.openWithHand(player, new SimpleMenuProvider((id, inventory, opener) -> {
            HeldCrate held = new HeldCrate(opener, stack, slots(kind));
            return switch (kind) {
                case TUNGSTEN -> new ContainerCrateTungsten(id, inventory, held, heldSlot);
                case SAFE -> new ContainerSafe(id, inventory, held, heldSlot);
                default -> new MenuCrate(ModHeldCrateMenus.CRATE_HELD.get(), id, inventory, crateType(kind), held, heldSlot);
            };
        }, title), InteractionHand.MAIN_HAND);
    }

    /** 1.7 {@code TileEntityCrateBase.spawnSpiders(player, world, stack)}: three cave spiders, then the flag is gone. */
    public static void spawnSpiders(Player player, Level level, ItemStack crate) {
        if (!ItemStacks.carriedBoolean(crate, "spiders") || !(level instanceof ServerLevel server)) return;
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            var spider = Entities.createTriggered(EntityType.CAVE_SPIDER, server);
            if (spider == null) continue;
            spider.snapTo(player.getX() + random.nextGaussian() * 2, player.getY() + 1, player.getZ() + random.nextGaussian() * 2,
                    random.nextFloat(), 0);
            spider.setTarget(player);
            level.addFreshEntity(spider);
        }
        ItemStacks.setCarriedBoolean(crate, "spiders", false);
    }

    /**
     * 1.7 {@code ContainerCrateBase.slotClick} for a held crate: nothing may move the crate's own hotbar slot - no
     * click on it and no number-key swap into it.
     */
    public static boolean blocksClick(AbstractContainerMenu menu, int heldSlot, int slotId, int button,
                                      CompatContainerMenu.ClickAction clickType) {
        if (heldSlot < 0) return false;
        if (clickType == CompatContainerMenu.ClickAction.SWAP && button == heldSlot) return true;
        if (slotId >= 0 && slotId < menu.slots.size()) {
            Slot slot = menu.slots.get(slotId);
            return slot.container instanceof Inventory && slot.getContainerSlot() == heldSlot;
        }
        return false;
    }

    /** The held crate's slots over its stack: 1.7 {@code InventoryCrate}. The client builds a plain stand-in. */
    public static final class HeldCrate extends SimpleContainer {

        private final Player player;
        public final ItemStack target;
        private final boolean loaded;

        public HeldCrate(Player player, ItemStack crate, int slots) {
            super(slots);
            this.player = player;
            this.target = crate;
            NonNullList<ItemStack> items = ItemStacks.carriedItems(crate, "inventory", slots);
            for (int i = 0; i < slots; i++) super.setItem(i, items.get(i));
            this.loaded = true;
            // 1.7 stamped only a tagless crate, so a renamed one could still take a merge while open
            stamp(target, player.level());
            playSound(ModSounds.CRATE_OPEN.get()); // 1.7 openInventory: only the server builds one of these
        }

        /** 1.7 {@code markDirty}: every change goes straight back onto the stack. */
        @Override
        public void setChanged() {
            super.setChanged();
            if (!loaded || player.level().isClientSide()) return;
            List<ItemStack> list = new ArrayList<>(getContainerSize());
            for (int i = 0; i < getContainerSize(); i++) list.add(getItem(i).copy());
            ItemStacks.setCarriedItems(target, "inventory", list);
            stamp(target, player.level());
        }

        /** 1.7 {@code isUseableByPlayer}: while the crate is still the held item, and still one crate. */
        @Override
        public boolean stillValid(Player player) {
            return player == this.player && player.getMainHandItem() == target && target.getCount() == 1;
        }

        /** Called by the held menus when they close (1.7 {@code closeInventory}). */
        public void closed() {
            playSound(ModSounds.CRATE_CLOSE.get());
            if (player.level().isClientSide()) return;
            // an emptied, unnamed, unlocked crate stacks again
            ItemStacks.removeCarried(target, STACKLOCK);
            if (!ItemStacks.isPlain(target)) stamp(target, player.level());
        }

        /** 1.7 {@code ItemInventory.openInventory}/{@code closeInventory}: at the player, pitch 0.8. */
        private void playSound(SoundEvent sound) {
            if (player.level().isClientSide()) return;
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }
}
