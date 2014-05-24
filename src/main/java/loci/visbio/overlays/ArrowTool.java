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

package loci.visbio.overlays;

import loci.visbio.data.TransformEvent;
import visad.DisplayEvent;

/**
 * ArrowTool is the tool for creating arrow overlays.
 */
public class ArrowTool extends OverlayTool {

	// -- Fields --

	/** Arrow currently being drawn. */
	protected OverlayArrow arrow;

	// -- Constructor --

	/** Constructs an arrow overlay creation tool. */
	public ArrowTool(final OverlayTransform overlay) {
		super(overlay, "Arrow", "Arrow", "arrow.png");
	}

	// -- OverlayTool API methods --

	/** Instructs this tool to respond to a mouse press. */
	@Override
	public void mouseDown(final DisplayEvent e, final int px, final int py,
		final float dx, final float dy, final int[] pos, final int mods)
	{
		deselectAll();
		arrow = new OverlayArrow(overlay, dx, dy, dx, dy);
		overlay.addObject(arrow, pos);
	}

	/** Instructs this tool to respond to a mouse release. */
	@Override
	public void mouseUp(final DisplayEvent e, final int px, final int py,
		final float dx, final float dy, final int[] pos, final int mods)
	{
		if (arrow == null) return;
		arrow.setDrawing(false);
		if (!arrow.hasData()) overlay.removeObject(arrow);
		arrow = null;
		overlay.notifyListeners(new TransformEvent(overlay));
	}

	/** Instructs this tool to respond to a mouse drag. */
	@Override
	public void mouseDrag(final DisplayEvent e, final int px, final int py,
		final float dx, final float dy, final int[] pos, final int mods)
	{
		if (arrow == null) return;
		arrow.setCoords2(dx, dy);
		overlay.notifyListeners(new TransformEvent(overlay));
	}

}
