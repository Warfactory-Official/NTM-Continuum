package com.hbm.gametest;

import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.MachineGasCent;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.MachineUF6Tank;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.inventory.container.ContainerMachineGasCent;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.ModBatteryItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.ItemBatteryPack.EnumBatteryPack;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineGasCent;
import com.hbm.tileentity.machine.TileEntityMachineUF6Tank;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.lib.DirPos;
import com.hbm.inventory.gui.GasCentTooltips;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Focused contracts for the gas centrifuge's pseudo-fluid cascade. */
public final class GasCentrifugeGameTests {

    private GasCentrifugeGameTests() {}

    public static void recipes(GameTestHelper helper) {
        GasCentrifugeRecipes.register();
        helper.assertTrue(GasCentrifugeRecipes.RECIPES.size() == 4,
                "gas centrifuge must expose all four legacy JEI recipes");
        helper.assertTrue(GasCentrifugeRecipes.RECIPES.get(0).highSpeed
                        && GasCentrifugeRecipes.RECIPES.get(0).input.type == Fluids.UF6,
                "gas centrifuge high-speed UF6 recipe is missing");
        helper.assertTrue(GasCentrifugeRecipes.FLUID_CONVERSIONS.get(Fluids.UF6)
                        == GasCentrifugeRecipes.PseudoFluidType.NUF6,
                "UF6 must convert to natural pseudo-fluid");
        helper.assertTrue(GasCentrifugeRecipes.PseudoFluidType.NUF6.getOutputType()
                        == GasCentrifugeRecipes.PseudoFluidType.LEUF6,
                "natural UF6 must enrich into low-enriched UF6");
        ItemStack fluidIcon = ItemFluidIcon.make(new FluidStack(Fluids.UF6, 1200, 42));
        helper.assertTrue(ItemFluidIcon.getFluid(fluidIcon) == Fluids.UF6
                        && ModDataComponents.fluidAmount(fluidIcon) == 1200
                        && ModDataComponents.fluidPressure(fluidIcon) == 42,
                "gas centrifuge JEI fluid icon must retain type, amount, and pressure");
        helper.succeed();
    }

    public static void layoutAndPorts(GameTestHelper helper) {
        MachineGasCent block = ModMachineBlocks.machine_gascent.get();
        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos core = helper.absolutePos(relative);
        helper.setBlock(relative, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), core, Direction.NORTH);

        helper.assertTrue(helper.getLevel().getBlockEntity(core) instanceof TileEntityMachineGasCent,
                "gas centrifuge core block entity was not created");
        helper.assertTrue(core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), core.above())),
                "gas centrifuge tower dummy did not resolve to the core");
        helper.assertTrue(block.canConnect(helper.getLevel(), core, com.hbm.lib.ForgeDirection.EAST),
                "gas centrifuge east power port did not connect");
        helper.assertTrue(block.canConnect(Fluids.UF6, helper.getLevel(), core.getX(), core.getY(), core.getZ(),
                        com.hbm.lib.ForgeDirection.EAST),
                "gas centrifuge east fluid port did not connect");
        helper.succeed();
    }

    public static void processing(GameTestHelper helper) {
        TileEntityMachineGasCent gasCent = place(helper, new BlockPos(2, 1, 2));
        gasCent.power = TileEntityMachineGasCent.MAX_POWER;
        gasCent.inputTank.setFill(GasCentrifugeRecipes.PseudoFluidType.NUF6.getFluidConsumed());
        gasCent.progress = 1;

        for (int i = 0; i < TileEntityMachineGasCent.PROCESSING_SPEED - 1; i++) {
            TileEntityMachineGasCent.serverTick(helper.getLevel(), gasCent.getBlockPos(),
                    helper.getLevel().getBlockState(gasCent.getBlockPos()), gasCent);
        }

        helper.assertTrue(gasCent.inputTank.getFill() == 0,
                "gas centrifuge did not consume natural UF6 pseudo-fluid");
        helper.assertTrue(gasCent.outputTank.getTankType() == GasCentrifugeRecipes.PseudoFluidType.LEUF6
                        && gasCent.outputTank.getFill() == 300,
                "gas centrifuge did not produce 300 mB of low-enriched UF6");
        helper.assertTrue(gasCent.inventory.getStackInSlot(0).is(ModItems.nugget_u238.get()),
                "gas centrifuge did not produce its uranium byproduct");
        helper.assertTrue(gasCent.power == TileEntityMachineGasCent.MAX_POWER
                        - (long) (TileEntityMachineGasCent.PROCESSING_SPEED - 1)
                        * TileEntityMachineGasCent.BASE_CONSUMPTION,
                "gas centrifuge did not draw exactly 200 HE/t");
        helper.succeed();
    }

    public static void twoBatchesResetProgress(GameTestHelper helper) {
        TileEntityMachineGasCent gasCent = place(helper, new BlockPos(2, 1, 2));
        gasCent.power = TileEntityMachineGasCent.MAX_POWER;
        int consumed = GasCentrifugeRecipes.PseudoFluidType.NUF6.getFluidConsumed();
        gasCent.inputTank.setFill(consumed * 2);

        for (int i = 0; i < TileEntityMachineGasCent.PROCESSING_SPEED; i++) tick(helper, gasCent);
        helper.assertTrue(gasCent.progress == 0 && gasCent.outputTank.getFill() == 300,
                "gas centrifuge must reset progress after the first enrichment batch");

        for (int i = 0; i < TileEntityMachineGasCent.PROCESSING_SPEED - 1; i++) tick(helper, gasCent);
        helper.assertTrue(gasCent.outputTank.getFill() == 300,
                "gas centrifuge must not process the second batch one tick early");
        tick(helper, gasCent);
        helper.assertTrue(gasCent.progress == 0 && gasCent.outputTank.getFill() == 600
                        && gasCent.inputTank.getFill() == 0,
                "gas centrifuge must process two batches at the normal cadence");
        helper.succeed();
    }

    public static void uf6FluidNetwork(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos gasRelative = new BlockPos(3, 1, 3);
        BlockPos gasPos = helper.absolutePos(gasRelative);
        MachineGasCent gasBlock = ModMachineBlocks.machine_gascent.get();
        helper.setBlock(gasRelative, gasBlock.defaultBlockState());
        gasBlock.fillSpace(level, gasPos, Direction.NORTH);
        TileEntityMachineGasCent gasCent = (TileEntityMachineGasCent) level.getBlockEntity(gasPos);

        BlockPos ductPos = gasPos.west();
        BlockFluidDuct ductBlock = ModMachineBlocks.fluid_duct_mk2.get();
        level.setBlock(ductPos, ductBlock.defaultBlockState(), 3);
        TileEntityFluidDuctNT duct = (TileEntityFluidDuctNT) level.getBlockEntity(ductPos);
        duct.setFluidType(Fluids.UF6);

        BlockPos sourcePos = gasPos.west(2);
        MachineUF6Tank sourceBlock = ModMachineBlocks.machine_uf6_tank.get();
        level.setBlock(sourcePos, sourceBlock.defaultBlockState()
                .setValue(BlockDummyable.META, BlockDummyable.offset + 2), 3);
        TileEntityMachineUF6Tank source = (TileEntityMachineUF6Tank) level.getBlockEntity(sourcePos);
        source.tank.setFill(1_000);

        DirPos gasPort = gasCent.getConPos()[2];
        helper.assertTrue(gasPort.getPos().equals(ductPos) && gasPort.getDir() == com.hbm.lib.ForgeDirection.WEST,
                "UF6 network test must use the gas centrifuge's west fluid port");
        helper.assertTrue(source.getConPos()[0].getPos().equals(ductPos),
                "UF6 tank must face the same duct as the gas centrifuge");

        awaitUf6Network(helper, level, ductPos, gasCent, source);
    }

    public static void overclock(GameTestHelper helper) {
        TileEntityMachineGasCent gasCent = place(helper, new BlockPos(2, 1, 2));
        gasCent.power = TileEntityMachineGasCent.MAX_POWER;
        gasCent.inputTank.setTankType(GasCentrifugeRecipes.PseudoFluidType.HEUF6);
        gasCent.inputTank.setFill(GasCentrifugeRecipes.PseudoFluidType.HEUF6.getFluidConsumed());
        gasCent.progress = 1;
        TileEntityMachineGasCent.serverTick(helper.getLevel(), gasCent.getBlockPos(),
                helper.getLevel().getBlockState(gasCent.getBlockPos()), gasCent);
        helper.assertTrue(!gasCent.isProgressing && gasCent.progress == 0,
                "HEUF6 must not process without the overclock upgrade");

        gasCent.inventory.setStackInSlot(6, new ItemStack(ModUpgradeItems.upgrade_gc_speed.get()));
        gasCent.power = TileEntityMachineGasCent.MAX_POWER;
        gasCent.progress = 1;
        for (int i = 0; i < TileEntityMachineGasCent.PROCESSING_SPEED - 71; i++) {
            TileEntityMachineGasCent.serverTick(helper.getLevel(), gasCent.getBlockPos(),
                    helper.getLevel().getBlockState(gasCent.getBlockPos()), gasCent);
        }
        helper.assertTrue(gasCent.inventory.getStackInSlot(1).is(ModItems.nugget_u235.get()),
                "overclock upgrade must enable terminal HEUF6 separation");
        helper.succeed();
    }

    public static void menu(GameTestHelper helper) {
        TileEntityMachineGasCent gasCent = place(helper, new BlockPos(2, 1, 2));
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper,
                net.minecraft.world.level.GameType.SURVIVAL);
        ContainerMachineGasCent menu = new ContainerMachineGasCent(0, player.getInventory(), gasCent);

        helper.assertTrue(menu.slots.size() == ContainerMachineGasCent.MACHINE_SLOTS + 36,
                "gas centrifuge menu must contain seven machine slots and the player inventory");
        helper.assertTrue(menu.getSlot(4).mayPlace(new ItemStack(
                        ModBatteryItems.PACKS.get(EnumBatteryPack.BATTERY_LEAD).get())),
                "gas centrifuge battery slot must accept a battery");
        helper.assertTrue(menu.getSlot(5).mayPlace(com.hbm.items.machine.ItemFluidId.make(Fluids.UF6)),
                "gas centrifuge identifier slot must accept a fluid identifier");
        for (int slot = 0; slot < 4; slot++) {
            helper.assertFalse(menu.getSlot(slot).mayPlace(new ItemStack(Items.IRON_INGOT)),
                    "gas centrifuge output slot " + slot + " must be take-only");
        }
        helper.succeed();
    }

    public static void tooltips(GameTestHelper helper) {
        assertTooltip(helper, GasCentTooltips.enrichment(), ChatFormatting.DARK_GREEN, 4);
        assertTooltip(helper, GasCentTooltips.output(), ChatFormatting.GOLD, 2);
        helper.succeed();
    }

    public static void constructionRecipe(GameTestHelper helper) {
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.gascent");
        helper.assertTrue(recipe != null && recipe.duration == 400 && recipe.power == 100,
                "gas centrifuge assembly recipe is missing or has wrong processing parameters");
        helper.assertTrue(recipe.outputItem != null && recipe.outputItem.length == 1
                        && recipe.outputItem[0].is(ModMachineBlocks.machine_gascent.get().asItem()),
                "gas centrifuge assembly recipe has the wrong output");
        helper.succeed();
    }

    private static TileEntityMachineGasCent place(GameTestHelper helper, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        MachineGasCent block = ModMachineBlocks.machine_gascent.get();
        helper.setBlock(relative, block.defaultBlockState());
        return (TileEntityMachineGasCent) helper.getLevel().getBlockEntity(pos);
    }

    private static void assertTooltip(GameTestHelper helper, java.util.List<Component> lines,
                                      ChatFormatting headingStyle, int bodyLines) {
        helper.assertTrue(lines.size() == bodyLines + 1, "gas centrifuge tooltip has the wrong line count");
        helper.assertTrue(lines.get(0).getStyle().getColor() != null
                        && lines.get(0).getStyle().getColor().getValue() == headingStyle.getColor(),
                "gas centrifuge tooltip heading has the wrong style");
        for (int i = 0; i < lines.size(); i++) {
            helper.assertFalse(lines.get(i).getString().indexOf('\u00a7') >= 0,
                    "gas centrifuge tooltip line contains raw formatting: " + i);
        }
        for (int i = 1; i < lines.size(); i++) {
            helper.assertTrue(lines.get(i).getStyle().getColor() == null,
                    "gas centrifuge tooltip body line must be unformatted: " + i);
        }
    }

    private static void tick(GameTestHelper helper, TileEntityMachineGasCent gasCent) {
        TileEntityMachineGasCent.serverTick(helper.getLevel(), gasCent.getBlockPos(),
                helper.getLevel().getBlockState(gasCent.getBlockPos()), gasCent);
    }

    private static void awaitUf6Network(GameTestHelper helper, net.minecraft.server.level.ServerLevel level,
                                        BlockPos ductPos, TileEntityMachineGasCent gasCent,
                                        TileEntityMachineUF6Tank source) {
        boolean transferred = source.tank.getFill() < 1_000
                && gasCent.tank.getFill() + gasCent.inputTank.getFill() > 0;

        if (!transferred) {
            helper.runAfterDelay(1, () -> awaitUf6Network(helper, level, ductPos, gasCent, source));
            return;
        }

        helper.assertTrue(source.tank.getFill() + gasCent.tank.getFill() + gasCent.inputTank.getFill() == 1_000,
                "UF6 must transfer through the real duct into the centrifuge without loss: source="
                        + source.tank.getFill() + ", gas=" + gasCent.tank.getFill()
                        + ", input=" + gasCent.inputTank.getFill());
        helper.succeed();
    }
}
