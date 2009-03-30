//
// ViewHandler.java
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

package loci.visbio.view;

import java.rmi.RemoteException;
import loci.visbio.VisBioFrame;
import loci.visbio.data.DataTransform;
import loci.visbio.data.ImageTransform;
import loci.visbio.state.*;
import loci.visbio.util.*;
import org.w3c.dom.Element;
import visad.*;
import visad.java2d.DisplayImplJ2D;

/**
 * Provides logic for controlling a VisAD display's view.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/visbio/src/loci/visbio/view/ViewHandler.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/visbio/src/loci/visbio/view/ViewHandler.java">SVN</a></dd></dl>
 */
public class ViewHandler implements Saveable {

  // -- Constants --

  /** Default zoom factor for 2D displays. */
  public static final double DEFAULT_ZOOM_2D = 0.90;

  /** Default zoom factor for 3D displays. */
  public static final double DEFAULT_ZOOM_3D = 0.5;

  /** Default rotation factor for 3D displays.  */
  public static final double DEFAULT_ROTATION = 80;

  /** How far display zooms in or out each time. */
  public static final double ZOOM_AMOUNT = 1.5;

  /** How far display rotates each time. */
  public static final double ROTATION_AMOUNT = 15;

  /** How far display pans each time. */
  public static final double PAN_AMOUNT = 0.25;

  // -- Fields --

  /** Associated display window. */
  protected DisplayWindow window;

  /** Aspect ratio of the display. */
  protected double xasp, yasp, zasp;

  /** Whether scale is visible. */
  protected boolean showScale;

  /** Whether bounding box is visible. */
  protected boolean boundingBox;

  /** Whether display is in parallel projection mode. */
  protected boolean parallel;

  // -- Fields - initial state --

  /** Matrix representing current display projection. */
  protected double[] matrix;

  // -- GUI components --

  /** GUI controls for view handler. */
  protected ViewPanel panel;

  // -- Constructor --

  /** Creates a display view handler. */
  public ViewHandler(DisplayWindow dw) {
    window = dw;

    // default view settings
    xasp = yasp = zasp = 1.0;
    showScale = false;
    boundingBox = true;
    parallel = false;
  }

  // -- ViewHandler API methods --

  /** Gets associated display window. */
  public DisplayWindow getWindow() { return window; }

  /** Gets GUI controls for this view handler. */
  public ViewPanel getPanel() { return panel; }

  /** Zooms in on the display. */
  public void zoomIn() { zoom(ZOOM_AMOUNT); }

  /** Zooms out on the display. */
  public void zoomOut() { zoom(1 / ZOOM_AMOUNT); }

  /** Rotates the display clockwise (2D and 3D). */
  public void rotateClockwise() { rotate(0, 0, ROTATION_AMOUNT); }

  /** Rotates the display counterclockwise (2D and 3D). */
  public void rotateCounterclockwise() { rotate(0, 0, -ROTATION_AMOUNT); }

  /** Rotates the display to the left (3D only). */
  public void rotateLeft() { rotate(0, ROTATION_AMOUNT, 0); }

  /** Rotates the display to the right (3D only). */
  public void rotateRight() { rotate(0, -ROTATION_AMOUNT, 0); }

  /** Rotates the display upward (3D only). */
  public void rotateUp() { rotate(ROTATION_AMOUNT, 0, 0); }

  /** Rotates the display downward (3D only). */
  public void rotateDown() { rotate(-ROTATION_AMOUNT, 0, 0); }

  /** Slides the display to the left. */
  public void panLeft() { pan(-PAN_AMOUNT, 0, 0); }

  /** Slides the display to the right. */
  public void panRight() { pan(PAN_AMOUNT, 0, 0); }

  /** Slides the display upward. */
  public void panUp() { pan(0, PAN_AMOUNT, 0); }

  /** Slides the display downward. */
  public void panDown() { pan(0, -PAN_AMOUNT, 0); }

  /** Zooms the display by the given amount. */
  public void zoom(double scale) {
    double[] zoom = window.getDisplay().make_matrix(0, 0, 0, scale, 0, 0, 0);
    applyMatrix(zoom);
  }

  /** Rotates the display in the given direction. */
  public void rotate(double rotx, double roty, double rotz) {
    double[] rotate = window.getDisplay().make_matrix(
      rotx, roty, rotz, 1, 0, 0, 0);
    applyMatrix(rotate);
  }

  /** Pans the display in the given direction. */
  public void pan(double panx, double pany, double panz) {
    double[] pan = window.getDisplay().make_matrix(
      0, 0, 0, 1, panx, pany, panz);
    applyMatrix(pan);
  }

  /** Applies the given matrix transform to the display. */
  public void applyMatrix(double[] m) {
    DisplayImpl display = window.getDisplay();
    try {
      ProjectionControl control = display.getProjectionControl();
      double[] oldMatrix = control.getMatrix();
      double[] newMatrix = display.multiply_matrix(m, oldMatrix);
      control.setMatrix(newMatrix);
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
    VisBioFrame bio = window.getVisBio();
    bio.generateEvent(bio.getManager(DisplayManager.class),
      "adjust orientation for " + window.getName(), true);
  }

  /** Gets the display's current matrix transform. */
  public double[] getMatrix() {
    DisplayImpl display = window.getDisplay();
    return display == null ? matrix :
      display.getProjectionControl().getMatrix();
  }

  /** Restores the display's zoom and orientation to the original values. */
  public void reset() {
    DisplayImpl display = window.getDisplay();
    if (display instanceof DisplayImplJ2D) {
      try { display.getProjectionControl().resetProjection(); }
      catch (VisADException exc) { exc.printStackTrace(); }
      catch (RemoteException exc) { exc.printStackTrace(); }
      doAspect(xasp, yasp, zasp);
    }
    else setMatrix(makeDefaultMatrix());
    VisBioFrame bio = window.getVisBio();
    bio.generateEvent(bio.getManager(DisplayManager.class),
      "reset orientation for " + window.getName(), true);
  }

  /** Guesses aspect ratio based on first linked data transform. */
  public void guessAspect() {
    TransformHandler transformHandler = window.getTransformHandler();
    DataTransform[] trans = transformHandler.getTransforms();
    double x = 1, y = 1, z = 1;
    for (int i=0; i<trans.length; i++) {
      if (!(trans[i] instanceof ImageTransform)) continue;
      ImageTransform it = (ImageTransform) trans[i];
      int w = it.getImageWidth();
      int h = it.getImageHeight();
      TransformLink link = (TransformLink) transformHandler.getLink(it);
      int axis = link instanceof StackLink ?
        ((StackLink) link).getStackAxis() : -1;
      int numSlices = axis < 0 ? 1 : it.getLengths()[axis];
      double mw = it.getMicronWidth();
      double mh = it.getMicronHeight();
      double ms = it.getMicronStep(axis);
      x = mw == mw ? mw : w;
      y = mh == mh ? mh : h;
      z = ms == ms ? (ms * (numSlices - 1)) : (x < y ? x : y);
      break;
    }
    panel.setAspect(x, y, z);
  }

  /** Adjusts the aspect ratio. */
  public void setAspect(double x, double y, double z) {
    if (x != x || x <= 0) x = 1;
    if (y != y || y <= 0) y = 1;
    double d = x > y ? x : y;
    double xx = x / d;
    double yy = y / d;
    double zz = (z == z && z > 0) ? z / d : 1.0;
    doAspect(xx, yy, zz);
    xasp = xx;
    yasp = yy;
    zasp = zz;
    VisBioFrame bio = window.getVisBio();
    bio.generateEvent(bio.getManager(DisplayManager.class),
      "adjust aspect ratio for " + window.getName(), true);
  }

  /** Gets aspect ratio X component. */
  public double getAspectX() { return xasp; }

  /** Gets aspect ratio Y component. */
  public double getAspectY() { return yasp; }

  /** Gets aspect ratio Z component. */
  public double getAspectZ() { return zasp; }

  /** Toggles visibility of scale. */
  public void toggleScale(boolean value) {
    showScale = value;

    GraphicsModeControl gmc = window.getDisplay().getGraphicsModeControl();
    try { gmc.setScaleEnable(value); }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
    VisBioFrame bio = window.getVisBio();
    String endis = value ? "enable" : "disable";
    bio.generateEvent(bio.getManager(DisplayManager.class),
      endis + " scale for " + window.getName(), true);
  }

  /** Gets visibility of scale. */
  public boolean isScale() { return showScale; }

  /** Toggles visibility of the display's bounding box. */
  public void toggleBoundingBox(boolean value) {
    boundingBox = value;

    DisplayRenderer dr = window.getDisplay().getDisplayRenderer();
    try { dr.setBoxOn(value); }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
    VisBioFrame bio = window.getVisBio();
    String endis = value ? "enable" : "disable";
    bio.generateEvent(bio.getManager(DisplayManager.class),
      endis + " bounding box for " + window.getName(), true);
  }

  /** Gets visibility of the display's bounding box. */
  public boolean isBoundingBox() { return boundingBox; }

  /** Toggles whether 3D display uses a parallel projection. */
  public void toggleParallel(boolean value) {
    if (!window.is3D()) return;
    parallel = value;

    DisplayUtil.setParallelProjection(window.getDisplay(), parallel);
    VisBioFrame bio = window.getVisBio();
    String endis = value ? "enable" : "disable";
    bio.generateEvent(bio.getManager(DisplayManager.class),
      endis + " parallel projection mode for " + window.getName(), true);
  }

  /** Gets whether 3D display uses a parallel projection. */
  public boolean isParallel() { return parallel; }

  // -- ViewHandler API methods - state logic --

  /** Tests whether two objects are in equivalent states. */
  public boolean matches(ViewHandler handler) {
    return ObjectUtil.arraysEqual(getMatrix(), handler.getMatrix()) &&
      xasp == handler.xasp &&
      yasp == handler.yasp &&
      zasp == handler.zasp &&
      showScale == handler.showScale &&
      boundingBox == handler.boundingBox &&
      parallel == handler.parallel;
  }

  /**
   * Modifies this object's state to match that of the given object.
   * If the argument is null, the object is initialized according to
   * its current state instead.
   */
  public void initState(ViewHandler handler) {
    if (handler != null) {
      matrix = handler.getMatrix();
      xasp = handler.xasp;
      yasp = handler.yasp;
      zasp = handler.zasp;
      showScale = handler.showScale;
      boundingBox = handler.boundingBox;
      parallel = handler.parallel;
    }

    if (matrix == null) matrix = makeDefaultMatrix();

    setMatrix(matrix);
    toggleScale(showScale);
    toggleBoundingBox(boundingBox);
    toggleParallel(parallel);

    // configure eye separation distance
    OptionManager om = (OptionManager)
      window.getVisBio().getManager(OptionManager.class);
    NumericOption eye = (NumericOption)
      om.getOption(DisplayManager.EYE_DISTANCE);
    double position = eye.getFloatingValue();
    DisplayUtil.setEyeSeparation(window.getDisplay(), position);

    if (panel == null) panel = new ViewPanel(this);
  }

  // -- Saveable API methods --

  /** Writes the current state to the given DOM element ("Display"). */
  public void saveState(Element el) throws SaveException {
    Element child = XMLUtil.createChild(el, "Appearance");
    child.setAttribute("matrix", ObjectUtil.arrayToString(getMatrix()));
    child.setAttribute("aspectX", "" + xasp);
    child.setAttribute("aspectY", "" + yasp);
    child.setAttribute("aspectZ", "" + zasp);
    child.setAttribute("showScale", "" + showScale);
    child.setAttribute("boundingBox", "" + boundingBox);
    child.setAttribute("parallel", "" + parallel);
  }

  /** Restores the current state from the given DOM element ("Display"). */
  public void restoreState(Element el) throws SaveException {
    Element child = XMLUtil.getFirstChild(el, "Appearance");
    matrix = ObjectUtil.stringToDoubleArray(child.getAttribute("matrix"));
    xasp = ObjectUtil.stringToDouble(child.getAttribute("aspectX"));
    yasp = ObjectUtil.stringToDouble(child.getAttribute("aspectY"));
    zasp = ObjectUtil.stringToDouble(child.getAttribute("aspectZ"));
    showScale = child.getAttribute("showScale").equalsIgnoreCase("true");
    boundingBox = child.getAttribute("boundingBox").equalsIgnoreCase("true");
    parallel = child.getAttribute("parallel").equalsIgnoreCase("true");
  }

  // -- Helper methods --

  /** Adjusts the aspect ratio. */
  protected void doAspect(double x, double y, double z) {
    DisplayImpl display = window.getDisplay();
    ProjectionControl pc = display.getProjectionControl();
    if (display instanceof DisplayImplJ2D) {
      try { pc.setAspect(new double[] {x, y}); }
      catch (VisADException exc) { exc.printStackTrace(); }
      catch (RemoteException exc) { exc.printStackTrace(); }
      return;
    }

    // get old projection matrix
    double[] oldMatrix = pc.getMatrix();

    // clear old aspect ratio from projection matrix
    double[] undoOldAspect = {
      1 / xasp, 0, 0, 0,
      0, 1 / yasp, 0, 0,
      0, 0, 1 / zasp, 0,
      0, 0, 0, 1
    };
    double[] newMatrix = display.multiply_matrix(oldMatrix, undoOldAspect);

    // compute new aspect ratio matrix
    double[] newAspect = {
      x, 0, 0, 0,
      0, y, 0, 0,
      0, 0, z, 0,
      0, 0, 0, 1
    };
    newMatrix = display.multiply_matrix(newMatrix, newAspect);

    // apply new projection matrix
    try { pc.setMatrix(newMatrix); }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
  }

  /** Creates a matrix at the default orientation for the display. */
  protected double[] makeDefaultMatrix() {
    DisplayImpl display = window.getDisplay();
    if (display == null || display instanceof DisplayImplJ2D) return null;

    double scale = window.is3D() ? DEFAULT_ZOOM_3D : DEFAULT_ZOOM_2D;
    double rotate = window.is3D() ? DEFAULT_ROTATION : 0;
    double[] newMatrix = display.make_matrix(rotate, 0, 0, scale, 0, 0, 0);
    double[] aspect = new double[] {xasp,
      0, 0, 0, 0, yasp, 0, 0, 0, 0, zasp, 0, 0, 0, 0, 1};
    return display.multiply_matrix(newMatrix, aspect);
  }

  /** Sets the display's projection matrix to match the given one. */
  protected void setMatrix(double[] m) {
    DisplayImpl display = window.getDisplay();
    if (display != null) {
      try { display.getProjectionControl().setMatrix(m); }
      catch (VisADException exc) { exc.printStackTrace(); }
      catch (RemoteException exc) { exc.printStackTrace(); }
    }
  }

}
