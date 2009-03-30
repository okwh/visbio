//
// BioComboBox.java
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

package loci.visbio.util;

import java.awt.Dimension;
import javax.swing.JComboBox;

/**
 * An extension of JComboBox that makes the widget slightly wider than normal,
 * to work around a bug in the Windows and GTK Look and Feels where combo boxes
 * are slightly too narrow.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/visbio/src/loci/visbio/util/BioComboBox.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/visbio/src/loci/visbio/util/BioComboBox.java">SVN</a></dd></dl>
 */
public class BioComboBox extends JComboBox {

  // -- Constructor --

  /** Constructs a new combo box. */
  public BioComboBox() { super(); }

  /** Constructs a new combo box. */
  public BioComboBox(String[] s) { super(s); }

  // -- Component API methods --

  /**
   * Gets a slightly wider preferred size than normal, to work around a bug
   * in the Windows and GTK Look and Feels where combo boxes are slightly too
   * narrow.
   */
  public Dimension getPreferredSize() {
    Dimension prefSize = super.getPreferredSize();
    if (LAFUtil.isWindowsLookAndFeel() || LAFUtil.isGTKLookAndFeel()) {
      return new Dimension(prefSize.width + 10, prefSize.height);
    }
    return prefSize;
  }

}
