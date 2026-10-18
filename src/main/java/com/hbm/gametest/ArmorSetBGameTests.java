package com.hbm.gametest;

import com.hbm.blocks.ModArmorSetBBlocks;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModArmorSetBItems;
import com.hbm.items.ModMedicineItems;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.items.gear.ArmorFSBFueled;
import com.hbm.items.special.ItemCell;
import com.hbm.items.special.ItemStarterKit;
import com.hbm.util.EnchantmentUtil;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

import java.util.List;

/**
 * GameTests for the armour set B: fuelled-suit fill/burn/enable rules and the steam suit's per-piece speed
 * modifier, the four IV bags, and the kit give-lists, which must not skip unresolved ids.
 */
public final class ArmorSetBGameTests {

    private ArmorSetBGameTests() {}

    private static Player player(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        return player;
    }

    private static void wear(Player player, Item helmet, Item plate, Item legs, Item boots) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmet));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(plate));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots));
    }

    /** 1.7 ArmorFSBFueled: spawns full, 500 mB per fill call, only its own fuel, damage x50, empty disables the set. */
    public static void fueled(GameTestHelper helper) {
        Player player = player(helper);
        wear(player, ModArmorSetBItems.steamsuit_helmet.get(), ModArmorSetBItems.steamsuit_plate.get(),
                ModArmorSetBItems.steamsuit_legs.get(), ModArmorSetBItems.steamsuit_boots.get());
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ArmorFSBFueled suit = (ArmorFSBFueled) chest.getItem();
        helper.assertTrue(suit.getFill(chest) == 64_000, "fresh steam suit holds " + suit.getFill(chest));
        helper.assertTrue(ArmorFSB.hasFSBArmor(player), "full steam suit not detected");

        suit.burn(chest, 2);
        helper.assertTrue(suit.getFill(chest) == 63_900, "2 damage burnt to " + suit.getFill(chest) + ", expected 63900");
        suit.setFill(chest, 0);
        helper.assertTrue(!ArmorFSB.hasFSBArmor(player), "an empty piece still enabled the set");
        helper.assertTrue(suit.tryFill(Fluids.WATER, 1000, chest) == 1000, "steam suit took water");
        int left = suit.tryFill(Fluids.STEAM, 1000, chest);
        helper.assertTrue(left == 500 && suit.getFill(chest) == 500, "fill rate 500: left " + left + ", fill "
                + suit.getFill(chest));
        helper.assertTrue(ArmorFSB.hasFSBArmor(player), "refuelled set not re-enabled");

        // four -0.025 multiply-base modifiers: 0.1 * (1 - 0.1)
        ModArmorSetBItems.tickPlayer(player);
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(Math.abs(speed - 0.09D) < 1e-6, "steam suit speed " + speed + ", expected 0.09");
        wear(player, Items.AIR, Items.AIR,
                Items.AIR, Items.AIR);
        ModArmorSetBItems.tickPlayer(player);
        speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(Math.abs(speed - 0.1D) < 1e-6, "speed not restored without the suit: " + speed);

        ItemStack diesel = new ItemStack(ModArmorSetBItems.dieselsuit_plate.get());
        ArmorFSBFueled dieselItem = (ArmorFSBFueled) diesel.getItem();
        dieselItem.setFill(diesel, 0);
        dieselItem.tryFill(Fluids.DIESEL_CRACK, 300, diesel);
        helper.assertTrue(dieselItem.getFill(diesel) == 300, "diesel suit refused cracked diesel");
        helper.succeed();
    }

    /** The IV bags: -5/+5 health and 100 experience each way. */
    public static void ivBags(GameTestHelper helper) {
        Player player = player(helper);
        player.setHealth(20F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModMedicineItems.iv_empty.get()));
        player.getItemInHand(InteractionHand.MAIN_HAND).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(Math.abs(player.getHealth() - 15F) < 1e-4, "IV bag drew to " + player.getHealth());
        helper.assertTrue(player.getMainHandItem().is(ModArmorSetBItems.iv_blood.get()), "no blood bag in hand");
        player.getItemInHand(InteractionHand.MAIN_HAND).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(Math.abs(player.getHealth() - 20F) < 1e-4, "blood bag healed to " + player.getHealth());
        helper.assertTrue(player.getMainHandItem().is(ModMedicineItems.iv_empty.get()), "blood bag left no IV bag");

        // 1.7 getTotalExperience adds cap * progress into an int, truncating the float bar: +-1 point per read is 1.7's.
        EnchantmentUtil.setExperience(player, 150);
        helper.assertTrue(near(EnchantmentUtil.getTotalExperience(player), 150),
                "set 150 xp, reads " + EnchantmentUtil.getTotalExperience(player));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModArmorSetBItems.iv_xp_empty.get()));
        player.getItemInHand(InteractionHand.MAIN_HAND).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(near(EnchantmentUtil.getTotalExperience(player), 50),
                "experience bag left " + EnchantmentUtil.getTotalExperience(player));
        helper.assertTrue(player.getMainHandItem().is(ModArmorSetBItems.iv_xp.get()), "no full experience bag");
        player.getItemInHand(InteractionHand.MAIN_HAND).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(near(EnchantmentUtil.getTotalExperience(player), 150),
                "full bag gave back to " + EnchantmentUtil.getTotalExperience(player));
        // below 100 the empty bag does nothing
        EnchantmentUtil.setExperience(player, 99);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModArmorSetBItems.iv_xp_empty.get()));
        player.getItemInHand(InteractionHand.MAIN_HAND).use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(ModArmorSetBItems.iv_xp_empty.get())
                && near(EnchantmentUtil.getTotalExperience(player), 99), "empty bag filled from 99 xp");
        helper.succeed();
    }

    private static boolean near(int value, int expected) {
        return Math.abs(value - expected) <= 2;
    }

    private static int count(List<ItemStack> list, String id) {
        int n = 0;
        for (ItemStack stack : list) {
            if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(id)) n += stack.getCount();
        }
        return n;
    }

    /** The kit entries resolve to the port's ids and the component cells, none skipped. */
    public static void kits(GameTestHelper helper) {
        List<ItemStack> starter = ItemStarterKit.contents(ItemStarterKit.Kind.NUKE_STARTER);
        helper.assertTrue(starter.size() == 18, "starter kit has " + starter.size() + " stacks, expected 18");
        helper.assertTrue(count(starter, "hbm:machine_industrial_turbine") == 2 && count(starter, "hbm:machine_reactor") == 0,
                "starter kit has the industrial turbine and no reactors");
        List<ItemStack> advanced = ItemStarterKit.contents(ItemStarterKit.Kind.NUKE_ADVANCED);
        helper.assertTrue(count(advanced, "hbm:machine_rtg_grey") == 1 && count(advanced, "hbm:pile_brick") == 125
                && count(advanced, "hbm:machine_industrial_turbine") == 4, "advanced kit machines missing");
        int emptyCells = 0;
        for (ItemStack stack : advanced) if (ItemCell.isEmptyCell(stack)) emptyCells += stack.getCount();
        helper.assertTrue(emptyCells == 32, "advanced kit has " + emptyCells + " empty cells, expected 32");
        List<ItemStack> commercial = ItemStarterKit.contents(ItemStarterKit.Kind.NUKE_COMMERCIALLY);
        helper.assertTrue(count(commercial, "hbm:iv_blood") == 8, "commercial kit blood bags missing");
        helper.assertTrue(count(ItemStarterKit.contents(ItemStarterKit.Kind.GADGET), "hbm:gadget_explosive8") == 4
                && count(ItemStarterKit.contents(ItemStarterKit.Kind.MAN), "hbm:gadget_explosive8") == 4
                && count(ItemStarterKit.contents(ItemStarterKit.Kind.MIKE), "hbm:man_explosive8") == 4
                && count(ItemStarterKit.contents(ItemStarterKit.Kind.TSAR), "hbm:man_explosive8") == 4,
                "explosive lenses missing from a bomb kit");
        int sas = 0;
        for (ItemStack stack : ItemStarterKit.contents(ItemStarterKit.Kind.PROTOTYPE)) {
            if (ItemCell.isFullCell(stack, Fluids.SAS3)) sas += stack.getCount();
        }
        helper.assertTrue(sas == 4, "prototype kit has " + sas + " SAS3 cells, expected 4");
        List<ItemStack> euph = ItemStarterKit.contents(ItemStarterKit.Kind.EUPHEMIUM);
        helper.assertTrue(euph.size() == 5 && euph.get(4).is(ModArmorSetBBlocks.statue_elb_f_item.get()),
                "euphemium kit statue missing");
        helper.succeed();
    }
}
