package raccoonman.reterraforged.world.worldgen.surface.rule;

import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.SurfaceRules;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.surface.rule.StrataRule.Strata;

public class RTFSurfaceRules {

	public static void bootstrap() {
		register("strata", StrataRule.CODEC);
	}
	
	public static StrataRule strata(Identifier name, Holder<Noise> selector, List<Strata> strata, int iterations) {
		return new StrataRule(name, selector, strata, iterations);
	}
	
	public static void register(String name, MapCodec<? extends SurfaceRules.RuleSource> value) {
		RegistryUtil.register(BuiltInRegistries.MATERIAL_RULE, name, value); //TODO: Convert to MapCodec
	}
}
