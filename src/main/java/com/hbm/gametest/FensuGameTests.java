package com.hbm.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blocks.machine.storage.MachineFENSU;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.storage.TileEntityBatteryBase;
import com.hbm.tileentity.machine.storage.TileEntityMachineFENSU;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.net.GolemBuffers;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.tags.TagKey;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Focused contracts for the legacy FEnSU storage machine. */
public final class FensuGameTests {

    private FensuGameTests() {}

    public static void numeric(GameTestHelper helper) {
        TileEntityMachineFENSU fensu = place(helper, new BlockPos(3, 1, 3));
        helper.assertTrue(fensu.getMaxPower() == Long.MAX_VALUE, "FEnSU capacity changed");
        helper.assertTrue(fensu.getProviderSpeed() == 0L
                        && fensu.getReceiverSpeed() == TileEntityMachineFENSU.MAX_TRANSFER,
                "default FEnSU transfer mode/rate changed");

        fensu.setPower(0L);
        helper.assertTrue(fensu.getComparatorPower() == 0, "empty FEnSU comparator output changed");
        long excess = fensu.transferPower(Long.MAX_VALUE, true);
        helper.assertTrue(excess == Long.MAX_VALUE - TileEntityMachineFENSU.MAX_TRANSFER
                        && fensu.power == 0L, "simulated transfer overflowed or changed power");
        fensu.transferPower(Long.MAX_VALUE, false);
        helper.assertTrue(fensu.power == TileEntityMachineFENSU.MAX_TRANSFER,
                "FEnSU did not enforce its exact transfer cap");

        fensu.setPower(Long.MAX_VALUE);
        helper.assertTrue(fensu.getComparatorPower() == 15, "full FEnSU comparator output changed");
        helper.assertTrue(fensu.getPowerRemainingScaled(52L) == 52L,
                "full FEnSU GUI scale is not 52");
        helper.assertTrue(fensu.transferPower(1L, false) == 1L && fensu.power == Long.MAX_VALUE,
                "full FEnSU accepted energy past Long.MAX_VALUE");
        helper.succeed();
    }

    public static void layoutAndPorts(GameTestHelper helper) {
        MachineFENSU block = ModBatteryBlocks.machine_fensu.get();
        BlockPos relativeCore = new BlockPos(3, 1, 3);
        BlockPos core = helper.absolutePos(relativeCore);
        helper.getLevel().setBlock(core, block.defaultBlockState(), 3);
        block.fillSpace(helper.getLevel(), core, Direction.NORTH);

        int cells = 0;
        for (int x = -2; x <= 2; x++) for (int y = 0; y <= 4; y++) for (int z = -1; z <= 1; z++) {
            if (helper.getLevel().getBlockState(core.offset(x, y, z)).getBlock() == block) cells++;
        }
        helper.assertTrue(cells == 75, "FEnSU structure occupancy changed: " + cells);
        helper.assertTrue(core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), core.offset(2, 4, 1))),
                "FEnSU dummy did not resolve through the full tower");

        TileEntityMachineFENSU fensu = (TileEntityMachineFENSU) helper.getLevel().getBlockEntity(core);
        DirPos[] connections = fensu.getConPos();
        helper.assertTrue(connections.length == 1 && connections[0].getPos().equals(core.below())
                        && connections[0].getDir() == ForgeDirection.DOWN,
                "FEnSU must have exactly one bottom connection");
        helper.assertTrue(block.canConnect(helper.getLevel(), core, ForgeDirection.DOWN),
                "FEnSU bottom cable port is missing");
        helper.assertFalse(block.canConnect(helper.getLevel(), core, ForgeDirection.NORTH),
                "FEnSU exposed a non-bottom cable port");

        var shape = block.getShape(helper.getLevel().getBlockState(core), helper.getLevel(), core,
                CollisionContext.empty());
        helper.assertTrue(Math.abs((shape.max(Direction.Axis.X)
                        - shape.min(Direction.Axis.X)) - 5D) < 0.0001D
                        && Math.abs((shape.max(Direction.Axis.Y)
                        - shape.min(Direction.Axis.Y)) - 5D) < 0.0001D
                        && Math.abs((shape.max(Direction.Axis.Z)
                        - shape.min(Direction.Axis.Z)) - 3D) < 0.0001D,
                "FEnSU MBB bounds were not retained");
        helper.assertTrue(Math.abs(shape.min(Direction.Axis.X) + 2.5D) < 0.0001D
                        && Math.abs(shape.max(Direction.Axis.X) - 2.5D) < 0.0001D
                        && Math.abs(shape.min(Direction.Axis.Y)) < 0.0001D
                        && Math.abs(shape.max(Direction.Axis.Y) - 5D) < 0.0001D
                        && Math.abs(shape.min(Direction.Axis.Z) + 0.5D) < 0.0001D
                        && Math.abs(shape.max(Direction.Axis.Z) - 2.5D) < 0.0001D,
                "FEnSU MBB bounds have the wrong rotated offsets");
        helper.succeed();
    }

    public static void persistenceAndSync(GameTestHelper helper) {
        TileEntityMachineFENSU original = place(helper, new BlockPos(3, 1, 3));
        original.power = 8_000_000_000_000_000_000L;
        original.delta = -123_456L;
        original.color = DyeColor.RED;
        original.redLow = TileEntityBatteryBase.mode_buffer;
        original.redHigh = TileEntityBatteryBase.mode_none;
        original.priority = IEnergyReceiverMK2.ConnectionPriority.HIGH;

        MachineFENSU block = ModBatteryBlocks.machine_fensu.get();
        ItemStack dropped = block.getDrops(original.getBlockState(), new LootParams.Builder((ServerLevel) helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(original.getBlockPos()))).get(0);
        helper.assertTrue(ModDataComponents.fensuPower(dropped) == original.power
                        && ModDataComponents.FENSU_COLOR.getOr(dropped, "").equals("red")
                        && ModDataComponents.fensuRedLow(dropped, -1) == original.redLow
                        && ModDataComponents.fensuRedHigh(dropped, -1) == original.redHigh
                        && ModDataComponents.FENSU_PRIORITY.getOr(dropped, "").equals("high"),
                "FEnSU power/color/modes were not carried by its dropped item");

        BlockPos clicked = helper.absolutePos(new BlockPos(10, 1, 3));
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);
        player.setPos(clicked.getX() - 4D, clicked.getY(), clicked.getZ() - 4D);
        helper.getLevel().setBlock(clicked, block.defaultBlockState(), 3);
        block.setPlacedBy(helper.getLevel(), clicked, block.defaultBlockState(), player, dropped);
        TileEntityMachineFENSU restored = (TileEntityMachineFENSU) helper.getLevel()
                .getBlockEntity(clicked.south());
        helper.assertTrue(restored != null && restored.power == original.power && restored.color == original.color
                        && restored.redLow == original.redLow && restored.redHigh == original.redHigh
                        && restored.priority == original.priority,
                "FEnSU item power/color/modes were not restored on placement");

        TileEntityMachineFENSU copy = new TileEntityMachineFENSU(BlockPos.ZERO,
                block.defaultBlockState());
        var buf = GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        original.serialize(buf);
        copy.deserialize(buf);
        helper.assertTrue(copy.power == original.power && copy.delta == original.delta
                        && copy.color == original.color && copy.redLow == original.redLow
                        && copy.redHigh == original.redHigh && copy.priority == original.priority,
                "FEnSU synced fields did not round-trip");
        helper.succeed();
    }

    public static void assets(GameTestHelper helper) {
        helper.assertTrue(resourceExists("assets/hbm/models/machines/fensu.obj"), "FEnSU OBJ is missing");
        for (DyeColor color : DyeColor.values()) {
            helper.assertTrue(resourceExists("assets/hbm/textures/models/machines/fensus/fensu_"
                            + color.getName() + ".png"), "FEnSU texture is missing: " + color.getName());
        }
        try (InputStream stream = FensuGameTests.class.getClassLoader()
                .getResourceAsStream("assets/hbm/items/machine_fensu.json")) {
            JsonObject root = stream == null ? null
                    : JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            // 1.7 ItemRenderLibrary machine_fensu: inv rotate(90, Y) translate(0, -2, 0) scale(2.5), raw (icon-P2)
            JsonObject model = root == null ? null : root.getAsJsonObject("model");
            helper.assertTrue(model != null && "hbm:machine_obj".equals(model.get("type").getAsString())
                            && model.has("rawLegacyTransform") && model.get("rawLegacyTransform").getAsBoolean()
                            && Math.abs(model.get("scale").getAsDouble() - 1D) < 0.000001D
                            && Math.abs(model.get("yaw").getAsDouble()) < 0.000001D
                            && Math.abs(model.get("guiScale").getAsDouble() - 2.5D) < 0.000001D
                            && Math.abs(model.get("guiYaw").getAsDouble() - 90D) < 0.000001D
                            && Math.abs(model.getAsJsonArray("guiTranslate").get(1).getAsDouble() + 2D) < 0.000001D
                            && model.get("ty").getAsDouble() == 0D
                            && root.get("oversized_in_gui").getAsBoolean(),
                    "FEnSU item model is not the 1.7 raw ItemRenderLibrary contract");
        } catch (IOException | RuntimeException exception) {
            helper.fail("FEnSU item model could not be read: " + exception);
        }
        try (InputStream stream = FensuGameTests.class.getClassLoader()
                .getResourceAsStream("assets/hbm/blockstates/machine_fensu.json")) {
            JsonObject root = stream == null ? null
                    : JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            Set<String> variants = root == null || !root.has("variants") ? Set.of()
                    : root.getAsJsonObject("variants").keySet();
            List<String> expected = new ArrayList<>();
            for (String facing : new String[]{"north", "east", "south", "west"}) {
                for (String cell : new String[]{"false", "true"}) {
                    expected.add("facing=" + facing + ",cell=" + cell);
                }
            }
            helper.assertTrue(variants.size() == expected.size() && variants.containsAll(expected),
                    "FEnSU blockstate does not declare every facing x cell variant: " + variants);
        } catch (IOException | RuntimeException exception) {
            helper.fail("FEnSU blockstate could not be read: " + exception);
        }
        helper.succeed();
    }

    /** The c:dyes tags retain the old metadata order while allowing compatible items from other mods. */
    public static void dyeTags(GameTestHelper helper) {
        Item[] vanillaDyes = {
                Items.WHITE_DYE, Items.ORANGE_DYE, Items.MAGENTA_DYE, Items.LIGHT_BLUE_DYE,
                Items.YELLOW_DYE, Items.LIME_DYE, Items.PINK_DYE, Items.GRAY_DYE,
                Items.LIGHT_GRAY_DYE, Items.CYAN_DYE, Items.PURPLE_DYE, Items.BLUE_DYE,
                Items.BROWN_DYE, Items.GREEN_DYE, Items.RED_DYE, Items.BLACK_DYE
        };
        helper.assertTrue(vanillaDyes.length == DyeColor.values().length,
                "FEnSU dye metadata count changed");
        for (int i = 0; i < vanillaDyes.length; i++) {
            DyeColor color = DyeColor.values()[i];
            TagKey<Item> tag = TagKey.create(Registries.ITEM,
                    Ids.of("c", "dyes/" + color.getName()));
            helper.assertTrue(new ItemStack(vanillaDyes[i]).is(tag),
                    "FEnSU dye tag is missing its vanilla member: " + color.getName());
        }
        helper.succeed();
    }

    private static boolean resourceExists(String name) {
        try (InputStream stream = FensuGameTests.class.getClassLoader().getResourceAsStream(name)) {
            return stream != null;
        } catch (IOException exception) {
            return false;
        }
    }

    private static TileEntityMachineFENSU place(GameTestHelper helper, BlockPos relativePos) {
        BlockPos pos = helper.absolutePos(relativePos);
        MachineFENSU block = ModBatteryBlocks.machine_fensu.get();
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        block.fillSpace(helper.getLevel(), pos, Direction.NORTH);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof TileEntityMachineFENSU,
                "FEnSU core BlockEntity was not created");
        return (TileEntityMachineFENSU) helper.getLevel().getBlockEntity(pos);
    }
}
