package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachinePress;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.ItemEnums.EnumCokeType;
import com.hbm.items.ModCasingItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModOilItems;
import com.hbm.items.machine.ModStampItems;
import com.hbm.tileentity.machine.TileEntityMachinePress;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Press recipes and the burner press. */
public final class PressGameTests {

    private PressGameTests() {}

    public static void casingRecipes(GameTestHelper helper) {
        PressRecipes.register();
        assertId(helper, ModCasingItems.casing(EnumCasingType.SMALL), "casing_small");
        assertId(helper, ModCasingItems.casing(EnumCasingType.LARGE), "casing_large");
        assertId(helper, ModCasingItems.casing(EnumCasingType.SMALL_STEEL), "casing_small_steel");
        assertId(helper, ModCasingItems.casing(EnumCasingType.LARGE_STEEL), "casing_large_steel");
        assertOutput(helper, ModItems.plate_gunmetal.get(), ModStampItems.stamp_9.get(),
                ModCasingItems.casing(EnumCasingType.SMALL), 4);
        assertOutput(helper, ModItems.plate_gunmetal.get(), ModStampItems.stamp_50.get(),
                ModCasingItems.casing(EnumCasingType.LARGE), 2);
        assertOutput(helper, ModItems.plate_weaponsteel.get(), ModStampItems.stamp_9.get(),
                ModCasingItems.casing(EnumCasingType.SMALL_STEEL), 4);
        assertOutput(helper, ModItems.plate_weaponsteel.get(), ModStampItems.stamp_50.get(),
                ModCasingItems.casing(EnumCasingType.LARGE_STEEL), 2);
        helper.succeed();
    }

    private static void assertOutput(GameTestHelper helper, Item input, Item stamp, Item output, int count) {
        ItemStack result = PressRecipes.getOutput(new ItemStack(input), new ItemStack(stamp));
        helper.assertTrue(result.is(output) && result.getCount() == count,
                "Press recipe produced " + result + ", expected " + count + " " + output);
    }

    private static void assertId(GameTestHelper helper, Item item, String path) {
        helper.assertTrue(Ids.of("hbm", path)
                        .equals(BuiltInRegistries.ITEM.getKey(item)),
                "Expected item id hbm:" + path + ", got "
                        + BuiltInRegistries.ITEM.getKey(item));
    }

    /** Coal coke burns, and 1.7's burner press still takes it as an input: flat stamp, coke in, graphite out. */
    public static void cokeToGraphite(GameTestHelper helper) {
        BlockPos core = FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_press.get(), new BlockPos(2, 1, 2), Direction.NORTH);
        TileEntityMachinePress press = (TileEntityMachinePress) helper.getLevel().getBlockEntity(core);
        ItemStack coke = new ItemStack(ModOilItems.coke(EnumCokeType.COAL), 4);
        ContainerMachinePress menu = new ContainerMachinePress(0, GameTestHelpers.mockServerPlayerInLevel(helper).getInventory(), press);
        helper.assertTrue(menu.getSlot(2).mayPlace(coke), "the input slot refuses coal coke");
        helper.assertTrue(press.canPlaceItemThroughFace(2, coke, Direction.NORTH), "a hopper cannot put coal coke into the input");
        press.inventory.setStackInSlot(0, new ItemStack(Items.COAL, 8));
        press.inventory.setStackInSlot(1, new ItemStack(ModStampItems.stamp_iron_flat.get()));
        press.inventory.setStackInSlot(2, coke);
        helper.succeedWhen(() -> helper.assertTrue(press.inventory.getStackInSlot(3).is(ModFuelItems.ingot_graphite.get()),
                "no graphite yet, output " + press.inventory.getStackInSlot(3)));
    }
}
