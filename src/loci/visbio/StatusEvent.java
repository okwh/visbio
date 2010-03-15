//
// StatusEvent.java
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

/**
 * A event indicating a status update.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/visbio/src/loci/visbio/StatusEvent.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/visbio/src/loci/visbio/StatusEvent.java">SVN</a></dd></dl>
 */
public class StatusEvent {

  // -- Fields --

  /** Current progress value. */
  protected int progress;

  /** Current progress maximum. */
  protected int maximum;

  /** Current status message. */
  protected String status;

  /** Whether or not this is a warning event. */
  protected boolean warning;

  // -- Constructor --

  /** Constructs a status event. */
  public StatusEvent(String message) {
    this(-1, -1, message);
  }

  /** Constructs a status event. */
  public StatusEvent(String message, boolean warn) {
    this(-1, -1, message, warn);
  }

  /** Constructs a status event. */
  public StatusEvent(int progress, int maximum, String message) {
    this(progress, maximum, message, false);
  }

  /** Constructs a status event. */
  public StatusEvent(int progress, int maximum, String message, boolean warn) {
    this.progress = progress;
    this.maximum = maximum;
    status = message;
    warning = warn;
  }

  // -- StatusEvent API methods --

  /** Gets progress value. Returns -1 if progress is unknown. */
  public int getProgressValue() { return progress; }

  /** Gets progress maximum. Returns -1 if progress is unknown. */
  public int getProgressMaximum() { return maximum; }

  /** Gets status message. */
  public String getStatusMessage() { return status; }

  /** Returns whether or not this is a warning event. */
  public boolean isWarning() { return warning; }

}
