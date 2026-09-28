package com.hbm.blocks.generic;

import com.hbm.Constants;
import com.hbm.blocks.material.MaterialCompletionBlockItem;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlockItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The structure-editing blocks of 1.7 {@code ModBlocks} ({@code wand_air}, {@code wand_loot}, {@code wand_jigsaw},
 * {@code wand_logic}, {@code wand_tandem}, {@code wand_structure}) and their block entities under 1.7
 * {@code TileMappings}' names. 1.7 gave them no creative tab and no hardness (they break instantly); the port does
 * the same. {@code wand_structure}'s two 1.7 subtypes (save 0, load 1) are one item told apart by
 * {@code hbm:item_variant}, and one block told apart by its {@code mode}.
 */
public final class ModStructureWandBlocks {

    private ModStructureWandBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final String[] IDS = {"wand_air", "wand_loot", "wand_jigsaw", "wand_logic", "wand_tandem", "wand_structure"};

    // 1.7 Material.glass, no hardness
    public static final RegistryObject<BlockWand> wand_air = BLOCKS.registerBlock("wand_air",
            p -> new BlockWand(p, () -> Blocks.AIR),
            p -> p.instabreak().sound(SoundType.GLASS).noOcclusion().noCollision()
                    .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false));
    // 1.7 Material.iron, no hardness
    public static final RegistryObject<BlockWandLoot> wand_loot = BLOCKS.registerBlock("wand_loot",
            BlockWandLoot::new, p -> p.instabreak().sound(SoundType.METAL));
    public static final RegistryObject<BlockWandJigsaw> wand_jigsaw = BLOCKS.registerBlock("wand_jigsaw",
            BlockWandJigsaw::new, p -> p.instabreak().sound(SoundType.METAL));
    public static final RegistryObject<BlockWandLogic> wand_logic = BLOCKS.registerBlock("wand_logic",
            BlockWandLogic::new, p -> p.instabreak().sound(SoundType.METAL));
    public static final RegistryObject<BlockWandTandem> wand_tandem = BLOCKS.registerBlock("wand_tandem",
            BlockWandTandem::new, p -> p.instabreak().sound(SoundType.METAL));
    public static final RegistryObject<BlockWandStructure> wand_structure = BLOCKS.registerBlock("wand_structure",
            BlockWandStructure::new, p -> p.instabreak().sound(SoundType.METAL));

    static {
        ITEMS.registerBlockItem("wand_air", wand_air, Compat::blockDescription);
        tooltipItem("wand_loot", wand_loot, List.of(
                Component.literal("Define loot crates/piles in .nbt structures"),
                Component.literal("Use screwdriver to increase/decrease minimum loot").withStyle(ChatFormatting.GOLD),
                Component.literal("Use hand drill to increase/decrease maximum loot").withStyle(ChatFormatting.GOLD),
                Component.literal("Use defuser to cycle loot types").withStyle(ChatFormatting.GOLD),
                Component.literal("Use container block to set the block that spawns with loot inside").withStyle(ChatFormatting.GOLD)));
        ITEMS.registerBlockItem("wand_jigsaw", wand_jigsaw, Compat::blockDescription);
        tooltipItem("wand_logic", wand_logic, List.of(
                Component.literal("Use screwdriver to cycle forwards through the action list, shift click to go back").withStyle(ChatFormatting.GOLD),
                Component.literal("Use defuser to cycle forwards through the condition list, shift click to go back").withStyle(ChatFormatting.GOLD),
                Component.literal("Use hand drill to cycle forwards through the interaction list, shift click to go back").withStyle(ChatFormatting.GOLD),
                Component.literal("Use a detonator to transform").withStyle(ChatFormatting.YELLOW)));
        ITEMS.registerBlockItem("wand_tandem", wand_tandem, Compat::blockDescription);
        ITEMS.registerItem("wand_structure", p -> new MaterialCompletionBlockItem(wand_structure.get(), p,
                "wand_structure", 2, i -> i == 1 ? "load" : "save"), Compat::blockDescription);
    }

    private static void tooltipItem(String id, RegistryObject<? extends Block> block, List<Component> lines) {
        ITEMS.registerItem(id, p -> new CompatBlockItem(block.get(), p) {
            @Override
            protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                                 TooltipFlag flag) {
                tooltip.addAll(lines);
            }
        }, Compat::blockDescription);
    }

    public static final RegistryObject<BlockEntityType<BlockWandLoot.TileEntityWandLoot>> WAND_LOOT_ENTITY =
            BLOCK_ENTITIES.register("tileentity_wand_loot", () -> BlockEntities.serverTicking(
                    BlockWandLoot.TileEntityWandLoot::new, BlockWandLoot.TileEntityWandLoot::serverTick, wand_loot.get()));
    public static final RegistryObject<BlockEntityType<BlockWandJigsaw.TileEntityWandJigsaw>> WAND_JIGSAW_ENTITY =
            BLOCK_ENTITIES.register("tileentity_wand_jigsaw", () -> BlockEntities.serverTicking(
                    BlockWandJigsaw.TileEntityWandJigsaw::new, BlockWandJigsaw.TileEntityWandJigsaw::serverTick, wand_jigsaw.get()));
    public static final RegistryObject<BlockEntityType<BlockWandLogic.TileEntityWandLogic>> WAND_LOGIC_ENTITY =
            BLOCK_ENTITIES.register("tileentity_wand_spawner", () -> BlockEntities.serverTicking(
                    BlockWandLogic.TileEntityWandLogic::new, BlockWandLogic.TileEntityWandLogic::serverTick, wand_logic.get()));
    public static final RegistryObject<BlockEntityType<BlockWandTandem.TileEntityWandTandem>> WAND_TANDEM_ENTITY =
            BLOCK_ENTITIES.register("tileentity_wand_tandem", () -> BlockEntities.serverTicking(
                    BlockWandTandem.TileEntityWandTandem::new, BlockWandTandem.TileEntityWandTandem::serverTick, wand_tandem.get()));
    public static final RegistryObject<BlockEntityType<BlockWandStructure.TileEntityWandStructure>> WAND_STRUCTURE_ENTITY =
            BLOCK_ENTITIES.register("tileentity_wand_structure", () -> BlockEntities.serverTicking(
                    BlockWandStructure.TileEntityWandStructure::new, BlockWandStructure.TileEntityWandStructure::serverTick, wand_structure.get()));

    /** 1.7 {@code ModBlocks.getBlockFromStack}: the block a held block item places, or null. */
    public static @Nullable Block getBlockFromStack(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item ? item.getBlock() : null;
    }

    /** 1.7 {@code ModBlocks.isStructureBlock}: the structure wand blocks, {@code wand_air} only when asked. */
    public static boolean isStructureBlock(Block block, boolean includeAir) {
        if (block == wand_air.get()) return includeAir;
        return block == wand_structure.get() || block == wand_jigsaw.get() || block == wand_logic.get()
                || block == wand_tandem.get() || block == wand_loot.get();
    }

    public static void init() {
        com.hbm.blocks.StructureWandDeclarations.declare();
    }
}
