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

package loci.visbio.data;

import java.awt.image.BufferedImage;
import java.math.BigInteger;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import loci.formats.gui.AWTImageTools;
import loci.visbio.state.Dynamic;
import loci.visbio.state.SaveException;
import loci.visbio.util.MathUtil;
import loci.visbio.util.ObjectUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

/**
 * DataSampling is a resampling of another transform.
 */
public class DataSampling extends ImageTransform {

	// -- Fields --

	/** Minimum index value for each dimension. */
	protected int[] min;

	/** Maximum index value for each dimension. */
	protected int[] max;

	/** Index step value for each dimension. */
	protected int[] step;

	/** Width of each image. */
	protected int resX;

	/** Height of each image. */
	protected int resY;

	/** List of included range components. */
	protected boolean[] range;

	/** Range component count for each image. */
	protected int numRange;

	/** Controls for this data sampling. */
	protected SamplingWidget controls;

	// -- Constructors --

	/** Creates an uninitialized data sampling. */
	public DataSampling() {
		super();
	}

	/** Creates a data sampling from the given transform. */
	public DataSampling(final DataTransform parent, final String name,
		final int[] min, final int[] max, final int[] step, final int resX,
		final int resY, final boolean[] range)
	{
		super(parent, name, ((ImageTransform) parent).getMicronWidth(),
			((ImageTransform) parent).getMicronHeight(), ((ImageTransform) parent)
				.getMicronStep());
		this.min = min;
		this.max = max;
		this.step = step;
		this.resX = resX;
		this.resY = resY;
		this.range = range;
		initState(null);
	}

	// -- DataSampling API methods --

	/** Assigns the parameters for this data sampling. */
	public void setParameters(final int[] min, final int[] max, final int[] step,
		final int resX, final int resY, final boolean[] range)
	{
		this.min = min;
		this.max = max;
		this.step = step;
		this.resX = resX;
		this.resY = resY;
		this.range = range;

		// recompute range counter
		numRange = 0;
		for (int i = 0; i < range.length; i++)
			if (range[i]) numRange++;

		// recompute lengths
		for (int i = 0; i < min.length; i++) {
			lengths[i] = (max[i] - min[i]) / step[i] + 1;
		}
		makeLabels();

		// signal parameter change to listeners
		notifyListeners(new TransformEvent(this));
	}

	/** Gets minimum dimensional index values. */
	public int[] getMin() {
		return min;
	}

	/** Gets maximum dimensional index values. */
	public int[] getMax() {
		return max;
	}

	/** Gets dimensional index step values. */
	public int[] getStep() {
		return step;
	}

	/** Gets included range components. */
	public boolean[] getRange() {
		return range;
	}

	// -- ImageTransform API methods --

	/** Gets width of each image. */
	@Override
	public int getImageWidth() {
		return resX;
	}

	/** Gets height of each image. */
	@Override
	public int getImageHeight() {
		return resY;
	}

	/** Gets number of range components at each pixel. */
	@Override
	public int getRangeCount() {
		return numRange;
	}

	/** Obtains an image from the source(s) at the given dimensional position. */
	@Override
	public BufferedImage getImage(final int[] pos) {
		final int[] p = new int[pos.length];
		for (int i = 0; i < p.length; i++)
			p[i] = min[i] + step[i] * pos[i] - 1;
		final ImageTransform it = (ImageTransform) parent;
		final BufferedImage img = it.getImage(p);
		if (img == null) return null;
		final int w = resX > 0 ? resX : img.getWidth();
		final int h = resY > 0 ? resY : img.getHeight();
		return AWTImageTools.scale(img, w, h, false);
	}

	/**
	 * Gets the physical distance between image planes in microns for the given
	 * axis.
	 */
	@Override
	public double getMicronStep(final int axis) {
		final int q = axis < 0 ? 1 : step[axis];
		return q * super.getMicronStep(axis);
	}

	// -- Static DataTransform API methods --

	/** Creates a new data sampling, with user interaction. */
	public static DataTransform makeTransform(final DataManager dm) {
		final DataTransform data = dm.getSelectedData();
		if (!isValidParent(data)) return null;
		final String n =
			(String) JOptionPane.showInputDialog(dm.getControls(), "Sampling name:",
				"Create sampling", JOptionPane.INFORMATION_MESSAGE, null, null, data
					.getName() +
					" sampling");
		if (n == null) return null;

		// guess at some reasonable defaults
		final int[] len = data.getLengths();
		final int[] lo = new int[len.length];
		final int[] hi = new int[len.length];
		final int[] inc = new int[len.length];
		for (int i = 0; i < len.length; i++) {
			lo[i] = 1;
			hi[i] = len[i];
			inc[i] = 1;
		}
		final ImageTransform it = (ImageTransform) data;
		final int w = it.getImageWidth();
		final int h = it.getImageHeight();
		final boolean[] rng = new boolean[it.getRangeCount()];
		for (int i = 0; i < rng.length; i++)
			rng[i] = true;

		return new DataSampling(data, n, lo, hi, inc, w, h, rng);
	}

	/**
	 * Indicates whether this transform type would accept the given transform as
	 * its parent transform.
	 */
	public static boolean isValidParent(final DataTransform data) {
		return data != null && data instanceof ImageTransform;
	}

	/** Indicates whether this transform type requires a parent transform. */
	public static boolean isParentRequired() {
		return true;
	}

	// -- DataTransform API methods --

	/** Gets whether this transform provides data of the given dimensionality. */
	@Override
	public boolean isValidDimension(final int dim) {
		return dim == 2 && parent.isValidDimension(dim);
	}

	/**
	 * Gets a string id uniquely describing this data transform at the given
	 * dimensional position, for the purposes of thumbnail caching. If global flag
	 * is true, the id is suitable for use in the default, global cache file.
	 */
	@Override
	public String getCacheId(final int[] pos, final boolean global) {
		final StringBuffer sb = new StringBuffer(parent.getCacheId(pos, global));
		sb.append("{");
		appendArray(sb, "min", min, true);
		appendArray(sb, "max", max, true);
		appendArray(sb, "step", step, true);
		sb.append("x=" + resX + ";");
		sb.append("y=" + resY + ";");
		final int[] r = new int[range.length];
		for (int i = 0; i < r.length; i++)
			r[i] = range[i] ? 1 : 0;
		appendArray(sb, "range", r, false);
		sb.append("}");
		return sb.toString();
	}

	/** Gets associated GUI controls for this transform. */
	@Override
	public JComponent getControls() {
		return controls;
	}

	/** Gets a description of this transform, with HTML markup. */
	@Override
	public String getHTMLDescription() {
		final StringBuffer sb = new StringBuffer();
		final int[] len = getLengths();
		final String[] dimTypes = getDimTypes();

		final int width = getImageWidth();
		final int height = getImageHeight();
		final int rangeCount = getRangeCount();

		// name
		sb.append(getName());
		sb.append("<p>\n\n");

		// list of dimensional axes
		sb.append("Dimensionality: ");
		sb.append(len.length + 2);
		sb.append("D\n");
		sb.append("<ul>\n");
		BigInteger images = BigInteger.ONE;
		if (len.length > 0) {
			for (int i = 0; i < len.length; i++) {
				images = images.multiply(new BigInteger("" + len[i]));
				sb.append("<li>");
				sb.append(len[i]);
				sb.append(" ");
				sb.append(getUnitDescription(dimTypes[i], len[i]));
				sb.append(" (");
				sb.append(min[i]);
				sb.append(" to ");
				sb.append(max[i]);
				if (step[i] != 1) {
					sb.append(" step ");
					sb.append(step[i]);
				}
				sb.append(")</li>\n");
			}
		}

		// image resolution
		sb.append("<li>");
		sb.append(width);
		sb.append(" x ");
		sb.append(height);
		sb.append(" pixel");
		if (width * height != 1) sb.append("s");

		// physical width and height in microns
		if (micronWidth == micronWidth && micronHeight == micronHeight) {
			sb.append(" (");
			sb.append(micronWidth);
			sb.append(" x ");
			sb.append(micronHeight);
			sb.append(" " + MU + ")");
		}
		sb.append("</li>\n");

		// physical distance between slices in microns
		if (micronStep == micronStep) {
			sb.append("<li>");
			sb.append(micronStep);
			sb.append(" " + MU + " between slices</li>\n");
		}

		// range component count
		sb.append("<li>");
		sb.append(rangeCount);
		sb.append(" of ");
		sb.append(range.length);
		sb.append(" range component");
		if (range.length != 1) sb.append("s");
		sb.append("</li>\n");
		sb.append("</ul>\n");

		// image and pixel counts
		BigInteger pixels = images.multiply(new BigInteger("" + width));
		pixels = pixels.multiply(new BigInteger("" + height));
		pixels = pixels.multiply(new BigInteger("" + rangeCount));
		sb.append(images);
		sb.append(" image");
		if (!images.equals(BigInteger.ONE)) sb.append("s");
		sb.append(" totaling ");
		sb.append(MathUtil.getValueWithUnit(pixels, 2));
		sb.append("pixel");
		if (!pixels.equals(BigInteger.ONE)) sb.append("s");
		sb.append(".<p>\n");

		return sb.toString();
	}

	// -- Internal DataTransform API methods --

	/** Creates labels based on data transform parameters. */
	@Override
	protected void makeLabels() {
		labels = new String[lengths.length][];
		for (int i = 0; i < lengths.length; i++) {
			labels[i] = new String[lengths[i]];
			for (int j = 0; j < lengths[i]; j++) {
				labels[i][j] = "" + (min[i] + j * step[i]);
			}
		}
	}

	// -- Dynamic API methods --

	/** Tests whether two dynamic objects are equivalent. */
	@Override
	public boolean matches(final Dynamic dyn) {
		if (!super.matches(dyn) || !isCompatible(dyn)) return false;
		final DataSampling data = (DataSampling) dyn;

		return ObjectUtil.arraysEqual(min, data.min) &&
			ObjectUtil.arraysEqual(max, data.max) &&
			ObjectUtil.arraysEqual(step, data.step) && resX == data.resX &&
			resY == data.resY && ObjectUtil.arraysEqual(range, data.range);
	}

	/**
	 * Tests whether the given dynamic object can be used as an argument to
	 * initState, for initializing this dynamic object.
	 */
	@Override
	public boolean isCompatible(final Dynamic dyn) {
		return dyn instanceof DataSampling;
	}

	/**
	 * Modifies this object's state to match that of the given object. If the
	 * argument is null, the object is initialized according to its current state
	 * instead.
	 */
	@Override
	public void initState(final Dynamic dyn) {
		if (dyn != null && !isCompatible(dyn)) return;
		super.initState(dyn);
		final DataSampling data = (DataSampling) dyn;

		if (data != null) {
			min = data.min;
			max = data.max;
			step = data.step;
			resX = data.resX;
			resY = data.resY;
			range = data.range;
		}

		lengths = new int[min.length];
		dims = (String[]) ObjectUtil.copy(parent.dims);

		setParameters(min, max, step, resX, resY, range);

		controls = new SamplingWidget(this);
		thumbs = makeThumbnailHandler();
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("DataTransforms"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element child = XMLUtil.createChild(el, "DataSampling");
		super.saveState(child);
		child.setAttribute("min", ObjectUtil.arrayToString(min));
		child.setAttribute("max", ObjectUtil.arrayToString(max));
		child.setAttribute("step", ObjectUtil.arrayToString(step));
		child.setAttribute("resX", "" + resX);
		child.setAttribute("resY", "" + resY);
		child.setAttribute("range", ObjectUtil.arrayToString(range));
	}

	/**
	 * Restores the current state from the given DOM element ("DataSampling").
	 */
	@Override
	public void restoreState(final Element el) throws SaveException {
		super.restoreState(el);
		min = ObjectUtil.stringToIntArray(el.getAttribute("min"));
		max = ObjectUtil.stringToIntArray(el.getAttribute("max"));
		step = ObjectUtil.stringToIntArray(el.getAttribute("step"));
		resX = Integer.parseInt(el.getAttribute("resX"));
		resY = Integer.parseInt(el.getAttribute("resY"));
		range = ObjectUtil.stringToBooleanArray(el.getAttribute("range"));
	}

	// -- Helper methods --

	/** Creates custom handler for data sampling's thumbnails. */
	private ThumbnailHandler makeThumbnailHandler() {
		return new SamplingThumbHandler(this, getCacheFilename());
	}

	/** Chooses filename for thumbnail cache based on parent cache's name. */
	private String getCacheFilename() {
		ThumbnailCache cache = null;
		final ThumbnailHandler th = parent.getThumbHandler();
		if (th != null) cache = th.getCache();
		if (cache == null) return "data_sampling.visbio";
		String s = cache.getCacheFile().getAbsolutePath();
		final int dot = s.lastIndexOf(".");
		String suffix;
		if (dot < 0) suffix = "";
		else {
			suffix = s.substring(dot);
			s = s.substring(0, dot);
		}
		return s + "_sampling" + suffix;
	}

	/** Appends the given array to the specified string buffer. */
	private void appendArray(final StringBuffer sb, final String name,
		final int[] array, final boolean semicolon)
	{
		sb.append(name);
		sb.append("=[");
		for (int i = 0; i < array.length; i++) {
			if (i > 0) sb.append(",");
			sb.append(array[i]);
		}
		sb.append("]");
		if (semicolon) sb.append(";");
	}

}
