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

package loci.visbio;

import com.jgoodies.looks.LookUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import loci.visbio.help.HelpManager;
import visad.util.Util;

/**
 * SystemManager is the manager encapsulating VisBio's system information report
 * logic.
 */
public class SystemManager extends LogicManager implements ActionListener,
	Runnable
{

	// -- Control panel --

	/** System information control panel. */
	private SystemControls systemControls;

	/** JFrame containing system information control panel. */
	private JFrame systemFrame;

	// -- Constructor --

	/** Constructs a system manager. */
	public SystemManager(final VisBioFrame bio) {
		super(bio);
	}

	// -- SystemManager API methods --

	/** Gets a string detailing current memory usage. */
	public String getMemoryUsage() {
		final Runtime runtime = Runtime.getRuntime();
		final long total = runtime.totalMemory();
		final long free = runtime.freeMemory();
		final long used = total - free;
		final long memUsed = used >> 20;
		final long memTotal = total >> 20;
		return memUsed + " MB used (" + memTotal + " MB reserved)";
	}

	/** Gets maximum amount of memory available to VisBio in megabytes. */
	public int getMaximumMemory() {
		return (int) (Runtime.getRuntime().maxMemory() / 1048376);
	}

	/** Calls the Java garbage collector to free wasted memory. */
	public void cleanMemory() {
		Util.invoke(false, this);
	}

	/**
	 * Updates the VisBio launch parameters to specify the given maximum heap and
	 * look and feel settings.
	 */
	public void writeScript(final int heap, final String laf, final String j3d) {
		// a platform-dependent mess!
		String filename;
		if (LookUtils.IS_OS_WINDOWS) filename = "launcher.cfg";
		else if (LookUtils.IS_OS_MAC) filename = "VisBio.app/Contents/Info.plist";
		else filename = "visbio";

		// read in the VisBio startup script
		final Vector lines = new Vector();
		try {
			final BufferedReader fin = new BufferedReader(new FileReader(filename));
			while (true) {
				final String line = fin.readLine();
				if (line == null) break;
				lines.add(line);
			}
			fin.close();
		}
		catch (final IOException exc) {
			exc.printStackTrace();
		}

		// alter settings in VisBio startup script
		PrintWriter fout = null;
		int size = 0;
		try {
			fout = new PrintWriter(new FileWriter(filename));
			size = lines.size();
		}
		catch (final IOException exc) {
			exc.printStackTrace();
		}

		boolean heapChanged = heap < 0;
		boolean lafChanged = laf == null;
		boolean j3dChanged = j3d == null;

		for (int i = 0; i < size; i++) {
			String line = (String) lines.elementAt(i);

			if (heap >= 0) {
				// adjust maximum heap setting
				final String heapString = "mx";
				final int heapPos = line.indexOf(heapString);
				if (heapPos >= 0) {
					final int end = line.indexOf("m", heapPos + 1);
					if (end >= 0) {
						line =
							line.substring(0, heapPos + heapString.length()) + heap +
								line.substring(end);
						heapChanged = true;
					}
				}
			}

			if (laf != null) {
				// check for L&F setting
				final String lafString = "LookAndFeel";
				final int lafPos = line.indexOf(lafString);
				if (lafPos >= 0) {
					final int start =
						line.lastIndexOf(LookUtils.IS_OS_MAC ? ">" : "=", lafPos);
					if (start >= 0) {
						line =
							line.substring(0, start + 1) + laf + line.substring(lafPos + 11);
						lafChanged = true;
					}
				}
			}

			if (j3d != null) {
				// check for J3D renderer setting
				final String j3dString = "j3d.rend";
				final int j3dPos = line.indexOf(j3dString);
				if (j3dPos >= 0) {
					final int start = line.indexOf("=", j3dPos);
					final int end = line.indexOf(" ", start);
					if (start >= 0) {
						line = line.substring(0, start + 1) + j3d + line.substring(end);
						j3dChanged = true;
					}
				}
			}

			fout.println(line);
		}
		fout.close();

		if (!heapChanged) {
			System.err.println("Warning: no maximum heap setting found " +
				"in launch script " + filename + ".");
		}
		if (!lafChanged) {
			System.err.println("Warning: no Look & Feel setting found " +
				"in launch script " + filename + ".");
		}
	}

	/** Detects whether VisBio was launched with Java Web Start. */
	public boolean isJNLP() {
		return System.getProperty("jnlpx.home") != null;
	}

	/** Gets associated control panel. */
	public SystemControls getControls() {
		if (systemControls == null) {
			// control panel construction delayed until
			// first use, to improve program startup speed
			final WindowManager wm =
				(WindowManager) bio.getManager(WindowManager.class);
			wm.setWaitCursor(true);

			// control panel
			systemControls = new SystemControls(this);
			systemFrame = new JFrame("System Information");
			systemFrame.getContentPane().add(systemControls);

			// register system information window with window manager
			wm.addWindow(systemFrame);
			wm.setWaitCursor(false);
		}
		return systemControls;
	}

	// -- Menu commands --

	/** Displays the system information window. */
	public void showSystemInfo() {
		getControls();
		final WindowManager wm =
			(WindowManager) bio.getManager(WindowManager.class);
		wm.showWindow(systemFrame);
	}

	// -- LogicManager API methods --

	/** Called to notify the logic manager of a VisBio event. */
	@Override
	public void doEvent(final VisBioEvent evt) {
		final int eventType = evt.getEventType();
		if (eventType == VisBioEvent.LOGIC_ADDED) {
			final LogicManager lm = (LogicManager) evt.getSource();
			if (lm == this) doGUI();
		}
	}

	/** Gets the number of tasks required to initialize this logic manager. */
	@Override
	public int getTasks() {
		return 2;
	}

	// -- ActionListener API methods --

	/** Outputs current RAM usage to console. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		System.out.println(System.currentTimeMillis() + ": " + getMemoryUsage());
	}

	// -- Runnable API methods --

	/** Performs garbage collection, displaying a wait cursor while doing so. */
	@Override
	public void run() {
		final WindowManager wm =
			(WindowManager) bio.getManager(WindowManager.class);
		wm.setWaitCursor(true);
		gc();
		wm.setWaitCursor(false);
	}

	// -- Helper methods --

	/** Adds system-related GUI components to VisBio. */
	private void doGUI() {
		// menu items
		bio.setSplashStatus("Initializing system information window");
		final JMenuItem system =
			bio.addMenuItem("Window", "System information",
				"loci.visbio.SystemManager.showSystemInfo", 'i');
		system.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
		bio.addMenuSeparator("Window");

		// help window
		bio.setSplashStatus(null);
		final HelpManager hm = (HelpManager) bio.getManager(HelpManager.class);
		final String s = "Control panels/System panel";
		hm.addHelpTopic(s, "system_panel.html");
		hm.addHelpTopic(s + "/Changing the memory limit", "memory_limit.html");
		hm.addHelpTopic(s + "/Changing VisBio's appearance", "look_and_feel.html");
		hm.addHelpTopic(s + "/Changing the renderer", "renderer.html");

		// RAM usage debugging output
		if (VisBioFrame.DEBUG) new Timer(500, this).start();
	}

	// -- Utility methods --

	/** Does some garbage collection, to free up memory. */
	public static void gc() {
		try {
			for (int i = 0; i < 2; i++) {
				System.gc();
				Thread.sleep(100);
				System.runFinalization();
				Thread.sleep(100);
			}
		}
		catch (final InterruptedException exc) {
			exc.printStackTrace();
		}
	}

}
