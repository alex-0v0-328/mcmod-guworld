package net.alex.guzhenrenworld;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;

/**
 * What a datapack registry bootstrap may do: register its entries and look up the registries they name.
 *
 * <p>Every function package writes its datagen entries against this interface, and the {@code datagen} provider
 * wraps vanilla's bootstrap context in it ({@link #of}), so the bootstraps compile unchanged on each Minecraft
 * version the mod targets, whatever that version calls the context.
 *
 * @author Alex
 * @version 1.0.0
 * @since 1.0.0
 */

public interface DatapackBootstrap<T> {

    Holder.Reference<T> register(ResourceKey<T> key, T value);

    <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registry);

    static <T> DatapackBootstrap<T> of(BootstapContext<T> context) {
        return new DatapackBootstrap<>() {
            @Override
            public Holder.Reference<T> register(ResourceKey<T> key, T value) { return context.register(key, value); }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registry) {
                return context.lookup(registry);
            }
        };
    }
}
