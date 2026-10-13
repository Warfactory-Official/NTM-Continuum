package com.hbm.gametest;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.machine.MachineElectrolyser;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.container.ContainerMachineElectrolyser;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.items.machine.ItemFluidId;
import com.hbm.items.special.BedrockOreType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineElectrolyser;
import dev.golem.core.gametest.GameTestHelpers;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;

/** Focused contracts for the electrolyser's independent fluid and metal pipelines. */
public final class ElectrolyserGameTests {

    private ElectrolyserGameTests() {}

    public static void recipes(GameTestHelper helper) {
        ElectrolyserFluidRecipes.register();
        ElectrolyserMetalRecipes.register();
        helper.assertTrue(ElectrolyserFluidRecipes.RECIPES.size() == 8,
                "electrolyser must expose all eight legacy fluid recipes");
        // Eighteen crystal recipes plus 1.7's bedrock-ore products: primary-first, primary-second and
        // crumbs for each of the six ore types.
        int metalExpected = 18 + 3 * BedrockOreType.VALUES.length;
        helper.assertTrue(ElectrolyserMetalRecipes.RECIPES.size() == metalExpected,
                "electrolyser must expose all " + metalExpected + " ported metal recipes, found "
                        + ElectrolyserMetalRecipes.RECIPES.size());
        helper.assertTrue(ElectrolyserFluidRecipes.getRecipe(Fluids.HEAVYWATER).output1.type == Fluids.DEUTERIUM,
                "heavy water must produce deuterium");
        helper.assertTrue(ElectrolyserMetalRecipes.getRecipe(new ItemStack(ModItems.crystal_uranium.get())).output2.material
                        == Mats.MAT_RADIUM,
                "uranium crystal must produce radium as its second molten output");
        helper.succeed();
    }

    /**
     * The complete legacy footprint, metadata write order, and all six distance-six ports.
     *
     * <p>All four facings are built at the same core, one after another, not side by side: an electrolyser
     * reaches six blocks out in every horizontal direction, so four spread across a grid would leave the
     * 24-wide plot and make the result depend on the neighbouring plot. Reusing one core and clearing
     * between facings keeps every written block inside its own plot.
     */
    public static void layoutAndPorts(GameTestHelper helper) {
        MachineElectrolyser block = ModMachineBlocks.machine_electrolyser.get();
        ForgeDirection[] facings = {ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST};
        BlockPos relativeCore = new BlockPos(12, 1, 12);

        for (int i = 0; i < facings.length; i++) {
            ForgeDirection facing = facings[i];
            BlockPos core = helper.absolutePos(relativeCore);
            BlockPos base = core.offset(facing.offsetX * block.getOffset(), 0,
                    facing.offsetZ * block.getOffset());

            helper.assertTrue(block.checkRequirement(helper.getLevel(), core, facing.toDirection(), base),
                    "empty electrolyser placement failed for " + facing);
            BlockPos obstruction = core.above();
            helper.getLevel().setBlock(obstruction, Blocks.STONE.defaultBlockState(), 3);
            helper.assertFalse(block.checkRequirement(helper.getLevel(), core, facing.toDirection(), base),
                    "electrolyser extra volume was not checked for " + facing);
            helper.getLevel().removeBlock(obstruction, false);

            FoldedTestSupport.assemble(helper, block, relativeCore, facing.toDirection());
            TileEntityMachineElectrolyser electrolyser =
                    (TileEntityMachineElectrolyser) helper.getLevel().getBlockEntity(core);
            helper.assertTrue(electrolyser != null, "electrolyser core block entity was not created for " + facing);

            for (BlockPos cell : expectedLayout(core, facing)) {
                helper.assertTrue(helper.getLevel().getBlockState(cell).getBlock() == block,
                        "missing electrolyser footprint cell at " + cell + " for " + facing);
                helper.assertTrue(cell.equals(core)
                                || core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), cell)),
                        "electrolyser cell at " + cell + " is not owned by the core for " + facing);
            }
            assertPorts(helper, block, electrolyser, core, facing);
            clearStructure(helper, block, core);
        }
        helper.succeed();
    }

    /** Removes every block this machine placed, so the next facing starts from the empty plot again. */
    private static void clearStructure(GameTestHelper helper, MachineElectrolyser block, BlockPos core) {
        helper.getLevel().removeBlock(core, false);
        for (int x = -7; x <= 7; x++) {
            for (int y = -1; y <= 4; y++) {
                for (int z = -7; z <= 7; z++) {
                    BlockPos pos = core.offset(x, y, z);
                    if (helper.getLevel().getBlockState(pos).getBlock() == block) {
                        helper.getLevel().removeBlock(pos, false);
                    }
                }
            }
        }
    }

    public static void fluidProcessing(GameTestHelper helper) {
        TileEntityMachineElectrolyser electrolyser = place(helper);
        electrolyser.power = TileEntityMachineElectrolyser.MAX_POWER;
        electrolyser.tanks[0].setFill(2_000);
        electrolyser.progressFluid = 1;
        int duration = ElectrolyserFluidRecipes.getRecipe(Fluids.WATER).duration;
        for (int i = 0; i < duration - 1; i++) tick(helper, electrolyser);

        helper.assertTrue(electrolyser.tanks[0].getFill() == 0, "electrolyser did not consume 2,000 mB water");
        helper.assertTrue(electrolyser.tanks[1].getTankType() == Fluids.HYDROGEN
                        && electrolyser.tanks[1].getFill() == 200,
                "electrolyser did not produce 200 mB hydrogen");
        helper.assertTrue(electrolyser.tanks[2].getTankType() == Fluids.OXYGEN
                        && electrolyser.tanks[2].getFill() == 200,
                "electrolyser did not produce 200 mB oxygen");
        helper.assertTrue(electrolyser.power == TileEntityMachineElectrolyser.MAX_POWER
                        - (long) (duration - 1) * TileEntityMachineElectrolyser.BASE_USAGE_FLUID,
                "electrolyser fluid processing drew the wrong power: " + electrolyser.power);
        helper.succeed();
    }

    public static void metalProcessing(GameTestHelper helper) {
        TileEntityMachineElectrolyser electrolyser = place(helper);
        electrolyser.power = TileEntityMachineElectrolyser.MAX_POWER;
        electrolyser.tanks[3].setFill(100);
        electrolyser.inventory.setStackInSlot(14, new ItemStack(ModItems.crystal_iron.get()));
        electrolyser.progressMetal = 1;
        for (int i = 0; i < TileEntityMachineElectrolyser.BASE_METAL_DURATION - 1; i++) tick(helper, electrolyser);

        helper.assertTrue(electrolyser.inventory.getStackInSlot(14).isEmpty(), "electrolyser did not consume the crystal");
        helper.assertTrue(electrolyser.leftStack != null && electrolyser.leftStack.material == Mats.MAT_IRON
                        && electrolyser.leftStack.amount == MaterialShapes.INGOT.quantity * 6,
                "electrolyser produced the wrong primary molten output");
        helper.assertTrue(electrolyser.rightStack != null && electrolyser.rightStack.material == Mats.MAT_TITANIUM
                        && electrolyser.rightStack.amount == MaterialShapes.INGOT.quantity * 2,
                "electrolyser produced the wrong secondary molten output");
        helper.assertTrue(electrolyser.inventory.getStackInSlot(15).is(ModItems.powder_lithium_tiny.get())
                        && electrolyser.inventory.getStackInSlot(15).getCount() == 3,
                "electrolyser did not produce its lithium byproduct");
        helper.succeed();
    }

    public static void pausesAndResumes(GameTestHelper helper) {
        TileEntityMachineElectrolyser electrolyser = place(helper);
        electrolyser.tanks[0].setFill(2_000);
        electrolyser.tanks[3].setFill(100);
        electrolyser.inventory.setStackInSlot(14, new ItemStack(ModItems.crystal_iron.get()));
        electrolyser.progressFluid = 3;
        electrolyser.progressMetal = 41;

        electrolyser.power = 0;
        tick(helper, electrolyser);
        helper.assertTrue(electrolyser.progressFluid == 3 && electrolyser.progressMetal == 41,
                "electrolyser fluid and metal progress must pause instead of resetting");

        electrolyser.power = TileEntityMachineElectrolyser.MAX_POWER;
        tick(helper, electrolyser);
        helper.assertTrue(electrolyser.progressFluid == 4 && electrolyser.progressMetal == 42,
                "electrolyser fluid and metal progress must resume after a stall: fluid="
                        + electrolyser.progressFluid + ", metal=" + electrolyser.progressMetal
                        + ", durationFluid=" + electrolyser.fluidDuration + ", durationMetal=" + electrolyser.metalDuration);
        helper.succeed();
    }

    public static void moltenPourRate(GameTestHelper helper) {
        TileEntityMachineElectrolyser electrolyser = place(helper);
        helper.assertTrue(electrolyser.getPourQuanta() == MaterialShapes.NUGGET.quantity * 3,
                "electrolyser must pour three nuggets per cycle without speed upgrades");

        electrolyser.inventory.setStackInSlot(1, new ItemStack(ModUpgradeItems.upgrade_speed_2.get()));
        tick(helper, electrolyser);
        helper.assertTrue(electrolyser.getPourQuanta() == MaterialShapes.NUGGET.quantity * 3 * 2,
                "electrolyser molten pouring must include the speed multiplier");
        helper.succeed();
    }

    public static void menus(GameTestHelper helper) {
        TileEntityMachineElectrolyser electrolyser = place(helper);
        try (GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = fixture.player();
            BlockPos core = electrolyser.getBlockPos();
            player.setPos(core.getX() + 0.5D, core.getY() + 0.5D, core.getZ() + 0.5D);
            ContainerMachineElectrolyser fluid = new ContainerMachineElectrolyser(0, player.getInventory(), electrolyser, false);
            ContainerMachineElectrolyser metal = new ContainerMachineElectrolyser(1, player.getInventory(), electrolyser, true);
            player.containerMenu = fluid;
            helper.assertTrue(fluid.slots.size() == 14 + 36 && metal.slots.size() == 10 + 36,
                    "electrolyser mode menus have the wrong slot counts");
            helper.assertTrue(fluid.getSlot(3).mayPlace(ItemFluidId.make(Fluids.WATER)),
                    "fluid electrolyser menu must accept a fluid identifier");
            helper.assertTrue(metal.getSlot(3).getContainerSlot() == 14
                            && electrolyser.canPlaceItem(14, new ItemStack(ModItems.crystal_iron.get())),
                    "metal electrolyser menu must expose the crystal input");

            CompoundTag packet = new CompoundTag();
            packet.putBoolean("sgm", false);
            electrolyser.receiveControl(packet);
            helper.assertTrue(electrolyser.lastSelectedGUI == 1,
                    "electrolyser metal control must use tag presence, not the boolean value");
            packet = new CompoundTag();
            packet.putBoolean("sgf", false);
            electrolyser.receiveControl(packet);
            helper.assertTrue(electrolyser.lastSelectedGUI == 0,
                    "electrolyser fluid control must use tag presence, not the boolean value");

            player.setPos(core.getX() + 100.5D, core.getY() + 0.5D, core.getZ() + 0.5D);
            helper.assertFalse(electrolyser.hasPermission(player), "a distant player must not control the electrolyser");
            helper.assertFalse(fluid.clickMenuButton(player, ContainerMachineElectrolyser.MODE_METAL),
                    "a distant player must not switch the electrolyser mode");
            helper.assertTrue(fixture.outboundPackets().isEmpty(),
                    "a denied electrolyser mode switch must not send an opening packet");
            player.setPos(core.getX() + 0.5D, core.getY() + 0.5D, core.getZ() + 0.5D);

            helper.assertTrue(fluid.clickMenuButton(player, ContainerMachineElectrolyser.MODE_METAL),
                    "fluid electrolyser menu did not switch to metal");
            helper.assertTrue(electrolyser.lastSelectedGUI == 1
                            && player.containerMenu instanceof ContainerMachineElectrolyser switched
                            && switched != fluid && switched.isMetal() && switched.stillValid(player),
                    "fluid-to-metal switch was not server-authoritative or did not replace the open menu");
            ContainerMachineElectrolyser switchedMetal = (ContainerMachineElectrolyser) player.containerMenu;
            assertOpeningPacket(helper, fixture.outboundPackets(), switchedMetal, core,
                    ModMenus.MACHINE_ELECTROLYSER_METAL.get());

            helper.assertTrue(switchedMetal.clickMenuButton(player, ContainerMachineElectrolyser.MODE_FLUID),
                    "metal electrolyser menu did not switch to fluid");
            helper.assertTrue(electrolyser.lastSelectedGUI == 0
                            && player.containerMenu instanceof ContainerMachineElectrolyser switched
                            && switched != switchedMetal && !switched.isMetal() && switched.stillValid(player),
                    "metal-to-fluid switch was not server-authoritative or did not replace the open menu");
            ContainerMachineElectrolyser switchedFluid = (ContainerMachineElectrolyser) player.containerMenu;
            assertOpeningPacket(helper, fixture.outboundPackets(), switchedFluid, core,
                    ModMenus.MACHINE_ELECTROLYSER_FLUID.get());
            helper.succeed();
        }
    }

    private static void assertOpeningPacket(GameTestHelper helper, List<Packet<?>> packets,
                                            ContainerMachineElectrolyser menu, BlockPos core,
                                            MenuType<?> expectedType) {
        GameTestHelpers.MenuOpening opening = GameTestHelpers.menuOpening(helper, packets);
        helper.assertTrue(opening.containerId() == menu.containerId,
                "electrolyser opening packet container id was not synchronized with the server menu");
        helper.assertTrue(opening.menuType() == expectedType && opening.menuType() == menu.getType(),
                "electrolyser opening packet did not identify the replacement menu");
        helper.assertTrue(core.equals(opening.position()),
                "electrolyser opening packet carried the wrong machine position");
    }

    private static Set<BlockPos> expectedLayout(BlockPos core, ForgeDirection facing) {
        Set<BlockPos> expected = new HashSet<>();
        addExpectedVolume(expected, core, new int[]{0, 0, 5, 5, 1, 3}, facing);
        int[][] extraVolumes = {
                {2, -1, 5, 5, 1, 1}, {3, -3, 5, 5, 0, 0},
                {3, -1, 4, -4, -3, 3}, {3, -1, 2, -2, -3, 3},
                {3, -1, 0, 0, -3, 3}, {3, -1, -2, 2, -3, 3},
                {3, -1, -4, 4, -3, 3}
        };
        for (int[] volume : extraVolumes) addExpectedVolume(expected, core, volume, facing);
        for (int along : new int[]{4, 2, 0, -2, -4}) {
            addExpectedVolume(expected, core.offset(facing.offsetX * along, 3, facing.offsetZ * along),
                    new int[]{0, 0, 0, 0, -1, 2}, facing);
        }
        expected.add(core);
        return expected;
    }

    private static void addExpectedVolume(Set<BlockPos> expected, BlockPos anchor, int[] dimensions,
                                          ForgeDirection facing) {
        int[] rotated = MultiblockHandlerXR.rotate(dimensions, facing.toDirection());
        for (int x = anchor.getX() - rotated[4]; x <= anchor.getX() + rotated[5]; x++) {
            for (int y = anchor.getY() - rotated[1]; y <= anchor.getY() + rotated[0]; y++) {
                for (int z = anchor.getZ() - rotated[2]; z <= anchor.getZ() + rotated[3]; z++) {
                    BlockPos position = new BlockPos(x, y, z);
                    if (position.equals(anchor)) continue;
                    expected.add(position);
                }
            }
        }
    }

    private static void assertPorts(GameTestHelper helper, MachineElectrolyser block,
                                    TileEntityMachineElectrolyser electrolyser, BlockPos core,
                                    ForgeDirection facing) {
        ForgeDirection rot = facing.getRotation(ForgeDirection.UP);
        assertPort(helper, block, electrolyser, core.offset(-facing.offsetX * 6, 0, -facing.offsetZ * 6),
                facing.getOpposite(), 0);
        assertPort(helper, block, electrolyser, core.offset(-facing.offsetX * 6 + rot.offsetX, 0,
                -facing.offsetZ * 6 + rot.offsetZ), facing.getOpposite(), 1);
        assertPort(helper, block, electrolyser, core.offset(-facing.offsetX * 6 - rot.offsetX, 0,
                -facing.offsetZ * 6 - rot.offsetZ), facing.getOpposite(), 2);
        assertPort(helper, block, electrolyser, core.offset(facing.offsetX * 6, 0, facing.offsetZ * 6),
                facing, 3);
        assertPort(helper, block, electrolyser, core.offset(facing.offsetX * 6 + rot.offsetX, 0,
                facing.offsetZ * 6 + rot.offsetZ), facing, 4);
        assertPort(helper, block, electrolyser, core.offset(facing.offsetX * 6 - rot.offsetX, 0,
                facing.offsetZ * 6 - rot.offsetZ), facing, 5);
    }

    private static void assertPort(GameTestHelper helper, MachineElectrolyser block,
                                   TileEntityMachineElectrolyser electrolyser, BlockPos port,
                                   ForgeDirection direction, int index) {
        DirPos actual = electrolyser.getConPos()[index];
        BlockPos cell = port.offset(-direction.offsetX, -direction.offsetY, -direction.offsetZ);
        helper.assertTrue(actual.getPos().equals(port) && actual.getDir() == direction,
                "electrolyser port " + index + " is not at distance six");
        helper.assertTrue(block.canConnect(helper.getLevel(), cell, direction)
                        && block.canConnect(Fluids.WATER, helper.getLevel(), cell.getX(), cell.getY(), cell.getZ(), direction),
                "electrolyser port " + index + " does not connect for its facing");
        helper.assertFalse(block.canConnect(helper.getLevel(), cell, direction.getOpposite()),
                "electrolyser exposed a false connection beside port " + index);
    }

    public static void constructionRecipe(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        var recipe = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.electrolyzer");
        helper.assertTrue(recipe != null && recipe.duration == 200 && recipe.power == 100,
                "electrolyser assembly recipe is missing or has wrong processing parameters");
        helper.assertTrue(recipe.outputItem != null && recipe.outputItem[0].is(ModMachineBlocks.machine_electrolyser.get().asItem()),
                "electrolyser assembly recipe has the wrong output");
        helper.succeed();
    }

    private static TileEntityMachineElectrolyser place(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        MachineElectrolyser block = ModMachineBlocks.machine_electrolyser.get();
        helper.setBlock(new BlockPos(2, 1, 2), block.defaultBlockState());
        return (TileEntityMachineElectrolyser) helper.getLevel().getBlockEntity(pos);
    }

    private static void tick(GameTestHelper helper, TileEntityMachineElectrolyser electrolyser) {
        TileEntityMachineElectrolyser.serverTick(helper.getLevel(), electrolyser.getBlockPos(),
                helper.getLevel().getBlockState(electrolyser.getBlockPos()), electrolyser);
    }
}
