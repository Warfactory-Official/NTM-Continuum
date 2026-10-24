package com.hbm.gametest;

import com.hbm.blocks.ModIcfBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.dfc.ModDFCBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.tileentity.machine.TileEntityICF;
import com.hbm.tileentity.machine.TileEntitySILEX;
import com.hbm.tileentity.machine.dfc.TileEntityCore;
import com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler;
import com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT;
import com.hbm.tileentity.machine.fusion.TileEntityFusionPlasmaForge;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.info.BlockInfo;
import dev.golem.core.info.Gauge;
import dev.golem.core.info.Tank;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The block-info lines for plasma (TU), the ICF, the DFC (Spk) and the SILEX pool: each machine's core is placed,
 * given a value, and its declared gauges and tanks must carry it in 1.7's unit. Values the machine zeroes every
 * tick are set before one server tick, so the overlay must read the last-tick copy.
 */
public final class BlockInfoGameTests {

    private static final BlockPos POS = new BlockPos(1, 1, 1);

    private BlockInfoGameTests() {}

    public static void machines(GameTestHelper helper) {
        TileEntityFusionTorus torus = place(helper, ModMachineBlocks.fusion_torus.get(), TileEntityFusionTorus.class);
        torus.plasmaEnergy = 1_250_000L;
        expect(helper, torus, "", "TU", 1_250_000L, 0L);
        expect(helper, torus, "", "KyU", 0L, 0L);

        TileEntityFusionPlasmaForge forge = place(helper, ModMachineBlocks.fusion_plasma_forge.get(),
                TileEntityFusionPlasmaForge.class);
        forge.plasmaEnergySync = 400_000L;
        expect(helper, forge, "", "TU", 400_000L, 0L);

        TileEntityFusionBoiler boiler = place(helper, ModMachineBlocks.fusion_boiler.get(), TileEntityFusionBoiler.class);
        boiler.plasmaEnergy = 300_000L;
        TileEntityFusionBoiler.serverTick(helper.getLevel(), boiler.getBlockPos(), boiler.getBlockState(), boiler);
        expect(helper, boiler, "", "TU", 300_000L, 0L);

        TileEntityFusionMHDT turbine = place(helper, ModMachineBlocks.fusion_mhdt.get(), TileEntityFusionMHDT.class);
        turbine.plasmaEnergy = 6_000_000L;
        TileEntityFusionMHDT.serverTick(helper.getLevel(), turbine.getBlockPos(), turbine.getBlockState(), turbine);
        expect(helper, turbine, "", "TU/t", 6_000_000L, TileEntityFusionMHDT.minimumPlasma);

        TileEntityICF icf = place(helper, ModIcfBlocks.icf.get(), TileEntityICF.class);
        helper.assertTrue(gauge(icf, "", "TU/t") == null, "an ICF without lasers showed a laser line");
        icf.laser = 2_000_000L;
        icf.maxLaser = 8_000_000L;
        icf.tick(helper.getLevel(), icf.getBlockPos());
        expect(helper, icf, "", "TU/t", 2_000_000L, 8_000_000L);
        expect(helper, icf, "", "TU", icf.heat, TileEntityICF.maxHeat);
        helper.assertTrue(icf.heat > 0, "the laser did not heat the ICF");

        TileEntityCoreEmitter emitter = place(helper, ModDFCBlocks.dfc_emitter.get(), TileEntityCoreEmitter.class);
        emitter.prev = 5_000L;
        expect(helper, emitter, "Output", "Spk", 5_000L, 0L);

        TileEntityCoreReceiver receiver = place(helper, ModDFCBlocks.dfc_receiver.get(), TileEntityCoreReceiver.class);
        receiver.prevJoules = 4_750L;
        expect(helper, receiver, "Input", "Spk", 4_750L, 0L);

        TileEntityCore core = place(helper, ModDFCBlocks.dfc_core.get(), TileEntityCore.class);
        core.field = 60;
        core.prevHeat = 25;
        expect(helper, core, "Restriction Field", "%", 60L, 100L);
        expect(helper, core, "Heat Saturation", "%", 25L, 100L);
        core.field = 0;
        core.prevHeat = 0;

        TileEntitySILEX silex = place(helper, ModMachineBlocks.machine_silex.get(), TileEntitySILEX.class);
        silex.current = ItemFluidIcon.make(Fluids.UF6);
        silex.currentFill = 3_000;
        List<Tank> tanks = BlockInfo.tanksOf(silex);
        helper.assertTrue(tanks.stream().anyMatch(tank -> tank.fluid() == Fluids.UF6.getFluid()
                && tank.millibuckets() == 3_000 && tank.capacity() == TileEntitySILEX.maxFill), "SILEX pool " + tanks);
        silex.current = new ItemStack(Items.REDSTONE);
        expect(helper, silex, Items.REDSTONE.getDescriptionId(), "mB", 3_000L, TileEntitySILEX.maxFill);
        helper.succeed();
    }

    private static <T extends BlockEntity> T place(GameTestHelper helper, Block block, Class<T> type) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockMultiblockCore.FACING)) state = state.setValue(BlockMultiblockCore.FACING, Direction.NORTH);
        helper.setBlock(POS, state);
        T be = GameTestHelpers.blockEntity(helper, POS, type);
        helper.assertTrue(be != null, "no " + type.getSimpleName());
        return be;
    }

    private static Gauge gauge(Object be, String label, String unit) {
        for (Gauge gauge : BlockInfo.gaugesOf(be)) {
            if (gauge.label().equals(label) && gauge.unit().equals(unit)) return gauge;
        }
        return null;
    }

    private static void expect(GameTestHelper helper, Object be, String label, String unit, long value, long max) {
        Gauge gauge = gauge(be, label, unit);
        String name = be.getClass().getSimpleName() + " " + label + " " + unit;
        helper.assertTrue(gauge != null, name + " missing from " + BlockInfo.gaugesOf(be));
        helper.assertTrue(gauge.value() == value && gauge.max() == max,
                name + ": " + gauge.value() + " / " + gauge.max() + ", expected " + value + " / " + max);
    }
}
