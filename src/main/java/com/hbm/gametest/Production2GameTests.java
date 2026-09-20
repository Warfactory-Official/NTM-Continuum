package com.hbm.gametest;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.items.ModRtgContent;
import com.hbm.items.ModToolItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.blocks.machine.BlockRtgFurnaceBase;
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.tileentity.machine.TileEntityDiFurnaceRTG;
import com.hbm.tileentity.machine.TileEntityMachineExcavator;
import com.hbm.tileentity.machine.TileEntityMachineOreSlopper;
import com.hbm.tileentity.machine.TileEntityRtgFurnace;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Processing-loop contracts for the group-B production machines (excavator, ore slopper, RTG furnaces). */
public final class Production2GameTests {

    private Production2GameTests() {}

    /**
     * One slopper cycle at 600 ticks: consumes one bedrock ore chunk and 1,000 mB of water, produces
     * 1,000 mB of slop, and credits every density channel with the chunk's ore amount.
     */
    public static void oreSlopperProcessing(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.setBlock(new BlockPos(2, 1, 2), ModProduction2Blocks.MACHINE_ORE_SLOPPER.get().defaultBlockState());
        TileEntityMachineOreSlopper slopper = (TileEntityMachineOreSlopper) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(slopper != null, "ore slopper core block entity was not created");

        ItemStack ore = new ItemStack(ModOreBlocks.bedrock_ore_base.get());
        ItemBedrockOreBase.setOreAmount(ore, 1000, 1000, 1D);
        double expected = ItemBedrockOreBase.getOreAmount(ore, BedrockOreType.LIGHT_METAL);
        slopper.inventory.setStackInSlot(2, ore);
        slopper.tanks[0].setFill(1_000);
        slopper.power = TileEntityMachineOreSlopper.maxPower;

        // 1.7 accumulates {@code progress += 1F / 600} in a float; 600 additions sum to 0.99999911, so the
        // cycle completes on the 601st tick exactly as the original does. Its 100,000 HE buffer at 200 HE/t
        // also runs dry after 500 ticks and resets progress, so the network's job of refilling it every
        // tick is played here by topping the buffer up before each tick; the draw is checked per tick.
        int ticks = 0;
        while (ticks < 700 && !slopper.inventory.getStackInSlot(2).isEmpty()) {
            slopper.power = TileEntityMachineOreSlopper.maxPower;
            TileEntityMachineOreSlopper.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), slopper);
            ticks++;
        }
        helper.assertTrue(slopper.inventory.getStackInSlot(2).isEmpty(), "ore slopper did not consume the bedrock ore chunk");
        helper.assertTrue(ticks >= 600 && ticks <= 601, "ore slopper cycle took " + ticks + " ticks, expected 600-601");
        helper.assertTrue(slopper.tanks[0].getFill() == 0, "ore slopper did not consume 1,000 mB water: " + slopper.tanks[0].getFill());
        helper.assertTrue(slopper.tanks[1].getTankType() == Fluids.SLOP && slopper.tanks[1].getFill() == 1_000,
                "ore slopper did not produce 1,000 mB slop");
        // The ore amount is paid out as whole items, so the credited total is what remains plus what was paid.
        int paid = 0;
        for (int i = 3; i <= 8; i++) {
            ItemStack out = slopper.inventory.getStackInSlot(i);
            if (out.is(ModProduction2Blocks.bedrockOreBase(BedrockOreType.LIGHT_METAL))) paid += out.getCount();
        }
        double credited = slopper.ores[BedrockOreType.LIGHT_METAL.ordinal()] + paid;
        helper.assertTrue(Math.abs(credited - expected) < 1e-6,
                "ore slopper credited " + credited + " light metal, expected " + expected);
        helper.assertTrue(slopper.power == TileEntityMachineOreSlopper.maxPower - TileEntityMachineOreSlopper.consumptionBase,
                "ore slopper drew the wrong power: " + slopper.power);
        helper.succeed();
    }

    /** A 600-heat lead pellet cooks one iron ore in two ticks (1,000 / 600) and lights the block while cooking. */
    public static void rtgFurnaceProcessing(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.setBlock(new BlockPos(2, 1, 2), ModProduction2Blocks.MACHINE_RTG_FURNACE.get().defaultBlockState());
        TileEntityRtgFurnace furnace = (TileEntityRtgFurnace) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(furnace != null, "rtg furnace block entity was not created");

        furnace.inventory.setStackInSlot(0, new ItemStack(Items.RAW_IRON, 2));
        furnace.inventory.setStackInSlot(1, new ItemStack(ModRtgContent.PELLET_RTG_LEAD.get()));

        TileEntityRtgFurnace.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), furnace);
        helper.assertTrue(furnace.dualCookTime == 600, "rtg furnace cook time after one tick: " + furnace.dualCookTime);
        helper.assertTrue(helper.getLevel().getBlockState(pos).getValue(BlockRtgFurnaceBase.LIT), "rtg furnace must be lit while cooking");
        TileEntityRtgFurnace.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), furnace);
        helper.assertTrue(furnace.inventory.getStackInSlot(4).is(Items.IRON_INGOT) && furnace.inventory.getStackInSlot(4).getCount() == 1,
                "rtg furnace did not smelt one iron ingot");
        helper.assertTrue(furnace.inventory.getStackInSlot(0).getCount() == 1, "rtg furnace did not consume one raw iron");
        helper.assertTrue(furnace.dualCookTime == 0, "rtg furnace cook time must reset after a smelt");
        helper.succeed();
    }

    /** Six 50-heat pellets give 300 heat/tick: iron + coal becomes steel after 4 ticks (1,200 / 300); under 15 heat nothing runs. */
    public static void diFurnaceRtgProcessing(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.setBlock(new BlockPos(2, 1, 2), ModProduction2Blocks.MACHINE_DIFURNACE_RTG.get().defaultBlockState());
        TileEntityDiFurnaceRTG furnace = (TileEntityDiFurnaceRTG) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(furnace != null, "nuclear blast furnace block entity was not created");

        furnace.inventory.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 4));
        furnace.inventory.setStackInSlot(1, new ItemStack(Items.COAL, 4));
        furnace.inventory.setStackInSlot(3, new ItemStack(ModRtgContent.PELLET_RTG.get())); // 10 heat: below the 15 threshold
        TileEntityDiFurnaceRTG.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), furnace);
        helper.assertTrue(furnace.progress == 0 && furnace.inventory.getStackInSlot(2).isEmpty(),
                "nuclear blast furnace must not run under 15 heat");

        for (int slot = 3; slot <= 8; slot++) furnace.inventory.setStackInSlot(slot, new ItemStack(ModRtgContent.PELLET_RTG_POLONIUM.get()));
        for (int i = 0; i < 4; i++) {
            TileEntityDiFurnaceRTG.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), furnace);
        }
        helper.assertTrue(furnace.inventory.getStackInSlot(2).is(ModItems.ingot_steel.get()),
                "nuclear blast furnace did not produce steel: " + furnace.inventory.getStackInSlot(2));
        helper.assertTrue(furnace.inventory.getStackInSlot(0).getCount() == 3 && furnace.inventory.getStackInSlot(1).getCount() == 3,
                "nuclear blast furnace did not consume one iron and one coal");
        helper.succeed();
    }

    /**
     * The excavator's drill loop: with a steel bit and power, one stone block under the shaft is broken
     * after ceil(1.5 / 1.0) = 2 working ticks, and the toggle packets flip their flags by tag presence.
     */
    public static void excavatorDrilling(GameTestHelper helper) {
        BlockPos rel = new BlockPos(6, 6, 6);
        BlockPos pos = helper.absolutePos(rel);
        helper.setBlock(rel, ModProduction2Blocks.MACHINE_EXCAVATOR.get().defaultBlockState());
        TileEntityMachineExcavator excavator = (TileEntityMachineExcavator) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(excavator != null, "excavator core block entity was not created");

        // Only the centre column of the first ring holds a block; the rest of the 3x3 is air.
        BlockPos target = pos.below(4);
        helper.getLevel().setBlock(target, Blocks.STONE.defaultBlockState(), 3);

        excavator.inventory.setStackInSlot(4, new ItemStack(ModToolItems.drillbit_steel.get()));
        excavator.power = TileEntityMachineExcavator.maxPower;

        CompoundTag toggle = new CompoundTag();
        toggle.putBoolean("drill", false);
        excavator.receiveControl(toggle);
        helper.assertTrue(excavator.enableDrill, "excavator drill toggle must use tag presence, not the boolean value");

        TileEntityMachineExcavator.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), excavator);
        helper.assertTrue(excavator.operational, "excavator must be operational with a bit and power");
        helper.assertTrue(helper.getLevel().getBlockState(target).is(Blocks.STONE), "excavator broke stone after a single working tick");
        TileEntityMachineExcavator.serverTick(helper.getLevel(), pos, helper.getLevel().getBlockState(pos), excavator);
        helper.assertTrue(helper.getLevel().getBlockState(target).isAir(), "excavator did not break the stone under the shaft");
        helper.assertTrue(excavator.power == TileEntityMachineExcavator.maxPower - 2 * excavator.baseConsumption,
                "excavator drew the wrong power: " + excavator.power);
        helper.succeed();
    }
}
