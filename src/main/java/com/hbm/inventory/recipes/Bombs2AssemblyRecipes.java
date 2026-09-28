package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static com.hbm.inventory.OreDictManager.AL;
import static com.hbm.inventory.OreDictManager.BE;
import static com.hbm.inventory.OreDictManager.GOLD;
import static com.hbm.inventory.OreDictManager.MINGRADE;
import static com.hbm.inventory.OreDictManager.NP237;
import static com.hbm.inventory.OreDictManager.P_RED;
import static com.hbm.inventory.OreDictManager.SA326;
import static com.hbm.inventory.OreDictManager.SA327;
import static com.hbm.inventory.OreDictManager.STEEL;
import static com.hbm.inventory.OreDictManager.TI;
import static com.hbm.inventory.OreDictManager.U235;
import static com.hbm.inventory.OreDictManager.W;
import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.items.ModBombs2Items;

/**
 * 1.7 {@code AssemblyMachineRecipes} entries for the special bombs and their parts. The 528 pools
 * ({@code setPools528}) apply only in 528 mode, as upstream. Euphemium has no material tag here, so its
 * nugget/ingot is matched by item.
 */
public final class Bombs2AssemblyRecipes {

    private Bombs2AssemblyRecipes() {}

    private static AStack tag(String path, int count) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.of("c", path)), count);
    }

    public static void registerAll(GenericRecipes recipes) {
        recipes.registerGenerated(new GenericRecipe("ass.ninadidnothingwrong").setup(300, 100)
                .inputItems(new ComparableStack(ModItems.dysfunctional_reactor.get()), STEEL.shell().count(2),
                        new ComparableStack(ModItems.ingot_euphemium.get(), 3),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 8))
                .outputItems(new ItemStack(ModBombs2Blocks.nuke_prototype.get()))
                .setPools528("528.controller"));
        recipes.registerGenerated(new GenericRecipe("ass.fleija").setup(300, 100)
                .inputItems(AL.shell(), new ComparableStack(ModItems.fins_quad_titanium.get()),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER)),
                        new ComparableStack(Items.WHITE_DYE, 4))
                .outputItems(new ItemStack(ModBombs2Blocks.nuke_fleija.get()))
                .setPools528("528.controller"));
        recipes.registerGenerated(new GenericRecipe("ass.solinium").setup(300, 100)
                .inputItems(STEEL.shell().count(2), new ComparableStack(ModItems.fins_quad_titanium.get()),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER)),
                        new ComparableStack(Items.GRAY_DYE, 8))
                .outputItems(new ItemStack(ModBombs2Blocks.nuke_solinium.get()))
                .setPools528("528.controller"));
        recipes.registerGenerated(new GenericRecipe("ass.balefirebomb").setup(400, 100)
                .inputItems(new ComparableStack(ModItems.sphere_steel.get()), TI.shell().count(6),
                        new ComparableStack(ModItems.fins_big_steel.get()), new ComparableStack(ModItems.powder_magic.get(), 8),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 4),
                        new ComparableStack(Items.GRAY_DYE, 8))
                .outputItems(new ItemStack(ModBombs2Blocks.nuke_fstbmb.get()))
                .setPools528("528.controller"));
        recipes.registerGenerated(new GenericRecipe("ass.customnuke").setup(300, 100)
                .inputItems(STEEL.shell().count(2), new ComparableStack(ModItems.fins_small_steel.get()),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.CONTROLLER_ADVANCED), 8),
                        new ComparableStack(Items.GRAY_DYE, 4))
                .outputItems(new ItemStack(ModBombs2Blocks.nuke_custom.get()))
                .setPools528("528.controller"));
        recipes.registerGenerated(new GenericRecipe("ass.levibomb").setup(200, 100)
                .inputItems(TI.plate().count(12), SA326.nugget().count(3),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED), 4),
                        GOLD.wireDense().count(8))
                .outputItems(new ItemStack(ModBombs2Blocks.float_bomb.get())));
        recipes.registerGenerated(new GenericRecipe("ass.endobomb").setup(200, 100)
                .inputItems(TI.plate().count(12), new ComparableStack(ModItems.powder_ice.get(), 32),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED)),
                        new ComparableStack(ModItems.coil_gold.get(), 4))
                .outputItems(new ItemStack(ModBombs2Blocks.therm_endo.get())));
        recipes.registerGenerated(new GenericRecipe("ass.exobomb").setup(200, 100)
                .inputItems(TI.plate().count(12), P_RED.dust().count(32),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED)),
                        new ComparableStack(ModItems.coil_gold.get(), 4))
                .outputItems(new ItemStack(ModBombs2Blocks.therm_exo.get())));

        recipes.registerGenerated(new GenericRecipe("ass.fleijaigniter").setup(200, 100)
                .inputItems(TI.plate().count(6), SA326.wire().count(2),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED)))
                .outputItems(new ItemStack(ModBombs2Items.fleija_igniter.get())));
        recipes.registerGenerated(new GenericRecipe("ass.fleijacore").setup(600, 100)
                .inputItems(U235.nugget().count(8), NP237.nugget().count(2), BE.nugget().count(4),
                        new ComparableStack(ModItems.coil_copper.get(), 2))
                .outputItems(new ItemStack(ModBombs2Items.fleija_core.get())));
        recipes.registerGenerated(new GenericRecipe("ass.fleijacharge").setup(300, 100)
                .inputItems(tag("ingots/any_highexplosive", 3), SA326.plate().count(8))
                .outputItems(new ItemStack(ModBombs2Items.fleija_propellant.get())));
        recipes.registerGenerated(new GenericRecipe("ass.soliniumigniter").setup(200, 100)
                .inputItems(TI.plate().count(4), MINGRADE.wire().count(8),
                        new ComparableStack(ModIntermediateItems.circuit(EnumCircuitType.ADVANCED)),
                        new ComparableStack(ModItems.coil_gold.get()))
                .outputItems(new ItemStack(ModBombs2Items.solinium_igniter.get())));
        recipes.registerGenerated(new GenericRecipe("ass.soliniumcore").setup(600, 100)
                .inputItems(SA327.nugget().count(9), new ComparableStack(ModItems.nugget_euphemium.get()))
                .outputItems(new ItemStack(ModBombs2Items.solinium_core.get())));
        recipes.registerGenerated(new GenericRecipe("ass.soliniumcharge").setup(300, 100)
                .inputItems(tag("ingots/any_highexplosive", 3), new ComparableStack(ModItems.neutron_reflector.get(), 2),
                        new ComparableStack(ModItems.plate_polymer.get(), 6), W.wire().count(6),
                        new ComparableStack(ModFuelItems.biomass_compressed.get(), 4))
                .outputItems(new ItemStack(ModBombs2Items.solinium_propellant.get())));
    }
}
