package net.minecraft.network.codec;
import java.util.function.Function; import com.mojang.datafixers.util.Function4;
public interface StreamCodec<B,T> {
 static <B,C,T1,T2,T3,T4> StreamCodec<B,C> composite(StreamCodec<? super B,T1> c1,Function<C,T1> g1,StreamCodec<? super B,T2> c2,Function<C,T2> g2,StreamCodec<? super B,T3> c3,Function<C,T3> g3,StreamCodec<? super B,T4> c4,Function<C,T4> g4,Function4<T1,T2,T3,T4,C> f){return null;} }
