package com.hbm.compat;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.IFluidUserMK2;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.tileentity.machine.TileEntityCrucible;
import com.hbm.tileentity.machine.TileEntityFireboxBase;
import com.hbm.tileentity.machine.TileEntityFoundryBase;
import com.hbm.tileentity.machine.TileEntityFurnaceCombination;
import com.hbm.tileentity.machine.TileEntityFurnaceSteel;
import com.hbm.tileentity.machine.TileEntityHeatBoiler;
import com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial;
import com.hbm.tileentity.machine.TileEntityHeaterElectric;
import com.hbm.tileentity.machine.TileEntityHeaterHeatex;
import com.hbm.tileentity.machine.TileEntityHeaterOilburner;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron;
import com.hbm.tileentity.machine.fusion.TileEntityFusionTorus;
import dev.golem.core.info.BlockInfo;
import dev.golem.core.info.Gauge;
import dev.golem.core.info.Tank;
import java.util.List;
import java.util.function.Consumer;

/**
 * HBM's buffers in a block-info overlay (Jade): HE, tanks, heat (TU), klystron energy (KyU) and molten materials.
 * Folded multiblock cells show their core. HE ramps blue to green so it never reads as FE.
 */
public final class HbmBlockInfo {

    private static final int HE_FROM = 0xFF1E50FF;
    private static final int HE_TO = 0xFF30E060;
    private static final int TU_FROM = 0xFF901C00;
    private static final int TU_TO = 0xFFFFA030;
    private static final int KYU_FROM = 0xFF5028D0;
    private static final int KYU_TO = 0xFFE850FF;

    private HbmBlockInfo() {}

    public static void init() {
        BlockInfo.redirect((level, pos) ->
                MultiblockSurface.isFoldedCell(level.getBlockState(pos)) ? MultiblockSurface.coreOfCell(level, pos) : null);

        BlockInfo.gauges(IEnergyHandlerMK2.class, (machine, out) -> {
            if (machine.getMaxPower() > 0) {
                out.accept(Gauge.energy(machine.getPower(), machine.getMaxPower(), "HE", HE_FROM, HE_TO));
            }
        });
        BlockInfo.tanks(IFluidUserMK2.class, (machine, out) -> {
            for (FluidTankNTM tank : machine.getAllTanks()) {
                if (tank == null || tank.getMaxFill() <= 0) continue;
                FluidType type = tank.getTankType();
                out.accept(new Tank(type == Fluids.NONE ? null : type.getFluid(), tank.getFill(), tank.getMaxFill()));
            }
        });

        BlockInfo.gauges(TileEntityFireboxBase.class, (te, out) -> heat(out, te.heatEnergy, te.getMaxHeat()));
        BlockInfo.gauges(TileEntityHeaterOilburner.class,
                (te, out) -> heat(out, te.heatEnergy, TileEntityHeaterOilburner.maxHeatEnergy));
        BlockInfo.gauges(TileEntityHeaterElectric.class, (te, out) -> heat(out, te.heatEnergy, 0));
        BlockInfo.gauges(TileEntityHeaterHeatex.class, (te, out) -> heat(out, te.heatEnergy, 0));
        BlockInfo.gauges(TileEntityHeatBoiler.class, (te, out) -> heat(out, te.heat, TileEntityHeatBoiler.maxHeat));
        BlockInfo.gauges(TileEntityHeatBoilerIndustrial.class,
                (te, out) -> heat(out, te.heat, TileEntityHeatBoilerIndustrial.maxHeat));
        BlockInfo.gauges(TileEntityFurnaceSteel.class, (te, out) -> heat(out, te.heat, TileEntityFurnaceSteel.maxHeat));
        BlockInfo.gauges(TileEntityFurnaceCombination.class,
                (te, out) -> heat(out, te.heat, TileEntityFurnaceCombination.maxHeat));

        BlockInfo.gauges(TileEntityFusionKlystron.class,
                (te, out) -> out.accept(Gauge.of("", te.output, te.outputTarget, "KyU", KYU_FROM, KYU_TO)));
        BlockInfo.gauges(TileEntityFusionTorus.class, (te, out) -> {
            long ignition = te.fusionModule.getRecipe() instanceof FusionRecipe recipe ? recipe.ignitionTemp : 0;
            out.accept(Gauge.of("", te.klystronEnergy, ignition, "KyU", KYU_FROM, KYU_TO));
        });

        BlockInfo.gauges(TileEntityCrucible.class, (te, out) -> {
            heat(out, te.heat, TileEntityCrucible.maxHeat);
            materials(out, te.recipeStack, TileEntityCrucible.recipeZCapacity);
            materials(out, te.wasteStack, TileEntityCrucible.wasteZCapacity);
        });
        BlockInfo.gauges(TileEntityFoundryBase.class, (te, out) -> {
            if (te.type != null && te.amount > 0) out.accept(material(te.type.getDisplayName(), te.amount,
                    te.getCapacity(), te.type.moltenColor));
        });
    }

    private static void heat(Consumer<Gauge> out, int heat, int max) {
        out.accept(Gauge.of("", heat, max, "TU", TU_FROM, TU_TO));
    }

    private static void materials(Consumer<Gauge> out, List<Mats.MaterialStack> stacks, int capacity) {
        for (Mats.MaterialStack stack : stacks) {
            out.accept(material(stack.material.getDisplayName(), stack.amount, capacity, stack.material.moltenColor));
        }
    }

    /** Amounts in mB, quanta × 2 as 1.7's {@code Mats.formatAmount} prints them. */
    private static Gauge material(String name, int quanta, int capacity, int color) {
        int opaque = 0xFF000000 | color;
        return Gauge.of(name, quanta * 2L, capacity * 2L, "mB", opaque & 0xFF7F7F7F, opaque);
    }
}
