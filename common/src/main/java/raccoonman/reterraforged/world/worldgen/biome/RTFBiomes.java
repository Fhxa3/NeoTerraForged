package raccoonman.reterraforged.world.worldgen.biome;

import org.jetbrains.annotations.Nullable;

import net.minecraft.sounds.Music;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;

public class RTFBiomes {
    @Nullable
    private static final Music NORMAL_MUSIC = null;

    public static Biome coldMarshland() {
    	//TODO add shrubbery and stuff
    	return null;
    }
    
    private static Biome biome(boolean hasPrecipitation, float skyColor, float downfall, MobSpawnSettings.Builder mobSpawnSettings, BiomeGenerationSettings.Builder generationSettings, @Nullable Music music) {
        return biome(hasPrecipitation, skyColor, downfall, 4159204, 329011, null, null, mobSpawnSettings, generationSettings, music);
    }

    private static Biome biome(boolean hasPrecipitation, float skyColor, float downfall, int waterColor, int waterFogColor, @Nullable Integer grassColorOverride, @Nullable Integer foliageColorOverride, MobSpawnSettings.Builder mobSpawnSettings, BiomeGenerationSettings.Builder generationSettings, @Nullable Music music) {
        BiomeSpecialEffects.Builder specialEffects = new BiomeSpecialEffects.Builder()
        	.waterColor(waterColor);
        if (grassColorOverride != null) {
            specialEffects.grassColorOverride(grassColorOverride);
        }
        if (foliageColorOverride != null) {
            specialEffects.foliageColorOverride(foliageColorOverride);
        }
        Biome.BiomeBuilder builder = new Biome.BiomeBuilder().hasPrecipitation(hasPrecipitation).temperature(skyColor).downfall(downfall).specialEffects(specialEffects.build()).mobSpawnSettings(mobSpawnSettings.build()).generationSettings(generationSettings.build())
        	.setAttribute(EnvironmentAttributes.SKY_COLOR, calculateSkyColor(skyColor))
        	.setAttribute(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS);
        if (waterFogColor != 329011) {
            builder.setAttribute(EnvironmentAttributes.WATER_FOG_COLOR, ARGB.opaque(waterFogColor));
        }
        if (music != null) {
            builder.setAttribute(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(music));
        }
        return builder.build();
    }

    private static int calculateSkyColor(float f) {
        float g = f;
        g /= 3.0f;
        g = Mth.clamp(g, -1.0f, 1.0f);
        return ARGB.opaque(Mth.hsvToRgb(0.62222224f - g * 0.05f, 0.5f + g * 0.1f, 1.0f));
    }
}
