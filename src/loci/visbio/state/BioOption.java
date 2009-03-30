//
// BioOption.java
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

package loci.visbio.state;

import java.awt.Component;
import org.w3c.dom.Element;

/**
 * BioOption represents an option in the VisBio Options dialog.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/visbio/src/loci/visbio/state/BioOption.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/visbio/src/loci/visbio/state/BioOption.java">SVN</a></dd></dl>
 */
public abstract class BioOption implements Saveable {

  // -- Fields --

  /** String identifying this option. */
  protected String text;

  // -- Constructor --

  /** Constructs a new option. */
  public BioOption(String text) { this.text = text; }

  // -- BioOption API methods --

  /** Gets text identifying this option. */
  public String getText() { return text; }

  /** Gets a GUI component representing this option. */
  public abstract Component getComponent();

  // -- Saveable API methods --

  /** Writes the current state to the given DOM element ("Options"). */
  public void saveState(Element el) throws SaveException { }

  /** Restores the current state from the given DOM element ("Options"). */
  public void restoreState(Element el) throws SaveException { }

}
