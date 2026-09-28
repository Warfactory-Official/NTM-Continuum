package com.hbm.items.tool;

import com.hbm.Constants;
import com.hbm.api.item.IDepthRockTool;
import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.IBaseAbility;
import com.hbm.handler.ability.IToolAreaAbility;
import com.hbm.handler.ability.IToolHarvestAbility;
import com.hbm.handler.ability.ToolPreset;
import com.hbm.packet.PlayerInformPayload;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.data.Tags;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.CompatToolItem;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.Harvests;
import dev.golem.core.world.ToolTier;
import dev.golem.core.world.Tools;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 1.7 {@code ItemToolAbility}: a tool whose selected preset rewrites what mining does — which blocks come
 * with the one being mined (area abilities) and what they turn into (harvest abilities) — plus weapon
 * abilities on hit.
 *
 * <p>1.7 took the break over in {@code onBlockStartBreak}; the port uses Core's
 * {@link #compatCanDestroyBlock} (26.1 {@code canDestroyBlock}, older {@code canAttackBlock}), which runs
 * at the same point of the server's destroy sequence and likewise stops the vanilla break when it answers
 * false. Presets live in the stack under {@link #ABILITY_CONFIG}, in 1.7's NBT layout.
 */
public class ItemToolAbility extends CompatToolItem implements IDepthRockTool {

    /** 1.7 stored {@code ability} and {@code abilityPresets} in the stack tag; this compound holds both. */
    public static final GolemComponent<CompoundTag> ABILITY_CONFIG =
            GolemComponent.compound(Constants.MOD_ID, "tool_ability");

    protected boolean isShears = false;
    protected final EnumToolType toolType;
    protected final float damage;
    protected final double movement;
    protected final AvailableAbilities availableAbilities = new AvailableAbilities().addToolAbilities();
    protected boolean rockBreaker = false;

    /**
     * 1.7 {@code EnumToolType}. The block and material lists are the modern mineable tags; the miner is the
     * pickaxe and shovel at once.
     */
    public enum EnumToolType {
        PICKAXE(BlockTags.MINEABLE_WITH_PICKAXE),
        AXE(BlockTags.MINEABLE_WITH_AXE),
        SHOVEL(BlockTags.MINEABLE_WITH_SHOVEL),
        MINER(BlockTags.MINEABLE_WITH_PICKAXE, BlockTags.MINEABLE_WITH_SHOVEL);

        public final List<TagKey<Block>> mineable;

        @SafeVarargs
        EnumToolType(TagKey<Block>... mineable) {
            this.mineable = List.of(mineable);
        }

        /**
         * 1.7 had no attack cooldown; the port swings at vanilla's speed for the tool's class and tier so a
         * modern client does not treat every HBM tool as a fist.
         */
        public float attackSpeed(int level) {
            return switch (this) {
                case PICKAXE, MINER -> Tools.PICKAXE_SPEED;
                case SHOVEL -> Tools.SHOVEL_SPEED;
                case AXE -> Tools.axeSpeed(level);
            };
        }
    }

    public ItemToolAbility(Properties properties, float damage, double movement, ToolTier material, EnumToolType type) {
        super(properties, material, type.mineable, damage, type.attackSpeed(material.level()), movement);
        this.damage = damage;
        this.movement = movement;
        this.toolType = type;
    }

    public ItemToolAbility setShears() {
        this.isShears = true;
        return this;
    }

    public ItemToolAbility addAbility(IBaseAbility ability, int level) {
        this.availableAbilities.addAbility(ability, level);
        return this;
    }

    public ItemToolAbility setDepthRockBreaker() {
        this.rockBreaker = true;
        return this;
    }

    public AvailableAbilities getAvailableAbilities() {
        return availableAbilities;
    }

    // --- weapon abilities ---

    @Override
    protected void compatHurtEnemy(ItemStack stack, LivingEntity victim, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel world && attacker instanceof Player player && canOperate(stack)) {
            this.availableAbilities.getWeaponAbilities().forEach((ability, level) ->
                    ability.onHit(level, world, player, victim, this));
        }
        // 1.7 stack.damageItem(1): Core damages durability tools; charge and fuel tools spend here.
        spendOnHit(stack, attacker);
    }

    /** What a hit costs besides durability; the powered and fueled tools pay in charge or fuel. */
    protected void spendOnHit(ItemStack stack, LivingEntity attacker) {
    }

    // --- mining ---

    /** Where everything this dig drops lands: 1.7's static {@code dropX/Y/Z}, the block actually mined. */
    public static BlockPos dropPos = BlockPos.ZERO;
    /** The stack drops are rolled against; silk and fortune presets enchant this copy for the dig. */
    public static ItemStack harvestTool = ItemStack.EMPTY;

    @Override
    protected boolean compatCanDestroyBlock(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                            LivingEntity user) {
        if (!(level instanceof ServerLevel world) || !(user instanceof ServerPlayer player)) return true;

        // 1.7: keyholes are neither processable nor silk-touchable, so they always take vanilla's own break -
        // breakExtraBlock refuses the Strange Stone, which would otherwise desync the client's removal
        if (state.is(com.hbm.blocks.ModDungeonBlocks.stone_keyhole.get())
                || state.is(com.hbm.blocks.ModDungeonBlocks.stone_keyhole_meta.get())) return true;
        if ((canHarvestBlock(state, stack) || canShearBlock(state, stack)) && canOperate(stack)) {
            Configuration config = getConfiguration(stack);
            ToolPreset preset = config.getActivePreset();

            dropPos = pos.immutable();
            harvestTool = stack.copy();

            preset.harvestAbility.preHarvestAll(preset.harvestAbilityLevel, world, player);

            boolean skipRef = preset.areaAbility.onDig(preset.areaAbilityLevel, world, pos, player, this);

            if (!skipRef) {
                breakExtraBlock(world, pos, player, pos);
            }

            preset.harvestAbility.postHarvestAll(preset.harvestAbilityLevel, world, player);
            harvestTool = ItemStack.EMPTY;

            // The dig broke the block itself; the vanilla break must not run a second time.
            return false;
        }

        return true;
    }

    @Override
    protected float compatDestroySpeed(ItemStack stack, BlockState state) {
        if (!canOperate(stack)) return 1;
        return super.compatDestroySpeed(stack, state);
    }

    public boolean canOperate(ItemStack stack) {
        return true;
    }

    /** 1.7 {@code canHarvestBlock}: the silk preset claims everything, otherwise any block it digs fast. */
    public boolean canHarvestBlock(BlockState state, ItemStack stack) {
        if (!canOperate(stack)) return false;
        if (this.getConfiguration(stack).getActivePreset().harvestAbility == IToolHarvestAbility.SILK) return true;
        return getDestroySpeed(stack, state) > 1;
    }

    @Override
    protected boolean compatIsCorrectToolForDrops(ItemStack stack, BlockState state) {
        // 1.20.1 Fabric asks without a stack; answer for an operable tool with no preset.
        if (stack.isEmpty()) return super.compatIsCorrectToolForDrops(stack, state);
        if (!canOperate(stack)) return false;
        if (getConfiguration(stack).getActivePreset().harvestAbility == IToolHarvestAbility.SILK) return true;
        return super.compatIsCorrectToolForDrops(stack, state);
    }

    @Override
    public boolean canBreakRock(Level level, Player player, ItemStack tool, BlockState state, BlockPos pos) {
        return canOperate(tool) && this.rockBreaker;
    }

    /**
     * 1.7 {@code canShearBlock}: a shears tool claims whatever {@code IShearable} covered — leaves, vines,
     * grass, wool, webs. Vanilla shears' own speed answers the same question on every target.
     */
    public boolean canShearBlock(BlockState state, ItemStack stack) {
        return this.isShears(stack) && new ItemStack(Items.SHEARS).getDestroySpeed(state) > 1.0F;
    }

    public boolean isShears(ItemStack stack) {
        return this.isShears;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return super.isFoil(stack) || !getConfiguration(stack).getActivePreset().isNone();
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        availableAbilities.addInformation(list);

        if (this.rockBreaker) {
            list.add(Component.literal(""));
            list.add(Component.literal("Can break depth rock!").withStyle(ChatFormatting.RED));
        }
    }

    /** 1.7 {@code breakExtraBlock}: one block the dig claimed, run through the active harvest ability. */
    public void breakExtraBlock(ServerLevel world, BlockPos pos, ServerPlayer player, BlockPos ref) {
        if (world.isEmptyBlock(pos)) return;

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return;

        BlockState state = world.getBlockState(pos);

        if (!(canHarvestBlock(state, stack) || canShearBlock(state, stack))
                || (state.getDestroySpeed(world, pos) == -1.0F && state.getDestroyProgress(player, world, pos) == 0.0F)
                || state.is(com.hbm.blocks.ModDungeonBlocks.stone_keyhole.get()))
            return;

        BlockState refState = world.getBlockState(ref);
        float refStrength = refState.getDestroyProgress(player, world, ref);
        float strength = state.getDestroyProgress(player, world, pos);

        if (!player.hasCorrectToolForDrops(state) || refStrength / strength > 10f || refStrength < 0) return;

        if (player.blockActionRestricted(world, pos, player.gameMode.getGameModeForPlayer())) return;
        if (!Harvests.fireBreakEvent(world, player, pos, state)) return;

        ToolPreset preset = getConfiguration(stack).getActivePreset();
        preset.harvestAbility.onHarvestBlock(preset.harvestAbilityLevel, world, pos, player, state);
    }

    /**
     * 1.7 {@code standardDigPost}: breaks the block the conventional way, except that the drops land on
     * {@link #dropPos}, and experience is not dropped (1.7 left that commented out).
     */
    public static void standardDigPost(ServerLevel world, BlockPos pos, ServerPlayer player) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        BlockEntity blockEntity = world.getBlockEntity(pos);

        world.levelEvent(null, 2001, pos, Block.getId(state));

        if (player.isCreative()) {
            block.playerWillDestroy(world, pos, state, player);
            if (world.removeBlock(pos, false)) block.destroy(world, pos, state);
            return;
        }

        ItemStack itemstack = player.getMainHandItem();
        ItemStack before = itemstack.copy();
        boolean canHarvest = player.hasCorrectToolForDrops(state);

        block.playerWillDestroy(world, pos, state, player);
        boolean removed = world.removeBlock(pos, false);
        if (removed) block.destroy(world, pos, state);

        if (!itemstack.isEmpty()) {
            itemstack.mineBlock(world, state, pos, player);
        }

        if (removed && canHarvest) {
            player.awardStat(Stats.BLOCK_MINED.get(block));
            player.causeFoodExhaustion(0.005F);
            ItemStack rollWith = harvestTool.isEmpty() ? before : harvestTool;
            for (ItemStack drop : Block.getDrops(state, world, pos, blockEntity, player, rollWith)) {
                Block.popResource(world, dropPos, drop);
            }
            state.spawnAfterBreak(world, pos, rollWith, false);
        }
    }

    /** Wears the tool by {@code amount} uses; the powered and fueled tools spend charge or fuel instead. */
    public void damageTool(ItemStack stack, int amount, LivingEntity user) {
        Tools.hurt(stack, amount, user);
    }

    // --- presets ---

    public static class Configuration {
        public List<ToolPreset> presets;
        public int currentPreset;

        public Configuration() {
            this.presets = null;
            this.currentPreset = 0;
        }

        public Configuration(List<ToolPreset> presets, int currentPreset) {
            this.presets = presets;
            this.currentPreset = currentPreset;
        }

        public void writeToNBT(CompoundTag nbt) {
            nbt.putInt("ability", currentPreset);

            ListTag nbtPresets = new ListTag();
            for (ToolPreset preset : presets) {
                CompoundTag nbtPreset = new CompoundTag();
                preset.writeToNBT(nbtPreset);
                nbtPresets.add(nbtPreset);
            }

            nbt.put("abilityPresets", nbtPresets);
        }

        public void readFromNBT(CompoundTag nbt) {
            currentPreset = Tags.getIntOr(nbt, "ability", 0);

            ListTag nbtPresets = Tags.getListOrEmpty(nbt, "abilityPresets");
            int numPresets = Math.min(nbtPresets.size(), 99);

            presets = new ArrayList<>(numPresets);

            for (int i = 0; i < numPresets; i++) {
                CompoundTag nbtPreset = Tags.getCompoundOrEmpty(nbtPresets, i);
                ToolPreset preset = new ToolPreset();
                preset.readFromNBT(nbtPreset);
                presets.add(preset);
            }

            currentPreset = Math.max(0, Math.min(currentPreset, presets.size() - 1));
        }

        public void reset(AvailableAbilities availableAbilities) {
            currentPreset = 0;

            presets = new ArrayList<>(availableAbilities.size());
            presets.add(new ToolPreset());

            availableAbilities.getToolAreaAbilities().forEach((ability, level) -> {
                if (ability == IToolAreaAbility.NONE) return;
                presets.add(new ToolPreset(ability, level, IToolHarvestAbility.NONE, 0));
            });

            availableAbilities.getToolHarvestAbilities().forEach((ability, level) -> {
                if (ability == IToolHarvestAbility.NONE) return;
                presets.add(new ToolPreset(IToolAreaAbility.NONE, 0, ability, level));
            });

            presets.sort(
                    Comparator.comparing((ToolPreset p) -> p.harvestAbility)
                            .thenComparingInt(p -> p.harvestAbilityLevel)
                            .thenComparing(p -> p.areaAbility)
                            .thenComparingInt(p -> p.areaAbilityLevel)
            );
        }

        public void restrictTo(AvailableAbilities availableAbilities) {
            for (ToolPreset preset : presets) {
                preset.restrictTo(availableAbilities);
            }
        }

        public ToolPreset getActivePreset() {
            return presets.get(currentPreset);
        }
    }

    public Configuration getConfiguration(ItemStack stack) {
        Configuration config = new Configuration();
        CompoundTag tag = stack == null || stack.isEmpty() ? null : ABILITY_CONFIG.get(stack).orElse(null);

        if (tag == null || !Tags.contains(tag, "ability") || !Tags.contains(tag, "abilityPresets")) {
            config.reset(availableAbilities);
            return config;
        }

        config.readFromNBT(tag);
        if (config.presets.isEmpty()) {
            config.reset(availableAbilities);
            return config;
        }
        config.restrictTo(availableAbilities);
        return config;
    }

    public void setConfiguration(ItemStack stack, Configuration config) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = new CompoundTag();
        config.writeToNBT(tag);
        ABILITY_CONFIG.set(stack, tag);
    }

    /** 1.7 {@code receiveControl}: the configuration screen's result, restricted to what this tool offers. */
    public void receiveControl(ItemStack stack, CompoundTag data) {
        Configuration config = new Configuration();
        config.readFromNBT(data);
        if (config.presets.isEmpty()) return;
        config.restrictTo(availableAbilities);
        setConfiguration(stack, config);
    }

    // --- cycling ---

    /**
     * 1.7 cycled on the ability key, which defaults to the use button, and left the click alone when the
     * aimed block had its own activation. Modern vanilla already offers the item only the clicks no block
     * consumed, so right-clicking air or an inert block cycles, sneaking goes back to the first preset.
     */
    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        return cycle(level, player, player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return cycle(context.getLevel(), player, context.getItemInHand());
    }

    protected InteractionResult cycle(Level world, Player player, ItemStack stack) {
        if (!canOperate(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        Configuration config = getConfiguration(stack);
        if (config.presets.size() < 2) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(player instanceof ServerPlayer playerMP)) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            config.currentPreset = 0;
        } else {
            config.currentPreset = (config.currentPreset + 1) % config.presets.size();
        }

        setConfiguration(stack, config);
        GolemNetwork.sendToPlayer(playerMP, PlayerInformPayload.CODEC, config.getActivePreset().informPayload());
        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.25F, config.getActivePreset().isNone() ? 0.75F : 1.25F);
        return InteractionResult.SUCCESS;
    }
}
