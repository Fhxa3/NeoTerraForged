package raccoonman.reterraforged.platform.neoforge;

import net.neoforged.fml.ModList;

public class ModLoaderUtilImpl {

	public static boolean isLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}
}
