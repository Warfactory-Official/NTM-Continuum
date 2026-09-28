package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.machine.ItemFluidIDMulti;
import com.hbm.items.special.ItemBookLore;
import com.hbm.items.special.ItemClayTablet;
import com.hbm.items.special.ItemHolotapeImage;
import com.hbm.items.special.ItemPolaroid;
import com.hbm.items.tool.ItemAnalysisTool;
import com.hbm.items.tool.ItemBookLemegeton;
import com.hbm.items.tool.ItemColtanCompass;
import com.hbm.items.tool.ItemDigammaDiagnostic;
import com.hbm.items.tool.ItemDyatlov;
import com.hbm.items.tool.ItemGuideBook;
import com.hbm.items.tool.ItemLeadBox;
import com.hbm.items.tool.ItemMeteorRemote;
import com.hbm.items.tool.ItemOreDensityScanner;
import com.hbm.items.tool.ItemPipette;
import com.hbm.items.tool.ItemPollutionDetector;
import com.hbm.items.tool.ItemPowerNetTool;
import com.hbm.items.tool.ItemRTTYPager;
import com.hbm.items.tool.ItemSettingsTool;
import com.hbm.items.tool.ItemSurveyScanner;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.data.ItemContents;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;

/**
 * The handheld utility and diagnostic tools and the books, from 1.7 {@code ModItems}: the analysis, power-net,
 * settings and meltdown tools, the Coltass, containment box, digamma diagnostic, multi fluid identifier,
 * meteorite remote, ore density / survey scanners, pollution detector, RoR pager, the Polaroid, the three
 * pipettes, the guide books, lore books, holotapes, clay tablets and the Lemegeton. Tabs follow 1.7
 * ({@code consumableTab}, {@code controlTab}, {@code templateTab}); items 1.7 hid stay hidden. Metadata items
 * (guide book, holotape, clay tablet) keep one id and select their variant with {@code item_variant}.
 */
public final class ModUtilityToolItems {

    private ModUtilityToolItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** Every id this class registers, in order: the harness and GameTests walk it. */
    public static final List<String> IDS = new ArrayList<>();

    // --- stack data, 1.7's NBT keys under one component each ---
    /** Coltass deposit: {@code colX}, {@code colZ}. */
    public static final GolemComponent<CompoundTag> COLTASS = GolemComponent.compound(Constants.MOD_ID, "coltass");
    /** The containment box's 20 slots. */
    public static final GolemComponent<ItemContents> CONTAINMENT =
            GolemComponent.of(Constants.MOD_ID, "containment_box", ItemContents.CODEC);
    /** Pipette: {@code type}, {@code fill}, {@code capacity}. */
    public static final GolemComponent<CompoundTag> PIPETTE = GolemComponent.compound(Constants.MOD_ID, "pipette");
    /** Multi fluid identifier: {@code fluid1}, {@code fluid2}. */
    public static final GolemComponent<CompoundTag> FLUID_MULTI = GolemComponent.compound(Constants.MOD_ID, "fluid_multi");
    /** Settings tool: the copied settings plus {@code tileName}, {@code copyIndex}, {@code inputDelay}, {@code displayInfo}. */
    public static final GolemComponent<CompoundTag> SETTINGS = GolemComponent.compound(Constants.MOD_ID, "settings_tool");
    /** RoR pager channel ({@code chan}). */
    public static final GolemComponent<String> PAGER_CHANNEL = GolemComponent.string(Constants.MOD_ID, "pager_channel");
    /** Lore book: {@code k}, {@code p}, {@code cov_col}, {@code tit_col}, {@code p<n>} arguments. */
    public static final GolemComponent<CompoundTag> BOOK_LORE = GolemComponent.compound(Constants.MOD_ID, "book_lore");
    /** Clay tablet {@code tabletSeed}. */
    public static final GolemComponent<Long> TABLET_SEED = GolemComponent.longValue(Constants.MOD_ID, "tablet_seed");

    private static <I extends Item> RegistryObject<I> reg(String id, HbmTab tab, Function<Item.Properties, I> factory,
                                                          UnaryOperator<Item.Properties> props) {
        RegistryObject<I> ro = ITEMS.registerItem(id, factory, props);
        if (tab != null) ModTabs.addTo(tab, ro);
        IDS.add(id);
        return ro;
    }

    private static UnaryOperator<Item.Properties> single() {
        return p -> p.stacksTo(1);
    }

    public static final RegistryObject<ItemLeadBox> containment_box = reg("containment_box", HbmTab.CONSUMABLES,
            ItemLeadBox::new, single());
    public static final RegistryObject<ItemDyatlov> meltdown_tool = reg("meltdown_tool", HbmTab.CONTROL,
            ItemDyatlov::new, single());
    /** 1.7 {@code setMaxDamage(2)}: broken when damage passes 2, i.e. three uses; vanilla breaks at max, hence 3. */
    public static final RegistryObject<ItemMeteorRemote> meteor_remote = reg("meteor_remote", HbmTab.CONSUMABLES,
            ItemMeteorRemote::new, p -> p.stacksTo(1).durability(3));
    public static final RegistryObject<ItemDigammaDiagnostic> digamma_diagnostic = reg("digamma_diagnostic",
            HbmTab.CONSUMABLES, ItemDigammaDiagnostic::new, single());
    public static final RegistryObject<ItemPollutionDetector> pollution_detector = reg("pollution_detector",
            HbmTab.CONSUMABLES, ItemPollutionDetector::new, single());
    public static final RegistryObject<ItemOreDensityScanner> ore_density_scanner = reg("ore_density_scanner",
            HbmTab.CONSUMABLES, ItemOreDensityScanner::new, single());
    public static final RegistryObject<ItemSurveyScanner> survey_scanner = reg("survey_scanner", HbmTab.CONSUMABLES,
            ItemSurveyScanner::new, single());
    public static final RegistryObject<ItemColtanCompass> coltan_tool = reg("coltan_tool", HbmTab.CONSUMABLES,
            ItemColtanCompass::new, single());
    public static final RegistryObject<ItemPowerNetTool> power_net_tool = reg("power_net_tool", HbmTab.CONSUMABLES,
            ItemPowerNetTool::new, single());
    public static final RegistryObject<ItemAnalysisTool> analysis_tool = reg("analysis_tool", HbmTab.CONSUMABLES,
            ItemAnalysisTool::new, single());
    public static final RegistryObject<ItemSettingsTool> settings_tool = reg("settings_tool", HbmTab.CONSUMABLES,
            ItemSettingsTool::new, single());
    public static final RegistryObject<ItemRTTYPager> rtty_pager = reg("rtty_pager", HbmTab.CONSUMABLES,
            ItemRTTYPager::new, single());
    /** 1.7 templateTab, one identifier per fluid ({@link ItemFluidIDMulti#creativeStacks}). */
    public static final RegistryObject<ItemFluidIDMulti> fluid_identifier_multi = reg("fluid_identifier_multi", null,
            ItemFluidIDMulti::new, single());
    public static final RegistryObject<ItemPipette> pipette = reg("pipette", HbmTab.CONTROL, ItemPipette::new, single());
    public static final RegistryObject<ItemPipette> pipette_boron = reg("pipette_boron", HbmTab.CONTROL,
            ItemPipette::new, single());
    public static final RegistryObject<ItemPipette> pipette_laboratory = reg("pipette_laboratory", HbmTab.CONTROL,
            ItemPipette::new, single());

    /** 1.7 consumableTab with the RBMK and starter books ({@link ItemGuideBook#creativeStacks}). */
    public static final RegistryObject<ItemGuideBook> book_guide = reg("book_guide", null, ItemGuideBook::new, single());
    public static final RegistryObject<ItemBookLore> book_lore = reg("book_lore", null, ItemBookLore::new, single());
    public static final RegistryObject<ItemHolotapeImage> holotape_image = reg("holotape_image", null,
            ItemHolotapeImage::new, single());
    public static final RegistryObject<ItemClayTablet> clay_tablet = reg("clay_tablet", null, ItemClayTablet::new,
            single());
    public static final RegistryObject<ItemPolaroid> polaroid = reg("polaroid", HbmTab.CONSUMABLES, ItemPolaroid::new,
            single());
    public static final RegistryObject<ItemBookLemegeton> book_lemegeton = reg("book_lemegeton", null,
            ItemBookLemegeton::new, single());

    static {
        ModTabs.addStacksTo(HbmTab.TEMPLATES, ItemFluidIDMulti::creativeStacks);
        ModTabs.addStacksTo(HbmTab.CONSUMABLES, ItemGuideBook::creativeStacks);
    }

    public static void init() {}
}
