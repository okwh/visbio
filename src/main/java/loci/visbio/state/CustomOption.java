//
// CustomOption.java
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

/**
 * CustomOption is an option in the VisBio Options dialog.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/visbio/src/loci/visbio/state/CustomOption.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/visbio/src/loci/visbio/state/CustomOption.java">SVN</a></dd></dl>
 */
public class CustomOption extends BioOption {

  // -- Fields --

  /** Custom GUI component. */
  private Component c;

  // -- Constructor --

  /** Constructs a new option. */
  public CustomOption(Component c) {
    super("[Custom]");
    this.c = c;
  }

  // -- BioOption API methods --

  /** Gets a GUI component representing this option. */
  public Component getComponent() { return c; }

  /** Does nothing for a custom option. */
  public void setValue(String value) { }

}
