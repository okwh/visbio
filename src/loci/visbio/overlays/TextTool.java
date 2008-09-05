//
// TextTool.java
//

/*
VisBio application for visualization of multidimensional biological
image data. Copyright (C) 2002-@year@ Curtis Rueden and Abraham Sorber.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.visbio.overlays;

import loci.visbio.data.TransformEvent;
import visad.DisplayEvent;

/**
 * TextTool is the tool for creating text overlays.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/visbio/src/loci/visbio/overlays/TextTool.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/visbio/src/loci/visbio/overlays/TextTool.java">SVN</a></dd></dl>
 */
public class TextTool extends OverlayTool {

  // -- Fields --

  /** Text currently being rendered. */
  protected OverlayText text;

  // -- Constructor --

  /** Constructs a text overlay creation tool. */
  public TextTool(OverlayTransform overlay) {
    super(overlay, "Text", "Text", "text.png");
  }

  // -- OverlayTool API methods --

  /** Instructs this tool to respond to a mouse press. */
  public void mouseDown(DisplayEvent e, int px, int py,
    float dx, float dy, int[] pos, int mods)
  {
    deselectAll();
    OverlayWidget controls = (OverlayWidget) overlay.getControls();
    text = new OverlayText(overlay, dx, dy, controls.getText());
    overlay.addObject(text, pos);
  }

  /** Instructs this tool to respond to a mouse release. */
  public void mouseUp(DisplayEvent e, int px, int py,
    float dx, float dy, int[] pos, int mods)
  {
    if (text == null) return;
    text.setDrawing(false);
    text = null;
    overlay.notifyListeners(new TransformEvent(overlay));
  }

  /** Instructs this tool to respond to a mouse drag. */
  public void mouseDrag(DisplayEvent e, int px, int py,
    float dx, float dy, int[] pos, int mods)
  {
    if (text == null) return;
    text.setCoords(dx, dy);
    overlay.notifyListeners(new TransformEvent(overlay));
  }

}
