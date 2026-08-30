package raccoonman.reterraforged.platform.neoforge;

import net.neoforged.fml.ModList;

public class ModLoaderUtilImpl {

	public static boolean isLoaded(String modId) {
		ModList modList = ModList.get();
		return modList != null && modList.isLoaded(modId);
	}
}
