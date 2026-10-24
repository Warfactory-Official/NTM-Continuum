package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.ModEnergyNetContent;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.storage.TileEntityMachineCapacitor;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jspecify.annotations.Nullable;

/** The copper capacitor is a mounted passive HE buffer; the other registrations are decorative. */
public class MachineCapacitor extends CompatBlock implements TickingBlock, IEnergyConnectorBlock,
        ICapabilityBlock, ILookOverlay {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public final long maxPower;
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().powerIn().powerOut().fe()
            .faces(TileEntityMachineCapacitor.class, (owner, side) -> side == owner.getBlockState().getValue(FACING));

    public MachineCapacitor(Properties properties) { this(properties, 0); }
    public MachineCapacitor(Properties properties, long maxPower) {
        super(properties);
        this.maxPower = maxPower;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override public MachineCaps caps() { return maxPower > 0 ? CAPS : MachineCaps.NONE; }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return maxPower > 0 ? new TileEntityMachineCapacitor(pos, state) : null;
    }
    @Override public boolean canConnect(BlockGetter level, BlockPos pos, ForgeDirection side) {
        return maxPower > 0 && side.toDirection() == level.getBlockState(pos).getValue(FACING);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TileEntityMachineCapacitor capacitor)
            ModEnergyNetContent.CAPACITOR_STATE.get(stack).ifPresent(capacitor::restore);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (maxPower == 0) return super.getDrops(state, params);
        var owner = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (owner == null) owner = params.getLevel().getBlockEntity(BlockPos.containing(params.getParameter(LootContextParams.ORIGIN)));
        return List.of(storedStack(owner));
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return storedStack(level.getBlockEntity(pos));
    }

    private ItemStack storedStack(@Nullable BlockEntity owner) {
        var stack = new ItemStack(this);
        if (owner instanceof TileEntityMachineCapacitor capacitor)
            ModEnergyNetContent.CAPACITOR_STATE.set(stack, capacitor.carriedState());
        return stack;
    }

    public List<Component> persistentInfo(ItemStack stack) {
        var state = ModEnergyNetContent.CAPACITOR_STATE.get(stack);
        if (state.isEmpty()) return List.of();
        return List.of(Component.literal(Library.getShortNumber(state.get().power()) + "/"
                + Library.getShortNumber(state.get().maxPower()) + "HE").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public ILookOverlay.@Nullable Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityMachineCapacitor capacitor)) return null;
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, List.of(
                Library.getShortNumber(capacitor.getPower()) + "/" + Library.getShortNumber(capacitor.getMaxPower()) + " HE",
                "Received: " + Library.getShortNumber(capacitor.powerReceivedThisTick()) + " HE/t",
                "Sent: " + Library.getShortNumber(capacitor.powerSentThisTick()) + " HE/t"));
    }
}
