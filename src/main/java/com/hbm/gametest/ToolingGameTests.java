package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.items.ModToolItems;
import com.hbm.items.tool.ItemTooling;
import com.hbm.handler.NTMToolHandler;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Focused contracts for ordinary tooling classification and wear semantics. */
public final class ToolingGameTests {

    private ToolingGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function CLASSIFICATION =
            TESTS.function("tooling_classification", 20, () -> ToolingGameTests::classification);
    public static final GameTests.Function DURABILITY =
            TESTS.function("tooling_durability", 40, () -> ToolingGameTests::durability);

    public static void init() {}

    public static void classification(GameTestHelper helper) {
        assertTool(helper, ModToolItems.screwdriver, "screwdriver", ToolType.SCREWDRIVER, 100);
        assertTool(helper, ModToolItems.hand_drill, "hand_drill", ToolType.HAND_DRILL, 100);
        assertTool(helper, ModToolItems.defuser, "defuser", ToolType.DEFUSER, 100);
        helper.succeed();
    }

    public static void durability(GameTestHelper helper) {
        // The production table has only TORCH/BOLT entries today. These no-material entries isolate the
        // ItemTooling wear contract while exercising the same IToolable block consumer used by machines.
        for (ToolType type : List.of(ToolType.SCREWDRIVER, ToolType.HAND_DRILL, ToolType.DEFUSER)) {
            NTMToolHandler.register(type, ModMachineBlocks.fusion_component::get,
                    ModMachineBlocks.fusion_component_bscco_welded::get);
        }

        for (int i = 0; i < 3; i++) {
            ItemTooling tool = switch (i) {
                case 0 -> ModToolItems.screwdriver.get();
                case 1 -> ModToolItems.hand_drill.get();
                default -> ModToolItems.defuser.get();
            };
            ItemStack stack = new ItemStack(tool);
            helper.getLevel().setBlock(helper.absolutePos(new BlockPos(i + 1, 1, 1)),
                    ModMachineBlocks.fusion_component.get().defaultBlockState(), 3);
            invoke(helper, stack, i + 1);
            helper.assertTrue(stack.getDamageValue() == 1,
                    tool.getType() + " must cost one durability on an accepted action");
        }

        for (int i = 0; i < 3; i++) {
            ItemTooling tool = switch (i) {
                case 0 -> ModToolItems.screwdriver.get();
                case 1 -> ModToolItems.hand_drill.get();
                default -> ModToolItems.defuser.get();
            };
            ItemStack stack = new ItemStack(tool);
            helper.getLevel().setBlock(helper.absolutePos(new BlockPos(i + 5, 1, 1)),
                    ModMachineBlocks.watz_casing.get().defaultBlockState(), 3);
            invoke(helper, stack, i + 5);
            helper.assertTrue(stack.getDamageValue() == 0,
                    tool.getType() + " must not cost durability on a rejected action");
        }
        helper.succeed();
    }

    private static void invoke(GameTestHelper helper, ItemStack stack, int x) {
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(new BlockPos(x, 1, 1));
        BlockHitResult hit = new BlockHitResult(
                new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D),
                Direction.UP, pos, false);
        stack.getItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
    }

    private static void assertTool(GameTestHelper helper, RegistryObject<? extends Item> entry, String path,
                                   ToolType type, int durability) {
        Item item = entry.get();
        ItemStack stack = new ItemStack(item);
        helper.assertTrue(Ids.of(Constants.MOD_ID, path).equals(BuiltInRegistries.ITEM.getKey(item)),
                "Wrong registry id for " + path);
        helper.assertTrue(stack.getMaxStackSize() == 1 && stack.getMaxDamage() == durability,
                path + " has incorrect stack/durability properties");
        helper.assertTrue(ToolType.getType(stack) == type,
                path + " has incorrect ToolType classification");
    }
}
