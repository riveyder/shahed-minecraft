package io.github.riveyder.shahed.compat;
import java.util.function.*;
public record StreamCodec<B,T>(BiConsumer<T,B> writer,Function<B,T> reader){
    public static <B,T> StreamCodec<B,T> ofMember(BiConsumer<T,B> w,Function<B,T> r){return new StreamCodec<>(w,r);}
    public T decode(B b){return reader.apply(b);}public void encode(B b,T v){writer.accept(v,b);}
}
