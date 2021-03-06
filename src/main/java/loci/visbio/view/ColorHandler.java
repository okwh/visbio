/*
 * #%L
 * VisBio application for visualization of multidimensional biological
 * image data.
 * %%
 * Copyright (C) 2002 - 2014 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package loci.visbio.view;

import java.rmi.RemoteException;
import java.util.Arrays;

import loci.visbio.VisBioFrame;
import loci.visbio.data.DataTransform;
import loci.visbio.data.ImageTransform;
import loci.visbio.data.ThumbnailHandler;
import loci.visbio.state.SaveException;
import loci.visbio.util.ColorUtil;
import loci.visbio.util.DataUtil;
import loci.visbio.util.DialogPane;
import loci.visbio.util.DisplayUtil;
import loci.visbio.util.ObjectUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

import visad.BaseColorControl;
import visad.Display;
import visad.DisplayImpl;
import visad.DisplayRealType;
import visad.FlatField;
import visad.RealType;
import visad.ScalarMap;
import visad.VisADException;

/**
 * Provides logic for controlling a TransformLink's color settings.
 */
public class ColorHandler {

	// -- Constants --

	/** Starting brightness value. */
	protected static final int NORMAL_BRIGHTNESS = 128;

	/** Starting contrast value. */
	protected static final int NORMAL_CONTRAST = 128;

	/** Starting opacity value. */
	protected static final int NORMAL_OPACITY = 256;

	// -- Fields --

	/** Associated link between data object and display. */
	protected TransformLink link;

	/** Dialog pane for adjusting color settings. */
	protected ColorPane colorPane;

	/** Brightness and contrast of images. */
	protected int brightness, contrast;

	/** Opacity value. */
	protected int opacityValue;

	/** Transparency model (CONSTANT_ALPHA or CURVED_ALPHA). */
	protected int opacityModel;

	/** Color model (RGB_MODEL, HSV_MODEL or COMPOSITE_MODEL). */
	protected int colorModel;

	/** Red, green and blue components of images. */
	protected RealType red, green, blue;

	// -- Fields - initial state --

	/** Minimum and maximum color range values. */
	protected double[] lo, hi;

	/** Flags indicating color ranges should not be auto-scaled. */
	protected boolean[] fixed;

	/** Color tables. */
	protected float[][][] colorTables;

	// -- Constructor --

	/** Creates a color handler for the given transform link. */
	public ColorHandler(final TransformLink link) {
		this.link = link;

		// default color settings
		brightness = NORMAL_BRIGHTNESS;
		contrast = NORMAL_CONTRAST;
		opacityValue = NORMAL_OPACITY;
		opacityModel = ColorUtil.CONSTANT_ALPHA;
		colorModel = ColorUtil.RGB_MODEL;
		red = green = blue = ColorUtil.CLEAR;
	}

	// -- ColorHandler API methods --

	/** Gets associated transform link. */
	public TransformLink getLink() {
		return link;
	}

	/** Gets GUI control pane for this color handler. */
	public ColorPane getColorPane() {
		return colorPane;
	}

	/** Gets the VisBio display window affected by this color handler. */
	public DisplayWindow getWindow() {
		return link.getHandler().getWindow();
	}

	/** Gets color mappings for this color handler's transform link. */
	public ScalarMap[] getMaps() {
		final DisplayImpl display = getWindow().getDisplay();
		final DataTransform trans = link.getTransform();
		if (!(trans instanceof ImageTransform)) return null;
		final ImageTransform it = (ImageTransform) trans;
		return DisplayUtil.getMaps(display, it.getRangeTypes(),
			new DisplayRealType[] { Display.RGB, Display.RGBA });
	}

	/**
	 * Applies color settings to the latest linked data object. If the reset flag
	 * is set, guesses at good initial color mappings.
	 */
	public void initColors(final boolean reset) {
		refreshPreviewData();

		if (reset) { // reset to the default color settings
			brightness = NORMAL_BRIGHTNESS;
			contrast = NORMAL_CONTRAST;
			opacityValue = NORMAL_OPACITY;
			opacityModel = ColorUtil.CONSTANT_ALPHA;
			colorModel = ColorUtil.RGB_MODEL;
			red = colorPane.getRed();
			green = colorPane.getGreen();
			blue = colorPane.getBlue();
		}
		final ScalarMap[] maps = getMaps();
		if (reset || lo == null) {
			lo = new double[maps.length];
			Arrays.fill(lo, 0.0);
		}
		if (reset || hi == null) {
			hi = new double[maps.length];
			Arrays.fill(hi, 255.0);
		}
		if (reset || fixed == null) {
			fixed = new boolean[maps.length];
			Arrays.fill(fixed, false);
		}
		if (reset || colorTables == null) {
			colorTables =
				ColorUtil.computeColorTables(maps, brightness, contrast, colorModel,
					red, green, blue);
		}

		final DisplayImpl display = getWindow().getDisplay();
		DisplayUtil.setDisplayDisabled(display, true);
		setColors(brightness, contrast, colorModel, red, green, blue, false);
		setOpacity(opacityValue, opacityModel, false);
		setRanges(lo, hi, fixed);
		setTables(colorTables);
		DisplayUtil.setDisplayDisabled(display, false);
	}

	/** Refreshes preview data from the transform link. */
	public void refreshPreviewData() {
		final DataTransform trans = link.getTransform();
		final ThumbnailHandler thumbs = trans.getThumbHandler();
		if (thumbs != null) {
			final int[] pos = link.getHandler().getPos(trans);
			FlatField ff = thumbs.getThumb(pos);
			try {
				if (trans instanceof ImageTransform) {
					final ImageTransform it = (ImageTransform) trans;
					if (ff != null) {
						ff = DataUtil.switchType(ff, it.getType(), it.getImageUnits());
					}
				}
				colorPane.setPreviewData(ff);
			}
			catch (final VisADException exc) {
				exc.printStackTrace();
			}
			catch (final RemoteException exc) {
				exc.printStackTrace();
			}
		}
	}

	/**
	 * Displays the color dialog pane onscreen and alters the color scheme as
	 * requested.
	 */
	public void showColorDialog() {
		refreshPreviewData();
		final DisplayWindow window = getWindow();
		final int rval = colorPane.showDialog(window.getControls());
		if (rval != DialogPane.APPROVE_OPTION) return;
		final DisplayImpl display = window.getDisplay();
		DisplayUtil.setDisplayDisabled(display, true);
		setColors(colorPane.getBrightness(), colorPane.getContrast(), colorPane
			.getColorMode(), colorPane.getRed(), colorPane.getGreen(), colorPane
			.getBlue(), false);
		setOpacity(colorPane.getOpacityValue(), colorPane.getOpacityModel(), false);
		setRanges(colorPane.getLo(), colorPane.getHi(), colorPane.getFixed());
		setTables(colorPane.getTables());
		DisplayUtil.setDisplayDisabled(display, false);
	}

	/** Updates color settings to those given. */
	public void setColors(final int brightness, final int contrast,
		final int colorModel, final RealType red, final RealType green,
		final RealType blue, final boolean compute)
	{
		this.brightness = brightness;
		this.contrast = contrast;
		this.colorModel = colorModel;
		this.red = red;
		this.green = green;
		this.blue = blue;
		if (compute) {
			setTables(ColorUtil.computeColorTables(getMaps(), brightness, contrast,
				colorModel, red, green, blue));
		}
	}

	/**
	 * Sets opacity.
	 * 
	 * @param opacityValue How opaque data should be, ranging from 0 - 256
	 * @param opacityModel ColorUtil.CONSTANT_ALPHA or ColorUtil.CURVED_ALPHA
	 * @param compute Whether to actually update the data
	 */
	public void setOpacity(final int opacityValue, final int opacityModel,
		final boolean compute)
	{
		this.opacityValue = opacityValue;
		this.opacityModel = opacityModel;
		if (compute) {
			final float[] alpha =
				ColorUtil.computeAlphaTable(opacityValue, opacityModel);
			ColorUtil.setAlphaTable(getMaps(), alpha);
		}
	}

	/** Updates map ranges to those given. */
	public void setRanges(final double[] lo, final double[] hi,
		final boolean[] fixed)
	{
		ColorUtil
			.setColorRanges(getWindow().getDisplay(), getMaps(), lo, hi, fixed);
	}

	/** Updates color tables to those given. */
	public void setTables(final float[][][] tables) {
		final DisplayWindow window = getWindow();
		ColorUtil.setColorMode(window.getDisplay(), colorModel);
		ColorUtil.setColorTables(getMaps(), tables);
		final VisBioFrame bio = window.getVisBio();
		bio.generateEvent(bio.getManager(DisplayManager.class),
			"color adjustment for " + window.getName(), true);
	}

	/** Recomputes autoscaled color range bounds. */
	public void reAutoScale() {
		if (fixed == null) return;
		final DisplayImpl display = getWindow().getDisplay();
		final ScalarMap[] maps = getMaps();
		for (int i = 0; i < maps.length; i++) {
			if (fixed[i]) continue;
			ColorUtil.reAutoScale(display, maps[i]);
		}
	}

	/** Gets brightness value. */
	public int getBrightness() {
		return brightness;
	}

	/** Gets contrast value. */
	public int getContrast() {
		return contrast;
	}

	/** Gets opacity value. */
	public int getOpacityValue() {
		return opacityValue;
	}

	/** Gets opacity model (CONSTANT_ALPHA or CURVED_ALPHA). */
	public int getOpacityModel() {
		return opacityModel;
	}

	/** Gets color model (RGB_MODEL, HSV_MODEL or COMPOSITE_MODEL). */
	public int getColorModel() {
		return colorModel;
	}

	/** Gets RealType mapped to red. */
	public RealType getRed() {
		return red;
	}

	/** Gets RealType mapped to green. */
	public RealType getGreen() {
		return green;
	}

	/** Gets RealType mapped to blue. */
	public RealType getBlue() {
		return blue;
	}

	/** Gets color mapping minimums. */
	public double[] getLo() {
		final ScalarMap[] maps = getMaps();
		if (maps == null) return lo;
		final double[] min = new double[maps.length];
		for (int i = 0; i < maps.length; i++)
			min[i] = maps[i].getRange()[0];
		return min;
	}

	/** Gets color mapping maximums. */
	public double[] getHi() {
		final ScalarMap[] maps = getMaps();
		if (maps == null) return hi;
		final double[] max = new double[maps.length];
		for (int i = 0; i < maps.length; i++)
			max[i] = maps[i].getRange()[1];
		return max;
	}

	/** Gets whether each color mapping has a fixed range. */
	public boolean[] getFixed() {
		final ScalarMap[] maps = getMaps();
		if (maps == null) return fixed;
		final boolean[] fix = new boolean[maps.length];
		for (int i = 0; i < maps.length; i++)
			fix[i] = !maps[i].isAutoScale();
		return fix;
	}

	/** Gets current color tables. */
	public float[][][] getTables() {
		final ScalarMap[] maps = getMaps();
		if (maps == null) return colorTables;
		final float[][][] tables = new float[maps.length][][];
		for (int i = 0; i < maps.length; i++) {
			final BaseColorControl cc = (BaseColorControl) maps[i].getControl();
			tables[i] = cc.getTable();
		}
		return tables;
	}

	// -- ColorHandler API methods - state logic --

	/** Tests whether two objects are in equivalent states. */
	public boolean matches(final ColorHandler handler) {
		return brightness == handler.brightness && contrast == handler.contrast &&
			opacityValue == handler.opacityValue &&
			opacityModel == handler.opacityModel &&
			colorModel == handler.colorModel &&
			ObjectUtil.objectsEqual(red, handler.red) &&
			ObjectUtil.objectsEqual(green, handler.green) &&
			ObjectUtil.objectsEqual(blue, handler.blue) &&
			ObjectUtil.arraysEqual(getLo(), handler.getLo()) &&
			ObjectUtil.arraysEqual(getHi(), handler.getHi()) &&
			ObjectUtil.arraysEqual(getFixed(), handler.getFixed()) &&
			ObjectUtil.arraysEqual(getTables(), handler.getTables());
	}

	/**
	 * Modifies this object's state to match that of the given object. If the
	 * argument is null, the object is initialized according to its current state
	 * instead.
	 */
	public void initState(final ColorHandler handler) {
		if (handler != null) {
			brightness = handler.brightness;
			contrast = handler.contrast;
			opacityValue = handler.opacityValue;
			opacityModel = handler.opacityModel;
			colorModel = handler.colorModel;
			red = handler.red;
			green = handler.green;
			blue = handler.blue;
			lo = handler.getLo();
			hi = handler.getHi();
			fixed = handler.getFixed();
			colorTables = handler.getTables();
		}

		if (colorPane == null) colorPane = new ColorPane(this);
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("TransformLink"). */
	public void saveState(final Element el) throws SaveException {
		final Element child = XMLUtil.createChild(el, "Colors");

		// save attributes
		child.setAttribute("brightness", "" + brightness);
		child.setAttribute("contrast", "" + contrast);
		child.setAttribute("opacityValue", "" + opacityValue);
		child.setAttribute("opacityModel", opacityModel == ColorUtil.CONSTANT_ALPHA
			? "constant" : "curved");
		child.setAttribute("colorModel", colorModel == ColorUtil.RGB_MODEL ? "rgb"
			: (colorModel == ColorUtil.HSV_MODEL ? "hsv" : "composite"));
		child.setAttribute("red", red == null ? "null" : red.getName());
		child.setAttribute("green", green == null ? "null" : green.getName());
		child.setAttribute("blue", blue == null ? "null" : blue.getName());
		child.setAttribute("min", ObjectUtil.arrayToString(getLo()));
		child.setAttribute("max", ObjectUtil.arrayToString(getHi()));
		child.setAttribute("fixed", ObjectUtil.arrayToString(getFixed()));

		// save color tables
		final float[][][] tables = getTables();
		if (tables != null) {
			for (int i = 0; i < tables.length; i++) {
				final Element tel = XMLUtil.createChild(child, "ColorTable");
				if (tables[i] != null) {
					for (int j = 0; j < tables[i].length; j++) {
						final Element cel = XMLUtil.createChild(tel, "ColorChannel");
						if (tables[i][j] != null) {
							XMLUtil.createText(cel, ObjectUtil.arrayToString(tables[i][j]));
						}
					}
				}
			}
		}
	}

	/**
	 * Restores the current state from the given DOM element ("TransformLink").
	 */
	public void restoreState(final Element el) throws SaveException {
		final Element child = XMLUtil.getFirstChild(el, "Colors");

		// restore attributes
		brightness = Integer.parseInt(child.getAttribute("brightness"));
		contrast = Integer.parseInt(child.getAttribute("contrast"));
		opacityValue = Integer.parseInt(child.getAttribute("opacityValue"));
		final String om = child.getAttribute("opacityModel");
		if ("constant".equals(om)) opacityModel = ColorUtil.CONSTANT_ALPHA;
		else if ("curved".equals(om)) opacityModel = ColorUtil.CURVED_ALPHA;
		else {
			System.err.println("Warning: invalid opacity model (" + om + ")");
			opacityModel = -1;
		}
		final String cm = child.getAttribute("colorModel");
		if ("rgb".equals(cm)) colorModel = ColorUtil.RGB_MODEL;
		else if ("hsv".equals(cm)) colorModel = ColorUtil.HSV_MODEL;
		else if ("composite".equals(cm)) colorModel = ColorUtil.COMPOSITE_MODEL;
		else {
			System.err.println("Warning: invalid color model (" + cm + ")");
			colorModel = -1;
		}
		final String r = child.getAttribute("red");
		red = r.equals("null") ? null : RealType.getRealType(r);
		final String g = child.getAttribute("green");
		green = g.equals("null") ? null : RealType.getRealType(g);
		final String b = child.getAttribute("blue");
		blue = b.equals("null") ? null : RealType.getRealType(b);
		lo = ObjectUtil.stringToDoubleArray(child.getAttribute("min"));
		hi = ObjectUtil.stringToDoubleArray(child.getAttribute("max"));
		fixed = ObjectUtil.stringToBooleanArray(child.getAttribute("fixed"));

		// restore attributes
		colorTables = null;
		final Element[] tels = XMLUtil.getChildren(child, "ColorTable");
		if (tels != null) {
			colorTables = new float[tels.length][][];
			for (int i = 0; i < tels.length; i++) {
				final Element[] cels = XMLUtil.getChildren(tels[i], "ColorChannel");
				if (cels != null) {
					colorTables[i] = new float[cels.length][];
					for (int j = 0; j < cels.length; j++) {
						colorTables[i][j] =
							ObjectUtil.stringToFloatArray(XMLUtil.getText(cels[j]));
					}
				}
			}
		}
	}

}
