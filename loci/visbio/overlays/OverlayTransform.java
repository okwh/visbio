//
// OverlayTransform.java
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

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.rmi.RemoteException;
import java.util.*;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import loci.formats.FormatTools;
import loci.visbio.data.*;
import loci.visbio.state.Dynamic;
import loci.visbio.util.ObjectUtil;
import loci.visbio.view.DisplayWindow;
import loci.visbio.view.TransformLink;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import visad.*;
import visad.util.CursorUtil;

/** A set of overlays on top another transform. */
public class OverlayTransform extends DataTransform
  implements TransformListener
{

  // -- Constants --

  /** MathType for red color mappings. */
  protected static final RealType RED_TYPE =
    RealType.getRealType("overlay_red");

  /** MathType for green color mappings. */
  protected static final RealType GREEN_TYPE =
    RealType.getRealType("overlay_green");

  /** MathType for blue color mappings. */
  protected static final RealType BLUE_TYPE =
    RealType.getRealType("overlay_blue");

  /** MathType for alpha color mappings. */
  protected static final RealType ALPHA_TYPE =
    RealType.getRealType("overlay_alpha");

  /** Overlay range type. */
  protected static final RealTupleType RANGE_TUPLE = makeRangeTuple();

  /** Constructs the overlay range type for most overlays. */
  protected static RealTupleType makeRangeTuple() {
    RealTupleType rtt = null;
    try {
      rtt = new RealTupleType(new RealType[] {
        RED_TYPE, GREEN_TYPE, BLUE_TYPE, ALPHA_TYPE
      });
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    return rtt;
  }

  /** String indicating a given field is not applicable to an overlay. */
  protected static final String NOT_APPLICABLE = "N/A";

  // -- Fields --

  /** Controls for creating overlays. */
  protected OverlayWidget controls;

  /** List of overlays for each dimensional position. */
  protected Vector[] overlays;

  /** Current dimensional position. */
  protected int[] pos;

  /** MathType for Text mappings. */
  protected TextType textType;

  /** Overlay range type for text overlays. */
  protected TupleType textRangeTuple;

  /** Whether left mouse button is currently being pressed. */
  protected boolean mouseDownLeft;

  /** Whether right mouse button is currently being pressed. */
  protected boolean mouseDownRight;

  /** Font metrics for the current font. */
  protected FontMetrics fontMetrics;

  /** Whether to draw text. */
  protected boolean drawText = true;

  /** Clipboard stores overlays grabbed with the copy function. */
  protected Vector clipboard = new Vector();

  /** Dimensional position of overlays stored in the clipboard. */
  protected int[] clipboardPos = null;

  /** Transient Select Box, if active */
  protected TransientSelectBox selectBox;

  /** Type of last tool used */
  protected String lastToolType;

  /** Whether last tool has changed */
  protected boolean toolChanged;

  // -- Constructor --

  /** Creates an overlay object for the given transform. */
  public OverlayTransform(DataTransform parent, String name) {
    super(parent, name);
    // text type is unique to each transform instance so that
    // each transform can have its own font settings
    textType = TextType.getTextType("overlay_text_" + transformId);
    try {
      textRangeTuple = new TupleType(new ScalarType[] {
        textType, RED_TYPE, GREEN_TYPE, BLUE_TYPE, ALPHA_TYPE
      });
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    initState(null);
    parent.addTransformListener(this);

    lastToolType = "";
    toolChanged = false;
  }

  // -- OverlayTransform API methods --

  /** Adds an overlay object at the current dimensional position. */
  public void addObject(OverlayObject obj) { addObject(obj, pos); }

  /** Adds an overlay object at the given dimensional position. */
  public void addObject(OverlayObject obj, int[] pos) {
    configureOverlay(obj);
    int ndx = FormatTools.positionToRaster(lengths, pos);
    if (ndx < 0 || ndx >= overlays.length) return;
    synchronized (overlays) {
      overlays[ndx].add(obj);
    }
    if (ObjectUtil.arraysEqual(pos, this.pos)) controls.refreshListObjects();
    notifyListeners(new TransformEvent(this));
  }

  /** Removes an overlay object at the current dimensional position. */
  public void removeObject(OverlayObject obj) { removeObject(obj, pos); }

  /** Removes an overlay object at the given dimensional position. */
  public void removeObject(OverlayObject obj, int[] pos) {
    int ndx = FormatTools.positionToRaster(lengths, pos);
    if (ndx < 0 || ndx >= overlays.length) return;
    synchronized (overlays) {
      overlays[ndx].remove(obj);
    }
    if (ObjectUtil.arraysEqual(pos, this.pos)) controls.refreshListObjects();
    notifyListeners(new TransformEvent(this));
  }

  /** Removes selected overlay objects at the current dimensional position. */
  public void removeSelectedObjects() { removeSelectedObjects(pos); }

  /** Adds a transient selection box. */
  public void addTSB (TransientSelectBox tsb) { selectBox = tsb; }

  /** Removes transient selection box. */
  public void removeTSB () { selectBox = null; }

  /** Removes selected overlay objects at the given dimensional position. */
  public void removeSelectedObjects(int[] pos) {
    int ndx = FormatTools.positionToRaster(lengths, pos);
    if (ndx < 0 || ndx >= overlays.length) return;
    boolean anyRemoved = false;
    int i = 0;
    synchronized (overlays) {
      while (i < overlays[ndx].size()) {
        OverlayObject obj = (OverlayObject) overlays[ndx].elementAt(i);
        if (obj.isSelected()) {
          overlays[ndx].removeElementAt(i);
          anyRemoved = true;
        }
        else i++;
      }
    }
    if (anyRemoved) {
      if (ObjectUtil.arraysEqual(pos, this.pos)) controls.refreshListObjects();
      notifyListeners(new TransformEvent(this));
    }
  }

  /**
   * Copies selected overlay objects at the current dimensional position
   * to the clipboard.
   */
  public void copySelectedObjects() { copySelectedObjects(pos); }

  /**
   * Copies selected overlay objects at the given dimensional position
   * to the clipboard.
   */
  public void copySelectedObjects(int[] pos) {
    int ndx = FormatTools.positionToRaster(lengths, pos);
    if (ndx < 0 || ndx >= overlays.length) return;
    synchronized (overlays) {
      clipboard.removeAllElements();
      clipboardPos = pos;
      for (int i=0; i<overlays[ndx].size(); i++) {
        OverlayObject obj = (OverlayObject) overlays[ndx].elementAt(i);
        if (obj.isSelected()) clipboard.add(obj);
      }
      controls.refreshPasteComponent(!clipboard.isEmpty());
    }
  }

  /** Pastes copied objects at the current dimensional position. */
  public void pasteObjects() { pasteObjects(pos); }

  /** Pastes copied objects at the given dimensional position. */
  public void pasteObjects(int[] pos) {
    int ndx = FormatTools.positionToRaster(lengths, pos);
    if (ndx < 0 || ndx >= overlays.length) return;
    synchronized (overlays) {
      if (clipboard.isEmpty()) return;
      for (int i=0; i<clipboard.size(); i++) {
        OverlayObject orig = (OverlayObject) clipboard.elementAt(i);
        OverlayObject obj =
          OverlayIO.createOverlay(orig.getClass().getName(), this);
        obj.x1 = orig.x1;
        obj.y1 = orig.y1;
        obj.x2 = orig.x2;
        obj.y2 = orig.y2;
        obj.text = orig.text;
        obj.color = orig.color;
        obj.filled = orig.filled;
        obj.group = orig.group;
        obj.notes = orig.notes;
        obj.drawing = false;
        obj.selected = true;
        if (obj instanceof OverlayText)
          ((OverlayText) obj).computeTextBounds();
        overlays[ndx].add(obj);
      }
    }
    controls.refreshListObjects();
    notifyListeners(new TransformEvent(this));
  }

  /**
   * Distributes one object per dimensional position linearly between the
   * position of the overlay currently on the clipboard, and the one selected
   * at the current dimensional position.
   *
   * Since there are a number of criteria necessary to perform this task
   * properly, it returns an error string if there was a problem, or null if
   * the operation was successful.
   */
  public String distributeObjects() { return distributeObjects(pos); }

  /**
   * Distributes one object per dimensional position linearly between the
   * position of the overlay currently on the clipboard, and the one selected
   * at the given dimensional position.
   *
   * Since there are a number of criteria necessary to perform this task
   * properly, it returns an error string if there was a problem, or null if
   * the operation was successful.
   */
  public String distributeObjects(int[] pos) {
    int ndx = FormatTools.positionToRaster(lengths, pos);
    if (ndx < 0 || ndx >= overlays.length) {
      return "Invalid dimensional position.";
    }

    synchronized (overlays) {
      // grab overlay from the clipboard
      int size = clipboard.size();
      if (size == 0) {
        return "You must first copy an overlay to the clipboard.";
      }
      else if (size > 1) {
        return "There must not be multiple overlays on the clipboard.";
      }
      OverlayObject clip = (OverlayObject) clipboard.firstElement();

      // grab currently selected overlay
      OverlayObject sel = null;
      for (int i=0; i<overlays[ndx].size(); i++) {
        OverlayObject obj = (OverlayObject) overlays[ndx].elementAt(i);
        if (obj.isSelected()) {
          if (sel != null) {
            return "There must not be multiple overlays selected.";
          }
          sel = obj;
        }
      }
      if (sel == null) return "There must be an overlay selected.";

      // ensure matching types
      if (!clip.getClass().equals(sel.getClass())) {
        return "The overlay on the clipboard must " +
          "be the same kind as the selected overlay.";
      }

      // check dimensional positions
      if (pos.length != clipboardPos.length) return "Incompatible overlays.";
      int diffIndex = -1;
      for (int i=0; i<pos.length; i++) {
        if (pos[i] != clipboardPos[i]) {
          if (diffIndex != -1) {
            return "Dimensional positions of copied overlay and selected " +
              "overlay must not vary across multiple axes.";
          }
          diffIndex = i;
        }
      }
      if (diffIndex == -1) {
        return "Nothing to distribute -- copied overlay and selected overlay " +
          "have identical dimensional positions.";
      }
      int distance = pos[diffIndex] - clipboardPos[diffIndex];
      boolean reverse = distance < 0;
      if (reverse) distance = -distance;
      if (distance < 2) {
        return "Nothing to distribute -- there are no frames between copied " +
          "overlay and selected overlay.";
      }

      // compile some information about the overlays
      String className = sel.getClass().getName();
      boolean filled = clip.filled && sel.filled;
      String group = ObjectUtil.objectsEqual(clip.group, sel.group) ?
        clip.group : null;
      String notes = ObjectUtil.objectsEqual(clip.notes, sel.notes) ?
        clip.notes : null;

      // loop through intermediate dimensional positions
      int inc = reverse ? 1 : -1;
      int[] p = new int[pos.length];
      System.arraycopy(pos, 0, p, 0, pos.length);
      for (int i=1; i<distance; i++) {
        p[diffIndex] = pos[diffIndex] + i * inc;
        ndx = FormatTools.positionToRaster(lengths, p);

        OverlayObject obj = OverlayIO.createOverlay(className, this);

        float q = (float) i / distance;
        obj.x1 = q * clip.x1 + (1 - q) * sel.x1;
        obj.y1 = q * clip.y1 + (1 - q) * sel.y1;
        obj.x2 = q * clip.x2 + (1 - q) * sel.x2;
        obj.y2 = q * clip.y2 + (1 - q) * sel.y2;
        obj.color = new Color(
          (int) (q * clip.color.getRed() + (1 - q) * sel.color.getRed()),
          (int) (q * clip.color.getGreen() + (1 - q) * sel.color.getGreen()),
          (int) (q * clip.color.getBlue() + (1 - q) * sel.color.getBlue()));
        obj.filled = filled;
        obj.group = group;
        obj.notes = notes;
        obj.drawing = false;
        obj.selected = false;
        if (obj instanceof OverlayText)
          ((OverlayText) obj).computeTextBounds();

        overlays[ndx].add(obj);
      }
    }

    notifyListeners(new TransformEvent(this));
    return null;
  }

  /** Gets the overlay objects at the current dimensional position. */
  public OverlayObject[] getObjects() { return getObjects(pos); }

  /** Gets the overlay objects at the given dimensional position. */
  public OverlayObject[] getObjects(int[] pos) {
    int ndx = FormatTools.positionToRaster(lengths, pos);
    if (ndx < 0 || ndx >= overlays.length) return null;
    OverlayObject[] oo = new OverlayObject[overlays[ndx].size()];
    overlays[ndx].copyInto(oo);
    return oo;
  }

  /** Sets transform's current dimensional position. */
  public void setPos(int[] pos) {
    if (ObjectUtil.arraysEqual(this.pos, pos)) return;
    this.pos = pos;
    controls.refreshListObjects();
  }

  /** Gets transform's current dimensional position. */
  public int[] getPos() { return pos; }

  /** Reads the overlays from the given reader. */
  public void loadOverlays(BufferedReader in) throws IOException {
    Vector[] loadedOverlays = OverlayIO.loadOverlays(in, this);
    if (loadedOverlays == null) return;
    overlays = loadedOverlays;
    controls.refreshListObjects();
    notifyListeners(new TransformEvent(this));
  }

  /** Writes the overlays to the given writer. */
  public void saveOverlays(PrintWriter out) {
    OverlayIO.saveOverlays(out, this);
  }

  /** Exports the overlays as .xls to the given output file.*/
  public HSSFWorkbook exportOverlays() {
    return OverlayIO.exportOverlays (this);
  }

  /** Gets domain type (XY). */
  public RealTupleType getDomainType() {
    ImageTransform it = (ImageTransform) parent;
    RealTupleType rtt = null;
    try { rtt = new RealTupleType(it.getXType(), it.getYType()); }
    catch (VisADException exc) { exc.printStackTrace(); }
    return rtt;
  }

  /** Gets range type (RGB). */
  public TupleType getRangeType() { return RANGE_TUPLE; }

  /** Gets range type for text overlays (R, G, B, text). */
  public TupleType getTextRangeType() { return textRangeTuple; }

  /** Constructs a range value with the given component values. */
  public Tuple getTextRangeValue(String text,
    float r, float g, float b, float a)
  {
    Tuple tuple = null;
    try {
      tuple = new Tuple(textRangeTuple, new Data[] {
        new Text(textType, text),
        new Real(RED_TYPE, r),
        new Real(GREEN_TYPE, g),
        new Real(BLUE_TYPE, b),
        new Real(ALPHA_TYPE, a)
      });
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
    return tuple;
  }

  /**
   * Gets a scaling value suitable for computing
   * an overlay size or picking threshold.
   */
  public int getScalingValue() {
    int width = getScalingValueX();
    int height = getScalingValueY();
    return width < height ? width : height;
  }

  /**
   * Gets a scaling value along the X axis, suitable for
   * computing an overlay width or picking threshold.
   */
  public int getScalingValueX() {
    ImageTransform it = (ImageTransform) parent;
    return it.getImageWidth();
  }

  /**
   * Gets a scaling value along the Y axis, suitable for
   * computing an overlay height or picking threshold.
   */
  public int getScalingValueY() {
    ImageTransform it = (ImageTransform) parent;
    return it.getImageHeight();
  }

  /** Gets font metrics for the current font. */
  public FontMetrics getFontMetrics() { return fontMetrics; }

  /**
   * Whether text is drawn. This toggle exists because drawing text is very
   * slow, so some operations temporarily turn off text rendering to speed
   * up onscreen updates.
   */
  public void setTextDrawn(boolean value) { drawText = value; }

  /** Gets whether text is drawn. */
  public boolean isTextDrawn() { return drawText; }

  /** Gets whether the current tool has changed since last mouse gesture. */
  public boolean hasToolChanged() { return toolChanged; }

  // -- Static DataTransform API methods --

  /** Creates a new set of overlays, with user interaction. */
  public static DataTransform makeTransform(DataManager dm) {
    DataTransform dt = dm.getSelectedData();
    if (!isValidParent(dt)) return null;
    String n = (String) JOptionPane.showInputDialog(dm.getControls(),
      "Title of overlays:", "Create overlays",
      JOptionPane.INFORMATION_MESSAGE, null, null,
      dt.getName() + " overlays");
    if (n == null) return null;
    return new OverlayTransform(dt, n);
  }

  /**
   * Indicates whether this transform type would accept
   * the given transform as its parent transform.
   */
  public static boolean isValidParent(DataTransform data) {
    return data != null && data instanceof ImageTransform;
  }

  /** Indicates whether this transform type requires a parent transform. */
  public static boolean isParentRequired() { return true; }

  // -- DataTransform API methods --

  /**
   * Retrieves the data corresponding to the given dimensional position,
   * for the given display dimensionality.
   *
   * @return null if the transform does not provide data of that dimensionality
   */
  public Data getData(TransformLink link, int[] pos, int dim, DataCache cache) {
    // note to ACS: do not assume TransformLink is null.  It may be null!
    // If so, assume some reasonable defaults when computing your selection
    // grids.

    // The current display
    DisplayImpl display = link.getHandler().getWindow().getDisplay(); 

    if (dim != 2) {
      System.err.println(name + ": invalid dimensionality (" + dim + ")");
      return null;
    }
    int q = FormatTools.positionToRaster(lengths, pos);
    if (q < 0 || q >= overlays.length) return null;
    synchronized (overlays) {
      int size = overlays[q].size();
      DataImpl selectData = null;
      FieldImpl rgbField = null, txtField = null;
      try {
        if (size > 0) {
          // compute number of selected objects, text objects
          int rgbSize = 0, txtSize = 0, sel = 0, outline = 0;
          for (int i=0; i<size; i++) {
            OverlayObject obj = (OverlayObject) overlays[q].elementAt(i);
            if (obj.hasText()) {
              if (drawText || obj.isSelected()) txtSize++;
              else outline++;
            }
            else rgbSize++;
            // do not paint grids for objects still in the initial draw phase
            if (obj.isSelected() && !obj.isDrawing()) sel++;
          }
          RealType index = RealType.getRealType("overlay_index");

          // compile standard objects into RGB field
          if (rgbSize > 0 || sel > 0 || outline > 0) {
            FunctionType fieldType = new FunctionType(index,
              new FunctionType(getDomainType(), getRangeType()));
            GriddedSet fieldSet = new Integer1DSet(rgbSize + sel + outline);
            rgbField = new FieldImpl(fieldType, fieldSet);
            // compute overlay data for each non-text object
            for (int i=0, c=0; i<size && c<rgbSize; i++) {
              OverlayObject obj = (OverlayObject) overlays[q].elementAt(i);
              // rescale object if appropriate
              // (currently applies only to OverlayMarkers)
              if (obj.isScalable())
                obj.rescale(OverlayUtil.getMultiplier(display)); 
              if (obj.hasText()) continue;
              rgbField.setSample(c++, obj.getData(), false);
            }
            // compute selection grid for each selected object
            for (int i=0, c=0; i<size && c<sel; i++) {
              OverlayObject obj = (OverlayObject) overlays[q].elementAt(i);
              if (!obj.isSelected() || obj.isDrawing()) continue;
              DataImpl layer = OverlayUtil.getSelectionLayer(obj, link, false);
              rgbField.setSample(rgbSize + c++, layer, false);
            }
            // compute outline grid for each invisible text object
            for (int i=0, c=0; i<size && c<outline; i++) {
              OverlayObject obj = (OverlayObject) overlays[q].elementAt(i);
              if (!obj.hasText() || obj.isSelected()) continue;
              DataImpl layer = OverlayUtil.getSelectionLayer(obj, link, true);
              rgbField.setSample(rgbSize + sel + c++, layer, false);
            }
          }

          // compile text objects into text field
          if (txtSize > 0) {
            FunctionType fieldType = new FunctionType(index,
              new FunctionType(getDomainType(), getTextRangeType()));
            GriddedSet fieldSet = new Integer1DSet(txtSize);
            txtField = new FieldImpl(fieldType, fieldSet);
            // compute overlay data for each text object
            int c = 0;
            for (int i=0; i<size && c<txtSize; i++) {
              OverlayObject obj = (OverlayObject) overlays[q].elementAt(i);
              if (!obj.hasText() || !drawText) continue;
              txtField.setSample(c++, obj.getData(), false);
            }
          }
        }

        // retrieve select box data
        if (selectBox != null) {
            selectData = selectBox.getData();
        }

        // display only non-null data objects
        Vector v = new Vector();
        if (selectData != null) v.add(selectData);
        if (rgbField != null) v.add(rgbField);
        if (txtField != null) v.add(txtField);
        if (v.size() == 0) return null;
        else if (v.size() == 1) return (Data) v.elementAt(0);
        Data[] data = (Data[]) v.toArray(new Data[0]);
        return new Tuple(data, false);
      }
      catch (VisADException exc) { exc.printStackTrace(); }
      catch (RemoteException exc) { exc.printStackTrace(); }
    }
    return null;
  }

  /** Gets whether this transform provides data of the given dimensionality. */
  public boolean isValidDimension(int dim) { return dim == 2; }

  /** Retrieves a set of mappings for displaying this transform effectively. */
  public ScalarMap[] getSuggestedMaps() {
    // Overlay XYZ types piggyback on the parent, so that two sets of XYZ
    // coordinates don't show up during cursor probes (and so that the overlays
    // are placed properly without an extra set of setRange calls).
    RealType x = ((ImageTransform) parent).getXType();
    RealType y = ((ImageTransform) parent).getYType();

    // We do need new RGB types, so that overlay colors can be
    // independently manipulated, and a new Text type, for overlaid text.
    ScalarMap[] maps = null;
    try {
      ScalarMap xMap = new ScalarMap(x, Display.XAxis);
      ScalarMap yMap = new ScalarMap(y, Display.YAxis);
      ScalarMap tMap = new ScalarMap(textType, Display.Text);
      ScalarMap rMap = new ScalarMap(RED_TYPE, Display.Red);
      ScalarMap gMap = new ScalarMap(GREEN_TYPE, Display.Green);
      ScalarMap bMap = new ScalarMap(BLUE_TYPE, Display.Blue);
      ScalarMap aMap = new ScalarMap(ALPHA_TYPE, Display.Alpha);
      rMap.setRange(0, 1);
      gMap.setRange(0, 1);
      bMap.setRange(0, 1);
      aMap.setRange(0, 1);
      maps = new ScalarMap[] {xMap, yMap, tMap, rMap, gMap, bMap, aMap};
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
    return maps;
  }

  /**
   * Gets a string id uniquely describing this data transform at the given
   * dimensional position, for the purposes of thumbnail caching.
   * If global flag is true, the id is suitable for use in the default,
   * global cache file.
   */
  public String getCacheId(int[] pos, boolean global) { return null; }

  /**
   * Overlay transforms are rendered immediately,
   * due to frequent user interaction.
   */
  public boolean isImmediate() { return true; }

  /** Gets associated GUI controls for this transform. */
  public JComponent getControls() { return controls; }

  /**
   * Sets the font used for text overlays.
   * Since changing the font changes the data (text overlay bounding boxes,
   * this method is overridden to send a DATA_CHANGED after recomputing them.
   */
  public void setFont(Font font) {
    super.setFont(font);

    // obtain new font metrics
    // HACK - always use metrics for font size 11, since it seems to work well
    fontMetrics = controls.getFontMetrics(
      new Font(font.getName(), font.getStyle(), 11));

    // recompute grid boxes for text overlays
    // 4/24 removed computeGridParameters method
    for (int j=0; j<overlays.length; j++) {
      for (int i=0; i<overlays[j].size(); i++) {
        OverlayObject obj = (OverlayObject) overlays[j].get(i);
        if (obj instanceof OverlayText)
          ((OverlayText) obj).computeTextBounds();
      }
    }

    notifyListeners(new TransformEvent(this, TransformEvent.DATA_CHANGED));
  }

  /** Responds to mouse gestures with appropriate overlay interaction. */
  public void displayChanged(DisplayEvent e) {
    int id = e.getId();
    OverlayTool tool = controls.getActiveTool();
    DisplayImpl display = (DisplayImpl) e.getDisplay();

    if (tool != null) {
      String toolType = tool.getName();
      if (toolType.equals(lastToolType)) toolChanged = false;
      else toolChanged = true;
      lastToolType = toolType;
    }

    if (id == DisplayEvent.TRANSFORM_DONE) updatePosition(display);
    else if (id == DisplayEvent.MOUSE_MOVED) {
      if (tool != null) {
        int px = e.getX(), py = e.getY();
        double[] coords = CursorUtil.pixelToDomain(display, px, py);
        tool.mouseMoved(e, px, py,
          (float) coords[0], (float) coords[1], pos, e.getModifiers());
      }
    }
    else if (id == DisplayEvent.MOUSE_PRESSED_RIGHT) {
      mouseDownRight = true;
      if (mouseDownLeft) releaseLeft(e, display, tool);
    }
    else if (id == DisplayEvent.MOUSE_RELEASED_RIGHT) mouseDownRight = false;
    else if (id == DisplayEvent.MOUSE_PRESSED_LEFT) {
      if (mouseDownRight) return;
      mouseDownLeft = true;
      updatePosition(display);
      if (tool != null) {
        int px = e.getX(), py = e.getY();
        double[] coords = CursorUtil.pixelToDomain(display, px, py);
        tool.mouseDown(e, px, py,
          (float) coords[0], (float) coords[1], pos, e.getModifiers());
      }
    }
    else if (id == DisplayEvent.MOUSE_RELEASED_LEFT) {
      if (!mouseDownLeft) return;
      releaseLeft(e, display, tool);
    }
    else if (mouseDownLeft && id == DisplayEvent.MOUSE_DRAGGED) {
      updatePosition(display);
      if (tool != null) {
        int px = e.getX(), py = e.getY();
        double[] coords = CursorUtil.pixelToDomain(display, px, py);
        tool.mouseDrag(e, px, py,
          (float) coords[0], (float) coords[1], pos, e.getModifiers());
      }
    }
    else if (id == DisplayEvent.KEY_PRESSED) {
      updatePosition(display);
      int code = e.getKeyCode();
      boolean ctrl = (e.getModifiers() & InputEvent.CTRL_MASK) != 0;
      if (code == KeyEvent.VK_DELETE) removeSelectedObjects();
      else if (ctrl && code == KeyEvent.VK_C) copySelectedObjects();
      else if (ctrl && code == KeyEvent.VK_V) pasteObjects();
      else {
        // update selected text objects
        int ndx = FormatTools.positionToRaster(lengths, pos);
        if (ndx < 0 || ndx >= overlays.length) return;
        Vector objs = overlays[ndx];
        boolean changed = false;
        for (int i=0; i<objs.size(); i++) {
          OverlayObject oo = (OverlayObject) objs.elementAt(i);
          if (oo.isSelected() && oo.hasText()) {
            if (code == KeyEvent.VK_BACK_SPACE) {
              String text = oo.getText();
              int len = text.length();
              if (len > 0) {
                oo.setText(text.substring(0, len - 1));
                changed = true;
              }
            }
            else {
              char c = ((KeyEvent) e.getInputEvent()).getKeyChar();
              if (c != KeyEvent.CHAR_UNDEFINED && c >= ' ') {
                oo.setText(oo.getText() + c);
                changed = true;
              }
            }
          }
        }
        if (changed) notifyListeners(new TransformEvent(this));
      }
      // No tools use keyPressed functionality, so it is disabled for now
      //if (tool != null) tool.keyPressed(e.getKeyCode(), e.getModifiers());
    }
    else if (id == DisplayEvent.KEY_RELEASED) {
      updatePosition(display);
      // No tools use keyReleased functionality, so it is disabled for now
      //if (tool != null) tool.keyReleased(e.getKeyCode(), e.getModifiers());
    }
  }

  // -- Dynamic API methods --

  /** Tests whether two dynamic objects are equivalent. */
  public boolean matches(Dynamic dyn) {
    if (!super.matches(dyn) || !isCompatible(dyn)) return false;
    OverlayTransform data = (OverlayTransform) dyn;

    // CTR TODO return true iff data matches this object
    return false;
  }

  /**
   * Tests whether the given dynamic object can be used as an argument to
   * initState, for initializing this dynamic object.
   */
  public boolean isCompatible(Dynamic dyn) {
    return dyn instanceof OverlayTransform;
  }

  /**
   * Modifies this object's state to match that of the given object.
   * If the argument is null, the object is initialized according to
   * its current state instead.
   */
  public void initState(Dynamic dyn) {
    if (dyn != null && !isCompatible(dyn)) return;
    super.initState(dyn);
    OverlayTransform data = (OverlayTransform) dyn;

    if (data != null) {
      // CTR TODO synchronize data with this object
    }

    lengths = parent.getLengths();
    dims = parent.getDimTypes();
    makeLabels();

    int len = FormatTools.getRasterLength(lengths);
    Vector[] v = new Vector[len];
    int minLen = 0;
    if (overlays != null) {
      // CTR - This logic is simplistic and will result in erroneous behavior
      // should a transform with multiple dimensional axes suffer a length
      // alteration along its axes. That is, the rasterization will probably be
      // shifted so that a particular position such as (3, 5) will no longer be
      // (3, 5), even if (3, 5) is still a valid dimensional position. But we
      // cannot guarantee much in the general case, because the number of
      // dimensional positions could also have shifted. What we should do is
      // sniff out exactly how a transform has changed by examining the old and
      // new lengths arrays, and act appropriately, but for now we simply
      // preserve as many overlays as possible. If the dimensional axes have
      // been significantly altered, too bad.
      minLen = overlays.length < len ? overlays.length : len;
      System.arraycopy(overlays, 0, v, 0, minLen);
    }
    for (int i=minLen; i<len; i++) v[i] = new Vector();
    overlays = v;
    pos = new int[lengths.length];

    controls = new OverlayWidget(this);
    fontMetrics = controls.getFontMetrics(font);
  }

  /**
   * Called when this object is being discarded in favor of
   * another object with a matching state.
   */
  public void discard() { }

  // -- TransformListener API methods --

  /** Called when parent data transform's parameters are updated. */
  public void transformChanged(TransformEvent e) {
    int id = e.getId();
    if (id == TransformEvent.DATA_CHANGED) {
      initState(null);
      notifyListeners(new TransformEvent(this));
    }
  }

  // -- Helper methods --
  
  /** Configures the given overlay to match the current settings. */
  protected void configureOverlay(OverlayObject obj) {
    OverlayWidget panel = (OverlayWidget) getControls();
    obj.setFilled(panel.isFilled());
    obj.setColor(panel.getActiveColor());
    obj.setGroup(panel.getActiveGroup());
    obj.setNotes(panel.getNotes());
  }

  /**
   * Updates the dimensional position based on
   * the current state of the given display.
   */
  protected void updatePosition(DisplayImpl display) {
    DisplayWindow window = DisplayWindow.getDisplayWindow(display);
    setPos(window.getTransformHandler().getPos(this));
  }

  /** Helper method that handles left mouse button releases. */
  protected void releaseLeft(DisplayEvent e,
    DisplayImpl display, OverlayTool tool)
  {
    mouseDownLeft = false;
    updatePosition(display);
    if (tool != null) {
      int px = e.getX(), py = e.getY();
      double[] coords = CursorUtil.pixelToDomain(display, px, py);
      tool.mouseUp(e, px, py,
        (float) coords[0], (float) coords[1], pos, e.getModifiers());
    }
  }

}
