package com.hbm.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.blocks.machine.MachineAshpit;
import com.hbm.blocks.machine.MachineChimneyBrick;
import com.hbm.blocks.machine.MachineChimneyIndustrial;
import com.hbm.inventory.container.ContainerAshpit;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ModIngredientItems;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityAshpit;
import com.hbm.tileentity.machine.TileEntityChimneyBrick;
import com.hbm.tileentity.machine.TileEntityChimneyIndustrial;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.net.GolemBuffers;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import com.hbm.blocks.machine.ModMachineBlocks;

/** Feature-local contracts for the smokestack-to-ashpit chain. */
public final class SmokeChainGameTests {

    private SmokeChainGameTests() {}

    public static void layouts(GameTestHelper helper) {
        BlockPos brickPos = new BlockPos(3, 1, 3);
        MachineChimneyBrick brick = ModMachineBlocks.chimney_brick.get();
        FoldedTestSupport.assemble(helper, brick, brickPos);

        BlockPos industrialPos = new BlockPos(12, 1, 3);
        MachineChimneyIndustrial industrial = ModMachineBlocks.chimney_industrial.get();
        FoldedTestSupport.assemble(helper, industrial, industrialPos);

        helper.assertTrue(Arrays.equals(brick.getDimensions(), new int[]{12, 0, 1, 1, 1, 1}),
                "Brick chimney dimensions changed");
        helper.assertTrue(Arrays.equals(industrial.getDimensions(), new int[]{22, 0, 1, 1, 1, 1}),
                "Industrial chimney dimensions changed");
        assertTowerCells(helper, helper.absolutePos(brickPos), 12, brick);
        assertTowerCells(helper, helper.absolutePos(industrialPos), 22, industrial);
        FoldedTestSupport.assertPort(helper, brickPos, brickPos.east());
        FoldedTestSupport.assertPort(helper, industrialPos, industrialPos.north());
        helper.succeed();
    }

    public static void portsAndCapture(GameTestHelper helper) {
        TileEntityChimneyBrick chimney = placeBrick(helper, new BlockPos(3, 3, 3));
        MachineChimneyBrick brick = ModMachineBlocks.chimney_brick.get();
        BlockPos core = chimney.getBlockPos();
        helper.assertTrue(chimney.getPollutionMod() == 0.25D,
                "Brick chimney pollution multiplier changed");
        DirPos[] ports = chimney.getConPos();
        helper.assertTrue(ports.length == 4, "Brick chimney must expose four smoke ports");
        assertPort(helper, ports[0], core.east(2), ForgeDirection.EAST);
        assertPort(helper, ports[1], core.west(2), ForgeDirection.WEST);
        assertPort(helper, ports[2], core.south(2), ForgeDirection.SOUTH);
        assertPort(helper, ports[3], core.north(2), ForgeDirection.NORTH);

        for (BlockPos cell : new BlockPos[]{core.east(), core.west(), core.south(), core.north()}) {
            ForgeDirection direction = directionFrom(core, cell);
            helper.assertTrue(brick.canConnect(Fluids.SMOKE, helper.getLevel(), cell.getX(), cell.getY(), cell.getZ(), direction),
                    "Brick chimney rejected SMOKE at " + cell);
            helper.assertTrue(brick.canConnect(Fluids.SMOKE_LEADED, helper.getLevel(), cell.getX(), cell.getY(), cell.getZ(), direction),
                    "Brick chimney rejected SMOKE_LEADED at " + cell);
            helper.assertTrue(brick.canConnect(Fluids.SMOKE_POISON, helper.getLevel(), cell.getX(), cell.getY(), cell.getZ(), direction),
                    "Brick chimney rejected SMOKE_POISON at " + cell);
            helper.assertFalse(brick.canConnect(Fluids.WATER, helper.getLevel(), cell.getX(), cell.getY(), cell.getZ(), direction),
                    "Brick chimney accepted a non-smoke fluid");
            helper.assertFalse(brick.canConnect(Fluids.SMOKE, helper.getLevel(), cell.getX(), cell.getY(), cell.getZ(), ForgeDirection.UP),
                    "Brick chimney exposed a vertical smoke port");
        }

        MachineAshpit ashpitBlock = ModMachineBlocks.machine_ashpit.get();
        BlockPos ashpitPos = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlock(ashpitPos,
                ashpitBlock.defaultBlockState(), 3);
        TileEntityAshpit ashpit = (TileEntityAshpit) helper.getLevel().getBlockEntity(ashpitPos);
        helper.assertTrue(ashpit != null, "Ashpit core BlockEntity was not created");

        chimney.transferFluid(Fluids.SMOKE, 0, 123L);
        TileEntityChimneyBrick.serverTick(helper.getLevel(), core, chimney.getBlockState(), chimney);
        helper.assertTrue(ashpit.ashLevelFly == 123 && chimney.ashTick == 0 && chimney.sootTick == 0,
                "Brick chimney did not deliver captured fly ash to the ashpit");
        helper.succeed();
    }

    public static void industrialSoot(GameTestHelper helper) {
        TileEntityChimneyIndustrial chimney = placeIndustrial(helper, new BlockPos(3, 3, 3));
        helper.assertTrue(chimney.getPollutionMod() == 0.1D,
                "Industrial chimney pollution multiplier changed");
        chimney.transferFluid(Fluids.SMOKE_POISON, 0, 9L);
        helper.assertTrue(chimney.ashTick == 9 && chimney.sootTick == 9 && chimney.onTicks == 20,
                "Industrial chimney capture counters are wrong");
        helper.assertTrue(chimney.transferFluid(Fluids.WATER, 0, 7L) == 7L,
                "Chimney must return unsupported fluid unchanged");
        helper.succeed();
    }

    public static void ashpitProcessingAndHoppers(GameTestHelper helper) {
        TileEntityAshpit ashpit = placeAshpit(helper, new BlockPos(3, 1, 3));
        ashpit.ashLevelWood = TileEntityAshpit.thresholdWood;
        TileEntityAshpit.serverTick(helper.getLevel(), ashpit.getBlockPos(), ashpit.getBlockState(), ashpit);

        helper.assertTrue(ashpit.inventory.getStackInSlot(0).is(ModIngredientItems.ash(EnumAshType.WOOD)),
                "Ashpit did not output wood ash");
        helper.assertTrue(ashpit.ashLevelWood == -TileEntityAshpit.thresholdWood,
                "Ashpit did not preserve the legacy empty-slot extra subtraction");
        helper.assertTrue(ashpit.isFull, "Ashpit full indicator did not follow non-empty output slots");

        MachineAshpit block = ModMachineBlocks.machine_ashpit.get();
        WorldlyContainer exposed = block.getContainer(
                helper.getLevel().getBlockState(ashpit.getBlockPos().east()), helper.getLevel(), ashpit.getBlockPos().east());
        helper.assertTrue(exposed == ashpit && exposed.getSlotsForFace(Direction.DOWN).length == 5,
                "Ashpit did not expose all five output slots through its dummy cells");
        helper.assertFalse(exposed.canPlaceItemThroughFace(0,
                        new ItemStack(ModIngredientItems.ash(EnumAshType.WOOD)),
                        Direction.DOWN),
                "Ashpit accepted hopper insertion into an output slot");
        helper.assertTrue(exposed.canTakeItemThroughFace(0, ashpit.inventory.getStackInSlot(0),
                        Direction.DOWN), "Ashpit did not allow hopper extraction");
        helper.succeed();
    }

    public static void menuAndSync(GameTestHelper helper) {
        TileEntityAshpit ashpit = placeAshpit(helper, new BlockPos(3, 1, 3));
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ContainerAshpit menu = new ContainerAshpit(0, player.getInventory(), ashpit);
        helper.assertTrue(menu.slots.size() == 41
                        && menu.slots.get(0).x == 44 && menu.slots.get(0).y == 27
                        && menu.slots.get(4).x == 116 && menu.slots.get(4).y == 27
                        && menu.slots.get(5).x == 8 && menu.slots.get(5).y == 86
                        && menu.slots.get(40).x == 152 && menu.slots.get(40).y == 144,
                "Ashpit menu layout changed");
        helper.assertFalse(menu.slots.get(0).mayPlace(new ItemStack(
                        ModIngredientItems.ash(EnumAshType.WOOD))), "Ashpit output slot accepted GUI insertion");
        helper.assertTrue(ashpit.playersUsing == 1, "Ashpit menu did not increment playersUsing");
        menu.removed(player);
        helper.assertTrue(ashpit.playersUsing == 0, "Ashpit menu did not decrement playersUsing");

        ashpit.playersUsing = 2;
        ashpit.isFull = true;
        RegistryFriendlyByteBuf buffer = GolemBuffers.wrap(
                Unpooled.buffer(), helper.getLevel());
        ashpit.serialize(buffer);
        TileEntityAshpit copy = new TileEntityAshpit(BlockPos.ZERO,
                ModMachineBlocks.machine_ashpit.get().defaultBlockState());
        copy.deserialize(buffer);
        helper.assertTrue(copy.playersUsing == 2 && copy.isFull,
                "Ashpit GUI sync fields did not round-trip");
        helper.succeed();
    }

    public static void itemModels(GameTestHelper helper) {
        assertItemModel(helper, "chimney_brick", "hbm:models/machines/chimney_brick.obj",
                "hbm:textures/models/machines/chimney_brick.png", 0.5D, 0D, true);
        assertItemModel(helper, "chimney_industrial", "hbm:models/machines/chimney_industrial.obj",
                "hbm:textures/models/machines/chimney_industrial.png", 0.25D, 0D, true);
        assertItemModel(helper, "machine_ashpit", "hbm:models/machines/heating_oven.obj",
                "hbm:textures/models/machines/ashpit.png", 1.0D, 0D, true);
        helper.succeed();
    }

    private static TileEntityChimneyBrick placeBrick(GameTestHelper helper, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        MachineChimneyBrick block = ModMachineBlocks.chimney_brick.get();
        FoldedTestSupport.assemble(helper, block, relative);
        return (TileEntityChimneyBrick) helper.getLevel().getBlockEntity(pos);
    }

    private static TileEntityChimneyIndustrial placeIndustrial(GameTestHelper helper, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        MachineChimneyIndustrial block = ModMachineBlocks.chimney_industrial.get();
        FoldedTestSupport.assemble(helper, block, relative);
        return (TileEntityChimneyIndustrial) helper.getLevel().getBlockEntity(pos);
    }

    private static TileEntityAshpit placeAshpit(GameTestHelper helper, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        MachineAshpit block = ModMachineBlocks.machine_ashpit.get();
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 3);
        block.fillSpace(helper.getLevel(), pos, Direction.NORTH);
        return (TileEntityAshpit) helper.getLevel().getBlockEntity(pos);
    }

    private static void assertTowerCells(GameTestHelper helper, BlockPos core, int height, Object block) {
        for (int y = 0; y <= height; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    helper.assertTrue(helper.getLevel().getBlockState(core.offset(dx, y, dz)).getBlock() == block,
                            "Chimney tower cell missing at " + dx + "," + y + "," + dz);
                }
            }
        }
    }

    private static ForgeDirection directionFrom(BlockPos core, BlockPos cell) {
        if (cell.getX() > core.getX()) return ForgeDirection.EAST;
        if (cell.getX() < core.getX()) return ForgeDirection.WEST;
        if (cell.getZ() > core.getZ()) return ForgeDirection.SOUTH;
        return ForgeDirection.NORTH;
    }

    private static void assertPort(GameTestHelper helper, DirPos actual, BlockPos expected, ForgeDirection direction) {
        helper.assertTrue(actual.getPos().equals(expected) && actual.getDir() == direction,
                "Chimney port mismatch: expected " + expected + "/" + direction
                        + ", got " + actual.getPos() + "/" + actual.getDir());
    }

    private static void assertItemModel(GameTestHelper helper, String name, String obj, String texture,
                                        double scale, double yaw, boolean legacy) {
        try (InputStream stream = SmokeChainGameTests.class.getClassLoader()
                .getResourceAsStream("assets/hbm/items/" + name + ".json")) {
            JsonObject root = stream == null ? null
                    : JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            JsonObject outer = root == null ? null : root.getAsJsonObject("model");
            boolean direct = outer != null && "hbm:machine_obj".equals(outer.get("type").getAsString());
            JsonObject model = direct ? outer
                    : outer == null ? null : outer.getAsJsonObject("model");
            boolean shape = direct == legacy && (direct
                    || (outer != null && "minecraft:special".equals(outer.get("type").getAsString())
                            && "hbm:item/machine_obj_base".equals(outer.get("base").getAsString())));
            helper.assertTrue(shape
                            && model != null && "hbm:machine_obj".equals(model.get("type").getAsString())
                            && obj.equals(model.get("obj").getAsString())
                            && texture.equals(model.get("texture").getAsString())
                            && Math.abs(model.get("scale").getAsDouble() - scale) < 0.000001D
                            && Math.abs(model.get("yaw").getAsDouble() - yaw) < 0.000001D
                            && model.get("ty").getAsDouble() == 0D
                            && model.has("rawLegacyTransform") == legacy,
                    "Invalid smoke-chain item model: " + name);
        } catch (IOException | RuntimeException exception) {
            helper.fail("Could not read item model " + name + ": " + exception);
        }
    }
}
