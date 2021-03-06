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

import com.jgoodies.looks.LookUtils;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.rmi.RemoteException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import loci.common.ReflectException;
import loci.common.ReflectedUniverse;
import loci.visbio.VisBioFrame;
import loci.visbio.WindowManager;
import loci.visbio.data.DataTransform;
import loci.visbio.state.BooleanOption;
import loci.visbio.state.Dynamic;
import loci.visbio.state.OptionManager;
import loci.visbio.state.SaveException;
import loci.visbio.state.Saveable;
import loci.visbio.util.BreakawayPanel;
import loci.visbio.util.DisplayUtil;
import loci.visbio.util.FormsUtil;
import loci.visbio.util.ObjectUtil;
import loci.visbio.util.SwingUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

import visad.DisplayEvent;
import visad.DisplayImpl;
import visad.DisplayListener;
import visad.GraphicsModeControl;
import visad.VisADException;

/**
 * DisplayWindow is a window containing a 2D or 3D VisAD display and associated
 * controls.
 */
public class DisplayWindow extends JFrame implements ActionListener,
	DisplayListener, Dynamic, KeyListener, Saveable
{

	// -- Static fields --

	/** Stereo graphics configuration. */
	protected static final GraphicsConfiguration STEREO = DisplayUtil
		.getStereoConfiguration();

	// -- Fields --

	/** Name of this display. */
	protected String name;

	/** True if this display is 3D, false if 2D. */
	protected boolean threeD;

	// -- Fields - handlers --

	/** Handles logic for controlling the VisAD display's view. */
	protected ViewHandler viewHandler;

	/** Handles logic for capturing the display screenshots and movies. */
	protected CaptureHandler captureHandler;

	/** Handles logic for linking data transforms to the VisAD display. */
	protected TransformHandler transformHandler;

	// -- Fields - GUI components --

	/** Associated display manager. */
	protected DisplayManager manager;

	/** Associated VisAD display. */
	protected DisplayImpl display;

	/** Panel containing dimensional slider widgets. */
	protected JPanel sliders;

	/** Breakaway panel for display controls. */
	protected BreakawayPanel controls;

	// -- Fields - initial state --

	/** Initial edge of breakaway panel. */
	protected String initialEdge;

	// -- Fields - other --

	/** String representation of this display. */
	protected String string;

	// -- Constructors --

	/** Creates an uninitialized display object. */
	public DisplayWindow() {}

	/** Creates an uninitialized display object. */
	public DisplayWindow(final DisplayManager dm) {
		super();
		manager = dm;
	}

	/** Creates a new display object according to the given parameters. */
	public DisplayWindow(final DisplayManager dm, final String name,
		final boolean threeD)
	{
		super(name);
		manager = dm;
		this.name = name;
		this.threeD = threeD;
		initState(null);
	}

	// -- DisplayWindow API methods --

	/**
	 * Enlarges the display to its preferred width and/or height if it is too
	 * small, keeping the display itself square.
	 */
	public void repack() {
		if (sliders == null) return; // not yet fully initialized
		sliders.removeAll();
		sliders.add(transformHandler.getSliderPanel());
		if (!isVisible()) validate(); // force recomputation of slider panel size
		final Dimension size = SwingUtil.getRepackSize(this);
		final String edge = controls.getEdge();
		if (edge == BorderLayout.EAST || edge == BorderLayout.WEST) {
			size.width = controls.getPreferredSize().width + size.height - 20;
			// HACK - work around a layout issue where panel is slightly too short
			// this hack also appears in loci.visbio.util.SwingUtil.pack()
			if (LookUtils.IS_OS_LINUX) size.height += 10;
		}
		else if (edge == BorderLayout.NORTH || edge == BorderLayout.SOUTH) {
			size.height = controls.getPreferredSize().height + size.width + 20;
		}
		else controls.repack();
		final Dimension actualSize = getSize();
		if (actualSize.width > size.width) size.width = actualSize.width;
		if (actualSize.height > size.height) size.height = actualSize.height;
		setSize(size);
	}

	/** Gets associated VisBio frame. */
	public VisBioFrame getVisBio() {
		return manager.getVisBio();
	}

	/** Gets associated display manager. */
	public DisplayManager getManager() {
		return manager;
	}

	/** Gets the associated VisAD display. */
	public DisplayImpl getDisplay() {
		return display;
	}

	/** Gets associated breakaway control panel. */
	public BreakawayPanel getControls() {
		return controls;
	}

	/** Gets the view handler. */
	public ViewHandler getViewHandler() {
		return viewHandler;
	}

	/** Gets the capture handler. */
	public CaptureHandler getCaptureHandler() {
		return captureHandler;
	}

	/** Gets the transform handler. */
	public TransformHandler getTransformHandler() {
		return transformHandler;
	}

	/** Gets the name of this display. */
	@Override
	public String getName() {
		return name;
	}

	/** Gets whether this view handler's display is 3D. */
	public boolean is3D() {
		return threeD;
	}

	/** Links the given data transform to the display. */
	public void addTransform(final DataTransform trans) {
		transformHandler.addTransform(trans);
		viewHandler.guessAspect();
		refresh();
		getVisBio().generateEvent(manager, "add data object to display", true);
	}

	/** Removes the given data transform from the display. */
	public void removeTransform(final DataTransform trans) {
		transformHandler.removeTransform(trans);
		refresh();
		getVisBio().generateEvent(manager, "remove data object from display", true);
	}

	/** Unlinks all data transforms from the display. */
	public void removeAllTransforms() {
		transformHandler.removeAllTransforms();
		refresh();
		getVisBio().generateEvent(manager, "remove all data objects from display",
			true);
	}

	/** Gets whether the given transform is currently linked to the display. */
	public boolean hasTransform(final DataTransform trans) {
		return transformHandler.hasTransform(trans);
	}

	/** Sets whether transparency mode is nicest vs fastest. */
	public void setTransparencyMode(final boolean nice) {
		final ReflectedUniverse r = new ReflectedUniverse();
		try {
			r.exec("import visad.java3d.DisplayImplJ3D");
			final int nicest =
				((Integer) r.getVar("DisplayImplJ3D.NICEST")).intValue();
			final int fastest =
				((Integer) r.getVar("DisplayImplJ3D.FASTEST")).intValue();
			final GraphicsModeControl gmc = display.getGraphicsModeControl();
			if (gmc.getClass().getName()
				.equals("visad.java3d.GraphicsModeControlJ3D"))
			{
				gmc.setTransparencyMode(nice ? nicest : fastest);
			}
		}
		catch (final ReflectException exc) {
			System.err.println("Warning: transparency mode setting (nice=" + nice +
				") will have no effect. Java3D is probably not installed.");
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
	}

	/** Sets wehther texture mapping is enabled. */
	public void setTextureMapping(final boolean textureMapping) {
		try {
			display.getGraphicsModeControl().setTextureEnable(textureMapping);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
	}

	/** Sets whether volume rendering uses 3D texturing. */
	public void set3DTexturing(final boolean texture3d) {
		try {
			display.getGraphicsModeControl()
				.setTexture3DMode(
					texture3d ? GraphicsModeControl.TEXTURE3D
						: GraphicsModeControl.STACK2D);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
	}

	// -- Component API methods --

	/** Shows or hides this window. */
	@Override
	public void setVisible(final boolean b) {
		super.setVisible(b);
		if (b) controls.reshow();
	}

	// -- Object API methods --

	/** Gets a string representation of this display (just its name). */
	@Override
	public String toString() {
		return string;
	}

	// -- ActionListener API methods --

	/** Handles button presses. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final String cmd = e.getActionCommand();
	}

	// -- DisplayListener API methods --

	/** Listens for keyboard presses within the display. */
	@Override
	public void displayChanged(final DisplayEvent e) {
		final int id = e.getId();
		if (id == DisplayEvent.KEY_PRESSED) {
			keyPressed((KeyEvent) e.getInputEvent());
		}
		else if (id == DisplayEvent.KEY_RELEASED) {
			keyReleased((KeyEvent) e.getInputEvent());
		}
	}

	// -- Dynamic API methods --

	/** Tests whether two dynamic objects are equivalent. */
	@Override
	public boolean matches(final Dynamic dyn) {
		if (!isCompatible(dyn)) return false;
		final DisplayWindow window = (DisplayWindow) dyn;

		return ObjectUtil.objectsEqual(name, window.name) &&
			viewHandler.matches(window.viewHandler) &&
			captureHandler.matches(window.captureHandler) &&
			transformHandler.matches(window.transformHandler);
	}

	/**
	 * Tests whether the given dynamic object can be used as an argument to
	 * initState, for initializing this dynamic object.
	 */
	@Override
	public boolean isCompatible(final Dynamic dyn) {
		if (!(dyn instanceof DisplayWindow)) return false;
		final DisplayWindow window = (DisplayWindow) dyn;
		return threeD == window.threeD;
	}

	/**
	 * Modifies this object's state to match that of the given object. If the
	 * argument is null, the object is initialized according to its current state
	 * instead.
	 */
	@Override
	public void initState(final Dynamic dyn) {
		if (dyn != null && !isCompatible(dyn)) return;
		final DisplayWindow window = (DisplayWindow) dyn;

		if (window != null) {
			name = window.name;
			threeD = window.threeD;
		}
		string = name + (threeD ? " (3D)" : " (2D)");

		if (display == null) {
			final OptionManager om =
				(OptionManager) getVisBio().getManager(OptionManager.class);
			final boolean doStereo =
				((BooleanOption) om.getOption(DisplayManager.DO_STEREO)).getValue();
			final GraphicsConfiguration gc = doStereo ? STEREO : null;

			om.getOption(DisplayManager.EYE_DISTANCE);
			display = DisplayUtil.makeDisplay(name, threeD, gc);
			setTransparencyMode(manager.isNiceTransparency());
			setTextureMapping(manager.isTextureMapped());
			set3DTexturing(manager.is3DTextured());
			display.addDisplayListener(this);
		}
		else display.setName(name);
		setTitle("Display - " + name);

		// handlers
		createHandlers();
		if (window == null) {
			viewHandler.initState(null);
			captureHandler.initState(null);
			transformHandler.initState(null);
		}
		else {
			// handlers' initState methods are smart enough to reinitialize
			// their components only when necessary, to ensure efficiency
			viewHandler.initState(window.viewHandler);
			captureHandler.initState(window.captureHandler);
			transformHandler.initState(window.transformHandler);
		}

		if (controls == null) {
			// display window's content pane
			final Container pane = getContentPane();
			pane.setLayout(new BorderLayout());

			// panel for dimensional sliders
			sliders = new JPanel();
			sliders.setLayout(new BorderLayout());

			// breakaway panel for display controls
			controls = new BreakawayPanel(pane, "Controls - " + name, true);
			if (initialEdge == null) initialEdge = BorderLayout.EAST;
			else if (initialEdge.equals("null")) initialEdge = null;
			controls.setEdge(initialEdge);

			// add display controls breakaway window to window manager
			final WindowManager wm =
				(WindowManager) getVisBio().getManager(WindowManager.class);
			wm.addWindow(controls.getWindow());

			// listen for key presses
			// addKeyListener(this);
			// controls.addKeyListener(this);

			// NB: Adding the KeyListener directly to frames and panels is not
			// effective, because some child always has the keyboard focus and eats
			// the event. Better would be to add the keyboard listener to each
			// component that does not need the arrow keys for its own purposes. For
			// now, the display itself must have the focus (just click it first).

			// lay out components
			pane.add(display.getComponent(), BorderLayout.CENTER);

			final JPanel viewPanel = viewHandler.getPanel();
			final JPanel capturePanel = captureHandler.getPanel();
			final JPanel transformPanel = transformHandler.getPanel();

			viewPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
			capturePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
			transformPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
			sliders.setBorder(new EmptyBorder(12, 0, 0, 0));

			final JTabbedPane tabs = new JTabbedPane();
			tabs.addTab("Appearance", viewPanel);
			tabs.addTab("Capture", capturePanel);
			tabs.addTab("Data", transformPanel);

			final Object[] rows = { tabs, sliders };
			controls.setContentPane(FormsUtil.makeColumn(rows, null, true));
			pack();
			repack();
		}
		else controls.getWindow().setTitle("Controls - " + name);
	}

	/**
	 * Called when this object is being discarded in favor of another object with
	 * a matching state.
	 */
	@Override
	public void discard() {
		getContentPane().removeAll();

		// sever ties with un-GC-able display object
		if (display != null) {
			display.removeDisplayListener(this);
			try {
				display.destroy();
			}
			catch (final VisADException exc) {
				exc.printStackTrace();
			}
			catch (final RemoteException exc) {
				exc.printStackTrace();
			}
			display = null;
		}

		// NB: Despite all of the above, ties are still not completely severed.
		// VisADCanvasJ3D maintains a parent field (from the Canvas3D superclass)
		// that points to this DisplayWindow. I am uncertain how to kill that
		// reference, so instead we cut ties to a number of objects below. That
		// way, though this DisplayWindow is not GCed, at least its constituent
		// handlers are.
		viewHandler = null;
		captureHandler = null;
		transformHandler = null;
		sliders = null;
		controls = null;
		string = null;

		// NB: And despite all of the fields nulled above, the handlers are also
		// still not GCed. I do not understand why not. Frustrating.
	}

	// -- KeyListener API methods --

	/** Handles key presses. */
	@Override
	public void keyPressed(final KeyEvent e) {
		final int code = e.getKeyCode();
		final int mods = e.getModifiers();
		if (mods != 0) return;
		if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT) {
			final int axis = transformHandler.getPanel().getLeftRightAxis();
			final BioSlideWidget bsw = transformHandler.getSlider(axis);
			if (bsw == null) return;
			if (code == KeyEvent.VK_LEFT) bsw.step(false);
			else bsw.step(true); // code == KeyEvent.VK_RIGHT
		}
		else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN) {
			final int axis = transformHandler.getPanel().getUpDownAxis();
			final BioSlideWidget bsw = transformHandler.getSlider(axis);
			if (bsw == null) return;
			if (code == KeyEvent.VK_DOWN) bsw.step(false);
			else bsw.step(true); // code == KeyEvent.VK_UP
		}
	}

	/** Handles key releases. */
	@Override
	public void keyReleased(final KeyEvent e) {}

	/** Handles key strokes. */
	@Override
	public void keyTyped(final KeyEvent e) {}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("Displays"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element child = XMLUtil.createChild(el, "Display");
		child.setAttribute("name", name);
		child.setAttribute("threeD", "" + threeD);
		child.setAttribute("edge", "" +
			(controls == null ? initialEdge : controls.getEdge()));
		viewHandler.saveState(child);
		captureHandler.saveState(child);
		transformHandler.saveState(child);
	}

	/** Restores the current state from the given DOM element ("Display"). */
	@Override
	public void restoreState(final Element el) throws SaveException {
		name = el.getAttribute("name");
		threeD = el.getAttribute("threeD").equalsIgnoreCase("true");
		initialEdge = el.getAttribute("edge");

		createHandlers();
		viewHandler.restoreState(el);
		captureHandler.restoreState(el);
		transformHandler.restoreState(el);
	}

	// -- Helper methods --

	/** Constructs logic handlers. */
	protected void createHandlers() {
		if (viewHandler == null) viewHandler = new ViewHandler(this);
		if (captureHandler == null) captureHandler = new CaptureHandler(this);
		if (transformHandler == null) {
			transformHandler =
				threeD ? new StackHandler(this) : new TransformHandler(this);
		}
	}

	/** Refreshes GUI components. */
	protected void refresh() {
		if (transformHandler.getTransformCount() == 0) setVisible(false);
		manager.getControls().refresh();
	}

	// -- Utility methods --

	/** Figures out which DisplayWindow contains the given display, if any. */
	public static DisplayWindow getDisplayWindow(final DisplayImpl d) {
		final Window w = SwingUtil.getWindow(d.getComponent());
		if (!(w instanceof DisplayWindow)) return null;
		return (DisplayWindow) w;
	}

}
