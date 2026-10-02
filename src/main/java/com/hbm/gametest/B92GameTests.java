package com.hbm.gametest;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityB92Beam;
import com.hbm.items.ModGunItems;
import com.hbm.items.weapon.GunB92;
import com.hbm.items.weapon.GunB92Cell;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Focused server contracts for the separately implemented original B92 weapon pair. */
public final class B92GameTests {

    private B92GameTests() {}

    public static void registry(GameTestHelper helper) {
        helper.assertTrue(ModGunItems.gun_b92.get() instanceof GunB92, "B92 gun registration is missing");
        helper.assertTrue(ModGunItems.gun_b92_ammo.get() instanceof GunB92Cell, "B92 cell registration is missing");
        helper.assertTrue(ModEntities.BEAM_BOMB.get() != null, "B92 beam entity registration is missing");
        helper.succeed();
    }

    public static void firesBeams(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        // The mock player starts at the world origin, which a filtered run never loads: the beams would spawn
        // into an unloaded chunk and be dropped. Stand the player inside the test structure, which is loaded.
        player.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        ItemStack gun = new ItemStack(ModGunItems.gun_b92.get());
        GunB92.setPower(gun, 2);
        player.setItemSlot(EquipmentSlot.MAINHAND, gun);
        int before = helper.getLevel().getEntitiesOfClass(EntityB92Beam.class,
                player.getBoundingBox().inflate(64D)).size();

        ModGunItems.gun_b92.get().releaseUsing(gun, helper.getLevel(), player, 72_000 - 10);

        int after = helper.getLevel().getEntitiesOfClass(EntityB92Beam.class,
                player.getBoundingBox().inflate(64D)).size();
        helper.assertTrue(after == before + 2, "two stored charges must create two beam entities");
        helper.assertTrue(GunB92.getPower(gun) == 0, "firing must consume all stored B92 charge");
        helper.succeed();
    }

    public static void cellDrawsCharge(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack gun = new ItemStack(ModGunItems.gun_b92.get());
        ItemStack cell = new ItemStack(ModGunItems.gun_b92_ammo.get());
        GunB92.setPower(gun, 3);
        player.getInventory().setItem(0, gun);

        ModGunItems.gun_b92_ammo.get().tickCell(cell, helper.getLevel(), player);

        helper.assertTrue(GunB92.getPower(gun) == 2, "cell must draw one charge from a B92 with more than one charge");
        helper.assertTrue(GunB92Cell.getPower(cell) == 1, "cell must retain the drawn charge");
        helper.succeed();
    }
}
