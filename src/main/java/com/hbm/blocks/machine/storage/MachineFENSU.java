package com.hbm.blocks.machine.storage;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockMaskTable;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.storage.TileEntityMachineFENSU;
import dev.golem.core.Ids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Legacy FEnSU flywheel energy store: the MBB-sized, single-bottom-port battery. */
public class MachineFENSU extends BlockMultiblockMachine implements IEnergyConnectorBlock, ILookOverlay,
        WorldlyContainerHolder {

    /** The one top-level box in old {@code bb_fensu0.mbb}; it is core-relative. */
    private static final AABB LEGACY_BOUNDS = new AABB(-1D, 0D, -2D, 2D, 5D, 3D);

    public MachineFENSU(Properties properties) {
        super(properties);
        this.bounding.add(LEGACY_BOUNDS);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineFENSU(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 1, 1, 2, 2};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                       Player player, BlockHitResult hit) {
        return level.isClientSide() ? InteractionResult.SUCCESS
                : player.isShiftKeyDown() ? InteractionResult.TRY_WITH_EMPTY_HAND
                : super.onUseWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityMachineFENSU fensu) {
            fensu.color = ModDataComponents.fensuColor(stack, DyeColor.LIGHT_BLUE);
            fensu.setPower(ModDataComponents.fensuPower(stack));
            fensu.redLow = (short) Math.max(TileEntityMachineFENSU.mode_input,
                    Math.min(ModDataComponents.fensuRedLow(stack, TileEntityMachineFENSU.mode_input), TileEntityMachineFENSU.mode_none));
            fensu.redHigh = (short) Math.max(TileEntityMachineFENSU.mode_input,
                    Math.min(ModDataComponents.fensuRedHigh(stack, TileEntityMachineFENSU.mode_output), TileEntityMachineFENSU.mode_none));
            fensu.priority = ModDataComponents.fensuPriority(stack, IEnergyReceiverMK2.ConnectionPriority.LOW);
            fensu.setChanged();
            fensu.networkPackNT();
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineFENSU fensu
                ? fensu.getComparatorPower() : 0;
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineFENSU fensu ? fensu : null;
    }

    /**
     * 1.7's detailed MBB hitbox turns with the machine, which the generic silhouette does not do. 1.7
     * rotates this one box about the core's half-block centre and leaves every other machine's alone.
     */
    @Override
    public @Nullable VoxelShape cellShape(int lx, int ly, int lz, Direction facing, boolean open,
                                          boolean forCollision) {
        return Shapes.create(rotateBounds(LEGACY_BOUNDS, facing.getClockWise()).move(
                -MultiblockMaskTable.worldX(lx, ly, lz, facing) + 0.5D, -ly,
                -MultiblockMaskTable.worldZ(lx, ly, lz, facing) + 0.5D));
    }

    private static AABB rotateBounds(AABB box, Direction rotation) {
        return switch (rotation) {
            case EAST -> new AABB(-box.maxZ, box.minY, box.minX, -box.minZ, box.maxY, box.maxX);
            case SOUTH -> new AABB(-box.maxX, box.minY, -box.maxZ, -box.minX, box.maxY, -box.minZ);
            case WEST -> new AABB(box.minZ, box.minY, -box.maxX, box.maxZ, box.maxY, -box.minX);
            default -> box;
        };
    }

    /** Vanilla dye items are the direct current API equivalent of the old dye ore-dictionary lookup. */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos,
                                                               Player player, InteractionHand hand,
                                                               ItemStack stack, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        DyeColor dyeColor = dyeColor(stack);
        if (dyeColor != null) {
            BlockPos core = MultiblockSurface.coreOfAny(level, pos);
            if (core != null && level.getBlockEntity(core) instanceof TileEntityMachineFENSU fensu) {
                if (!level.isClientSide()) {
                    fensu.color = dyeColor;
                    fensu.setChanged();
                    fensu.networkPackNT();
                    if (!player.isCreative()) stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    private static DyeColor dyeColor(ItemStack stack) {
        for (DyeColor color : DyeColor.values()) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM,
                    Ids.of("c", "dyes/" + color.getName()));
            if (stack.is(tag)) return color;
        }
        return null;
    }

    /** Carry the FEnSU's internal power and selected dye with its one dropped machine item. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        BlockPos core = MultiblockSurface.coreOfAny(params.getLevel(), pos);
        ItemStack stack = new ItemStack(this);
        if (core != null && params.getLevel().getBlockEntity(core) instanceof TileEntityMachineFENSU fensu) {
            ModDataComponents.fensuPower(stack, fensu.power);
            ModDataComponents.setFensuColor(stack, fensu.color);
            ModDataComponents.setFensuRedLow(stack, fensu.redLow);
            ModDataComponents.setFensuRedHigh(stack, fensu.redHigh);
            ModDataComponents.setFensuPriority(stack, fensu.priority);
        }
        return List.of(stack);
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return cloneStack(level, pos);
    }

    private ItemStack cloneStack(BlockGetter level, BlockPos pos) {
        ItemStack stack = new ItemStack(this);
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityMachineFENSU fensu) {
            ModDataComponents.fensuPower(stack, fensu.power);
            ModDataComponents.setFensuColor(stack, fensu.color);
            ModDataComponents.setFensuRedLow(stack, fensu.redLow);
            ModDataComponents.setFensuRedHigh(stack, fensu.redHigh);
            ModDataComponents.setFensuPriority(stack, fensu.priority);
        }
        return stack;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineFENSU fensu)) return null;

        List<String> text = new ArrayList<>();
        text.add("§6<> §rStored Energy: " + Library.getShortNumber(fensu.power) + "/9.22EHE");
        if (fensu.delta == 0) text.add("§e-- §r0HE/s");
        else if (fensu.delta > 0) text.add("§a-> §r" + Library.getShortNumber(fensu.delta) + "HE/s");
        else text.add("§c<- §r" + Library.getShortNumber(-fensu.delta) + "HE/s");
        double fraction = (double) fensu.power / (double) TileEntityMachineFENSU.MAX_POWER;
        text.add("&[" + Library.getColorProgress(fraction) + "&]    "
                + Library.getPercentage(fraction) + "%");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    /** 1.7 takes power under the core alone. */
    @Override
    public int coreMask() {
        return MASK_DOWN;
    }
}
