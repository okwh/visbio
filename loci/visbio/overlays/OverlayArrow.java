//
// OverlayArrow.java
//

/*
VisBio application for visualization of multidimensional
biological image data. Copyright (C) 2002-2004 Curtis Rueden.

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

import java.rmi.RemoteException;

import java.util.Arrays;

import loci.visbio.util.MathUtil;

import visad.*;

/** OverlayArrow is an arrow wedge overlay. */
public class OverlayArrow extends OverlayObject {

  // -- Fields --

  /** Endpoint coordinates. */
  protected float x1, y1, x2, y2;

  /** Flag indicating overlay is solid. */
  protected boolean filled;


  // -- Constructor --

  /** Constructs an arrow wedge overlay. */
  public OverlayArrow(OverlayTransform overlay,
    float x1, float y1, float x2, float y2)
  {
    super(overlay);
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
    computeGridParameters();
  }

  // -- OverlayArrow API methods --

  /** Changes coordinates of the arrow's first endpoint. */
  public void setCoords1(float x1, float y1) {
    this.x1 = x1;
    this.y1 = y1;
    computeGridParameters();
  }

  /** Changes coordinates of the arrow's second endpoint. */
  public void setCoords2(float x2, float y2) {
    this.x2 = x2;
    this.y2 = y2;
    computeGridParameters();
  }

  /** Sets whether overlay is solid. */
  public void setFilled(boolean filled) { this.filled = filled; }

  /** Gets whether overlay is solid. */
  public boolean isFilled() { return filled; }


  // -- OverlayObject API methods --

  /** Gets VisAD data object representing this overlay. */
  public DataImpl getData() {
    float padding = 0.02f * overlay.getScalingValue();
    double xx = x2 - x1;
    double yy = y2 - y1;
    double dist = Math.sqrt(xx * xx + yy * yy);
    double mult = padding / dist;
    float qx = (float) (mult * xx);
    float qy = (float) (mult * yy);

    RealTupleType domain = overlay.getDomainType();
    RealTupleType range = overlay.getRangeType();

    float[][] setSamples = {
      {x1, x2 - qy, x2 + qy, x1}, {y1, y2 + qx, y2 - qx, y1}
    };

    boolean missing = false;
    for (int i=0; i<setSamples.length; i++) {
      for (int j=0; j<setSamples[i].length; j++) {
        if (Float.isNaN(setSamples[i][j])) missing = true;
      }
    }

    GriddedSet fieldSet = null;
    try {
      if (filled && !missing) {
        fieldSet = new Gridded2DSet(domain,
          setSamples, 2, 2, null, null, null, false, false);
      }
      else {
        fieldSet = new Gridded2DSet(domain,
          setSamples, setSamples[0].length, null, null, null, false);
      }
    }
    catch (VisADException exc) { exc.printStackTrace(); }

    float r = color.getRed() / 255f;
    float g = color.getGreen() / 255f;
    float b = color.getBlue() / 255f;
    float[][] fieldSamples = new float[3][setSamples[0].length];
    Arrays.fill(fieldSamples[0], r);
    Arrays.fill(fieldSamples[1], g);
    Arrays.fill(fieldSamples[2], b);

    FlatField field = null;
    try {
      FunctionType fieldType = new FunctionType(domain, range);
      field = new FlatField(fieldType, fieldSet);
      field.setSamples(fieldSamples, false);
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
    return field;
  }

  /** Computes the shortest distance from this object to the given point. */
  public double getDistance(double x, double y) {
    return MathUtil.getDistance(new double[] {x1, y1},
      new double[] {x2, y2}, new double[] {x, y}, true);
  }


  // -- Helper methods --

  /** Computes parameters needed for selection grid computation. */
  protected void computeGridParameters() {
    float padding = 0.02f * overlay.getScalingValue();
    float[] corners1 = OverlayLine.computeCorners(x1, y1, x2, y2, padding, 2);
    float[] corners2 = OverlayLine.computeCorners(x2, y2, x1, y1, padding, 1);

    xGrid1 = corners1[0]; yGrid1 = corners1[1];
    xGrid2 = corners1[2]; yGrid2 = corners1[3];
    xGrid3 = corners2[2]; yGrid3 = corners2[3];
    xGrid4 = corners2[0]; yGrid4 = corners2[1];
    horizGridCount = 3; vertGridCount = 2;
  }

}