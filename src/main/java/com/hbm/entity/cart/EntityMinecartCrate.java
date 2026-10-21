package com.hbm.entity.cart;

import dev.golem.core.world.Explosions;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.entity.ModNtmEntities;
import dev.golem.core.world.Compat;
import com.hbm.inventory.container.MenuCrate;
import com.hbm.items.tool.ItemModMinecart;
import com.hbm.items.tool.ItemModMinecart.EnumCartBase;
import com.hbm.items.tool.ItemModMinecart.EnumMinecart;
import com.hbm.tileentity.machine.storage.CrateType;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import com.hbm.blocks.ModBlocks;

/**
 * 1.7 {@code entity/cart/EntityMinecartCrate}: a steel crate on rails. Only the VANILLA chassis carries it.
 */
public class EntityMinecartCrate extends EntityMinecartContainerBase {

    private static final CrateType CRATE = CrateType.STEEL;

    public EntityMinecartCrate(EntityType<? extends EntityMinecartCrate> type, Level level) {
        super(type, level);
    }

    public EntityMinecartCrate(Level level, double x, double y, double z, EnumCartBase base, ItemStack stack) {
        super(ModNtmEntities.CART_CRATE.get(), level, x, y, z, base);
        // 1.7 restored the crate's contents from the item's own NBT when placed
        if (stack != null && !stack.isEmpty()) {
            NonNullList<ItemStack> carried = ItemStacks.carriedItems(stack, getItemStacks().size());
            for (int i = 0; i < carried.size(); i++) getItemStacks().set(i, carried.get(i));
        }
    }

    /**
     * 1.7 killMinecart: the whole hold packs into the dropped item, nothing spills. A packed cart whose
     * compressed NBT tops 6000 bytes explodes instead.
     */
    @Override
    public void destroy(ServerLevel level, DamageSource source) {
        List<ItemStack> held = List.copyOf(getItemStacks());
        boolean any = held.stream().anyMatch(s -> !s.isEmpty());
        clearContent();
        Compat.kill(this, level);

        if (!Compat.entityDrops(level)) return;

        ItemStack stack = getCartItem();
        if (hasCustomName()) ItemStacks.setCustomName(stack, getCustomName());

        ItemStack packed = stack.copy();
        if (any) ItemStacks.setCarriedItems(packed, held);
        boolean oversize = ItemStacks.compressedSize(packed, level.registryAccess()) > 6000;

        if (oversize) {
            // 1.7: also dropped the packed cart beside a bonus empty one; the block crate's own guard ejects the hold
            Explosions.explode(level, this, getX(), getY(), getZ(), 2.0F, true, Level.ExplosionInteraction.BLOCK);
            for (ItemStack loose : held) if (!loose.isEmpty()) spawnAtLocation(level, loose);
        } else if (any) {
            stack = packed;
        }

        spawnAtLocation(level, stack);
    }

    @Override
    public int getContainerSize() {
        return CRATE.slots;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new MenuCrate(containerId, inventory, CRATE, this);
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return ModStorageBlocks.crate_steel.get().defaultBlockState();
    }

    @Override
    // 1.7 carts extend EntityMinecart, not EntityMinecartContainer, so the vanilla default is 6
    public int getDefaultDisplayOffset() {
        return 6;
    }

    @Override
    public ItemStack getCartItem() {
        return ItemModMinecart.createCartItem(getBase(), EnumMinecart.CRATE);
    }
}
