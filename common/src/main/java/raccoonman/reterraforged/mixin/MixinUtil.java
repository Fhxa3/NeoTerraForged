package raccoonman.reterraforged.mixin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.util.Util;
import raccoonman.reterraforged.concurrent.ThreadPools;
import raccoonman.reterraforged.concurrent.cache.Cache;

@Mixin(Util.class)
public class MixinUtil {

	@Inject(method = "shutdownExecutors()V", at = @At("TAIL"))
	private static void shutdownExecutors(CallbackInfo callback) {
		shutdownExecutor(ThreadPools.WORLD_GEN);
		shutdownExecutor(Cache.SCHEDULER);
	}

    private static void shutdownExecutor(ExecutorService executorService) {
    	executorService.shutdown();
    	try {
    		executorService.awaitTermination(3L, TimeUnit.SECONDS);
    	} catch (InterruptedException e) {
    		Thread.currentThread().interrupt();
    	}
    }
}
