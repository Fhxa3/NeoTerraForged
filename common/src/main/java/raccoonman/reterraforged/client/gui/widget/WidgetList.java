package raccoonman.reterraforged.client.gui.widget;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import raccoonman.reterraforged.client.gui.screen.presetconfig.PresetEditorPage;

public class WidgetList<T extends AbstractWidget> extends ContainerObjectSelectionList<WidgetList.Entry<T>> {
	private boolean renderSelected;
	
    public WidgetList(Minecraft minecraft, int i, int j, int k, int l) {
        super(minecraft, i, j, k, l);
    }

    public void select(T widget) {
    	for(Entry<T> entry : this.children()) {
    		if(entry.widget.equals(widget)) {
    			this.setSelected(entry);
    			return;
    		}
    	}
    }
    
    public <W extends T> W addWidget(W widget) {
        super.addEntry(new Entry<>(widget));
        return widget;
    }

    public void setRenderSelected(boolean renderSelected) {
    	this.renderSelected = renderSelected;
    }

    @Override
    protected boolean entriesCanBeSelected() {
        return this.renderSelected;
    }

    @Override
    public int getRowWidth() {
        return this.width - 20;
    }

    @Override
    protected int scrollBarX() {
        return this.getRowRight();
    }

    public static class Entry<T extends AbstractWidget> extends ContainerObjectSelectionList.Entry<Entry<T>> {
        private T widget;

        public Entry(T widget) {
            this.widget = widget;
        }

        public T getWidget() {
        	return this.widget;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.singletonList(this.widget);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            int width = this.getWidth();
            int optionWidth = Math.min(396, width);
            int padding = (width - optionWidth) / 2;
            widget.setX(this.getX() + padding);
            widget.setY(this.getY());
            widget.visible = true;
            widget.setWidth(optionWidth);
            widget.setHeight(this.getHeight() - 1);
            if(widget instanceof PresetEditorPage.Preview preview) {
            	widget.setHeight(widget.getWidth());
            }
            widget.extractRenderState(graphics, mouseX, mouseY, a);
        }

		@Override
		public List<T> narratables() {
			return Collections.singletonList(this.widget);
		}
    }
}
