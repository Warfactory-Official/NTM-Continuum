package com.hbm.blocks.generic;

import com.hbm.extprop.HbmPlayerProps;

import com.hbm.inventory.recipes.PedestalRecipes;
import com.hbm.inventory.recipes.PedestalRecipes.PedestalExtraCondition;
import com.hbm.inventory.recipes.PedestalRecipes.PedestalRecipe;
import com.hbm.items.ModArmorModItems;
import com.hbm.items.armor.ItemModDefuser;
import com.hbm.particle.helper.ExplosionSmallCreator;
import com.hbm.tileentity.ModDungeonBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.Skies;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.7 {@code BlockPedestal}: a stone pedestal (a quarter-block foot and head around a narrower column, drawn by
 * 1.7's {@code RenderPedestal}) that holds one stack and shows it floating above ({@code RenderPedestalTile}).
 * An empty hand takes the stack, a full hand puts the whole held stack down; sneaking does neither.
 *
 * <p>Powered by redstone it performs the ritual recipes ({@link PedestalRecipes}): eight pedestals around it,
 * two blocks out on the diagonals and three out on the axes, plus itself make the 3x3 grid (north at the top),
 * and the first recipe whose nine cells match exactly - item and count, empty for empty - consumes the eight
 * outer stacks and turns its own into the product with a small blast. The moon/sun conditions read the sky as
 * 1.7 did; the karma conditions ask whether any player within 20 blocks has a reputation of at least 10 (good) or
 * at most -10 (bad) ({@link HbmPlayerProps}, earned and lost at old lanterns). 1.7 also counted a "legendary" statistic for everyone within 50 blocks; the port has no such stat.
 *
 * <p>A pedestal holding a charm of protection or a meteorite charm registers itself every second for the
 * meteor handler 1.7 consulted, which the port does not have yet; the registry is kept so it can. A golden
 * defuser on a pedestal defuses every creeper within 25 blocks every three seconds.
 */
public class BlockPedestal extends CompatBlock implements TickingBlock {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 4, 16), Block.box(0, 12, 0, 16, 16, 16), Block.box(2, 4, 2, 14, 12, 14));

    public BlockPedestal(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPedestal(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        return takeOrPlace(level, pos, player, hand);
    }

    /** 1.7 {@code onBlockActivated}, shared with the skeleton holder, which is the same code. */
    static InteractionResult takeOrPlace(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() || hand != InteractionHand.MAIN_HAND) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(pos) instanceof ItemHolder holder)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        ItemStack held = player.getMainHandItem();

        if (holder.getItem().isEmpty() && !held.isEmpty()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            holder.setItem(held.copy());
            player.getInventory().setItem(Compat.selectedSlot(player), ItemStack.EMPTY);
            return InteractionResult.SUCCESS;
        } else if (!holder.getItem().isEmpty() && held.isEmpty()) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            player.getInventory().setItem(Compat.selectedSlot(player), holder.getItem().copy());
            holder.setItem(ItemStack.EMPTY);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** 1.7 {@code breakBlock}: the held stack pops out at the block's foot. */
    static void dropHeld(@Nullable Level level, BlockPos pos, ItemStack held) {
        if (level == null || level.isClientSide() || held.isEmpty()) return;
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, held.copy()));
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        if (level.isClientSide() || !level.hasNeighborSignal(pos)) return;

        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        TileEntityPedestal[] tileArray = {
                at(level, x - 2, y, z - 2), at(level, x, y, z - 3), at(level, x + 2, y, z - 2),
                at(level, x - 3, y, z), at(level, x, y, z), at(level, x + 3, y, z),
                at(level, x - 2, y, z + 2), at(level, x, y, z + 3), at(level, x + 2, y, z + 2)
        };
        TileEntityPedestal center = tileArray[4];
        if (center == null) return;
        // 1.7: the players whose reputation the karma conditions read, within 20 of the centre block
        java.util.List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class,
                new net.minecraft.world.phys.AABB(x, y, z, x + 1, y + 1, z + 1).inflate(20, 20, 20));

        float angle = Skies.celestialAngle(level, pos);
        int moon = Skies.moonPhase(level, pos);

        outer:
        for (PedestalRecipe recipe : PedestalRecipes.recipes) {

            /// EXTRA CONDITIONS ///
            if (recipe.extra == PedestalExtraCondition.FULL_MOON) {
                if (angle < 0.35 || angle > 0.65) continue;
                if (moon != 0) continue;
            }
            if (recipe.extra == PedestalExtraCondition.NEW_MOON) {
                if (angle < 0.35 || angle > 0.65) continue;
                if (moon != 4) continue;
            }
            if (recipe.extra == PedestalExtraCondition.SUN) {
                if (angle > 0.15 && angle < 0.85) continue;
            }
            if (recipe.extra == PedestalExtraCondition.BAD_KARMA) {
                boolean matches = false;
                for (Player player : nearbyPlayers) if (HbmPlayerProps.getReputation(player) <= -10) { matches = true; break; }
                if (!matches) continue;
            }
            if (recipe.extra == PedestalExtraCondition.GOOD_KARMA) {
                boolean matches = false;
                for (Player player : nearbyPlayers) if (HbmPlayerProps.getReputation(player) >= 10) { matches = true; break; }
                if (!matches) continue;
            }

            /// CHECK ITEMS ///
            for (int i = 0; i < 9; i++) {
                ItemStack pedestal = tileArray[i] != null ? tileArray[i].getItem() : ItemStack.EMPTY;
                PedestalRecipes.Input input = recipe.input[i];
                if (pedestal.isEmpty() && input != null) continue outer;
                if (!pedestal.isEmpty() && input == null) continue outer;
                if (pedestal.isEmpty()) continue;
                if (!PedestalRecipes.matches(input, pedestal)) continue outer;
            }

            ItemStack output = PedestalRecipes.output(recipe);
            if (output.isEmpty()) continue;

            /// REMOVE ITEMS ///
            for (int i = 0; i < 9; i++) {
                if (i == 4) continue;
                if (tileArray[i] == null || recipe.input[i] == null) continue;
                tileArray[i].setItem(ItemStack.EMPTY);
            }

            /// PRODUCE RESULT ///
            center.setItem(output);
            ExplosionSmallCreator.composeEffect(level, x + 0.5, y + 1.5, z + 0.5, 10, 2.5F, 1F);
            return;
        }
    }

    private static @Nullable TileEntityPedestal at(Level level, int x, int y, int z) {
        return level.getBlockEntity(new BlockPos(x, y, z)) instanceof TileEntityPedestal p ? p : null;
    }

    /** A block entity holding one displayed stack: the pedestal and the skeleton holder. */
    public interface ItemHolder {
        ItemStack getItem();

        void setItem(ItemStack stack);
    }

    /** 1.7 {@code TileEntityPedestal}: the stack, synced whole to the client like 1.7's description packet. */
    public static class TileEntityPedestal extends TileEntityLoadedBase implements ItemHolder {

        private ItemStack item = ItemStack.EMPTY;

        public TileEntityPedestal(BlockPos pos, BlockState state) {
            super(ModDungeonBlockEntities.PEDESTAL.get(), pos, state);
        }

        @Override
        public ItemStack getItem() {
            return item;
        }

        @Override
        public void setItem(ItemStack stack) {
            this.item = stack == null ? ItemStack.EMPTY : stack;
            setChanged();
            if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            super.preRemoveSideEffects(pos, state);
            dropHeld(level, pos, item);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPedestal pedestal) {
            if (level.getGameTime() % 20 != 0 || pedestal.item.isEmpty()) return;
            if (pedestal.item.is(ModArmorModItems.protection_charm.get()))
                pushPedestalEntry(level, PedestalEntryType.CHARM_OF_PROTECTION, pos);
            if (pedestal.item.is(ModArmorModItems.meteor_charm.get()))
                pushPedestalEntry(level, PedestalEntryType.METEORITE_CHARM, pos);
            if (level.getGameTime() % 60 == 0 && pedestal.item.is(com.hbm.items.ModExplosivesItems.defuser_gold.get()))
                pedestal.castrateCreepers();
        }

        public void castrateCreepers() {
            AABB box = new AABB(worldPosition).inflate(25, 25, 25);
            for (Creeper creeper : level.getEntitiesOfClass(Creeper.class, box)) {
                ItemModDefuser.castrateCreeper(creeper, null, false);
            }
        }

        @Override
        protected void loadData(DataIn input) {
            super.loadData(input);
            this.item = input.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        }

        @Override
        protected void saveData(DataOut output) {
            super.saveData(output);
            if (!item.isEmpty()) output.store("item", ItemStack.CODEC, item);
        }
    }

    public static final Map<ResourceKey<Level>, List<PedestalEntry>> pedestalEntries = new HashMap<>();

    public static void pushPedestalEntry(Level level, PedestalEntryType type, BlockPos pos) {
        pedestalEntries.computeIfAbsent(level.dimension(), key -> new ArrayList<>())
                .add(new PedestalEntry(type, pos, level.getGameTime()));
    }

    public static final int timeout = 60; // 3 seconds

    public static void checkPedestalEntries(ResourceKey<Level> dim, long currentTime) {
        List<PedestalEntry> entries = pedestalEntries.get(dim);
        if (entries == null) return;
        entries.removeIf(x -> x.timestamp < currentTime - timeout);
    }

    public static @Nullable List<PedestalEntry> getEntriesForDimension(ResourceKey<Level> dim) {
        return pedestalEntries.get(dim);
    }

    public record PedestalEntry(PedestalEntryType type, BlockPos pos, long timestamp) {}

    public enum PedestalEntryType {
        CHARM_OF_PROTECTION,
        METEORITE_CHARM
    }
}
