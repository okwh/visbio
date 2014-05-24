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

import java.awt.event.ItemListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import loci.visbio.util.BioComboBox;
import loci.visbio.util.ColorUtil;
import visad.RealType;
import visad.ScalarType;

/**
 * BioColorWidget is a widget for controlling mappings from range scalars to
 * color scalars.
 */
public class BioColorWidget extends JPanel {

	// -- Constants --

	/** List of color component names. */
	protected static final String[][] COLOR_NAMES = { { "Red", "Green", "Blue" },
		{ "Hue", "Saturation", "Value" } };

	// -- GUI components --

	/** Label naming color component. */
	protected JLabel color;

	/** Combo box listing available range components. */
	protected BioComboBox box;

	/** List of range component scalars. */
	protected Vector scalars;

	// -- Other fields --

	/** Color model: RGB or HSV. */
	protected int model;

	/** Color type: 1=Red/Hue, 2=Green/Saturation, 3=Blue/Value. */
	protected int type;

	// -- Constructor --

	/** Constructs a new color widget. */
	public BioColorWidget(final int colorType) {
		model = ColorUtil.RGB_MODEL;
		type = colorType;

		// create components
		color = new JLabel(COLOR_NAMES[model][type]);
		box = new BioComboBox();
		scalars = new Vector();
		addItem("None", ColorUtil.CLEAR);
		addItem("Full", ColorUtil.SOLID);
		color.setLabelFor(box);

		// lay out components
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		final JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
		p.add(Box.createHorizontalGlue());
		p.add(color);
		p.add(Box.createHorizontalGlue());
		add(p);
		add(box);
	}

	// -- BioColorWidget API methods --

	/** Adds a scalar to the list of available choices. */
	public void addItem(final String name, final ScalarType type) {
		box.addItem(name);
		scalars.add(type);
	}

	/** Gets the currently selected RealType, or null if none. */
	public RealType getSelectedItem() {
		return (RealType) scalars.elementAt(box.getSelectedIndex());
	}

	/** Gets the widget's color model (RGB, HSV or COMPOSITE). */
	public int getModel() {
		return model;
	}

	/** Sets the currently selected RealType. */
	public void setSelectedItem(final RealType rt) {
		if (rt == null) box.setSelectedIndex(0);
		else box.setSelectedIndex(scalars.indexOf(rt));
	}

	/** Sets the widget's color model (RGB, HSV or COMPOSITE). */
	public void setModel(final int model) {
		this.model = model;
		if (model == ColorUtil.COMPOSITE_MODEL) {
			color.setText(COLOR_NAMES[ColorUtil.RGB_MODEL][type]);
			color.setEnabled(false);
			box.setEnabled(false);
		}
		else {
			color.setText(COLOR_NAMES[model][type]);
			color.setEnabled(true);
			box.setEnabled(true);
		}
	}

	/** Adds an item listener to this widget. */
	public void addItemListener(final ItemListener l) {
		box.addItemListener(l);
	}

	/** Removes an item listener from this widget. */
	public void removeItemListener(final ItemListener l) {
		box.removeItemListener(l);
	}

	/** Sets the mnemonic for this widget. */
	public void setMnemonic(final char c) {
		color.setDisplayedMnemonic(c);
	}

	/** Sets the tool tip for this widget. */
	@Override
	public void setToolTipText(final String text) {
		color.setToolTipText(text);
		box.setToolTipText(text);
	}

	/** Chooses most desirable range type for this widget's color. */
	public void guessType(final RealType[] rt) {
		box.removeAllItems();
		scalars.removeAllElements();
		addItem("None", ColorUtil.CLEAR);
		addItem("Full", ColorUtil.SOLID);
		if (rt != null) {
			for (int i = 0; i < rt.length; i++)
				addItem("#" + (i + 1), rt[i]);
		}

		// Autodetect types

		if (model == ColorUtil.RGB_MODEL || model == ColorUtil.COMPOSITE_MODEL) {
			// Case 0: no rtypes
			// R -> none
			// G -> none
			// B -> none

			if (rt == null || rt.length == 0) setSelectedItem(null); // none

			// Case 1: rtypes.length == 1
			// R -> rtypes[0]
			// G -> rtypes[0]
			// B -> rtypes[0]

			else if (rt.length == 1) setSelectedItem(rt[0]);

			// Case 2: rtypes.length == 2
			// R -> rtypes[0]
			// G -> rtypes[1]
			// B -> none

			// Case 3: rtypes.length >= 3
			// R -> rtypes[0]
			// G -> rtypes[1]
			// B -> rtypes[2]

			else {
				if (type == 0) setSelectedItem(rt[0]);
				else if (type == 1) setSelectedItem(rt[1]);
				else if (type == 2 && rt.length >= 3) setSelectedItem(rt[2]);
				else setSelectedItem(null); // none
			}
		}
		else { // model == ColorUtil.HSV_MODEL
			// Case 0: no rtypes
			// H -> none
			// S -> none
			// V -> none

			if (rt == null || rt.length == 0) setSelectedItem(null); // none

			// Case 1: rtypes.length >= 1
			// H -> rtypes[0]
			// S -> full
			// V -> full

			else {
				if (type == 0) setSelectedItem(rt[0]);
				else setSelectedItem(ColorUtil.SOLID); // full
			}
		}
	}

}
