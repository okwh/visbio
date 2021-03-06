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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import loci.visbio.util.FormsUtil;
import loci.visbio.util.LAFUtil;
import loci.visbio.util.SwingUtil;

/**
 * Provides GUI controls for a display view handler.
 */
public class ViewPanel extends JPanel implements ActionListener,
	DocumentListener
{

	// -- Fields --

	/** View handler upon which GUI controls operate. */
	protected ViewHandler handler;

	/** Aspect ratio X component text field. */
	protected JTextField aspectX;

	/** Aspect ratio Y component text field. */
	protected JTextField aspectY;

	/** Aspect ratio Z component text field. */
	protected JTextField aspectZ;

	// -- Constructor --

	/** Creates a panel containing view handler GUI controls. */
	public ViewPanel(final ViewHandler h) {
		super();
		handler = h;

		// lay out components
		setLayout(new BorderLayout());
		add(FormsUtil.makeColumn(new Object[] { doOrientationPanel(),
			doAspectPanel(), doAppearancePanel() }, "pref:grow", false));
	}

	// -- ViewPanel API methods --

	/** Updates aspect ratio GUI controls. */
	public void setAspect(final double dx, final double dy, final double dz) {
		aspectX.getDocument().removeDocumentListener(this);
		aspectY.getDocument().removeDocumentListener(this);
		if (aspectZ != null) aspectZ.getDocument().removeDocumentListener(this);
		aspectX.setText("" + dx);
		aspectY.setText("" + dy);
		if (aspectZ != null) aspectZ.setText("" + dz);
		aspectX.getDocument().addDocumentListener(this);
		aspectY.getDocument().addDocumentListener(this);
		if (aspectZ != null) aspectZ.getDocument().addDocumentListener(this);
		doAspect();
	}

	// -- ActionListener API methods --

	/** Handles button presses and combo box selections. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final String cmd = e.getActionCommand();
		if (cmd.equals("zoomIn")) handler.zoomIn();
		else if (cmd.equals("zoomOut")) handler.zoomOut();
		else if (cmd.equals("zoomReset")) handler.reset();
		else if (cmd.equals("panLeft")) handler.panLeft();
		else if (cmd.equals("panRight")) handler.panRight();
		else if (cmd.equals("panUp")) handler.panUp();
		else if (cmd.equals("panDown")) handler.panDown();
		else if (cmd.equals("rotateClock")) handler.rotateClockwise();
		else if (cmd.equals("rotateCounter")) handler.rotateCounterclockwise();
		else if (cmd.equals("rotateLeft")) handler.rotateLeft();
		else if (cmd.equals("rotateRight")) handler.rotateRight();
		else if (cmd.equals("rotateUp")) handler.rotateUp();
		else if (cmd.equals("rotateDown")) handler.rotateDown();
		else if (cmd.equals("scale")) {
			final JCheckBox scale = (JCheckBox) e.getSource();
			handler.toggleScale(scale.isSelected());
		}
		else if (cmd.equals("box")) {
			final JCheckBox box = (JCheckBox) e.getSource();
			handler.toggleBoundingBox(box.isSelected());
		}
		else if (cmd.equals("parallel")) {
			final JCheckBox parallel = (JCheckBox) e.getSource();
			handler.toggleParallel(parallel.isSelected());
		}
	}

	// -- DocumentListener API methods --

	/** Handles text field changes. */
	@Override
	public void changedUpdate(final DocumentEvent e) {
		doAspect();
	}

	/** Handles text insertions into a text field. */
	@Override
	public void insertUpdate(final DocumentEvent e) {
		doAspect();
	}

	/** Handles text removals from a text field. */
	@Override
	public void removeUpdate(final DocumentEvent e) {
		doAspect();
	}

	// -- Helper methods --

	/** Creates a panel with orientation-related components. */
	protected JPanel doOrientationPanel() {
		final boolean threeD = handler.getWindow().is3D();

		// zoom buttons
		final JButton zoomIn =
			makeButton("zoom-in.png", "Zoom in", "zoomIn", "Zooms in on the display");
		final JButton zoomOut =
			makeButton("zoom-out.png", "Zoom out", "zoomOut",
				"Zooms out on the display");
		final JButton zoomReset =
			makeButton("zoom-reset.png", "Reset zoom", "zoomReset",
				"Resets the display to its original orientation");

		// pan buttons
		final JButton panL =
			makeButton("pan-left.png", "Pan left", "panLeft",
				"Pans the display to the left");
		final JButton panR =
			makeButton("pan-right.png", "Pan right", "panRight",
				"Pans the display to the right");
		final JButton panU =
			makeButton("pan-up.png", "Pan up", "panUp", "Pans the display upward");
		final JButton panD =
			makeButton("pan-down.png", "Pan down", "panDown",
				"Pans the display downward");

		// rotate buttons
		final JButton rotateCW =
			makeButton("rotate-clock.png", "Rotate clockwise", "rotateClock",
				"Rotates the display clockwise");
		final JButton rotateCC =
			makeButton("rotate-counter.png", "Rotate counterclockwise",
				"rotateCounter", "Rotates the display counterclockwise");
		JButton rotateL = null, rotateR = null, rotateU = null, rotateD = null;
		if (threeD) {
			rotateL =
				makeButton("rotate-left.png", "Rotate left", "rotateLeft",
					"Rotates the display to the left");
			rotateR =
				makeButton("rotate-right.png", "Rotate right", "rotateRight",
					"Rotates the display to the right");
			rotateU =
				makeButton("rotate-up.png", "Rotate up", "rotateUp",
					"Rotates the display upward");
			rotateD =
				makeButton("rotate-down.png", "Rotate down", "rotateDown",
					"Rotates the display downward");
		}

		// lay out components
		final PanelBuilder builder =
			new PanelBuilder(new FormLayout("pref, 3dlu, "
				+ "pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref",
				"pref, 3dlu, pref, 3dlu, pref"));
		final CellConstraints cc = new CellConstraints();

		builder.addLabel("Zoom", cc.xy(1, 1));
		builder.add(zoomIn, cc.xy(3, 1));
		builder.add(zoomOut, cc.xy(5, 1));
		builder.add(zoomReset, cc.xy(7, 1));

		builder.addLabel("Pan", cc.xy(1, 3));
		builder.add(panL, cc.xy(3, 3));
		builder.add(panR, cc.xy(5, 3));
		builder.add(panU, cc.xy(7, 3));
		builder.add(panD, cc.xy(9, 3));

		builder.addLabel("Rotate", cc.xy(1, 5));
		builder.add(rotateCW, cc.xy(3, 5));
		builder.add(rotateCC, cc.xy(5, 5));
		if (threeD) {
			builder.add(rotateL, cc.xy(7, 5));
			builder.add(rotateR, cc.xy(9, 5));
			builder.add(rotateU, cc.xy(11, 5));
			builder.add(rotateD, cc.xy(13, 5));
		}

		return builder.getPanel();
	}

	/** Creates a panel with aspect ratio-related components. */
	protected JPanel doAspectPanel() {
		final boolean threeD = handler.getWindow().is3D();

		// aspect ratio X component
		aspectX = new JTextField("" + handler.getAspectX(), 4);
		aspectX.getDocument().addDocumentListener(this);

		// aspect ratio Y component
		aspectY = new JTextField("" + handler.getAspectY(), 4);
		aspectY.getDocument().addDocumentListener(this);

		// aspect ratio Z component
		if (threeD) {
			aspectZ = new JTextField("" + handler.getAspectZ(), 4);
			aspectZ.getDocument().addDocumentListener(this);
		}

		// lay out components
		final Object[] o =
			threeD ? new Object[] { "Aspect ratio:", "&X", aspectX, "&Y", aspectY,
				"&Z", aspectZ } : new Object[] { "Aspect ratio:", "&X", aspectX, "&Y",
				aspectY };
		return FormsUtil.makeRow(o);
	}

	/** Creates a panel with appearance-related components. */
	protected JPanel doAppearancePanel() {
		final boolean threeD = handler.getWindow().is3D();

		// scale checkbox
		final JCheckBox scale = new JCheckBox("Scale", handler.isScale());
		if (!LAFUtil.isMacLookAndFeel()) scale.setMnemonic('s');
		scale.setToolTipText("Toggles scale");
		scale.setActionCommand("scale");
		scale.addActionListener(this);

		// bounding box checkbox
		final JCheckBox boundingBox =
			new JCheckBox("Bounding box", handler.isBoundingBox());
		if (!LAFUtil.isMacLookAndFeel()) boundingBox.setMnemonic('b');
		boundingBox.setToolTipText("Toggles display's white bounding box");
		boundingBox.setActionCommand("box");
		boundingBox.addActionListener(this);

		// parallel projection checkbox
		JCheckBox parallel = null;
		if (threeD) {
			parallel = new JCheckBox("Parallel projection", handler.isParallel());
			if (!LAFUtil.isMacLookAndFeel()) parallel.setMnemonic('p');
			parallel.setToolTipText("Toggles whether display uses a parallel "
				+ "projection (instead of perspective)");
			parallel.setActionCommand("parallel");
			parallel.addActionListener(this);
		}

		// lay out components
		final Object[] o =
			threeD ? new Object[] { scale, boundingBox, parallel } : new Object[] {
				scale, boundingBox };
		return FormsUtil.makeRow(o);
	}

	/** Creates a button with a graphical icon. */
	protected JButton makeButton(final String filename, final String altText,
		final String cmd, final String tip)
	{
		final JButton b = SwingUtil.makeButton(this, filename, altText, 6, 6);
		b.setToolTipText(tip);
		b.setActionCommand(cmd);
		b.addActionListener(this);
		return b;
	}

	/** Updates aspect ratio based on text field values. */
	protected void doAspect() {
		double xasp, yasp, zasp;

		try {
			xasp = Double.parseDouble(aspectX.getText());
		}
		catch (final NumberFormatException exc) {
			xasp = 1.0;
		}

		try {
			yasp = Double.parseDouble(aspectY.getText());
		}
		catch (final NumberFormatException exc) {
			yasp = 1.0;
		}

		if (aspectZ == null) zasp = 1.0;
		else {
			try {
				zasp = Double.parseDouble(aspectZ.getText());
			}
			catch (final NumberFormatException exc) {
				zasp = 1.0;
			}
		}

		handler.setAspect(xasp, yasp, zasp);
	}

}
