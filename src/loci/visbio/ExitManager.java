//
// ExitManager.java
//

/*
VisBio application for visualization of multidimensional biological
image data. Copyright (C) 2002-@year@ Curtis Rueden and Abraham Sorber.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.visbio;

import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import com.jgoodies.looks.LookUtils;

/**
 * ExitManager is the manager encapsulating VisBio's shutdown logic.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/visbio/src/loci/visbio/ExitManager.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/visbio/src/loci/visbio/ExitManager.java">SVN</a></dd></dl>
 */
public class ExitManager extends LogicManager implements WindowListener {

  // -- Fields --

  /** Flag indicating shutdown process may continue. */
  private boolean exitOk;

  // -- Constructor --

  /** Constructs an exit manager. */
  public ExitManager(VisBioFrame bio) { super(bio); }

  // -- ExitManager API methods --

  /** Trip flag indicating shutdown process should not continue. */
  public void cancelShutdown() { exitOk = false; }

  // -- LogicManager API methods --

  /** Called to notify the logic manager of a VisBio event. */
  public void doEvent(VisBioEvent evt) {
    int eventType = evt.getEventType();
    if (eventType == VisBioEvent.LOGIC_ADDED) {
      LogicManager lm = (LogicManager) evt.getSource();
      if (lm == this) doGUI();
    }
  }

  /** Gets the number of tasks required to initialize this logic manager. */
  public int getTasks() { return 2; }

  // -- Helper methods --

  /** Adds shutdown-related GUI components to VisBio. */
  private void doGUI() {
    // close action
    bio.setSplashStatus("Initializing shutdown logic");
    bio.setDefaultCloseOperation(VisBioFrame.DO_NOTHING_ON_CLOSE);
    bio.addWindowListener(this);

    // file menu
    bio.setSplashStatus(null);
    if (!LookUtils.IS_OS_MAC) {
      bio.addMenuSeparator("File");
      bio.addMenuItem("File", "Exit", "loci.visbio.ExitManager.fileExit", 'x');
      bio.setMenuShortcut("File", "Exit", KeyEvent.VK_Q);
    }
  }

  // -- WindowListener API methods --

  /** Called when something tries to shut down VisBio. */
  public void windowClosing(WindowEvent e) { fileExit(); }

  /** Unused WindowListener method. */
  public void windowActivated(WindowEvent e) { }

  /** Unused WindowListener method. */
  public void windowClosed(WindowEvent e) { }

  /** Unused WindowListener method. */
  public void windowDeactivated(WindowEvent e) { }

  /** Unused WindowListener method. */
  public void windowDeiconified(WindowEvent e) { }

  /** Unused WindowListener method. */
  public void windowIconified(WindowEvent e) { }

  /** Unused WindowListener method. */
  public void windowOpened(WindowEvent e) { }

  // -- Menu commands --

  /**
   * Exits the application, allowing all logic managers to
   * take appropriate actions before doing so.
   */
  public void fileExit() {
    exitOk = true;
    bio.generateEvent(this, "shutdown request", false);
    if (!exitOk) return;
    bio.generateEvent(this, "shutdown", false);
    bio.destroy();

    // HACK - don't force exit if VisBio was launched from within MATLAB
    boolean force = loci.visbio.ext.MatlabUtil.getMatlabVersion() == null;
    if (force) System.exit(0);
  }

}
