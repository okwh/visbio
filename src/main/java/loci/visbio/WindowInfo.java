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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import loci.visbio.util.SwingUtil;

/**
 * A class for keeping track of information about a window.
 */
public class WindowInfo implements WindowListener {

  // -- Constants --

  /** Gap between cascading windows. */
  protected static final int GAP = 25;

  /** Current gap from top left edge of screen. */
  protected static int gap;

  // -- Fields --

  /** Window for which this object stores additional information. */
  protected Window window;

  /** Whether to pack this window the first time it is shown. */
  protected boolean pack;

  /** True if the window has not yet been shown onscreen. */
  protected boolean first;

  // -- Constructor --

  /**
   * Creates a new window information object for the given window,
   * with the pack flag indicating whether the window is to be packed
   * prior to being shown for the first time.
   */
  public WindowInfo(Window w, boolean pack) {
    window = w;
    this.pack = pack;
    first = true;
    window.addWindowListener(this);
  }

  // -- WindowInfo API methods --

  /** Displays the window onscreen. */
  public void showWindow() {
    if (first && pack) SwingUtil.pack(window);

    // arrange window in cascade formation
    Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
    Rectangle scr = new Rectangle(0, 0, ss.width, ss.height);
    Rectangle win = window.getBounds();
    boolean offscreen = !win.intersects(scr);

    if (first || offscreen) {
      gap += GAP;
      if (gap + win.width > scr.width || gap + win.height > scr.height) {
        gap = 0;
      }
      window.setLocation(gap, gap);
    }

    window.setVisible(true);
  }

  /** Sets the window to match the given state. */
  public void setState(WindowState ws) {
    ws.applyTo(window);
    first = !ws.hasPosition();
    pack = !ws.hasSize();
  }

  /** Gets the associated window. */
  public Window getWindow() { return window; }

  // -- WindowListener API methods --

  public void windowOpened(WindowEvent e) { first = false; }

  public void windowActivated(WindowEvent e) { }
  public void windowClosing(WindowEvent e) { }
  public void windowClosed(WindowEvent e) { }
  public void windowDeactivated(WindowEvent e) { }
  public void windowDeiconified(WindowEvent e) { }
  public void windowIconified(WindowEvent e) { }

}
