package com.hbm.gametest;

import com.hbm.blocks.machine.storage.BlockTemplateCrate;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.inventory.container.ContainerCrateTemplate;
import com.hbm.inventory.ModMenus;
import com.hbm.tileentity.machine.storage.TemplateCrateLayout;
import com.hbm.tileentity.machine.storage.TileEntityCrateTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

/** GameTest contracts for the registered ordinary 27-slot template crate. */
public final class TemplateCrateGameTests {

    private TemplateCrateGameTests() {}

    public static void layoutAndInventory(GameTestHelper helper) {
        helper.assertTrue(TemplateCrateLayout.SLOTS == 27 && TemplateCrateLayout.COLUMNS == 9
                        && TemplateCrateLayout.ROWS == 3, "template-crate inventory dimensions changed");
        helper.assertTrue(TemplateCrateLayout.GUI_WIDTH == 176 && TemplateCrateLayout.GUI_HEIGHT == 168
                        && TemplateCrateLayout.CRATE_X == 8 && TemplateCrateLayout.CRATE_Y == 18
                        && TemplateCrateLayout.PLAYER_INVENTORY_Y == 86 && TemplateCrateLayout.HOTBAR_Y == 144,
                "template-crate GUI layout changed");

        BlockTemplateCrate block = ModStorageBlocks.crate_template.get();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        BlockEntity entity = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(entity instanceof TileEntityCrateTemplate,
                "registered template-crate block did not create its BE");
        TileEntityCrateTemplate crate = (TileEntityCrateTemplate) entity;
        helper.assertTrue(crate.getContainerSize() == 27, "template crate slot count changed");
        crate.setItem(0, new ItemStack(Items.DIAMOND, 3));
        helper.assertTrue(crate.getItem(0).getItem() == Items.DIAMOND && crate.getItem(0).getCount() == 3,
                "template crate inventory did not retain its contents");

        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ContainerCrateTemplate menu = new ContainerCrateTemplate(1, player.getInventory(), crate,
                ModMenus.CRATE_TEMPLATE::get);
        helper.assertTrue(menu.slots.size() == 63, "template-crate menu must expose 27 plus 36 slots");
        helper.succeed();
    }

    public static void assets(GameTestHelper helper) {
        String[] assets = {
                "assets/hbm/blockstates/crate_template.json",
                "assets/hbm/models/block/crate_template.json",
                "assets/hbm/models/item/crate_template.json",
                "assets/hbm/items/crate_template.json",
                "assets/hbm/textures/blocks/crate_template.png",
                "assets/hbm/textures/gui/storage/gui_crate_template.png",
                "data/hbm/loot_table/blocks/crate_template.json"
        };
        for (String asset : assets) {
            helper.assertTrue(TemplateCrateGameTests.class.getClassLoader().getResource(asset) != null,
                    "template-crate asset is missing: " + asset);
        }
        helper.succeed();
    }
}
