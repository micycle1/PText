package vectorText;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;

import static processing.core.PApplet.abs;

import java.util.ArrayList;

/**
 * Vectorised text. A middleman PFonts and PShapes.
 * 
 * TODO max width, if letter.x > width, new line (offset y too)
 * 
 * @author micycle1
 *
 */
public class VectorText extends PShape {

	float scale = 1;
	
	float scaleX, scaleY = 1;

	/** dd */
	float width, height;

	private PApplet p;

	private PFont font;

	private int fontSize; // declare separate in case font is changed?

	private String text;

	private ArrayList<Float> charDescent = new ArrayList<Float>();

	private float descent = 0;

	/**
	 * Using a specific font
	 * 
	 * @param p
	 * @param font
	 */
	public VectorText(PApplet p, PFont font) {
		super(p.getGraphics(), GROUP);
		this.font = font;
		this.p = p;
	}

	/**
	 * Uses current sketch font.
	 * 
	 * @param p
	 */
	public VectorText(PApplet p) {
		this(p, p.getGraphics().textFont);
		if (font == null) {
			font = p.createFont("Arial", 64, true);
		}
		this.p = p;
	}

	/**
	 * Sets X and Y scale, regardless of existing scale.
	 * 
	 * @param scale
	 * @see #scale(float), scales by multiplying existing scale
	 */
	public void setScale(float scale) {
		scale(scale / this.scale);
		this.scale = scale;
	}

	/**
	 * Scales scale by current scale.
	 * 
	 * @see #setScale(float)
	 */
	@Override
	public void scale(float s) {
		super.scale(s);
		scale *= s;
		width *= s;
		height *= s;
	}
	
	@Override
	public void scale(float x, float y) {
		width*=x;
		super.scale(x, y);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		setText(text.toCharArray());
	}

	int characterSpacing = 0;

	public void setText(char[] text) {

		width = 0;
		height = Float.NEGATIVE_INFINITY;

		while (getChildCount() > 0) { // remove any exising letters
			removeChild(0);
		}
		charDescent.clear();

		setScale(1); // reset scaling

		this.text = new String(text);

		float translationX = -charWhiteSpace(text[0]) / 2; // running total of character x-axis translation

		for (Character c : text) {
			PShape character = font.getShape(c);

			/**
			 * Offset using vertex X position, not translate(), since translation value is
			 * not accessible.
			 */
			for (int i = 0; i < character.getVertexCount(); i++) {
				float newX = character.getVertexX(i) + translationX;
				character.setVertex(i, newX, character.getVertexY(i));
			}

//			character.translate(translationX, 0);
			character.disableStyle(); // parent should override style

			addChild(character);

			translationX += charWidth(c); // width() returns width with size 1, so scale up
			translationX -= characterSpacing;

//			height = PApplet.max(height, charHeight(c));
			charDescent.add(getMaxY(character)); // store this character's descent value
		}

//		width = getWidth(this); // width from charWidth()
		width = translationX - charWhiteSpace(text[text.length - 1]) / 2f;
//		width = getMaxX(this) - getMinX(this);
//		parent.height = (font.ascent() + font.descent())*font.getSize();
		height = getHeight(this);
//		height = Float.MIN_VALUE;
//		width = 0;
//		for (Character c : text) {
////			height = p.max(height, charHeight(c));
//			width += font.getGlyph(c).width * scale;
//			width += charWhiteSpace(c);
//		}
		descent = getMaxY(this);
	}

	public void debug(int mouseX, int mouseY) {
		float translationX = 0;

		p.pushStyle(); // save style

		p.stroke(0);
		p.strokeWeight(3);
		/**
		 * Draw string bounding box
		 */
		p.rect(mouseX, mouseY + descent*scaleX, width, -height);

		p.strokeWeight(2);
//		p.line(mouseX, mouseY, mouseX + width, mouseY); // baseline

		p.stroke(0, 255, 0);

		/**
		 * Draw per-character bounding boxes
		 */
		for (int i = 0; i < text.toCharArray().length; i++) {
			Character c = text.toCharArray()[i];
			p.rect(translationX + mouseX - charWhiteSpace(text.charAt(0)) / 2, mouseY + charDescent.get(i), charWidth(c), -charHeight(c));
			translationX += charWidth(c);
		}

		p.fill(0, 255, 0); // modify style
		p.noStroke(); // modify style
		p.ellipseMode(CENTER);
		for (int j = 0; j < getChildren().length; j++) {
			PShape child = getChildren()[j];
			for (int i = 0; i < child.getVertexCount(); i++) {
				p.ellipse(child.getVertex(i).x * scale + mouseX, child.getVertex(i).y * scale + mouseY, 4, 4);
			}
		}

		p.popStyle(); // return saved style

	}

	/**
	 * Above the line (based on letter 'd')
	 * 
	 * @return
	 */
	public float ascent() {
		return font.ascent() * font.getSize() * scale;
	}

	/**
	 * Below the line (based on letter 'p')
	 * 
	 * @return
	 */
	public float descent() {
		return font.descent() * font.getSize() * scale;
	}

	public float charWidth(Character c) {
		return font.width(c) * font.getSize() * scale;
	}

	public float charHeight(Character c) {
		return font.getGlyph(c).height * scale;
	}

	/**
	 * Get horizontal character whitespace (difference between font char and glyph)
	 * 
	 * @param c
	 * @return
	 */
	public float charWhiteSpace(Character c) {
		return charWidth(c) - font.getGlyph(c).width * scale;
	}

	/**
	 * Rasterise the text and return as a PImage.
	 * 
	 * @return
	 */
	public PImage get() {
		PGraphics temp = p.createGraphics((int) width, (int) (height + descent()));
		temp.smooth();
		temp.beginDraw();
		temp.shape(this, 0, height);
		temp.endDraw();

		return temp.get();
	}

	/**
	 * 
	 * When masked with a Pimage, keep only the text.
	 * 
	 * @return
	 */
	public PImage getAlphaMask(int width, int height) {
		int fillColorCache = fillColor;

		this.setFill(255); // set white (mask preserve)

		PGraphics temp = p.createGraphics((int) width, (int) height);
		temp.smooth();
		temp.beginDraw();
		temp.shape(this, 0, p.height - height);
		temp.endDraw();

		this.setFill(fillColorCache); // restore actual fill colour

		return temp.get();
	}

	/**
	 * When masked with a Pimage, remvoe the text
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	public PImage getAlphaMaskInverse(int width, int height) {
		int fillColorCache = fillColor;

		this.setFill(0); // set white (mask preserve)

		PGraphics temp = p.createGraphics((int) width, (int) height);
		temp.smooth();
		temp.beginDraw();
		temp.background(255);
		temp.shape(this, 0, p.height - height);
		temp.endDraw();

		this.setFill(fillColorCache); // restore actual fill colour

		return temp.get();
	}

	private static float getMinY(PShape shape) {
		float min = Float.MAX_VALUE;
		if (shape.getFamily() == GROUP) {
			for (PShape child : shape.getChildren()) { // search all children PShapes
				for (int i = 0; i < child.getVertexCount(); i++) {
					min = PApplet.min(child.getVertex(i).y, min);
				}
			}
		} else {
			for (int i = 0; i < shape.getVertexCount(); i++) { // search only parent PShape
				min = PApplet.min(shape.getVertex(i).y, min);
			}
		}
		return min;
	}

	/**
	 * @param shape PShape (vectorised text)
	 * @return y difference (height) between lowest and highest points
	 */
	public static float getHeight(PShape shape) {

		float min = Float.MAX_VALUE;
		float max = Float.NEGATIVE_INFINITY;

		if (shape.getFamily() == GROUP) {
			for (PShape child : shape.getChildren()) { // search all children PShapes
				for (int i = 0; i < child.getVertexCount(); i++) {
					min = PApplet.min(child.getVertex(i).y, min);
					max = PApplet.max(child.getVertex(i).y, max);
				}
			}
		} else {
			for (int i = 0; i < shape.getVertexCount(); i++) { // search only parent PShape
				min = PApplet.min(shape.getVertex(i).y, min);
				max = PApplet.max(shape.getVertex(i).y, max);
			}
		}
		return max - min;
	}

	/**
	 * Return value of greatest (down-most) Y coordinate from shape
	 * 
	 * @param shape
	 * @return
	 */
	private static float getMaxY(PShape shape) {
		float max = Float.NEGATIVE_INFINITY;
		if (shape.getFamily() == GROUP) {
			for (PShape child : shape.getChildren()) { // search all children PShapes
				for (int i = 0; i < child.getVertexCount(); i++) {
					max = PApplet.max(child.getVertex(i).y, max);
				}
			}
		} else {
			for (int i = 0; i < shape.getVertexCount(); i++) { // search only parent PShape
				max = PApplet.max(shape.getVertex(i).y, max);
			}
		}
		return max;
	}

}
