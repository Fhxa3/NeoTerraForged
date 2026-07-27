package raccoonman.reterraforged.client.gui.screen.presetconfig;

import java.awt.Color;
import java.io.IOException;
import java.util.Optional;

import org.joml.Matrix3x2fStack;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import raccoonman.reterraforged.RTFCommon;
import raccoonman.reterraforged.client.data.RTFTranslationKeys;
import raccoonman.reterraforged.client.gui.screen.page.BisectedPage;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetListPage.PresetEntry;
import raccoonman.reterraforged.client.gui.widget.SquareWidget;
import raccoonman.reterraforged.client.gui.widget.ValueButton;
import raccoonman.reterraforged.concurrent.cache.CacheManager;
import raccoonman.reterraforged.config.PerformanceConfig;
import raccoonman.reterraforged.data.worldgen.preset.settings.Preset;
import raccoonman.reterraforged.data.worldgen.preset.settings.SpawnType;
import raccoonman.reterraforged.data.worldgen.preset.settings.WorldSettings;
import raccoonman.reterraforged.registries.RTFRegistries;
import raccoonman.reterraforged.world.worldgen.GeneratorContext;
import raccoonman.reterraforged.world.worldgen.cell.Cell;
import raccoonman.reterraforged.world.worldgen.cell.heightmap.Levels;
import raccoonman.reterraforged.world.worldgen.densityfunction.tile.Tile;
import raccoonman.reterraforged.world.worldgen.noise.NoiseUtil;
import raccoonman.reterraforged.world.worldgen.noise.module.Noise;
import raccoonman.reterraforged.world.worldgen.util.PosUtil;

public abstract class PresetEditorPage extends BisectedPage<PresetConfigScreen, AbstractWidget, AbstractWidget> {
	private CycleButton<RenderMode> renderMode;
	private ValueButton<Integer> seed;
	private Preview preview;
	protected PresetEntry preset;

	public PresetEditorPage(PresetConfigScreen screen, PresetEntry preset) {
		super(screen);

		this.preset = preset;
	}

	protected void regenerate() {
		this.preview.regenerate();
	}

	@Override
	public void init() {
		super.init();

		Preview oldPreview = this.preview;
		if(oldPreview != null) {
			try {
				oldPreview.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		this.renderMode = PresetWidgets.createCycle(ImmutableList.copyOf(RenderMode.values()), this.renderMode != null ? this.renderMode.getValue() : RenderMode.BIOME_TYPE, Optional.empty(), (button, value) -> {
			this.preview.recolor();
		}, RenderMode::name);
		this.seed = PresetWidgets.createRandomButton(RTFTranslationKeys.GUI_BUTTON_SEED, (int) this.screen.getSettings().options().seed(), (i) -> {
			this.screen.setSeed(i);
			this.regenerate();
		});

		this.preview = new Preview();
		if(oldPreview != null) {
			this.preview.copyViewFrom(oldPreview);
		}
		this.preview.regenerate();

		this.right.addWidget(this.renderMode);
		this.right.addWidget(this.seed);
		this.right.addWidget(this.preview);
	}

	@Override
	public void onClose() {
		super.onClose();

		try {
			this.preset.save();
			this.preview.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDone() {
		super.onDone();

		try {
			this.screen.applyPreset(this.preset);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public class Preview extends AbstractWidget implements SquareWidget {
	    private static final int FACTOR = 4;
	    public static final int SIZE = (1 << 4) << FACTOR;
	    private static final float[] LEGEND_SCALES = { 1, 0.9F, 0.75F, 0.6F };
	    private static final int CLICK_DRAG_TOLERANCE = 4;
	    private static final int MIN_ZOOM_VALUE = 1;
	    private static final int MAX_ZOOM_VALUE = 100;
	    private static final int DEFAULT_ZOOM_VALUE = 68;

	    private final Identifier textureId = Identifier.fromNamespaceAndPath(RTFCommon.MOD_ID, "preview_framebuffer");
	    private final DynamicTexture texture = new DynamicTexture(this.textureId::toString, new NativeImage(SIZE, SIZE, true));
	    private Tile tile;
	    private int centerX, centerZ;

	    private String hoveredCoords = "";
	    //TODO maybe make this a map or something instead?
	    private String[] legendValues = {"", "", ""};
	    private Component[] legendLabels = { Component.translatable(RTFTranslationKeys.GUI_LABEL_PREVIEW_AREA), Component.translatable(RTFTranslationKeys.GUI_LABEL_PREVIEW_TERRAIN), Component.translatable(RTFTranslationKeys.GUI_LABEL_PREVIEW_BIOME) };

	    private double offsetX, offsetZ;
	    private boolean panned;
	    private int zoomValue = DEFAULT_ZOOM_VALUE;
	    private double pressX, pressY;

	    private GeneratorContext generatorContext;
	    private Levels levels;
	    private boolean contextDirty = true;
	    private boolean generating;
	    private boolean queued;
	    private boolean closed;

	    public Preview() {
	        super(-1, -1, -1, -1, CommonComponents.EMPTY);
	        Minecraft.getInstance().getTextureManager().register(this.textureId, this.texture);
	    }

	    private void copyViewFrom(Preview old) {
	    	this.offsetX = old.offsetX;
	    	this.offsetZ = old.offsetZ;
	    	this.panned = old.panned;
	    	this.zoomValue = old.zoomValue;
	    }

	    /**
	     * Full refresh: the preset, seed, or another generation input changed, so the
	     * generator context must be rebuilt before the next tile is generated.
	     */
	    public void regenerate() {
	    	this.contextDirty = true;
	    	this.requestTile();
	    }

	    /**
	     * Re-render the current tile with the active {@link RenderMode}; no generation needed.
	     */
	    public void recolor() {
	    	this.renderTile();
	    }

	    /*
	     * Tile generation is asynchronous and coalesced: at most one generation runs at a
	     * time, and requests made while one is in flight collapse into a single follow-up
	     * using the latest pan/zoom state. All fields are touched only on the render thread.
	     */
	    private void requestTile() {
	    	if(this.closed) {
	    		return;
	    	}
	    	if(this.generating) {
	    		this.queued = true;
	    		return;
	    	}
	    	this.generating = true;
	    	try {
		    	if(this.contextDirty) {
		    		this.rebuildContext();
		    		this.contextDirty = false;
		    	}
		    	int cx = NoiseUtil.floor((float) this.offsetX);
		    	int cz = NoiseUtil.floor((float) this.offsetZ);
		        this.generatorContext.generator.generateZoomed(cx, cz, this.getZoom(), false).whenComplete((tile, error) -> {
		        	Minecraft.getInstance().execute(() -> this.acceptTile(cx, cz, tile, error));
		        });
	    	} catch (Exception e) {
	    		this.generating = false;
	    		RTFCommon.LOGGER.error("Failed to generate preview tile", e);
	    	}
	    }

	    private void rebuildContext() {
			WorldCreationContext settings = PresetEditorPage.this.screen.getSettings();
	        RegistryAccess.Frozen registries = settings.worldgenLoadContext();
	        HolderLookup.Provider provider = PresetEditorPage.this.preset.getPreset().buildPatch(registries);
	        HolderGetter<Preset> presets = provider.lookupOrThrow(RTFRegistries.PRESET);
	        HolderGetter<Noise> noises = provider.lookupOrThrow(RTFRegistries.NOISE);
	        Preset preset = presets.getOrThrow(Preset.KEY).value();
	        WorldSettings.Properties properties = preset.world().properties;
	        this.levels = new Levels(properties.terrainScaler(), properties.seaLevel);

	        try {
				CacheManager.clear();
			} catch (Exception e) {
				e.printStackTrace();
			}
			PerformanceConfig config = PerformanceConfig.read(PerformanceConfig.DEFAULT_FILE_PATH)
				.resultOrPartial(RTFCommon.LOGGER::error)
				.orElseGet(PerformanceConfig::makeDefault);
	        this.generatorContext = GeneratorContext.makeUncached(preset, noises, (int) settings.options().seed(), FACTOR, 0, config.batchCount());

	        if(!this.panned && properties.spawnType == SpawnType.CONTINENT_CENTER) {
	        	long center = this.generatorContext.lookup.getHeightmap().continent().getNearestCenter((int) this.offsetX, (int) this.offsetZ);
	        	this.offsetX = PosUtil.unpackLeft(center);
	        	this.offsetZ = PosUtil.unpackRight(center);
	        }
	    }

	    private void acceptTile(int cx, int cz, Tile tile, Throwable error) {
	    	this.generating = false;
	    	if(error != null) {
	    		RTFCommon.LOGGER.error("Failed to generate preview tile", error);
	    	} else if(!this.closed && tile != null) {
	    		this.tile = tile;
	    		this.centerX = cx;
	    		this.centerZ = cz;
	    		this.renderTile();
	    	}
	    	if(this.queued && !this.closed) {
	    		this.queued = false;
	    		this.requestTile();
	    	}
	    }

	    private void renderTile() {
	    	if(this.tile == null || this.closed) {
	    		return;
	    	}
	        RenderMode renderMode = PresetEditorPage.this.renderMode.getValue();

	        int stroke = 2;
	        int width = this.tile.getBlockSize().size();

	        NativeImage pixels = this.texture.getPixels();
	        this.tile.iterate((cell, x, z) -> {
	            if (x < stroke || z < stroke || x >= width - stroke || z >= width - stroke) {
	                pixels.setPixelABGR(x, z, Color.BLACK.getRGB());
	            } else {
	                pixels.setPixelABGR(x, z, renderMode.getColor(cell, this.levels));
	            }
	        });
	        this.texture.upload();
	    }

	    public void close() throws Exception {
	    	this.closed = true;
	    	this.texture.close();
	    	try {
				CacheManager.clear();
			} catch (Exception e) {
				e.printStackTrace();
			}
	    }

	    @Override
	    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mx, int my, float partialTicks) {
	    	int x = this.getX();
	    	int y = this.getY();

	    	this.height = this.getWidth();
	    	graphics.blit(RenderPipelines.GUI_TEXTURED, this.textureId, x, y, 0.0F, 0.0F, this.width, this.height, this.width, this.height);

	    	this.updateLegend(mx, my);

	    	this.renderLegend(graphics, mx, my, this.legendLabels, this.legendValues, x, y + this.width, 10, 0xFFFFFFFF);
	    }

	    @Override
	    protected void updateWidgetNarration(NarrationElementOutput output) {
	    }

	    @Override
	    public void onClick(MouseButtonEvent event, boolean doubleClick) {
	    	this.pressX = event.x();
	    	this.pressY = event.y();
	    }

	    @Override
	    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
	    	int tileSize = this.tile != null ? this.tile.getBlockSize().size() : SIZE;
	    	double blocksPerPixel = (double) tileSize * this.getZoom() / Math.max(1, this.getWidth());
	    	this.offsetX -= dx * blocksPerPixel;
	    	this.offsetZ -= dy * blocksPerPixel;
	    	this.panned = true;
	    	this.requestTile();
	    }

	    @Override
	    public void onRelease(MouseButtonEvent event) {
	    	double dx = event.x() - this.pressX;
	    	double dy = event.y() - this.pressY;
	    	if(dx * dx + dy * dy <= CLICK_DRAG_TOLERANCE * CLICK_DRAG_TOLERANCE
	    			&& this.updateLegend((int) event.x(), (int) event.y()) && !this.hoveredCoords.isEmpty()) {
	    		PresetEditorPage.this.screen.minecraft.keyboardHandler.setClipboard(this.hoveredCoords);
	    	}
	    }

	    @Override
	    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
	    	if(!this.isMouseOver(mouseX, mouseY)) {
	    		return false;
	    	}
	    	// zoom faster the further out we are
	    	int step = Math.max(1, (MAX_ZOOM_VALUE - this.zoomValue) / 10);
	    	int zoom = Math.max(MIN_ZOOM_VALUE, Math.min(MAX_ZOOM_VALUE, this.zoomValue + (int) Math.signum(scrollY) * step));
	    	if(zoom != this.zoomValue) {
	    		this.zoomValue = zoom;
	    		this.requestTile();
	    	}
	    	return true;
	    }

	    private boolean updateLegend(int mx, int my) {
	        if (this.tile != null) {
	            int left = this.getX();
	            int top = this.getY();
	            float size = this.width;

	            int zoom = this.getZoom();
	            int width = Math.max(1, this.tile.getBlockSize().size() * zoom);
	            int height = Math.max(1, this.tile.getBlockSize().size() * zoom);
	            this.legendValues[0] = width + "x" + height;
	            if (mx >= left && mx <= left + size && my >= top && my <= top + size) {
	                float fx = (mx - left) / size;
	                float fz = (my - top) / size;
	                int ix = NoiseUtil.round(fx * this.tile.getBlockSize().size());
	                int iz = NoiseUtil.round(fz * this.tile.getBlockSize().size());
	                Cell cell = this.tile.lookup(ix, iz);
	                this.legendValues[1] = getTerrainName(cell);
	                this.legendValues[2] = getBiomeName(cell);

	                int dx = (ix - (this.tile.getBlockSize().size() / 2)) * zoom;
	                int dz = (iz - (this.tile.getBlockSize().size() / 2)) * zoom;

	                this.hoveredCoords = (this.centerX + dx) + ":" + (this.centerZ + dz);
	                return true;
	            } else {
	            	this.hoveredCoords = "";
	            }
	        }
	        return false;
	    }

	    private float getLegendScale() {
	        int index = PresetEditorPage.this.screen.minecraft.options.guiScale().get() - 1;
	        if (index < 0 || index >= LEGEND_SCALES.length) {
	            // index=-1 == GuiScale(AUTO) which is the same as GuiScale(4)
	            // values above 4 don't exist but who knows what mods might try set it to
	            // in both cases use the smallest acceptable scale
	            index = LEGEND_SCALES.length - 1;
	        }
	        return LEGEND_SCALES[index];
	    }

	    private void renderLegend(GuiGraphicsExtractor guiGraphics, int mx, int my, Component[] labels, String[] values, int left, int top, int lineHeight, int color) {
	        float scale = this.getLegendScale();
	        Matrix3x2fStack pose = guiGraphics.pose();

	        pose.pushMatrix();
	        pose.translate(left + 3.75F * scale, top - lineHeight * (3.2F * scale));
	        pose.scale(scale, scale);

	        Minecraft mc = Minecraft.getInstance();
	        Font renderer = mc.font;
	        int spacing = 0;
	        for (Component s : labels) {
	            spacing = Math.max(spacing, renderer.width(s));
	        }

	        float maxWidth = (this.width - 4) / scale;
	        for (int i = 0; i < labels.length && i < values.length; i++) {
	        	Component label = labels[i];
	            String value = values[i];

	            while (value.length() > 0 && spacing + renderer.width(value) > maxWidth) {
	                value = value.substring(0, value.length() - 1);
	            }

	            guiGraphics.text(renderer, label, 0, i * lineHeight, color);
	            guiGraphics.text(renderer, value, spacing, i * lineHeight, color);
	        }

	        pose.popMatrix();

	        if (!this.hoveredCoords.isEmpty()) {
	        	guiGraphics.centeredText(renderer, this.hoveredCoords, mx, my - 10, 0xFFFFFFFF);
	        }
	    }

	    private int getZoom() {
	        return NoiseUtil.round(1.5F * (101 - (float) this.zoomValue));
	    }

	    private static String getTerrainName(Cell cell) {
	        if (cell.terrain.isRiver()) {
	            return "river";
	        }
	        return cell.terrain.getName().toLowerCase();
	    }

	    private static String getBiomeName(Cell cell) {
	        String terrain = cell.terrain.getName().toLowerCase();
	        if (terrain.contains("ocean")) {
	            if (cell.temperature < 0.3F) {
	                return "cold_" + terrain;
	            }
	            if (cell.temperature > 0.6F) {
	                return "warm_" + terrain;
	            }
	            return terrain;
	        }
	        if (terrain.contains("river")) {
	            return "river";
	        }
	        return cell.biome.name().toLowerCase();
	    }
	}
}
