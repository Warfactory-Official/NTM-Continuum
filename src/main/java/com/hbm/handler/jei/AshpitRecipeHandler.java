package com.hbm.handler.jei;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ItemEnums.EnumCokeType;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModOilItems;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** 1.7 {@code AshpitHandler}: which heater or chimney and which fuel or smoke leave which ash. */
public class AshpitRecipeHandler extends ConstructionRecipeHandler {

    public AshpitRecipeHandler(ViewerType<Row> type, Component title, ItemStack... machines) {
        super(type, title, machines);
    }

    public static List<Row> buildRecipes() {
        List<ItemStack> machine = List.of(new ItemStack(ModMachineBlocks.machine_ashpit.get()));
        List<ItemStack> ovens = List.of(new ItemStack(ModMachineBlocks.heater_firebox.get()),
                new ItemStack(ModMachineBlocks.heater_oven.get()));
        List<ItemStack> chimneys = List.of(new ItemStack(ModMachineBlocks.chimney_brick.get()),
                new ItemStack(ModMachineBlocks.chimney_industrial.get()));
        List<ItemStack> industrial = List.of(new ItemStack(ModMachineBlocks.chimney_industrial.get()));
        List<ItemStack> coals = List.of(new ItemStack(Items.COAL), new ItemStack(ModFuelItems.lignite.get()),
                ModOilItems.coke(EnumCokeType.COAL, 1));
        List<ItemStack> wood = List.of(new ItemStack(Items.OAK_LOG), new ItemStack(Items.ACACIA_LOG),
                new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_SAPLING), new ItemStack(Items.CHARCOAL));
        List<ItemStack> misc = List.of(new ItemStack(ModFuelItems.solid_fuel.get()), new ItemStack(ModItems.scrap.get()),
                new ItemStack(ModItems.dust.get()), new ItemStack(ModFuelItems.rocket_fuel.get()));

        List<Row> rows = new ArrayList<>();
        rows.add(new Row(List.of(ovens, coals), ash(EnumAshType.COAL), machine));
        rows.add(new Row(List.of(ovens, wood), ash(EnumAshType.WOOD), machine));
        rows.add(new Row(List.of(ovens, misc), ash(EnumAshType.MISC), machine));
        for (FluidType smoke : new FluidType[] {Fluids.SMOKE, Fluids.SMOKE_LEADED, Fluids.SMOKE_POISON}) {
            rows.add(new Row(List.of(chimneys, List.of(ItemFluidIcon.make(new FluidStack(smoke, 2_000)))), ash(EnumAshType.FLY), machine));
            rows.add(new Row(List.of(industrial, List.of(ItemFluidIcon.make(new FluidStack(smoke, 8_000)))), ash(EnumAshType.SOOT), machine));
        }
        return rows;
    }

    private static ItemStack ash(EnumAshType type) {
        return new ItemStack(ModIngredientItems.ash(type));
    }
}
