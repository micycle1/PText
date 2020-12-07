package vectorText;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.core.PShape;
import processing.event.MouseEvent;

public class VectorTextTest extends PApplet {

	VectorText shape;

	PShape shape2;

	PImage img;

	public static void main(String[] args) {
		PApplet.main(VectorTextTest.class);
	}

	@Override
	public void settings() {
		size(1200, 1200);
		smooth(4);
		String[] fontList = PFont.list();
//		printArray(fontList);
	}

	float scale = 1;

	PFont font;

	float testHeight;
	float testWidth;
	String string = "asdX344g#";
	float whitespace = 0;
	float maxY;

	@Override
	public void setup() {
//		strokeWeight(5);
//		shape2 = text2PShape("@8asdDXz", createFont("Freestyle Script", 156, true));
//		shape.setFill(125);
//		shape.setStroke(color(0,255,0));

//		shape.setScale(0.77f);

		string = "@#'pdXW\\";

		font = createFont("Arial", 96, true);
		textFont(font);

		float minY = Float.MAX_VALUE;
		maxY = Float.NEGATIVE_INFINITY;

		for (Character c : string.toCharArray()) {
			PShape character = font.getShape(c); // character vector
			for (int i = 0; i < character.getVertexCount(); i++) {
				minY = min(character.getVertex(i).y, minY);
				maxY = max(character.getVertex(i).y, maxY);
			}
		}

		float height = maxY - minY;

//		for (Character c : string.toCharArray()) {
//			height = max(height, font.getGlyph(c).height);
//		}

//		height = VectorText.getHeight(vectorText);

		width = 0;

		for (Character c : string.toCharArray()) {
			final float charWidth = font.width(c) * font.getSize();
			width += charWidth;
		}
		System.out.println(width);

		width = (int) (textWidth(string));

		whitespace = (font.width(string.charAt(string.length() - 1)) * font.getSize()
				- font.getGlyph(string.charAt(string.length() - 1)).width) / 2;
		width -= whitespace;

		whitespace = (font.width(string.charAt(0)) * font.getSize() - font.getGlyph(string.charAt(0)).width) / 2;
		width -= whitespace;

//		width -= whitespace;
//		width = (int) (textWidth(string) - whitespace);

		testHeight = height;
		testWidth = width;

		shape = new VectorText(this, font);
//		shape.setText("@8a#p`dDXz");
		shape.setText("PDdpal#0!");
//		shape.setText(string);
		shape.setFill(0);

		img = loadImage("img.jpg");

		img.mask(shape.getAlphaMaskInverse(640, 640));
		shape.setFill(color(255, 0, 0));

		shapeMode(CENTER);
		
		shape.scale(2, 1);

	}

	float o = 0;

	@Override
	public void draw() {
		background(255);

//		image(img, 0, 0);
//		shape.setScale(map(mouseX, 0, width, 0.5f, 3));
		noStroke();
		shape.setStrokeWeight(0);
		shape(shape, mouseX, mouseY);
//		image(shape.get(), 0, 0);
//		image(shape.get(640, 640),0,0);
//		shape(shape2, mouseX, mouseY+-shape.height);
//		rect(mouseX, mouseY, shape.width, -shape.height); // should be affected by scale
//		shape.setScale(abs(sin(frameCount*0.01f))*3);
		noFill();

		o = (font.width(string.charAt(0)) * font.getSize() - font.getGlyph(string.charAt(0)).width) / 2;

		stroke(0);
		strokeWeight(3);
//		text(string, mouseX, mouseY);
		stroke(0, 155, 0);
//		rect(mouseX + whitespace, mouseY + maxY, width, -testHeight);
//		rect(mouseX, mouseY, textWidth(string), -textAscent() + textDescent());
		shape.debug(mouseX, mouseY);
		strokeWeight(3);
		int yoff = 0;
		float w = 0;

		noFill();
		strokeWeight(1);
//		rect(mouseX, mouseY + o, w, -shape.height);

		////////////////////

//		textSize(36);

		String str = "Hello world";
		float x = 100;
		float y = 100;
		float strWidth = textWidth(str);
		float strAscent = textAscent();
		float strDescent = textDescent();
		float strHeight = strAscent + strDescent;

		rect(x, y - strAscent, strWidth, strHeight);

		fill(0);
		text(str, x, y);

	}

	@Override
	public void keyPressed() {
		o++;
	}

	@Override
	public void mouseWheel(MouseEvent event) {
		scale += event.getAction() * 0.01f;
		shape.setScale(scale);
	}

//	/**
//	 * Vector text using sketch's current font (default or any font set via
//	 * textFont()).
//	 * 
//	 * @param text
//	 * @return
//	 */
//	public VectorText text2PShape(String text) {
//		return textToPShape(text, getGraphics().textFont);
//	}
//
//	/**
//	 * Vector text using a given PFont.
//	 * 
//	 * @param text
//	 * @param font
//	 * @return
//	 */
//	public VectorText text2PShape(String text, PFont font) {
//		return textToPShape(text, font);
//	}
//
//	/**
//	 * VectorText extends PShape? TODO optical kerning
//	 * 
//	 * @param text
//	 * @param font
//	 * @return
//	 */
//	private VectorText textToPShape(String text, PFont font) {
//
//		VectorText parent = new VectorText(this);
//		
//
//		float translationX = 0; // running total of character x-axis translation
//
//		for (Character c : text.toCharArray()) {
//			PShape character = font.getShape(c);
//
//			character.translate(translationX, 0);
//			character.disableStyle(); // parent should override style
//
//			parent.addChild(character);
//
//			translationX += font.width(c) * font.getSize(); // width() returns width with size 1, so scale up
//		}
//		
//		parent.width = translationX;
////		parent.height = (font.ascent() + font.descent())*font.getSize();
//		parent.height = getMaxY(parent) - getMinY(parent);
//		
//		return parent;
//	}
//
//	/**
//	 * 
//	 * @param shape GROUP PShape
//	 * @return
//	 */
//	public static float getMinY(PShape shape) {
//		float min = Float.MAX_VALUE;
//		if (shape.getFamily() == GROUP) {
//			for (PShape child : shape.getChildren()) {
//				for (int i = 0; i < child.getVertexCount(); i++) {
//					min = min(child.getVertex(i).y, min);
//				}
//			}
//		} else {
//			for (int i = 0; i < shape.getVertexCount(); i++) {
//				min = min(shape.getVertex(i).y, min);
//			}
//		}
//		return min;
//	}
//
//	public static float getMaxY(PShape shape) {
//		float max = Float.MIN_VALUE;
//		if (shape.getFamily() == GROUP) {
//			for (PShape child : shape.getChildren()) {
//				for (int i = 0; i < child.getVertexCount(); i++) {
//					max = max(child.getVertex(i).y, max);
//				}
//			}
//		} else {
//			for (int i = 0; i < shape.getVertexCount(); i++) {
//				max = max(shape.getVertex(i).y, max);
//			}
//		}
//		return max;
//	}

}
