package com.hbm.gametest;

import net.minecraft.core.Direction;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.blocks.machine.storage.MachineBatteryREDD;
import com.hbm.blocks.machine.storage.ModBatteryBlocks;
import com.hbm.inventory.container.ContainerBatteryREDD;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.storage.TileEntityBatteryREDD;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Focused contracts for the current machine_battery_redd FEnSU. */
public final class BatteryReddGameTests {

    private static final BigInteger ABOVE_LONG = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.valueOf(12345L));

    private BatteryReddGameTests() {}

    public static void layoutAndPorts(GameTestHelper helper) {
        MachineBatteryREDD block = ModBatteryBlocks.machine_battery_redd.get();
        BlockPos relative = new BlockPos(10, 1, 10);
        BlockPos core = helper.absolutePos(relative);
        FoldedTestSupport.assemble(helper, block, relative);

        int cells = 0;
        int extras = 0;
        for (int x = -4; x <= 4; x++) for (int z = -2; z <= 2; z++) {
            BlockPos cell = core.offset(x, 0, z);
            if (helper.getLevel().getBlockState(cell).getBlock() != block) continue;
            cells++;
            if (MultiblockSurface.carriesPassive(helper.getLevel(), cell,
                    dev.golem.core.multiblock.MultiblockCore.PASSIVE_POWER_IN)) extras++;
        }
        helper.assertTrue(cells == 45 && extras == 6, "FEnSU occupancy or six extra port cells changed");
        helper.assertTrue(java.util.Arrays.equals(block.getDimensions(), new int[]{9, 0, 2, 2, 4, 4})
                        && block.getOffset() == 2, "FEnSU dimensions/offset changed");

        TileEntityBatteryREDD battery = (TileEntityBatteryREDD) helper.getLevel().getBlockEntity(core);
        DirPos[] connections = battery.getConPos();
        assertConnection(helper, connections[0], core.offset(2, 0, -3), ForgeDirection.NORTH);
        assertConnection(helper, connections[1], core.offset(-2, 0, -3), ForgeDirection.NORTH);
        assertConnection(helper, connections[2], core.offset(2, 0, 3), ForgeDirection.SOUTH);
        assertConnection(helper, connections[3], core.offset(-2, 0, 3), ForgeDirection.SOUTH);
        assertConnection(helper, connections[4], core.offset(5, 0, 0), ForgeDirection.EAST);
        assertConnection(helper, connections[5], core.offset(-5, 0, 0), ForgeDirection.WEST);
        for (DirPos connection : connections) {
            BlockPos cell = connection.getPos().offset(-connection.getDir().offsetX,
                    -connection.getDir().offsetY, -connection.getDir().offsetZ);
            helper.assertTrue(block.canConnect(helper.getLevel(), cell, connection.getDir().toDirection()),
                    "FEnSU connection is not exposed at its anchor");
        }
        helper.assertFalse(block.canConnect(helper.getLevel(), core, Direction.NORTH),
                "FEnSU exposed a non-port side");
        helper.succeed();
    }

    public static void bigIntegerAndTransfer(GameTestHelper helper) {
        TileEntityBatteryREDD battery = place(helper, new BlockPos(3, 1, 3));
        helper.assertTrue(battery.getMaxPower() == Long.MAX_VALUE / 100L,
                "FEnSU exposed capacity changed");
        battery.power = ABOVE_LONG;
        helper.assertTrue(battery.getPower() == TileEntityBatteryREDD.MAX_POWER / 2L,
                "FEnSU exposed power must be min(power,max/2)");

        battery.power = BigInteger.ZERO;
        long remainder = battery.transferPower(Long.MAX_VALUE, true);
        helper.assertTrue(remainder == 0L && battery.power.signum() == 0,
                "simulated FEnSU transfer changed power or return contract");
        battery.transferPower(Long.MAX_VALUE, false);
        helper.assertTrue(battery.power.equals(BigInteger.valueOf(Long.MAX_VALUE)),
                "FEnSU transfer did not accept the complete positive long");
        battery.setPower(5L);
        helper.assertTrue(battery.power.equals(BigInteger.valueOf(Long.MAX_VALUE)),
                "FEnSU setPower must not truncate the arbitrary-precision store");

        battery.power = BigInteger.valueOf(123456789L);
        float expected = (float) Math.min(Math.pow(Math.log(123456789L * 0.05D + 1D) * 0.05F, 5), 15F);
        helper.assertTrue(Float.compare(battery.getSpeed(), expected) == 0, "FEnSU speed formula changed");

        battery.power = BigInteger.valueOf(100L);
        for (int tick = 0; tick < 20; tick++) {
            TileEntityBatteryREDD.serverTick(helper.getLevel(), battery.getBlockPos(), battery.getBlockState(), battery);
        }
        helper.assertTrue(battery.delta.equals(BigInteger.valueOf(100L)),
                "FEnSU delta must use the exact 20-sample rolling window");
        helper.succeed();
    }

    public static void persistenceAndSync(GameTestHelper helper) {
        TileEntityBatteryREDD original = place(helper, new BlockPos(3, 1, 3));
        original.power = ABOVE_LONG;
        original.delta = BigInteger.valueOf(-987654321L);
        original.redLow = TileEntityBatteryREDD.mode_buffer;
        original.redHigh = TileEntityBatteryREDD.mode_none;
        original.priority = com.hbm.api.energymk2.IEnergyReceiverMK2.ConnectionPriority.HIGH;
        original.muffled = true;

        RegistryFriendlyByteBuf buffer = dev.golem.core.net.GolemBuffers.wrap(
                io.netty.buffer.Unpooled.buffer(), helper.getLevel());
        original.serialize(buffer);
        TileEntityBatteryREDD copy = new TileEntityBatteryREDD(BlockPos.ZERO,
                ModBatteryBlocks.machine_battery_redd.get().defaultBlockState());
        copy.deserialize(buffer);
        helper.assertTrue(copy.power.equals(original.power) && copy.delta.equals(original.delta)
                        && copy.redLow == original.redLow && copy.redHigh == original.redHigh
                        && copy.priority == original.priority,
                "FEnSU BufPacket fields did not round-trip losslessly");

        MachineBatteryREDD block = ModBatteryBlocks.machine_battery_redd.get();
        ItemStack dropped = block.getDrops(original.getBlockState(), new LootParams.Builder((ServerLevel) helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(original.getBlockPos()))).get(0);
        helper.assertTrue(ModDataComponents.batteryReddPower(dropped).equals(ABOVE_LONG),
                "FEnSU dropped item did not retain arbitrary-precision power");
        helper.assertTrue(ModDataComponents.batteryReddMuffled(dropped),
                "FEnSU dropped item did not retain its muffled state");

        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos clicked = helper.absolutePos(new BlockPos(18, 1, 3));
        player.setYRot(0F);
        player.setPos(clicked.getX() - 4D, clicked.getY(), clicked.getZ() - 4D);
        helper.getLevel().setBlock(clicked, block.defaultBlockState(), 3);
        block.setPlacedBy(helper.getLevel(), clicked, block.defaultBlockState(), player, dropped);
        TileEntityBatteryREDD restored = (TileEntityBatteryREDD) helper.getLevel()
                .getBlockEntity(clicked.south(2));
        helper.assertTrue(restored != null && restored.power.equals(ABOVE_LONG) && restored.muffled,
                "FEnSU placement did not restore dropped power and muffled state");
        helper.succeed();
    }

    public static void menuAndItemContract(GameTestHelper helper) {
        TileEntityBatteryREDD battery = place(helper, new BlockPos(3, 1, 3));
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ContainerBatteryREDD menu = new ContainerBatteryREDD(0, player.getInventory(), battery);
        helper.assertTrue(menu.slots.size() == 38
                        && menu.slots.get(0).x == 26 && menu.slots.get(0).y == 53
                        && menu.slots.get(1).x == 80 && menu.slots.get(1).y == 53
                        && menu.slots.get(2).x == 8 && menu.slots.get(2).y == 99,
                "FEnSU menu coordinates or slot count changed");
        ItemStack batteryItem = new ItemStack(com.hbm.items.ModBatteryItems.PACKS.get(
                com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack.BATTERY_QUANTUM).get());
        helper.assertTrue(menu.slots.get(0).mayPlace(batteryItem) && menu.slots.get(1).mayPlace(batteryItem),
                "FEnSU slots must accept battery items");
        helper.assertTrue(battery.getSlotsForFace(net.minecraft.core.Direction.NORTH).length == 0
                        && !battery.canPlaceItemThroughFace(0, batteryItem, net.minecraft.core.Direction.NORTH)
                        && !battery.canTakeItemThroughFace(1, batteryItem, net.minecraft.core.Direction.NORTH),
                "FEnSU must not expose its GUI-only slots to sided automation");
        helper.assertTrue(menu.clickMenuButton(player, 0) && battery.redLow == TileEntityBatteryREDD.mode_buffer
                        && menu.clickMenuButton(player, 1) && battery.redHigh == TileEntityBatteryREDD.mode_none
                        && menu.clickMenuButton(player, 2)
                        && battery.priority == com.hbm.api.energymk2.IEnergyReceiverMK2.ConnectionPriority.NORMAL,
                "FEnSU mode/priority buttons changed");
        helper.succeed();
    }

    public static void itemModels(GameTestHelper helper) {
        assertItemModel(helper, "machine_fensu", "hbm:models/machines/fensu.obj",
                "hbm:textures/models/machines/fensus/fensu_light_blue.png", 0.21D, 90D, false, null, false);
        assertItemModel(helper, "machine_bigasstank", "hbm:models/machines/bigasstank.obj",
                "hbm:textures/models/machines/bigasstank.png", 0.5D, 0D, false, null, true);
        assertItemModel(helper, "machine_bat9000", "hbm:models/machines/bat9000.obj",
                "hbm:textures/models/machines/bat9000.png", 0.2D, 0D, false, null, false);
        assertItemModel(helper, "machine_battery_redd", "hbm:models/machines/fensu2.obj",
                "hbm:textures/models/machines/fensu2.png", 0.5D, -90D, true, "Plasma", true);
        helper.succeed();
    }

    private static void assertItemModel(GameTestHelper helper, String name, String obj, String texture,
                                        double scale, double yaw, boolean excluded, String excludedPart,
                                        boolean legacy) {
        try (InputStream stream = BatteryReddGameTests.class.getClassLoader()
                .getResourceAsStream("assets/hbm/items/" + name + ".json")) {
            JsonObject root = stream == null ? null
                    : JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject outer = root == null ? null : root.getAsJsonObject("model");
            boolean direct = outer != null && "hbm:machine_obj".equals(outer.get("type").getAsString());
            JsonObject model = direct ? outer : outer == null ? null : outer.getAsJsonObject("model");
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
                            && model.has("rawLegacyTransform") == legacy
                            && (!legacy || root.get("oversized_in_gui").getAsBoolean())
                            && (!excluded || contains(model.getAsJsonArray("exclude"), excludedPart)),
                    (legacy ? "Invalid 1.7-framed OBJ item contract: " : "Invalid centered OBJ item contract: ")
                            + name);
        } catch (IOException | RuntimeException exception) {
            helper.fail("Could not read item model " + name + ": " + exception);
        }
    }

    private static boolean contains(com.google.gson.JsonArray values, String value) {
        if (values == null) return false;
        for (var element : values) if (value.equals(element.getAsString())) return true;
        return false;
    }

    private static void assertConnection(GameTestHelper helper, DirPos actual, BlockPos expected,
                                         ForgeDirection direction) {
        helper.assertTrue(actual.getPos().equals(expected) && actual.getDir() == direction,
                "FEnSU anchor mismatch: expected " + expected + "/" + direction
                        + ", got " + actual.getPos() + "/" + actual.getDir());
    }

    private static TileEntityBatteryREDD place(GameTestHelper helper, BlockPos relative) {
        MachineBatteryREDD block = ModBatteryBlocks.machine_battery_redd.get();
        BlockPos core = helper.absolutePos(relative);
        FoldedTestSupport.assemble(helper, block, relative);
        helper.assertTrue(helper.getLevel().getBlockEntity(core) instanceof TileEntityBatteryREDD,
                "FEnSU core BlockEntity was not created");
        return (TileEntityBatteryREDD) helper.getLevel().getBlockEntity(core);
    }

}
