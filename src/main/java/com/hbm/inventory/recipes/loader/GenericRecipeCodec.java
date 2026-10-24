package com.hbm.inventory.recipes.loader;

import static dev.golem.core.world.RecipeFields.field;

import com.hbm.inventory.FluidContainerStack;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModDataComponents;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.RecipeFields;
import dev.golem.core.world.RecipeFields.Kind;
import dev.golem.core.world.RecipeFields.Role;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The JSON form of a {@link GenericRecipe}: ntm-next's machine-recipe schema, so a datapack (or KubeJS) writes the same
 * file for both ports.
 *
 * <pre>
 * {
 *   "type": "hbm:chemical_plant",
 *   "name": "chem.oxygen",                                   the recipe's internal name (machines save it)
 *   "input_items": [{"ingredient": "minecraft:coal", "count": 1}, {"ingredient": "#c:dusts/sulfur"}],
 *   "input_fluids": [{"type": "hbm:air", "amount": 8000, "pressure": 0}],
 *   "output_items": ["minecraft:cobblestone", {"id": "hbm:powder_coal", "count": 2, "chance": 0.5}],
 *   "output_fluids": [{"type": "hbm:oxygen", "amount": 500}],
 *   "duration": 20, "power": 400,
 *   "icon": {"id": "hbm:fluid_icon", "item_data": {"fluid": "hbm:oxygen"}},   optional; else the first output
 *   "custom_localization": true,                             display the translation of "name" (1.7 setNamed)
 *   "name_wrapper": "purex.recycle", "blueprint_pools": ["alt.x"], "auto_switch_group": "...", "order": 12,
 *   "input_items_expensive": [...], "input_fluids_expensive": [...],    1.7 inputItemsEx / inputFluidsEx
 *   "blueprint_pools_528": ["528.chip"],                               1.7 setPools528 (ntm-next's field)
 *   "requires": ["!enable_528", "expensive_mode", "#c:nuggets/naquadria"]   see GenericRecipe.requires
 * }
 * </pre>
 *
 * <b>Ingredients</b> ({@link #STACK}): {@code {"ingredient": id or #tag, "count"}}, an item with exactly given data
 * {@code {"ingredient": id, "exact": true, "item_data": {...}}} (1.7 {@code new ComparableStack(ItemStack)}; without
 * {@code exact} an item matches with any data), an autogen shape {@code {"material", "shape", "count"}}, and the typed
 * kinds of {@link RecipeIngredients}: {@code {"type": "hbm:fluid_container", "fluid", "amount"}} (any full container of
 * that fluid and amount, 1.7's fluid dictionary entries), {@code {"type": "hbm:cold", "item"}}, {@code {"type":
 * "hbm:any_of", "of": [ingredients]}} and {@code {"type": "hbm:fluid_item", "item", "fluid"}}.
 *
 * <b>Outputs</b>: {@code "id"}, {@code {"id", "count", "chance", "item_data"}}, {@code {"tag", "count"}} (the tag's first
 * item when the table rebuilds, 1.7's {@code OreDictionary.getOres(name).get(0)}), or one weighted choice (1.7
 * {@code ChanceOutputMulti}, ntm-next's weighted list) as the only entry: {@code [[{"data": output, "weight": 90},
 * {"data": output, "weight": 10}]]}. Item data is {@link ItemData}, one form on every Minecraft version.
 *
 * Fluids are vanilla fluid ids ({@code minecraft:water}, {@code hbm:oxygen}). A subclass row adds its own fields with
 * {@link #codec(Function, MapCodec, Function, BiConsumer)} (fusion temperatures, plasma ignition). Writing a row this
 * schema cannot state fails rather than dropping data.
 */
public final class GenericRecipeCodec {

    private GenericRecipeCodec() {}

    public static final Codec<Either<TagKey<Item>, Item>> INGREDIENT =
            Codec.either(TagKey.hashedCodec(Registries.ITEM), ItemStacks.itemCodec());

    private record Input(Either<TagKey<Item>, Item> ingredient, int count, boolean exact, Optional<ItemData> data) {}

    private static final Codec<Input> INPUT = RecordCodecBuilder.create(i -> i.group(
            INGREDIENT.fieldOf("ingredient").forGetter(Input::ingredient),
            Codec.INT.optionalFieldOf("count", 1).forGetter(Input::count),
            Codec.BOOL.optionalFieldOf("exact", false).forGetter(Input::exact),
            ItemData.CODEC.optionalFieldOf("item_data").forGetter(Input::data)
    ).apply(i, Input::new));

    private record MaterialInput(NTMMaterial material,
                                 MaterialShapes shape, int count) {}

    /** HBM's autogen shapes (one item per shape, told apart by material): {@code {"material": "bscco", "shape": "wiredense"}}. */
    private static final Codec<MaterialInput> MATERIAL_INPUT = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.comapFlatMap(name -> {
                NTMMaterial material = Mats.byName(name);
                return material == null ? DataResult.error(() -> "unknown material " + name) : DataResult.success(material);
            }, material -> material.name).fieldOf("material").forGetter(MaterialInput::material),
            Codec.STRING.comapFlatMap(name -> {
                try {
                    return DataResult.success(MaterialShapes.valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    return DataResult.error(() -> "unknown material shape " + name);
                }
            }, shape -> shape.name().toLowerCase(Locale.ROOT)).fieldOf("shape").forGetter(MaterialInput::shape),
            Codec.INT.optionalFieldOf("count", 1).forGetter(MaterialInput::count)
    ).apply(i, MaterialInput::new));

    /** A fluid by its vanilla fluid id; only fluids HBM knows (its own, water, lava). */
    public static final Codec<FluidType> FLUID = BuiltInRegistries.FLUID.byNameCodec().comapFlatMap(
            fluid -> {
                FluidType type = Fluids.fromVanilla(fluid);
                return type == Fluids.NONE && fluid != net.minecraft.world.level.material.Fluids.EMPTY
                        ? DataResult.error(() -> "not an HBM fluid: " + fluid)
                        : DataResult.success(type);
            },
            Fluids::toVanilla);

    public static final Codec<FluidStack> FLUID_STACK = RecordCodecBuilder.create(i -> i.group(
            FLUID.fieldOf("type").forGetter(stack -> stack.type),
            Codec.INT.fieldOf("amount").forGetter(stack -> stack.fill),
            Codec.INT.optionalFieldOf("pressure", 0).forGetter(stack -> stack.pressure)
    ).apply(i, FluidStack::new));

    // Typed ingredient kinds

    private record Typed<A>(String type, A body) {}

    /** {@code body} plus a {@code "type": type} key that must match. */
    private static <A> Codec<A> typed(String type, MapCodec<A> body) {
        return RecordCodecBuilder.<Typed<A>>create(i -> i.group(
                Codec.STRING.fieldOf("type").forGetter(Typed::type),
                body.forGetter(Typed::body)
        ).apply(i, Typed::new)).flatXmap(
                typed -> type.equals(typed.type()) ? DataResult.success(typed.body())
                        : DataResult.error(() -> "expected ingredient type " + type + ", got " + typed.type()),
                value -> DataResult.success(new Typed<>(type, value)));
    }

    private static final String TYPE_FLUID_CONTAINER = "hbm:fluid_container";
    private static final String TYPE_COLD = "hbm:cold";
    private static final String TYPE_ANY_OF = "hbm:any_of";
    private static final String TYPE_FLUID_ITEM = "hbm:fluid_item";

    private static final Codec<FluidContainerStack> FLUID_CONTAINER = typed(TYPE_FLUID_CONTAINER,
            RecordCodecBuilder.<FluidContainerStack>mapCodec(i -> i.group(
                    FLUID.fieldOf("fluid").forGetter(stack -> Fluids.fromName(stack.fluid())),
                    Codec.INT.fieldOf("amount").forGetter(FluidContainerStack::amount),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(stack -> stack.stacksize)
            ).apply(i, (fluid, amount, count) -> {
                FluidContainerStack stack = new FluidContainerStack(fluid.getName(), amount);
                stack.stacksize = count;
                return stack;
            })));

    private static final Codec<RecipeIngredients.Cold> COLD = typed(TYPE_COLD,
            RecordCodecBuilder.<RecipeIngredients.Cold>mapCodec(i -> i.group(
                    ItemStacks.itemCodec().fieldOf("item").forGetter(stack -> stack.item),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(stack -> stack.stacksize)
            ).apply(i, RecipeIngredients.Cold::new)));

    /** {@link #STACK}, for the ingredients nested in {@code any_of}. */
    private static final Codec<AStack> NESTED = new Codec<>() {
        @Override
        public <T> DataResult<Pair<AStack, T>> decode(DynamicOps<T> ops, T input) {
            return STACK.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(AStack input, DynamicOps<T> ops, T prefix) {
            return STACK.encode(input, ops, prefix);
        }
    };

    private static final Codec<RecipeIngredients.AnyOf> ANY_OF = typed(TYPE_ANY_OF,
            RecordCodecBuilder.<RecipeIngredients.AnyOf>mapCodec(i -> i.group(
                    NESTED.listOf().fieldOf("of").forGetter(stack -> stack.choices),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(stack -> stack.stacksize)
            ).apply(i, RecipeIngredients.AnyOf::new)));

    private static final Codec<RecipeIngredients.FluidItem> FLUID_ITEM = typed(TYPE_FLUID_ITEM,
            RecordCodecBuilder.<RecipeIngredients.FluidItem>mapCodec(i -> i.group(
                    ItemStacks.itemCodec().fieldOf("item").forGetter(stack -> stack.item),
                    FLUID.fieldOf("fluid").forGetter(stack -> stack.fluid),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(stack -> stack.stacksize)
            ).apply(i, RecipeIngredients.FluidItem::new)));

    /**
     * Whether {@code stack} matches only its item with exactly some data (1.7 {@code new ComparableStack(ItemStack)}),
     * and that data; empty when any data of the item matches. A plain {@link ComparableStack} keeps its match stack
     * private, so this asks it: a stack of the item carrying data no recipe uses is refused only by an exact match.
     */
    static Optional<ItemData> exactData(ComparableStack stack) {
        if (stack instanceof RecipeIngredients.Exact exact) return Optional.of(exact.data);
        ItemStack probe = new ItemStack(stack.item);
        ModDataComponents.HAZARD_RADIATION.set(probe, -12_345F);
        if (stack.matchesRecipe(probe, true)) return Optional.empty();
        DataResult<ItemData> data = ItemData.of(stack.getDisplayStack());
        return Optional.of(data.result().orElseThrow(() -> new IllegalArgumentException(
                data.error().map(error -> error.message()).orElse("exact ingredient " + stack.item))));
    }

    private static DataResult<Input> input(AStack stack) {
        if (stack instanceof TagStack tag) return DataResult.success(new Input(Either.left(tag.tag), tag.stacksize, false, Optional.empty()));
        if (stack instanceof ComparableStack comparable) {
            Optional<ItemData> exact;
            try {
                exact = exactData(comparable);
            } catch (IllegalArgumentException e) {
                return DataResult.error(e::getMessage);
            }
            return DataResult.success(new Input(Either.right(comparable.item), comparable.stacksize, exact.isPresent(),
                    exact.filter(data -> !data.isEmpty())));
        }
        return DataResult.error(() -> "ingredient kind is not in the recipe schema: " + stack.getClass().getName());
    }

    private static DataResult<AStack> fromInput(Input input) {
        return input.ingredient().map(
                tag -> input.exact() || input.data().isPresent()
                        ? DataResult.error(() -> "a tag ingredient cannot be exact: #" + tag.location())
                        : DataResult.success((AStack) new TagStack(tag, input.count())),
                item -> DataResult.success(input.exact() || input.data().isPresent()
                        ? new RecipeIngredients.Exact(item, input.count(), input.data().orElse(ItemData.NONE))
                        : new ComparableStack(item, input.count())));
    }

    /**
     * An ingredient: {@code {"ingredient": id or #tag, "count", "exact", "item_data"}}, {@code {"material", "shape",
     * "count"}}, or a typed kind (see the class comment).
     */
    public static final Codec<AStack> STACK = new Codec<>() {
        @Override
        public <T> DataResult<Pair<AStack, T>> decode(DynamicOps<T> ops, T input) {
            Optional<String> type = ops.get(input, "type").flatMap(ops::getStringValue).result();
            if (type.isPresent()) {
                return switch (type.get()) {
                    case TYPE_FLUID_CONTAINER -> FLUID_CONTAINER.decode(ops, input).map(p -> p.mapFirst(s -> (AStack) s));
                    case TYPE_COLD -> COLD.decode(ops, input).map(p -> p.mapFirst(s -> (AStack) s));
                    case TYPE_ANY_OF -> ANY_OF.decode(ops, input).map(p -> p.mapFirst(s -> (AStack) s));
                    case TYPE_FLUID_ITEM -> FLUID_ITEM.decode(ops, input).map(p -> p.mapFirst(s -> (AStack) s));
                    default -> DataResult.error(() -> "unknown ingredient type " + type.get());
                };
            }
            if (ops.get(input, "material").result().isPresent()) {
                return MATERIAL_INPUT.decode(ops, input).map(p -> p.mapFirst(material -> (AStack)
                        new RecipesCommon.MaterialStack(material.material(), material.shape(), material.count())));
            }
            return INPUT.decode(ops, input).flatMap(p -> fromInput(p.getFirst()).map(stack -> Pair.of(stack, p.getSecond())));
        }

        @Override
        public <T> DataResult<T> encode(AStack stack, DynamicOps<T> ops, T prefix) {
            if (stack instanceof RecipesCommon.MaterialStack material) {
                return MATERIAL_INPUT.encode(new MaterialInput(material.material, material.shape, material.stacksize), ops, prefix);
            }
            if (stack instanceof FluidContainerStack container) return FLUID_CONTAINER.encode(container, ops, prefix);
            if (stack instanceof RecipeIngredients.Cold cold) return COLD.encode(cold, ops, prefix);
            if (stack instanceof RecipeIngredients.AnyOf any) return ANY_OF.encode(any, ops, prefix);
            if (stack instanceof RecipeIngredients.FluidItem fluid) return FLUID_ITEM.encode(fluid, ops, prefix);
            return input(stack).flatMap(input -> INPUT.encode(input, ops, prefix));
        }
    };

    // Outputs

    /**
     * An item output held as item + count until it is needed: 26.1 decodes recipes before item components exist, so a
     * row may not build an {@code ItemStack} while it is read. JSON: {@code "id"} or {@code {"id", "count"}}.
     */
    public record ItemOut(Item item, int count) {

        public ItemStack stack() {
            return new ItemStack(item, count);
        }

        public static ItemOut of(ItemStack stack) {
            if (!ItemStacks.sameItemAndData(stack, new ItemStack(stack.getItem(), stack.getCount()))) {
                throw new IllegalArgumentException("item data is not in the recipe schema yet: " + stack);
            }
            return new ItemOut(stack.getItem(), stack.getCount());
        }
    }

    /** {@code "id"} for a single item, else {@code {"id", "count"}}. */
    public static final Codec<ItemOut> ITEM_OUT = Codec.either(ItemStacks.itemCodec(),
            RecordCodecBuilder.<ItemOut>create(i -> i.group(
                    ItemStacks.itemCodec().fieldOf("id").forGetter(ItemOut::item),
                    Codec.INT.optionalFieldOf("count", 1).forGetter(ItemOut::count)
            ).apply(i, ItemOut::new))).xmap(
            either -> either.map(item -> new ItemOut(item, 1), out -> out),
            out -> out.count() == 1 ? Either.left(out.item()) : Either.right(out));

    /** One output; with {@code tag}, the first item of that tag ({@code spec} then holds only the count). */
    private record Output(ItemSpec spec, Optional<Float> chance, Optional<TagKey<Item>> tag) {}

    private static final Codec<Output> OUTPUT_OBJECT = RecordCodecBuilder.create(i -> i.group(
            ItemSpec.MAP_CODEC.forGetter(Output::spec),
            Codec.FLOAT.optionalFieldOf("chance").forGetter(Output::chance)
    ).apply(i, (spec, chance) -> new Output(spec, chance, Optional.empty())));

    /** 1.7's {@code OreDictionary.getOres(name).get(0)} as an output: {@code {"tag", "count", "chance"}}. */
    private static final Codec<Output> TAG_OUTPUT = RecordCodecBuilder.create(i -> i.group(
            TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(output -> output.tag().orElseThrow()),
            Codec.INT.optionalFieldOf("count", 1).forGetter(output -> output.spec().count()),
            Codec.FLOAT.optionalFieldOf("chance").forGetter(Output::chance)
    ).apply(i, (tag, count, chance) -> new Output(new ItemSpec(Items.AIR, count, ItemData.NONE),
            chance, Optional.of(tag))));

    /** {@code "id"} for one plain item, else {@code {"id", "count", "chance", "item_data"}} or {@code {"tag", "count"}}. */
    private static final Codec<Output> OUTPUT = Codec.either(ItemStacks.itemCodec(),
            Codec.either(TAG_OUTPUT, OUTPUT_OBJECT)).xmap(
            either -> either.map(item -> new Output(new ItemSpec(item, 1, ItemData.NONE), Optional.empty(), Optional.empty()),
                    object -> object.map(output -> output, output -> output)),
            output -> output.tag().isPresent() ? Either.right(Either.left(output))
                    : output.spec().count() == 1 && output.spec().data().isEmpty() && output.spec().patch().isEmpty()
                    && output.chance().isEmpty()
                    ? Either.left(output.spec().item()) : Either.right(Either.right(output)));

    /** A weight, written as an integer when it is one (1.7's weights are). */
    private static final Codec<Float> WEIGHT = new Codec<>() {
        @Override
        public <T> DataResult<Pair<Float, T>> decode(DynamicOps<T> ops, T input) {
            return Codec.FLOAT.decode(ops, input);
        }

        @Override
        public <T> DataResult<T> encode(Float weight, DynamicOps<T> ops, T prefix) {
            return weight == Math.rint(weight) && Math.abs(weight) < 1.0E9F
                    ? Codec.INT.encode(weight.intValue(), ops, prefix) : Codec.FLOAT.encode(weight, ops, prefix);
        }
    };

    private record Choice(ItemSpec spec, float weight) {}

    /** One entry of a weighted output, ntm-next's (vanilla weighted-list) shape: {@code {"data": output, "weight": n}}. */
    private static final Codec<Choice> CHOICE = RecordCodecBuilder.create(i -> i.group(
            ItemSpec.CODEC.fieldOf("data").forGetter(Choice::spec),
            WEIGHT.fieldOf("weight").forGetter(Choice::weight)
    ).apply(i, Choice::new));

    private record Outputs(List<Output> items, List<Choice> choices) {

        static Outputs of(GenericRecipe recipe) {
            if (recipe.pendingChoices != null) {
                List<Choice> choices = new ArrayList<>();
                for (int index = 0; index < recipe.pendingChoices.length; index++) {
                    choices.add(new Choice(recipe.pendingChoices[index], recipe.pendingChoiceWeights[index]));
                }
                return new Outputs(List.of(), choices);
            }
            if (recipe.outputChoices != null) {
                List<Choice> choices = new ArrayList<>();
                for (GenericRecipe.OutputChoice choice : recipe.outputChoices) {
                    choices.add(new Choice(spec(recipe, choice.stack()), choice.weight()));
                }
                return new Outputs(List.of(), choices);
            }
            List<Output> items = new ArrayList<>();
            if (recipe.pendingOutputs != null) {
                for (int index = 0; index < recipe.pendingOutputs.length; index++) {
                    items.add(new Output(recipe.pendingOutputs[index], chance(recipe, index), outputTag(recipe, index)));
                }
            } else if (recipe.outputItem != null) {
                for (int index = 0; index < recipe.outputItem.length; index++) {
                    Optional<TagKey<Item>> tag = outputTag(recipe, index);
                    ItemSpec spec = tag.isPresent()
                            ? new ItemSpec(Items.AIR, recipe.outputTagCount(index), ItemData.NONE)
                            : spec(recipe, recipe.outputItem[index]);
                    items.add(new Output(spec, chance(recipe, index), tag));
                }
            }
            return new Outputs(items, List.of());
        }

        private static Optional<TagKey<Item>> outputTag(GenericRecipe recipe, int index) {
            return recipe.outputTags != null && index < recipe.outputTags.length
                    ? Optional.ofNullable(recipe.outputTags[index]) : Optional.empty();
        }

        private static Optional<Float> chance(GenericRecipe recipe, int index) {
            return recipe.outputChance != null && index < recipe.outputChance.length
                    ? Optional.of(recipe.outputChance[index]) : Optional.empty();
        }
    }

    private static ItemSpec spec(GenericRecipe recipe, ItemStack stack) {
        DataResult<ItemSpec> spec = ItemSpec.of(stack);
        return spec.result().orElseThrow(() -> new IllegalArgumentException(recipe.getInternalName() + ": "
                + spec.error().map(error -> error.message()).orElse(String.valueOf(stack))));
    }

    /** {@code output_items}: plain outputs, or one weighted choice as the only entry. */
    private static final Codec<Outputs> OUTPUTS = Codec.either(OUTPUT, CHOICE.listOf()).listOf().flatXmap(
            slots -> {
                List<Output> items = new ArrayList<>();
                List<Choice> choices = new ArrayList<>();
                for (Either<Output, List<Choice>> slot : slots) {
                    slot.ifLeft(items::add);
                    slot.ifRight(choices::addAll);
                }
                if (!choices.isEmpty() && slots.size() != 1) {
                    return DataResult.error(() -> "a weighted output must be the only entry of output_items");
                }
                return DataResult.success(new Outputs(items, choices));
            },
            outputs -> {
                List<Either<Output, List<Choice>>> slots = new ArrayList<>();
                if (!outputs.choices().isEmpty()) slots.add(Either.right(outputs.choices()));
                for (Output output : outputs.items()) slots.add(Either.left(output));
                return DataResult.success(slots);
            });

    private static Optional<ItemSpec> icon(GenericRecipe recipe) {
        if (recipe.pendingIcon != null) return Optional.of(recipe.pendingIcon);
        ItemStack icon = recipe.iconOverride();
        return icon.isEmpty() ? Optional.empty() : Optional.of(spec(recipe, icon));
    }

    private static <T> List<T> list(T[] array) {
        return array == null ? List.of() : Arrays.asList(array);
    }

    private static Optional<List<String>> names(String[] array) {
        return Optional.ofNullable(array).map(Arrays::asList);
    }

    // Rows

    /** The config-mode variants and row conditions (separate from the base fields: a record codec holds 16). */
    private record Modes(Optional<List<AStack>> itemsEx, Optional<List<FluidStack>> fluidsEx,
                         Optional<List<String>> pools528, Optional<List<String>> requires) {

        static Modes of(GenericRecipe recipe) {
            return new Modes(Optional.ofNullable(recipe.inputItemEx).map(Arrays::asList),
                    Optional.ofNullable(recipe.inputFluidEx).map(Arrays::asList),
                    names(recipe.blueprintPools528), names(recipe.requires));
        }

        void applyTo(GenericRecipe recipe) {
            recipe.inputItemEx = itemsEx.map(list -> list.toArray(AStack[]::new)).orElse(null);
            recipe.inputFluidEx = fluidsEx.map(list -> list.toArray(FluidStack[]::new)).orElse(null);
            recipe.blueprintPools528 = pools528.map(list -> list.toArray(String[]::new)).orElse(null);
            recipe.requires = requires.map(list -> list.toArray(String[]::new)).orElse(null);
        }
    }

    private static final MapCodec<Modes> MODES = RecordCodecBuilder.mapCodec(i -> i.group(
            STACK.listOf().optionalFieldOf("input_items_expensive").forGetter(Modes::itemsEx),
            FLUID_STACK.listOf().optionalFieldOf("input_fluids_expensive").forGetter(Modes::fluidsEx),
            Codec.STRING.listOf().optionalFieldOf("blueprint_pools_528").forGetter(Modes::pools528),
            Codec.STRING.listOf().optionalFieldOf("requires").forGetter(Modes::requires)
    ).apply(i, Modes::new));

    private static <T extends GenericRecipe> MapCodec<T> base(Function<String, T> factory) {
        return RecordCodecBuilder.<T>mapCodec(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(GenericRecipe::getInternalName),
                STACK.listOf().optionalFieldOf("input_items", List.of()).forGetter(r -> list(r.normalInputItems())),
                FLUID_STACK.listOf().optionalFieldOf("input_fluids", List.of()).forGetter(r -> list(r.normalInputFluids())),
                OUTPUTS.optionalFieldOf("output_items", new Outputs(List.of(), List.of())).forGetter(Outputs::of),
                FLUID_STACK.listOf().optionalFieldOf("output_fluids", List.of()).forGetter(r -> list(r.outputFluid)),
                Codec.INT.optionalFieldOf("duration", 0).forGetter(r -> r.duration),
                Codec.LONG.optionalFieldOf("power", 0L).forGetter(r -> r.power),
                ItemSpec.MAP_CODEC.codec().optionalFieldOf("icon").forGetter(GenericRecipeCodec::icon),
                Codec.BOOL.optionalFieldOf("custom_localization", false).forGetter(r -> r.customLocalization),
                Codec.STRING.optionalFieldOf("name_wrapper").forGetter(r -> Optional.ofNullable(r.nameWrapper)),
                Codec.STRING.listOf().optionalFieldOf("blueprint_pools").forGetter(r -> names(r.normalPools())),
                Codec.STRING.optionalFieldOf("auto_switch_group").forGetter(r -> Optional.ofNullable(r.autoSwitchGroup)),
                Codec.INT.optionalFieldOf("order", Integer.MAX_VALUE).forGetter(r -> r.order)
        ).apply(i, (name, inputItems, inputFluids, outputs, outputFluids, duration, power, icon, named, wrapper, pools,
                    group, order) -> {
            T recipe = factory.apply(name);
            recipe.setup(duration, power);
            if (!inputItems.isEmpty()) recipe.inputItem = inputItems.toArray(AStack[]::new);
            if (!inputFluids.isEmpty()) recipe.inputFluid = inputFluids.toArray(FluidStack[]::new);
            // stacks are built by GenericRecipe.bindStacks once item components exist (see pendingOutputs)
            if (!outputs.choices().isEmpty()) {
                recipe.pendingChoices = outputs.choices().stream().map(Choice::spec).toArray(ItemSpec[]::new);
                recipe.pendingChoiceWeights = new float[outputs.choices().size()];
                for (int index = 0; index < outputs.choices().size(); index++) {
                    recipe.pendingChoiceWeights[index] = outputs.choices().get(index).weight();
                }
            } else if (!outputs.items().isEmpty()) {
                List<Output> items = outputs.items();
                recipe.pendingOutputs = items.stream().map(Output::spec).toArray(ItemSpec[]::new);
                if (items.stream().anyMatch(o -> o.tag().isPresent())) {
                    @SuppressWarnings("unchecked")
                    TagKey<Item>[] tags = new TagKey[items.size()];
                    for (int index = 0; index < items.size(); index++) tags[index] = items.get(index).tag().orElse(null);
                    recipe.outputTags = tags;
                }
                if (items.stream().anyMatch(o -> o.chance().isPresent())) {
                    recipe.outputChance = new float[items.size()];
                    for (int index = 0; index < items.size(); index++) recipe.outputChance[index] = items.get(index).chance().orElse(1F);
                }
            }
            if (!outputFluids.isEmpty()) recipe.outputFluid = outputFluids.toArray(FluidStack[]::new);
            icon.ifPresent(spec -> recipe.pendingIcon = spec);
            recipe.customLocalization = named;
            recipe.nameWrapper = wrapper.orElse(null);
            // pools and groups are joined by the table when it (re)builds, not while a file is parsed
            recipe.blueprintPools = pools.map(p -> p.toArray(String[]::new)).orElse(null);
            recipe.autoSwitchGroup = group.orElse(null);
            recipe.order = order;
            return recipe;
        }));
    }

    // Script fields

    /**
     * This schema's fields for scripts ({@link RecipeFields}; Golem Core's KubeJS adapter makes
     * {@code event.recipes.<ns>.<table>(name, outputItems, inputItems, outputFluids, inputFluids, duration, power)} of
     * them, every other key a builder method). {@code name} is required, as in a datapack file: machines save it
     * and recipe selectors show it. Every {@link DataGenericRecipes} table states these; a subclass's own keys
     * (fusion temperatures, ...) come on top
     * ({@link DataGenericRecipes#DataGenericRecipes(String, String, java.util.function.Supplier, RecipeFields)}) or
     * are offered as raw JSON. Shapes: {@link RecipeShapes}.
     */
    public static final RecipeFields FIELDS = RecipeFields.of()
            .add(field("name", Kind.STRING))
            .add(field("output_items", Kind.ITEM_OUT).list().optional().shape(RecipeShapes.OUTPUT).alias("outputItems"))
            .add(field("input_items", Kind.ITEM_IN).list().optional().shape(RecipeShapes.INGREDIENT).alias("inputItems"))
            .add(field("output_fluids", Kind.FLUID_OUT).list().optional().shape(RecipeShapes.FLUID_STACK).alias("outputFluids"))
            .add(field("input_fluids", Kind.FLUID_IN).list().optional().shape(RecipeShapes.FLUID_STACK).alias("inputFluids"))
            .add(field("duration", Kind.INT).optional())
            .add(field("power", Kind.LONG).optional())
            .add(field("icon", Kind.ITEM_OUT).role(Role.OTHER).optional().shape(RecipeShapes.STACK_OUT_OBJECT))
            .add(field("custom_localization", Kind.BOOLEAN).optional().alias("customLocalization"))
            .add(field("name_wrapper", Kind.STRING).optional().alias("nameWrapper"))
            .add(field("blueprint_pools", Kind.STRING).list().optional().alias("blueprintPools"))
            .add(field("auto_switch_group", Kind.STRING).optional().alias("autoSwitchGroup"))
            .add(field("order", Kind.INT).optional())
            .add(field("input_items_expensive", Kind.ITEM_IN).list().optional().shape(RecipeShapes.INGREDIENT)
                    .alias("inputItemsExpensive"))
            .add(field("input_fluids_expensive", Kind.FLUID_IN).list().optional().shape(RecipeShapes.FLUID_STACK)
                    .alias("inputFluidsExpensive"))
            .add(field("blueprint_pools_528", Kind.STRING).list().optional().alias("blueprintPools528"))
            .add(field("requires", Kind.STRING).list().optional());

    /** The row codec of a {@link GenericRecipe} table. */
    public static MapCodec<GenericRecipe> codec() {
        return codec(GenericRecipe::new);
    }

    /** The row codec of a table whose rows are {@code T} with no fields of their own. */
    public static <T extends GenericRecipe> MapCodec<T> codec(Function<String, T> factory) {
        return RecordCodecBuilder.<T>mapCodec(i -> i.group(
                base(factory).forGetter(r -> r),
                MODES.forGetter(Modes::of)
        ).apply(i, (recipe, modes) -> {
            modes.applyTo(recipe);
            return recipe;
        }));
    }

    /**
     * The row codec of a table whose rows add fields: {@code extras} states them in the same JSON object (ntm-next's
     * {@code GenericRecipe.mapCodec(factory, extras, apply, extract)}).
     */
    public static <T extends GenericRecipe, E> MapCodec<T> codec(Function<String, T> factory, MapCodec<E> extras,
                                                                 Function<T, E> extract, BiConsumer<T, E> apply) {
        return RecordCodecBuilder.<T>mapCodec(i -> i.group(
                codec(factory).forGetter(r -> r),
                extras.forGetter(extract)
        ).apply(i, (recipe, extra) -> {
            apply.accept(recipe, extra);
            return recipe;
        }));
    }

    // Comparison

    /** One ingredient as text: id, {@code #tag}, {@code mat:}, {@code !exact} with its data, or its typed kind. */
    public static String describe(AStack stack) {
        String body;
        if (stack instanceof TagStack tag) body = "#" + tag.tag.location();
        else if (stack instanceof RecipesCommon.MaterialStack material) body = "mat:" + material.material.name + "/" + material.shape;
        else if (stack instanceof ComparableStack comparable) {
            Optional<ItemData> exact = exactData(comparable);
            body = BuiltInRegistries.ITEM.getKey(comparable.item) + exact.map(data -> "!exact" + data.describe()).orElse("");
        } else if (stack instanceof FluidContainerStack container) body = "fc:" + Fluids.fromName(container.fluid()).getKey() + "/" + container.amount();
        else if (stack instanceof RecipeIngredients.Cold cold) body = "cold:" + BuiltInRegistries.ITEM.getKey(cold.item);
        else if (stack instanceof RecipeIngredients.FluidItem fluid) body = "fi:" + BuiltInRegistries.ITEM.getKey(fluid.item) + "/" + fluid.fluid.getKey();
        else if (stack instanceof RecipeIngredients.AnyOf any) {
            List<String> parts = new ArrayList<>();
            for (AStack choice : any.choices) parts.add(describe(choice));
            body = "any(" + String.join("|", parts) + ")";
        } else body = "?" + stack.getClass().getName();
        return body + "x" + stack.stacksize;
    }

    private static String stacks(AStack[] stacks) {
        StringBuilder out = new StringBuilder();
        for (AStack stack : list(stacks)) out.append(describe(stack)).append(',');
        return out.toString();
    }

    private static String fluids(FluidStack[] fluids) {
        StringBuilder out = new StringBuilder();
        for (FluidStack fluid : list(fluids)) out.append(fluid.type.getKey()).append('x').append(fluid.fill).append('@').append(fluid.pressure).append(',');
        return out.toString();
    }

    private static String stack(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "x" + stack.getCount()
                + ItemData.of(stack).result().map(ItemData::describe).orElse("{?" + stack + "}");
    }

    /**
     * Every field of {@code recipe} as one line, for comparing a converted table with its source: equal lines mean the
     * JSON row restores the recipe exactly (ingredients with their kind and exactness, counts, fluids, chances, weighted
     * choices, item data, pools, groups, names, the config-mode variants, row conditions and subclass fields; the
     * effective inputs and pools are those of the config in force; order excluded).
     */
    public static String describe(GenericRecipe recipe) {
        StringBuilder out = new StringBuilder(recipe.getInternalName());
        out.append(" d=").append(recipe.duration).append(" p=").append(recipe.power);
        out.append(" in=").append(stacks(recipe.inputItem));
        out.append(" fin=").append(fluids(recipe.inputFluid));
        out.append(" out=");
        if (recipe.outputItem != null) for (int index = 0; index < recipe.outputItem.length; index++) {
            out.append(stack(recipe.outputItem[index])).append('%').append(recipe.getChance(index)).append(',');
        }
        out.append(" fout=").append(fluids(recipe.outputFluid));
        out.append(" icon=").append(recipe.iconOverride().isEmpty() ? "-" : stack(recipe.iconOverride()));
        out.append(" named=").append(recipe.customLocalization).append(" wrap=").append(recipe.nameWrapper);
        out.append(" pools=").append(recipe.blueprintPools == null ? "-" : String.join("|", recipe.blueprintPools));
        out.append(" group=").append(recipe.autoSwitchGroup);
        if (recipe.outputChoices != null) {
            out.append(" choices=");
            for (GenericRecipe.OutputChoice choice : recipe.outputChoices) out.append(stack(choice.stack())).append('w').append(choice.weight()).append(',');
        }
        if (recipe.inputItemEx != null) out.append(" ex=").append(stacks(recipe.inputItemEx));
        if (recipe.inputFluidEx != null) out.append(" fex=").append(fluids(recipe.inputFluidEx));
        if (recipe.blueprintPools528 != null) out.append(" pools528=").append(String.join("|", recipe.blueprintPools528));
        if (recipe.requires != null) out.append(" requires=").append(String.join("|", recipe.requires));
        String extra = recipe.describeExtra();
        if (!extra.isEmpty()) out.append(' ').append(extra);
        return out.toString();
    }
}
