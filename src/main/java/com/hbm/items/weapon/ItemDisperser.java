package com.hbm.items.weapon;

import com.hbm.entity.grenade.EntityDisperserCanister;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemFluidTank;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1.7 ItemDisperser: a throwable fluid tank that bursts into a gas cloud; the two variants differ in capacity,
 * overlay icon, creative fluids and display-name word order. Copied from ntm-next onto this port's
 * {@link ItemFluidTank} (typed by {@link FluidType} rather than a vanilla {@code Fluid}).
 */
public class ItemDisperser extends ItemFluidTank {

    private final Kind kind;
    private final String base;
    private final Identifier icon;
    private final Identifier overlayIcon;

    public ItemDisperser(Item.Properties properties, int capacity, String base, Kind kind, Identifier icon,
                         Identifier overlayIcon) {
        super(properties, capacity, base);
        this.kind = kind;
        this.base = base;
        this.icon = icon;
        this.overlayIcon = overlayIcon;
    }

    /** The fluid this stack carries ({@code Fluids.NONE} when empty). */
    public FluidType getContent(ItemStack stack) {
        return getType(stack);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            // 1.7: "random.bow", pitch 0.4 / (rand*0.4 + 0.8); 1.7 wins.
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT,
                    SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            EntityDisperserCanister canister = new EntityDisperserCanister(level, player);
            canister.setDisperserItem(this);
            canister.setFluid(getContent(stack));
            level.addFreshEntity(canister);
            dev.golem.core.world.ItemStacks.consume(stack, 1, player);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * 1.7 {@code FluidContainerRegistry} registers a full canister and a full gland only for
     * {@link FluidType#isDispersable() dispersable} fluids (not antimatter, not container-less, not viscous), so
     * nothing else can be filled into either.
     */
    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return type != null && type.isDispersable() && super.acceptsFluid(type, stack);
    }

    /**
     * 1.7 getSubItems: the canister offers every dispersable fluid, the gland pheromone and sulfuric acid; NARROWER
     * than {@link #acceptsFluid} for the gland - 1.7's registry lets a machine fill either with any dispersable fluid.
     */
    public boolean isCreativeVariant(FluidType fluid) {
        if (fluid == null || fluid == Fluids.NONE) return false;
        return switch (kind) {
            case CANISTER -> fluid.isDispersable();
            case GLAND -> fluid == Fluids.PHEROMONE || fluid == Fluids.SULFURIC_ACID;
        };
    }

    /**
     * The creative-tab stacks, one full item per {@link #isCreativeVariant} fluid. 1.7 walked
     * {@code Fluids.getInNiceOrder()}; the port has no nice order, so this is registration order.
     */
    public java.util.List<ItemStack> creativeVariants() {
        java.util.List<ItemStack> list = new java.util.ArrayList<>();
        for (FluidType fluid : Fluids.getAll()) {
            if (isCreativeVariant(fluid)) list.add(make(fluid, capacity));
        }
        return list;
    }

    /**
     * 1.7 getItemStackDisplayName: "<name> <fluid>" for the canister, "<fluid> <name>" for the gland, CONCATENATING two trimmed translations - the word order is in the Java, not the lang, and there is no empty branch ("...: None").
     */
    @Override
    public Component getName(ItemStack stack) {
        FluidType fluid = getContent(stack);
        Component name = fluid == null || fluid == Fluids.NONE
                ? Component.translatable("hbmfluid.none")
                : fluid.getDisplayName();
        String self = Component.translatable("item.hbm." + base).getString().trim();
        String other = name.getString().trim();
        return Component.literal(kind == Kind.GLAND ? other + " " + self : self + " " + other);
    }

    /**
     * 1.7 getIconFromDamageForRenderPass: pass 0 the body, pass 1 the fluid-tinted overlay; bare sprite names, no item/ prefix.
     */
    public Identifier iconTexture(int pass) {
        return pass == 1 ? overlayIcon : icon;
    }

    /**
     * 1.7 branched on {@code this == ModItems.disperser_canister}; the port carries the distinction explicitly.
     */
    public enum Kind {CANISTER, GLAND}

    // The passes ARE the model layers; the gland's overlay is the identifier's.
}
