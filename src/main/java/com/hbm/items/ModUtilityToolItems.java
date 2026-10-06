package com.hbm.items;

import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
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
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Tints;
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
 * (guide book, holotape, clay tablet) are one item per variant (History §19es-itemvariant).
 */
public final class ModUtilityToolItems {

    private ModUtilityToolItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    /** Every id this class registers, in order: the harness and GameTests walk it. */
    public static final List<String> IDS = new ArrayList<>();

    // --- stack data, 1.7's NBT keys under one component each ---
    /** Coltass deposit: {@code colX}, {@code colZ}. */
    public static final GolemComponent<CompoundTag> COLTASS = GolemComponent.compound(Constants.MOD_ID, "coltass");
    /** The containment box's 20 slots. */
    public static final GolemComponent<ItemContents> CONTAINMENT =
            GolemComponent.of(Constants.MOD_ID, "containment_box", () -> ItemContents.CODEC);
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

    /**
     * One registration with its item model, both as the old {@code UtilityToolDeclarations} stated them: 1.7 texture
     * names; tinted layers and variant sprites as 1.7's {@code getIcon}/{@code getColorFromItemStack} chose them.
     */
    private static <I extends Item> RegistryObject<I> reg(String id, HbmTab tab, Function<Item.Properties, I> factory,
                                                          UnaryOperator<Item.Properties> props, Preset model) {
        Reg.Handle<I> ro = REG.item(id, factory, props).transform(model);
        if (tab != null) ro.tab(tab);
        IDS.add(id);
        return ro;
    }

    private static UnaryOperator<Item.Properties> single() {
        return p -> p.stacksTo(1);
    }

    /** 1.7 ItemPipette.getIcon: pass 1 is pipette_empty at fill 0, else the (laboratory) overlay, tinted. */
    private static Preset pipette(String overlay) {
        return handle -> handle.icon(d -> d
                .models(Models.group()
                        .and("", Models.generated("hbm:items/" + handle.path(), "hbm:items/pipette_empty"))
                        .and("_filled", Models.generated("hbm:items/" + handle.path(), "hbm:items/" + overlay)))
                .selects(Constants.MOD_ID + ":model_state")
                .tints(Tints.constant(-1), Tints.customModelData(0, 0xFFFFFF)));
    }

    /** 1.7 TextureColtass: 32 compass frames, the frame chosen by the client's needle. */
    private static Preset coltassFrames() {
        return handle -> handle.icon(d -> {
            Models.Group frames = Models.group();
            for (int i = 0; i < 32; i++) {
                // the first case is the item's own model
                frames = frames.and(i == 0 ? "" : String.format("_%02d", i),
                        Models.generated(String.format("hbm:items/coltass_%02d", i)));
            }
            return d.models(frames).selects(Constants.MOD_ID + ":coltass");
        });
    }

    /** 1.7 setTextureName("polaroid_" + polaroidID): case n is polaroid id n + 1. */
    private static Preset polaroids() {
        return handle -> handle.icon(d -> {
            Models.Group polaroids = Models.group();
            for (int i = 1; i <= 18; i++) {
                polaroids = polaroids.and(i == 1 ? "" : "_" + i, Models.generated("hbm:items/polaroid_" + i));
            }
            return d.models(polaroids).selects(Constants.MOD_ID + ":polaroid");
        });
    }

    public static final RegistryObject<ItemLeadBox> containment_box = reg("containment_box", HbmTab.CONSUMABLES,
            ItemLeadBox::new, single(), Presets.sprite());
    public static final RegistryObject<ItemDyatlov> meltdown_tool = reg("meltdown_tool", HbmTab.CONTROL,
            ItemDyatlov::new, single(), Presets.sprite());
    /** 1.7 {@code setMaxDamage(2)}: broken when damage passes 2, i.e. three uses; vanilla breaks at max, hence 3. */
    public static final RegistryObject<ItemMeteorRemote> meteor_remote = reg("meteor_remote", HbmTab.CONSUMABLES,
            ItemMeteorRemote::new, p -> p.stacksTo(1).durability(3), Presets.handheld()); // setFull3D
    public static final RegistryObject<ItemDigammaDiagnostic> digamma_diagnostic = reg("digamma_diagnostic",
            HbmTab.CONSUMABLES, ItemDigammaDiagnostic::new, single(), Presets.sprite());
    public static final RegistryObject<ItemPollutionDetector> pollution_detector = reg("pollution_detector",
            HbmTab.CONSUMABLES, ItemPollutionDetector::new, single(), Presets.sprite());
    public static final RegistryObject<ItemOreDensityScanner> ore_density_scanner = reg("ore_density_scanner",
            HbmTab.CONSUMABLES, ItemOreDensityScanner::new, single(), Presets.sprite());
    public static final RegistryObject<ItemSurveyScanner> survey_scanner = reg("survey_scanner", HbmTab.CONSUMABLES,
            ItemSurveyScanner::new, single(), Presets.sprite());
    public static final RegistryObject<ItemColtanCompass> coltan_tool = reg("coltan_tool", HbmTab.CONSUMABLES,
            ItemColtanCompass::new, single(), coltassFrames());
    public static final RegistryObject<ItemPowerNetTool> power_net_tool = reg("power_net_tool", HbmTab.CONSUMABLES,
            ItemPowerNetTool::new, single(), Presets.sprite());
    public static final RegistryObject<ItemAnalysisTool> analysis_tool = reg("analysis_tool", HbmTab.CONSUMABLES,
            ItemAnalysisTool::new, single(), Presets.sprite());
    public static final RegistryObject<ItemSettingsTool> settings_tool = reg("settings_tool", HbmTab.CONSUMABLES,
            ItemSettingsTool::new, single(), Presets.sprite());
    public static final RegistryObject<ItemRTTYPager> rtty_pager = reg("rtty_pager", HbmTab.CONSUMABLES,
            ItemRTTYPager::new, single(), Presets.sprite());
    /** 1.7 templateTab, one identifier per fluid ({@link ItemFluidIDMulti#creativeStacks}); pass 1 tinted with the primary fluid. */
    public static final RegistryObject<ItemFluidIDMulti> fluid_identifier_multi = reg("fluid_identifier_multi", null,
            ItemFluidIDMulti::new, single(),
            Presets.sprite("hbm:items/fluid_identifier_multi", "hbm:items/fluid_identifier_overlay")
                    .tints(Tints.constant(-1), Tints.customModelData(0, 0xFFFFFF)));
    public static final RegistryObject<ItemPipette> pipette = reg("pipette", HbmTab.CONTROL, ItemPipette::new, single(),
            pipette("pipette_overlay"));
    public static final RegistryObject<ItemPipette> pipette_boron = reg("pipette_boron", HbmTab.CONTROL,
            ItemPipette::new, single(), pipette("pipette_overlay"));
    public static final RegistryObject<ItemPipette> pipette_laboratory = reg("pipette_laboratory", HbmTab.CONTROL,
            ItemPipette::new, single(), pipette("pipette_laboratory_overlay"));

    /**
     * 1.7 book_guide metadata, one item per {@link ItemGuideBook.BookType} as ntm-next registers them
     * ({@code book_guide_<type>}, all drawn with the 1.7 sprite; History §19es-itemvariant). 1.7 consumableTab with
     * the RBMK and starter books ({@link ItemGuideBook#creativeStacks}).
     */
    public static final List<RegistryObject<ItemGuideBook>> book_guide = guideBooks();

    /** By name, in {@link ItemGuideBook.BookType} order: the enum builds its pages and needs a running game. */
    private static List<RegistryObject<ItemGuideBook>> guideBooks() {
        List<RegistryObject<ItemGuideBook>> books = new ArrayList<>();
        String[] names = {"test", "rbmk", "hadron", "starter"};
        for (int i = 0; i < names.length; i++) {
            int ordinal = i;
            books.add(reg("book_guide_" + names[i], null,
                    p -> new ItemGuideBook(p, ItemGuideBook.BookType.values()[ordinal]), single(),
                    Presets.sprite("hbm:items/book_guide")));
        }
        return List.copyOf(books);
    }
    /** 1.7 ItemBookLore: pages, the tinted cover, the tinted title. */
    public static final RegistryObject<ItemBookLore> book_lore = reg("book_lore", null, ItemBookLore::new, single(),
            Presets.sprite("hbm:items/book_pages", "hbm:items/book_cover", "hbm:items/book_title")
                    .tints(Tints.constant(-1), Tints.customModelData(0, 0x303030), Tints.customModelData(1, 0xFFFFFF)));
    /** 1.7 holotape_image metadata, one item per image as ntm-next registers them ({@code holotape_image_<image>}). */
    public static final ItemFamily<ItemHolotapeImage.EnumHoloImage, ItemHolotapeImage> holotape_image = ItemFamily.of(
            ItemHolotapeImage.EnumHoloImage.class,
            type -> reg("holotape_image_" + type.name().toLowerCase(java.util.Locale.ROOT), null,
                    p -> new ItemHolotapeImage(p, type), single(), Presets.sprite("hbm:items/holotape")));
    /** 1.7 clay_tablet metadata 0 (the red-room tablet) and 1 (the dungeon one), one item each as in ntm-next. */
    public static final RegistryObject<ItemClayTablet> clay_tablet = reg("clay_tablet", null,
            p -> new ItemClayTablet(p, 0), single(), Presets.sprite());
    public static final RegistryObject<ItemClayTablet> clay_tablet_1 = reg("clay_tablet_1", null,
            p -> new ItemClayTablet(p, 1), single(), Presets.sprite("hbm:items/clay_tablet"));
    public static final RegistryObject<ItemPolaroid> polaroid = reg("polaroid", HbmTab.CONSUMABLES, ItemPolaroid::new,
            single(), polaroids());
    public static final RegistryObject<ItemBookLemegeton> book_lemegeton = reg("book_lemegeton", null,
            ItemBookLemegeton::new, single(), Presets.sprite());

    static {
        ModTabs.addStacksTo(HbmTab.TEMPLATES, ItemFluidIDMulti::creativeStacks);
        ModTabs.addStacksTo(HbmTab.CONSUMABLES, ItemGuideBook::creativeStacks);
    }

    public static void init() {}
}
