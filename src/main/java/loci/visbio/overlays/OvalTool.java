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
 * OvalTool is the tool for creating oval overlays.
 */
public class OvalTool extends OverlayTool {

  // -- Fields --

  /** Oval currently being drawn. */
  protected OverlayOval oval;

  // -- Constructor --

  /** Constructs an oval overlay creation tool. */
  public OvalTool(OverlayTransform overlay) {
    super(overlay, "Oval", "Oval", "oval.png");
  }

  // -- OverlayTool API methods --

  /** Instructs this tool to respond to a mouse press. */
  public void mouseDown(DisplayEvent e, int px, int py,
    float dx, float dy, int[] pos, int mods)
  {
    deselectAll();
    oval = new OverlayOval(overlay, dx, dy, dx, dy);
    overlay.addObject(oval, pos);
  }

  /** Instructs this tool to respond to a mouse release. */
  public void mouseUp(DisplayEvent e, int px, int py,
    float dx, float dy, int[] pos, int mods)
  {
    if (oval == null) return;
    oval.setDrawing(false);
    if (!oval.hasData()) overlay.removeObject(oval);
    oval = null;
    overlay.notifyListeners(new TransformEvent(overlay));
  }

  /** Instructs this tool to respond to a mouse drag. */
  public void mouseDrag(DisplayEvent e, int px, int py,
    float dx, float dy, int[] pos, int mods)
  {
    if (oval == null) return;
    oval.setCoords2(dx, dy);
    overlay.notifyListeners(new TransformEvent(overlay));
  }

}
