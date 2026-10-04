package com.hbm.items.tool;

import com.hbm.items.ModDataComponents;
import com.hbm.interfaces.IItemLookOverlay;
import com.hbm.interfaces.ILookOverlay;
import dev.golem.core.data.Tags;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.blocks.network.BlockConveyorBase;
import com.hbm.blocks.network.BlockConveyorBendable;
import com.hbm.util.ConveyorBend;
import com.hbm.util.ConveyorRoute;

/**
 * 1.7 {@code items/tool/ItemConveyorWand}: the belt family's only source and its only placer. One click anchors,
 * the second builds the whole run between the two, and the router turns, lifts and drops on its own. The four
 * metadata subtypes are four items ({@code conveyor_wand_<type>}).
 *
 * <p>The pending run 1.7 kept in the stack NBT lives as {@code run_*} keys of the persistent-data component; the
 * client reads it back through {@link #pendingRun} to draw the ghost preview
 * ({@code com.hbm.client.render.ConveyorPreviewRenderer}, 1.7 {@code RenderOverhead.setActionPreview}).
 */
public class ItemConveyorWand extends CompatItem implements IItemLookOverlay {

    /** 1.7 {@code breakExtra}: the creative line break walks at most this far from the block struck. */
    private static final int BREAK_DEPTH = 32;

    public final ConveyorType type;

    public ItemConveyorWand(Properties properties, ConveyorType type) {
        super(properties);
        this.type = type;
    }

    public static ConveyorType getType(ItemStack stack) {
        return stack.getItem() instanceof ItemConveyorWand wand ? wand.type : ConveyorType.REGULAR;
    }

    public static Block getConveyorBlock(ConveyorType type) {
        return type.block.get();
    }

    /** 1.7: only the plain belt's wand may place lifts and chutes. */
    public static boolean hasSnakesAndLadders(ConveyorType type) {
        return type == ConveyorType.REGULAR;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Language language = Language.getInstance();
        for (int i = 0; language.has("item.hbm.conveyor_wand.desc." + i); i++) {
            tooltip.add(Component.translatable("item.hbm.conveyor_wand.desc." + i).withStyle(ChatFormatting.YELLOW));
        }
        if (hasSnakesAndLadders(getType(stack))) {
            tooltip.add(Component.translatable("item.hbm.conveyor_wand.vertical.desc").withStyle(ChatFormatting.AQUA));
        }
    }

    /**
     * 1.7 {@code IItemLookOverlay}: sneaking in creative and looking at a belt, the wand announces that breaking
     * it takes the whole line ({@link #compatCanDestroyBlock}).
     */
    @Override
    public ILookOverlay.@Nullable Data getLookOverlay(Level level, BlockPos pos, ItemStack stack, Player player) {
        if (!player.isShiftKeyDown() || !player.getAbilities().instabuild) return null;

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockConveyorBase)) return null;

        return new ILookOverlay.Data(state.getBlock().getName().getString(), 0xffff00, 0x404000,
                List.of("Break whole conveyor line"));
    }

    // --- the pending run: 1.7's x/y/z/side/count stack NBT, prefixed inside the persistent-data compound ---

    /** 1.7 stack NBT {@code x/y/z/side/count}: the first click of a two-click run plus the belt budget then counted. */
    public record Run(BlockPos anchor, Direction side, int budget) {}

    /** The run the first click stored, or null while the wand is idle. Readable on the client for the preview. */
    public static @Nullable Run pendingRun(ItemStack stack) {
        CompoundTag run = run(stack);
        if (run == null) return null;
        return new Run(new BlockPos(Tags.getIntOr(run, "run_x", 0), Tags.getIntOr(run, "run_y", 0), Tags.getIntOr(run, "run_z", 0)),
                com.hbm.util.SavedIds.getEnum(run, "run_side", Direction.UP), Tags.getIntOr(run, "run_count", 0));
    }

    private static final String[] RUN_KEYS = {"run_x", "run_y", "run_z", "run_side", "run_count"};

    private static @Nullable CompoundTag run(ItemStack stack) {
        CompoundTag data = ModDataComponents.PERSISTENT_DATA.get(stack).orElse(null);
        if (data == null || !data.contains("run_x")) return null;
        return data;
    }

    private static void setRun(ItemStack stack, @Nullable CompoundTag run) {
        CompoundTag data = ModDataComponents.PERSISTENT_DATA.get(stack).map(CompoundTag::copy).orElse(new CompoundTag());
        for (String key : RUN_KEYS) data.remove(key);
        if (run != null) {
            for (String key : RUN_KEYS) data.putInt(key, Tags.getIntOr(run, key, 0));
        }
        if (data.isEmpty()) ModDataComponents.PERSISTENT_DATA.remove(stack);
        else ModDataComponents.PERSISTENT_DATA.set(stack, data);
    }

    /** 1.7 {@code onUpdate}: a run whose wand leaves the hand is abandoned. */
    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot, boolean held) {
        boolean inHand = held && slot != EquipmentSlot.OFFHAND; // below 26.1 the slot is null and held is the main hand
        if (!inHand && run(stack) != null) setRun(stack, null);
    }

    /** 1.7 {@code onBlockStartBreak}: sneaking in creative, breaking one belt takes the whole line with it. */
    @Override
    protected boolean compatCanDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos pos, LivingEntity user) {
        if (!(level instanceof ServerLevel server) || !(user instanceof Player player)) return true;
        if (!player.isShiftKeyDown() || !player.getAbilities().instabuild) return true;
        if (!(state.getBlock() instanceof BlockConveyorBase belt)) return true;

        breakExtra(server, player, pos.relative(belt.getInputDirection(level, pos)), BREAK_DEPTH);
        breakExtra(server, player, pos.relative(belt.getOutputDirection(level, pos)), BREAK_DEPTH);
        return true;
    }

    private static void breakExtra(ServerLevel level, Player player, BlockPos pos, int depth) {
        if (--depth <= 0) return;

        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlockConveyorBase belt)) return;

        Direction input = belt.getInputDirection(level, pos);
        Direction output = belt.getOutputDirection(level, pos);

        level.destroyBlock(pos, false, player);

        breakExtra(level, player, pos.relative(input), depth);
        breakExtra(level, player, pos.relative(output), depth);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        CompoundTag run = run(stack);

        if (player.isShiftKeyDown() && run == null) {
            if (level instanceof ServerLevel server) placeSingle(server, player, stack, pos, side);
            return InteractionResult.SUCCESS;
        }

        // If placing on top of a conveyor block, auto-snap to edge if possible
        // this makes it easier to connect without having to click the small edge of a conveyor
        if (level.getBlockState(pos).getBlock() instanceof BlockConveyorBendable bendable) {
            Direction moveDir = run != null ? bendable.getInputDirection(level, pos) : bendable.getOutputDirection(level, pos);
            if (level.getBlockState(pos.relative(moveDir)).canBeReplaced()) side = moveDir;
        }

        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;

        if (run == null) {
            // Starting placement
            CompoundTag nbt = new CompoundTag();
            nbt.putInt("run_x", pos.getX());
            nbt.putInt("run_y", pos.getY());
            nbt.putInt("run_z", pos.getZ());
            com.hbm.util.SavedIds.putEnum(nbt, "run_side", side);
            nbt.putInt("run_count", budget(player, stack));
            setRun(stack, nbt);
            return InteractionResult.SUCCESS;
        }

        // Constructing conveyor
        buildRun(server, player, stack, pos, side);
        setRun(stack, null);
        return InteractionResult.SUCCESS; // always eat interactions
    }

    /** 1.7: the sneak arm places one belt against the clicked face, walking the lift/chute ring on the way. */
    private void placeSingle(ServerLevel level, Player player, ItemStack stack, BlockPos pos, Direction side) {
        ConveyorType type = getType(stack);
        BlockState onState = level.getBlockState(pos);
        Block onBlock = onState.getBlock();
        Block conveyor = ModConveyorBlocks.CONVEYOR.get();
        Block lift = ModConveyorBlocks.CONVEYOR_LIFT.get();
        Block chute = ModConveyorBlocks.CONVEYOR_CHUTE.get();

        if (hasSnakesAndLadders(type) && onBlock == conveyor
                && onState.getValue(BlockConveyorBendable.BEND) == ConveyorBend.STRAIGHT) {
            if (side == Direction.UP) onBlock = lift;
            else if (side == Direction.DOWN) onBlock = chute;

            if (onBlock != conveyor) {
                ConveyorRoute.setBelt(level, pos, onBlock.defaultBlockState()
                        .setValue(BlockConveyorBase.FACING, onState.getValue(BlockConveyorBase.FACING)));
            }
        }

        Block toPlace = getConveyorBlock(type);
        if (hasSnakesAndLadders(type)) {
            if (onBlock == lift && side == Direction.UP) toPlace = lift;
            if (onBlock == chute && side == Direction.DOWN) toPlace = chute;
        }

        BlockPos target = pos.relative(side);
        if (!level.getBlockState(target).canBeReplaced()) return;

        ConveyorRoute.setBelt(level, target, toPlace.defaultBlockState()
                .setValue(BlockConveyorBase.FACING, player.getDirection()));
        dev.golem.core.world.ItemStacks.consume(stack, 1, player);
    }

    /** 1.7: creative carries 256, survival the wands of this exact type held in the main inventory. */
    private static int budget(Player player, ItemStack stack) {
        if (player.getAbilities().instabuild) return 256;

        int count = 0;
        for (ItemStack carried : dev.golem.core.data.Inventories.main(player.getInventory())) {
            if (!carried.isEmpty() && carried.getItem() == stack.getItem()) count += carried.getCount();
        }
        return count;
    }

    private void buildRun(ServerLevel level, Player player, ItemStack stack, BlockPos end, Direction endSide) {
        ConveyorType type = getType(stack);
        Run pending = pendingRun(stack);
        if (pending == null) return;
        BlockPos anchor = pending.anchor();
        Direction anchorSide = pending.side();
        int count = pending.budget();

        // pretend to construct, if it doesn't fail, actually construct
        int planned = ConveyorRoute.construct(level, false, type, player, anchor, anchorSide, end, endSide, count);

        if (planned > 0) {
            int toRemove = ConveyorRoute.construct(level, true, type, player, anchor, anchorSide, end, endSide, count);

            if (!player.getAbilities().instabuild) {
                for (ItemStack carried : dev.golem.core.data.Inventories.main(player.getInventory())) {
                    if (!carried.isEmpty() && carried.getItem() == stack.getItem()) {
                        int removing = Math.min(toRemove, carried.getCount());
                        carried.shrink(removing);
                        toRemove -= removing;
                    }
                    if (toRemove <= 0) break;
                }
                player.inventoryMenu.broadcastChanges();
            }

            player.sendSystemMessage(Component.literal("Conveyor built!"));
        } else if (planned == 0) {
            player.sendSystemMessage(Component.literal("Not enough conveyors, build cancelled"));
        } else {
            player.sendSystemMessage(Component.literal("Conveyor obstructed, build cancelled"));
        }
    }

    /** 1.7 {@code ItemConveyorWand.ConveyorType}: the four metadata subtypes. */
    public enum ConveyorType {
        REGULAR(() -> ModConveyorBlocks.CONVEYOR.get()),
        EXPRESS(() -> ModConveyorBlocks.CONVEYOR_EXPRESS.get()),
        DOUBLE(() -> ModConveyorBlocks.CONVEYOR_DOUBLE.get()),
        TRIPLE(() -> ModConveyorBlocks.CONVEYOR_TRIPLE.get());

        public static final ConveyorType[] VALUES = values();

        public final String id = name().toLowerCase(Locale.ROOT);
        private final Supplier<Block> block;

        ConveyorType(Supplier<Block> block) {
            this.block = block;
        }
    }
}
