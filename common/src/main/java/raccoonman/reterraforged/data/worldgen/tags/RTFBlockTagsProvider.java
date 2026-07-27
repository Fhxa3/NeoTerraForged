package raccoonman.reterraforged.data.worldgen.tags;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.references.BlockItemIds;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import raccoonman.reterraforged.data.worldgen.preset.settings.MiscellaneousSettings;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.tags.RTFBlockTags;

public class RTFBlockTagsProvider extends TagsProvider<Block> {
	private Preset preset;

	public RTFBlockTagsProvider(Preset preset, PackOutput packOutput, CompletableFuture<Provider> completableFuture) {
		super(packOutput, Registries.BLOCK, completableFuture);

		this.preset = preset;
	}

	@Override
	protected void addTags(HolderLookup.Provider provider) {
//		MiscellaneousSettings miscellaneousSettings = this.preset.miscellaneous();

		this.tag(RTFBlockTags.SOIL).add(BlockItemIds.DIRT.block(), BlockItemIds.COARSE_DIRT.block());
		this.tag(RTFBlockTags.CLAY).add(BlockItemIds.CLAY.block());
		this.tag(RTFBlockTags.SEDIMENT).add(BlockItemIds.SAND.block(), BlockItemIds.GRAVEL.block());
		this.tag(RTFBlockTags.ERODIBLE).add(BlockItemIds.SNOW_BLOCK.block()).add(BlockItemIds.POWDER_SNOW.block()).add(BlockItemIds.GRAVEL.block()).addOptionalTag(BlockTags.DIRT);

//		if(!miscellaneousSettings.oreCompatibleStoneOnly) {
			this.tag(RTFBlockTags.ROCK).add(BlockItemIds.GRANITE.block(), BlockItemIds.ANDESITE.block(), BlockItemIds.STONE.block(), BlockItemIds.DIORITE.block());
//		} else{
			//TODO
//		}
	}
}
