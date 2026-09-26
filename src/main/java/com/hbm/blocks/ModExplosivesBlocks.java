package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import com.hbm.blocks.bomb.BlockChargeBase;
import com.hbm.blocks.bomb.BlockChargeC4;
import com.hbm.blocks.bomb.BlockChargeDynamite;
import com.hbm.blocks.bomb.BlockChargeMiner;
import com.hbm.blocks.bomb.BlockChargeSemtex;
import com.hbm.blocks.bomb.BlockCrashedBomb;
import com.hbm.blocks.bomb.BlockFissureBomb;
import com.hbm.blocks.bomb.BlockPlasticExplosive;
import com.hbm.blocks.bomb.BombMulti;
import com.hbm.blocks.bomb.DetCord;
import com.hbm.blocks.bomb.DetMiner;
import com.hbm.blocks.bomb.ExplosiveCharge;
import com.hbm.blocks.bomb.Landmine;
import com.hbm.items.block.ExplosivesBlockItem;

/**
 * Blocks of the conventional-explosives family: landmines, timed charges, redstone-fired det blocks,
 * the solid plastic-explosive blocks, the fissure bomb, the crashed duds and the multi purpose bomb.
 * Properties follow the 1.7 {@code ModBlocks} lines: {@code Material.iron} blocks require a pickaxe for
 * their drop (and are in the mineable tag), {@code Material.tnt} ones do not.
 */
public final class ModExplosivesBlocks {
    private ModExplosivesBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    // --- landmines: Material.iron, hardness 1.0 (resistance 5), no drop ---
    public static final RegistryObject<Landmine> mine_ap = BLOCKS.registerBlock("mine_ap",
            p -> new Landmine(p, Landmine.Type.AP, 1.5D, 1D), ModExplosivesBlocks::mine);
    public static final RegistryObject<Landmine> mine_he = BLOCKS.registerBlock("mine_he",
            p -> new Landmine(p, Landmine.Type.HE, 2D, 5D), ModExplosivesBlocks::mine);
    public static final RegistryObject<Landmine> mine_shrap = BLOCKS.registerBlock("mine_shrap",
            p -> new Landmine(p, Landmine.Type.SHRAP, 1.5D, 1D), ModExplosivesBlocks::mine);
    public static final RegistryObject<Landmine> mine_fat = BLOCKS.registerBlock("mine_fat",
            p -> new Landmine(p, Landmine.Type.FAT, 2.5D, 1D), ModExplosivesBlocks::mine);
    public static final RegistryObject<Landmine> mine_naval = BLOCKS.registerBlock("mine_naval",
            p -> new Landmine(p, Landmine.Type.NAVAL, 2.5D, 1D), ModExplosivesBlocks::mine);

    // --- timed charges: Material.tnt, hardness 0, resistance 1, no drop ---
    public static final RegistryObject<BlockChargeDynamite> charge_dynamite = BLOCKS.registerBlock("charge_dynamite",
            BlockChargeDynamite::new, ModExplosivesBlocks::charge);
    public static final RegistryObject<BlockChargeMiner> charge_miner = BLOCKS.registerBlock("charge_miner",
            BlockChargeMiner::new, ModExplosivesBlocks::charge);
    public static final RegistryObject<BlockChargeC4> charge_c4 = BLOCKS.registerBlock("charge_c4",
            BlockChargeC4::new, ModExplosivesBlocks::charge);
    public static final RegistryObject<BlockChargeSemtex> charge_semtex = BLOCKS.registerBlock("charge_semtex",
            BlockChargeSemtex::new, ModExplosivesBlocks::charge);

    // --- redstone-fired det blocks: Material.iron, hardness 0.1, resistance 0 ---
    public static final RegistryObject<DetCord> det_cord = BLOCKS.registerBlock("det_cord", DetCord::new,
            p -> det(p).noOcclusion());
    public static final RegistryObject<ExplosiveCharge> det_charge = BLOCKS.registerBlock("det_charge",
            p -> new ExplosiveCharge(p, ExplosiveCharge.Kind.CHARGE), ModExplosivesBlocks::det);
    public static final RegistryObject<ExplosiveCharge> det_nuke = BLOCKS.registerBlock("det_nuke",
            p -> new ExplosiveCharge(p, ExplosiveCharge.Kind.NUKE), ModExplosivesBlocks::det);
    public static final RegistryObject<DetMiner> det_miner = BLOCKS.registerBlock("det_miner", DetMiner::new,
            ModExplosivesBlocks::det);

    // --- solid plastic explosive: Material.tnt, metal sound, hardness 2, resistance 2 ---
    public static final RegistryObject<BlockPlasticExplosive> block_semtex = BLOCKS.registerBlock("block_semtex",
            BlockPlasticExplosive::new, p -> p.strength(2.0F, 2.0F).sound(SoundType.METAL));
    public static final RegistryObject<BlockPlasticExplosive> block_c4 = BLOCKS.registerBlock("block_c4",
            BlockPlasticExplosive::new, p -> p.strength(2.0F, 2.0F).sound(SoundType.METAL));

    // --- fissure bomb: BlockTNTBase, hardness 0, grass sound ---
    public static final RegistryObject<BlockFissureBomb> fissure_bomb = BLOCKS.registerBlock("fissure_bomb",
            BlockFissureBomb::new, p -> p.strength(0.0F).sound(SoundType.GRASS).ignitedByLava());

    // --- crashed duds: Material.iron, unbreakable, resistance 6000; the 1.7 metadata enum flattened ---
    public static final RegistryObject<BlockCrashedBomb> crashed_bomb_balefire = BLOCKS.registerBlock("crashed_bomb_balefire",
            p -> new BlockCrashedBomb(p, BlockCrashedBomb.EnumDudType.BALEFIRE), ModExplosivesBlocks::dud);
    public static final RegistryObject<BlockCrashedBomb> crashed_bomb_conventional = BLOCKS.registerBlock("crashed_bomb_conventional",
            p -> new BlockCrashedBomb(p, BlockCrashedBomb.EnumDudType.CONVENTIONAL), ModExplosivesBlocks::dud);
    public static final RegistryObject<BlockCrashedBomb> crashed_bomb_nuke = BLOCKS.registerBlock("crashed_bomb_nuke",
            p -> new BlockCrashedBomb(p, BlockCrashedBomb.EnumDudType.NUKE), ModExplosivesBlocks::dud);
    public static final RegistryObject<BlockCrashedBomb> crashed_bomb_salted = BLOCKS.registerBlock("crashed_bomb_salted",
            p -> new BlockCrashedBomb(p, BlockCrashedBomb.EnumDudType.SALTED), ModExplosivesBlocks::dud);

    // --- multi purpose bomb: Material.iron, resistance 200 (hardness left at 0 upstream) ---
    public static final RegistryObject<BombMulti> bomb_multi = BLOCKS.registerBlock("bomb_multi", BombMulti::new,
            p -> p.strength(0.0F, 200.0F).requiresCorrectToolForDrops().noOcclusion());

    private static BlockBehaviour.Properties mine(BlockBehaviour.Properties p) {
        return p.strength(1.0F, 5.0F).noOcclusion().noLootTable();
    }

    private static BlockBehaviour.Properties charge(BlockBehaviour.Properties p) {
        return p.strength(0.0F, 1.0F).noOcclusion().noLootTable();
    }

    private static BlockBehaviour.Properties det(BlockBehaviour.Properties p) {
        return p.strength(0.1F, 0.0F).requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties dud(BlockBehaviour.Properties p) {
        return p.strength(-1.0F, 6000.0F).noOcclusion().noLootTable();
    }

    static {
        for (RegistryObject<Landmine> mine : List.of(mine_ap, mine_he, mine_shrap, mine_fat, mine_naval)) {
            ModTabs.addTo(HbmTab.BOMBS, ITEMS.registerBlockItem(mine.getId().getPath(), mine));
        }
        charge("charge_dynamite", charge_dynamite);
        charge("charge_miner", charge_miner);
        charge("charge_c4", charge_c4);
        charge("charge_semtex", charge_semtex);
        for (RegistryObject<? extends net.minecraft.world.level.block.Block> det : List.of(det_cord, det_charge, det_nuke, det_miner)) {
            ModTabs.addTo(HbmTab.BOMBS, ITEMS.registerBlockItem(det.getId().getPath(), det));
        }
        ModTabs.addTo(HbmTab.BLOCKS, ITEMS.registerBlockItem("block_semtex", block_semtex));
        ModTabs.addTo(HbmTab.BLOCKS, ITEMS.registerBlockItem("block_c4", block_c4));
        ModTabs.addTo(HbmTab.BOMBS, ITEMS.registerBlockItem("fissure_bomb", fissure_bomb));
        for (RegistryObject<BlockCrashedBomb> dud : List.of(crashed_bomb_balefire, crashed_bomb_conventional,
                crashed_bomb_nuke, crashed_bomb_salted)) {
            ModTabs.addTo(HbmTab.BOMBS, ITEMS.registerBlockItem(dud.getId().getPath(), dud));
        }
        ModTabs.addTo(HbmTab.BOMBS, ITEMS.registerBlockItem("bomb_multi", bomb_multi));
    }

    private static void charge(String id, RegistryObject<? extends BlockChargeBase> block) {
        Supplier<List<Component>> lines = () -> block.get().tooltip();
        RegistryObject<? extends BlockItem> item = ITEMS.registerItem(id,
                p -> new ExplosivesBlockItem(block.get(), p, lines), Compat::blockDescription);
        ModTabs.addTo(HbmTab.BOMBS, item);
    }

    public static void init() {}
}
