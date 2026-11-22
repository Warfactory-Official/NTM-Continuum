package net.neoforged.neoforge.registries;
import java.util.function.*; import java.util.Collection; import net.minecraft.core.Registry; import net.minecraft.resources.ResourceKey; import net.neoforged.bus.api.IEventBus;
public class DeferredRegister<T> {
 public static <T> DeferredRegister<T> create(ResourceKey<? extends Registry<T>> k,String ns){return null;}
 public static Blocks createBlocks(String ns){return null;} public static Items createItems(String ns){return null;}
 public <I extends T> DeferredHolder<T,I> register(String n,Supplier<? extends I> s){return null;} public void register(IEventBus b){}
 public Collection<DeferredHolder<T,? extends T>> getEntries(){return null;}
 public static class Blocks extends DeferredRegister<net.minecraft.world.level.block.Block> {
  public <B extends net.minecraft.world.level.block.Block> DeferredBlock<B> registerBlock(String n,Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties,? extends B> f,net.minecraft.world.level.block.state.BlockBehaviour.Properties p){return null;} }
 public static class Items extends DeferredRegister<net.minecraft.world.item.Item> {
  public DeferredItem<net.minecraft.world.item.BlockItem> registerSimpleBlockItem(net.minecraft.core.Holder<net.minecraft.world.level.block.Block> b){return null;}
  public <I extends net.minecraft.world.item.Item> DeferredItem<I> registerItem(String n,Function<net.minecraft.world.item.Item.Properties,? extends I> f,net.minecraft.world.item.Item.Properties p){return null;}
  public DeferredItem<net.minecraft.world.item.Item> registerSimpleItem(String n,net.minecraft.world.item.Item.Properties p){return null;} } }
