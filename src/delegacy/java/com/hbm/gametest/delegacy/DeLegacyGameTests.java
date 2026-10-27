package com.hbm.gametest.delegacy;

import com.hbm.Constants;
import com.hbm.delegacy.legacy.LegacyStacks;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModArmorModItems;
import com.hbm.items.ModDataComponents;
import dev.golem.core.data.ItemContents;
import dev.golem.core.gametest.GameTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** The {@code hbm:delegacy_*} GameTests: a DE stack decoded with no converter in between. */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class DeLegacyGameTests {
    static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    static {
        TESTS.function("delegacy_aliases", 20, () -> DeLegacyGameTests::aliases);
        TESTS.function("delegacy_legacy_stacks", 20, () -> DeLegacyGameTests::legacyStacks);
        TESTS.function("delegacy_armor_mods", 20, () -> DeLegacyGameTests::armorMods);
    }

    private DeLegacyGameTests() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        GameTests.register(event, TESTS);
    }

    private static void aliases(GameTestHelper helper) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "hbm:wire_fine_mingrade");
        tag.putInt("count", 3);
        ItemStack stack = ItemStack.parse(helper.getLevel().registryAccess(), tag).orElse(ItemStack.EMPTY);
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("hbm:wire_fine_red_copper")
                && stack.getCount() == 3, "a DE stack loads as its Continuum item: " + stack);
        helper.assertTrue(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("hbm:reactor_zirnox"))
                == BuiltInRegistries.BLOCK.get(ResourceLocation.parse("hbm:machine_zirnox")), "block alias");
        helper.assertTrue(BuiltInRegistries.ITEM.get(ResourceLocation.parse("hbm:capacitor_copper"))
                != BuiltInRegistries.ITEM.get(ResourceLocation.parse("hbm:machine_capacitor_copper")),
                "a Continuum id is never aliased away");
        for (String id : new String[]{"hbm:ingot_metal_key", "hbm:bismuth_tool", "hbm:memory"}) {
            CompoundTag de = new CompoundTag();
            de.putString("id", id);
            de.putInt("count", 1);
            helper.assertTrue(!ItemStack.parse(helper.getLevel().registryAccess(), de).orElse(ItemStack.EMPTY).isEmpty(),
                    "the 1.7 item DE kept loads as itself: " + id);
        }
        helper.succeed();
    }

    /** A DE split item with a DE-only component decodes, then converts with the importer's rules. */
    private static void legacyStacks(GameTestHelper helper) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "hbm:canister");
        tag.putInt("count", 2);
        CompoundTag components = new CompoundTag();
        components.putLong("hbm:anchor_pos", BlockPos.asLong(10, 64, -3));
        tag.put("components", components);
        ItemStack stack = ItemStack.parse(helper.getLevel().registryAccess(), tag).orElse(ItemStack.EMPTY);
        helper.assertTrue(stack.getCount() == 2 && LegacyStacks.isLegacy(stack), "a DE stack decodes as a legacy stack: " + tag);
        SimpleContainer chest = new SimpleContainer(stack, new ItemStack(Items.STONE));
        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(LegacyStacks.convertAll(chest, server, "gametest") == 1, "only the DE stack converts");
        CompoundTag out = (CompoundTag) chest.getItem(0).save(helper.getLevel().registryAccess());
        helper.assertTrue(out.getString("id").equals("hbm:canister_empty") && out.getInt("count") == 2,
                "the split resolves: " + out);
        helper.assertTrue(out.getCompound("components").contains("hbm:anchor_position")
                && !out.getCompound("components").contains("hbm:anchor_pos"), "the component converts: " + out);
        helper.assertTrue(!LegacyStacks.isLegacy(chest.getItem(0)) && chest.getItem(1).is(Items.STONE), "nothing else changed");
        helper.succeed();
    }

    /** DE's {@code [{slot, item}]} armour modules decode without the importer, into Continuum's nine positions. */
    private static void armorMods(GameTestHelper helper) {
        CompoundTag module = new CompoundTag();
        module.putString("id", "hbm:servo_set");
        module.putInt("count", 1);
        CompoundTag socket = new CompoundTag();
        socket.putInt("slot", 2);
        socket.put("item", module);
        ListTag mods = new ListTag();
        mods.add(socket);
        CompoundTag components = new CompoundTag();
        components.put("hbm:armor_mods", mods);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "hbm:steel_plate");
        tag.putInt("count", 1);
        tag.put("components", components);
        ItemStack stack = ItemStack.parse(helper.getLevel().registryAccess(), tag).orElse(ItemStack.EMPTY);
        helper.assertTrue(!stack.isEmpty(), "a DE armour piece with modules loads: " + tag);
        List<ItemStack> slots = ModDataComponents.ARMOR_MODS.get(stack).map(ItemContents::copies).orElse(List.of());
        helper.assertTrue(slots.size() == ArmorModHandler.MOD_SLOTS && slots.get(2).is(ModArmorModItems.servo_set.get())
                && slots.get(0).isEmpty(), "the module lands in its slot: " + slots);
        CompoundTag saved = (CompoundTag) stack.save(helper.getLevel().registryAccess());
        ListTag out = saved.getCompound("components").getList("hbm:armor_mods", Tag.TAG_COMPOUND);
        helper.assertTrue(out.size() == ArmorModHandler.MOD_SLOTS && !out.getCompound(2).contains("slot"),
                "it saves back in Continuum's form: " + saved);
        helper.succeed();
    }
}
