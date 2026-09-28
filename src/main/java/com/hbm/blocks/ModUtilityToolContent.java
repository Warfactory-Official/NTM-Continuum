package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.blocks.generic.Guide;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.hazard.HazardSystem;
import com.hbm.hazard.transformer.HazardTransformerRadiationContainer;
import com.hbm.handler.ToolKeybinds;
import com.hbm.inventory.ModUtilityToolMenus;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.items.UtilityToolDeclarations;
import com.hbm.items.tool.ItemSettingsTool;
import com.hbm.packet.ItemControlPayload;
import com.hbm.packet.ToolKeyPayload;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ItemFirstUse;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;

/**
 * One bootstrap hook for the utility tools and books ({@link ModUtilityToolItems}), their menus, the legacy
 * manual block {@code book_guide_block}, the item-control and copy-tool key payloads, the containment box's
 * hazard transformer and the settings tool's first-use hook.
 */
public final class ModUtilityToolContent {

    private ModUtilityToolContent() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** 1.7 {@code ModBlocks.book_guide}: {@code Material.iron}, hardness 5, resistance 10, nuke tab. */
    public static final RegistryObject<Guide> book_guide_block = BLOCKS.registerBlock("book_guide_block", Guide::new,
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());

    static {
        RegistryObject<BlockItem> item = BLOCK_ITEMS.registerBlockItem("book_guide_block", book_guide_block,
                Compat::blockDescription);
        ModTabs.addTo(HbmTab.BOMBS, item);
    }

    public static void init() {
        ModUtilityToolItems.init();
        ModUtilityToolMenus.init();
        UtilityToolDeclarations.declare();

        GolemNetwork.registerPlayToServer(ItemControlPayload.CODEC, (payload, player) ->
                ItemControlPayload.handle(payload, player));
        GolemNetwork.registerPlayToServer(ToolKeyPayload.CODEC, (payload, player) ->
                ToolKeybinds.setKeyPressed(player, payload.key(), payload.pressed()));
        Lifecycle.onServerStopping(server -> ToolKeybinds.clear());

        HazardSystem.trafos.add(new HazardTransformerRadiationContainer());
        ItemFirstUse.register(ItemSettingsTool::useFirst);
    }
}
