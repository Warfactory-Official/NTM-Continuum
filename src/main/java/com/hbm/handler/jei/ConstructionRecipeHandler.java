package com.hbm.handler.jei;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModCustomMissileBlocks;
import com.hbm.blocks.ModIcfBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.fusion.MachineFusionTorus;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModMeleeKitItems;
import com.hbm.items.ModToolItems;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ConstructionHandler}: what each struct core assembles into and what the build takes. 1.7 states the build
 * lists in the handler itself and counts the torus from {@code MachineFusionTorus.layout}; so does this.
 */
public class ConstructionRecipeHandler extends JEIUniversalHandler<ConstructionRecipeHandler.Row> {

    /** Input slots (alternatives per slot), the output, and the block shown in the pill. */
    public record Row(List<List<ItemStack>> inputs, ItemStack output, List<ItemStack> machine) {}

    public ConstructionRecipeHandler(ViewerType<Row> type, Component title, ItemStack... machines) {
        super(type, title, machines);
    }

    public static ItemStack[] stations() {
        return new ItemStack[]{new ItemStack(ModMeleeKitItems.acetylene_torch.get()),
                new ItemStack(ModToolItems.blowtorch.get()), new ItemStack(ModToolItems.boltgun.get())};
    }

    @Override
    protected List<List<ItemStack>> inputs(Row recipe) {
        return recipe.inputs();
    }

    @Override
    protected List<List<ItemStack>> outputs(Row recipe) {
        return List.of(List.of(recipe.output()));
    }

    @Override
    protected List<ItemStack> machine(Row recipe) {
        return recipe.machine();
    }

    public static List<Row> buildRecipes() {
        List<Row> rows = new ArrayList<>();
        ItemStack boltgun = new ItemStack(ModToolItems.boltgun.get());
        ItemStack blowtorch = new ItemStack(ModToolItems.blowtorch.get());

        rows.add(row(ModMachineBlocks.watz.get(), ModMachineBlocks.struct_watz_core.get(),
                stack(ModMachineBlocks.watz_casing.get(), 48),
                autogen(MaterialShapes.BOLT, Mats.MAT_DURA, 64), autogen(MaterialShapes.BOLT, Mats.MAT_DURA, 64),
                autogen(MaterialShapes.BOLT, Mats.MAT_DURA, 64),
                stack(ModBlocks.watz_element.get(), 36), stack(ModBlocks.watz_cooler.get(), 26), boltgun));

        rows.add(row(ModCustomMissileBlocks.compact_launcher.get(), ModCustomMissileBlocks.struct_launcher_core.get(),
                stack(ModBlocks.struct_launcher.get(), 8)));

        rows.add(row(ModCustomMissileBlocks.launch_table.get(), ModCustomMissileBlocks.struct_launcher_core_large.get(),
                stack(ModBlocks.struct_launcher.get(), 16), stack(ModBlocks.struct_launcher.get(), 64),
                stack(ModBlocks.struct_scaffold.get(), 11)));

        rows.add(row(ModMachineBlocks.soyuz_launcher.get(), ModMachineBlocks.struct_soyuz_core.get(),
                stack(ModBlocks.struct_launcher.get(), 30), stack(ModBlocks.struct_launcher.get(), 384),
                stack(ModBlocks.struct_scaffold.get(), 63), stack(ModBlocks.struct_scaffold.get(), 384),
                stack(ModBlocks.concrete_smooth.get(), 38), stack(ModBlocks.concrete_smooth.get(), 320)));

        List<List<ItemStack>> icf = new ArrayList<>();
        for (ItemStack stack : new ItemStack[]{stack(ModIcfBlocks.icf_component.get(), 50),
                stack(ModIcfBlocks.icf_component_structure.get(), 240), autogen(MaterialShapes.BOLT, Mats.MAT_DURA, 960),
                autogen(MaterialShapes.CASTPLATE, Mats.MAT_STEEL, 240), stack(ModIcfBlocks.icf_component_vessel.get(), 117)}) {
            icf.add(List.of(stack));
        }
        icf.add(List.of(autogen(MaterialShapes.CASTPLATE, Mats.MAT_BBRONZE, 117),
                autogen(MaterialShapes.CASTPLATE, Mats.MAT_ABRONZE, 117)));
        icf.add(List.of(blowtorch));
        icf.add(List.of(boltgun));
        rows.add(new Row(icf, new ItemStack(ModIcfBlocks.icf.get()), List.of(new ItemStack(ModIcfBlocks.struct_icf_core.get()))));

        int walls = 0;
        int blankets = 0;
        int pipes = -1; // one block is replaced by the core
        for (int iy = 0; iy < 5; iy++) {
            int[][] layer = MachineFusionTorus.layout[iy > 2 ? 4 - iy : iy];
            for (int[] line : layer) for (int meta : line) {
                if (meta == 1) walls++;
                if (meta == 2) blankets++;
                if (meta == 3) pipes++;
            }
        }
        List<ItemStack> torus = new ArrayList<>();
        split(torus, new ItemStack(ModMachineBlocks.fusion_component.get()), walls);
        split(torus, autogen(MaterialShapes.CASTPLATE, Mats.MAT_STEEL, 1), walls);
        split(torus, new ItemStack(ModMachineBlocks.fusion_component_blanket.get()), blankets);
        split(torus, new ItemStack(ModMachineBlocks.fusion_component_motor.get()), pipes);
        torus.add(blowtorch);
        rows.add(row(ModMachineBlocks.fusion_torus.get(), ModMachineBlocks.struct_torus_core.get(), torus.toArray(ItemStack[]::new)));
        return rows;
    }

    private static Row row(Block output, Block core, ItemStack... inputs) {
        List<List<ItemStack>> slots = new ArrayList<>();
        for (ItemStack stack : inputs) slots.add(List.of(stack));
        return new Row(slots, new ItemStack(output), List.of(new ItemStack(core)));
    }

    /** 1.7 adds the torus parts in stacks of at most 256. */
    private static void split(List<ItemStack> list, ItemStack stack, int count) {
        while (count > 0) {
            int a = Math.min(count, 256);
            list.add(stack.copyWithCount(a));
            count -= a;
        }
    }

    private static ItemStack stack(ItemLike item, int count) {
        return new ItemStack(item, count);
    }

    private static ItemStack autogen(MaterialShapes shape, NTMMaterial material, int count) {
        return new ItemStack(ModAutogenItems.get(shape, material), count);
    }
}
