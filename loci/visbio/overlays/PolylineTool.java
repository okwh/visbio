//
// PolylineTool.java
//

/*
VisBio application for visualization of multidimensional
biological image data. Copyright (C) 2002-@year@ Greg Meyer.

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

import java.awt.event.InputEvent;
import java.util.Vector;
import loci.visbio.data.TransformEvent;
import loci.visbio.util.DisplayUtil;
import loci.visbio.util.MathUtil;
import visad.DisplayEvent;
import visad.DisplayImpl;

/** FreeformTool is the tool for creating freeform objects. */
public class PolylineTool extends OverlayTool {

  // -- Constants --



  // -- Fields --

  /** Curve currently being drawn or modified. */
  protected OverlayPolyline line;

  /** Curve close to the mouse when drawing is not occurring */
  protected OverlayPolyline near;

  /** Nearest node on Polyline near */
  protected int nearNode;

  /** Whether the active node of the polyline is anchored. */
  protected boolean anchored;

  /** Whether the mouse is currently near the last node of 
   *  the active polyline. */
  protected boolean nearTail;

  /** Whether the mouse is currently near the first node of 
   *  the active polyline. */
  protected boolean nearHead;

  /** Whether the mouse has moved sufficiently far from the 
   *  last node placed. */
  protected boolean departed;

  // -- Constructor --

  /** Constructs a creation tool. */
  public PolylineTool(OverlayTransform overlay) {
    super(overlay, "Polyline", "Polyline", "polyline.png");
    line = null;
    anchored = false;
    nearTail = false;
    nearHead = false;
    departed = false;
    unsetMouseOverNode();
  }

  // -- OverlayTool API methods --

  /** Instructs this tool to respond to a mouse press. */
  public void mouseDown(DisplayEvent e, int px, int py,
    float dx, float dy, int[] pos, int mods)
  {
    // -- housekeeping
    boolean ctl = (mods & InputEvent.CTRL_MASK) != 0;
    DisplayImpl display = (DisplayImpl) e.getDisplay();

    double dpx = (double) px;
    double dpy = (double) py;

    // -- action!
    deselectAll(); 
   
    if (line == null) {
      if (!isMouseOverNode()) {
        line =  new OverlayPolyline(overlay, dx, dy, dx, dy);
        configureOverlay(line);
        overlay.addObject(line, pos);
      }
    }
    else {
      line.setActiveDisplay (display);

      if (nearTail) {
        if (!anchored) line.deleteNode(line.getNumNodes()-1);
        releaseLine();
        //System.out.println("nearTail, ending line"); // TEMP
      }
      else if (nearHead) {
        if (anchored) System.out.println("puzzling case is here");
        else {
          float[] c = line.getNodeCoords(0);
          line.setLastNode(c[0], c[1]);
          releaseLine(); 
          //System.out.println("nearHead, ending line"); // TEMP
        }
      }
      else {
        nearTail = true;
        if (anchored) {
          line.setNextNode(dx, dy);
        }
        else { 
          line.setNodeCoords(line.getNumNodes() - 1, dx, dy);
        }
        line.computeLength();
      }
    }

    anchored = true;
    departed = false;

    overlay.notifyListeners(new TransformEvent(overlay));
  } // end mouseDown

  /** Instructs this tool to respond to a mouse drag. */
  public void mouseDrag(DisplayEvent e, int px, int py,
    float dx, float dy, int[] pos, int mods) {
    // System.out.println("mouseDrag"); // TEMP
    if (isMouseOverNode()) {
      near.setNodeCoords(nearNode, dx, dy);
      overlay.notifyListeners(new TransformEvent(overlay));
    } 
    else {
      mouseMoved(e, px, py, dx, dy, pos, mods);
    }
  } 

  /** Instructs this tool to respond to a mouse release. */
  public void mouseUp(DisplayEvent e, int px, int py, 
      float dx, float dy, int[] pos, int mods) {
    if (isMouseOverNode()) {
      // System.out.println("mouseUp"); // TEMP
      near.updateBoundingBox();
      near.computeGridParameters();
      unsetMouseOverNode();
    }
  }

  /** Instructs this tool to respond to a mouse movement. */
  public void mouseMoved(DisplayEvent e, int px, int py, 
      float dx, float dy, int[] pos, int mods) {
    DisplayImpl display = (DisplayImpl) e.getDisplay();
    if (line != null) {
      line.setActiveDisplay(display);

      nearTail = false;
      nearHead = false;

      if (anchored) {
        line.setNextNode(dx, dy);
        anchored = false;
        nearTail = true;
      }
      else {
        line.setLastNode(dx, dy);
      }

      // determine whether to highlight head or tail nodes
      double[] movePxl = {(double) px, (double) py}; 
      float[] prevDom = line.getNodeCoords(line.getNumNodes() - 2);
      double[] prevDomDbl = {(double) prevDom[0], (double) prevDom[1]}; 
      int[] prevPxlInt = DisplayUtil.domainToPixel(display, prevDomDbl);
      double[] prevPxl = {(double) prevPxlInt[0], (double) prevPxlInt[1]};
      double tailDist = MathUtil.getDistance(movePxl, prevPxl);

      float[] headDom = line.getNodeCoords(0);
      double[] headDomDbl = {(double) headDom[0], (double) headDom[1]};
      int[] headPxlInt = DisplayUtil.domainToPixel(display, headDomDbl);
      double[] headPxl = {(double) headPxlInt[0], (double) headPxlInt[1]};
      double headDist = MathUtil.getDistance(movePxl, headPxl);

      if (tailDist > 10.0) departed = true;

      if (headDist < 2.0) {
        line.setHighlightNode(0);
        //System.out.println("near head"); // TEMP
        nearHead = true;
      }
      else if (departed && tailDist < 2.0) {
        line.setHighlightNode(line.getNumNodes() - 2);
        //System.out.println("near tail");// TEMP
        nearTail = true;
      }
      else {
        line.turnOffHighlighting();
      }
    }
    else {
      // find out if you're near a node
      OverlayObject[] objects = overlay.getObjects();
      double threshold = 2.0;
      int[] objNode =  getNearestNode(display, objects, px, py, threshold);

      unHighlightAllPolylines(objects);
      if (objNode != null) {
        int obj = objNode[0];
        int node = objNode[1];
        //System.out.println("near node " + node + " of object " + obj); // TEMP 
        OverlayPolyline pln = (OverlayPolyline) objects[obj];
        pln.setHighlightNode(node);
        setMouseOverNode(pln, node);
      }
      else if (isMouseOverNode()) {
        unsetMouseOverNode();
      }
    }
    overlay.notifyListeners(new TransformEvent(overlay));
  }

  // -- Helper methods -- 
  
  private void unsetMouseOverNode() {
    near = null;
    nearNode = -1;
  }
  
  private void setMouseOverNode(OverlayPolyline pln, int node) {
    near = pln;
    nearNode = node;
  }

  private boolean isMouseOverNode() { return (near != null); }
  
  /** Unhighlights all polylines in a list of OverlayObjects */
  private void unHighlightAllPolylines(OverlayObject[] objects) {
    for (int i=0; i<objects.length; i++) {
      if (objects[i] instanceof OverlayPolyline) 
        ((OverlayPolyline) objects[i]).turnOffHighlighting();
    }
  }

  /** Finds nearest (subject to a threshold) node of all polylines
   *  in a list of OverlayObjects.
   *  @param objects An array of OverlayObjects on this display
   *  Returns an array int[2], with item 0 the index of the nearest polyline
   *  in the objects array, item 1 the index of the nearest node in the nearest
   *  polyline */
  private int[] getNearestNode(DisplayImpl display,
      OverlayObject[] objects, int px, int py, double threshold) {
    Vector polylines = new Vector();
    Vector indices = new Vector();
    double[] p = {(double) px, (double) py};

    for (int i=0; i<objects.length; i++) {
      if (objects[i] instanceof OverlayPolyline) {
        polylines.add(objects[i]);
        indices.add(new Integer(i));
      }
    }

    int nearestPline = -1;
    int nearestNode = -1;
    double minDist = Double.POSITIVE_INFINITY;
    for (int i=0; i<polylines.size(); i++) {
      OverlayPolyline pln = (OverlayPolyline) polylines.get(i);
      for (int j=0; j<pln.getNumNodes(); j++) {
        float[] c = pln.getNodeCoords(j);
        double[] cDbl = {c[0], c[1]}; // auto cast
        int[] cPxl = DisplayUtil.domainToPixel(display, cDbl);
        double[] cPxlDbl = {(double) cPxl[0], (double) cPxl[1]};
        double dist = MathUtil.getDistance (cPxlDbl, p);
        if (dist < minDist && dist < threshold) {
          minDist = dist;
          nearestPline = ((Integer) indices.get(i)).intValue();
          nearestNode = j;
        }
      }
    }

    if (nearestPline == -1) return null;
    else return new int[]{nearestPline, nearestNode};
  }
  

  
  /** Ends drawing of the current line */
  private void releaseLine() {
    if (line != null) {
      line.turnOffHighlighting();
      line.updateBoundingBox();
      line.computeGridParameters();
      line.computeLength();
      line.setDrawing(false);
      line.setSelected(false);
      line = null;
    }
  }

  /** Casts an array of floats to doubles */
  private double[][] floatsToPixelDoubles(DisplayImpl d, float[][] nodes) {
    double[][] nodesDbl = new double[nodes.length][nodes[0].length];
    for (int j=0; j<nodes[0].length; j++) {
      int[] c = DisplayUtil.domainToPixel(d, new double[]{
        (double) nodes[0][j], (double) nodes[1][j]});
      nodesDbl[0][j] = (double) c[0];
      nodesDbl[1][j] = (double) c[1];
    }
    return nodesDbl;
  }

  /** Prints node array of current freeform; for debugging */
  private void printNodes(float[][] nodes) {
    System.out.println("Printing nodes...");
    for (int i = 0; i < nodes[0].length; i++){
      System.out.println(i+":("+nodes[0][i]+","+nodes[1][i]+")");
    }
  }

  /** Prints node array of current freeform; for debugging */
  private void printNodes() {
    if (line!=null){
      float[][] nodes = line.getNodes();
      printNodes(nodes);
    }
  }

  /** Prints a message for debugging */
  public void print(String methodName, String message) {
    boolean toggle = true;
    if (toggle) {
      String header = "FreeformTool.";
      String out = header + methodName + ": " + message;
      System.out.println(out);
    }
  }
} // end class FreeformTool