package com.hbm.blocks.generic;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.items.gear.ArmorFSBPowered;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import com.hbm.sound.EnergyNetSounds;
import com.hbm.tileentity.network.TileEntityHEVBattery;

/**
 * {@code hev_battery}: the Half-Life wall charger in block form. Right-clicked by a player in a full
 * powered FSB suit it adds 150 kHE to every battery-backed piece and vanishes. The block entity only
 * exists so the {@code battery.obj} model has a renderer to hang off (1.7 drew it with a block renderer).
 */
public class HEVBattery extends CompatBlock implements TickingBlock {

    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 6, 10);

    public HEVBattery(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHEVBattery(pos, state);
    }

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (ArmorFSB.hasFSBArmorIgnoreCharge(player)
                && player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ArmorFSBPowered) { // 1.7 armorInventory[3] is the helmet
            for (EquipmentSlot slot : ARMOR) {
                ItemStack st = player.getItemBySlot(slot);
                if (st.isEmpty() || !(st.getItem() instanceof IBatteryItem battery)) continue;
                long maxcharge = battery.getMaxCharge(st);
                long charge = battery.getCharge(st);
                battery.setCharge(st, Math.min(charge + 150000, maxcharge));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), EnergyNetSounds.ITEM_BATTERY.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            level.removeBlock(pos, false);
        }
        return InteractionResult.SUCCESS;
    }
}
