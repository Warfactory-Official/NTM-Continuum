package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.handler.ArmorModEvents;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.container.ContainerArmorTable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModArmorModItems;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.Armors;
import dev.golem.core.world.Gliders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * Armour modules on any armour (1.7 {@code ArmorModHandler.isApplicable} took any
 * {@code ItemArmor}), the port-only elytra module, and fillable items refuelling in the fluid tank's fill bay
 * (1.7 {@code FluidLoaderFillableItem}).
 */
public final class Pt1012ArmorGameTests {

    private Pt1012ArmorGameTests() {}

    /** A vanilla chestplate goes into the table, takes a module through the menu and the module works while worn. */
    public static void vanillaArmorTakesModules(GameTestHelper helper) {
        ArmorModEvents.init();
        helper.assertTrue(Armors.slotOf(new ItemStack(Items.IRON_CHESTPLATE)) == ArmorSlot.CHESTPLATE, "iron chestplate");
        helper.assertTrue(Armors.slotOf(new ItemStack(Items.DIAMOND_HELMET)) == ArmorSlot.HELMET, "diamond helmet");
        helper.assertTrue(Armors.slotOf(new ItemStack(Items.LEATHER_BOOTS)) == ArmorSlot.BOOTS, "leather boots");
        helper.assertTrue(Armors.slotOf(new ItemStack(Items.ELYTRA)) == null, "the elytra is not armour");
        helper.assertTrue(Armors.slotOf(new ItemStack(Items.CARVED_PUMPKIN)) == null, "a pumpkin is not armour");
        helper.assertTrue(Armors.slotOf(new ItemStack(ModArmorItems.steel_plate.get())) == ArmorSlot.CHESTPLATE,
                "HBM armour still resolves");

        Player player = player(helper);
        ContainerArmorTable menu = new ContainerArmorTable(0, player.getInventory(), player.blockPosition());
        ItemStack chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        helper.assertTrue(menu.getSlot(ContainerArmorTable.ARMOR_SLOT).mayPlace(chest),
                "the armor table refused a diamond chestplate");
        menu.getSlot(ContainerArmorTable.ARMOR_SLOT).setByPlayer(chest);
        ItemStack servos = new ItemStack(ModArmorModItems.servo_set.get());
        helper.assertTrue(menu.getSlot(ArmorModHandler.servos).mayPlace(servos), "servo slot refused servos on diamond");
        menu.getSlot(ArmorModHandler.servos).setByPlayer(servos);
        ItemStack installed = menu.getSlot(ContainerArmorTable.ARMOR_SLOT).getItem();
        helper.assertTrue(ArmorModHandler.pryMod(installed, ArmorModHandler.servos).is(servos.getItem()),
                "servos were not written into the diamond chestplate");
        // the player's own armour slots in the table follow vanilla's equip rule (1.7 isValidArmor)
        helper.assertTrue(menu.getSlot(ContainerArmorTable.ARMOR_SLOT + 2).mayPlace(new ItemStack(Items.IRON_CHESTPLATE)),
                "chest equipment slot refused an iron chestplate");
        helper.assertFalse(menu.getSlot(ContainerArmorTable.ARMOR_SLOT + 2).mayPlace(new ItemStack(Items.IRON_HELMET)),
                "chest equipment slot accepted a helmet");

        player.setItemSlot(EquipmentSlot.CHEST, installed.copy());
        ArmorModEvents.onLivingUpdate(player);
        double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(Math.abs(damage - 1.5D) < 1.0E-6D, "servos on a diamond chestplate: attack damage " + damage);
        helper.succeed();
    }

    /** port: the elytra module - chestplates only, shares the jetpack slot, glides, wears the stored elytra. */
    public static void elytraModuleGlides(GameTestHelper helper) {
        ArmorModEvents.init();
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        ItemStack elytra = new ItemStack(Items.ELYTRA);
        helper.assertTrue(ArmorModHandler.isApplicable(chest, elytra), "elytra not applicable to an iron chestplate");
        helper.assertFalse(ArmorModHandler.isApplicable(new ItemStack(Items.IRON_HELMET), elytra), "elytra on a helmet");
        helper.assertTrue(ArmorModHandler.getModuleSlot(elytra) == ArmorModHandler.plate_only, "elytra slot");
        helper.assertTrue(ArmorModHandler.getModuleSlot(new ItemStack(ModArmorItems.jetpack_glider.get()))
                == ArmorModHandler.plate_only, "the elytra shares the jetpack slot");
        ArmorModHandler.applyMod(chest, elytra);
        helper.assertTrue(Gliders.wingsFor(chest).is(Items.ELYTRA), "no wings for the modded chestplate");

        Player player = player(helper);
        player.setPos(player.getX(), player.getY() + 3D, player.getZ()); // mid-air, as a jump would be
        player.setOnGround(false);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        helper.assertTrue(Gliders.canGlide(player), "the installed elytra does not let the wearer glide");
        helper.assertTrue(player.tryToStartFallFlying() && player.isFallFlying(),
                "vanilla's start check (tryToStartFallFlying) refused the elytra chestplate");

        helper.assertTrue(Gliders.wearNow(player), "no glide source wore");
        ItemStack stored = ArmorModHandler.installedElytra(player.getItemBySlot(EquipmentSlot.CHEST));
        helper.assertTrue(stored.getDamageValue() == 1, "stored elytra damage " + stored.getDamageValue());

        // vanilla stops one point short of breaking; so does the module, and the elytra comes back out intact
        stored.setDamageValue(stored.getMaxDamage() - 1);
        ArmorModHandler.applyMod(player.getItemBySlot(EquipmentSlot.CHEST), stored);
        helper.assertFalse(Gliders.canGlide(player), "a worn-out elytra still glides");
        ContainerArmorTable menu = new ContainerArmorTable(0, player.getInventory(), player.blockPosition());
        menu.getSlot(ContainerArmorTable.ARMOR_SLOT).setByPlayer(player.getItemBySlot(EquipmentSlot.CHEST).copy());
        ItemStack back = menu.getSlot(ArmorModHandler.plate_only).getItem();
        helper.assertTrue(back.is(Items.ELYTRA) && back.getDamageValue() == back.getMaxDamage() - 1,
                "the table did not hand back the stored elytra with its damage");
        helper.succeed();
    }

    /** 1.7 FluidLoaderFillableItem: the fill bay refuels a jetpack, empty or partly filled, in place. */
    public static void tankFillsGlider(GameTestHelper helper) {
        Fluids.init();
        TileEntityMachineFluidTank tank = placeTank(helper);
        tank.tank.setTankType(Fluids.KEROSENE);
        tank.tank.setFill(30_000);
        ItemStack glider = new ItemStack(ModArmorItems.jetpack_glider.get());
        helper.assertTrue(tank.isItemValidForSlot(4, glider), "the fill bay refused an empty glider jetpack");
        tank.inventory.setStackInSlot(4, glider);
        TileEntityMachineFluidTank.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        ItemStack out = tank.inventory.getStackInSlot(5).isEmpty() ? tank.inventory.getStackInSlot(4)
                : tank.inventory.getStackInSlot(5);
        int fill = ModArmorItems.jetpack_glider.get().getFill(out);
        helper.assertTrue(fill == 20_000 && tank.tank.getFill() == 10_000,
                "glider fuel " + fill + ", tank left " + tank.tank.getFill());

        // a partly used glider is refuelled too (1.7 filled any IFillableItem whatever its fill)
        ModArmorItems.jetpack_glider.get().tryEmpty(Fluids.KEROSENE, 5_000, out);
        helper.assertTrue(tank.isItemValidForSlot(4, out), "the fill bay refused a partly filled glider");
        tank.inventory.setStackInSlot(4, out.copy());
        tank.inventory.setStackInSlot(5, ItemStack.EMPTY);
        TileEntityMachineFluidTank.serverTick(helper.getLevel(), tank.getBlockPos(), tank.getBlockState(), tank);
        ItemStack refilled = tank.inventory.getStackInSlot(5).isEmpty() ? tank.inventory.getStackInSlot(4)
                : tank.inventory.getStackInSlot(5);
        helper.assertTrue(ModArmorItems.jetpack_glider.get().getFill(refilled) == 20_000 && tank.tank.getFill() == 5_000,
                "partly filled glider not topped up: " + ModArmorItems.jetpack_glider.get().getFill(refilled));
        helper.assertFalse(tank.isItemValidForSlot(4, new ItemStack(Items.IRON_CHESTPLATE)),
                "the fill bay took a plain chestplate");

        helper.succeed();
    }

    private static TileEntityMachineFluidTank placeTank(GameTestHelper helper) {
        BlockPos relative = new BlockPos(3, 1, 3);
        var block = ModMachineBlocks.machine_fluidtank.get();
        BlockPos absolute = helper.absolutePos(relative);
        helper.setBlock(relative, block.defaultBlockState());
        block.fillSpace(helper.getLevel(), absolute, Direction.NORTH);
        return GameTestHelpers.blockEntity(helper, relative, TileEntityMachineFluidTank.class);
    }

    private static Player player(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        return player;
    }
}
