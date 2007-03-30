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

/** OverlayBox is a rectangle overlay. */
public class OverlayBox extends OverlayObject {

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
    computeGridParameters();
  }

  // -- OverlayObject API methods --

  /** Gets VisAD data object representing this overlay. */
  public DataImpl getData() {
    if (x1 == x2 || y1 == y2) return null; // don't try to render a zero-area box
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

    return "Box coordinates = (" + x1 + ", " + y1 +
      ")-(" + x2 + ", " + y2 + ")\n" +
      "Center = (" + centerX + ", " + centerY + ")\n" +
      "Width = " + width + "; Height = " + height + "\n" +
      "Area = " + area + "; Perimeter = " + perim;
  }

  /** True iff this overlay has an endpoint coordinate pair. */
  public boolean hasEndpoint() { return true; }

  /** True iff this overlay has a second endpoint coordinate pair. */
  public boolean hasEndpoint2() { return true; }

  /** True iff this overlay supports the filled parameter. */
  public boolean canBeFilled() { return true; }

  // -- Internal OverlayObject API methods --

  /** Computes parameters needed for selection grid computation. */
  protected void computeGridParameters() {
    float padding = 0.02f * overlay.getScalingValue();
    boolean flipX = x2 < x1;
    float xx1 = flipX ? (x1 + padding) : (x1 - padding);
    float xx2 = flipX ? (x2 - padding) : (x2 + padding);
    boolean flipY = y2 < y1;
    float yy1 = flipY ? (y1 + padding) : (y1 - padding);
    float yy2 = flipY ? (y2 - padding) : (y2 + padding);

    xGrid1 = xx1; yGrid1 = yy1;
    xGrid2 = xx2; yGrid2 = yy1;
    xGrid3 = xx1; yGrid3 = yy2;
    xGrid4 = xx2; yGrid4 = yy2;
    horizGridCount = 3; vertGridCount = 3;
  }

  // -- Object API methods --

  /** Gets a short string representation of this overlay box. */
  public String toString() { return "Box"; }

  public DataImpl getSelectionGrid() { return getSelectionGrid(false); }

  public DataImpl getSelectionGrid(boolean outline) {
    RealTupleType domain = overlay.getDomainType();
    TupleType range = overlay.getRangeType();

    float delta = GLOW_WIDTH;

    // Determine orientation of (x1, y1) relative to (x2, y2)
    // and flip if need be.
    // I've set up the code in this section based on the 
    // suppositioin that the box is oriented like this:
    //
    // (x1, y1) +--------+
    //          |        |
    //          |        |
    //          +--------+ (x2, y2)
    //
    // which means x1 is supposed to be _less_ than x2, but inconsistently,
    // y1 is supposed to be _greater_ than y2.
    float[][] c;
    boolean flipX = x2 > x1;
    float xx1 = flipX ? x1 : x2;
    float xx2 = flipX ? x2 : x1;
    boolean flipY = y2 < y1;
    float yy1 = flipY ? y1 : y2;
    float yy2 = flipY ? y2 : y1;

    int rangeSamplesLength = 16;
    SampledSet domainSet = null;
    if (2 * delta > xx2 - xx1 || 2 * delta > yy1 - yy2) {
      // if box is narrower than twice the width the highlighting
      // band would be,
      // just throw down a translucent rectangle over the box

      rangeSamplesLength = 4;
      
      float[][] setSamples = {
        {xx1 - delta, xx2 + delta, xx1 - delta, xx2 + delta},
        {yy1 + delta, yy1 + delta, yy2 - delta, yy2 - delta}};

      try {
        domainSet = new Gridded2DSet(domain, setSamples, 2, 2,
          null, null, null, false);
      }
      catch (VisADException ex) { ex.printStackTrace(); }
    }
    else {
      // construct a trapezoidal highlighting band for each of the 
      // four line segments which constitute the box
      c = new float[][]{{xx1, yy1}, {xx2, yy2}};

      float[][] s1 = 
        {{c[0][0] - delta, c[1][0] + delta, c[0][0] + delta, c[1][0] - delta},
        {c[0][1] + delta, c[0][1] + delta, c[0][1] - delta, c[0][1] - delta}};
      float[][] s2 = 
        {{c[1][0] + delta, c[1][0] + delta, c[1][0] - delta, c[1][0] - delta},
        {c[0][1] + delta, c[1][1] - delta, c[0][1] - delta, c[1][1] + delta}};
      float[][] s3 = 
        {{c[1][0] + delta, c[0][0] - delta, c[1][0] - delta, c[0][0] + delta},
        {c[1][1] - delta, c[1][1] - delta, c[1][1] + delta, c[1][1] + delta}};
      float[][] s4 = 
        {{c[0][0] - delta, c[0][0] - delta, c[0][0] + delta, c[0][0] + delta},
        {c[1][1] - delta, c[0][1] + delta, c[1][1] + delta, c[0][1] - delta}};

      float[][][] setSamples = {s1, s2, s3, s4};

      Gridded2DSet[] segments = new Gridded2DSet[4];
      for (int i=0; i<4; i++) {
        Gridded2DSet segment = null;
        try {
          segment = new Gridded2DSet (domain, setSamples[i], 2, 2, null, null,
              null,
              false);
        }
        catch (VisADException exc) { exc.printStackTrace(); }
        segments[i] = segment;
      }

      try { domainSet = new UnionSet(domain, segments); }
      catch (VisADException exc) { exc.printStackTrace(); }
    } // end else 

    //************************************************************** 
    
    // construct range samples
    float[][] rangeSamples = new float[4][rangeSamplesLength];
    float r = GLOW_COLOR.getRed() / 255f;
    float g = GLOW_COLOR.getGreen() / 255f;
    float b = GLOW_COLOR.getBlue() / 255f;
    Arrays.fill(rangeSamples[0], r);
    Arrays.fill(rangeSamples[1], g);
    Arrays.fill(rangeSamples[2], b);
    Arrays.fill(rangeSamples[3], GLOW_ALPHA);

    // construct field
    FlatField field = null;
    try {
      FunctionType fieldType = new FunctionType(domain, range);
      field = new FlatField(fieldType, domainSet);
      field.setSamples(rangeSamples);
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }

    return field;
  }
}
