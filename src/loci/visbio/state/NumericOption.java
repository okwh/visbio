//
// NumericOption.java
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

package loci.visbio.state;

import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.JTextField;
import loci.visbio.util.FormsUtil;
import loci.visbio.util.XMLUtil;
import org.w3c.dom.Element;

/**
 * NumericOption is an integer option in the VisBio Options dialog.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/loci/visbio/state/NumericOption.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/loci/visbio/state/NumericOption.java">SVN</a></dd></dl>
 */
public class NumericOption extends BioOption {

  // -- Fields --

  /** Panel containing GUI components. */
  private JPanel panel;

  /** Text field GUI component. */
  private JTextField field;

  // -- Constructor --

  /** Constructs a new option. */
  public NumericOption(String text, String unit, String tip, int value) {
    this(text, unit, tip, "" + value);
  }

  /** Constructs a new option. */
  public NumericOption(String text, String unit, String tip, double value) {
    this(text, unit, tip, "" + value);
  }

  /** Constructs a new option. */
  public NumericOption(String text, String unit, String tip, String value) {
    super(text);

    // text field
    field = new JTextField(4);
    field.setText(value);
    field.setToolTipText(tip);

    // lay out components
    panel = unit == null ? FormsUtil.makeRow(text, field) :
      FormsUtil.makeRow(text, field, unit);
  }

  // -- NumericOption API methods --

  /** Gets this option's current setting as an integer value. */
  public int getIntegerValue() {
    int value;
    try { value = Integer.parseInt(field.getText()); }
    catch (NumberFormatException exc) { value = -1; }
    return value;
  }

  /**
   * Gets this option's current setting as a
   * double-precision floating point value.
   */
  public double getFloatingValue() {
    double value;
    try { value = Double.parseDouble(field.getText()); }
    catch (NumberFormatException exc) { value = Double.NaN; }
    return value;
  }

  // -- BioOption API methods --

  /** Gets a GUI component representing this option. */
  public Component getComponent() { return panel; }

  /** Sets the GUI component to reflect the given value. */
  public void setValue(String value) { field.setText(value); }

  // -- Saveable API methods --

  /** Writes the current state to the given DOM element ("Options"). */
  public void saveState(Element el) throws SaveException {
    Element e = XMLUtil.createChild(el, "Number");
    e.setAttribute("name", text);
    e.setAttribute("value", field.getText());
  }

  /** Restores the current state from the given DOM element ("Options"). */
  public void restoreState(Element el) throws SaveException {
    Element[] e = XMLUtil.getChildren(el, "Number");
    for (int i=0; i<e.length; i++) {
      String name = e[i].getAttribute("name");
      if (!name.equals(text)) continue;
      String value = e[i].getAttribute("value");
      field.setText(value);
      break;
    }
  }

}
