package com.hbm.recipes.tables;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blocks.ModIcfBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.handler.NTMToolHandler;
import com.hbm.handler.NTMToolHandler.Row;
import com.hbm.inventory.RecipesCommon.AStack;
import dev.golem.core.world.RecipeSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.List;

import static com.hbm.inventory.OreDictManager.*;

/**
 * 1.7 {@code BlockToolConversion.registerRecipes}. 1.7's meta blocks are the port's split blocks.
 */
public final class ToolConversionTable extends RecipeSource<Row> {

    public ToolConversionTable() {
        super(NTMToolHandler.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        put(ToolType.BOLT, ModMachineBlocks.watz_casing.get(), ModMachineBlocks.watz_casing_tooled.get(), DURA.bolt().count(4));
        put(ToolType.TORCH, ModMachineBlocks.fusion_component.get(), ModMachineBlocks.fusion_component_bscco_welded.get(), STEEL.plateCast());
        put(ToolType.TORCH, ModIcfBlocks.icf_component_vessel.get(), ModIcfBlocks.icf_component_vessel_welded.get(), ANY_BISMOIDBRONZE.plateCast());
        put(ToolType.BOLT, ModIcfBlocks.icf_component_structure.get(), ModIcfBlocks.icf_component_structure_bolted.get(), STEEL.plateCast(), DURA.bolt().count(4));
    }

    /** 1.7 {@code conversions.put(new Pair(tool, from), new Pair(materials, to))}; the id is the source block's path. */
    private void put(ToolType tool, Block from, Block to, AStack... materials) {
        register(BuiltInRegistries.BLOCK.getKey(from).getPath(), new Row(tool, from, to, List.of(materials)));
    }
}
