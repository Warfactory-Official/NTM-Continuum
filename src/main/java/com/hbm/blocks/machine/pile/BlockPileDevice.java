package com.hbm.blocks.machine.pile;

import com.hbm.api.block.IToolable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.pile.TileEntityPileControl;
import com.hbm.tileentity.machine.pile.TileEntityPileCore;
import com.hbm.tileentity.machine.pile.TileEntityPileLoader;
import com.hbm.tileentity.machine.pile.TileEntityPileVent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The three devices that dock onto a pile from outside: the fuel loader, the air blower and the control
 * rod. They are one block with one item, distinguished by a variant, exactly as upstream distinguishes
 * them by item damage; the block's {@link #META} packs that choice together with the horizontal side of
 * the pile the device faces.
 */
public class BlockPileDevice extends dev.golem.core.world.CompatBlock implements dev.golem.core.registry.TickingBlock,
        ILookOverlay, IToolable {

    public static final int ITEM_META_LOADER = 0;
    public static final int ITEM_META_VENT = 1;
    public static final int ITEM_META_CONTROL = 2;

    public static final int BLOCK_META_LOADER = 0;
    public static final int BLOCK_META_VENT = 4;
    public static final int BLOCK_META_CONTROL = 8;

    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 11);

    public BlockPileDevice(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(META, BLOCK_META_LOADER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(META);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (deviceOf(state)) {
            case BLOCK_META_LOADER -> new TileEntityPileLoader(pos, state);
            case BLOCK_META_VENT -> new TileEntityPileVent(pos, state);
            case BLOCK_META_CONTROL -> new TileEntityPileControl(pos, state);
            default -> null;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    public static int deviceOf(BlockState state) {
        int meta = state.hasProperty(META) ? state.getValue(META) : 0;
        return meta - meta % 4;
    }

    public static int itemMetaToBlockMeta(int meta) {
        if (meta >= ITEM_META_CONTROL) return BLOCK_META_CONTROL;
        if (meta == ITEM_META_VENT) return BLOCK_META_VENT;
        return BLOCK_META_LOADER;
    }

    public static int blockMetaToItemMeta(BlockState state) {
        int device = deviceOf(state);
        if (device >= BLOCK_META_CONTROL) return ITEM_META_CONTROL;
        if (device >= BLOCK_META_VENT) return ITEM_META_VENT;
        return ITEM_META_LOADER;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int device = itemMetaToBlockMeta(ModDataComponents.ITEM_VARIANT.getOr(context.getItemInHand(), 0));
        int meta = device + Mth.clamp(context.getClickedFace().get3DDataValue() - 2, 0, 3);

        // Upstream re-derives the control rod's facing from the player when the clicked face gave no
        // horizontal answer, which is what placing one on top of a pile always does.
        if (meta == BLOCK_META_CONTROL && context.getPlayer() != null) {
            int i = Mth.floor(context.getPlayer().getYRot() * 4.0F / 360.0F + 0.5D) & 3;
            meta = BLOCK_META_CONTROL + switch (i) {
                case 1 -> 3;
                case 2 -> 1;
                case 3 -> 2;
                default -> 0;
            };
        }

        return defaultBlockState().setValue(META, meta);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(ModPileBlocks.pileDeviceItem(blockMetaToItemMeta(state)));
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return ModPileBlocks.pileDeviceItem(blockMetaToItemMeta(state));
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        return interact(state, level, pos, player, ItemStack.EMPTY);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                         InteractionHand hand, ItemStack held, BlockHitResult hit) {
        return interact(state, level, pos, player, held);
    }

    /** The loader takes a rod straight from the hand, or fires a stroke when it is empty-handed. */
    private InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, ItemStack held) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (deviceOf(state) != BLOCK_META_LOADER) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!(level.getBlockEntity(pos) instanceof TileEntityPileLoader loader)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (loader.travel > 0D || loader.loading) return InteractionResult.SUCCESS;

        if (!held.isEmpty() && loader.stack.isEmpty() && TileEntityPileLoader.isItemLoadable(held)) {
            loader.setItem(0, held.copyWithCount(1));
            held.shrink(1);
            level.playSound(null, pos, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1F, 1F);
            return InteractionResult.SUCCESS;
        }

        loader.loading = true;
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit,
                           InteractionHand hand, ToolType tool) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(META)) return false;

        BlockPos target;
        Direction face;
        if (deviceOf(state) >= BLOCK_META_CONTROL) {
            target = pos.below();
            face = Direction.UP;
        } else {
            ForgeDirection dir = ForgeDirection.getOrientation(state.getValue(META) % 4 + 2);
            target = pos.offset(-dir.offsetX, 0, -dir.offsetZ);
            face = dir.toDirection();
        }

        if (level.getBlockState(target).getBlock() instanceof BlockPile pile) {
            return pile.onScrew(level, player, target, face, hit, hand, tool);
        }
        return false;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        List<String> text = new ArrayList<>();
        BlockEntity tile = level.getBlockEntity(pos);

        if (tile instanceof TileEntityPileLoader loader) {
            text.add("Temp: " + Math.round(loader.channelTemp) + " / " + TileEntityPileCore.MAX_HEAT + "°C");
            if (!loader.syncStack.isEmpty()) text.add("Loading: " + loader.syncStack.getHoverName().getString());
            if (!loader.channelStack.isEmpty()) {
                text.add("Last rod: " + loader.channelStack.getHoverName().getString());
                if (loader.channelDepletion > 0) {
                    text.add("Depletion: " + Math.round(loader.channelDepletion) + "%");
                }
            }
        }

        if (tile instanceof TileEntityPileControl control) {
            text.add("Extraction level: " + (int) (control.extraction * 100) + "%");
        }

        if (text.isEmpty()) return null;
        return new ILookOverlay.Data(
                ModPileBlocks.pileDeviceItem(blockMetaToItemMeta(level.getBlockState(pos))).getHoverName().getString(),
                0xffff00, 0x404000, text);
    }

}
