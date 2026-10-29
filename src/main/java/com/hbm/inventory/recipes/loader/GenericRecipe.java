package com.hbm.inventory.recipes.loader;

import dev.golem.core.Ids;
import dev.golem.core.gui.TextSearch;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.Registry;
import com.hbm.config.GeneralConfig;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.items.machine.ItemFluidIcon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One machine recipe, ported from HBM's {@code GenericRecipe}. Inputs are {@link AStack}s
 * (exact item or tag); outputs are plain {@link ItemStack}s with optional per-item chances, alongside
 * typed fluid inputs and outputs. A recipe may belong to one or more
 * <b>blueprint pools</b> (which template unlocks it) and an <b>auto-switch group</b> (machines pick the
 * matching recipe in the group from the inserted ingredient).
 */
public class GenericRecipe {

    public final String name;
    /** The datapack entry that supplied this row; synthesized rules have no entry. */
    private ResourceKey<Recipe<?>> recipeId;
    public AStack[] inputItem;
    public ItemStack[] outputItem;
    public FluidStack[] inputFluid;
    public FluidStack[] outputFluid;
    /**
     * Per-output drop chance, index-aligned with {@link #outputItem}; a missing entry means guaranteed.
     * An explicit {@code 0} means the output never drops, matching the original {@code ChanceOutput}. A parallel
     * array keeps every {@code outputItems(ItemStack...)} call site untouched. As in the original, the roll
     * happens per <i>item</i> in the stack, so a 50% ×4 output averages two items.
     */
    public float[] outputChance;
    /**
     * Index-aligned with {@link #outputItem}: an output that is the first item of a tag when the table rebuilds (1.7's
     * {@code OreDictionary.getOres(name).get(0)} copied into a recipe), null for a plain output. Set with
     * {@link #outputFirstOf}; the row should also {@link #requires} the tag, as 1.7 registered it only when the tag had one.
     */
    public TagKey<Item>[] outputTags;
    /** The counts of the tag outputs, index-aligned with {@link #outputTags} (their stacks are empty while a tag is). */
    private int[] outputTagCounts;
    /** Mutually exclusive weighted outputs for one result slot, matching ChanceOutputMulti. */
    public OutputChoice[] outputChoices;
    private OutputSelector outputSelector;
    public int duration;
    public long power;
    public String autoSwitchGroup = null;
    /** The pools in effect: {@link #blueprintPools528} while 528 mode is on and the row has them, else the normal ones. */
    public String[] blueprintPools = null;
    /** 1.7 {@code inputItemsEx}: the inputs while {@code enableExpensiveMode} is on (null: the normal ones apply). */
    public AStack[] inputItemEx = null;
    /** 1.7 {@code inputFluidsEx}: the fluid inputs while {@code enableExpensiveMode} is on. */
    public FluidStack[] inputFluidEx = null;
    /** 1.7 {@code setPools528}: the pools that replace {@link #blueprintPools} while {@code enable528} is on. */
    public String[] blueprintPools528 = null;
    /**
     * The flags a datapack row needs to be part of its table ({@code "requires"}, {@link RecipeConditions}): 1.7's config
     * branches at registration ({@code if(!GeneralConfig.enable528)}, the LBSM ternaries, the PUREX naquadria gate), re-tested
     * every time the table rebuilds. Null: always present.
     */
    public String[] requires = null;
    /** The normal inputs and pools, kept once a mode's variants were first swapped in. */
    private AStack[] inputItemNormal;
    private FluidStack[] inputFluidNormal;
    private String[] blueprintPoolsNormal;
    private boolean normalKept;
    /** Translation key wrapping the icon's name for display, e.g. {@code purex.recycle} = "Reprocessing of %s". */
    public String nameWrapper = null;
    /**
     * 1.7 {@code customLocalization}: the recipe is displayed under its own name, the translation of its
     * internal name ({@code ass.nitra} = "Nitra to Ammunition"), instead of its icon's.
     */
    public boolean customLocalization = false;
    private ItemStack icon = ItemStack.EMPTY;
    /** Display order in a machine's recipe list (ntm-next's {@code order}); ties sort by recipe id. */
    public int order = Integer.MAX_VALUE;

    public GenericRecipe(String name) {
        this.name = name;
    }

    private static final ResourceKey<Registry<Recipe<?>>> RECIPE_REGISTRY =
            ResourceKey.createRegistryKey(Ids.minecraft("recipe"));

    public void bindRecipeId(String id) {
        recipeId = ResourceKey.create(RECIPE_REGISTRY, Ids.parse(id));
    }

    public ResourceKey<Recipe<?>> recipeId() { return recipeId; }

    public String[] getPools() { return blueprintPools; }

    public GenericRecipe setup(int duration, long power) {
        this.duration = duration;
        this.power = power;
        return this;
    }

    /**
     * The row's {@link #requires} flags: where 1.7 registered a recipe only under a config branch
     * ({@code if(!GeneralConfig.enable528)}), the data row is always written and carries the branch as a flag.
     */
    public GenericRecipe requires(String... flags) {
        this.requires = flags;
        return this;
    }

    /** 1.7 {@code setDuration}: the processing time in ticks. */
    public GenericRecipe setDuration(int duration) {
        this.duration = duration;
        return this;
    }

    /** 1.7 {@code setPower}: the power use per tick. */
    public GenericRecipe setPower(long power) {
        this.power = power;
        return this;
    }

    /** 1.7 {@code setupNamed}: {@link #setup} plus {@link #setNamed}. */
    public GenericRecipe setupNamed(int duration, long power) {
        return setup(duration, power).setNamed();
    }

    /** 1.7 {@code setNamed}: display the translation of the internal name (lang {@code ass.*}, {@code chem.*}, ...). */
    public GenericRecipe setNamed() {
        this.customLocalization = true;
        return this;
    }

    public GenericRecipe inputItems(AStack... input) {
        this.inputItem = input;
        return this;
    }

    public GenericRecipe outputItems(ItemStack... output) {
        this.outputItem = output;
        this.outputChoices = null;
        this.outputSelector = null;
        return this;
    }

    /** Sets one result slot whose output is selected by weight when the recipe completes. */
    public GenericRecipe outputChoices(OutputChoice... choices) {
        this.outputChoices = choices;
        this.outputSelector = new FloatOutputs(choices);
        this.outputItem = choices.length == 0 ? null : new ItemStack[]{choices[0].stack().copy()};
        return this;
    }

    public record OutputChoice(ItemStack stack, float weight) { }

    /**
     * Makes output {@code index} (set by {@link #outputItems}; any placeholder there) {@code count} of the first item of
     * {@code tag}, resolved whenever the table rebuilds.
     */
    @SuppressWarnings("unchecked")
    public GenericRecipe outputFirstOf(int index, TagKey<Item> tag, int count) {
        if (outputTags == null) {
            outputTags = new TagKey[outputItem.length];
            outputTagCounts = new int[outputItem.length];
        }
        outputTags[index] = tag;
        outputTagCounts[index] = count;
        outputItem[index] = firstOf(tag, count);
        return this;
    }

    /** The count of output {@code index}: a tag output's even while its tag is empty. */
    public int outputTagCount(int index) {
        if (outputTagCounts != null && index < outputTagCounts.length && outputTags[index] != null) return outputTagCounts[index];
        return outputItem[index].getCount();
    }

    private static ItemStack firstOf(TagKey<Item> tag, int count) {
        for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) return new ItemStack(item.value(), count);
        return ItemStack.EMPTY;
    }

    /** Drop chances (0..1) for the outputs set by {@link #outputItems}, in the same order. */
    public GenericRecipe outputChances(float... chances) {
        this.outputChance = chances;
        return this;
    }

    public GenericRecipe inputFluids(FluidStack... input) {
        this.inputFluid = input;
        return this;
    }

    public GenericRecipe outputFluids(FluidStack... output) {
        this.outputFluid = output;
        return this;
    }

    /** The recipe's blueprint pools; the table joins them when the recipe is registered, never while it is built. */
    public GenericRecipe setPools(String... pools) {
        this.blueprintPools = pools;
        return this;
    }

    /**
     * 1.7 {@code inputItemsEx}: the inputs while {@code enableExpensiveMode} is on (off by default). Always recorded, so a
     * table can be written out with both variants; put in effect now when the mode is on, as 1.7 did.
     */
    public GenericRecipe inputItemsEx(AStack... input) {
        this.inputItemEx = input;
        if (GeneralConfig.enableExpensiveMode) {
            keepNormal();
            this.inputItem = input;
        }
        return this;
    }

    /** 1.7 {@code inputFluidsEx}: the fluid counterpart of {@link #inputItemsEx}. */
    public GenericRecipe inputFluidsEx(FluidStack... input) {
        this.inputFluidEx = input;
        if (GeneralConfig.enableExpensiveMode) {
            keepNormal();
            this.inputFluid = input;
        }
        return this;
    }

    /** 1.7 {@code setPools528}: blueprint pools that replace the normal ones while {@code enable528} is on (off by default). */
    public GenericRecipe setPools528(String... pools) {
        this.blueprintPools528 = pools;
        if (GeneralConfig.enable528) {
            keepNormal();
            this.blueprintPools = pools;
        }
        return this;
    }

    /**
     * The recipe's auto-switch group; the table joins it when the recipe is registered. {@code set} is 1.7's table
     * argument, kept so 1.7's {@code setGroup(name, this)} reads unchanged; it is not used.
     */
    public GenericRecipe setGroup(String autoSwitch, Object set) {
        this.autoSwitchGroup = autoSwitch;
        return this;
    }

    private void keepNormal() {
        if (normalKept) return;
        inputItemNormal = inputItem;
        inputFluidNormal = inputFluid;
        blueprintPoolsNormal = blueprintPools;
        normalKept = true;
    }

    /** The inputs outside expensive mode (what a file's {@code input_items} holds). */
    public AStack[] normalInputItems() { return normalKept ? inputItemNormal : inputItem; }

    /** The fluid inputs outside expensive mode. */
    public FluidStack[] normalInputFluids() { return normalKept ? inputFluidNormal : inputFluid; }

    /** The blueprint pools outside 528 mode. */
    public String[] normalPools() { return normalKept ? blueprintPoolsNormal : blueprintPools; }

    /**
     * Puts the variants of the given config modes in effect: the expensive inputs ({@link #inputItemEx},
     * {@link #inputFluidEx}) and the 528 pools ({@link #blueprintPools528}) where the recipe has them, else the normal
     * ones. A data table calls it every time it rebuilds, so a changed config applies with the next rebuild.
     */
    public synchronized void applyModes(boolean expensive, boolean mode528) {
        keepNormal();
        this.inputItem = expensive && inputItemEx != null ? inputItemEx : inputItemNormal;
        this.inputFluid = expensive && inputFluidEx != null ? inputFluidEx : inputFluidNormal;
        this.blueprintPools = mode528 && blueprintPools528 != null ? blueprintPools528 : blueprintPoolsNormal;
    }

    /** Fields a subclass adds (fusion temperatures, plasma ignition...), for {@link GenericRecipeCodec#describe}. */
    public String describeExtra() {
        return "";
    }

    public GenericRecipe setIcon(ItemStack icon) {
        this.icon = icon;
        return this;
    }

    /** 1.7 {@code setIconToFirstIngredient}: the first stack the first item input shows. */
    public GenericRecipe setIconToFirstIngredient() {
        if (inputItem != null && inputItem.length > 0) {
            List<ItemStack> stacks = inputItem[0].getDisplayStacks();
            if (!stacks.isEmpty()) this.icon = stacks.get(0).copy();
        }
        return this;
    }

    public GenericRecipe setNameWrapper(String key) {
        this.nameWrapper = key;
        return this;
    }

    /** The chance of output {@code index} dropping, 1 when no chance table applies. */
    public float getChance(int index) {
        if (outputChance == null || index >= outputChance.length) return 1F;
        return Math.max(0F, Math.min(outputChance[index], 1F));
    }

    /** {@code stack} after output {@code index}'s chance, rolled once per item (1.7 {@code ChanceOutput.collapse}). */
    public ItemStack rollChance(int index, ItemStack stack, RandomSource random) {
        float chance = getChance(index);
        if (chance >= 1F) return stack;
        if (chance <= 0F) return ItemStack.EMPTY;
        int kept = 0;
        for (int j = 0; j < stack.getCount(); j++) if (random.nextFloat() <= chance) kept++;
        if (kept <= 0) return ItemStack.EMPTY;
        stack.setCount(kept);
        return stack;
    }

    /** Checks every possible result without consuming random state. */
    public boolean canFitOutput(int index, ItemStack slot, int slotLimit) {
        if (outputChoices != null && index == 0) {
            for (var choice : outputChoices) {
                if (choice.weight() > 0F && !outputFits(slot, choice.stack(), slotLimit)) return false;
            }
            return true;
        }
        return outputFits(slot, outputItem[index], slotLimit);
    }

    public static boolean outputFits(ItemStack slot, ItemStack made, int slotLimit) {
        if (made.isEmpty()) return true;
        var limit = Math.min(slotLimit, made.getMaxStackSize());
        return made.getCount() <= limit && (slot.isEmpty()
                || ItemStacks.sameItemAndData(slot, made)
                && slot.getCount() + made.getCount() <= Math.min(limit, slot.getMaxStackSize()));
    }

    private interface OutputSelector {
        ItemStack pick(RandomSource random);
    }

    /** Retains the existing module protocol and floating-point distribution. */
    private static final class FloatOutputs implements OutputSelector {
        private final OutputChoice[] choices;
        private final float total;

        private FloatOutputs(OutputChoice[] choices) {
            this.choices = choices;
            var sum = 0F;
            for (var choice : choices) sum += Math.max(0F, choice.weight());
            total = sum;
        }

        @Override public ItemStack pick(RandomSource random) {
            var pick = random.nextFloat() * total;
            for (var choice : choices) {
                pick -= Math.max(0F, choice.weight());
                if (pick <= 0F) return choice.stack();
            }
            return choices[0].stack();
        }
    }

    /** Integer weighted builders retain their exact weights and bounded-integer draw. */
    private static final class IntegerOutputs implements OutputSelector {
        private final ItemStack[] choices;
        private final int[] cumulative;
        private final int total;

        private IntegerOutputs(int[] weights, ItemStack[] choices) {
            this.choices = choices;
            cumulative = new int[choices.length];
            var sum = 0;
            for (var i = 0; i < choices.length; i++) {
                if (weights[i] < 0) throw new IllegalArgumentException("Negative output weight");
                cumulative[i] = sum = Math.addExact(sum, weights[i]);
            }
            total = sum;
        }

        @Override public ItemStack pick(RandomSource random) {
            if (total == 0) return ItemStack.EMPTY;
            var pick = random.nextInt(total);
            for (var i = 0; i < cumulative.length; i++) if (pick < cumulative[i]) return choices[i];
            throw new AssertionError("Weighted output selection exceeded its total");
        }
    }

    /** Explicit integer selector for builders whose weights are integers rather than continuous floats. */
    public GenericRecipe weightedOutputs(int[] weights, ItemStack... choices) {
        if (weights.length != choices.length) throw new IllegalArgumentException("Output and weight counts differ");
        outputChoices = new OutputChoice[choices.length];
        for (var i = 0; i < choices.length; i++) outputChoices[i] = new OutputChoice(choices[i], weights[i]);
        outputSelector = new IntegerOutputs(weights, choices);
        outputItem = new ItemStack[] {choices.length == 0 ? ItemStack.EMPTY : choices[0].copy()};
        return this;
    }

    /** Rolls a single committed result; display and space checks use the declarations directly. */
    public ItemStack rollOutput(int index, RandomSource random) {
        var made = outputItem[index].copy();
        if (outputChoices != null && index == 0) {
            made = outputSelector.pick(random).copy();
        }
        return rollChance(index, made, random);
    }

    public boolean isPooled() { return blueprintPools != null; }

    public boolean isPartOfPool(String lookingFor) {
        if (!isPooled() || lookingFor == null) return false;
        for (String pool : blueprintPools) if (pool.equals(lookingFor)) return true;
        return false;
    }

    public String getInternalName() { return name; }

    /** The icon set with {@link #setIcon}, or empty when the recipe shows its first output. */
    public ItemStack iconOverride() { return icon; }

    /**
     * Outputs, weighted choices and icon read from a datapack file, as item + count + {@link ItemData}: 26.1 decodes
     * recipes before item components are bound, when no {@link ItemStack} may be created yet. {@link #bindStacks} turns
     * them into stacks; the table does it when it (re)builds, after the reload.
     */
    ItemSpec[] pendingOutputs;
    ItemSpec[] pendingChoices;
    float[] pendingChoiceWeights;
    ItemSpec pendingIcon;

    /**
     * Builds the stacks of a recipe read from a datapack (see {@link #pendingOutputs}); a no-op otherwise. The specs are
     * kept, and the stacks are built again on every call: a stack's data can depend on data that loads later than the
     * recipes (a container's tint from {@code hbm:fluid_containers}, a tag output's first item).
     */
    public synchronized void bindStacks() {
        if (pendingOutputs != null) {
            ItemStack[] stacks = new ItemStack[pendingOutputs.length];
            if (outputTags != null) outputTagCounts = new int[stacks.length];
            for (int index = 0; index < stacks.length; index++) {
                boolean tagged = outputTags != null && outputTags[index] != null;
                if (tagged) outputTagCounts[index] = pendingOutputs[index].count();
                stacks[index] = tagged ? firstOf(outputTags[index], pendingOutputs[index].count()) : pendingOutputs[index].stack();
            }
            this.outputItem = stacks;
        }
        if (pendingChoices != null) {
            OutputChoice[] choices = new OutputChoice[pendingChoices.length];
            for (int index = 0; index < choices.length; index++) {
                choices[index] = new OutputChoice(pendingChoices[index].stack(), pendingChoiceWeights[index]);
            }
            outputChoices(choices);
        }
        if (pendingIcon != null) {
            this.icon = pendingIcon.stack();
        }
    }

    public ItemStack getIcon() {
        if (!icon.isEmpty()) return icon;
        if (outputItem != null && outputItem.length > 0) return outputItem[0];
        // No item output: represent the recipe by its first output (or input) fluid's droplet icon.
        if (outputFluid != null && outputFluid.length > 0) return ItemFluidIcon.make(outputFluid[0]);
        if (inputFluid != null && inputFluid.length > 0) return ItemFluidIcon.make(inputFluid[0]);
        return ItemStack.EMPTY;
    }

    /**
     * The recipe's display name, as 1.7's {@code getLocalizedName}: the translation of the internal name for a
     * named recipe ({@link #setNamed}), otherwise the icon's item name; wrapped by {@link #nameWrapper} when set
     * (e.g. "Reprocessing of Spent MOX").
     */
    public String getLocalizedName() {
        String base;
        if (customLocalization) {
            base = Component.translatable(this.name).getString();
        } else {
            ItemStack icon = getIcon();
            base = icon.isEmpty() ? this.name : icon.getHoverName().getString();
        }
        return nameWrapper == null ? base : Component.translatable(nameWrapper, base).getString();
    }

    /**
     * Multi-line recipe description for the GUI tooltip (name, duration, power, item/fluid inputs and
     * outputs). Ported from HBM's {@code GenericRecipe.print()}.
     */
    public List<Component> print() {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal(getLocalizedName()).withStyle(ChatFormatting.YELLOW));

        // 1.7 autoSwitch(): the group's name and what joining it means, one line per '$' of the lang entry
        if (autoSwitchGroup != null) {
            String group = Component.translatable(autoSwitchGroup).getString();
            for (String line : Component.translatable("autoswitch", group).getString().split("\\$")) {
                list.add(Component.literal(line).withStyle(ChatFormatting.GOLD));
            }
        }

        if (duration > 0) {
            list.add(Component.translatable("gui.recipe.duration")
                    .append(": " + (duration / 20D) + "s").withStyle(ChatFormatting.RED));
        }
        if (power > 0) {
            list.add(Component.translatable("gui.recipe.consumption")
                    .append(": " + power + "HE/t").withStyle(ChatFormatting.RED));
        }

        list.add(Component.translatable("gui.recipe.input").append(":").withStyle(ChatFormatting.BOLD));
        if (inputItem != null) for (AStack stack : inputItem) {
            ItemStack display = stack.getDisplayStack();
            list.add(Component.literal("  " + display.getCount() + "x ")
                    .append(display.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        if (inputFluid != null) for (FluidStack fluid : inputFluid) list.add(fluidLine(fluid));

        list.add(Component.translatable("gui.recipe.output").append(":").withStyle(ChatFormatting.BOLD));
        if (outputChoices != null) {
            float total = 0F;
            for (OutputChoice choice : outputChoices) total += Math.max(0F, choice.weight());
            for (OutputChoice choice : outputChoices) {
                int chance = total <= 0F ? 0 : Math.round(choice.weight() * 100F / total);
                ItemStack out = choice.stack();
                list.add(Component.literal("  " + chance + "% " + out.getCount() + "x ")
                        .append(out.getHoverName()).withStyle(ChatFormatting.GRAY));
            }
        } else if (outputItem != null) for (int i = 0; i < outputItem.length; i++) {
            ItemStack out = outputItem[i];
            float chance = getChance(i);
            String prefix = chance >= 1F ? "  " : "  " + Math.round(chance * 100F) + "% ";
            list.add(Component.literal(prefix + out.getCount() + "x ")
                    .append(out.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
        if (outputFluid != null) for (FluidStack fluid : outputFluid) list.add(fluidLine(fluid));

        return list;
    }

    private static Component fluidLine(FluidStack fluid) {
        String text = "  " + fluid.fill + " mB " + fluid.type.getDisplayNameString()
                + (fluid.pressure != 0 ? " @ " + fluid.pressure + " PU" : "");
        return Component.literal(text).withStyle(ChatFormatting.BLUE);
    }

    /** Whether this recipe's localized name contains {@code substring} (case-insensitive). */
    public boolean matchesSearch(String substring) {
        return TextSearch.contains(getLocalizedName().toLowerCase(Locale.US), substring.toLowerCase(Locale.US));
    }
}
