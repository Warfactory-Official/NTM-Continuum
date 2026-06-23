package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import static com.hbm.inventory.OreDictManager.*;

public class AssemblyMachineRecipes extends GenericRecipes {

    public static final AssemblyMachineRecipes INSTANCE = new AssemblyMachineRecipes();

    public static final String POOL_PLATES = POOL_PREFIX_ALT + "plates";
    private static final String GROUP_PLATES = "plates";
    private static final String GROUP_CYCLOTRON = "cyclotron";

    @Override
    public void registerRecipes() {
        if (!recipeOrderedList.isEmpty()) return;

        plate("ass.plateiron", IRON, ModItems.plate_iron.get());
        plate("ass.plategold", GOLD, ModItems.plate_gold.get());
        plate("ass.platetitanium", TI, ModItems.plate_titanium.get());
        plate("ass.platealu", AL, ModItems.plate_aluminium.get());
        plate("ass.platesteel", STEEL, ModItems.plate_steel.get());
        plate("ass.platelead", PB, ModItems.plate_lead.get());
        plate("ass.platecopper", CU, ModItems.plate_copper.get());
        plate("ass.plategunmetal", GUNMETAL, ModItems.plate_gunmetal.get());
        plate("ass.plateweaponsteel", WEAPONSTEEL, ModItems.plate_weaponsteel.get());
        plate("ass.platecmb", CMB, ModItems.plate_combine_steel.get());
        plate("ass.platedura", DURA, ModItems.plate_dura_steel.get());
        plate("ass.platesaturnite", BIGMT, ModItems.plate_saturnite.get());
        plate("ass.plateschrab", SA326, ModItems.plate_schrabidium.get());

        register(new GenericRecipe("ass.firecloth").setup(50, 100)
                .inputItems(new ComparableStack(ModItems.ingot_asbestos.get(), 1), new ComparableStack(Items.STRING, 8))
                .outputItems(new ItemStack(ModItems.asbestos_cloth.get(), 4)));

        register(new GenericRecipe("ass.centrifugetower").setup(100, 100)
                .inputItems(new ComparableStack(ModItems.plate_dura_steel.get(), 4),
                        new ComparableStack(ModItems.plate_titanium.get(), 4),
                        new ComparableStack(ModItems.motor.get(), 1))
                .outputItems(new ItemStack(ModItems.centrifuge_element.get(), 1)));

        register(new GenericRecipe("ass.partberyl").setup(40, 100)
                .inputItems(new ComparableStack(ModItems.powder_beryllium.get(), 1))
                .outputItems(new ItemStack(ModItems.part_beryllium.get(), 8))
                .setGroup(GROUP_CYCLOTRON, this));
        register(new GenericRecipe("ass.partcop").setup(40, 100)
                .inputItems(new ComparableStack(ModItems.powder_copper.get(), 1))
                .outputItems(new ItemStack(ModItems.part_copper.get(), 8))
                .setGroup(GROUP_CYCLOTRON, this));

        for (NTMMaterial mat : new NTMMaterial[]{STEEL, CU, TI}) {
            register(new GenericRecipe("ass.cast." + mat.name).setup(80, 100)
                    .inputItems(mat.plate().count(3))
                    .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.CASTPLATE, mat), 1)));
            register(new GenericRecipe("ass.weld." + mat.name).setup(120, 100)
                    .inputItems(new MaterialStack(mat, MaterialShapes.CASTPLATE, 2))
                    .outputItems(new ItemStack(ModAutogenItems.get(MaterialShapes.WELDEDPLATE, mat), 1)));
        }
    }

    private void plate(String name, NTMMaterial mat, Item plate) {
        register(new GenericRecipe(name)
                .setup(60, 100)
                .inputItems(mat.ingot())
                .outputItems(new ItemStack(plate, 1))
                .setPools(POOL_PLATES)
                .setGroup(GROUP_PLATES, this));
    }
}
