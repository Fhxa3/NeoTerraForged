package raccoonman.reterraforged.world.worldgen.cell.biome.spawn;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.biome.Climate.TargetPoint;
import raccoonman.reterraforged.world.worldgen.biome.RTFClimateSampler;
	
@Deprecated(forRemoval = true)
public class SpawnFinderFix {
	public Result result;

	public SpawnFinderFix(List<ParameterPoint> list, Sampler sampler) {
		if ((Object) sampler instanceof RTFClimateSampler rtfClimateSampler) {
			BlockPos center = rtfClimateSampler.getSpawnSearchCenter();

			this.result = SpawnFinderFix.getSpawnPositionAndFitness(list, sampler, center.getX(), center.getZ(), center);
			this.radialSearch(list, sampler, 2048.0f, 512.0f, center);
			this.radialSearch(list, sampler, 512.0f, 32.0f, center);
		}
	}

	private void radialSearch(List<ParameterPoint> list, Sampler sampler, float f, float g, BlockPos center) {
		float h = 0.0f;
		float i = g;
		BlockPos blockPos = this.result.location();
		while (i <= f) {
			int j = blockPos.getX() + (int) (Math.sin(h) * (double) i);
			Result result = SpawnFinderFix.getSpawnPositionAndFitness(list, sampler, j,
					blockPos.getZ() + (int) (Math.cos(h) * (double) i), center);
			if (result.fitness() < this.result.fitness()) {
				this.result = result;
			}
			if (!((double) (h += g / i) > Math.PI * 2))
				continue;
			h = 0.0f;
			i += g;
		}
	}

	private static Result getSpawnPositionAndFitness(List<ParameterPoint> list, Sampler sampler, int i, int j, BlockPos center) {
		TargetPoint targetPoint = sampler.sample(QuartPos.fromBlock(i), 0, QuartPos.fromBlock(j));
		TargetPoint targetPoint2 = new TargetPoint(targetPoint.temperature(), targetPoint.humidity(),
				targetPoint.continentalness(), targetPoint.erosion(), 0L, targetPoint.weirdness());
		long m = Long.MAX_VALUE;
		for (ParameterPoint parameterPoint : list) {
			m = Math.min(m, parameterPoint.fitness(targetPoint2));
		}
		long l = Mth.square((long) (i - center.getX())) + Mth.square((long) (j - center.getZ()));
		return new Result(new BlockPos(i, 0, j), m * Mth.square(2048L) + l);
	}

	public record Result(BlockPos location, long fitness) {
	}
}
