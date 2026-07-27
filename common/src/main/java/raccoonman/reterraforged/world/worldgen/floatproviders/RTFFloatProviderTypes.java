package raccoonman.reterraforged.world.worldgen.floatproviders;

import com.mojang.serialization.Codec;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.valueproviders.FloatProvider;
import raccoonman.reterraforged.platform.RegistryUtil;

public class RTFFloatProviderTypes {
	public static final MapCodec<LegacyCanyonYScale> LEGACY_CANYON_Y_SCALE = register("legacy_canyon_y_scale", LegacyCanyonYScale.CODEC);

	public static void bootstrap() {
	}

	private static <T extends FloatProvider> MapCodec<T> register(String name, MapCodec<T> codec) {
		RegistryUtil.register(BuiltInRegistries.FLOAT_PROVIDER_TYPE, name, codec);
		return codec;
	}
}
