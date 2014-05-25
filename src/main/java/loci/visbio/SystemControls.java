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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.looks.LookUtils;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import loci.formats.gui.LegacyQTTools;
import loci.visbio.ext.MatlabUtil;
import loci.visbio.util.DisplayUtil;
import loci.visbio.util.LAFUtil;
import loci.visbio.util.SwingUtil;
import visad.util.Util;

/**
 * SystemControls is the control panel for reporting system information.
 */
public class SystemControls extends ControlPanel implements ActionListener {

	// -- GUI components --

	/** Memory usage text field. */
	private final JTextField memField;

	/** Look &amp; Feel text field. */
	private final JTextField lafField;

	// -- Fields --

	/** Current memory usage. */
	protected String memUsage;

	// -- Constructor --

	/** Constructs a control panel for viewing system information. */
	public SystemControls(final LogicManager logic) {
		super(logic, "System", "Reports system information");
		final VisBioFrame bio = lm.getVisBio();
		final SystemManager sm = (SystemManager) lm;

		// dump properties button
		final JButton dump = new JButton("Dump all");
		if (!LAFUtil.isMacLookAndFeel()) dump.setMnemonic('d');
		dump.setToolTipText("Dumps system property values to the output console");
		dump.setActionCommand("dump");
		dump.addActionListener(this);

		// operating system text field
		final JTextField osField =
			new JTextField(System.getProperty("os.name") + " (" +
				System.getProperty("os.arch") + ")");
		osField.setEditable(false);

		// java version text field
		final JTextField javaField =
			new JTextField(System.getProperty("java.version") + " (" +
				System.getProperty("java.vendor") + ")");
		javaField.setEditable(false);

		// memory usage text field
		memField = new JTextField("xxxx MB used (xxxx MB reserved)");
		memField.setEditable(false);

		// garbage collection button
		final JButton clean = new JButton("Clean");
		if (!LAFUtil.isMacLookAndFeel()) clean.setMnemonic('c');
		clean
			.setToolTipText("Calls the Java garbage collector to free wasted memory");
		clean.setActionCommand("clean");
		clean.addActionListener(this);

		// memory maximum text field
		final JTextField heapField =
			new JTextField(sm.getMaximumMemory() + " MB maximum");
		heapField.setEditable(false);

		// memory maximum alteration button
		final JButton heap = new JButton("Change...");
		if (!LAFUtil.isMacLookAndFeel()) heap.setMnemonic('a');
		if (sm.isJNLP()) heap.setEnabled(false);
		heap
			.setToolTipText("Edits the maximum amount of memory available to VisBio");
		heap.setActionCommand("heap");
		heap.addActionListener(this);

		// Java3D library text field
		final String j3dVersion = getVersionString("javax.vecmath.Point3d");
		final JTextField java3dField = new JTextField(j3dVersion);
		java3dField.setEditable(false);

		// QuickTime library text field
		final String qtVersion = new LegacyQTTools().getQTVersion();
		final JTextField qtField = new JTextField(qtVersion);
		qtField.setEditable(false);

		// Python library text field
		final JTextField pythonField =
			new JTextField(getVersionString("org.python.util.PythonInterpreter"));
		pythonField.setEditable(false);

		// MATLAB library text field
		final String matlabVersion = MatlabUtil.getMatlabVersion();
		final JTextField matlabField =
			new JTextField(matlabVersion == null ? "Missing" : matlabVersion);
		matlabField.setEditable(false);

		// Look & Feel text field
		lafField = new JTextField(LAFUtil.getLookAndFeel()[0]);
		lafField.setEditable(false);

		// Look & Feel alteration button
		final JButton laf = new JButton("Change...");
		if (!LAFUtil.isMacLookAndFeel()) laf.setMnemonic('n');
		if (sm.isJNLP()) laf.setEnabled(false);
		laf.setToolTipText("Edits VisBio's graphical Look & Feel");
		laf.setActionCommand("laf");
		laf.addActionListener(this);

		// Renderer text field
		final boolean j3d = j3dVersion != null && !j3dVersion.equals("Missing");
		// HACK - Util.canDoJava3D("1.3.2") does not work as expected
		// boolean j3d132 = Util.canDoJava3D("1.3.2");
		int ndx = j3dVersion.indexOf(" ");
		if (ndx < 0) ndx = j3dVersion.length();
		final boolean j3d132 =
			Util.canDoJava3D("1.4") || j3dVersion.substring(0, ndx).equals("1.3.2");
		final boolean j3dWin132 = LookUtils.IS_OS_WINDOWS && j3d132;
		final String rend =
			j3d ? (j3dWin132 ? getJ3DString() : "Java3D") : "Java2D";
		final JTextField renderField = new JTextField(rend);
		renderField.setEditable(false);

		// Renderer alteration button
		final JButton render = new JButton("Change...");
		if (!LAFUtil.isMacLookAndFeel()) render.setMnemonic('g');
		if (!j3dWin132) render.setEnabled(false);
		render.setToolTipText("Changes the renderer used for visualization");
		render.setActionCommand("render");
		render.addActionListener(this);

		// Stereo configuration text field
		final JTextField stereoField =
			new JTextField(DisplayUtil.getStereoConfiguration() == null
				? "Not available" : "Enabled");
		stereoField.setEditable(false);

		// lay out components
		final FormLayout layout =
			new FormLayout("right:pref, 3dlu, pref:grow, 3dlu, pref",
				"pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 9dlu, "
					+ "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 9dlu, "
					+ "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");
		final PanelBuilder builder = new PanelBuilder(layout);
		builder.setDefaultDialogBorder();
		final CellConstraints cc = new CellConstraints();
		int row = 1;

		builder.addSeparator("Properties", cc.xyw(1, row, 3));
		builder.add(dump, cc.xy(5, row));
		row += 2;
		builder.addLabel("&Operating system", cc.xy(1, row)).setLabelFor(osField);
		builder.add(osField, cc.xyw(3, row, 3));
		row += 2;
		builder.addLabel("&Java version", cc.xy(1, row)).setLabelFor(javaField);
		builder.add(javaField, cc.xyw(3, row, 3));
		row += 2;
		builder.addLabel("Memory &usage", cc.xy(1, row)).setLabelFor(memField);
		builder.add(memField, cc.xy(3, row));
		builder.add(clean, cc.xy(5, row));
		row += 2;
		builder.addLabel("Memory ma&ximum", cc.xy(1, row)).setLabelFor(heapField);
		builder.add(heapField, cc.xy(3, row));
		builder.add(heap, cc.xy(5, row));
		row += 2;
		builder.addSeparator("Libraries", cc.xyw(1, row, 5));
		row += 2;
		builder.addLabel("Java&3D", cc.xy(1, row)).setLabelFor(java3dField);
		builder.add(java3dField, cc.xyw(3, row, 3));
		row += 2;
		builder.addLabel("&QuickTime", cc.xy(1, row)).setLabelFor(qtField);
		builder.add(qtField, cc.xyw(3, row, 3));
		row += 2;
		builder.addLabel("&Python", cc.xy(1, row)).setLabelFor(pythonField);
		builder.add(pythonField, cc.xyw(3, row, 3));
		row += 2;
		builder.addLabel("&MATLAB", cc.xy(1, row)).setLabelFor(matlabField);
		builder.add(matlabField, cc.xyw(3, row, 3));
		row += 2;
		builder.addSeparator("Configuration", cc.xyw(1, row, 5));
		row += 2;
		builder.addLabel("&Look && Feel", cc.xy(1, row)).setLabelFor(lafField);
		builder.add(lafField, cc.xy(3, row));
		builder.add(laf, cc.xy(5, row));
		row += 2;
		builder.addLabel("&Renderer", cc.xy(1, row)).setLabelFor(renderField);
		builder.add(renderField, cc.xy(3, row));
		builder.add(render, cc.xy(5, row));
		row += 2;
		builder.addLabel("&Stereo", cc.xy(1, row)).setLabelFor(stereoField);
		builder.add(stereoField, cc.xyw(3, row, 3));
		row += 2;
		add(builder.getPanel());

		// update system information twice per second
		final Timer t = new Timer(500, this);
		t.start();
	}

	// -- ActionListener API methods --

	/** Handles action events. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final String cmd = e.getActionCommand();
		final SystemManager sm = (SystemManager) lm;
		if ("dump".equals(cmd)) {
			final Properties properties = System.getProperties();
			// Properties.list() truncates the property values, so we iterate
			// properties.list(System.out);
			System.out.println("-- listing properties --");
			final Enumeration<?> list = properties.propertyNames();
			while (list.hasMoreElements()) {
				final String key = list.nextElement().toString();
				final String value = properties.getProperty(key);
				System.out.println(key + "=" + value);
			}
		}
		else if ("clean".equals(cmd)) sm.cleanMemory();
		else if ("heap".equals(cmd)) {
			final String max = "" + sm.getMaximumMemory();
			final String heapSize =
				(String) JOptionPane.showInputDialog(this, "New maximum memory value:",
					"VisBio", JOptionPane.QUESTION_MESSAGE, null, null, "" + max);
			if (heapSize == null || heapSize.equals(max)) return;
			int maxHeap = -1;
			try {
				maxHeap = Integer.parseInt(heapSize);
			}
			catch (final NumberFormatException exc) {}
			if (maxHeap < 16) {
				JOptionPane.showMessageDialog(this,
					"Maximum memory value must be at least 16 MB.", "VisBio",
					JOptionPane.ERROR_MESSAGE);
				return;
			}
			sm.writeScript(maxHeap, null, null);
			JOptionPane.showMessageDialog(this,
				"The change will take effect next time you run VisBio.", "VisBio",
				JOptionPane.INFORMATION_MESSAGE);
		}
		else if ("laf".equals(cmd)) {
			final String[] laf = LAFUtil.getLookAndFeel();
			final String[][] lafs = LAFUtil.getAvailableLookAndFeels();
			final String lafName =
				(String) JOptionPane.showInputDialog(this, "New Look & Feel:",
					"VisBio", JOptionPane.QUESTION_MESSAGE, null, lafs[0], laf[0]);
			if (lafName == null) return;
			int ndx = -1;
			for (int i = 0; i < lafs[0].length; i++) {
				if (lafs[0][i].equals(lafName)) {
					ndx = i;
					break;
				}
			}
			if (ndx < 0 || lafs[1][ndx].equals(laf[1])) return; // cancel or same

			try {
				// update Look and Feel
				UIManager.setLookAndFeel(lafs[1][ndx]);
				final WindowManager wm =
					(WindowManager) lm.getVisBio().getManager(WindowManager.class);
				final Window[] w = wm.getWindows();
				for (int i = 0; i < w.length; i++) {
					SwingUtilities.updateComponentTreeUI(w[i]);
					SwingUtil.repack(w[i]);
				}

				// save change to startup script
				sm.writeScript(-1, lafs[1][ndx], null);

				// update L&F field
				lafField.setText(LAFUtil.getLookAndFeel()[0]);
			}
			catch (final ClassNotFoundException exc) {
				exc.printStackTrace();
			}
			catch (final IllegalAccessException exc) {
				exc.printStackTrace();
			}
			catch (final InstantiationException exc) {
				exc.printStackTrace();
			}
			catch (final UnsupportedLookAndFeelException exc) {
				exc.printStackTrace();
			}
		}
		else if ("render".equals(cmd)) {
			final String rend = getJ3DString();
			final String[] renderers = { "Java3D (OpenGL)", "Java3D (Direct3D)" };
			final String[] renderFlags = { "ogl", "d3d" };
			final String renderName =
				(String) JOptionPane.showInputDialog(this, "New renderer:", "VisBio",
					JOptionPane.QUESTION_MESSAGE, null, renderers, renderers[0]);
			if (renderName == null) return;
			int ndx = -1;
			for (int i = 0; i < renderers.length; i++) {
				if (renderers[i].equals(renderName)) {
					ndx = i;
					break;
				}
			}
			if (ndx < 0 || renderers[ndx].equals(rend)) return; // cancel or same
			sm.writeScript(-1, null, renderFlags[ndx]);
			JOptionPane.showMessageDialog(this,
				"The change will take effect next time you run VisBio.", "VisBio",
				JOptionPane.INFORMATION_MESSAGE);
		}
		else {
			// update system information
			if (!lm.getVisBio().isVisible()) return;
			final String mem = ((SystemManager) lm).getMemoryUsage();
			if (!mem.equals(memUsage)) {
				memUsage = mem;
				memField.setText(mem);
			}
		}
	}

	// -- Utility methods --

	/** Gets version information for the specified class. */
	private static String getVersionString(final String clas) {
		Class<?> c = null;
		try {
			c = Class.forName(clas);
		}
		catch (final ClassNotFoundException exc) {
			c = null;
		}
		return getVersionString(c);
	}

	/** Gets version information for the specified class. */
	private static String getVersionString(final Class<?> c) {
		if (c == null) return "Missing";
		final Package p = c.getPackage();
		if (p == null) return "No package";
		final String vendor = p.getImplementationVendor();
		final String version = p.getImplementationVersion();
		if (vendor == null && version == null) return "Available";
		else if (vendor == null) return version;
		else if (version == null) return vendor;
		else return version + " (" + vendor + ")";
	}

	/** Gets a string representing the Java3D renderer currently in use. */
	private static String getJ3DString() {
		final String rend = System.getProperty("j3d.rend");
		return (rend == null || rend.equals("ogl")) ? "Java3D (OpenGL)" : (rend
			.equals("d3d") ? "Java3D (Direct3D)" : "Java3D (" + rend + ")");
	}

}
