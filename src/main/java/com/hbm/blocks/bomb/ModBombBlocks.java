package com.hbm.blocks.bomb;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.block.ItemBlockTaint;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
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

    /** Taint residue: a hard, spreading block with a shallow collision box. */
    public static final RegistryObject<BlockTaint> taint = BLOCKS.registerBlock("taint", BlockTaint::new,
            p -> p.mapColor(MapColor.COLOR_GRAY).strength(15.0F, 10.0F).sound(SoundType.METAL)
                    .randomTicks().noLootTable());

    // --- the DRX/GAMMA burn products. No creative tab upstream (setCreativeTab(null)) and no block
    // item: these exist only to be PLACED by a balefire detonation or an FEL beam. ---

    /** Balefire: light 15, hardness 0, spreads violently and irradiates anything standing in it. */
    public static final RegistryObject<BlockBalefire> balefire = BLOCKS.registerBlock("balefire",
            BlockBalefire::new, p -> p.strength(0.0F).lightLevel(s -> 15).noCollision()
                    .noLootTable().replaceable().instabreak());

    /** Digamma flame: light 15, blast resistance 150, eternal, and it never spreads. */
    public static final RegistryObject<BlockDigammaFlame> fire_digamma = BLOCKS.registerBlock("fire_digamma",
            BlockDigammaFlame::new, p -> p.strength(0.0F, 150.0F).lightLevel(s -> 15).noCollision()
                    .noLootTable().replaceable().instabreak());

    /** Digamma matter: unbreakable, blast resistance 18,000,000, spreads seven generations then dies. */
    public static final RegistryObject<BlockDigammaMatter> digamma_matter = BLOCKS.registerBlock("digamma_matter",
            BlockDigammaMatter::new, p -> p.strength(-1.0F, 18_000_000.0F).noCollision()
                    .noOcclusion().noLootTable());

    public static final RegistryObject<NukeMan> nuke_man = BLOCKS.registerBlock(
            "nuke_man", NukeMan::new,
            // INVISIBLE render (drawn by a BlockEntityRenderer) -> noOcclusion so it doesn't cull
            // neighbours or render a missing model.
            p -> p.strength(5.0F, 6000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<NukeN2> nuke_n2 = BLOCKS.registerBlock(
            "nuke_n2", NukeN2::new,
            p -> p.strength(5.0F, 6000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<NukeBoy> nuke_boy = BLOCKS.registerBlock(
            "nuke_boy", NukeBoy::new,
            p -> p.strength(5.0F, 6000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<NukeGadget> nuke_gadget = BLOCKS.registerBlock(
            "nuke_gadget", NukeGadget::new,
            p -> p.strength(5.0F, 6000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<NukeTsar> nuke_tsar = BLOCKS.registerBlock(
            "nuke_tsar", NukeTsar::new,
            p -> p.strength(5.0F, 6000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    public static final RegistryObject<NukeMike> nuke_mike = BLOCKS.registerBlock(
            "nuke_mike", NukeMike::new,
            p -> p.strength(5.0F, 6000.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

    /** The firework battery. 1.7 sets resistance only, leaving the default 1.0 hardness. */
    public static final RegistryObject<BlockFireworks> fireworks = BLOCKS.registerBlock(
            "fireworks", BlockFireworks::new, p -> p.strength(1.0F, 5.0F).sound(SoundType.METAL));

    public static final RegistryObject<BlockTNT> tnt = BLOCKS.registerBlock(
            "tnt_ntm", BlockTNT::new, p -> p.strength(0.0F).sound(SoundType.GRASS).ignitedByLava());
    public static final RegistryObject<BlockDynamite> dynamite = BLOCKS.registerBlock(
            "dynamite", BlockDynamite::new, p -> p.strength(0.0F).sound(SoundType.GRASS).ignitedByLava());
    public static final RegistryObject<BlockSemtex> semtex = BLOCKS.registerBlock(
            "semtex", BlockSemtex::new, p -> p.strength(0.0F).sound(SoundType.GRASS).ignitedByLava());
    public static final RegistryObject<BlockC4> c4 = BLOCKS.registerBlock(
            "c4", BlockC4::new, p -> p.strength(0.0F).sound(SoundType.GRASS).ignitedByLava());

    static {
        RegistryObject<ItemBlockTaint> taintItem = ITEMS.registerItem("taint",
                p -> new ItemBlockTaint(taint.get(), p), Compat::blockDescription);
        // BlockTaint's old constructor put its item in the control tab, unlike the missile item itself.
        ModTabs.addTo(HbmTab.CONTROL, taintItem);
        RegistryObject<? extends BlockItem> item = ITEMS.registerItem("nuke_man",
                p -> new ItemNukeBlock(nuke_man.get(), p, ItemNukeBlock.Type.FAT_MAN), Compat::blockDescription);
        ModTabs.addTo(HbmTab.BOMBS, item);
        item = ITEMS.registerItem("nuke_n2",
                p -> new ItemNukeBlock(nuke_n2.get(), p, ItemNukeBlock.Type.N2), Compat::blockDescription);
        ModTabs.addTo(HbmTab.BOMBS, item);
        item = ITEMS.registerItem("nuke_boy",
                p -> new ItemNukeBlock(nuke_boy.get(), p, ItemNukeBlock.Type.LITTLE_BOY), Compat::blockDescription);
        ModTabs.addTo(HbmTab.BOMBS, item);
        item = ITEMS.registerItem("nuke_gadget",
                p -> new ItemNukeBlock(nuke_gadget.get(), p, ItemNukeBlock.Type.GADGET), Compat::blockDescription);
        ModTabs.addTo(HbmTab.BOMBS, item);
        item = ITEMS.registerItem("nuke_tsar",
                p -> new ItemNukeBlock(nuke_tsar.get(), p, ItemNukeBlock.Type.TSAR), Compat::blockDescription);
        ModTabs.addTo(HbmTab.BOMBS, item);
        item = ITEMS.registerItem("nuke_mike", p -> new ItemNukeMikeBlock(nuke_mike.get(), p),
                Compat::blockDescription);
        ModTabs.addTo(HbmTab.BOMBS, item);
        item = ITEMS.registerBlockItem("fireworks", fireworks);
        ModTabs.addTo(HbmTab.BOMBS, item);
        item = ITEMS.registerBlockItem("tnt_ntm", tnt);
        ModTabs.addTo(HbmTab.BOMBS, item);
        item = ITEMS.registerBlockItem("dynamite", dynamite);
        ModTabs.addTo(HbmTab.BOMBS, item);
        item = ITEMS.registerBlockItem("semtex", semtex);
        ModTabs.addTo(HbmTab.BOMBS, item);
        item = ITEMS.registerBlockItem("c4", c4);
        ModTabs.addTo(HbmTab.BOMBS, item);
    }

    public static void init() {
        com.hbm.entity.explosive.ModExplosiveEntities.init();
    }
}
