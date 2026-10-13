package com.hbm.items.machine;

import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.items.LegacyMeta;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModMachineItems;
import dev.golem.core.world.CompatItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** The old blueprint booklet: right-click chooses a recipe pool and creates a dynamic blueprint. */
public final class ItemBlueprintFolder extends CompatItem implements LegacyMeta {

    /** 1.7 metadata: 0 draws an ALT pool, 1 a DISCOVER pool, 2 a SECRET pool. */
    private final int variant;

    public ItemBlueprintFolder(Properties properties, int variant) {
        super(properties);
        this.variant = variant;
    }

    @Override
    public int legacyMeta() {
        return variant;
    }

    public static ItemStack make(int variant) {
        return new ItemStack(variant == 1 ? ModMachineItems.blueprint_folder_discover.get()
                : variant == 2 ? ModMachineItems.blueprint_folder_secret.get() : ModMachineItems.blueprint_folder.get());
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // Machine recipes are lazy in the modern port; 1.12.2 had already loaded them before item use.
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        // 1.7 onItemRightClick: damage 0 ALT, 1 DISCOVER, 2 SECRET (the port had dropped the SECRET folder)
        String prefix = variant == 1 ? GenericRecipes.POOL_PREFIX_DISCOVER
                : variant == 2 ? GenericRecipes.POOL_PREFIX_SECRET : GenericRecipes.POOL_PREFIX_ALT;
        List<String> pools = new ArrayList<>();
        for (String pool : GenericRecipes.blueprintPools.keySet()) {
            if (pool.startsWith(prefix)) pools.add(pool);
        }
        if (pools.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        String selected = pools.get(player.getRandom().nextInt(pools.size()));
        ItemStack blueprint = ItemBlueprints.make(selected);
        if (player.isCreative()) {
            player.drop(blueprint, false);
        } else {
            held.shrink(1);
            if (held.isEmpty()) player.setItemInHand(hand, blueprint);
            else if (!player.getInventory().add(blueprint)) player.drop(blueprint, false);
        }
        player.swing(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.hbm.blueprint_folder");
    }
}
