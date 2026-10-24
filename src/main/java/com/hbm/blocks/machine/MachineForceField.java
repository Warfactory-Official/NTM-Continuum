package com.hbm.blocks.machine;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.Direction;
import com.hbm.tileentity.machine.TileEntityForceField;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code MachineForceField}: the renderer-drawn emitter. A plain click opens its menu (a sneaking click is
 * swallowed without doing anything, as 1.7 returned true for it). While the field is up four particles rise
 * off the top each display tick (lava while the emitter is blinking red, red dust otherwise), and four
 * smoke puffs rise while it is cooling down. Breaking it spills the battery and the upgrades (the block
 * entity is a container, which vanilla empties on removal).
 */
public class MachineForceField extends CompatBlock implements TickingBlock, ICapabilityBlock {

    public MachineForceField(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityForceField(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (!level.isClientSide() && !player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TileEntityForceField field) {
            Menus.openWithPos(serverPlayer, field, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityForceField te)) return;

        if (te.isOn && te.cooldown == 0 && te.power > 0) {
            for (int i = 0; i < 4; i++) {
                double x = pos.getX() + rand.nextFloat();
                double y = pos.getY() + 2F;
                double z = pos.getZ() + rand.nextFloat();
                if (te.color == 0xFF0000) level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
                else level.addParticle(DustParticleOptions.REDSTONE, x, y, z, 0, 0, 0);
            }
        } else if (te.cooldown > 0) {
            for (int i = 0; i < 4; i++) {
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + rand.nextFloat(), pos.getY() + 2F,
                        pos.getZ() + rand.nextFloat(), 0, 0, 0);
            }
        }
    }

    /** Live connection rules of the corresponding 1.7 tile, shared by native and foreign contacts. */
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().powerIn().fe().items()
            .faces(BlockEntity.class, (be, side) -> side != Direction.UP);

    @Override
    public MachineCaps caps() { return CAPS; }
}
