package com.hbm.blocks.machine;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.Library;
import dev.golem.core.graph.LevelNodeGraph;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.hbm.blocks.ModEnergyNetContent;
import com.hbm.items.block.EnergyNetBlockItem;
import com.hbm.tileentity.network.TileEntityCapacitor;

/**
 * 1.7's {@code MachineCapacitor} ({@code capacitor_tantalium}, {@code capacitor_schrabidate}): a one-block
 * store that takes power on the face it was placed against and pushes it out the far side. Its charge
 * travels with the dropped item ({@code IPersistentNBT}).
 */
public class MachineCapacitor extends CompatBlock implements TickingBlock, ILookOverlay {

    /** 1.7's metadata: the side of the clicked neighbour, i.e. this block's input face. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    public final long power;

    public MachineCapacitor(Properties properties, long power) {
        super(properties);
        this.power = power;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCapacitor(pos, state);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighbourBlock, movedByPiston);
        if (level instanceof ServerLevel server) LevelNodeGraph.invalidateEndpointsAround(server, pos);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityCapacitor battery)) return null;
        List<String> text = new ArrayList<>();
        text.add(Library.getShortNumber(battery.getPower()) + " / " + Library.getShortNumber(battery.getMaxPower()) + "HE");
        double percent = battery.getMaxPower() == 0 ? 0 : (double) battery.getPower() / (double) battery.getMaxPower();
        int charge = (int) Math.floor(percent * 10_000D);
        int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);
        text.add("&[" + color + "&]" + (charge / 100D) + "%");
        text.add("§a-> §r+" + Library.getShortNumber(battery.powerReceived) + "HE/t");
        text.add("§c<- §r-" + Library.getShortNumber(battery.powerSent) + "HE/t");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    // --- the charge travels with the item ---

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        if (level.getBlockEntity(pos) instanceof TileEntityCapacitor capacitor) {
            capacitor.setPower(Math.min(ModEnergyNetContent.CAPACITOR_POWER.getOr(stack, 0L), power));
            capacitor.setChanged();
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(this);
        // A player break removes the block (and its block entity) before the drops are rolled, so the tile is
        // the one the loot context carries; the level lookup only answers for the explosion/other paths.
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be == null) {
            BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
            be = params.getLevel().getBlockEntity(pos);
        }
        if (be instanceof TileEntityCapacitor capacitor && capacitor.getPower() > 0) {
            ModEnergyNetContent.CAPACITOR_POWER.set(stack, capacitor.getPower());
        }
        return List.of(stack);
    }

    /** The item's persistent lines, as {@code IPersistentInfoProvider.addInformation} wrote them. */
    public List<net.minecraft.network.chat.Component> persistentInfo(ItemStack stack) {
        List<net.minecraft.network.chat.Component> list = new ArrayList<>();
        list.add(EnergyNetBlockItem.gold("Stores up to " + Library.getShortNumber(power) + "HE"));
        list.add(EnergyNetBlockItem.gold("Charge speed: " + Library.getShortNumber(power / 200) + "HE"));
        list.add(EnergyNetBlockItem.gold("Discharge speed: " + Library.getShortNumber(power / 600) + "HE"));
        Long stored = ModEnergyNetContent.CAPACITOR_POWER.get(stack).orElse(null);
        if (stored != null) {
            list.add(net.minecraft.network.chat.Component.literal(Library.getShortNumber(stored) + "/"
                    + Library.getShortNumber(power) + "HE").withStyle(net.minecraft.ChatFormatting.YELLOW));
        }
        return list;
    }
}
