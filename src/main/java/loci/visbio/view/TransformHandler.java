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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Font;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import loci.visbio.VisBioFrame;
import loci.visbio.data.DataCache;
import loci.visbio.data.DataTransform;
import loci.visbio.data.ImageTransform;
import loci.visbio.state.SaveException;
import loci.visbio.state.Saveable;
import loci.visbio.state.StateManager;
import loci.visbio.util.DisplayUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

import visad.Display;
import visad.DisplayImpl;
import visad.DisplayRealType;
import visad.ScalarMap;
import visad.TextControl;
import visad.VisADException;
import visad.java2d.DisplayImplJ2D;

/**
 * Provides logic for linking data transforms to a display.
 */
public class TransformHandler implements ChangeListener, Runnable, Saveable {

	// -- Constant --

	/** Starting burn-in delay in milliseconds. */
	public static final long DEFAULT_BURN_DELAY = 3000;

	/** Minimum amount of time to delay burn-in. */
	public static final long MINIMUM_BURN_DELAY = 0;

	/** Starting FPS for animation. */
	public static final int DEFAULT_ANIMATION_RATE = 10;

	// -- Fields --

	/** Associated display window. */
	protected DisplayWindow window;

	/** GUI controls for transform handler. */
	protected TransformPanel panel;

	/** Cache of full-resolution data in memory. */
	protected DataCache cache;

	/** Data transform links. */
	protected Vector links;

	/** Dimensional slider widgets for linked transforms. */
	protected Vector sliders;

	/** Panel containing dimensional slider widgets. */
	protected JPanel sliderPanel;

	/** Default burn-in delay in milliseconds. */
	protected long burnDelay;

	/** Flag indicating status of animation. */
	protected boolean animating;

	/** Animation rate. */
	protected int fps;

	/** Dimensional axis to use for animation. */
	protected int animAxis;

	/** Thread responsible for animation. */
	protected Thread animThread;

	/** Synchronization object for animation. */
	protected Object animSync = new Object();

	// -- Fields - initial state --

	/** List of uninitialized links. */
	protected Vector newLinks;

	// -- Constructor --

	/** Creates a display transform handler. */
	public TransformHandler(final DisplayWindow dw) {
		window = dw;
		cache = new DataCache();
		links = new Vector();
		sliders = new Vector();
		sliderPanel = new JPanel();
		burnDelay = DEFAULT_BURN_DELAY;
		fps = DEFAULT_ANIMATION_RATE;
		makePanel();
	}

	// -- TransformHandler API methods --

	/** Links the given data transform to the display. */
	public void addTransform(final DataTransform trans) {
		links.add(new TransformLink(this, trans));
		rebuild(links.size() == 1);
		panel.addTransform(trans);
	}

	/** Removes the given data transform from the display. */
	public void removeTransform(final DataTransform trans) {
		final TransformLink link = getLink(trans);
		if (link != null) {
			links.remove(link);
			link.destroy();
		}
		panel.removeTransform(trans);
		rebuild(false);
	}

	/** Unlinks all data transforms from the display. */
	public void removeAllTransforms() {
		links.removeAllElements();
		panel.removeAllTransforms();
		rebuild(false);
	}

	/** Moves the given transform up in the Z-order. */
	public void moveTransformUp(final DataTransform trans) {
		final int index = getLinkIndex(trans);
		if (index >= 0) {
			final TransformLink link = (TransformLink) links.elementAt(index);
			links.removeElementAt(index);
			links.insertElementAt(link, index - 1);
		}
		doLinks(index - 1, true);
		panel.moveTransformUp(trans);
	}

	/** Moves the given transform down in the Z-order. */
	public void moveTransformDown(final DataTransform trans) {
		final int index = getLinkIndex(trans);
		if (index >= 0) {
			final TransformLink link = (TransformLink) links.elementAt(index);
			links.removeElementAt(index);
			links.insertElementAt(link, index + 1);
		}
		doLinks(index, true);
		panel.moveTransformDown(trans);
	}

	/** Gets whether the given transform is currently linked to the display. */
	public boolean hasTransform(final DataTransform trans) {
		return panel.hasTransform(trans);
	}

	/** Gets data transforms linked to the display. */
	public DataTransform[] getTransforms() {
		final DataTransform[] dt = new DataTransform[links.size()];
		for (int i = 0; i < links.size(); i++) {
			final TransformLink link = (TransformLink) links.elementAt(i);
			dt[i] = link.getTransform();
		}
		return dt;
	}

	/** Gets number of data transforms linked to the display. */
	public int getTransformCount() {
		return links.size();
	}

	/** Gets cache of full-resolution data in memory. */
	public DataCache getCache() {
		return cache;
	}

	/** Gets associated display window. */
	public DisplayWindow getWindow() {
		return window;
	}

	/** Gets GUI controls for this transform handler. */
	public TransformPanel getPanel() {
		return panel;
	}

	/** Gets a panel containing sliders widgets for linked transforms. */
	public JPanel getSliderPanel() {
		return sliderPanel;
	}

	/** Gets the slider at the given index, or null if no such slider exists. */
	public BioSlideWidget getSlider(final int index) {
		if (index < 0 || index >= sliders.size()) return null;
		return (BioSlideWidget) sliders.elementAt(index);
	}

	/**
	 * Gets the dimensional position specified by the given transform's slider
	 * widgets.
	 */
	public int[] getPos(final DataTransform trans) {
		final int[] pos = new int[trans.getLengths().length];
		Arrays.fill(pos, -1);
		for (int s = 0; s < sliders.size(); s++) {
			final BioSlideWidget bsw = (BioSlideWidget) sliders.elementAt(s);
			final int axis = getAxis(trans, s);
			if (axis >= 0) pos[axis] = bsw.getValue();
		}
		return pos;
	}

	/** Sets the delay in milliseconds before full-resolution burn-in occurs. */
	public void setBurnDelay(long delay) {
		if (delay < MINIMUM_BURN_DELAY) delay = MINIMUM_BURN_DELAY;
		burnDelay = delay;
	}

	/** Gets the delay in milliseconds before full-resolution burn-in occurs. */
	public long getBurnDelay() {
		return burnDelay;
	}

	/** Toggles animation of the display. */
	public void setAnimating(final boolean on) {
		if (animating == on) return;
		animating = on;
		if (!animating) return;
		startAnimation();
	}

	/** Gets whether display is currently animating. */
	public boolean isAnimating() {
		return animating;
	}

	/** Sets animation rate. */
	public void setAnimationRate(final int fps) {
		synchronized (animSync) {
			this.fps = fps;
		}
		final VisBioFrame bio = window.getVisBio();
		bio.generateEvent(bio.getManager(DisplayManager.class),
			"change animation rate for " + window.getName(), true);
	}

	/** Gets animation rate. */
	public int getAnimationRate() {
		return fps;
	}

	/** Assigns the given axis as the animation axis. */
	public void setAnimationAxis(final int animAxis) {
		synchronized (animSync) {
			this.animAxis = animAxis;
		}
		final VisBioFrame bio = window.getVisBio();
		bio.generateEvent(bio.getManager(DisplayManager.class),
			"change animation axis for " + window.getName(), true);
	}

	/** Gets the currently assigned animation axis. */
	public int getAnimationAxis() {
		return animAxis;
	}

	/** Gets the transform link object for the given data transform. */
	public TransformLink getLink(final DataTransform trans) {
		for (int i = 0; i < links.size(); i++) {
			final TransformLink link = (TransformLink) links.elementAt(i);
			if (link.getTransform() == trans) return link;
		}
		return null;
	}

	/** Gets the transform link index for the given data transform. */
	public int getLinkIndex(final DataTransform trans) {
		for (int i = 0; i < links.size(); i++) {
			final TransformLink link = (TransformLink) links.elementAt(i);
			if (link.getTransform() == trans) return i;
		}
		return -1;
	}

	/**
	 * Identifies the dimensional axis of the given transform to which the
	 * specified slider axis corresponds, or -1 if none.
	 */
	public int getAxis(final DataTransform trans, final int axis) {
		if (axis < 0 || axis >= sliders.size()) return -1;
		final BioSlideWidget bsw = (BioSlideWidget) sliders.elementAt(axis);
		final DataTransform[] dt = bsw.getTransforms();
		final int[] ndx = bsw.getIndices();
		for (int t = 0; t < dt.length; t++) {
			if (trans == dt[t]) return ndx[t];
		}
		return -1;
	}

	// -- TransformHandler API methods - state logic --

	/** Tests whether two objects are in equivalent states. */
	public boolean matches(final TransformHandler handler) {
		if (handler == null) return false;
		final int size = links.size();
		if (handler.links == null || handler.links.size() != size) return false;
		for (int i = 0; i < size; i++) {
			final TransformLink link = (TransformLink) links.elementAt(i);
			final TransformLink hlink = (TransformLink) handler.links.elementAt(i);
			if (link == null && hlink == null) continue;
			if (link == null || hlink == null || !link.matches(hlink)) return false;
		}
		return animating == handler.animating && fps == handler.fps &&
			animAxis == handler.animAxis;
	}

	/**
	 * Modifies this object's state to match that of the given object. If the
	 * argument is null, the object is initialized according to its current state
	 * instead.
	 */
	public void initState(final TransformHandler handler) {
		if (handler == null) {
			if (newLinks != null) {
				// initialize new links
				StateManager.mergeStates(links, newLinks);
				links = newLinks;
				newLinks = null;
			}
		}
		else {
			// merge handler links with current links
			final Vector vn =
				handler.newLinks == null ? handler.links : handler.newLinks;
			StateManager.mergeStates(links, vn);
			links = vn;
			animating = handler.animating;
			fps = handler.fps;
			animAxis = handler.animAxis;
		}

		panel.removeAllTransforms();
		final int size = links.size();
		for (int i = 0; i < size; i++) {
			final TransformLink link = (TransformLink) links.elementAt(i);
			panel.addTransform(link.getTransform());
		}

		rebuild(false);
		if (animating) startAnimation();
	}

	// -- Internal TransformHandler API methods --

	/** Constructs GUI controls for the transform handler. */
	protected void makePanel() {
		panel = new TransformPanel(this);
	}

	/** Notifies the transform panel of a new dimensional axis. */
	protected void addAxis(final String axis) {
		panel.addAxis(axis);
	}

	/** Adds any required custom mappings to the display. */
	protected void doCustomMaps() throws VisADException, RemoteException {}

	/** Links in the transform links, starting at the given index. */
	protected void doLinks(final int startIndex, final boolean unlinkFirst) {
		final DisplayImpl display = window.getDisplay();
		DisplayUtil.setDisplayDisabled(display, true);

		final int size = links.size();
		if (unlinkFirst) {
			for (int l = startIndex; l < size; l++) {
				((TransformLink) links.elementAt(l)).unlink();
			}
		}

		for (int l = startIndex; l < size; l++) {
			((TransformLink) links.elementAt(l)).link();
		}
		for (int l = startIndex; l < size; l++) {
			((TransformLink) links.elementAt(l)).doTransform();
		}

		DisplayUtil.setDisplayDisabled(display, false);
	}

	/** Rebuilds sliders and display mappings for all linked transforms. */
	protected void rebuild(final boolean resetColors) {
		synchronized (animSync) {
			final TransformLink[] lnk = new TransformLink[links.size()];
			links.copyInto(lnk);

			// HACK - temporary code to fix color refresh bug
			for (int i = 0; i < lnk.length; i++) {
				final ColorHandler colorHandler = lnk[i].getColorHandler();
				if (colorHandler != null) {
					colorHandler.colorTables = colorHandler.getTables();
				}
			}

			// clear old transforms
			final DisplayImpl display = window.getDisplay();
			try {
				DisplayUtil.setDisplayDisabled(display, true);
				display.removeAllReferences();
				display.clearMaps();
			}
			catch (final VisADException exc) {
				exc.printStackTrace();
			}
			catch (final RemoteException exc) {
				exc.printStackTrace();
			}
			sliders.removeAllElements();
			panel.removeAllAxes();

			// rebuild dimensional sliders and mappings list
			final Vector mapList = new Vector(), mapTrans = new Vector();
			for (int l = 0; l < lnk.length; l++) {
				final DataTransform trans = lnk[l].getTransform();
				final String[] types = trans.getDimTypes();
				for (int ndx = 0; ndx < types.length; ndx++) {
					boolean success = false;
					for (int s = 0; s < sliders.size(); s++) {
						final BioSlideWidget bsw = (BioSlideWidget) sliders.elementAt(s);
						success = bsw.addTransform(trans, ndx);
						if (success) break;
					}
					if (!success) {
						// create new slider to accommodate incompatible dimensional axis
						final BioSlideWidget bsw = new BioSlideWidget(trans, ndx);
						bsw.getSlider().addChangeListener(this);
						sliders.add(bsw);
						addAxis(types[ndx]);
					}
				}
				final ScalarMap[] maps = trans.getSuggestedMaps();
				for (int m = 0; m < maps.length; m++) {
					if (!mapList.contains(maps[m])) {
						mapList.add(maps[m]);
						mapTrans.add(trans); // save first transform that needs this map
					}
				}
			}

			// reconstruct display mappings
			try {
				for (int i = 0; i < mapList.size(); i++) {
					final ScalarMap map = (ScalarMap) mapList.elementAt(i);
					final DisplayRealType drt = map.getDisplayScalar();
					boolean mappingOk = true;
					if (!window.is3D()) {
						// 2D displays do not support every type of mapping
						if (drt.equals(Display.ZAxis)) mappingOk = false;
					}
					if (display instanceof DisplayImplJ2D) {
						// Java2D does not support every type of mapping
						if (drt.equals(Display.Alpha)) mappingOk = false;
						// NB: Display.RGBA not currently handled
					}
					if (mappingOk) display.addMap(map);

					// configure map's controls according to transform settings;
					// if multiple transforms have the same map, but different
					// settings, the first transform's settings take precedence
					final DataTransform trans = (DataTransform) mapTrans.elementAt(i);
					if (map.getDisplayScalar().equals(Display.Text)) {
						// HACK - always use font size 8, since it renders faster
						// and has virtually no effect on rendering size
						Font font = trans.getFont();
						if (font != null) {
							font = new Font(font.getName(), font.getStyle(), 8);
						}

						// for Text maps, configure font
						final TextControl textControl = (TextControl) map.getControl();
						if (textControl != null) textControl.setFont(font);
					}
					else if (map.getDisplayScalar().equals(Display.XAxis)) {
						// fix X range according to first transform (if it is an image)
						if (trans instanceof ImageTransform) {
							final ImageTransform it = (ImageTransform) trans;
							final double mw = it.getMicronWidth();
							map.setRange(0, mw == mw ? mw : it.getImageWidth() - 1);
						}
					}
					else if (map.getDisplayScalar().equals(Display.YAxis)) {
						// fix Y range according to first transform (if it is an image)
						if (trans instanceof ImageTransform) {
							final ImageTransform it = (ImageTransform) trans;
							final double mh = it.getMicronHeight();
							map.setRange(0, mh == mh ? mh : it.getImageHeight() - 1);
						}
					}
				}
				doCustomMaps();
				doLinks(0, false);
				DisplayUtil.setDisplayDisabled(display, false);
			}
			catch (final VisADException exc) {
				exc.printStackTrace();
			}
			catch (final RemoteException exc) {
				exc.printStackTrace();
			}

			// rebuild slider panel
			final int size = sliders.size();
			if (size == 0) sliderPanel = new JPanel();
			else {
				final StringBuffer sb = new StringBuffer("pref");
				for (int i = 1; i < size; i++)
					sb.append(", 3dlu, pref");

				final PanelBuilder builder =
					new PanelBuilder(new FormLayout("pref:grow", sb.toString()));
				final CellConstraints cc = new CellConstraints();
				for (int i = 0; i < size; i++) {
					final BioSlideWidget bsw = (BioSlideWidget) sliders.elementAt(i);
					builder.add(bsw, cc.xy(1, 2 * i + 1));
				}
				sliderPanel = builder.getPanel();
			}

			// update GUI to reflect new dimensional position
			panel.updateControls();

			// reinitialize colors
			final StateManager sm =
				(StateManager) window.getVisBio().getManager(StateManager.class);
			final boolean reset = resetColors && !sm.isRestoring();
			for (int i = 0; i < lnk.length; i++) {
				final ColorHandler colorHandler = lnk[i].getColorHandler();
				if (colorHandler != null) colorHandler.initColors(reset);
			}

			if (lnk.length > 0) window.repack();
			else window.setVisible(false);
		}
	}

	/** Starts a new thread for animation. */
	protected void startAnimation() {
		if (animThread != null) {
			try {
				animThread.join();
			}
			catch (final InterruptedException exc) {}
		}
		animThread = new Thread(this, "VisBio-AnimThread-" + window.getName());
		animThread.start();
	}

	// -- ChangeListener API methods --

	/** Handles slider updates. */
	@Override
	public void stateChanged(final ChangeEvent e) {
		final Object src = e.getSource();
		DataTransform[] trans = null;
		for (int s = 0; s < sliders.size(); s++) {
			final BioSlideWidget bsw = (BioSlideWidget) sliders.elementAt(s);
			final JSlider slider = bsw.getSlider();
			if (src == slider) {
				trans = bsw.getTransforms();
				break;
			}
		}
		for (int t = 0; t < trans.length; t++) {
			final TransformLink link = getLink(trans[t]);
			link.doTransform();
		}

		// update GUI to reflect new dimensional position
		panel.updateControls();
	}

	// -- Runnable API methods --

	/** Animates the display. */
	@Override
	public void run() {
		while (animating) {
			long waitTime;
			synchronized (animSync) {
				final long start = System.currentTimeMillis();
				if (animAxis >= 0) {
					((BioSlideWidget) sliders.elementAt(animAxis)).step(true);
				}
				final long end = System.currentTimeMillis();
				waitTime = 1000 / fps - end + start;
			}
			if (waitTime >= 0) {
				try {
					Thread.sleep(waitTime);
				}
				catch (final InterruptedException exc) {
					exc.printStackTrace();
				}
			}
		}
		for (int l = 0; l < links.size(); l++) {
			final TransformLink link = (TransformLink) links.elementAt(l);
			link.doTransform();
		}
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("Display"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element child = XMLUtil.createChild(el, "LinkedData");

		// save links
		final int len = links.size();
		for (int i = 0; i < len; i++) {
			final TransformLink link = (TransformLink) links.elementAt(i);
			link.saveState(child);
		}

		// save other parameters
		child.setAttribute("animating", "" + animating);
		child.setAttribute("FPS", "" + fps);
		child.setAttribute("animationAxis", "" + animAxis);
	}

	/** Restores the current state from the given DOM element ("Display"). */
	@Override
	public void restoreState(final Element el) throws SaveException {
		final Element child = XMLUtil.getFirstChild(el, "LinkedData");

		// restore links
		final Element[] els = XMLUtil.getChildren(child, "TransformLink");
		newLinks = new Vector();
		for (int i = 0; i < els.length; i++) {
			final TransformLink link = new TransformLink(this);
			link.restoreState(els[i]);
			newLinks.add(link);
		}

		// restore other parameters
		animating = child.getAttribute("animating").equalsIgnoreCase("true");
		fps = Integer.parseInt(child.getAttribute("FPS"));
		animAxis = Integer.parseInt(child.getAttribute("animationAxis"));
	}

}
