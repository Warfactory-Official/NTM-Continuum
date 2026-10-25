package com.hbm.handler.jei;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AnnihilatorRecipes;
import com.hbm.inventory.recipes.AnnihilatorRecipes.Milestone;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.OutgasserRecipes.OutgasserRecipe;
import com.hbm.inventory.recipes.StorageDrumRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModEntityItems;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import com.hbm.tileentity.machine.TileEntityDeuteriumExtractor;
import com.hbm.tileentity.machine.TileEntityMachineOreSlopper;
import dev.golem.core.Ids;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * The 1.7 {@code NEIUniversalHandler} tabs that only list pairs: one category class, one row builder per 1.7 handler.
 * Each builder reads the table or data map the machine itself reads.
 */
public class UniversalRecipeHandler extends JEIUniversalHandler<UniversalRecipeHandler.Row> {

    /** {@code machines} empty shows the category's machines; {@code note} is a red line on every input, or null. */
    public record Row(List<List<ItemStack>> inputs, List<List<ItemStack>> outputs, List<ItemStack> machines, Component note) {
        static Row of(ItemStack in, ItemStack out) {
            return new Row(List.of(List.of(in)), List.of(List.of(out)), List.of(), null);
        }
    }

    public UniversalRecipeHandler(ViewerType<Row> type, Component title, ItemStack... machines) {
        super(type, title, machines);
    }

    @Override
    protected List<List<ItemStack>> inputs(Row recipe) {
        return recipe.inputs();
    }

    @Override
    protected List<List<ItemStack>> outputs(Row recipe) {
        return recipe.outputs();
    }

    @Override
    protected List<ItemStack> machine(Row recipe) {
        return recipe.machines().isEmpty() ? super.machine(recipe) : recipe.machines();
    }

    @Override
    protected void inputTooltip(Row recipe, List<Component> tooltip) {
        if (recipe.note() != null) tooltip.add(recipe.note());
    }

    /** 1.7 {@code AnnihilatorRecipes.getRecipes}: one row per milestone, its amount in red on the input. */
    public static List<Row> annihilator() {
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<Object, AnnihilatorRecipes.AnnihilatorRecipe> entry : AnnihilatorRecipes.recipes().entrySet()) {
            Object key = entry.getKey();
            List<ItemStack> input = key instanceof Item item ? List.of(new ItemStack(item))
                    : key instanceof FluidType fluid ? List.of(ItemFluidIcon.make(fluid))
                    : new TagStack(TagKey.create(Registries.ITEM, Ids.parse((String) key))).getDisplayStacks();
            if (input.isEmpty() || input.stream().anyMatch(UniversalRecipeHandler::secret)) continue;
            for (Milestone milestone : entry.getValue().milestones) {
                if (secret(milestone.payout())) continue;
                Component amount = Component.literal(String.format(Locale.US, "%,d", milestone.amount())).withStyle(ChatFormatting.RED);
                rows.add(new Row(List.of(input), List.of(List.of(milestone.payout().copy())), List.of(), amount));
            }
        }
        return rows;
    }

    /** 1.7 hides {@code item_secret} from every universal tab. */
    private static boolean secret(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().startsWith("item_secret");
    }

    /** 1.7 {@code OreSlopperHandler}: water and bedrock ore in, every base-grade ore and slop out. */
    public static List<Row> oreSlopper() {
        int water = TileEntityMachineOreSlopper.waterUsedBase;
        List<List<ItemStack>> out = new ArrayList<>();
        for (BedrockOreType type : BedrockOreType.values()) out.add(List.of(new ItemStack(ModProduction2Blocks.bedrockOreBase(type))));
        out.add(List.of(ItemFluidIcon.make(new FluidStack(Fluids.SLOP, water))));
        List<List<ItemStack>> in = List.of(List.of(ItemFluidIcon.make(new FluidStack(Fluids.WATER, water))),
                List.of(new ItemStack(ModOreBlocks.bedrock_ore_base.get())));
        return List.of(new Row(in, out, List.of(), null));
    }

    /** 1.7 {@code OutgasserHandler}: fusion-only rows name the fusion breeder alone. */
    public static List<Row> outgasser() {
        OutgasserRecipes.INSTANCE.registerRecipes();
        List<Row> rows = new ArrayList<>();
        for (GenericRecipe recipe : OutgasserRecipes.INSTANCE.recipeOrderedList) {
            if (!(recipe instanceof OutgasserRecipe row) || recipe.inputItem == null || recipe.inputItem.length == 0) continue;
            List<List<ItemStack>> out = new ArrayList<>();
            if (!row.solidOutput().isEmpty()) out.add(List.of(row.solidOutput()));
            if (row.fluidOutput() != null) out.add(List.of(ItemFluidIcon.make(row.fluidOutput())));
            List<ItemStack> machines = row.fusionOnly ? List.of(new ItemStack(ModMachineBlocks.fusion_breeder.get())) : List.of();
            rows.add(new Row(List.of(recipe.inputItem[0].getDisplayStacks()), out, machines, null));
        }
        return rows;
    }

    /** 1.7 {@code DeuteriumHandler}: 1,000 mB water to its share of heavy water. */
    public static List<Row> deuterium() {
        return List.of(Row.of(ItemFluidIcon.make(new FluidStack(Fluids.WATER, 1_000)),
                ItemFluidIcon.make(new FluidStack(Fluids.HEAVYWATER, 1_000 / TileEntityDeuteriumExtractor.WATER_PER_HEAVY_WATER))));
    }

    /** 1.7 {@code GrenadeRecipeHandler} for its key: every shell, fitting filling and fuze, plain and with each extra. */
    public static List<Row> grenade() {
        List<Row> rows = new ArrayList<>();
        for (EnumGrenadeShell shell : EnumGrenadeShell.values()) {
            for (EnumGrenadeFilling filling : EnumGrenadeFilling.values()) {
                if (!filling.compatibleShells.contains(shell)) continue;
                for (EnumGrenadeFuze fuze : EnumGrenadeFuze.values()) {
                    rows.add(grenade(shell, filling, fuze, null));
                    for (EnumGrenadeExtra extra : EnumGrenadeExtra.values()) rows.add(grenade(shell, filling, fuze, extra));
                }
            }
        }
        return rows;
    }

    private static Row grenade(EnumGrenadeShell shell, EnumGrenadeFilling filling, EnumGrenadeFuze fuze, EnumGrenadeExtra extra) {
        List<List<ItemStack>> in = new ArrayList<>();
        in.add(List.of(ModEntityItems.grenade_shell.stack(shell)));
        in.add(List.of(ModEntityItems.grenade_filling.stack(filling)));
        in.add(List.of(ModEntityItems.grenade_fuze.stack(fuze)));
        if (extra != null) in.add(List.of(ModEntityItems.grenade_extra.stack(extra)));
        return new Row(in, List.of(List.of(ItemGrenadeUniversal.make(shell, filling, fuze, extra))), List.of(), null);
    }

    /** 1.7 {@code TileEntityReactorZirnox.fuelMap}: each rod to what it becomes when used up. */
    public static List<Row> zirnox() {
        return pairs(ReactorDataMaps.ZIRNOX_FUEL.byId(), ReactorDataMaps.ZirnoxFuel::depletesInto);
    }

    /** 1.7 {@code PWRRecipeHandler}: each fuel to its hot rod. */
    public static List<Row> pwr() {
        return pairs(ReactorDataMaps.PWR_FUEL.byId(), ReactorDataMaps.PWRFuel::hot);
    }

    /** 1.7 {@code WatzRecipeHandler}: each pellet to its depleted pellet. */
    public static List<Row> watz() {
        return pairs(ReactorDataMaps.WATZ_PELLET.byId(), ReactorDataMaps.WatzPellet::depleted);
    }

    private static <V> List<Row> pairs(Map<String, V> map, Function<V, String> result) {
        List<Row> rows = new ArrayList<>();
        for (Map.Entry<String, V> entry : map.entrySet()) {
            ItemStack in = new ItemStack(ReactorDataMaps.itemOf(entry.getKey()));
            ItemStack out = new ItemStack(ReactorDataMaps.itemOf(result.apply(entry.getValue())));
            if (!in.isEmpty() && !out.isEmpty()) rows.add(Row.of(in, out));
        }
        return rows;
    }

    /**
     * 1.7 {@code RBMKRodDisassemblyHandler}: every rod with pellets at five enrichments, each also xenon-poisoned when its
     * pellet tracks xenon, to eight pellets of that stage.
     */
    public static List<Row> rbmkDisassembly() {
        List<Row> rows = new ArrayList<>();
        for (String id : ReactorDataMaps.RBMK_FUEL.byId().keySet()) {
            if (!(ReactorDataMaps.itemOf(id) instanceof ItemRBMKRod rod) || !(rod.pelletItem() instanceof ItemRBMKPellet pellet)) continue;
            for (int enrichment = 0; enrichment <= 4; enrichment++) {
                rows.add(Row.of(rod(rod, enrichment, false), pellets(pellet, enrichment)));
                if (pellet.isXenonEnabled()) rows.add(Row.of(rod(rod, enrichment, true), pellets(pellet, enrichment + 5)));
            }
        }
        return rows;
    }

    /** 1.7 {@code ComparableStackHeat.toStack}: yield at {@code 1 - enrichment / 5}, at most 99%, poison 50. */
    private static ItemStack rod(ItemRBMKRod rod, int enrichment, boolean poison) {
        ItemStack stack = new ItemStack(rod);
        ItemRBMKRod.setYield(stack, Math.min(1 - enrichment / 5D, 0.99) * rod.stats().yield());
        if (poison) ItemRBMKRod.setPoison(stack, 50);
        return stack;
    }

    private static ItemStack pellets(ItemRBMKPellet pellet, int meta) {
        ItemStack stack = pellet.of(meta);
        stack.setCount(8);
        return stack;
    }

    /** 1.7 {@code RBMKWasteDecayHandler}, read from the storage drum's table. */
    public static List<Row> wasteDecay() {
        List<Row> rows = new ArrayList<>();
        for (StorageDrumRecipes.Recipe recipe : StorageDrumRecipes.getAll()) rows.add(Row.of(recipe.input(), recipe.output()));
        return rows;
    }
}
