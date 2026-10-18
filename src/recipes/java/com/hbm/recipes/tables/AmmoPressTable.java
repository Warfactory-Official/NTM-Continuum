package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.inventory.FluidContainerStack;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.AmmoPressRecipes.Row;
import com.hbm.inventory.recipes.loader.ItemSpec;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModCasingItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import dev.golem.core.world.RecipeSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.7 {@code AmmoPressRecipes.registerDefaults}. */
public final class AmmoPressTable extends RecipeSource<Row> {

    /** 1.7 {@code DictGroup}s not among the port's {@code OreDictManager} groups: their aggregate tags. */
    private static final NTMMaterial ANY_SMOKELESS = new NTMMaterial(-1, "any_smokeless");
    private static final NTMMaterial ANY_HIGHEXPLOSIVE = new NTMMaterial(-1, "any_highexplosive");
    /** 1.7 {@code P_WHITE} ("WhitePhosphorus"): {@code c:ingots/white_phosphorus}, 1.7's members. */
    private static final NTMMaterial P_WHITE = new NTMMaterial(-1, "white_phosphorus");

    private final Set<String> ids = new HashSet<>();

    public AmmoPressTable() {
        super(AmmoPressRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        AStack lead = PB.ingot();
        AStack nugget = PB.nugget();
        AStack flechette = PB.bolt();
        AStack steel = STEEL.ingot();
        AStack wSteel = WEAPONSTEEL.ingot();
        AStack copper = CU.ingot();
        AStack plastic = ANY_PLASTIC.ingot();
        AStack uranium = U238.ingot();
        AStack ferro = FERRO.ingot();
        AStack nb = NB.ingot();
        AStack smokeless = ANY_SMOKELESS.dust();
        AStack he = ANY_HIGHEXPLOSIVE.ingot();
        AStack wp = P_WHITE.ingot();
        AStack rp = P_RED.dust();
        AStack pipe = STEEL.pipe();
        AStack gunmetal = GUNMETAL.plate();
        AStack smokeful = new ComparableStack(Items.GUNPOWDER);
        AStack rocket = new ComparableStack(ModFuelItems.rocket_fuel.get());
        AStack cSmall = casing(EnumCasingType.SMALL);
        AStack cBig = casing(EnumCasingType.LARGE);
        AStack sSmall = casing(EnumCasingType.SMALL_STEEL);
        AStack sBig = casing(EnumCasingType.LARGE_STEEL);
        AStack bpShell = casing(EnumCasingType.SHOTSHELL);
        AStack pShell = casing(EnumCasingType.BUCKSHOT);
        AStack sShell = casing(EnumCasingType.BUCKSHOT_ADVANCED);

        add(new AmmoPressRecipe(standard(EnumAmmo.M357_BP, 16),
                null,    copy(lead, 2),    null,
                null,    smokeful,        null,
                null,    cSmall,            null));
        add(new AmmoPressRecipe(standard(EnumAmmo.M357_SP, 8),
                null,    lead,        null,
                null,    smokeless,    null,
                null,    cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.M357_FMJ, 8),
                null,    steel,        null,
                null,    smokeless,    null,
                null,    cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.M357_JHP, 8),
                plastic,    copper,        null,
                null,        smokeless,    null,
                null,        cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.M357_AP, 8),
                null,    wSteel,                null,
                null,    copy(smokeless, 2),    null,
                null,    sSmall,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.M357_EXPRESS, 8),
                null,    steel,                null,
                null,    copy(smokeless, 3),    null,
                null,    cSmall,                null));

        add(new AmmoPressRecipe(standard(EnumAmmo.M44_BP, 12),
                null,    copy(lead, 2),        null,
                null,    smokeful,    null,
                null,    cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.M44_SP, 6),
                null,    lead,        null,
                null,    smokeless,    null,
                null,    cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.M44_FMJ, 6),
                null,    steel,        null,
                null,    smokeless,    null,
                null,    cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.M44_JHP, 6),
                plastic,    copper,        null,
                null,        smokeless,    null,
                null,        cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.M44_AP, 6),
                null,    wSteel,                null,
                null,    copy(smokeless, 2),    null,
                null,    sSmall,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.M44_EXPRESS, 6),
                null,    steel,                null,
                null,    copy(smokeless, 3),    null,
                null,    cSmall,                null));

        add(new AmmoPressRecipe(standard(EnumAmmo.P22_SP, 24),
                null,    lead,        null,
                null,    smokeless,    null,
                null,    cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.P22_FMJ, 24),
                null,    steel,        null,
                null,    smokeless,    null,
                null,    cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.P22_JHP, 24),
                plastic,    copper,        null,
                null,        smokeless,    null,
                null,        cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.P22_AP, 24),
                null,    wSteel,                null,
                null,    copy(smokeless, 2),    null,
                null,    sSmall,                null));

        add(new AmmoPressRecipe(standard(EnumAmmo.P9_SP, 12),
                null,    lead,        null,
                null,    smokeless,    null,
                null,    cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.P9_FMJ, 12),
                null,    steel,        null,
                null,    smokeless,    null,
                null,    cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.P9_JHP, 12),
                plastic,    copper,        null,
                null,        smokeless,    null,
                null,        cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.P9_AP, 12),
                null,    wSteel,                null,
                null,    copy(smokeless, 2),    null,
                null,    sSmall,                null));

        add(new AmmoPressRecipe(standard(EnumAmmo.P45_SP, 8),
                null,    lead,        null,
                null,    smokeless,    null,
                null,    cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.P45_FMJ, 8),
                null,    steel,        null,
                null,    smokeless,    null,
                null,    cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.P45_JHP, 8),
                plastic,    copper,        null,
                null,        smokeless,    null,
                null,        cSmall,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.P45_AP, 8),
                null,    wSteel,                null,
                null,    copy(smokeless, 2),    null,
                null,    sSmall,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.P45_DU, 8),
                null,    uranium,            null,
                null,    copy(smokeless, 2),    null,
                null,    sSmall,                null));

        add(new AmmoPressRecipe(standard(EnumAmmo.R556_SP, 16),
                null,    copy(lead, 2),        null,
                null,    copy(smokeless, 2),    null,
                null,    copy(cSmall, 2),        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.R556_FMJ, 16),
                null,    copy(steel, 2),        null,
                null,    copy(smokeless, 2),    null,
                null,    copy(cSmall, 2),        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.R556_JHP, 16),
                plastic,    copy(copper, 2),        null,
                null,        copy(smokeless, 2),    null,
                null,        copy(cSmall, 2),        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.R556_AP, 16),
                null,    copy(wSteel, 2),        null,
                null,    copy(smokeless, 4),    null,
                null,    copy(sSmall, 2),        null));

        add(new AmmoPressRecipe(standard(EnumAmmo.R762_SP, 12),
                null,    copy(lead, 2),        null,
                null,    copy(smokeless, 2),    null,
                null,    copy(cSmall, 2),        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.R762_FMJ, 12),
                null,    copy(steel, 2),        null,
                null,    copy(smokeless, 2),    null,
                null,    copy(cSmall, 2),        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.R762_JHP, 12),
                plastic,    copy(copper, 2),        null,
                null,        copy(smokeless, 2),    null,
                null,        copy(cSmall, 2),        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.R762_AP, 12),
                null,    copy(wSteel, 2),        null,
                null,    copy(smokeless, 4),    null,
                null,    copy(sSmall, 2),        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.R762_DU, 12),
                null,    copy(uranium, 2),    null,
                null,    copy(smokeless, 4),    null,
                null,    copy(sSmall, 2),        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.R762_HE, 12),
                he,        ferro,                null,
                null,    copy(smokeless, 4),    null,
                null,    copy(sSmall, 2),        null));

        add(new AmmoPressRecipe(standard(EnumAmmo.BMG50_SP, 12),
                null,    copy(lead, 2),        null,
                null,    copy(smokeless, 3),    null,
                null,    cBig,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.BMG50_FMJ, 12),
                null,    copy(steel, 2),        null,
                null,    copy(smokeless, 3),    null,
                null,    cBig,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.BMG50_JHP, 12),
                plastic,    copy(copper, 2),        null,
                null,        copy(smokeless, 3),    null,
                null,        cBig,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.BMG50_AP, 12),
                null,    copy(wSteel, 2),        null,
                null,    copy(smokeless, 6),    null,
                null,    sBig,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.BMG50_DU, 12),
                null,    copy(uranium, 2),    null,
                null,    copy(smokeless, 6),    null,
                null,    sBig,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.BMG50_HE, 12),
                he,        ferro,                null,
                null,    copy(smokeless, 6),    null,
                null,    sBig,                null));

        add(new AmmoPressRecipe(standard(EnumAmmo.G12_BP, 6),
                null,    copy(nugget, 6), null,
                null,    smokeful,        null,
                null,    bpShell,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G12_BP_MAGNUM, 6),
                null,    copy(nugget, 8), null,
                null,    smokeful,        null,
                null,    bpShell,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G12_BP_SLUG, 6),
                null,    lead,         null,
                null,    smokeful,    null,
                null,    bpShell,    null));

        add(new AmmoPressRecipe(standard(EnumAmmo.G12, 6),
                null,    copy(nugget, 6),    null,
                null,    smokeless,        null,
                null,    pShell,            null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G12_SLUG, 6),
                null,    lead,         null,
                null,    smokeless,    null,
                null,    pShell,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G12_FLECHETTE, 6),
                null,    copy(flechette, 12),    null,
                null,    smokeless,            null,
                null,    pShell,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G12_MAGNUM, 6),
                null,    copy(nugget, 8),        null,
                null,    smokeless,            null,
                null,    sShell,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G12_EXPLOSIVE, 6),
                null,    he,            null,
                null,    smokeless,    null,
                null,    sShell,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G12_PHOSPHORUS, 6),
                null,    wp,            null,
                null,    smokeless,    null,
                null,    sShell,        null));

        add(new AmmoPressRecipe(standard(EnumAmmo.G10, 4),
                null,    copy(nugget, 8),        null,
                null,    copy(smokeless, 2),    null,
                null,    sShell,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G10_SHRAPNEL, 4),
                plastic,    copy(nugget, 8),        null,
                null,        copy(smokeless, 2),    null,
                null,        sShell,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G10_DU, 4),
                null,    uranium,            null,
                null,    copy(smokeless, 2),    null,
                null,    sShell,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G10_SLUG, 4),
                null,    lead,                null,
                null,    copy(smokeless, 2),    null,
                null,    sShell,                null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G10_EXPLOSIVE, 4),
                he,        ferro,                null,
                null,    copy(smokeless, 2),    null,
                null,    sShell,                null));

        add(new AmmoPressRecipe(standard(EnumAmmo.G26_FLARE, 4),
                null,    rp,            null,
                null,    smokeless,    null,
                null,    cBig,        null));

        AStack dyn = new ComparableStack(ModItems.ball_dynamite.get());
        AStack coplate = CU.plate();
        AStack diesel = getDict(Fluids.DIESEL, 1_000);
        add(new AmmoPressRecipe(standard(EnumAmmo.G40_HE, 4),
                null,    dyn,        null,
                null,    smokeless,    null,
                null,    cBig,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G40_HEAT, 4),
                coplate,    he,            null,
                null,        smokeless,    null,
                null,        cBig,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G40_DEMO, 4),
                null,    copy(he, 2),    null,
                null,    smokeless,    null,
                null,    cBig,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G40_INC, 4),
                diesel,    dyn,    null,
                null,    smokeless,    null,
                null,    cBig,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.G40_PHOSPHORUS, 4),
                wp,        he,            null,
                null,    smokeless,    null,
                null,    cBig,        null));

        add(new AmmoPressRecipe(standard(EnumAmmo.ROCKET_HE, 2),
                null,    dyn,                null,
                null,    cBig,                null,
                null,    copy(smokeless, 3),    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.ROCKET_HE, 2),
                null,    dyn,    null,
                null,    cBig,    null,
                null,    rocket,    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.ROCKET_HEAT, 2),
                coplate,    he,                    null,
                null,        cBig,                null,
                null,        copy(smokeless, 3),    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.ROCKET_HEAT, 2),
                coplate,    he,        null,
                null,        cBig,    null,
                null,        rocket,    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.ROCKET_DEMO, 2),
                null,    copy(he, 2),            null,
                null,    cBig,                null,
                null,    copy(smokeless, 3),    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.ROCKET_DEMO, 2),
                null,    copy(he, 2),    null,
                null,    cBig,        null,
                null,    rocket,        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.ROCKET_INC, 2),
                diesel,    dyn,                null,
                null,    cBig,                null,
                null,    copy(smokeless, 3),    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.ROCKET_INC, 2),
                diesel,    dyn,    null,
                null,    cBig,    null,
                null,    rocket,    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.ROCKET_PHOSPHORUS, 2),
                wp,        he,                    null,
                null,    cBig,                null,
                null,    copy(smokeless, 3),    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.ROCKET_PHOSPHORUS, 2),
                wp,        he,        null,
                null,    cBig,    null,
                null,    rocket,    null));

        AStack sPlate = STEEL.plate();
        AStack napalm = new ComparableStack(ModItems.canister_napalm.get());
        AStack gas = getDict(Fluids.GAS, 1000);
        AStack bf = getDict(Fluids.BALEFIRE, 1000);
        add(new AmmoPressRecipe(standard(EnumAmmo.FLAME_DIESEL, 1),
                null,    sPlate,    null,
                null,    diesel,    null,
                null,    sPlate,    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.FLAME_NAPALM, 1),
                null,    sPlate,    null,
                null,    napalm,    null,
                null,    sPlate,    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.FLAME_GAS, 1),
                null,    sPlate,    null,
                null,    gas,    null,
                null,    sPlate,    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.FLAME_BALEFIRE, 1),
                null,    sPlate,    null,
                null,    bf,        null,
                null,    sPlate,    null));

        AStack silicon = SI.billet();
        add(new AmmoPressRecipe(standard(EnumAmmo.CAPACITOR, 4),
                null,    plastic,            null,
                null,    copy(silicon, 4),    null,
                null,    plastic,            null));
        add(new AmmoPressRecipe(standard(EnumAmmo.CAPACITOR_OVERCHARGE, 4),
                null,    plastic,            null,
                null,    copy(silicon, 6),    null,
                null,    plastic,            null));
        add(new AmmoPressRecipe(standard(EnumAmmo.CAPACITOR_IR, 4),
                null,    plastic,    null,
                null,    nb,            null,
                null,    plastic,    null));

        AStack lPlate = PB.plate();
        add(new AmmoPressRecipe(standard(EnumAmmo.TAU_URANIUM, 16),
                null,    lPlate,        null,
                null,    uranium,    null,
                null,    lPlate    ,    null));

        AStack tungsten = W.ingot();
        add(new AmmoPressRecipe(standard(EnumAmmo.COIL_TUNGSTEN, 4),
                null,    null,        null,
                null,    tungsten,    null,
                null,    null    ,    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.COIL_FERROURANIUM, 4),
                null,    null,        null,
                null,    ferro,        null,
                null,    null    ,    null));

        AStack shell = new ComparableStack(ModItems.assembly_nuke.get());
        AStack tatb = new ComparableStack(ModItems.ball_tatb.get());
        AStack plutonium = PU239.nugget();
        add(new AmmoPressRecipe(standard(EnumAmmo.NUKE_STANDARD, 1),
                null,    plutonium,    null,
                null,    shell,        null,
                null,    null    ,    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.NUKE_DEMO, 1),
                null,    copy(plutonium, 2),    null,
                null,    shell,                null,
                null,    null    ,            null));
        add(new AmmoPressRecipe(standard(EnumAmmo.NUKE_HIGH, 1),
                null,    copy(plutonium, 4),    null,
                null,    shell,                null,
                null,    null    ,            null));
        add(new AmmoPressRecipe(standard(EnumAmmo.NUKE_TOTS, 1),
                null,    copy(plutonium, 2),    null,
                null,    copy(tatb, 2),        null,
                null,    copy(sPlate, 4)    ,    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.NUKE_HIVE, 1),
                null,    copy(he, 8),            null,
                null,    copy(sBig, 2),        null,
                null,    copy(sPlate, 4),        null));
        add(new AmmoPressRecipe(standard(EnumAmmo.NUKE_BALEFIRE, 1),
                null,    new ComparableStack(ModItems.egg_balefire_shard.get()),    null,
                null,    shell,                                                null,
                null,    null    ,                                            null));

        add(new AmmoPressRecipe(standard(EnumAmmo.CT_HOOK, 16),
                null,    steel,        null,
                null,    pipe,        null,
                null,    smokeless,    null));
        add(new AmmoPressRecipe(standard(EnumAmmo.CT_MORTAR, 4),
                null,    copy(he, 4),    null,
                null,    pipe,        null,
                null,    smokeless,    null));

        add(new AmmoPressRecipe(new ItemStack(ModItems.ammo_dgk.get()),
                null,    copy(steel, 5),        null,
                null,    copy(smokeless, 5),    null,
                null,    copy(gunmetal, 5),    null));
    }

    /** 1.7 {@code AmmoPressRecipe}: an output and the 3x3 press grid, {@code null} for an empty slot. */
    private record AmmoPressRecipe(ItemStack output, AStack... input) {}

    /**
     * 1.7 {@code recipes.add(recipe)}. The id is the ammo's name without {@code ammo_standard_}; a second recipe
     * for the same ammo is {@code <name>_alt}.
     */
    private void add(AmmoPressRecipe recipe) {
        String id = BuiltInRegistries.ITEM.getKey(recipe.output().getItem()).getPath().replaceFirst("^ammo_standard_", "");
        if (!ids.add(id)) {
            id = id + "_alt";
            ids.add(id);
        }
        List<Optional<AStack>> grid = new ArrayList<>();
        for (AStack stack : recipe.input()) grid.add(Optional.ofNullable(stack));
        register(id, new Row(ItemSpec.of(recipe.output()).getOrThrow(), grid, nextOrder()));
    }

    /** 1.7 {@code DictFrame.fromOne(ModItems.ammo_standard, type, count)}. */
    private static ItemStack standard(EnumAmmo type, int count) {
        return new ItemStack(ModAmmoItems.standardAmmo(type.name()).get(), count);
    }

    /** 1.7 {@code new ComparableStack(ModItems.casing, 1, type)}. */
    private static AStack casing(EnumCasingType type) {
        return new ComparableStack(ModCasingItems.casing(type));
    }

    /** 1.7 {@code new OreDictStack(fluid.getDict(amount))}: a full container of the fluid. */
    private static AStack getDict(FluidType fluid, int amount) {
        return new FluidContainerStack(fluid.getName(), amount);
    }

    /** 1.7 {@code stack.copy(count)}: the same ingredient with another count. */
    private static AStack copy(AStack stack, int count) {
        if (stack instanceof TagStack tag) return new TagStack(tag.tag, count);
        if (stack instanceof MaterialStack material) return new MaterialStack(material.material, material.shape, count);
        if (stack instanceof ComparableStack comparable) return new ComparableStack(comparable.item, count);
        throw new IllegalArgumentException("cannot copy " + stack);
    }
}
