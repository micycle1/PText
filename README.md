<h3 align="center"> 🚧 This README (and library) is under construction 🚧 </h3>

# PText

**PText** bridges the gap between Processing's PFont and PShape, providing some much needed functionality when working with text in Processing.

 and fixes inaccuracies with existing methods such as ...

PText extends `PShape`, meaning that is stores text in a vector format. With this, it offers methods for easy size manipulation and provides other methods to get the exact dimensions of the text. It provides some fully accurate versions of `textWidth()`, `textAscent()`, and `textDescent()` (see the appendix for why these are inaccurate).

## API

Notable parts of the API are described below.

### `getTextAscent()`

Returns, unlike the default PFont implementation which returns the maximum ascent of the font. [baseline](https://www.wikiwand.com/en/Baseline_(typography))

### `getTextDescent()`

For the **font** of the PText  

Likewise, 

### `setFont(PFont)`
### `setFont(String, int)`

Change the font of the text on the fly


`setText()`

Change the text of the string in the fly

### `scale()`
### `scaleWidth()`
### `scaleHeight()`

scale

### `getTextWidth()`
### `getTextHeight()`

get exact dimensions (the black bounding box in the example below)

### `debug()`
Draws debug info (string bounding box, per-character bounding boxes, character verticies and the baseline) Debug info is aligned with the text when shapeMode(CENTER) is used (Processing's default).

### `getWhiteSpaceLeft()`
### `getWhiteSpaceRight()`

The left version is useful for knowing how much whitespace there is between rendering the text and 



## Example

Resizing a PText shape using setTextWidth() and setTextHeight(), using debug() to show 

```
import pText.PText;

PText text;

void setup() {
  
  size(1280, 720);
  smooth(4);

  text = new PText(this, "Bauhaus 93", 192);
  
  text.setText("hello");
  
  text.setFill(color(55, 255, 90));
  text.setStrokeWeight(1);

  text.setScale(1, 1);
  //shapeMode(CENTER);
  text.setTextWidth(width);
  text.setTextHeight(height);
  
    noFill(); // you must call global noFill() after any setText(), otherwise text can't be filled
}

void draw() {

  background(255);

  shape(text, mouseX, mouseY);
  //text.debug(mouseX, mouseY);
}
```



<h1 align="center">
  <a href="https://github.com/micycle1/">
  <img src="resources/resize_example.gif" alt="example"/></a>
</h1>

## TODO

* Scale whitespace (to increase/decrease spacing between letters, independent of font size)
* Display dimension labels (such as ascent & descent) in debug mode
* String ascent: return the max ascent of the string's **current** characters
* String descent: return the max descent of the string's **current** characters
* Allow multiple fonts within one PText at once?

## Appendix

Using the inbuilt functions `textWidth()`, `textAscent()`, and `textDescent()` are an easy way to get a *good* approximate result for the height and width of a string (of a given font), but they are not *exact*.

Why? 

- `textAscent()` returns text height above the baseline **based on the letter 'd'**
- `textDescent()` returns text height below the baseline **based on the letter 'p'**.
- `textWidth()` includes glyph whitespace (aka padding; ideally we want to ignore this for the first and last characters)

`textAscent() + textDescent()` therefore measures the **maximum height** of a string in a given font and font size, and not the height of a specific string. In other words, if your text doesn't include both 'd' and 'p' characters, then using these methods to determine text height will overestimate the result.