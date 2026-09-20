package com.hbm.blocks.bomb;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.particle.helper.ExplosionCreator;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code BlockChargeSemtex}: semtex mining charge, VNT 10 at 32 rays, all drops with Fortune III, no damage. */
public class BlockChargeSemtex extends BlockChargeBase {

    public BlockChargeSemtex(Properties properties) {
        super(properties);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        removeSafely(level, pos);
        double x = pos.getX(), y = pos.getY(), z = pos.getZ();

        ExplosionVNT xnt = new ExplosionVNT(level, x + 0.5, y + 0.5, z + 0.5, 10F);
        xnt.setBlockAllocator(new BlockAllocatorStandard(32));
        xnt.setBlockProcessor(new BlockProcessorStandard().setAllDrop().setFortune(3));
        xnt.explode();
        ExplosionCreator.composeEffectSmall(level, x + 0.5, y + 1, z + 0.5);
        return BombReturnCode.DETONATED;
    }

    @Override
    public List<Component> tooltip() {
        List<Component> list = new ArrayList<>(super.tooltip());
        list.add(Component.literal("Will drop all blocks.").withStyle(ChatFormatting.BLUE));
        list.add(Component.literal("Does not do damage.").withStyle(ChatFormatting.BLUE));
        list.add(Component.literal("").withStyle(ChatFormatting.BLUE));
        list.add(Component.literal("Fortune III").withStyle(ChatFormatting.LIGHT_PURPLE));
        return list;
    }
}
