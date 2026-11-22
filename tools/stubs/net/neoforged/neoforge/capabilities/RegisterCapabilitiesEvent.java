package net.neoforged.neoforge.capabilities;
import net.minecraft.world.level.block.entity.*;
public class RegisterCapabilitiesEvent { public interface Provider<O,C,T> { T getCapability(O o,C c); }
 public <T,C,BE extends BlockEntity> void registerBlockEntity(BlockCapability<T,C> cap,BlockEntityType<BE> t,Provider<? super BE,C,T> p){} }
