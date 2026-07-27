package raccoonman.reterraforged.client.gui.widget;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;

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
    public Optional<GuiEventListener> getChildAt(double x, double y) {
        // square widgets overflow their fixed-height entry rect, so entry hit-testing
        // misses most of their area; hit-test the widget rect as well
        for (Entry<T> entry : this.children()) {
            if (entry.getWidget() instanceof SquareWidget && entry.getWidget().isMouseOver(x, y)) {
                return Optional.of(entry);
            }
        }
        return super.getChildAt(x, y);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // only square (map) widgets get first refusal on scroll; plain widgets must not
        // see it (CycleButton would cycle its value), and the list scrolls otherwise
        if (this.getChildAt(mouseX, mouseY)
                .filter(child -> child instanceof Entry<?> entry && entry.getWidget() instanceof SquareWidget)
                .filter(child -> child.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
                .isPresent()) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
            int optionWidth = Math.min(450, width);
            int padding = (width - optionWidth) / 2;
            widget.setX(this.getX() + padding);
            widget.setY(this.getY());
            widget.visible = true;
            widget.setWidth(optionWidth);
            widget.setHeight(this.getHeight() - 1);
            if(widget instanceof SquareWidget) {
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
