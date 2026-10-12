package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.PU;
import static com.hbm.inventory.OreDictManager.U;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.inventory.recipes.SILEXRecipes.FluidIconStack;
import com.hbm.inventory.recipes.SILEXRecipes.Row;
import com.hbm.inventory.recipes.SILEXRecipes.VariantStack;
import com.hbm.inventory.recipes.SILEXRecipes.WeightedOut;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModStorageDrumItems;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.items.special.ItemWasteStorage.LongWasteClass;
import com.hbm.items.special.ItemWasteStorage.ShortWasteClass;
import dev.golem.core.world.RecipeSource;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1.7 {@code SILEXRecipes.register}. 1.7's {@code itemTranslation} and {@code dictTranslation} aliases are rows of their
 * own with the target's values, written right after the target. RBMK pellets are their {@code hbm:rbmk_pellet} stage
 * (1.7 metadata), fluids their {@code fluid_icon}, waste classes the family member of that class. 1.7's
 * {@code ModItems.nugget_mercury} is the item registered as {@code nugget_mercury_tiny}, {@code ModItems.fallout}
 * {@code falloutitem}. A row's id is its input: the item path ({@code minecraft_<path>} for vanilla),
 * {@code <pellet>_<stage>}, {@code fluid_<fluid>}, or for a 1.7 ore name ({@code U.ingot()}) its HBM item's path
 * ({@code ingot_uranium}). {@code tinyWasteTranslation} stays code in {@code SILEXRecipes}.
 */
public final class SILEXTable extends RecipeSource<Row> {

    /** 1.7 {@code itemTranslation} and {@code dictTranslation}, in declaration order: {from, to}. */
    private final List<AStack[]> translations = new ArrayList<>();

    public SILEXTable() {
        super(SILEXRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        itemTranslation(fluidIcon(Fluids.UF6), new ComparableStack(ModItems.ingot_uranium.get()));
        dictTranslation(U.dust(), U.ingot());
        put(U.ingot(), new SILEXRecipe(900, 100, EnumWavelengths.VISIBLE)
                .addOut(new ItemStack(ModItems.nugget_u235.get()), 1)
                .addOut(new ItemStack(ModItems.nugget_u238.get()), 11)
                );

        put(new ComparableStack(ModItems.ingot_pu_mix.get()), new SILEXRecipe(900, 100, 2)
                .addOut(new ItemStack(ModItems.nugget_pu239.get()), 6)
                .addOut(new ItemStack(ModItems.nugget_pu240.get()), 3)
                );

        put(new ComparableStack(ModItems.ingot_am_mix.get()), new SILEXRecipe(900, 100, 2)
                .addOut(new ItemStack(ModItems.nugget_am241.get()), 3)
                .addOut(new ItemStack(ModItems.nugget_am242.get()), 6)
                );

        itemTranslation(fluidIcon(Fluids.PUF6), new ComparableStack(ModItems.ingot_plutonium.get()));
        dictTranslation(PU.dust(), PU.ingot());
        put(PU.ingot(), new SILEXRecipe(900, 100, 2)
                .addOut(new ItemStack(ModItems.nugget_pu238.get()), 3)
                .addOut(new ItemStack(ModItems.nugget_pu239.get()), 4)
                .addOut(new ItemStack(ModItems.nugget_pu240.get()), 2)
                );

        put(new ComparableStack(ModFuelItems.ingot_schraranium.get()), new SILEXRecipe(900, 100, 2)
                .addOut(new ItemStack(ModItems.nugget_schrabidium.get()), 4)
                .addOut(new ItemStack(ModItems.nugget_uranium.get()), 3)
                .addOut(new ItemStack(ModItems.nugget_neptunium.get()), 2)
                );

        itemTranslation(new ComparableStack(ModItems.powder_australium.get()), new ComparableStack(ModItems.ingot_australium.get()));
        put(new ComparableStack(ModItems.ingot_australium.get()), new SILEXRecipe(900, 100, 2)
                .addOut(new ItemStack(ModItems.nugget_australium_lesser.get()), 5)
                .addOut(new ItemStack(ModItems.nugget_australium_greater.get()), 1)
                );

        put(new ComparableStack(ModItems.crystal_schraranium.get()), new SILEXRecipe(900, 100, 3)
                .addOut(new ItemStack(ModItems.nugget_schrabidium.get()), 5)
                .addOut(new ItemStack(ModItems.nugget_uranium.get()), 2)
                .addOut(new ItemStack(ModItems.nugget_neptunium.get()), 2)
                );

        put(new ComparableStack(ModBlocks.ore_tikite.get().asItem()), new SILEXRecipe(900, 100, EnumWavelengths.UV)
                .addOut(new ItemStack(ModItems.powder_plutonium.get()), 2)
                .addOut(new ItemStack(ModItems.powder_cobalt.get()), 3)
                .addOut(new ItemStack(ModItems.powder_niobium.get()), 3)
                .addOut(new ItemStack(ModItems.powder_nitan_mix.get()), 2)
                );

        put(new ComparableStack(ModItems.crystal_trixite.get()), new SILEXRecipe(1200, 100, EnumWavelengths.UV)
                .addOut(new ItemStack(ModItems.powder_plutonium.get()), 2)
                .addOut(new ItemStack(ModItems.powder_cobalt.get()), 3)
                .addOut(new ItemStack(ModItems.powder_niobium.get()), 3)
                .addOut(new ItemStack(ModItems.powder_nitan_mix.get()), 1)
                .addOut(new ItemStack(ModItems.powder_spark_mix.get()), 1)
                );

        itemTranslation(new ComparableStack(ModItems.powder_lapis.get()), new ComparableStack(Items.LAPIS_LAZULI));
        put(new ComparableStack(Items.LAPIS_LAZULI), new SILEXRecipe(100, 100 ,1)
                .addOut(new ItemStack(ModItems.sulfur.get()), 4)
                .addOut(new ItemStack(ModItems.powder_aluminium.get()), 3)
                .addOut(new ItemStack(ModItems.powder_cobalt.get()), 3)
                );

        put(fluidIcon(Fluids.DEATH), new SILEXRecipe(1000, 1000, 4)
                .addOut(new ItemStack(ModItems.powder_impure_osmiridium.get()), 1)
                );

        put(fluidIcon(Fluids.VITRIOL), new SILEXRecipe(1000, 300, EnumWavelengths.IR)
                .addOut(new ItemStack(ModItems.powder_bromine.get()), 5)
                .addOut(new ItemStack(ModItems.powder_iodine.get()), 5)
                .addOut(new ItemStack(ModItems.powder_iron.get()), 5)
                .addOut(new ItemStack(ModItems.sulfur.get()), 15)
                );

        put(fluidIcon(Fluids.REDMUD), new SILEXRecipe(300, 50, EnumWavelengths.VISIBLE)
            .addOut(new ItemStack(ModItems.powder_aluminium.get()), 10)
            .addOut(new ItemStack(ModItems.powder_neodymium_tiny.get(), 3), 5)
            .addOut(new ItemStack(ModItems.powder_boron_tiny.get(), 3), 5)
            .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 5)
            .addOut(new ItemStack(ModItems.powder_iron.get()), 20)
            .addOut(new ItemStack(ModItems.powder_titanium.get()), 15)
            .addOut(new ItemStack(ModItems.powder_sodium.get()), 10)
        );

        for(int i = 0; i < 5; i++) {

            // UEU //
            put(pellet("ueu", i), new SILEXRecipe(600, 100, 1)     //NU and MEU will breed more plutonium due to their higher concentrations of U-238
                    .addOut(new ItemStack(ModItems.nugget_uranium.get()), 86 - i * 11)    //NU is unenriched to the point where it'll always be lower burnup; so more Pu239 for longer
                    .addOut(i < 2 ? new ItemStack(ModItems.nugget_pu239.get()) : new ItemStack(ModItems.nugget_pu_mix.get()), 10 + i * 3)
                    .addOut(longTiny(LongWasteClass.URANIUM235), 2 + 3 * i)
                    .addOut(shortTiny(ShortWasteClass.URANIUM235), 2 + 5 * i) );

            put(pellet("ueu", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_uranium.get()), 86 - i * 11)
                    .addOut(i < 2 ? new ItemStack(ModItems.nugget_pu239.get()) : new ItemStack(ModItems.nugget_pu_mix.get()), 10 + i * 3)
                    .addOut(longTiny(LongWasteClass.URANIUM235), 2 + 3 * i)
                    .addOut(shortTiny(ShortWasteClass.URANIUM235), 1 + 5 * i) );

            // MEU //
            put(pellet("meu", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_uranium_fuel.get()), 84 - i * 16)
                    .addOut(i < 1 ? new ItemStack(ModItems.nugget_pu239.get()) : new ItemStack(ModItems.nugget_pu_mix.get()), 6 + i * 4)
                    .addOut(longTiny(LongWasteClass.URANIUM235), 4 + 5 * i)
                    .addOut(shortTiny(ShortWasteClass.URANIUM235), 6 + 7 * i) );

            put(pellet("meu", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_uranium_fuel.get()), 83 - i * 16)
                    .addOut(i < 1 ? new ItemStack(ModItems.nugget_pu239.get()) : new ItemStack(ModItems.nugget_pu_mix.get()), 6 + i * 4)
                    .addOut(longTiny(LongWasteClass.URANIUM235), 4 + 5 * i)
                    .addOut(shortTiny(ShortWasteClass.URANIUM235), 6 + 7 * i) );

            // HEU233 //
            put(pellet("heu233", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_u233.get()), 90 - i * 20)
                    .addOut(longTiny(LongWasteClass.URANIUM233), 4 + 8 * i)
                    .addOut(shortTiny(ShortWasteClass.URANIUM233), 6 + 12 * i) );

            put(pellet("heu233", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_u233.get()), 89 - i * 20)
                    .addOut(longTiny(LongWasteClass.URANIUM233), 4 + 8 * i)
                    .addOut(shortTiny(ShortWasteClass.URANIUM233), 6 + 12 * i) );

            // HEU235 //
            put(pellet("heu235", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_u235.get()), 90 - i * 20)
                    .addOut(longTiny(LongWasteClass.URANIUM235), 4 + 8 * i)
                    .addOut(shortTiny(ShortWasteClass.URANIUM235), 6 + 12 * i) );

            put(pellet("heu235", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_u235.get()), 89 - i * 20)
                    .addOut(longTiny(LongWasteClass.URANIUM235), 4 + 8 * i)
                    .addOut(shortTiny(ShortWasteClass.URANIUM235), 6 + 12 * i) );

            // UZH //
            put(pellet("uzh", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 75)
                    .addOut(new ItemStack(ModItems.nugget_uranium_fuel.get()), 20 - i * 4)
                    .addOut(new ItemStack(ModItems.nugget_pu_mix.get()), 3 + i * 3)
                    .addOut(longTiny(LongWasteClass.URANIUM235), 1 + i * 1)
                    .addOut(shortTiny(ShortWasteClass.URANIUM235), 1 + i * 1) );

            put(pellet("uzh", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 75)
                    .addOut(new ItemStack(ModItems.nugget_uranium_fuel.get()), 19 - i * 4)
                    .addOut(new ItemStack(ModItems.nugget_pu_mix.get()), 3 + i * 3)
                    .addOut(longTiny(LongWasteClass.URANIUM235), 1 + i * 1)
                    .addOut(shortTiny(ShortWasteClass.URANIUM235), 1 + i * 1) );

            // TH232 //
            put(pellet("thmeu", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_thorium_fuel.get()), 84 - i * 20)
                    .addOut(new ItemStack(ModItems.nugget_u233.get()), 6 + i * 4)
                    .addOut(longTiny(LongWasteClass.THORIUM), 10 + 16 * i) );

            put(pellet("thmeu", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_thorium_fuel.get()), 83 - i * 20)
                    .addOut(new ItemStack(ModItems.nugget_u233.get()), 6 + i * 4)
                    .addOut(longTiny(LongWasteClass.THORIUM), 10 + 16 * i) );

            // LEP //
            put(pellet("lep", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_plutonium_fuel.get()), 84 - i * 14)
                    .addOut(i < 1 ? new ItemStack(ModItems.nugget_pu239.get()) : new ItemStack(ModItems.nugget_pu_mix.get()), 6 + i * 2)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM239), 7 + 8 * i)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM240), 3 + 4 * i) );

            put(pellet("lep", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_plutonium_fuel.get()), 83 - i * 14)
                    .addOut(i < 1 ? new ItemStack(ModItems.nugget_pu239.get()) : new ItemStack(ModItems.nugget_pu_mix.get()), 6 + i * 2)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM239), 7 + 8 * i)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM240), 3 + 4 * i) );

            // MEP //
            put(pellet("mep", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_pu_mix.get()), 85 - i * 20)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM239), 10 + 10 * i)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM240), 5 + 5 * i) );

            put(pellet("mep", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_pu_mix.get()), 84 - i * 20)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM239), 10 + 10 * i)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM240), 5 + 5 * i) );

            // HEP239 // (registered as hbm:rbmk_pellet_hep)
            put(pellet("hep", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_pu239.get()), 85 - i * 20)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM239), 15 + 20 * i) );

            put(pellet("hep", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_pu239.get()), 84 - i * 20)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM239), 15 + 20 * i) );

            // HEP241 //
            put(pellet("hep241", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.nugget_pu241.get()), 85 - i * 20)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM241), 15 + 20 * i) );

            put(pellet("hep241", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_pu241.get()), 84 - i * 20)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM241), 15 + 20 * i) );

            // MEN //
            put(pellet("men", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_neptunium_fuel.get()), 84 - i * 14)
                    .addOut(i < 1 ? new ItemStack(ModItems.nugget_pu239.get()) : new ItemStack(ModItems.nugget_pu_mix.get()), 6 + i * 2)
                    .addOut(longTiny(LongWasteClass.NEPTUNIUM), 4 + 5 * i)
                    .addOut(shortTiny(ShortWasteClass.NEPTUNIUM), 6 + 7 * i) );

            put(pellet("men", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_neptunium_fuel.get()), 83 - i * 14)
                    .addOut(i < 1 ? new ItemStack(ModItems.nugget_pu239.get()) : new ItemStack(ModItems.nugget_pu_mix.get()), 6 + i * 2)
                    .addOut(longTiny(LongWasteClass.NEPTUNIUM), 4 + 5 * i)
                    .addOut(shortTiny(ShortWasteClass.NEPTUNIUM), 6 + 7 * i) );

            // HEN //
            put(pellet("hen", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_neptunium.get()), 90 - i * 20)
                    .addOut(longTiny(LongWasteClass.NEPTUNIUM), 4 + 8 * i)
                    .addOut(shortTiny(ShortWasteClass.NEPTUNIUM), 6 + 12 * i) );

            put(pellet("hen", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_neptunium.get()), 89 - i * 20)
                    .addOut(longTiny(LongWasteClass.NEPTUNIUM), 4 + 8 * i)
                    .addOut(shortTiny(ShortWasteClass.NEPTUNIUM), 6 + 12 * i) );

            // MOX //
            put(pellet("mox", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_mox_fuel.get()), 84 - i * 20)
                    .addOut(new ItemStack(ModItems.nugget_pu_mix.get()), 6 + i * 4)
                    .addOut(longTiny(LongWasteClass.URANIUM235), 2 + 3 * i)
                    .addOut(shortTiny(ShortWasteClass.URANIUM235), 3 + 5 * i)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM239), 5 + 8 * i) );

            put(pellet("mox", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_mox_fuel.get()), 83 - i * 20)
                    .addOut(new ItemStack(ModItems.nugget_pu_mix.get()), 6 + i * 4)
                    .addOut(longTiny(LongWasteClass.URANIUM235), 2 + 3 * i)
                    .addOut(shortTiny(ShortWasteClass.URANIUM235), 3 + 5 * i)
                    .addOut(shortTiny(ShortWasteClass.PLUTONIUM239), 5 + 8 * i) );

            // LEAUS //
            put(pellet("leaus", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.nugget_australium_lesser.get()), 90 - i * 20)
                    .addOut(new ItemStack(ModItems.nugget_lead.get()), 6 + 12 * i)
                    .addOut(new ItemStack(ModItems.nugget_pb209.get()), 4 + 8 * i) );

            put(pellet("leaus", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_australium_lesser.get()), 89 - i * 20)
                    .addOut(new ItemStack(ModItems.nugget_lead.get()), 6 + 12 * i)
                    .addOut(new ItemStack(ModItems.nugget_pb209.get()), 4 + 8 * i) );

            // HEAUS //
            put(pellet("heaus", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.nugget_australium_greater.get()), 90 - i * 20)
                    .addOut(new ItemStack(ModItems.nugget_au198.get()), 5 + 10 * i)
                    .addOut(new ItemStack(Items.GOLD_NUGGET), 3 + 6 * i)
                    .addOut(new ItemStack(ModItems.nugget_pb209.get()), 2 + 4 * i) );

            put(pellet("heaus", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_australium_greater.get()), 89 - i * 20)
                    .addOut(new ItemStack(ModItems.nugget_au198.get()), 5 + 10 * i)
                    .addOut(new ItemStack(Items.GOLD_NUGGET), 3 + 6 * i)
                    .addOut(new ItemStack(ModItems.nugget_pb209.get()), 2 + 4 * i) );

            // LES //
            put(pellet("les", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.nugget_les.get()), 90 - i * 20)
                    .addOut(longTiny(LongWasteClass.NEPTUNIUM), 2 + 3 * i)
                    .addOut(shortTiny(ShortWasteClass.NEPTUNIUM), 2 + 5 * i)
                    .addOut(longTiny(LongWasteClass.SCHRABIDIUM), 1 + 2 * i)
                    .addOut(shortTiny(ShortWasteClass.SCHRABIDIUM), 1 + 2 * i)
                    .addOut(new ItemStack(ModFuelItems.powder_coal_tiny.get()), 4 + 8 * i) );

            //TODO: Readd xenon processing if/when the NEI handler can display more than 6 outputs properly
            put(pellet("les", i + 5), new SILEXRecipe(600, 100, 2)    //I'd rather not fuck up the NEI handler, so six items it is
                    .addOut(new ItemStack(ModItems.nugget_les.get()), 90 - i * 20)            //Just bullshit something about "not enough np237 for extractable amounts of xe135"
                    .addOut(longTiny(LongWasteClass.NEPTUNIUM), 2 + 3 * i)
                    .addOut(shortTiny(ShortWasteClass.NEPTUNIUM), 2 + 5 * i)
                    .addOut(longTiny(LongWasteClass.SCHRABIDIUM), 1 + 2 * i)
                    .addOut(shortTiny(ShortWasteClass.SCHRABIDIUM), 1 + 2 * i)
                    .addOut(new ItemStack(ModFuelItems.powder_coal_tiny.get()), 4 + 8 * i) );

            // MES //
            put(pellet("mes", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.nugget_schrabidium_fuel.get()), 90 - i * 20)
                    .addOut(longTiny(LongWasteClass.NEPTUNIUM), 1 + 3 * i)
                    .addOut(shortTiny(ShortWasteClass.NEPTUNIUM), 2 + 4 * i)
                    .addOut(longTiny(LongWasteClass.SCHRABIDIUM), 1 + 3 * i)
                    .addOut(shortTiny(ShortWasteClass.SCHRABIDIUM), 2 + 4 * i)
                    .addOut(new ItemStack(ModFuelItems.powder_coal_tiny.get()), 4 + 6 * i) );

            //TODO: Readd xenon processing if/when the NEI handler can display more than 6 outputs properly
            put(pellet("mes", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.nugget_schrabidium_fuel.get()), 90 - i * 20) //ditto
                    .addOut(longTiny(LongWasteClass.NEPTUNIUM), 1 + 3 * i)
                    .addOut(shortTiny(ShortWasteClass.NEPTUNIUM), 2 + 4 * i)
                    .addOut(longTiny(LongWasteClass.SCHRABIDIUM), 1 + 3 * i)
                    .addOut(shortTiny(ShortWasteClass.SCHRABIDIUM), 2 + 4 * i)
                    .addOut(new ItemStack(ModFuelItems.powder_coal_tiny.get()), 4 + 6 * i) );

            // HES //
            put(pellet("hes", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.nugget_hes.get()), 90 - i * 20)
                    .addOut(longTiny(LongWasteClass.NEPTUNIUM), 1 + 2 * i)
                    .addOut(shortTiny(ShortWasteClass.NEPTUNIUM), 1 + 3 * i)
                    .addOut(longTiny(LongWasteClass.SCHRABIDIUM), 2 + 5 * i)
                    .addOut(shortTiny(ShortWasteClass.SCHRABIDIUM), 4 + 6 * i)
                    .addOut(new ItemStack(ModFuelItems.powder_coal_tiny.get()), 2 + 4 * i) );

            //TODO: Readd xenon processing if/when the NEI handler can display more than 6 outputs properly
            put(pellet("hes", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.nugget_hes.get()), 90 - i * 20) //ditto
                    .addOut(longTiny(LongWasteClass.NEPTUNIUM), 1 + 2 * i)
                    .addOut(shortTiny(ShortWasteClass.NEPTUNIUM), 1 + 3 * i)
                    .addOut(longTiny(LongWasteClass.SCHRABIDIUM), 2 + 5 * i)
                    .addOut(shortTiny(ShortWasteClass.SCHRABIDIUM), 4 + 6 * i)
                    .addOut(new ItemStack(ModFuelItems.powder_coal_tiny.get()), 2 + 4 * i) );

            // BALEFIRE //
            put(pellet("balefire", i), new SILEXRecipe(400, 100, 3)
                    .addOut(new ItemStack(ModItems.powder_balefire.get()), 90 - i * 20)
                    .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 10 + 20 * i) );

            // FLASHGOLD //
            put(pellet("balefire_gold", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.nugget_au198.get()), 90 - 20 * i)
                    .addOut(new ItemStack(ModItems.powder_balefire.get()), 10 + 20 * i) );

            // FLASHLEAD //
            put(pellet("flashlead", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.nugget_au198.get()), 44 - 10 * i)
                    .addOut(new ItemStack(ModItems.nugget_pb209.get()), 44 - 10 * i)
                    .addOut(new ItemStack(ModItems.nugget_bismuth.get()), 1 + 6 * i)
                    .addOut(new ItemStack(ModItems.nugget_mercury_tiny.get()), 1 + 6 * i)
                    .addOut(new ItemStack(ModItems.nugget_gh336.get()), 10 + 8 * i) ); //Reimumunch

            // POBE //
            put(pellet("po210be", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_polonium.get()), 45 - 10 * i)
                    .addOut(new ItemStack(ModItems.nugget_beryllium.get()), 45 - 10 * i)
                    .addOut(new ItemStack(ModItems.nugget_lead.get()), 5 + 10 * i)
                    .addOut(new ItemStack(ModFuelItems.powder_coal_tiny.get()), 5 + 10 * i) );

            // PUBE //
            put(pellet("pu238be", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_pu238.get()), 45 - 10 * i)
                    .addOut(new ItemStack(ModItems.nugget_beryllium.get()), 45 - 10 * i)
                    .addOut(new ItemStack(ModItems.nugget_lead.get()), 3 + 5 * i)
                    .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 2 + 5 * i)
                    .addOut(new ItemStack(ModFuelItems.powder_coal_tiny.get()), 5 + 10 * i) );

            put(pellet("pu238be", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 1)
                    .addOut(new ItemStack(ModItems.nugget_pu238.get()), 44 - 10 * i)
                    .addOut(new ItemStack(ModItems.nugget_beryllium.get()), 45 - 10 * i)
                    .addOut(new ItemStack(ModItems.nugget_lead.get()), 3 + 5 * i)
                    .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 2 + 5 * i)
                    .addOut(new ItemStack(ModFuelItems.powder_coal_tiny.get()), 5 + 10 * i) );

            // RABE //
            put(pellet("ra226be", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(ModItems.nugget_ra226.get()), 45 - 10 * i)
                    .addOut(new ItemStack(ModItems.nugget_beryllium.get()), 45 - 10 * i)
                    .addOut(new ItemStack(ModItems.nugget_lead.get()), 3 + 5 * i)
                    .addOut(new ItemStack(ModItems.nugget_polonium.get()), 2 + 5 * i)
                    .addOut(new ItemStack(ModFuelItems.powder_coal_tiny.get()), 5 + 10 * i) );

            // DRX //
            put(pellet("drx", i), new SILEXRecipe(600, 100, 4)
                    .addOut(new ItemStack(ModItems.undefined.get()), 1)
                    .addOut(new ItemStack(ModItems.undefined.get()), 1)
                    .addOut(new ItemStack(ModItems.undefined.get()), 1)
                    .addOut(new ItemStack(ModItems.undefined.get()), 1)
                    .addOut(new ItemStack(ModItems.undefined.get()), 1)
                    .addOut(new ItemStack(ModItems.undefined.get()), 1) );

            put(pellet("drx", i + 5), new SILEXRecipe(600, 100, 4)
                    .addOut(new ItemStack(ModItems.undefined.get()), 1)
                    .addOut(new ItemStack(ModItems.undefined.get()), 1)
                    .addOut(new ItemStack(ModItems.undefined.get()), 1)
                    .addOut(new ItemStack(ModItems.undefined.get()), 1)
                    .addOut(new ItemStack(ModItems.undefined.get()), 1)
                    .addOut(new ItemStack(ModItems.undefined.get()), 1) );

            // ZFB BI //
            put(pellet("zfb_bismuth", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.nugget_uranium.get()), 50 - i * 10)
                    .addOut(new ItemStack(ModItems.nugget_pu241.get()), 50 - i * 10)
                    .addOut(new ItemStack(ModItems.nugget_bismuth.get()), 50 + i * 20)
                    .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 150) );

            put(pellet("zfb_bismuth", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 3)
                    .addOut(new ItemStack(ModItems.nugget_uranium.get()), 50 - i * 10)
                    .addOut(new ItemStack(ModItems.nugget_pu241.get()), 50 - i * 10)
                    .addOut(new ItemStack(ModItems.nugget_bismuth.get()), 50 + i * 20)
                    .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 147) );

            // ZFB PU-241 //
            put(pellet("zfb_pu241", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.nugget_u235.get()), 50 - i * 10)
                    .addOut(new ItemStack(ModItems.nugget_pu240.get()), 50 - i * 10)
                    .addOut(new ItemStack(ModItems.nugget_pu241.get()), 50 + i * 20)
                    .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 150) );

            put(pellet("zfb_pu241", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 3)
                    .addOut(new ItemStack(ModItems.nugget_u235.get()), 50 - i * 10)
                    .addOut(new ItemStack(ModItems.nugget_pu240.get()), 50 - i * 10)
                    .addOut(new ItemStack(ModItems.nugget_pu241.get()), 50 + i * 20)
                    .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 147) );

            // ZFB RG-AM //
            put(pellet("zfb_am_mix", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.nugget_pu241.get()), 100 - i * 20)
                    .addOut(new ItemStack(ModItems.nugget_am_mix.get()), 50 + i * 20)
                    .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 150) );

            put(pellet("zfb_am_mix", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(ModItems.powder_xe135_tiny.get()), 3)
                    .addOut(new ItemStack(ModItems.nugget_pu241.get()), 100 - i * 20)
                    .addOut(new ItemStack(ModItems.nugget_am_mix.get()), 50 + i * 20)
                    .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 147) );
        }

        put(longWaste(LongWasteClass.URANIUM235), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_neptunium.get()), 20)
                .addOut(new ItemStack(ModItems.nugget_pu239.get()), 45)
                .addOut(new ItemStack(ModItems.nugget_pu240.get()), 20)
                .addOut(new ItemStack(ModItems.nugget_technetium.get()), 15)
                );
        put(longDepleted(LongWasteClass.URANIUM235), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_lead.get()), 65)
                .addOut(new ItemStack(ModItems.nugget_bismuth.get()), 20)
                .addOut(new ItemStack(ModItems.dust_tiny.get()), 15)
                );
        put(shortWaste(ShortWasteClass.URANIUM235), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_pu238.get()), 12)
                .addOut(new ItemStack(ModItems.powder_sr90_tiny.get()), 10)
                .addOut(new ItemStack(ModItems.powder_i131_tiny.get()), 10)
                .addOut(new ItemStack(ModItems.powder_cs137_tiny.get()), 12)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 56)
                );
        put(shortDepleted(ShortWasteClass.URANIUM235), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 10)
                .addOut(new ItemStack(ModItems.dust_tiny.get()), 32)
                .addOut(new ItemStack(ModItems.nugget_lead.get()), 22)
                .addOut(new ItemStack(ModItems.nugget_u238.get()), 5)
                .addOut(new ItemStack(ModItems.nugget_bismuth.get()), 15)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 16)
                );

        put(longWaste(LongWasteClass.URANIUM233), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_u235.get()), 15)
                .addOut(new ItemStack(ModItems.nugget_neptunium.get()), 25)
                .addOut(new ItemStack(ModItems.nugget_pu239.get()), 45)
                .addOut(new ItemStack(ModItems.nugget_technetium.get()), 15)
                );
        put(longDepleted(LongWasteClass.URANIUM233), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_lead.get()), 60)
                .addOut(new ItemStack(ModItems.nugget_bismuth.get()), 25)
                .addOut(new ItemStack(ModItems.dust_tiny.get()), 15)
                );
        put(shortWaste(ShortWasteClass.URANIUM233), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_pu238.get()), 4)
                .addOut(new ItemStack(ModItems.powder_sr90_tiny.get()), 12)
                .addOut(new ItemStack(ModItems.powder_i131_tiny.get()), 10)
                .addOut(new ItemStack(ModItems.powder_cs137_tiny.get()), 14)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 60)
                );
        put(shortDepleted(ShortWasteClass.URANIUM233), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 12)
                .addOut(new ItemStack(ModItems.dust_tiny.get()), 34)
                .addOut(new ItemStack(ModItems.nugget_lead.get()), 13)
                .addOut(new ItemStack(ModItems.nugget_u238.get()), 2)
                .addOut(new ItemStack(ModItems.nugget_bismuth.get()), 10)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 29)
                );

        put(shortWaste(ShortWasteClass.PLUTONIUM239), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_pu240.get()), 10)
                .addOut(new ItemStack(ModItems.nugget_pu241.get()), 25)
                .addOut(new ItemStack(ModItems.powder_sr90_tiny.get()), 2)
                .addOut(new ItemStack(ModItems.powder_i131_tiny.get()), 5)
                .addOut(new ItemStack(ModItems.powder_cs137_tiny.get()), 6)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 52)
                );
        put(shortDepleted(ShortWasteClass.PLUTONIUM239), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 2)
                .addOut(new ItemStack(ModItems.dust_tiny.get()), 16)
                .addOut(new ItemStack(ModItems.nugget_lead.get()), 40)
                .addOut(new ItemStack(ModItems.nugget_u238.get()), 3)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 39)
                );

        put(shortWaste(ShortWasteClass.PLUTONIUM240), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_pu241.get()), 15)
                .addOut(new ItemStack(ModItems.nugget_neptunium.get()), 5)
                .addOut(new ItemStack(ModItems.powder_sr90_tiny.get()), 2)
                .addOut(new ItemStack(ModItems.powder_i131_tiny.get()), 5)
                .addOut(new ItemStack(ModItems.powder_cs137_tiny.get()), 7)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 66)
                );
        put(shortDepleted(ShortWasteClass.PLUTONIUM240), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 2)
                .addOut(new ItemStack(ModItems.dust_tiny.get()), 22)
                .addOut(new ItemStack(ModItems.nugget_bismuth.get()), 20)
                .addOut(new ItemStack(ModItems.nugget_lead.get()), 17)
                .addOut(new ItemStack(ModItems.nugget_u238.get()), 3)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 36)
                );

        put(shortWaste(ShortWasteClass.PLUTONIUM241), new SILEXRecipe(900, 100, 2)
                .addOut(new ItemStack(ModItems.nugget_am241.get()), 25)
                .addOut(new ItemStack(ModItems.nugget_am242.get()), 35)
                .addOut(new ItemStack(ModItems.nugget_technetium.get()), 5)
                .addOut(new ItemStack(ModItems.powder_i131_tiny.get()), 3)
                .addOut(new ItemStack(ModItems.powder_cs137_tiny.get()), 7)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 25)
                );
        put(shortDepleted(ShortWasteClass.PLUTONIUM241), new SILEXRecipe(900, 100, 2)
                .addOut(new ItemStack(ModItems.nugget_bismuth.get()), 60)
                .addOut(new ItemStack(ModItems.dust_tiny.get()), 20)
                .addOut(new ItemStack(ModItems.nugget_lead.get()), 15)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 5)
                );

        put(longWaste(LongWasteClass.THORIUM), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_u233.get()), 40)
                .addOut(new ItemStack(ModItems.nugget_u235.get()), 35)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 25)
                );
        put(longDepleted(LongWasteClass.THORIUM), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_lead.get()), 35)
                .addOut(new ItemStack(ModItems.nugget_bismuth.get()), 40)
                .addOut(new ItemStack(ModItems.dust_tiny.get()), 15)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 10)
                );

        put(longWaste(LongWasteClass.NEPTUNIUM), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_u238.get()), 15)
                .addOut(new ItemStack(ModItems.nugget_pu239.get()), 40)
                .addOut(new ItemStack(ModItems.nugget_pu240.get()), 15)
                .addOut(new ItemStack(ModItems.nugget_technetium.get()), 15)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 15)
                );
        put(longDepleted(LongWasteClass.NEPTUNIUM), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_u238.get()), 16)
                .addOut(new ItemStack(ModItems.nugget_lead.get()), 55)
                .addOut(new ItemStack(ModItems.dust_tiny.get()), 20)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 9)
                );
        put(shortWaste(ShortWasteClass.NEPTUNIUM), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_pu238.get()), 40)
                .addOut(new ItemStack(ModItems.powder_sr90_tiny.get()), 7)
                .addOut(new ItemStack(ModItems.powder_i131_tiny.get()), 5)
                .addOut(new ItemStack(ModItems.powder_cs137_tiny.get()), 8)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 40)
                );
        put(shortDepleted(ShortWasteClass.NEPTUNIUM), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_zirconium.get()), 7)
                .addOut(new ItemStack(ModItems.dust_tiny.get()), 29)
                .addOut(new ItemStack(ModItems.nugget_u238.get()), 2)
                .addOut(new ItemStack(ModItems.nugget_lead.get()), 45)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 17)
                );

        put(longWaste(LongWasteClass.SCHRABIDIUM), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_solinium.get()), 25)
                .addOut(new ItemStack(ModItems.nugget_euphemium.get()), 18)
                .addOut(new ItemStack(ModItems.nugget_gh336.get()), 16)
                .addOut(new ItemStack(ModItems.nugget_tantalium.get()), 8)
                .addOut(new ItemStack(ModItems.powder_neodymium_tiny.get()), 8)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 25)
                );
        put(longDepleted(LongWasteClass.SCHRABIDIUM), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_solinium.get()), 20)
                .addOut(new ItemStack(ModItems.nugget_euphemium.get()), 18)
                .addOut(new ItemStack(ModItems.nugget_gh336.get()), 15)
                .addOut(new ItemStack(ModItems.nugget_tantalium.get()), 8)
                .addOut(new ItemStack(ModItems.powder_neodymium_tiny.get()), 8)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 31)
                );
        put(shortWaste(ShortWasteClass.SCHRABIDIUM), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_pb209.get()), 7) //We don't have any spicy lanthanides, and lead 209 + gold 198 is already *severely* pushing it, but there's no
                .addOut(new ItemStack(ModItems.nugget_au198.get()), 7) //point in contributing to pointless item bloat, so this will have to do
                .addOut(new ItemStack(ModItems.powder_cs137_tiny.get()), 5)
                .addOut(new ItemStack(ModItems.powder_i131_tiny.get()), 5)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 76)
                );
        put(shortDepleted(ShortWasteClass.SCHRABIDIUM), new SILEXRecipe(900, 100, 1)
                .addOut(new ItemStack(ModItems.nugget_bismuth.get()), 7)
                .addOut(new ItemStack(ModItems.nugget_mercury_tiny.get()), 12)
                .addOut(new ItemStack(ModItems.powder_cerium_tiny.get()), 14)
                .addOut(new ItemStack(ModItems.powder_lanthanium_tiny.get()), 15)
                .addOut(new ItemStack(ModItems.dust_tiny.get()), 20)
                .addOut(new ItemStack(ModItems.nuclear_waste_tiny.get()), 32)
                );

        put(new ComparableStack(ModItems.falloutitem.get()), new SILEXRecipe(900, 100, 2)
                .addOut(new ItemStack(ModItems.dust_tiny.get()), 90)
                .addOut(new ItemStack(ModItems.nugget_co60.get()), 2)
                .addOut(new ItemStack(ModItems.powder_sr90_tiny.get()), 3)
                .addOut(new ItemStack(ModItems.powder_i131_tiny.get()), 1)
                .addOut(new ItemStack(ModItems.powder_cs137_tiny.get()), 3)
                .addOut(new ItemStack(ModItems.nugget_au198.get()), 1)
                );

        put(new ComparableStack(Items.GRAVEL), new SILEXRecipe(1000, 250, EnumWavelengths.VISIBLE)
                .addOut(new ItemStack(Items.FLINT), 80)
                .addOut(new ItemStack(ModItems.powder_boron.get()), 5)
                .addOut(new ItemStack(ModItems.powder_lithium.get()), 10)
                .addOut(new ItemStack(ModItems.fluorite.get()), 5)
                );

        put(fluidIcon(Fluids.FULLERENE),
                new SILEXRecipe(1_000, 1_000, EnumWavelengths.VISIBLE).addOut(new ItemStack(ModIngredientItems.ash(EnumAshType.FULLERENE)), 1));
    }

    /** 1.7 {@code recipes.put(input, recipe)}, then the translations onto {@code input}. */
    private void put(AStack input, SILEXRecipe recipe) {
        row(input, recipe);
        String target = id(input);
        for (AStack[] translation : translations) {
            if (id(translation[1]).equals(target)) row(translation[0], recipe);
        }
    }

    private void row(AStack input, SILEXRecipe recipe) {
        register(id(input), new Row(input, recipe.fluidProduced, recipe.fluidConsumed, recipe.laserStrength,
                List.copyOf(recipe.outputs), nextOrder()));
    }

    private void itemTranslation(AStack from, AStack to) {
        translations.add(new AStack[] {from, to});
    }

    private void dictTranslation(AStack from, AStack to) {
        translations.add(new AStack[] {from, to});
    }

    private static String id(AStack stack) {
        if (stack instanceof VariantStack pellet) return path(pellet.item) + "_" + pellet.variant;
        if (stack instanceof FluidIconStack fluid) return "fluid_" + fluid.fluid.getKey();
        if (stack instanceof TagStack ore) {
            // a 1.7 ore name: the id of its HBM item (c:ingots/uranium -> ingot_uranium, c:dusts/uranium -> powder_uranium)
            String[] path = ore.tag.location().getPath().split("/");
            String prefix = path[0].equals("dusts") ? "powder" : path[0].substring(0, path[0].length() - 1);
            return prefix + "_" + path[1];
        }
        return path(((ComparableStack) stack).item);
    }

    private static String path(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace().equals("minecraft") ? "minecraft_" + id.getPath() : id.getPath();
    }

    /** 1.7 {@code new ComparableStack(ModItems.rbmk_pellet_<name>, 1, meta)}. */
    private static VariantStack pellet(String name, int meta) {
        Item item = BuiltInRegistries.ITEM.getOptional(dev.golem.core.Ids.of("hbm", "rbmk_pellet_" + name))
                .orElseThrow(() -> new IllegalStateException("no hbm:rbmk_pellet_" + name));
        return new VariantStack(item, meta);
    }

    /** 1.7 {@code new ComparableStack(ModItems.fluid_icon, 1, fluid.getID())}. */
    private static FluidIconStack fluidIcon(FluidType fluid) {
        return new FluidIconStack(fluid);
    }

    private static ComparableStack longWaste(LongWasteClass c) { return new ComparableStack(ModStorageDrumItems.nuclear_waste_long.get(c.ordinal())); }
    private static ComparableStack longDepleted(LongWasteClass c) { return new ComparableStack(ModStorageDrumItems.nuclear_waste_long_depleted.get(c.ordinal())); }
    private static ComparableStack shortWaste(ShortWasteClass c) { return new ComparableStack(ModStorageDrumItems.nuclear_waste_short.get(c.ordinal())); }
    private static ComparableStack shortDepleted(ShortWasteClass c) { return new ComparableStack(ModStorageDrumItems.nuclear_waste_short_depleted.get(c.ordinal())); }
    private static ItemStack longTiny(LongWasteClass c) { return ModStorageDrumItems.nuclear_waste_long_tiny.stack(c.ordinal()); }
    private static ItemStack shortTiny(ShortWasteClass c) { return ModStorageDrumItems.nuclear_waste_short_tiny.stack(c.ordinal()); }

    /** 1.7 {@code SILEXRecipe}: the values of a row before it has an input. */
    private static final class SILEXRecipe {

        final int fluidProduced;
        final int fluidConsumed;
        final EnumWavelengths laserStrength;
        final List<WeightedOut> outputs = new ArrayList<>();

        SILEXRecipe(int fluidProduced, int fluidConsumed, EnumWavelengths laserStrength) {
            this.fluidProduced = fluidProduced;
            this.fluidConsumed = fluidConsumed;
            this.laserStrength = laserStrength;
        }

        SILEXRecipe(int fluidProduced, int fluidConsumed, int laserStrength) {
            this(fluidProduced, fluidConsumed, EnumWavelengths.values().get(laserStrength));
        }

        SILEXRecipe addOut(ItemStack stack, int weight) {
            outputs.add(new WeightedOut(ItemOut.of(stack), weight));
            return this;
        }
    }
}
