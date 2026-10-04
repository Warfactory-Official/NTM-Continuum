package com.hbm.items.tool;

import com.hbm.blocks.generic.ModStructureWandBlocks;
import com.hbm.items.ModDataComponents;
import com.hbm.util.SavedIds;
import net.minecraft.world.level.block.Blocks;
import dev.golem.core.data.Tags;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 1.7 {@code ItemWand} ({@code wand_k}, "Construction Wand"): a creative box-fill tool. Crouch-click a block to save
 * it, click one corner, click the other corner and the whole box is set to the saved block. Crouch-click air to save
 * air (1.7 wrote block id 0).
 *
 * <p>The stack keeps 1.7's NBT keys in the persistent-data component. 1.7 saved {@code block} (numeric block id) and
 * {@code meta}; the flattened equivalent of that pair is one block state, so {@code block} holds the global
 * block-state id ({@link Block#getId}) and {@code meta} stays 0. Both are just as world-local as 1.7's numeric ids,
 * and state id 0 is air exactly as block id 0 was.
 *
 * <p>Odd but kept: {@code 0, 0, 0} is the "no position" sentinel, so a corner clicked at the world origin is
 * forgotten; the fill uses setBlock flag 3 (update + notify) for every block, with no size limit.
 *
 * <p>1.7 decided the chat message on the client from the client's copy of the NBT and did the fill on the server.
 * Here the server owns the state and sends the same message, so the two cannot disagree.
 */
public class ItemWand extends CompatItem {

    public ItemWand(Properties properties) {
        super(properties);
    }

    private static CompoundTag data(ItemStack stack) {
        return ModDataComponents.PERSISTENT_DATA.get(stack).map(CompoundTag::copy).orElse(new CompoundTag());
    }

    private static String blockName(BlockState state) {
        return state.getBlock().getDescriptionId();
    }

    /** The saved block, by state name (1.7 kept a block id + meta; a numeric state id is never saved, History 19ej-ids). */
    private static BlockState savedBlock(CompoundTag tag) {
        return SavedIds.getState(tag, "block", Blocks.AIR.defaultBlockState());
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Creative-only item"));
        list.add(Component.literal("\"Destruction brings creation\""));
        list.add(Component.literal("(Set positions with right click,"));
        list.add(Component.literal("set block with shift-right click!)"));

        CompoundTag tag = ModDataComponents.PERSISTENT_DATA.get(stack).orElse(null);
        int x = tag == null ? 0 : Tags.getIntOr(tag, "x", 0);
        int y = tag == null ? 0 : Tags.getIntOr(tag, "y", 0);
        int z = tag == null ? 0 : Tags.getIntOr(tag, "z", 0);

        if (tag != null && !(x == 0 && y == 0 && z == 0)) {
            list.add(Component.literal("Pos: " + x + ", " + y + ", " + z));
        } else {
            list.add(Component.literal("Positions not set!"));
        }
        if (tag != null) {
            list.add(Component.literal("Block saved: " + blockName(savedBlock(tag))));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        CompoundTag tag = data(stack);

        if (player.isShiftKeyDown()) {
            SavedIds.putState(tag, "block", level.getBlockState(pos));
            tag.putInt("meta", 0);
            player.sendSystemMessage(Component.literal("Set block " + blockName(savedBlock(tag))));
        } else {
            if (Tags.getIntOr(tag, "x", 0) == 0 && Tags.getIntOr(tag, "y", 0) == 0 && Tags.getIntOr(tag, "z", 0) == 0) {
                tag.putInt("x", x);
                tag.putInt("y", y);
                tag.putInt("z", z);
                player.sendSystemMessage(Component.literal("Position set!"));
            } else {
                int ox = Tags.getIntOr(tag, "x", 0);
                int oy = Tags.getIntOr(tag, "y", 0);
                int oz = Tags.getIntOr(tag, "z", 0);

                tag.putInt("x", 0);
                tag.putInt("y", 0);
                tag.putInt("z", 0);

                BlockState state = savedBlock(tag);
                // 1.7: block == ModBlocks.wand_air only fills air
                boolean replaceAir = state.is(ModStructureWandBlocks.wand_air.get());
                BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

                for (int i = Math.min(ox, x); i <= Math.max(ox, x); i++) {
                    for (int j = Math.min(oy, y); j <= Math.max(oy, y); j++) {
                        for (int k = Math.min(oz, z); k <= Math.max(oz, z); k++) {
                            cursor.set(i, j, k);
                            if (replaceAir && !level.getBlockState(cursor).isAir()) continue;
                            level.setBlock(cursor, state, 3);
                        }
                    }
                }
                player.sendSystemMessage(Component.literal("Selection filled!"));
            }
        }

        ModDataComponents.PERSISTENT_DATA.set(stack, tag);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        CompoundTag tag = data(stack);

        if (player.isShiftKeyDown() && !level.isClientSide()) {
            SavedIds.putState(tag, "block", Blocks.AIR.defaultBlockState()); // 1.7 block id 0 = air
            tag.putInt("meta", 0);
            player.sendSystemMessage(Component.literal("Set block " + blockName(Blocks.AIR.defaultBlockState())));
        }
        // 1.7 created an empty tag on any right-click, which makes the "Block saved" tooltip line appear.
        if (!level.isClientSide()) ModDataComponents.PERSISTENT_DATA.set(stack, tag);

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
