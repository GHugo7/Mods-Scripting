package fr.ghugo.autofarm.compat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Écran de base : le dessin des textes passe par {@link #drawContent} (Minecraft 26.2). */
public abstract class BaseScreen extends Screen {
	protected BaseScreen(Component title) {
		super(title);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		drawContent(Compat.draw(graphics, this.font), mouseX, mouseY);
	}

	/** Dessine les textes de l'écran, par-dessus les boutons. */
	protected abstract void drawContent(Draw draw, int mouseX, int mouseY);
}
