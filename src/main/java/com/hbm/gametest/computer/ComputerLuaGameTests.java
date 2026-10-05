package com.hbm.gametest.computer;

//? if computercraft {
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.blocks.ModProductionBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.tileentity.machine.TileEntityMicrowave;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.Recipes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A real CC: Tweaked computer running Lua against HBM machines: the end-to-end path the peripheral GameTests skip
 * (CC's own peripheral lookup on each loader, its argument conversion, its computer and main threads).
 *
 * <p>Each test writes a {@code startup.lua} into the save folder of a computer id it picks, then places a computer
 * with that id already switched on ({@code {ComputerId, On}} block-entity data, read by CC on load), the machine
 * directly above it. The program calls the peripheral on {@code top} and writes the results as JSON into
 * {@code result.txt}, which the test reads back.
 */
public final class ComputerLuaGameTests {

    private ComputerLuaGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function GEIGER = TESTS.function(
            "cc_lua_geiger", 400, () -> ComputerLuaGameTests::geiger);
    public static final GameTests.Function MICROWAVE = TESTS.function(
            "cc_lua_microwave", 400, () -> ComputerLuaGameTests::microwave);
    public static final GameTests.Function PWRANGLER = TESTS.function(
            "cc_lua_pwrangler_disk", 400, () -> ComputerLuaGameTests::pwranglerDisk);

    // ids far above any the test world hands out (its folder is deleted before every run); one per test
    private static final AtomicInteger NEXT_ID = new AtomicInteger(900_000);

    private static final BlockPos COMPUTER = new BlockPos(1, 1, 1);
    private static final BlockPos MACHINE = COMPUTER.above();

    private static void geiger(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModMachineBlocks.geiger.get().defaultBlockState());
        run(helper, """
                local out = { peripheral.getType("top") }
                local rads = peripheral.call("top", "getRads")
                out[#out + 1] = type(rads)
                return out
                """, result -> {
            helper.assertTrue("ntm_geiger".equals(result.get(0).getAsString()), "type " + result);
            helper.assertTrue("number".equals(result.get(1).getAsString()), "getRads " + result);
        });
    }

    private static void microwave(GameTestHelper helper) {
        helper.setBlock(MACHINE, ModProductionBlocks.MACHINE_MICROWAVE.get().defaultBlockState());
        run(helper, """
                local m = peripheral.wrap("top")
                local out = { peripheral.getType("top"), m.test() }
                out[#out + 1] = select(1, m.variableset(4.9))
                sleep(0.2) -- the getter reads the copy taken after a server tick
                out[#out + 1] = m.variableget()
                return out
                """, result -> {
            helper.assertTrue("microwave".equals(result.get(0).getAsString()), "type " + result);
            helper.assertTrue("This is a testing device for everything OC.".equals(result.get(1).getAsString()),
                    "test() " + result);
            helper.assertTrue("test of the `setter` callback function".equals(result.get(2).getAsString()),
                    "variableset " + result);
            // 1.7: checkInteger truncates 4.9 to 4
            helper.assertTrue(result.get(3).getAsInt() == 4, "variableget " + result);
            TileEntityMicrowave be = GameTestHelpers.blockEntity(helper, MACHINE, TileEntityMicrowave.class);
            helper.assertTrue(be.speed == 4, "block entity speed " + be.speed);
        });
    }

    /**
     * 1.7's PWRangler floppy: crafting a CC disk with a PWR casing gives the treasure disk, and a computer reading it
     * from a disk drive sees its label and its program (ComputerCraftRecipes, data/computercraft/lua/treasure/hbm).
     */
    private static void pwranglerDisk(GameTestHelper helper) {
        ItemStack blank = new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.of("computercraft", "disk")));
        ItemStack casing = new ItemStack(BuiltInRegistries.ITEM.getValue(Ids.of(Constants.MOD_ID, "pwr_casing")));
        ItemStack crafted = Recipes.craftingResult(helper.getLevel(), 2, 1, List.of(blank, casing)).orElse(ItemStack.EMPTY);
        helper.assertTrue(ItemStacks.matchesItemId(crafted, "computercraft:treasure_disk"),
                "disk + pwr_casing crafted " + crafted + ", expected computercraft:treasure_disk");
        BlockPos drive = helper.absolutePos(MACHINE);
        MinecraftServer server = helper.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                "setblock " + drive.getX() + " " + drive.getY() + " " + drive.getZ() + " computercraft:disk_drive");
        helper.assertTrue(helper.getLevel().getBlockEntity(drive) instanceof Container,
                "the disk drive is not a container");
        Container driveSlots = (Container) helper.getLevel().getBlockEntity(drive);
        driveSlots.setItem(0, crafted);
        // the Container contract: the writer signals the change (CC 1.20.1's drive mounts the disk only then)
        driveSlots.setChanged();
        helper.assertTrue(!driveSlots.getItem(0).isEmpty(), "the disk drive did not take the disk");
        run(helper, """
                local out = { disk.isPresent("top"), disk.getLabel("top") or false, peripheral.getType("top") or false }
                local path = disk.getMountPath("top")
                out[#out + 1] = path ~= nil and fs.exists(fs.combine(path, "pwrangler.lua"))
                return out
                """, result -> {
            helper.assertTrue(result.get(0).getAsBoolean(), "no disk present " + result);
            helper.assertTrue("PWRangler".equals(result.get(1).getAsString()), "label " + result);
            helper.assertTrue(result.get(3).getAsBoolean(), "pwrangler.lua not on the disk " + result);
        });
    }

    @FunctionalInterface
    private interface Check {
        void accept(JsonArray result);
    }

    /**
     * Boots a computer below {@link #MACHINE} running {@code body} (a Lua chunk returning a list) and runs
     * {@code check} on the list once the computer has written it. A Lua error fails the test with its message.
     */
    private static void run(GameTestHelper helper, String body, Check check) {
        MinecraftServer server = helper.getLevel().getServer();
        int id = NEXT_ID.getAndIncrement();
        Path folder = server.getWorldPath(LevelResource.ROOT).resolve("computercraft").resolve("computer")
                .resolve(Integer.toString(id));
        Path result = folder.resolve("result.txt");
        String program = "local ok, value = pcall(function()\n" + body + "\nend)\n"
                + "local f = fs.open(\"result.txt\", \"w\")\n"
                + "if ok then f.write(textutils.serializeJSON(value)) else f.write(\"ERROR \" .. tostring(value)) end\n"
                + "f.close()\n"
                + "os.shutdown()\n";
        try {
            Files.createDirectories(folder);
            Files.deleteIfExists(result);
            Files.writeString(folder.resolve("startup.lua"), program, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("cannot write the test program for computer " + id, e);
        }
        BlockPos at = helper.absolutePos(COMPUTER);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                "setblock " + at.getX() + " " + at.getY() + " " + at.getZ()
                        + " computercraft:computer_normal{ComputerId:" + id + ",On:1b}");
        BlockState placed = helper.getLevel().getBlockState(at);
        helper.assertTrue(placed.getBlock() != net.minecraft.world.level.block.Blocks.AIR,
                "the computer was not placed (is CC: Tweaked loaded?)");
        helper.succeedWhen(() -> {
            helper.assertTrue(Files.isRegularFile(result), "computer " + id + " has not written its result yet");
            String text;
            try {
                text = Files.readString(result, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            helper.assertTrue(!text.startsWith("ERROR "), "Lua: " + text);
            JsonElement json = JsonParser.parseString(text);
            helper.assertTrue(json.isJsonArray(), "result is not a list: " + text);
            check.accept(json.getAsJsonArray());
        });
    }
}
//?}
