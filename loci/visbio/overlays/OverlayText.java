//
// OverlayText.java
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

import visad.*;

/** OverlayText is a text string overlay. */
public class OverlayText extends OverlayObject {

  // -- Fields --

  /** Text coordinates. */
  protected float x, y;

  /** Text string to render. */
  protected String text;


  // -- Constructor --

  /** Constructs a text string overlay. */
  public OverlayText(OverlayTransform overlay, float x, float y, String text) {
    super(overlay);
    this.x = x;
    this.y = y;
    this.text = text;
    computeGridParameters();
  }


  // -- OverlayText API methods --

  /** Changes coordinates of the text string. */
  public void setCoords(float x, float y) {
    this.x = x;
    this.y = y;
    computeGridParameters();
  }

  /** Changes text to render. */
  public void setText(String text) {
    this.text = text;
    computeGridParameters();
  }


  // -- OverlayObject API methods --

  /** Gets VisAD data object representing this overlay. */
  public DataImpl getData() {
    RealTupleType domain = overlay.getDomainType();
    TupleType range = overlay.getTextRangeType();

    float r = color.getRed() / 255f;
    float g = color.getGreen() / 255f;
    float b = color.getBlue() / 255f;

    FieldImpl field = null;
    try {
      FunctionType fieldType = new FunctionType(domain, range);
      Set fieldSet = new SingletonSet(
        new RealTuple(domain, new double[] {x, y}));
      field = new FieldImpl(fieldType, fieldSet);
      field.setSample(0,
        OverlayTransform.getTextRangeValue(text, r, g, b), false);
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
    return field;
  }

  /** Computes the shortest distance from this object to the given point. */
  public double getDistance(double x, double y) {
    double xx = this.x - x;
    double yy = this.y - y;
    return Math.sqrt(xx * xx + yy * yy);
  }

  /** True iff this overlay object returns text to render. */
  public boolean isText() { return true; }



  // -- Helper methods --

  /** Computes parameters needed for selection grid computation. */
  protected void computeGridParameters() {
    float padding = 0.03f * overlay.getScalingValue();
    float xx1 = x - padding;
    float xx2 = x + padding;
    float yy1 = y - padding;
    float yy2 = y + padding;

    xGrid1 = xx1; yGrid1 = yy1;
    xGrid2 = xx2; yGrid2 = yy1;
    xGrid3 = xx1; yGrid3 = yy2;
    xGrid4 = xx2; yGrid4 = yy2;
    horizGridCount = 2; vertGridCount = 2;
  }

}
