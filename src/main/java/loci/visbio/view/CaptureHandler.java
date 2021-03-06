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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import loci.formats.FormatException;
import loci.formats.gui.BufferedImageWriter;
import loci.formats.gui.ExtensionFileFilter;
import loci.visbio.SystemManager;
import loci.visbio.WindowManager;
import loci.visbio.state.SaveException;
import loci.visbio.state.Saveable;
import loci.visbio.state.StateManager;
import loci.visbio.util.ImageJUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

import visad.DisplayImpl;
import visad.ProjectionControl;
import visad.VisADException;
import visad.util.Util;

/**
 * Provides logic for capturing display screenshots and movies.
 */
public class CaptureHandler implements Saveable {

	// -- Fields - GUI components --

	/** Associated display window. */
	protected DisplayWindow window;

	/** GUI controls for capture handler. */
	protected CapturePanel panel;

	/** File chooser for snapshot output. */
	protected JFileChooser imageBox;

	/** File chooser for movie output. */
	protected JFileChooser movieBox;

	// -- Fields - initial state --

	/** List of positions. */
	protected Vector positions = new Vector();

	/** Movie speed. */
	protected int movieSpeed = 8;

	/** Movie frames per second. */
	protected int movieFPS = 10;

	/** Whether transitions use a smoothing sine function. */
	protected boolean movieSmooth = true;

	// -- Constructor --

	/** Creates a display capture handler. */
	public CaptureHandler(final DisplayWindow dw) {
		window = dw;
	}

	// -- CaptureHandler API methods --

	/** Gets positions on the list. */
	public Vector getPositions() {
		return panel == null ? positions : panel.getPositions();
	}

	/** Gets movie speed. */
	public int getSpeed() {
		return panel == null ? movieSpeed : panel.getSpeed();
	}

	/** Gets movie frames per second. */
	public int getFPS() {
		return panel == null ? movieFPS : panel.getFPS();
	}

	/** Gets whether transitions use a smoothing sine function. */
	public boolean isSmooth() {
		return panel == null ? movieSmooth : panel.isSmooth();
	}

	/** Gets associated display window. */
	public DisplayWindow getWindow() {
		return window;
	}

	/** Gets GUI controls for this capture handler. */
	public CapturePanel getPanel() {
		return panel;
	}

	/** Gets a snapshot of the display. */
	public BufferedImage getSnapshot() {
		return window.getDisplay().getImage();
	}

	/** Saves a snapshot of the display to a file specified by the user. */
	public void saveSnapshot() {
		final int rval = imageBox.showSaveDialog(window);
		if (rval != JFileChooser.APPROVE_OPTION) return;

		// determine file type
		String file = imageBox.getSelectedFile().getPath();
		String ext = "";
		final int dot = file.lastIndexOf(".");
		if (dot >= 0) ext = file.substring(dot + 1).toLowerCase();
		boolean tiff = ext.equals("tif") || ext.equals("tiff");
		boolean jpeg = ext.equals("jpg") || ext.equals("jpeg");
		final FileFilter filter = imageBox.getFileFilter();
		final String desc = filter.getDescription();
		if (desc.startsWith("JPEG")) {
			if (!jpeg) {
				file += ".jpg";
				jpeg = true;
			}
		}
		else if (desc.startsWith("TIFF")) {
			if (!tiff) {
				file += ".tif";
				tiff = true;
			}
		}
		if (!tiff && !jpeg) {
			JOptionPane.showMessageDialog(window, "Invalid filename (" + file +
				"): extension must indicate TIFF or JPEG format.",
				"Cannot export snapshot", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// save file in a separate thread
		final String id = file;
		final boolean isTiff = tiff, isJpeg = jpeg;
		new Thread("VisBio-SnapshotThread-" + window.getName()) {

			@Override
			public void run() {
				final BufferedImageWriter writer = new BufferedImageWriter();
				try {
					writer.setId(id);
					writer.savePlane(0, getSnapshot());
					writer.close();
				}
				catch (final FormatException exc) {
					exc.printStackTrace();
				}
				catch (final IOException exc) {
					exc.printStackTrace();
				}
			}
		}.start();
	}

	/** Sends a snapshot of the display to ImageJ. */
	public void sendToImageJ() {
		new Thread("VisBio-SendToImageJThread-" + window.getName()) {

			@Override
			public void run() {
				ImageJUtil.sendToImageJ(window.getName() + " snapshot", getSnapshot(),
					window.getVisBio());
			}
		}.start();
	}

	/** Creates a movie of the given transformation sequence. */
	public void captureMovie(final Vector matrices, final double secPerTrans,
		final int framesPerSec, final boolean sine, final boolean movie)
	{
		final int size = matrices.size();
		if (size < 1) {
			JOptionPane.showMessageDialog(window, "Must have at least "
				+ "two display positions on the list.", "Cannot record movie",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		final DisplayImpl d = window.getDisplay();
		if (d == null) {
			JOptionPane.showMessageDialog(window, "Display not found.",
				"Cannot record movie", JOptionPane.ERROR_MESSAGE);
			return;
		}
		final ProjectionControl pc = d.getProjectionControl();

		final int fps = framesPerSec;
		final int framesPerTrans = (int) (framesPerSec * secPerTrans);
		final int total = (size - 1) * framesPerTrans + 1;

		// get output filename(s) from the user
		String name = null;
		final boolean tiff = false, jpeg = false;
		if (movie) {
			final int rval = movieBox.showSaveDialog(window);
			if (rval != JFileChooser.APPROVE_OPTION) return;
			name = movieBox.getSelectedFile().getPath();
			if (name.indexOf(".") < 0) name += ".avi";
		}
		else {
			final int rval = imageBox.showSaveDialog(window);
			if (rval != JFileChooser.APPROVE_OPTION) return;
			name = imageBox.getSelectedFile().getPath();
			final String sel =
				((ExtensionFileFilter) imageBox.getFileFilter()).getExtension();
			String ext = "";
			final int dot = name.lastIndexOf(".");
			if (dot >= 0) ext = name.substring(dot + 1).toLowerCase();
			if (sel.equals("tif") && !ext.equals("tif")) name += ".tif";
			else if (sel.equals("jpg") && !ext.equals("jpg")) name += ".jpg";
		}

		// capture image sequence in a separate thread
		final boolean doMovie = movie;
		final String filename = name;
		final Vector pos = matrices;
		final int frm = framesPerTrans;
		final boolean doSine = sine;

		new Thread("VisBio-CaptureThread-" + window.getName()) {

			@Override
			public void run() {
				final WindowManager wm =
					(WindowManager) window.getVisBio().getManager(WindowManager.class);
				wm.setWaitCursor(true);

				setProgress(0, "Capturing movie");

				final String prefix, ext;
				int dot = filename.lastIndexOf(".");
				if (dot < 0) dot = filename.length();
				final String pre = filename.substring(0, dot);
				final String post = filename.substring(dot + 1);

				// step incrementally from position to position, grabbing images
				int count = 1;
				final BufferedImageWriter writer = new BufferedImageWriter();
				double[] mxStart = (double[]) pos.elementAt(0);
				for (int i = 1; i < size; i++) {
					final double[] mxEnd = (double[]) pos.elementAt(i);
					final double[] mx = new double[mxStart.length];
					for (int j = 0; j < frm; j++) {
						setProgress(100 * (count - 1) / total, "Saving image " + count +
							"/" + total);
						double p = (double) j / frm;
						if (doSine) p = sine(p);
						for (int k = 0; k < mx.length; k++) {
							mx[k] = p * (mxEnd[k] - mxStart[k]) + mxStart[k];
						}
						final BufferedImage image = captureImage(pc, mx, d);
						final String name = doMovie ? filename : (pre + count + post);
						try {
							writer.setId(name);
							writer.savePlane(count - 1, image);
						}
						catch (final IOException exc) {
							exc.printStackTrace();
						}
						catch (final FormatException exc) {
							exc.printStackTrace();
						}
						count++;
					}
					mxStart = mxEnd;
				}

				// cap off last frame
				setProgress(100, "Saving image " + count + "/" + total);
				BufferedImage image = captureImage(pc, mxStart, d);
				final String name = doMovie ? filename : (pre + count + post);
				try {
					writer.setId(name);
					writer.savePlane(count - 1, image);
					writer.close();
				}
				catch (final IOException exc) {
					exc.printStackTrace();
				}
				catch (final FormatException exc) {
					exc.printStackTrace();
				}

				// clean up
				setProgress(100, "Finishing up");
				image = null;
				SystemManager.gc();

				setProgress(0, "");
				wm.setWaitCursor(false);
			}
		}.start();
	}

	// -- CaptureHandler API methods - state logic --

	/** Tests whether two objects are in equivalent states. */
	public boolean matches(final CaptureHandler handler) {
		if (handler == null) return false;
		final Vector vo = getPositions();
		final Vector vn = handler.getPositions();
		if (vo == null && vn != null) return false;
		if (vo != null && !vo.equals(vn)) return false;
		if (getSpeed() != handler.getSpeed() || getFPS() != handler.getFPS() ||
			isSmooth() != handler.isSmooth())
		{
			return false;
		}
		return true;
	}

	/**
	 * Modifies this object's state to match that of the given object. If the
	 * argument is null, the object is initialized according to its current state
	 * instead.
	 */
	public void initState(final CaptureHandler handler) {
		if (handler != null) {
			// merge old and new position vectors
			final Vector vo = getPositions();
			final Vector vn = handler.getPositions();
			StateManager.mergeStates(vo, vn);
			positions = vn;

			// set other parameters
			movieSpeed = handler.getSpeed();
			movieFPS = handler.getFPS();
			movieSmooth = handler.isSmooth();
		}

		if (panel == null) {
			panel = new CapturePanel(this);

			// snapshot file chooser
			imageBox = new JFileChooser();
			imageBox.addChoosableFileFilter(new ExtensionFileFilter("jpg",
				"JPEG images"));
			imageBox.addChoosableFileFilter(new ExtensionFileFilter("tif",
				"TIFF images"));

			// movie file chooser
			movieBox = new JFileChooser();
			movieBox.addChoosableFileFilter(new ExtensionFileFilter("avi",
				"AVI movies"));
		}

		// set capture window state to match
		panel.setPositions(positions);
		panel.setSpeed(movieSpeed);
		panel.setFPS(movieFPS);
		panel.setSmooth(movieSmooth);
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("Display"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Vector pos = panel.getPositions();
		final int speed = panel.getSpeed();
		final int fps = panel.getFPS();
		final boolean smooth = panel.isSmooth();

		// save display positions
		final int numPositions = pos.size();
		final Element child = XMLUtil.createChild(el, "Capture");
		for (int i = 0; i < numPositions; i++) {
			final DisplayPosition position = (DisplayPosition) pos.elementAt(i);
			position.saveState(child);
		}

		// save other parameters
		child.setAttribute("speed", "" + speed);
		child.setAttribute("FPS", "" + fps);
		child.setAttribute("smooth", "" + smooth);
	}

	/** Restores the current state from the given DOM element ("Display"). */
	@Override
	public void restoreState(final Element el) throws SaveException {
		final Element child = XMLUtil.getFirstChild(el, "Capture");

		// restore display positions
		final Element[] els = XMLUtil.getChildren(child, "DisplayPosition");
		final Vector vn = new Vector(els.length);
		for (int i = 0; i < els.length; i++) {
			final DisplayPosition position = new DisplayPosition();
			position.restoreState(els[i]);
			vn.add(position);
		}
		final Vector vo = getPositions();
		if (vo != null) StateManager.mergeStates(vo, vn);
		positions = vn;

		// restore other parameters
		movieSpeed = Integer.parseInt(child.getAttribute("speed"));
		movieFPS = Integer.parseInt(child.getAttribute("FPS"));
		movieSmooth = child.getAttribute("smooth").equalsIgnoreCase("true");
	}

	// -- Helper methods --

	/**
	 * Takes a snapshot of the given display with the specified projection matrix.
	 */
	protected BufferedImage captureImage(final ProjectionControl pc,
		final double[] mx, final DisplayImpl d)
	{
		BufferedImage image = null;
		try {
			pc.setMatrix(mx);

			// HACK - lame, stupid waiting trick to capture images properly
			try {
				Thread.sleep(100);
			}
			catch (final InterruptedException exc) {
				exc.printStackTrace();
			}

			image = d.getImage(false);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
		catch (final IOException exc) {
			exc.printStackTrace();
		}
		return image;
	}

	/** Sets capture panel's progress bar percentage value and message. */
	protected void setProgress(final int percent, final String message) {
		final int value = percent;
		final String msg = message;
		Util.invoke(false, new Runnable() {

			@Override
			public void run() {
				panel.setProgressValue(value);
				if (msg != null) panel.setProgressMessage(msg);
			}
		});
	}

	// -- Utility methods --

	/** Evaluates a smooth sine function at the given value. */
	protected static double sine(final double x) {
		// [0, 1] -> [-pi/2, pi/2] -> [0, 1]
		return (Math.sin(Math.PI * (x - 0.5)) + 1) / 2;
	}

}
