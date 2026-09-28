package com.hbm.items.tool;

import com.google.gson.stream.JsonWriter;
import com.hbm.Constants;
import com.hbm.blocks.ModCustomMachineBlocks;
import com.hbm.blocks.machine.BlockCMAnchor;
import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.interfaces.IItemLookOverlay;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModDataComponents;
import dev.golem.core.data.Tags;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code ItemCMStructure} ({@code structure_custommachine}, "Custom Machine Structure Output Wand"). Click the
 * positioning anchor to remember it, then two corners; the third click writes every non-air block of the box,
 * anchor excluded, as {@code components} to {@code hbmConfig/CMstructureOutput.txt}, in the anchor's frame.
 *
 * <p>The stack NBT keys are 1.7's, kept in the persistent-data component. A written component is the block's
 * modern registry id with {@code metas: [0]}: flattened blocks carry their variant in the id. The file is written
 * by the server; 1.7 ran the click on both sides and wrote it twice.
 */
public class ItemCMStructure extends CompatItem implements IItemLookOverlay {

    private static final int RED = 0xFF5555, GOLD = 0xFFAA00, YELLOW = 0xFFFF55;

    public ItemCMStructure(Properties properties) {
        super(properties);
    }

    private static Path file() {
        return CustomMachineConfigJSON.configHbmDir().resolve("CMstructureOutput.txt");
    }

    private static CompoundTag data(ItemStack stack) {
        return ModDataComponents.PERSISTENT_DATA.get(stack).map(CompoundTag::copy).orElse(new CompoundTag());
    }

    public static @Nullable BlockPos getAnchor(ItemStack stack) {
        CompoundTag tag = ModDataComponents.PERSISTENT_DATA.get(stack).orElse(null);
        if (tag == null || !tag.contains("anchorX")) {
            return null;
        }
        return new BlockPos(Tags.getIntOr(tag, "anchorX", 0), Tags.getIntOr(tag, "anchorY", 0), Tags.getIntOr(tag, "anchorZ", 0));
    }

    public static void setAnchor(ItemStack stack, int x, int y, int z) {
        CompoundTag tag = data(stack);
        tag.putInt("anchorX", x);
        tag.putInt("anchorY", y);
        tag.putInt("anchorZ", z);
        ModDataComponents.PERSISTENT_DATA.set(stack, tag);
    }

    public static void writeToFile(Path config, CompoundTag tag, Level level) {
        int anchorX = Tags.getIntOr(tag, "anchorX", 0);
        int anchorY = Tags.getIntOr(tag, "anchorY", 0);
        int anchorZ = Tags.getIntOr(tag, "anchorZ", 0);
        int x1 = Tags.getIntOr(tag, "x1", 0);
        int y1 = Tags.getIntOr(tag, "y1", 0);
        int z1 = Tags.getIntOr(tag, "z1", 0);
        int x2 = Tags.getIntOr(tag, "x2", 0);
        int y2 = Tags.getIntOr(tag, "y2", 0);
        int z2 = Tags.getIntOr(tag, "z2", 0);
        BlockState anchorState = level.getBlockState(new BlockPos(anchorX, anchorY, anchorZ));
        Direction dir = anchorState.hasProperty(BlockCMAnchor.FACING) ? anchorState.getValue(BlockCMAnchor.FACING) : Direction.DOWN;
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        try (Writer out = Files.newBufferedWriter(config, StandardCharsets.UTF_8)) {
            JsonWriter writer = new JsonWriter(out);
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("components").beginArray();

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {

                        int compY = y - anchorY;
                        int compX = 0;
                        int compZ = 0;

                        if (dir == Direction.SOUTH) {
                            compX = anchorX - x;
                            compZ = anchorZ - z;
                        }
                        if (dir == Direction.NORTH) {
                            compX = x - anchorX;
                            compZ = z - anchorZ;
                        }

                        if (dir == Direction.WEST) {
                            compZ = x - anchorX;
                            compX = anchorZ - z;
                        }
                        if (dir == Direction.EAST) {
                            compZ = anchorX - x;
                            compX = z - anchorZ;
                        }

                        if (x == anchorX && y == anchorY && z == anchorZ) continue;
                        BlockState state = level.getBlockState(new BlockPos(x, y, z));
                        if (state.isAir()) continue;

                        writer.beginObject().setIndent("");
                        writer.name("block").value(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
                        writer.name("x").value(compX);
                        writer.name("y").value(compY);
                        writer.name("z").value(compZ);
                        writer.name("metas").beginArray().value(0).endArray();
                        writer.endObject().setIndent("  ");
                    }
                }
            }
            writer.endArray();
            writer.endObject();
            writer.flush();
            Constants.LOG.info("Wrote custom machine structure {}..{} around anchor {} {} {} to {}",
                    new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ), anchorX, anchorY, anchorZ, config);
        } catch (IOException e) {
            Constants.LOG.error("Unable to write {}", config, e);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        if (level.getBlockState(pos).getBlock() == ModCustomMachineBlocks.cm_anchor.get()) {
            if (!level.isClientSide()) setAnchor(stack, x, y, z);
            return InteractionResult.SUCCESS;
        }

        if (getAnchor(stack) == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        CompoundTag tag = data(stack);
        if (!tag.contains("x1")) {
            tag.putInt("x1", x);
            tag.putInt("y1", y);
            tag.putInt("z1", z);
        } else if (!tag.contains("x2")) {
            tag.putInt("x2", x);
            tag.putInt("y2", y);
            tag.putInt("z2", z);
        } else {
            writeToFile(file(), tag, level);
            for (String key : new String[]{"x1", "y1", "z1", "x2", "y2", "z2"}) tag.remove(key);
        }
        ModDataComponents.PERSISTENT_DATA.set(stack, tag);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Click Custom Machine Structure Positioning Anchor to").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Confirm the location of the custom machine core block.").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("Output all blocks between Position1 and Position2 with").withStyle(ChatFormatting.YELLOW));
        list.add(Component.literal("metadata to \"CMstructureOutput.txt\" in hbmConfig.").withStyle(ChatFormatting.YELLOW));
    }

    /** 1.7 {@code printHook}: the anchor and whichever corners are set. */
    @Override
    public ILookOverlay.@Nullable Data getLookOverlay(Level level, BlockPos pos, ItemStack stack, Player player) {
        List<String> text = new ArrayList<>();

        BlockPos anchor = getAnchor(stack);

        if (anchor == null) {
            text.add("&[" + RED + "&]No Anchor");
        } else {
            CompoundTag tag = data(stack);
            text.add("&[" + GOLD + "&]Anchor: " + anchor.getX() + " / " + anchor.getY() + " / " + anchor.getZ());
            if (tag.contains("x1")) {
                text.add("&[" + YELLOW + "&]Position1: " + Tags.getIntOr(tag, "x1", 0) + " / "
                        + Tags.getIntOr(tag, "y1", 0) + " / " + Tags.getIntOr(tag, "z1", 0));
            }
            if (tag.contains("x2")) {
                text.add("&[" + YELLOW + "&]Position2: " + Tags.getIntOr(tag, "x2", 0) + " / "
                        + Tags.getIntOr(tag, "y2", 0) + " / " + Tags.getIntOr(tag, "z2", 0));
            }
        }

        return new ILookOverlay.Data(stack.getHoverName().getString(), 0xffff00, 0x404000, text);
    }
}
