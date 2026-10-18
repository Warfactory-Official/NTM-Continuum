package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.RockMillRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe.OutputChoice;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModItems;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.7 {@code RockMillRecipes.registerDefaults}. */
public final class RockMillTable extends GenericTableSource<GenericRecipe> {

    private static final TagKey<Item> KEY_COBBLESTONE = TagKey.create(Registries.ITEM, Ids.of("c", "cobblestones"));
    private static final TagKey<Item> KEY_SAND = TagKey.create(Registries.ITEM, Ids.of("c", "sands"));

    public RockMillTable() {
        super(RockMillRecipes.INSTANCE);
    }

    /** The cobble and sand rows name their icon: 1.7's first "cobblestone" and "sand" entries are the vanilla blocks, the tags' are not. */
    @Override
    protected void registerDefaults() {

        int consumption = 25;
        int duraShort = 100;
        int duraLong = 200;

        String groupCrush = "autoswitch.crushing";

        this.register(new GenericRecipe("rock.cobble").setup(duraShort, consumption).setNameWrapper("rock.crushing")
                .inputItems(new TagStack(KEY_COBBLESTONE))
                .inputFluids(new FluidStack(Fluids.WATER, 250))
                .outputFluids(new FluidStack(Fluids.COLLOID, 250))
                .outputChoices(
                        new OutputChoice(new ItemStack(Items.GRAVEL), 95),
                        new OutputChoice(new ItemStack(ModItems.powder_quartz.get()), 5)
                ).setIcon(new ItemStack(Items.COBBLESTONE)).setGroup(groupCrush, this));

        this.register(new GenericRecipe("rock.gravel").setup(duraShort, consumption).setNameWrapper("rock.crushing")
                .inputItems(new ComparableStack(Items.GRAVEL))
                .inputFluids(new FluidStack(Fluids.WATER, 250))
                .outputFluids(new FluidStack(Fluids.COLLOID, 250))
                .outputChoices(
                        new OutputChoice(new ItemStack(Items.SAND), 75),
                        new OutputChoice(new ItemStack(Items.FLINT), 20),
                        new OutputChoice(new ItemStack(ModItems.powder_boron.get()), 5)
                ).setIconToFirstIngredient().setGroup(groupCrush, this));

        this.register(new GenericRecipe("rock.sand").setup(duraShort, consumption).setNameWrapper("rock.crushing")
                .inputItems(new TagStack(KEY_SAND))
                .inputFluids(new FluidStack(Fluids.WATER, 250))
                .outputFluids(new FluidStack(Fluids.COLLOID, 250))
                .outputChoices(
                        new OutputChoice(new ItemStack(ModItems.dust.get()), 90),
                        new OutputChoice(new ItemStack(ModItems.powder_calcium.get()), 5),
                        new OutputChoice(new ItemStack(ModItems.fluorite.get()), 5)
                ).setIcon(new ItemStack(Items.SAND)).setGroup(groupCrush, this));

        this.register(new GenericRecipe("rock.netherrack").setup(duraShort, consumption).setNameWrapper("rock.crushing")
                .inputItems(new ComparableStack(Items.NETHERRACK))
                .inputFluids(new FluidStack(Fluids.WATER, 250))
                .outputFluids(new FluidStack(Fluids.LAVA, 100))
                .outputChoices(
                        new OutputChoice(new ItemStack(Items.GRAVEL), 50),
                        new OutputChoice(new ItemStack(Items.SOUL_SAND), 25),
                        new OutputChoice(new ItemStack(Items.GLOWSTONE_DUST), 15),
                        new OutputChoice(new ItemStack(ModItems.powder_quartz.get()), 10)
                ).setIconToFirstIngredient().setGroup(groupCrush, this));

        this.register(new GenericRecipe("rock.soulsand").setup(duraShort, consumption).setNameWrapper("rock.crushing")
                .inputItems(new ComparableStack(Items.SOUL_SAND))
                .inputFluids(new FluidStack(Fluids.WATER, 250))
                .outputFluids(new FluidStack(Fluids.LAVA, 100))
                .outputChoices(
                        new OutputChoice(new ItemStack(Items.SAND), 50),
                        new OutputChoice(new ItemStack(ModFuelItems.powder_fire.get()), 25),
                        new OutputChoice(new ItemStack(ModItems.powder_uranium.get()), 15),
                        new OutputChoice(new ItemStack(Items.BLAZE_POWDER), 5),
                        new OutputChoice(new ItemStack(Items.NETHER_WART), 5)
                ).setIconToFirstIngredient().setGroup(groupCrush, this));

        this.register(new GenericRecipe("rock.schist").setup(duraLong, consumption).setNameWrapper("rock.crushing")
                .inputItems(new ComparableStack(ModBlocks.stone_gneiss.get().asItem()))
                .inputFluids(new FluidStack(Fluids.WATER, 250))
                .outputFluids(new FluidStack(Fluids.COLLOID, 250))
                .outputChoices(
                        new OutputChoice(new ItemStack(Items.GRAVEL), 50),
                        new OutputChoice(new ItemStack(Items.SAND), 10),
                        new OutputChoice(new ItemStack(ModItems.powder_lithium.get()), 25),
                        new OutputChoice(new ItemStack(ModItems.powder_niobium.get()), 5),
                        new OutputChoice(new ItemStack(ModItems.powder_uranium.get()), 5),
                        new OutputChoice(new ItemStack(ModItems.powder_gold.get()), 5)
                ).setIconToFirstIngredient().setGroup(groupCrush, this));

        this.register(new GenericRecipe("rock.basalt").setup(duraLong, consumption).setNameWrapper("rock.crushing")
                .inputItems(new ComparableStack(ModBlocks.basalt.get().asItem()))
                .inputFluids(new FluidStack(Fluids.WATER, 250))
                .outputFluids(new FluidStack(Fluids.COLLOID, 250))
                .outputChoices(
                        new OutputChoice(new ItemStack(Items.GRAVEL), 50),
                        new OutputChoice(ModIngredientItems.ash(EnumAshType.MISC, 1), 25),
                        new OutputChoice(new ItemStack(ModItems.powder_quartz.get()), 15),
                        new OutputChoice(new ItemStack(ModFallingBlocks.gravel_obsidian.get()), 10)
                ).setIconToFirstIngredient().setGroup(groupCrush, this));

        this.register(new GenericRecipe("rock.hematite").setup(duraLong, consumption).setNameWrapper("rock.crushing")
                .inputItems(HEMATITE.ore())
                .inputFluids(new FluidStack(Fluids.WATER, 250))
                .outputFluids(new FluidStack(Fluids.COLLOID, 250))
                .outputChoices(
                        new OutputChoice(new ItemStack(Items.GRAVEL), 65),
                        new OutputChoice(new ItemStack(ModItems.powder_iron.get()), 25),
                        new OutputChoice(new ItemStack(ModItems.powder_titanium.get()), 10)
                ).setIconToFirstIngredient().setGroup(groupCrush, this));

        this.register(new GenericRecipe("rock.bauxite").setup(duraLong, consumption).setNameWrapper("rock.crushing")
                .inputItems(BAUXITE.ore())
                .inputFluids(new FluidStack(Fluids.WATER, 250))
                .outputFluids(new FluidStack(Fluids.COLLOID, 250))
                .outputChoices(
                        new OutputChoice(new ItemStack(Items.GRAVEL), 25),
                        new OutputChoice(new ItemStack(Items.CLAY_BALL), 25),
                        new OutputChoice(new ItemStack(ModOreBlocks.stone_resource_hematite.get()), 25),
                        new OutputChoice(new ItemStack(ModBlocks.ore_titanium.get()), 25)
                ).setIconToFirstIngredient().setGroup(groupCrush, this));

        this.register(new GenericRecipe("rock.clay").setup(duraLong, consumption)
                .inputItems(new TagStack(KEY_SAND, 2))
                .inputFluids(new FluidStack(Fluids.COLLOID, 2_500))
                .outputItems(new ItemStack(Items.CLAY_BALL, 4)));

        this.register(new GenericRecipe("rock.colloid").setup(duraShort, consumption)
                .inputFluids(new FluidStack(Fluids.COLLOID, 1_000))
                .outputItems(new ItemStack(Items.SAND)));
    }
}
