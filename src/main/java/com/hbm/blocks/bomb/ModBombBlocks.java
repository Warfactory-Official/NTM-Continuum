package com.hbm.blocks.bomb;

import com.hbm.entity.explosive.ModExplosiveEntities;
import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.items.block.ItemBlockTaint;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * Registry for HBM bomb blocks. Separate from the generated {@link com.hbm.blocks.ModBlocks} and the
 * machine blocks so codegen never clobbers them. The first entry is the Fat Man ({@code nuke_man}).
 */
public final class ModBombBlocks {

    private ModBombBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /**
     * The MK5/Torex bombs: drawn by a BlockEntityRenderer over the empty placeholder, 1.7 facing metadata 2..5;
     * they drop themselves. Their pickaxe membership is generator-owned ({@code gen_wave1.py BOMB_BLOCK_IDS}).
     */
    private static final Preset RENDERED_BOMB = Presets.rendered(Variants.uniformRange("facing", 2, 5))
            .and(handle -> handle.loot(Loot.self()));
    /** The TNT-family blocks: 1.7 bottom/side/top icons, the armed flag, and a drop that survives the blast. */
    private static Preset tntFamily(String icon) {
        return Presets.cubeBottomTop("hbm:blocks/" + icon + "_bottom", "hbm:blocks/" + icon + "_side",
                        "hbm:blocks/" + icon + "_top").states(Variants.uniform("armed", "false", "true"))
                .and(handle -> handle.loot(Loot.selfAlways()));
    }

    /** Taint residue: a hard, spreading block with a shallow collision box. */
    public static final RegistryObject<BlockTaint> taint = REG.block("taint", BlockTaint::new,
                    p -> p.mapColor(MapColor.COLOR_GRAY).strength(15.0F, 10.0F).sound(SoundType.METAL)
                            .randomTicks())
            .transform(Presets.cubeAll().states(Variants.uniformRange("taintage", 0, 15)).viaItemModel())
            // 1.7: drops nothing, but a plain cube without a tile entity, so silk touch keeps it
            .loot(Loot.silkTouchOnly());

    // --- the DRX/GAMMA burn products. No creative tab upstream (setCreativeTab(null)) and no block
    // item: these exist only to be PLACED by a balefire detonation or an FEL beam. ---

    /** Balefire: light 15, hardness 0, spreads violently and irradiates anything standing in it. */
    public static final RegistryObject<BlockBalefire> balefire = REG.block("balefire",
                    BlockBalefire::new, p -> p.strength(0.0F).lightLevel(s -> 15).noCollision()
                            .noLootTable().replaceable().instabreak())
            .model(d -> d.models(Models.group()
                            .and("_floor", Models.renderType("minecraft:cutout", Models.parented(
                                    "minecraft:block/template_fire_floor",
                                    Slots.of("fire", "hbm:blocks/balefire", "particle", "hbm:blocks/balefire")))))
                    .states(Variants.select("age", "0", "_floor", "1", "_floor", "2", "_floor", "3", "_floor",
                            "4", "_floor", "5", "_floor", "6", "_floor", "7", "_floor", "8", "_floor", "9", "_floor",
                            "10", "_floor", "11", "_floor", "12", "_floor", "13", "_floor", "14", "_floor",
                            "15", "_floor"))
                    // 1.7 registerBlock gave balefire a tab-less ItemBlock, drawn as the block's flat icon (fire
                    // render type, no 3D item). HBM registers no item, so this is only that look on file: the old
                    // .blockItem() pointed at hbm:block/balefire, which does not exist (only _floor does).
                    .item(Models.generated("hbm:blocks/balefire")));

    /** Digamma flame: light 15, blast resistance 150, eternal, and it never spreads. */
    public static final RegistryObject<BlockDigammaFlame> fire_digamma = REG.block("fire_digamma",
                    BlockDigammaFlame::new, p -> p.strength(0.0F, 150.0F).lightLevel(s -> 15).noCollision()
                            .noLootTable().replaceable().instabreak())
            .transform(Presets.statesOnly("hbm:block/fire_digamma_floor", Variants.simple()));

    /** Digamma matter: unbreakable, blast resistance 18,000,000, spreads seven generations then dies. */
    public static final RegistryObject<BlockDigammaMatter> digamma_matter = REG.block("digamma_matter",
                    BlockDigammaMatter::new, p -> p.strength(-1.0F, 18_000_000.0F).noCollision()
                            .noOcclusion().noLootTable())
            .transform(Presets.shape(Models.renderType("minecraft:translucent",
                            Models.cubeAll("hbm:blocks/digamma_matter")))
                    .states(Variants.uniformRange("meta", 0, 15)).viaItemModel());

    public static final RegistryObject<NukeMan> nuke_man = REG.block(
                    "nuke_man", NukeMan::new,
                    // INVISIBLE render (drawn by a BlockEntityRenderer) -> noOcclusion so it doesn't cull
                    // neighbours or render a missing model.
                    p -> p.strength(5.0F, 6000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(RENDERED_BOMB)
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<NukeN2> nuke_n2 = REG.block(
                    "nuke_n2", NukeN2::new,
                    p -> p.strength(5.0F, 6000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(RENDERED_BOMB)
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<NukeBoy> nuke_boy = REG.block(
                    "nuke_boy", NukeBoy::new,
                    p -> p.strength(5.0F, 6000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(RENDERED_BOMB)
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<NukeGadget> nuke_gadget = REG.block(
                    "nuke_gadget", NukeGadget::new,
                    p -> p.strength(5.0F, 6000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(RENDERED_BOMB)
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<NukeTsar> nuke_tsar = REG.block(
                    "nuke_tsar", NukeTsar::new,
                    p -> p.strength(5.0F, 6000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(RENDERED_BOMB)
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<NukeMike> nuke_mike = REG.block(
                    "nuke_mike", NukeMike::new,
                    p -> p.strength(5.0F, 6000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(RENDERED_BOMB)
            .tag(BlockTag.PICKAXE);

    /** The firework battery. 1.7 sets resistance only, leaving the default 1.0 hardness. */
    public static final RegistryObject<BlockFireworks> fireworks = REG.block(
                    "fireworks", BlockFireworks::new, p -> p.strength(1.0F, 5.0F).sound(SoundType.METAL))
            .transform(Presets.cubeBottomTop("hbm:blocks/fireworks_bottom", "hbm:blocks/fireworks_side",
                    "hbm:blocks/fireworks_top"))
            .loot(Loot.selfAlways());

    public static final RegistryObject<BlockTNT> tnt = REG.block(
                    "tnt_ntm", BlockTNT::new, p -> p.strength(0.0F).sound(SoundType.GRASS).ignitedByLava())
            .transform(tntFamily("tnt"));
    public static final RegistryObject<BlockDynamite> dynamite = REG.block(
                    "dynamite", BlockDynamite::new, p -> p.strength(0.0F).sound(SoundType.GRASS).ignitedByLava())
            .transform(tntFamily("dynamite"));
    public static final RegistryObject<BlockSemtex> semtex = REG.block(
                    "semtex", BlockSemtex::new, p -> p.strength(0.0F).sound(SoundType.GRASS).ignitedByLava())
            .transform(tntFamily("semtex"));
    public static final RegistryObject<BlockC4> c4 = REG.block(
                    "c4", BlockC4::new, p -> p.strength(0.0F).sound(SoundType.GRASS).ignitedByLava())
            .transform(tntFamily("c4"));

    static {
        // BlockTaint's old constructor put its item in the control tab, unlike the missile item itself.
        REG.blockItem(taint, ItemBlockTaint::new, Compat::blockDescription).tab(HbmTab.CONTROL);
        REG.blockItem(nuke_man, (b, p) -> new ItemNukeBlock(b, p, ItemNukeBlock.Type.FAT_MAN),
                Compat::blockDescription).tab(HbmTab.BOMBS);
        REG.blockItem(nuke_n2, (b, p) -> new ItemNukeBlock(b, p, ItemNukeBlock.Type.N2),
                Compat::blockDescription).tab(HbmTab.BOMBS);
        REG.blockItem(nuke_boy, (b, p) -> new ItemNukeBlock(b, p, ItemNukeBlock.Type.LITTLE_BOY),
                Compat::blockDescription).tab(HbmTab.BOMBS);
        REG.blockItem(nuke_gadget, (b, p) -> new ItemNukeBlock(b, p, ItemNukeBlock.Type.GADGET),
                Compat::blockDescription).tab(HbmTab.BOMBS);
        REG.blockItem(nuke_tsar, (b, p) -> new ItemNukeBlock(b, p, ItemNukeBlock.Type.TSAR),
                Compat::blockDescription).tab(HbmTab.BOMBS);
        REG.blockItem(nuke_mike, ItemNukeMikeBlock::new, Compat::blockDescription).tab(HbmTab.BOMBS);
        REG.blockItem(fireworks).tab(HbmTab.BOMBS);
        REG.blockItem(tnt).tab(HbmTab.BOMBS);
        REG.blockItem(dynamite).tab(HbmTab.BOMBS);
        REG.blockItem(semtex).tab(HbmTab.BOMBS);
        REG.blockItem(c4).tab(HbmTab.BOMBS);
    }

    public static void init() {
        ModExplosiveEntities.init();
    }
}
