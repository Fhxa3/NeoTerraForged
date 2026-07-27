package raccoonman.reterraforged.neoforge;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFLanguageProvider;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.platform.neoforge.RegistryUtilImpl;
import raccoonman.reterraforged.server.RTFMinecraftServer;

@Mod("reterraforged")
public class RTFNeoForge {

    public RTFNeoForge(IEventBus modEventBus, ModContainer container) {
    	RTFCommon.bootstrap();

    	if (FMLEnvironment.getDist() == Dist.CLIENT) {
    		modEventBus.addListener(RTFNeoForgeClient::registerPresetEditors);
    	}
    	modEventBus.addListener(RTFNeoForge::gatherData);
    	RegistryUtilImpl.register(modEventBus);

    	NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent event) -> {
    		event.addListener(RTFCommon.location("feature_template_manager"), new SimplePreparableReloadListener<Void>() {
    			@Override
    			protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
    				return null;
    			}

    			@Override
    			protected void apply(Void preparations, ResourceManager resourceManager, ProfilerFiller profiler) {
    				MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
    				if (server != null) {
    					((RTFMinecraftServer) server).getFeatureTemplateManager().onReload(resourceManager);
    				}
    			}
    		});
    	});
    }

    private static void gatherData(GatherDataEvent.Client event) {
    	boolean includeClient = true; //event.includeClient();
    	DataGenerator generator = event.getGenerator();
    	PackOutput output = generator.getPackOutput();

    	generator.addProvider(includeClient, new RTFLanguageProvider.EnglishUS(output));
    	generator.addProvider(includeClient, PackMetadataGenerator.forFeaturePack(output, Component.translatable(RTFTranslationKeys.METADATA_DESCRIPTION)));
    }
}