package com.hbm.blocks.generic;

import com.hbm.Constants;
import com.hbm.api.block.IToolable;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.config.ServerConfig;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.ICopiable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.itempool.ItemPool;
import com.hbm.items.ModDataComponents;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.items.tool.ItemLock;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import com.hbm.util.LootGenerator;
import com.hbm.world.gen.nbt.ILegacyStructureTile;
import com.hbm.world.gen.nbt.INBTTileEntityTransformable;
import com.hbm.world.gen.nbt.LegacyStructurePalette;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 1.7 {@code BlockWandLoot} ({@code wand_loot}, "Structure Wand Block (Lootable)"): marks a loot container in an
 * {@code .nbt} structure. When a structure places it (outside structure debug), or a detonator triggers it, its
 * block entity replaces itself on its next tick with the configured container, filled from an item pool (or, for
 * a loot pile, a {@link LootGenerator} arrangement), optionally locked, and turned the way the placer faced.
 *
 * <p>Configured by hand: a container block in hand sets the replacement, screwdriver / hand drill change the
 * minimum / maximum item count (crouch lowers), the defuser cycles the pool, a padlock stamps its pins and pick
 * chance. {@link #ROTATION} is 1.7's metadata, which only rotates the top texture ({@code IBlockSideRotation}).
 */
public class BlockWandLoot extends CompatBlock implements TickingBlock, ILookOverlay, IToolable, IBomb {

    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);

    public BlockWandLoot(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityWandLoot(pos, state);
    }

    /** 1.7: quadrant 0..3 (south, west, north, east look) wrote metadata 3, 2, 0, 1. */
    static int quadrantMeta(LivingEntity placer) {
        int i = Mth.floor(placer.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        return switch (i) { case 0 -> 3; case 1 -> 2; case 2 -> 0; default -> 1; };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer == null) return;
        level.setBlock(pos, state.setValue(ROTATION, quadrantMeta(placer)), 2);
        if (level.getBlockEntity(pos) instanceof TileEntityWandLoot loot) loot.placedRotation = placer.getYRot();
    }

    @Override
    public @Nullable Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityWandLoot loot)) return null;

        List<String> text = new ArrayList<>();
        text.add("Will replace with: " + loot.replaceState.getBlock().getDescriptionId());
        text.add("   meta: " + loot.replaceMeta);
        text.add("Loot pool: " + loot.poolName);
        if (!loot.replaceState.is(ModDungeonBlocks.deco_loot.get())) {
            text.add("Minimum items: " + loot.minItems);
            text.add("Maximum items: " + loot.maxItems);
        }

        if (loot.lockCode != 0) {
            text.add("Container will be locked");
            text.add("Lockpicking chance:" + loot.lockMod);
        }

        return new Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityWandLoot loot)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!player.isShiftKeyDown()) {
            Block block = getLootableBlock(level, held);

            if (block != null) {
                if (!level.isClientSide()) {
                    loot.replaceState = block.defaultBlockState();
                    loot.replaceMeta = ModDataComponents.ITEM_VARIANT.getOr(held, 0);

                    List<String> poolNames = loot.getPoolNames(block == ModDungeonBlocks.deco_loot.get());
                    if (!poolNames.contains(loot.poolName)) {
                        loot.poolName = poolNames.isEmpty() ? "" : poolNames.get(0);
                    }
                    loot.setChanged();
                }
                return InteractionResult.SUCCESS;
            }
        }

        if (held.getItem() instanceof ItemLock lock && !level.isClientSide()) {
            loot.lockMod = (float) lock.lockMod;
            loot.lockCode = ItemKeyPin.pins(held);
            loot.setChanged();
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** 1.7: the loot pile, or any block whose tile entity is an inventory. */
    private static @Nullable Block getLootableBlock(Level level, ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) return null;
        Block block = item.getBlock();

        if (block == ModDungeonBlocks.deco_loot.get()) return block;

        if (block instanceof EntityBlock provider) {
            try {
                BlockEntity te = provider.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
                if (te instanceof Container) return block;
            } catch (RuntimeException ignored) {
                // a block entity that cannot be built detached is not a plain container
            }
        }

        return null;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityWandLoot loot)) return false;

        switch (tool) {
            case SCREWDRIVER -> {
                if (player.isShiftKeyDown()) {
                    loot.minItems--;
                    if (loot.minItems < 0) loot.minItems = 0;
                } else {
                    loot.minItems++;
                    loot.maxItems = Math.max(loot.minItems, loot.maxItems);
                }
            }
            case HAND_DRILL -> {
                if (player.isShiftKeyDown()) {
                    loot.maxItems--;
                    if (loot.maxItems < 0) loot.maxItems = 0;
                    loot.minItems = Math.min(loot.minItems, loot.maxItems);
                } else {
                    loot.maxItems++;
                }
            }
            case DEFUSER -> {
                List<String> poolNames = loot.getPoolNames(loot.replaceState.is(ModDungeonBlocks.deco_loot.get()));
                if (poolNames.isEmpty()) return true;
                int index = poolNames.indexOf(loot.poolName);

                index += player.isShiftKeyDown() ? -1 : 1;
                index = Mth.clamp(index, 0, poolNames.size() - 1);

                loot.poolName = poolNames.get(index);
            }
            default -> {
                return false;
            }
        }
        loot.setChanged();
        return true;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityWandLoot loot)) return null;
        loot.triggerReplace = true;
        return BombReturnCode.TRIGGERED;
    }

    public static class TileEntityWandLoot extends TileEntityLoadedBase implements INBTTileEntityTransformable, ICopiable, ILegacyStructureTile {

        private boolean triggerReplace;

        BlockState replaceState = ModDungeonBlocks.deco_loot.get().defaultBlockState();
        int replaceMeta;

        String poolName = LootGenerator.LOOT_BOOKLET;
        int minItems;
        int maxItems = 1;

        float placedRotation;

        float lockMod = 0;
        int lockCode = 0;
        boolean cheesable = true;

        public TileEntityWandLoot(BlockPos pos, BlockState state) {
            super(ModStructureWandBlocks.WAND_LOOT_ENTITY.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityWandLoot tile) {
            if (tile.triggerReplace) {
                // On the first tick of this TE, replace with intended block and fill with loot
                tile.replace();
            } else {
                tile.networkPackNT();
            }
        }

        private void replace() {
            Level world = level;
            BlockPos pos = worldPosition;
            if (world == null) return;
            if (!(world.getBlockState(pos).getBlock() instanceof BlockWandLoot)) {
                Constants.LOG.warn("Somehow the block at: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " isn't a loot block but we're doing a TE update as if it is, cancelling!");
                return;
            }

            List<ItemPool.Entry> pool = ItemPool.getPool(poolName);

            // 1.7 set the block, then ran its onBlockPlacedBy for a fake player turned to placedRotation; the flat
            // equivalent is the block's own placement state for that look direction
            BlockState placed = replaceState;
            try {
                BlockState turned = placed.getBlock().getStateForPlacement(new DirectionalPlaceContext(world, pos,
                        Direction.fromYRot(placedRotation), new ItemStack(placed.getBlock()), Direction.UP));
                if (turned != null) placed = turned;
            } catch (RuntimeException ex) {
                Constants.LOG.warn("Failed to correctly rotate loot block at: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
            }

            world.setBlock(pos, placed, 2);

            BlockEntity te = world.getBlockEntity(pos);

            if (te == null || te instanceof TileEntityWandLoot) {
                // Some generator has broken the TE->block relationship, which, honestly, rude.
                Constants.LOG.warn("TE set incorrectly at: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ". If you're using some sort of world generation mod, report it to the author!");
                return;
            }

            if (te instanceof TileEntityLockableBase lockable && lockCode != 0) {
                lockable.setPins(lockCode);
                lockable.setMod(lockMod);
                lockable.cheesable = lockMod != 0;
                lockable.lock();
            }
            if (te instanceof Container inventory) {
                int count = minItems;
                if (maxItems - minItems > 0) count += world.getRandom().nextInt(maxItems - minItems);
                generateChestContents(world.getRandom(), pool, inventory, count);
            } else if (te instanceof BlockLoot.TileEntityLoot) {
                LootGenerator.applyLoot(world, pos, poolName);
            }
            te.setChanged();
        }

        /**
         * 1.7 {@code WeightedRandomChestContent.generateChestContents}: {@code count} weighted draws, each stack put
         * into a random slot (overwriting), a draw larger than a stack split into single items.
         */
        static void generateChestContents(RandomSource rand, List<ItemPool.Entry> pool, Container inventory, int count) {
            int size = inventory.getContainerSize();
            if (size <= 0) return;
            for (int j = 0; j < count; ++j) {
                ItemStack drawn = ItemPool.getStack(pool, rand);
                if (drawn.isEmpty()) continue;
                if (drawn.getCount() <= drawn.getMaxStackSize()) {
                    inventory.setItem(rand.nextInt(size), drawn);
                } else {
                    for (int i = 0; i < drawn.getCount(); i++) {
                        inventory.setItem(rand.nextInt(size), dev.golem.core.world.ItemStacks.copyWithCount(drawn, 1));
                    }
                }
            }
        }

        List<String> getPoolNames(boolean loot) {
            if (loot) return Arrays.asList(LootGenerator.getLootNames());

            // 1.7 iterated its HashMap of pools; that order was arbitrary, alphabetical is stable
            List<String> names = new ArrayList<>(ItemPool.POOLS.keySet());
            names.sort(String::compareTo);
            return names;
        }

        @Override
        public void transformTE(net.minecraft.world.level.LevelAccessor level, int coordBaseMode) {
            triggerReplace = !ServerConfig.STRUCTURE_DEBUG.get();
            placedRotation = Mth.wrapDegrees(placedRotation + coordBaseMode * 90);
        }

        @Override
        protected void saveData(DataOut out) {
            super.saveData(out);
            out.putString("block", LegacyStructurePalette.stateString(replaceState));
            out.putInt("meta", replaceMeta);
            out.putInt("min", minItems);
            out.putInt("max", maxItems);
            out.putString("pool", poolName);
            out.putFloat("rot", placedRotation);

            out.putInt("lockCode", lockCode);
            out.putFloat("lockMod", lockMod);
            out.putBoolean("cheesable", cheesable);

            out.putBoolean("trigger", triggerReplace);
        }

        @Override
        protected void loadData(DataIn in) {
            super.loadData(in);
            BlockState state = LegacyStructurePalette.parseState(in.getStringOr("block", ""));
            replaceState = state.isAir() ? ModDungeonBlocks.deco_loot.get().defaultBlockState() : state;
            replaceMeta = in.getIntOr("meta", 0);
            minItems = in.getIntOr("min", 0);
            maxItems = in.getIntOr("max", 0);
            poolName = in.getStringOr("pool", "");
            placedRotation = in.getFloatOr("rot", 0);

            lockCode = in.getIntOr("lockCode", 0);
            lockMod = in.getFloatOr("lockMod", 0);
            cheesable = in.getBooleanOr("cheesable", false);

            triggerReplace = in.getBooleanOr("trigger", false);
        }

        /** The 1.7 keys, with {@code block} a 1.7 registry name and {@code meta} its metadata. */
        @Override
        public void readLegacyStructureNBT(CompoundTag nbt) {
            replaceMeta = Tags.getIntOr(nbt, "meta", 0);
            BlockState state = LegacyStructurePalette.legacyState(Tags.getStringOr(nbt, "block", ""), replaceMeta);
            replaceState = state.isAir() ? ModDungeonBlocks.deco_loot.get().defaultBlockState() : state;
            minItems = Tags.getIntOr(nbt, "min", 0);
            maxItems = Tags.getIntOr(nbt, "max", 0);
            poolName = Tags.getStringOr(nbt, "pool", "");
            placedRotation = Tags.getFloatOr(nbt, "rot", 0);

            lockCode = Tags.getIntOr(nbt, "lockCode", 0);
            lockMod = Tags.getFloatOr(nbt, "lockMod", 0);
            cheesable = Tags.getBooleanOr(nbt, "cheesable", false);

            triggerReplace = Tags.getBooleanOr(nbt, "trigger", false);
        }

        @Override
        public void serialize(RegistryFriendlyByteBuf buf) {
            buf.writeInt(Block.getId(replaceState));
            buf.writeInt(replaceMeta);
            buf.writeInt(minItems);
            buf.writeInt(maxItems);
            buf.writeUtf(poolName);

            buf.writeInt(lockCode);
            buf.writeFloat(lockMod);
            buf.writeBoolean(cheesable);
        }

        @Override
        public void deserialize(RegistryFriendlyByteBuf buf) {
            replaceState = Block.stateById(buf.readInt());
            replaceMeta = buf.readInt();
            minItems = buf.readInt();
            maxItems = buf.readInt();
            poolName = buf.readUtf();

            lockCode = buf.readInt();
            lockMod = buf.readFloat();
            cheesable = buf.readBoolean();
        }

        @Override
        public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("replaceBlock", LegacyStructurePalette.stateString(replaceState));
            nbt.putInt("replaceMeta", replaceMeta);
            nbt.putInt("minItems", minItems);
            nbt.putInt("maxItems", maxItems);
            nbt.putString("poolName", poolName);

            nbt.putInt("lockCode", lockCode);
            nbt.putFloat("lockMod", lockMod);
            nbt.putBoolean("cheesable", cheesable);

            return nbt;
        }

        @Override
        public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
            replaceState = LegacyStructurePalette.parseState(Tags.getStringOr(nbt, "replaceBlock", ""));
            replaceMeta = Tags.getIntOr(nbt, "replaceMeta", 0);
            minItems = Tags.getIntOr(nbt, "minItems", 0);
            maxItems = Tags.getIntOr(nbt, "maxItems", 0);
            poolName = Tags.getStringOr(nbt, "poolName", "");

            lockCode = Tags.getIntOr(nbt, "lockCode", 0);
            lockMod = Tags.getFloatOr(nbt, "lockMod", 0);
            cheesable = Tags.getBooleanOr(nbt, "cheesable", false);
            setChanged();
        }
    }
}
