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

import visad.DisplayEvent;

/**
 * OverlayTool is the superclass of all overlay tools.
 */
public class OverlayTool {

	// -- Fields --

	/** Associated overlay transform. */
	protected OverlayTransform overlay;

	/** Name of this tool. */
	protected String name;

	/** Tool tip text. */
	protected String tip;

	/** Filename of icon. */
	protected String icon;

	// -- Constructor --

	/** Constructs a measurement line creation tool. */
	public OverlayTool(final OverlayTransform overlay, final String name,
		final String tip, final String icon)
	{
		this.overlay = overlay;
		this.name = name;
		this.tip = tip;
		this.icon = icon;
	}

	// -- OverlayTool API methods --

	/**
	 * Instructs this tool to respond to a mouse press.
	 * 
	 * @param e DisplayEvent corresponding to this mouse press.
	 * @param px X coordinate of mouse press in pixel coordinate system.
	 * @param py Y coordinate of mouse press in pixel coordinate system.
	 * @param dx X coordinate of mouse press in data coordinate system.
	 * @param dy Y coordinate of mouse press in data coordinate system.
	 * @param pos Dimensional position of mouse press.
	 * @param mods Modifiers of mouse press.
	 */
	public void mouseDown(final DisplayEvent e, final int px, final int py,
		final float dx, final float dy, final int[] pos, final int mods)
	{}

	/**
	 * Instructs this tool to respond to a mouse release.
	 * 
	 * @param e DisplayEvent corresponding to this mouse release.
	 * @param px X coordinate of mouse release in pixel coordinate system.
	 * @param py Y coordinate of mouse release in pixel coordinate system.
	 * @param dx X coordinate of mouse release in data coordinate system.
	 * @param dy Y coordinate of mouse release in data coordinate system.
	 * @param pos Dimensional position of mouse release.
	 * @param mods Modifiers of mouse release.
	 */
	public void mouseUp(final DisplayEvent e, final int px, final int py,
		final float dx, final float dy, final int[] pos, final int mods)
	{}

	/**
	 * Instructs this tool to respond to a mouse drag.
	 * 
	 * @param e DisplayEvent corresponding to this mouse drag.
	 * @param px X coordinate of mouse drag in pixel coordinate system.
	 * @param py Y coordinate of mouse drag in pixel coordinate system.
	 * @param dx X coordinate of mouse drag in data coordinate system.
	 * @param dy Y coordinate of mouse drag in data coordinate system.
	 * @param pos Dimensional position of mouse drag.
	 * @param mods Modifiers of mouse drag.
	 */
	public void mouseDrag(final DisplayEvent e, final int px, final int py,
		final float dx, final float dy, final int[] pos, final int mods)
	{}

	/**
	 * Instructs this tool to respond to mouse movement.
	 * 
	 * @param e DisplayEvent corresponding to this mouse movement.
	 * @param px X coordinate of mouse movement in pixel coordinate system.
	 * @param py Y coordinate of mouse movement in pixel coordinate system.
	 * @param dx X coordinate of mouse movement in data coordinate system.
	 * @param dy Y coordinate of mouse movement in data coordinate system.
	 * @param pos Dimensional position of mouse movement.
	 * @param mods Modifiers of mouse movement.
	 */
	public void mouseMoved(final DisplayEvent e, final int px, final int py,
		final float dx, final float dy, final int[] pos, final int mods)
	{}

	/**
	 * Instructs this tool to respond to a key press.
	 * 
	 * @param code Key code of pressed key.
	 * @param mods Modifiers of pressed key.
	 */
	public void keyPressed(final int code, final int mods) {}

	/**
	 * Instructs this tool to respond to a key release.
	 * 
	 * @param code Key code of released key.
	 * @param mods Modifiers of released key.
	 */
	public void keyReleased(final int code, final int mods) {}

	/** Gets associated overlay transform. */
	public OverlayTransform getTransform() {
		return overlay;
	}

	/** Gets tool name. */
	public String getName() {
		return name;
	}

	/** Gets tool tip text. */
	public String getTip() {
		return tip;
	}

	/** Gets path to icon file. */
	public String getIcon() {
		return icon;
	}

	// -- Helper methods --

	/** Deselect all selected overlays. */
	protected void deselectAll() {
		final OverlayObject[] obj = overlay.getObjects();
		if (obj != null) {
			for (int i = 0; i < obj.length; i++)
				obj[i].setSelected(false);
		}
	}

}
