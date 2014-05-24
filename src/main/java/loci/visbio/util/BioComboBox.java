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

package loci.visbio.util;

import java.awt.Dimension;

import javax.swing.JComboBox;

/**
 * An extension of JComboBox that makes the widget slightly wider than normal,
 * to work around a bug in the Windows and GTK Look and Feels where combo boxes
 * are slightly too narrow.
 */
public class BioComboBox extends JComboBox {

	// -- Constructor --

	/** Constructs a new combo box. */
	public BioComboBox() {
		super();
	}

	/** Constructs a new combo box. */
	public BioComboBox(final String[] s) {
		super(s);
	}

	// -- Component API methods --

	/**
	 * Gets a slightly wider preferred size than normal, to work around a bug in
	 * the Windows and GTK Look and Feels where combo boxes are slightly too
	 * narrow.
	 */
	@Override
	public Dimension getPreferredSize() {
		final Dimension prefSize = super.getPreferredSize();
		if (LAFUtil.isWindowsLookAndFeel() || LAFUtil.isGTKLookAndFeel()) {
			return new Dimension(prefSize.width + 10, prefSize.height);
		}
		return prefSize;
	}

}
