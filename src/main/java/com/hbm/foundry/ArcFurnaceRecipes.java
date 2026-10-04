package com.hbm.foundry;

import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.machine.ItemScraps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Feature-local replacement for the old {@code ArcFurnaceRecipes}. It deliberately has no global
 * recipe-loader hook: the furnace leaf owns this table and the loader integration exposes it later.
 */
public final class ArcFurnaceRecipes {

    private ArcFurnaceRecipes() {}

    public static final List<ArcFurnaceRecipe> RECIPES = new ArrayList<>();
    private static final Set<String> SOLID_INPUTS = new HashSet<>();
    private static final Set<String> LIQUID_INPUTS = new HashSet<>();
    private static boolean registered;

    public static final class ArcFurnaceRecipe {
        public final RecipesCommon.AStack input;
        public final ItemStack solidOutput;
        public final Mats.MaterialStack[] fluidOutput;

        private ArcFurnaceRecipe(RecipesCommon.AStack input, ItemStack solidOutput,
                                 Mats.MaterialStack[] fluidOutput) {
            this.input = input;
            this.solidOutput = solidOutput;
            this.fluidOutput = fluidOutput;
        }
    }

    /** Registers the old explicit recipes, then only material recipes whose current tags have members. */
    public static synchronized void register() {
        if (registered) return;
        registered = true;

        int n = MaterialShapes.NUGGET.quantity;
        int i = MaterialShapes.INGOT.quantity;
        register(new RecipesCommon.TagStack(ItemTags.SAND), new ItemStack(ModItems.nugget_silicon.get()),
                new Mats.MaterialStack(Mats.MAT_SILICON, n));
        register(new RecipesCommon.ComparableStack(Items.FLINT), new ItemStack(ModItems.nugget_silicon.get(), 4),
                new Mats.MaterialStack(Mats.MAT_SILICON, i / 2));
        register(tag("c:gems/quartz"), new ItemStack(ModItems.nugget_silicon.get(), 3),
                new Mats.MaterialStack(Mats.MAT_SILICON, n * 3));
        register(tag("c:dusts/quartz"), new ItemStack(ModItems.nugget_silicon.get(), 3),
                new Mats.MaterialStack(Mats.MAT_SILICON, n * 3));
        register(new RecipesCommon.ComparableStack(Blocks.QUARTZ_BLOCK.asItem()),
                new ItemStack(ModItems.nugget_silicon.get(), 12),
                new Mats.MaterialStack(Mats.MAT_SILICON, n * 12));

        // 1.7 ArcFurnaceRecipes:58-59, FIBER.ingot() and FIBER.block()
        register(new RecipesCommon.ComparableStack(ModItems.ingot_fiberglass.get()),
                new ItemStack(ModItems.nugget_silicon.get(), 4),
                new Mats.MaterialStack(Mats.MAT_SILICON, i / 2));
        register(new RecipesCommon.ComparableStack(com.hbm.blocks.ModDecoContent.block_fiberglass.get().asItem()),
                new ItemStack(ModItems.nugget_silicon.get(), 40),
                new Mats.MaterialStack(Mats.MAT_SILICON, i * 9 / 2));
        register(Mats.MAT_ASBESTOS.ingot(), new ItemStack(ModItems.nugget_silicon.get(), 4),
                new Mats.MaterialStack(Mats.MAT_SILICON, i / 2));
        register(Mats.MAT_ASBESTOS.dust(), new ItemStack(ModItems.nugget_silicon.get(), 4),
                new Mats.MaterialStack(Mats.MAT_SILICON, i / 2));
        register(Mats.MAT_ASBESTOS.block(), new ItemStack(ModItems.nugget_silicon.get(), 40),
                new Mats.MaterialStack(Mats.MAT_SILICON, i * 9 / 2));
        register(new RecipesCommon.ComparableStack(com.hbm.blocks.ModFallingBlocks.sand_quartz.get().asItem()),
                new ItemStack(com.hbm.blocks.ModGlassBlocks.glass_quartz.get().asItem()));
        register(Mats.MAT_BORAX.dust(), new ItemStack(ModItems.powder_boron_tiny.get(), 3),
                new Mats.MaterialStack(Mats.MAT_BORON, n * 3));
        com.hbm.inventory.recipes.BedrockOreChainRecipes.arcFurnace(); // 1.7 order: before the material autogen

        registerMaterialShapes();
        registerExplicitMaterialItems();
    }

    private static void registerMaterialShapes() {
        for (NTMMaterial material : Mats.orderedList) {
            NTMMaterial output = material.smeltsInto;
            if (output == null || output.smeltable != NTMMaterial.SmeltingBehavior.SMELTABLE) continue;
            if (material.convIn == 0) continue;
            for (MaterialShapes shape : MaterialShapes.values()) {
                if (shape.quantity <= 0) continue;
                if (!hasTagMember(material.tag(shape))) continue;
                int amount = (int) ((long) shape.quantity * material.convOut / material.convIn);
                register(new RecipesCommon.TagStack(material.tag(shape)), null,
                        new Mats.MaterialStack(output, amount));
            }
        }
    }

    /** Covers current Mats explicit item distributions that have no per-shape tag. */
    private static void registerExplicitMaterialItems() {
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack singular = new ItemStack(item);
            List<Mats.MaterialStack> materials = Mats.getMaterialsFromItem(singular);
            if (materials.isEmpty()) continue;
            List<Mats.MaterialStack> fluid = new ArrayList<>();
            for (Mats.MaterialStack material : materials) {
                NTMMaterial output = material.material.smeltsInto;
                if (output != null && output.smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE) {
                    fluid.add(new Mats.MaterialStack(output,
                            (int) ((long) material.amount * material.material.convOut / material.material.convIn)));
                }
            }
            if (!fluid.isEmpty() && !hasMatchingRecipe(singular, true)) register(new RecipesCommon.ComparableStack(item), null,
                    fluid.toArray(new Mats.MaterialStack[0]));
        }
    }

    private static boolean hasTagMember(net.minecraft.tags.TagKey<Item> tag) {
        for (Item item : BuiltInRegistries.ITEM) if (new ItemStack(item).is(tag)) return true;
        return false;
    }

    private static boolean hasMatchingRecipe(ItemStack stack, boolean liquid) {
        for (ArcFurnaceRecipe recipe : RECIPES) {
            if (recipe.input.matchesRecipe(stack, true)
                    && (liquid ? recipe.fluidOutput.length > 0 : recipe.solidOutput != null)) return true;
        }
        return false;
    }

    public static void register(RecipesCommon.AStack input, ItemStack solidOutput,
                                Mats.MaterialStack... fluidOutput) {
        String key = inputKey(input);
        boolean solid = solidOutput != null && !solidOutput.isEmpty();
        boolean liquid = fluidOutput.length > 0;
        if (solid && !SOLID_INPUTS.add(key)) solid = false;
        if (liquid && !LIQUID_INPUTS.add(key)) liquid = false;
        if (!solid && !liquid) return;
        RECIPES.add(new ArcFurnaceRecipe(input, solid ? solidOutput : null,
                liquid ? fluidOutput : new Mats.MaterialStack[0]));
    }

    public static @org.jspecify.annotations.Nullable ArcFurnaceRecipe getOutput(ItemStack stack, boolean liquid) {
        if (stack.isEmpty()) return null;
        register();

        if (liquid && stack.getItem() == ModMachineItems.scraps.get()) {
            Mats.MaterialStack material = ItemScraps.getMats(stack);
            if (material != null && material.material.smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE) {
                return new ArcFurnaceRecipe(new RecipesCommon.ComparableStack(stack), null, new Mats.MaterialStack[]{material});
            }
        }

        for (ArcFurnaceRecipe recipe : RECIPES) {
            if (recipe.input.matchesRecipe(stack, true)
                    && (liquid ? recipe.fluidOutput.length > 0 : recipe.solidOutput != null)) return recipe;
        }
        return null;
    }

    /**
     * {@link #getOutput(ItemStack, boolean)} plus 1.7's {@code registerFurnaceSmeltables}: in solid mode, any item whose
     * furnace result exists and which (or whose result) is "arc smeltable" (any ingot / ore / plate / storage-block
     * ore-dict member, plus 1.7's vanilla list) smelts to its furnace result. The furnace recipes live in the level's
     * recipe manager, so this pass needs the level and is not listed in JEI.
     */
    public static @org.jspecify.annotations.Nullable ArcFurnaceRecipe getOutput(ItemStack stack, boolean liquid,
            net.minecraft.world.level.@org.jspecify.annotations.Nullable Level level) {
        ArcFurnaceRecipe explicit = getOutput(stack, liquid);
        if (explicit != null || liquid || level == null || stack.isEmpty()) return explicit;
        ItemStack result = dev.golem.core.world.Recipes.smeltingResult(level,
                dev.golem.core.world.ItemStacks.copyWithCount(stack, 1)).orElse(ItemStack.EMPTY);
        if (result.isEmpty()) return null;
        Set<Item> smeltable = arcSmeltable();
        if (!smeltable.contains(stack.getItem()) && !smeltable.contains(result.getItem())) return null;
        return new ArcFurnaceRecipe(new RecipesCommon.ComparableStack(stack.getItem()), result, new Mats.MaterialStack[0]);
    }

    private static Set<Item> arcSmeltable;

    /** 1.7 {@code OreDictManager.arcSmeltable}: every ingot/ore/plate/block ore-dict member, plus the vanilla list. */
    private static synchronized Set<Item> arcSmeltable() {
        if (arcSmeltable != null) return arcSmeltable;
        Set<Item> set = new HashSet<>();
        dev.golem.core.world.Compat.itemTagKeys().forEach(tag -> {
            if (!tag.location().getNamespace().equals("c")) return;
            String path = tag.location().getPath();
            if (path.startsWith("ingots/") || path.startsWith("ores/") || path.startsWith("plates/")
                    || path.startsWith("storage_blocks/")) {
                for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) set.add(holder.value());
            }
        });
        // OreDictManager.compensateMojangSpaghettiBullshit
        for (Item item : new Item[]{Items.GOLD_ORE, Items.IRON_ORE, Items.LAPIS_ORE, Items.DIAMOND_ORE, Items.REDSTONE_ORE,
                Items.EMERALD_ORE, Items.NETHER_QUARTZ_ORE, Items.GOLD_BLOCK, Items.IRON_BLOCK, Items.LAPIS_BLOCK,
                Items.DIAMOND_BLOCK, Items.REDSTONE_BLOCK, Items.EMERALD_BLOCK, Items.QUARTZ_BLOCK, Items.IRON_INGOT,
                Items.GOLD_INGOT, Items.BRICK, Items.NETHER_BRICK}) {
            set.add(item);
        }
        if (!set.isEmpty()) arcSmeltable = set; // tags may not be bound yet on the first call; retry until they are
        return set;
    }

    public static List<ArcFurnaceRecipe> solidRecipes() {
        register();
        return RECIPES.stream().filter(r -> r.solidOutput != null).toList();
    }

    public static List<ArcFurnaceRecipe> liquidRecipes() {
        register();
        return RECIPES.stream().filter(r -> r.fluidOutput.length > 0).toList();
    }

    private static RecipesCommon.TagStack tag(String id) {
        return new RecipesCommon.TagStack(net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM, dev.golem.core.Ids.parse(id)));
    }

    private static String inputKey(RecipesCommon.AStack input) {
        if (input instanceof RecipesCommon.TagStack tag) return tag.tag.location().toString();
        if (input instanceof RecipesCommon.ComparableStack comparable) return "item:" + comparable.item;
        return input.toString();
    }
}
