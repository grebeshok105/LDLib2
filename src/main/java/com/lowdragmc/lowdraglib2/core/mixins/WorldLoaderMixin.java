package com.lowdragmc.lowdraglib2.core.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import com.lowdragmc.lowdraglib2.Platform;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(WorldLoader.class)
public abstract class WorldLoaderMixin {
    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/RegistryDataLoader;load(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/core/RegistryAccess;Ljava/util/List;)Lnet/minecraft/core/RegistryAccess$Frozen;",
            shift = At.Shift.BEFORE)
    )
    private static <D, R> void ldlib2$loadResourceManager(
            WorldLoader.InitConfig initConfig,
            WorldLoader.WorldDataSupplier<D> worldDataSupplier,
            WorldLoader.ResultFactory<D, R> resultFactory,
            Executor backgroundExecutor,
            Executor gameExecutor,
            CallbackInfoReturnable<CompletableFuture<R>> cir,
            @Local CloseableResourceManager resourceManager
    ) {
        Platform.RESOURCE_MANAGER = resourceManager;
    }

    // Upstream injects into the synthetic lambda that closes the resource
    // manager (whenComplete stage of the returned future). Lambda names are
    // not stable across yarn/intermediary mappings, so instead we hook the
    // completion of the returned CompletableFuture with the same semantics:
    // clear the captured manager once the load future settles, success or not.
    @Inject(method = "load", at = @At("RETURN"))
    private static <D, R> void ldlib2$clearResourceManagerOnComplete(
            WorldLoader.InitConfig initConfig,
            WorldLoader.WorldDataSupplier<D> worldDataSupplier,
            WorldLoader.ResultFactory<D, R> resultFactory,
            Executor backgroundExecutor,
            Executor gameExecutor,
            CallbackInfoReturnable<CompletableFuture<R>> cir) {
        CompletableFuture<R> future = cir.getReturnValue();
        if (future != null) {
            future.whenComplete((result, throwable) -> Platform.RESOURCE_MANAGER = null);
        }
    }
}
