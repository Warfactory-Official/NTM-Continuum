package com.hbm.gametest;

import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.items.ModArmorSetItems;
import com.hbm.lib.ModDamageSources;
import com.hbm.lib.ModDamageSources.DamageClass;
import com.hbm.util.DamageResistanceHandler;
import dev.golem.core.Ids;
import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class DamageClassGameTests {

    private DamageClassGameTests() {}

    /** 1.7 NT grunt, five plates (DT 1, DR 0.1), 10 damage: plain 9 x 0.9, laser 9.5 x 0.95,
     * electric 9.75 x 0.975, subatomic 10 x 0.99, nuke 10 - 0.25. */
    public static void glyphidResistsDamageClasses(GameTestHelper helper) {
        EntityGlyphid glyphid = helper.spawnWithNoFreeWill(ModNtmEntities.GLYPHID.get(), new BlockPos(2, 2, 2));
        Map<ResourceKey<DamageType>, Float> expected = Map.of(
                DamageClass.PHYSICAL.type(), 8.1F, DamageClass.LASER.type(), 9.025F,
                DamageClass.ELECTRIC.type(), 9.50625F, DamageClass.SUBATOMIC.type(), 9.9F,
                ModDamageSources.LASER, 9.025F, ModDamageSources.NUCLEAR_BLAST, 9.75F);
        List<String> wrong = new ArrayList<>();
        expected.forEach((key, value) -> {
            float dealt = DamageResistanceHandler.calculateDamage(glyphid,
                    ModDamageSources.source(helper.getLevel(), key), 10F, 0F, 0F);
            if (Math.abs(dealt - value) > 1E-4F) wrong.add(Ids.keyId(key).getPath() + " " + dealt + " not " + value);
        });
        helper.assertTrue(wrong.isEmpty(), "glyphid damage after DT/DR: " + wrong);
        helper.succeed();
    }

    /** 1.7 {@code onEntityDamaged}: electric damage to anything wearing a powered chest is multiplied by 5 first. */
    public static void electricHitsPoweredChestFivefold(GameTestHelper helper) {
        LivingEntity electric = suited(helper, new BlockPos(1, 2, 1));
        LivingEntity microwave = suited(helper, new BlockPos(3, 2, 1));
        Compat.hurt(electric, helper.getLevel(),
                ModDamageSources.source(helper.getLevel(), DamageClass.ELECTRIC.type()), 1F);
        Compat.hurt(microwave, helper.getLevel(),
                ModDamageSources.source(helper.getLevel(), DamageClass.MICROWAVE.type()), 5F);
        float lostElectric = electric.getMaxHealth() - electric.getHealth();
        float lostMicrowave = microwave.getMaxHealth() - microwave.getHealth();
        helper.assertTrue(lostElectric > 0F && Math.abs(lostElectric - lostMicrowave) < 1E-4F,
                "1 electric took " + lostElectric + ", 5 microwave (same energy category) took " + lostMicrowave);
        helper.succeed();
    }

    private static LivingEntity suited(GameTestHelper helper, BlockPos pos) {
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, pos);
        zombie.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModArmorSetItems.hev_plate.get()));
        return zombie;
    }
}
