//
// OverlayBox.java
//

/*
VisBio application for visualization of multidimensional
biological image data. Copyright (C) 2002-@year@ Curtis Rueden.

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

package loci.visbio.overlays;

import java.awt.Color;
import java.rmi.RemoteException;
import java.util.Arrays;
import visad.*;

/**
 * OverlayBox is a rectangle overlay.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/loci/visbio/overlays/OverlayBox.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/loci/visbio/overlays/OverlayBox.java">SVN</a></dd></dl>
 */
public class OverlayBox extends OverlayObject {

  // -- Static Fields --

  /** The names of the statistics this object reports. */
  protected static final String COORDS = "Coordinates";
  protected static final String CTR = "Center";
  protected static final String WD = "Width";
  protected static final String HT = "Height";
  protected static final String AREA = "Area";
  protected static final String PERIM = "Perimeter";
  protected static final String[] STAT_TYPES =  {COORDS, CTR, WD, HT, AREA,
    PERIM};

  // -- Constructors --

  /** Constructs an uninitialized bounding rectangle. */
  public OverlayBox(OverlayTransform overlay) { super(overlay); }

  /** Constructs a bounding rectangle. */
  public OverlayBox(OverlayTransform overlay,
    float x1, float y1, float x2, float y2)
  {
    super(overlay);
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
  }

  // -- Static methods --

  /** Returns the names of the statistics this object reports. */
  public static String[] getStatTypes() { return STAT_TYPES; }

  // -- OverlayObject API methods --

  /** Returns whether this object is drawable, i.e., is of nonzero
   *  size, area, length, etc.
   */
  public boolean hasData() { return (x1 != x2 && y1 != y2); }

  /** Gets VisAD data object representing this overlay. */
  public DataImpl getData() {
    if (!hasData()) return null;
    // don't try to render a zero-area box
    RealTupleType domain = overlay.getDomainType();
    TupleType range = overlay.getRangeType();

    float[][] setSamples = null;
    GriddedSet fieldSet = null;

    try {
      if (filled) {
        setSamples = new float[][] {
          {x1, x2, x1, x2},
          {y1, y1, y2, y2}
        };
        fieldSet = new Gridded2DSet(domain,
          setSamples, 2, 2, null, null, null, false);
      }
      else {
        setSamples = new float[][] {
          {x1, x2, x2, x1, x1},
          {y1, y1, y2, y2, y1}
        };
        fieldSet = new Gridded2DSet(domain,
          setSamples, setSamples[0].length, null, null, null, false);
      }
    }
    catch (VisADException exc) { exc.printStackTrace(); }

    Color col = selected ? GLOW_COLOR : color;
    float r = col.getRed() / 255f;
    float g = col.getGreen() / 255f;
    float b = col.getBlue() / 255f;

    float[][] rangeSamples = new float[4][setSamples[0].length];
    Arrays.fill(rangeSamples[0], r);
    Arrays.fill(rangeSamples[1], g);
    Arrays.fill(rangeSamples[2], b);
    Arrays.fill(rangeSamples[3], 1.0f);

    FlatField field = null;
    try {
      FunctionType fieldType = new FunctionType(domain, range);
      field = new FlatField(fieldType, fieldSet);
      field.setSamples(rangeSamples);
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
    return field;
  }

  /** Computes the shortest distance from this object to the given point. */
  public double getDistance(double x, double y) {
    double xdist = 0;
    if (x < x1 && x < x2) xdist = Math.min(x1, x2) - x;
    else if (x > x1 && x > x2) xdist = x - Math.max(x1, x2);
    double ydist = 0;
    if (y < y1 && y < y2) ydist = Math.min(y1, y2) - y;
    else if (y > y1 && y > y2) ydist = y - Math.max(y1, y2);
    return Math.sqrt(xdist * xdist + ydist * ydist);
  }

  /** Returns a specific statistic of this object. */
  public String getStat(String name) {
    float xx = x2 - x1;
    float yy = y2 - y1;
    float width = xx < 0 ? -xx : xx;
    float height = yy < 0 ? -yy : yy;
    float centerX = x1 + xx / 2;
    float centerY = y1 + yy / 2;
    float area = width * height;
    float perim = width + width + height + height;

    if (name.equals(COORDS)) {
      return "(" + x1 + ", " + y1 + ")-(" + x2 + ", " + y2 + ")";
    }
    else if (name.equals(CTR)) {
      return "(" + centerX + ", " + centerY + ")";
    }
    else if (name.equals(WD)) {
      return "" + width;
    }
    else if (name.equals(HT)) {
      return "" + height;
    }
    else if (name.equals(AREA)) {
      return "" + area;
    }
    else if (name.equals(PERIM)){
      return "" + perim;
    }
    else return "No such statistic for this overlay type";
  }

  /** Retrieves useful statistics about this overlay. */
  public String getStatistics() {
    float xx = x2 - x1;
    float yy = y2 - y1;
    float width = xx < 0 ? -xx : xx;
    float height = yy < 0 ? -yy : yy;
    float centerX = x1 + xx / 2;
    float centerY = y1 + yy / 2;
    float area = width * height;
    float perim = width + width + height + height;

    return "Box " + COORDS + " = (" + x1 + ", " + y1 +
      ")-(" + x2 + ", " + y2 + ")\n" +
      CTR + " = (" + centerX + ", " + centerY + ")\n" +
      WD + " = " + width + "; " + HT + " = " + height + "\n" +
      AREA + " = " + area + "; " + PERIM + " = " + perim;
  }

  /** True iff this overlay has an endpoint coordinate pair. */
  public boolean hasEndpoint() { return true; }

  /** True iff this overlay has a second endpoint coordinate pair. */
  public boolean hasEndpoint2() { return true; }

  /** True iff this overlay supports the filled parameter. */
  public boolean canBeFilled() { return true; }

  // -- Object API methods --

  /** Gets a short string representation of this overlay box. */
  public String toString() { return "Box"; }
}
