package micycle.ptext;

import static processing.core.PApplet.sin;
import static processing.core.PApplet.cos;
import static processing.core.PApplet.round;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PVector;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Vectorised text for Processing. A middleman between PFonts and PShapes. Eases
 * working with text.
 * 
 * @author Michael Carleton
 *
 */
public class PText extends PShape {

	// TODO max width, if letter.x > width, new line (offset y too)
	// TODO return this, for method chaining
	// TODO TEXT SLANT (based on Y)
	// TODO ttf fonts
	// TODO use javadocs from
	// https://learning.oreilly.com/library/view/java-awt-reference/9781565922402/06_chapter-03.html

	private static final DecimalFormat format = new DecimalFormat("0.0");

	public float scaleX = 1;
	public float scaleY = 1;

	public float width, height;

	private PApplet p;

	private PFont font;

	private String text;

	private ArrayList<Float> charDescent = new ArrayList<Float>(); // used to draw debug outlines

	private float descent = 0; // descent for this specific string
	private float ascent = 0; // ascent for this specific string

	private float characterSpacing = 0;
	private float totalSpacing = 0; // sum of character spacing; not affected by scale
	private float[] perCharacterSpacing; // per-character spacing offset (sum of offsets before)

	private float[] perCharacterRotation; // per-character rotation

	private PVector perCharacterCenterPoint[];

	private float shearX = 0; // current shearX

	/**
	 * Using a specific font
	 * 
	 * @param p
	 * @param font
	 */
	public PText(PApplet p, PFont font) {
		super(p.getGraphics(), GROUP);
		if (font == null) {
			String randomFont = PFont.list()[PApplet.round(p.random(PFont.list().length))];
			font = p.createFont(randomFont, 96, true);
			System.err.println(String.format("ERROR: Null PFont. Using a random system font (%s), 96 pt.", randomFont));
		}
		this.font = font;
		this.p = p;
		setStrokeWeight(0); // no stroke by default
	}

	/**
	 * Uses current sketch font (random system font if no sketch font).
	 * 
	 * @param p
	 */
	public PText(PApplet p) {
		this(p, p.getGraphics().textFont);
	}

	public PText(PApplet p, String fontName, int size) {
		this(p, p.createFont(fontName, size, true));
	}

	/**
	 * Sets the horizontal and vertical scale, regardless of the existing scale.
	 * 
	 * @param scale
	 * @see #scale(float), scales by multiplying the existing scale value
	 */
	public void setScale(float scale) {
		setScale(scale, scale);
	}

	public void setScale(float scaleX, float scaleY) {
		scale(scaleX / this.scaleX, scaleY / this.scaleY);
	}

	/**
	 * 
	 * Scales scale by current scale.
	 * 
	 * * Increases or decreases the size of the text by expanding and contracting
	 * vertices. Transformations apply to everything that happens after and
	 * subsequent calls to the function multiply the effect. For example, calling
	 * scale(2.0) and then scale(1.5) is the same as scale(3.0).
	 * 
	 * TODO does not affect character spacing
	 * 
	 * @see #setScale(float)
	 */
	@Override
	public void scale(float s) {
		scale(s, s);
	}

	/**
	 * Increases or decreases the size of the text by expanding and contracting
	 * vertices. Transformations apply to everything that happens after and
	 * subsequent calls to the function multiply the effect. For example, calling
	 * scale(2.0) and then scale(1.5) is the same as scale(3.0).
	 * 
	 * @see #setScale(float, float)
	 */
	@Override
	public void scale(float x, float y) {
		x = PApplet.max(x, 0.01f); // prevent 0
		y = PApplet.max(y, 0.01f); // prevent 0
		scaleX *= x;
		width *= x;
		scaleY *= y;
		height *= y;
		super.scale(x, y);
	}

	@Override
	public void scale(float x, float y, float z) {
		scale(x, y);
	}

	/**
	 * Set the width of text (more precisely, the width of the bounding box) (the
	 * left and right whitespace is not counted)
	 * 
	 * @param width
	 */
	public void setTextWidth(float width) {
		// TODO account for character spacing
		final float factor = width / getTextWidth();
		scale(factor, 1);
	}

	/**
	 * Set the height of text (more precisely, the height of the bounding box)
	 * 
	 * @param height
	 */
	public void setTextHeight(float height) {
		final float factor = height / getTextHeight();
		scale(1, factor);
	}

	/**
	 * Set the font to the font given
	 * 
	 * @param font
	 */
	public void setFont(PFont font) {
		this.font = font;
		setText(text); // recreate text
	}

	/**
	 * 
	 * @param font an installed system font, identified by its name
	 */
	public void setFont(String font, int defaultSize) {
		this.font = p.createFont(font, defaultSize, true);
		setText(text); // recreate text
	}

	public void scaleHeight(float scaleY) {
		setScale(1, scaleY);
	}

	public void scaleWidth(float scaleX) {
		setScale(scaleX, 1);
	}

	public void setHeightScale(float scale) {
		// TODO
	}

	public void setWidthScale(float scale) {
		// TODO
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		setText(text.toCharArray());
	}

	public void setText(char[] text) {

		width = 0;
		height = Float.NEGATIVE_INFINITY;

		while (getChildCount() > 0) { // remove any exising letters
			removeChild(0);
		}
		charDescent.clear();

		setScale(1, 1); // reset scaling

		this.text = new String(text);

		float translationX = -getCharWhiteSpace(text[0]) / 2; // a running total of character x-axis translation

		perCharacterCenterPoint = new PVector[text.length]; // calculate during iteration // TODO change when scaled

		for (int i = 0; i < text.length; i++) {
			char c = text[i];
			PShape character = font.getShape(c);

			/**
			 * Offset using vertex X position, not translate(), since translation value is
			 * not accessible once set. This means Y values will be around 0.
			 */
			for (int j = 0; j < character.getVertexCount(); j++) {
				float newX = character.getVertexX(j) + translationX;
				character.setVertex(j, newX, character.getVertexY(j));
			}

			character.disableStyle(); // parent PText should override style

			addChild(character);

			translationX += getCharWidth(c); // width() returns width with size 1, so scale up

			charDescent.add(getMaxY(character)); // store this character's descent value

			perCharacterCenterPoint[i] = getCenterPoint(character);
		}

		width = translationX - getCharWhiteSpace(text[text.length - 1]) / 2f; // take away whitespace of last char

		height = getHeight(this);
		descent = getMaxY(this);
		ascent = PApplet.abs(getMinY(this));

		perCharacterSpacing = new float[text.length]; // init per-character spacing all to 0
		perCharacterRotation = new float[text.length]; // init per-character spacing all to 0

	}

	/**
	 * Sets the spacing between characters. This spacing is absolute (not affected
	 * by scaling).
	 * 
	 * @param spacing negative (characters closer) or positive (characters farther)
	 */
	public void setCharacterSpacing(float spacing) {

		float offset = spacing;
		for (int i = 1; i < getChildCount(); i++) { // start at 1; ignore first character
			PShape child = getChild(i);
			child.resetMatrix(); // reset old translation
			child.translate(offset / scaleX, 0); // current scale affects translate(), so divide to be scale invariant
			perCharacterSpacing[i] = offset; // write per-character total offset (scaleX invariant)
			offset += spacing;
		}

		totalSpacing = offset - spacing; // subtract right-spacing of last character
		characterSpacing = spacing;
	}

	/**
	 * Shear text in the X direction, based on the text's base line. Use this to
	 * italicise text.
	 * 
	 * @param x Defines the X offset of points which have the highest Y position.
	 *          Can be negative
	 */
	public void setShearX(float shear) {

		// TODO rotate char back to 0, perform shear, then rotate back

		/**
		 * The built-in shearX() affects transformation matrix only, not the location of
		 * a PShape's vertices TODO variable baseline
		 */

		shear *= -1;

		float logicalShear = shear - shearX; // only shear the difference between old and new shear

		// the built-in shearX() affects the matrix only, not the vertices location (and
		// there's no way to apply the matrix to vertices, so do manually here.

		if (logicalShear != 0) { // save on compute if no change needed

			for (PShape child : getChildren()) { // search all children PShapes
				for (int i = 0; i < child.getVertexCount(); i++) {
					PVector vertex = child.getVertex(i);
					vertex.x += PApplet.map(vertex.y, 0, getTextAscent(), 0, logicalShear);
					child.setVertex(i, vertex); // need to call setVertex() to apply change
				}
			}
		}

//		vertex.y += PApplet.map(vertex.x, 0, getTextWidth(), -logicalShear, logicalShear);
//		child.setVertex(i, vertex); // need to call setVertex() to apply change

		shearX = shear;

	}

	/**
	 * Sets the rotation for a character, given by it's index in the string. Rotates
	 * the character around midpoint of its vertices, not the baseline.
	 * 
	 * @param index index of character in string
	 * @param angle radians
	 * @see #setCharacterRotation(float)
	 */
	public void setCharacterRotation(final int index, float angle) {
		if (index >= 0 && index < getChildCount()) {

			angle %= TWO_PI;// set angle in range -2PI...2PI
			angle = angle < 0 ? TWO_PI + angle : angle; // make angle positive: 0...2PI

			float angleDiff = angle - perCharacterRotation[index]; // calc diff between new and current angle
			angleDiff = (angleDiff + PI) % TWO_PI - PI;
			perCharacterRotation[index] = angle;

			if (angleDiff != 0) { // don't rotate if none needed

				/**
				 * = Mutates the shape's vertices, rather than using translation matrix.
				 */
				PShape character = getChild(index);
				PVector centerPoint = getCenterPoint(character); // TODO possibly cache value
				for (int i = 0; i < character.getVertexCount(); i++) {
					character.setVertex(i, rotateAroundPoint(character.getVertex(i), centerPoint, angleDiff));
				}
			}

		} else {
			System.err.println("Character index out of bounds.");
		}

	}

	/**
	 * Sets the rotation for every character in the string.
	 * 
	 * @param angle
	 * @see PText#setCharacterRotation(int, float)
	 */
	public void setCharacterRotation(float angle) {
		for (int i = 0; i < getChildCount(); i++) {
			setCharacterRotation(i, angle);
		}
	}

	/**
	 * Draws debug info (string bounding box, per-character bounding boxes,
	 * character vertices and the baseline) Debug info is aligned with the text when
	 * shapeMode(CENTER) is used (Processing's default).
	 * 
	 * @param posX
	 * @param posY
	 * 
	 */
	public void debug(float posX, float posY) {
		debug(posX, posY, true, true, true, true, true, true);
	}

	/**
	 * 
	 * @param posX
	 * @param posY
	 * @param bounds
	 * @param charBounds
	 * @param baseline
	 * @param vertices
	 * @param charSpacing
	 * @param info
	 */
	public void debug(float posX, float posY, boolean bounds, boolean charBounds, boolean baseline, boolean vertices, boolean charSpacing,
			boolean info) {

		p.pushStyle(); // save existing style

		p.noFill();
		p.noStroke();

		/**
		 * Draw string bounding box (excludes whitespace)
		 */
		if (bounds) {
			p.stroke(0);
			p.strokeWeight(4);
//			p.rect(posX, posY + getTextDescent(), getTextWidth(), -getTextHeight());
			p.rect(posX + getMinX(getChild(0)), posY + getMaxY(this), getMaxX(this) - getMinX(getChild(0)), -getHeight(this));
		}

		/**
		 * Draw per-character bounding boxes (includes per-char whitespace)
		 */
		if (charBounds) {
			p.stroke(90, 230, 111);
			p.strokeWeight(2);
			float translationX = 0; // running total of character widths

			if (shearX == 0) {
				for (int i = 0; i < text.length(); i++) {
					final char c = text.charAt(i);
					final float charXPos = translationX + posX - getWhiteSpaceLeft() + perCharacterSpacing[i];
//					p.pushMatrix();
//					p.translate(perCharacterCenterPoint[i].x, perCharacterCenterPoint[i].y + posY);
//					p.rotate(perCharacterRotation[i]);
//					p.rect(0, 0, getCharWidth(c), -getCharHeight(c));
//					p.popMatrix();

					p.rect(charXPos, posY + charDescent.get(i) * scaleY, getCharWidth(c), -getCharHeight(c));
					translationX += getCharWidth(c);
				}
			} else { // TODO
				for (int i = 0; i < text.length(); i++) {

					if (i % 2 == 0) { // alternate stoke color to see overlap better
						p.stroke(90, 230, 111);
					} else {
						p.stroke(230, 164, 90);
					}

					float w = getWidth(getChild(i));// + getCharWhiteSpace(text.charAt(i));
					float h = getHeight(getChild(i));
					float xOff = getMinX(getChild(i));
					float yOff = getMaxY(getChild(i));
					char c = text.charAt(i);
					float charXPos = posX - getWhiteSpaceLeft() + perCharacterSpacing[i];
					p.rect(charXPos + xOff + getWhiteSpaceLeft(), posY + yOff, w, -h);
					translationX += w;
				}
			}
		}

		/**
		 * Draw baseline
		 */
		if (baseline) {
			p.strokeWeight(4);
			p.stroke(180, 90, 230);
			p.line(posX, posY, posX + getTextWidth(), posY);
		}

		/*
		 * Draw character spacing
		 */
		if (charSpacing && characterSpacing != 0) {
			p.strokeWeight(3);
			if (characterSpacing < 0) {
				p.stroke(230, 90, 90); // pale red
			} else {
				p.stroke(230, 210, 90); // pale yellow
			}
			float spacingOffset = posX + getCharWidth(text.charAt(0)) - getWhiteSpaceLeft(); // x pos of previous char
																								// bounding
																								// box (right side)
			for (int i = 1; i < text.length(); i++) {
				p.line(spacingOffset + perCharacterSpacing[i - 1], posY - getTextAscent() / 2, spacingOffset + perCharacterSpacing[i],
						posY - getTextAscent() / 2);
				spacingOffset += getCharWidth(text.charAt(i));
			}
			/**
			 * Draw boxes instead
			 */
//			spacingOffset = charWidth(text.charAt(0));
//			for (int i = 1; i < text.length() - 1; i++) {
//				Character c = text.charAt(i);
//				float charXPos = spacingOffset + posX - getWhiteSpaceLeft() + perCharacterSpacing[i];
//				p.rect(charXPos - characterSpacing / 2, posY + charDescent.get(i) * scaleY, charWidth(c) + characterSpacing,
//						-charHeight(c));
//				spacingOffset += charWidth(c);
//			}
		}

		/**
		 * Draw vertices as points
		 */
		if (vertices) {
			p.stroke(0, 255, 0); // modify style
			p.strokeWeight(4);
			p.ellipseMode(CENTER); // draw exactly on point
			for (int j = 0; j < getChildren().length; j++) {
				PShape child = getChild(j);
				for (int i = 0; i < child.getVertexCount(); i++) {
					p.stroke(child.getVertexCode(i) * 50);
					p.point((child.getVertex(i).x) * scaleX + posX + perCharacterSpacing[j], child.getVertex(i).y * scaleY + posY);
				}
			}
		}

		/*
		 * Draw text dimensions info
		 */
		if (info) {
			p.textSize(14);
			p.fill(0);
			p.textAlign(LEFT);
			p.text(String.format("Width: %s Height: %s Ascent: %s Descent: %s Char Spacing: %s", round(getTextWidth()),
					round(getTextHeight()), format(getTextAscent()), format(getTextDescent()), format(characterSpacing)), posX,
					posY - getTextAscent() - 14);
		}
		p.popStyle(); // return saved style

	}

	/**
	 * Returns the font's ascent (distance from the top of the tallest glyph (letter
	 * 'd') to the baseline).
	 * 
	 * @return
	 * @see #getTextAscent()
	 */
	public float getFontAscent() {
		return font.ascent() * font.getSize() * scaleY;
	}

	/**
	 * Returns the font's descent (distance from the baseline to the bottom of the
	 * lowest descenders of the glyphs (letter 'p'))
	 * 
	 * @return
	 * @see #getTextDescent()
	 */
	public float getFontDescent() {
		return font.descent() * font.getSize() * scaleY;
	}

	/**
	 * Returns the width for a given character Affected by vector text's current
	 * scaling
	 * 
	 * @param c
	 * @return
	 */
	public float getCharWidth(char c) {
		return font.width(c) * font.getSize() * scaleX;
	}

	public float getCharHeight(char c) {
		return font.getGlyph(c).height * scaleY;
	}

	/**
	 * Get horizontal character whitespace (difference between font char and glyph).
	 * The whitespace is the sum of the whitespace to the left of the char and the
	 * whitespace to the right of the char. Not affected by
	 * {@link #setCharacterSpacing(float)}.
	 * 
	 * @param c
	 * @return
	 */
	public float getCharWhiteSpace(char c) {
		return getCharWidth(c) - (font.getGlyph(c).width * scaleX);
	}

	/**
	 * Returns horizontal bounds/width of the text (including whitespace of the
	 * current string)
	 * 
	 * @return
	 */
	public float getTextWidth() {
		return width + totalSpacing;
	}

	/**
	 * Returns exact height of the text (including descent and ascent of the current
	 * string).
	 * 
	 * @return
	 */
	public float getTextHeight() {
		return height;
	}

	/**
	 * Returns the descent (the maximum height of any character below the baseline)
	 * of the current text. The units for this measurement are pixels.
	 * 
	 * @return
	 */
	public float getTextDescent() {
//		float maxDescent = Float.NEGATIVE_INFINITY;
//		for (float charDescent : charDescent) {
//			maxDescent = Math.max(maxDescent, charDescent);
//		}
//		return maxDescent * scaleY;
		return descent * scaleY;
	}

	/**
	 * Returns the ascent (the maximum height of any character above the baseline)
	 * of the current text. The units for this measurement are pixels.
	 * 
	 * @return
	 */
	public float getTextAscent() {
		// alternatively iterate over string, calling charHeight(char), and calc the max
		// charheight
		return ascent * scaleY;
	}

	/**
	 * Gets the whitespace
	 * 
	 * @return
	 */
	public float getWhiteSpaceLeft() {
		return getCharWhiteSpace(text.charAt(0)) / 2;
	}

	/**
	 * Gets the whitespace
	 * 
	 * @return
	 */
	public float getWhiteSpaceRight() {
		return getCharWhiteSpace(text.charAt(text.length() - 1)) / 2;
	}

	/**
	 * Rasterise the text and returning it as a PImage.
	 * 
	 * @return
	 */
	public PImage get() {
		PGraphics temp = p.createGraphics((int) width, (int) (height + getFontDescent()));
		temp.smooth();
		temp.beginDraw();
		temp.shape(this, 0, height);
		temp.endDraw();

		return temp.get();
	}

	/**
	 * 
	 * @param index
	 * @return
	 * @deprecated , see {@link #getChild(int)} ?
	 */
	public PShape getCharacterAsPShape(int index) {
		if (index >= 0 && index < getChildCount()) {
			return getChild(index);
		} else {
			System.err.println("Character index out of bounds.");
			return null;
		}
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

	/**
	 * Using shape() draws text according to LEFT,BASELINE. Use this method to draw
	 * at a custom, similar to textAlign()
	 * 
	 * @param x
	 * @param y
	 * @param alignX horizontal alignment, either LEFT, CENTER, or RIGHT
	 * @param alignY vertical alignment, either TOP, BOTTOM, CENTER, or BASELINE
	 * 
	 */
	public void draw(float x, float y, int alignX, int alignY) {

		final float offsetX;
		final float offsetY;

		switch (alignX) {
			case RIGHT :
				offsetX = -getTextWidth();
				break;
			case CENTER :
				offsetX = -getTextWidth() / 2;
				break;
			default :
				System.err.println("Unrecognised X alignment. Defaulting to LEFT.");
			case LEFT :
				offsetX = 0;
				break;
		}

		switch (alignY) {
			case TOP :
				offsetY = getTextAscent();
				break;
			case CENTER :
				offsetY = -getTextDescent() + getTextHeight() / 2;
				break;
			case BOTTOM :
				offsetY = -getTextDescent();
				break;
			default :
				System.err.println("Unrecognised X alignment. Defaulting to BASELINE.");
			case BASELINE :
				offsetY = 0;
		}

		p.shape(this, x + offsetX, y + offsetY);

	}

	/**
	 * @param shape PShape (vectorised text)
	 * @return y difference (height) between lowest and highest points of the shape
	 *         (including any children shapes)
	 */
	private static float getHeight(PShape shape) {

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
	 * Return value of smallest (up-most) Y coordinate from shape (may return
	 * negative values)
	 * 
	 * @param shape
	 * @return
	 */
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

	/**
	 * @param shape PShape (vectorised text)
	 * @return x difference (width) between left-most and right-most points of the
	 *         shape (including any children shapes)
	 */
	private static float getWidth(PShape shape) {

		float min = Float.MAX_VALUE;
		float max = Float.NEGATIVE_INFINITY;

		if (shape.getFamily() == GROUP) {
			for (PShape child : shape.getChildren()) { // search all children PShapes
				for (int i = 0; i < child.getVertexCount(); i++) {
					min = PApplet.min(child.getVertex(i).x, min);
					max = PApplet.max(child.getVertex(i).x, max);
				}
			}
		} else {
			for (int i = 0; i < shape.getVertexCount(); i++) { // search only parent PShape
				min = PApplet.min(shape.getVertex(i).x, min);
				max = PApplet.max(shape.getVertex(i).x, max);
			}
		}
		return max - min;
	}

	/**
	 * Return value of smallest (left-most) X coordinate from shape (may return
	 * negative values)
	 * 
	 * @param shape
	 * @return
	 */
	private static float getMinX(PShape shape) {
		float min = Float.MAX_VALUE;
		if (shape.getFamily() == GROUP) {
			for (PShape child : shape.getChildren()) { // search all children PShapes
				for (int i = 0; i < child.getVertexCount(); i++) {
					min = PApplet.min(child.getVertex(i).x, min);
				}
			}
		} else {
			for (int i = 0; i < shape.getVertexCount(); i++) { // search only parent PShape
				min = PApplet.min(shape.getVertex(i).x, min);

			}
		}
		return min;
	}

	/**
	 * Return value of greatest (right-most) X coordinate from shape
	 * 
	 * @param shape
	 * @return
	 */
	private static float getMaxX(PShape shape) {
		float max = Float.NEGATIVE_INFINITY;
		if (shape.getFamily() == GROUP) {
			for (PShape child : shape.getChildren()) { // search all children PShapes
				for (int i = 0; i < child.getVertexCount(); i++) {
					max = PApplet.max(child.getVertex(i).x, max);
				}
			}
		} else {
			for (int i = 0; i < shape.getVertexCount(); i++) { // search only parent PShape
				max = PApplet.max(shape.getVertex(i).x, max);
			}
		}
		return max;
	}

	/**
	 * Return value of greatest (right-most) X coordinate from shape
	 * 
	 * @param shape
	 * @return
	 */
	private static PVector getCenterPoint(PShape shape) {

		// TODO call when PText scaled etc

		PVector centerPoint = new PVector();

		if (shape.getFamily() == GROUP) {
			int n = 0;
			for (PShape child : shape.getChildren()) { // search all children PShapes
				for (int i = 0; i < child.getVertexCount(); i++) {
					centerPoint.x += child.getVertexX(i);
					centerPoint.y += child.getVertexY(i);
					n++;
				}
			}
			centerPoint.div(n);
		} else {
			for (int i = 0; i < shape.getVertexCount(); i++) { // search only parent PShape
				centerPoint.x += shape.getVertexX(i);
				centerPoint.y += shape.getVertexY(i);
			}
			centerPoint.div(shape.getVertexCount()); // divide total by n vertices
		}
		return centerPoint;
	}

	/**
	 * Rotates the point around the center
	 * 
	 * @param pvector PVector to rotate
	 * @param pointX
	 * @param pointY
	 * @param angle   radians
	 * @return
	 */
	private static PVector rotateAroundPoint(PVector point, PVector center, float angle) {
		float rotatedX = cos(angle) * (point.x - center.x) - sin(angle) * (point.y - center.y) + center.x;
		float rotatedY = sin(angle) * (point.x - center.x) + cos(angle) * (point.y - center.y) + center.y;
		return new PVector(rotatedX, rotatedY);

	}

	/**
	 * Format to 1 d.p.
	 * 
	 * @param n
	 * @return
	 */
	private static String format(float n) {
		return format.format(n);
	}

}
