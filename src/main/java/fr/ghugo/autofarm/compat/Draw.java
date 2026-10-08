package fr.ghugo.autofarm.compat;

/** Dessin de texte et de rectangles, indépendant de la version de Minecraft. */
public interface Draw {
	void text(String text, int x, int y, int color);

	void centered(String text, int x, int y, int color);

	void fill(int x1, int y1, int x2, int y2, int color);

	int width(String text);
}
