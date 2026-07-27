package raccoonman.reterraforged.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator.Pack;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFLanguageProvider;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.platform.RegistryUtil;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.server.RTFMinecraftServer;
import raccoonman.reterraforged.world.worldgen.biome.modifier.BiomeModifier;

public class RTFFabric implements ModInitializer, DataGeneratorEntrypoint {

	@Override
	public void onInitialize() {
		RTFCommon.bootstrap();

		RegistryUtil.createDataRegistry(RTFRegistries.BIOME_MODIFIER, BiomeModifier.CODEC, false);

		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			if(success) {
				((RTFMinecraftServer) server).getFeatureTemplateManager().onReload(resourceManager);
			}
		});
	}

	//TODO merge this with forge's datagen since they're the same now
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
		Pack pack = fabricDataGenerator.createPack();

		pack.addProvider((FabricPackOutput output) -> new RTFLanguageProvider.EnglishUS(output));
		pack.addProvider((FabricPackOutput output) -> PackMetadataGenerator.forFeaturePack(output, Component.translatable(RTFTranslationKeys.METADATA_DESCRIPTION)));
	}
}