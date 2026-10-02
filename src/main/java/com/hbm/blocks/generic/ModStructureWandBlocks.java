package com.hbm.blocks.generic;

import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Placement;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
 * {@code hbm:item_variant}, and one block told apart by its {@code mode}. Models, blockstates and loot are stated
 * on each registration (Reg; emitted by Dejson at build time).
 *
 * <p>1.7 looks: {@code wand_air} is {@code RenderBlockWand}'s 1..15 px glass box drawn from outside and from inside
 * (the inner faces are zero-thickness elements facing in), culled against other wand blocks, and its item the flat
 * block icon; {@code wand_structure} is its save or load icon on every face.
 */
public final class ModStructureWandBlocks {

    private ModStructureWandBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    /** Typed register(s): REG picks them without naming a registry key, which 1.20.1 cannot do in the build-time Dejson JVM. */
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.createBlockEntityTypes(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS, BLOCK_ENTITIES);

    public static final String[] IDS = {"wand_air", "wand_loot", "wand_jigsaw", "wand_logic", "wand_tandem", "wand_structure"};

    // 1.7 Material.glass, no hardness
    public static final RegistryObject<BlockWand> wand_air = REG.block("wand_air",
            p -> new BlockWand(p, () -> Blocks.AIR),
            p -> p.instabreak().sound(SoundType.GLASS).noOcclusion().noCollision()
                    .isSuffocating((s, l, pos) -> false).isViewBlocking((s, l, pos) -> false))
            // The 1..15 px box with outside faces plus six inward faces (1.7 renderFromInside) is hand-built
            // geometry, so its model is authored: assets/hbm/models/block/wand_air.json (cutout).
            .transform(Presets.statesOnly(Variants.simple()))
            .loot(Loot.self());
    // 1.7 Material.iron, no hardness
    public static final RegistryObject<BlockWandLoot> wand_loot = REG.block("wand_loot",
            BlockWandLoot::new, p -> p.instabreak().sound(SoundType.METAL))
            .transform(topRotated())
            .loot(Loot.self());
    public static final RegistryObject<BlockWandJigsaw> wand_jigsaw = REG.block("wand_jigsaw",
            BlockWandJigsaw::new, p -> p.instabreak().sound(SoundType.METAL))
            .transform(oriented())
            .loot(Loot.self());
    public static final RegistryObject<BlockWandLogic> wand_logic = REG.block("wand_logic",
            BlockWandLogic::new, p -> p.instabreak().sound(SoundType.METAL))
            .transform(topRotated())
            .loot(Loot.self());
    public static final RegistryObject<BlockWandTandem> wand_tandem = REG.block("wand_tandem",
            BlockWandTandem::new, p -> p.instabreak().sound(SoundType.METAL))
            .transform(oriented())
            .loot(Loot.self());
    public static final RegistryObject<BlockWandStructure> wand_structure = REG.block("wand_structure",
            BlockWandStructure::new, p -> p.instabreak().sound(SoundType.METAL))
            .transform(Presets.shape(Models.group()
                            .and("", Models.cubeAll("hbm:blocks/wand_structure_save"))
                            .and("_load", Models.cubeAll("hbm:blocks/wand_structure_load")))
                    .states(Variants.select("mode", "save", "", "load", "_load"))
                    .noItem())
            .loot(Loot.self());

    static {
        REG.blockItem(wand_air, Compat::blockDescription).transform(Presets.sprite("hbm:blocks/wand_air"));
        tooltipItem("wand_loot", wand_loot, List.of(
                Component.literal("Define loot crates/piles in .nbt structures"),
                Component.literal("Use screwdriver to increase/decrease minimum loot").withStyle(ChatFormatting.GOLD),
                Component.literal("Use hand drill to increase/decrease maximum loot").withStyle(ChatFormatting.GOLD),
                Component.literal("Use defuser to cycle loot types").withStyle(ChatFormatting.GOLD),
                Component.literal("Use container block to set the block that spawns with loot inside").withStyle(ChatFormatting.GOLD)));
        REG.blockItem(wand_jigsaw, Compat::blockDescription);
        tooltipItem("wand_logic", wand_logic, List.of(
                Component.literal("Use screwdriver to cycle forwards through the action list, shift click to go back").withStyle(ChatFormatting.GOLD),
                Component.literal("Use defuser to cycle forwards through the condition list, shift click to go back").withStyle(ChatFormatting.GOLD),
                Component.literal("Use hand drill to cycle forwards through the interaction list, shift click to go back").withStyle(ChatFormatting.GOLD),
                Component.literal("Use a detonator to transform").withStyle(ChatFormatting.YELLOW)));
        REG.blockItem(wand_tandem, Compat::blockDescription);
        REG.item("wand_structure", p -> new MaterialCompletionBlockItem(wand_structure.get(), p,
                        "wand_structure", 2, i -> i == 1 ? "load" : "save"), Compat::blockDescription)
                .icon(d -> d.models(Models.group()
                                .and("", Models.blockItem("hbm:block/wand_structure"))
                                .and("_load", Models.blockItem("hbm:block/wand_structure_load")))
                        .selects(Constants.MOD_ID + ":item_variant"));
    }

    private static void tooltipItem(String id, RegistryObject<? extends Block> block, List<Component> lines) {
        REG.item(id, p -> new CompatBlockItem(block.get(), p) {
            @Override
            protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                                 TooltipFlag flag) {
                tooltip.addAll(lines);
            }
        }, Compat::blockDescription);
    }

    public static final RegistryObject<BlockEntityType<BlockWandLoot.TileEntityWandLoot>> WAND_LOOT_ENTITY =
            REG.blockEntity("tileentity_wand_loot", () -> BlockEntities.serverTicking(
                    BlockWandLoot.TileEntityWandLoot::new, BlockWandLoot.TileEntityWandLoot::serverTick, wand_loot.get()));
    public static final RegistryObject<BlockEntityType<BlockWandJigsaw.TileEntityWandJigsaw>> WAND_JIGSAW_ENTITY =
            REG.blockEntity("tileentity_wand_jigsaw", () -> BlockEntities.serverTicking(
                    BlockWandJigsaw.TileEntityWandJigsaw::new, BlockWandJigsaw.TileEntityWandJigsaw::serverTick, wand_jigsaw.get()));
    public static final RegistryObject<BlockEntityType<BlockWandLogic.TileEntityWandLogic>> WAND_LOGIC_ENTITY =
            REG.blockEntity("tileentity_wand_spawner", () -> BlockEntities.serverTicking(
                    BlockWandLogic.TileEntityWandLogic::new, BlockWandLogic.TileEntityWandLogic::serverTick, wand_logic.get()));
    public static final RegistryObject<BlockEntityType<BlockWandTandem.TileEntityWandTandem>> WAND_TANDEM_ENTITY =
            REG.blockEntity("tileentity_wand_tandem", () -> BlockEntities.serverTicking(
                    BlockWandTandem.TileEntityWandTandem::new, BlockWandTandem.TileEntityWandTandem::serverTick, wand_tandem.get()));
    public static final RegistryObject<BlockEntityType<BlockWandStructure.TileEntityWandStructure>> WAND_STRUCTURE_ENTITY =
            REG.blockEntity("tileentity_wand_structure", () -> BlockEntities.serverTicking(
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

    /**
     * {@code wand_loot}/{@code wand_logic}: their {@code _top} icon on both ends, turned with the 1.7 metadata
     * ({@code IBlockSideRotation}).
     */
    private static Preset topRotated() {
        return handle -> {
            String id = handle.path();
            Map<String, List<Placement>> rotations = new LinkedHashMap<>();
            for (int r = 0; r < 4; r++) rotations.put("rotation=" + r, List.of(Placement.rotated(0, r * 90, false)));
            handle.transform(Presets.cubeBottomTop("hbm:blocks/" + id + "_top", "hbm:blocks/" + id, "hbm:blocks/" + id + "_top")
                    .states(Variants.of(Set.of("rotation"), rotations)));
        };
    }

    /**
     * {@code wand_jigsaw}/{@code wand_tandem}: the front on the facing side, the back opposite, {@code _top} on the
     * ends and {@code _side} around (for an up/down facing the {@code _top} icon moves to west/east, as 1.7's
     * {@code getIcon} did).
     */
    private static Preset oriented() {
        return handle -> {
            String front = "hbm:blocks/" + handle.path(), back = front + "_back", top = front + "_top", side = front + "_side";
            Map<String, List<Placement>> facings = new LinkedHashMap<>();
            facings.put("facing=north", List.of(Placement.NONE));
            facings.put("facing=east", List.of(Placement.rotated(0, 90, false)));
            facings.put("facing=south", List.of(Placement.rotated(0, 180, false)));
            facings.put("facing=west", List.of(Placement.rotated(0, 270, false)));
            facings.put("facing=up", List.of(Placement.model("_vertical")));
            facings.put("facing=down", List.of(Placement.model("_vertical").plus(Placement.rotated(180, 0, false))));
            handle.transform(Presets.shape(Models.group()
                            .and("", Models.parented("minecraft:block/cube", Slots.of("particle", front)
                                    .and("north", front).and("south", back).and("up", top).and("down", top)
                                    .and("east", side).and("west", side)))
                            .and("_vertical", Models.parented("minecraft:block/cube", Slots.of("particle", front)
                                    .and("up", front).and("down", back).and("north", side).and("south", side)
                                    .and("east", top).and("west", top))))
                    .states(Variants.of(Set.of("facing"), facings)));
        };
    }

    public static void init() {}
}
