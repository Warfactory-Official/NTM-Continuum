package com.hbm.items.tool;

import com.hbm.blocks.generic.BlockWandStructure.TileEntityWandStructure;
import com.hbm.blocks.generic.ModStructureWandBlocks;
import com.hbm.items.ModDataComponents;
import dev.golem.core.data.Tags;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 1.7 {@code ItemWandS} ({@code wand_s}, "Structure Wand"). Two plain clicks mark a box and drop a configured
 * {@code wand_structure} save block one below its minimum corner; crouch-clicking a block toggles it on the
 * save blacklist; crouch-clicking air clears the blacklist. The custom machine controller also builds its
 * structure when clicked with this wand ({@code BlockCustomMachine}).
 *
 * <p>The blacklist keeps 1.7's {@code blocks}/{@code metas} int arrays. A 1.7 (block, meta) pair is one flattened
 * block state, so {@code blocks} holds global block-state ids ({@link Block#getId}) and {@code metas} is all zero.
 *
 * <p>The second click places a {@code wand_structure} saving block one below the box's minimum corner and hands it
 * the box size and this wand's blacklist; its screen then saves the box as an {@code NBTStructure} file.
 */
public class ItemWandS extends CompatItem {

    public ItemWandS(Properties properties) {
        super(properties);
    }

    private static CompoundTag data(ItemStack stack) {
        return ModDataComponents.PERSISTENT_DATA.get(stack).map(CompoundTag::copy).orElse(new CompoundTag());
    }

    private static String blockName(int stateId) {
        return Block.stateById(stateId).getBlock().getDescriptionId();
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Creative-only item"));
        list.add(Component.literal("\"Replication breeds decadence\""));
        list.add(Component.literal("(Saves an area defined by two right-clicks,"));
        list.add(Component.literal("adds a block to the blacklist by crouch right-clicking!)"));

        CompoundTag tag = ModDataComponents.PERSISTENT_DATA.get(stack).orElse(null);
        if (tag == null) return;

        int px = Tags.getIntOr(tag, "x", 0);
        int py = Tags.getIntOr(tag, "y", 0);
        int pz = Tags.getIntOr(tag, "z", 0);

        if (px != 0 || py != 0 || pz != 0) {
            list.add(Component.literal("From: " + px + ", " + py + ", " + pz).withStyle(ChatFormatting.AQUA));
        } else {
            list.add(Component.literal("No start position set").withStyle(ChatFormatting.AQUA));
        }

        Set<Integer> blocks = getBlocks(tag);
        if (!blocks.isEmpty()) {
            list.add(Component.literal("Blacklist:"));
            for (int block : blocks) {
                list.add(Component.literal("- " + blockName(block)).withStyle(ChatFormatting.RED));
            }
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
            int target = Block.getId(level.getBlockState(pos));
            Set<Integer> blocks = getBlocks(tag);

            if (blocks.contains(target)) {
                blocks.remove(target);
                player.sendSystemMessage(Component.literal("Removed from blacklist " + blockName(target)));
            } else {
                blocks.add(target);
                player.sendSystemMessage(Component.literal("Added to blacklist " + blockName(target)));
            }

            setBlocks(tag, blocks);
        } else {
            int px = Tags.getIntOr(tag, "x", 0);
            int py = Tags.getIntOr(tag, "y", 0);
            int pz = Tags.getIntOr(tag, "z", 0);

            if (px == 0 && py == 0 && pz == 0) {
                setPosition(tag, x, y, z);
                player.sendSystemMessage(Component.literal("First position set!"));
            } else {
                setPosition(tag, 0, 0, 0);

                int minX = Math.min(x, px);
                int minY = Math.min(y, py) - 1;
                int minZ = Math.min(z, pz);

                int sizeX = Math.abs(x - px) + 1;
                int sizeY = Math.abs(y - py) + 1;
                int sizeZ = Math.abs(z - pz) + 1;

                BlockPos at = new BlockPos(minX, minY, minZ);
                level.setBlock(at, ModStructureWandBlocks.wand_structure.get().defaultBlockState(), 3);

                if (level.getBlockEntity(at) instanceof TileEntityWandStructure structure) {
                    structure.sizeX = sizeX;
                    structure.sizeY = sizeY;
                    structure.sizeZ = sizeZ;

                    structure.blacklist = new LinkedHashSet<>();
                    for (int id : getBlocks(tag)) structure.blacklist.add(Block.stateById(id));
                    structure.setChanged();
                } else {
                    player.sendSystemMessage(Component.literal("Could not add a structure block!"));
                    ModDataComponents.PERSISTENT_DATA.set(stack, tag);
                    return InteractionResult.SUCCESS;
                }

                player.sendSystemMessage(Component.literal("Structure block configured and added at: "
                        + minX + ", " + minY + ", " + minZ));
            }
        }

        ModDataComponents.PERSISTENT_DATA.set(stack, tag);
        return InteractionResult.SUCCESS;
    }

    private static void setPosition(CompoundTag tag, int x, int y, int z) {
        tag.putInt("x", x);
        tag.putInt("y", y);
        tag.putInt("z", z);
    }

    private static Set<Integer> getBlocks(CompoundTag tag) {
        Set<Integer> blocks = new LinkedHashSet<>();
        for (int id : Tags.getIntArrayOr(tag, "blocks", new int[0])) blocks.add(id);
        return blocks;
    }

    private static void setBlocks(CompoundTag tag, Set<Integer> blocks) {
        tag.putIntArray("blocks", blocks.stream().mapToInt(Integer::intValue).toArray());
        tag.putIntArray("metas", new int[blocks.size()]);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        CompoundTag tag = data(stack);
        if (player.isShiftKeyDown()) {
            tag.putIntArray("blocks", new int[0]);
            tag.putIntArray("metas", new int[0]);
            player.sendSystemMessage(Component.literal("Cleared blacklist"));
        }
        ModDataComponents.PERSISTENT_DATA.set(stack, tag);

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
