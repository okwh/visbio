//
// DataControls.java
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

package loci.visbio.data;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import loci.common.ReflectException;
import loci.common.ReflectedUniverse;
import loci.visbio.BioDropHandler;
import loci.visbio.ControlPanel;
import loci.visbio.LogicManager;
import loci.visbio.VisBioFrame;
import loci.visbio.WindowManager;
import loci.visbio.ome.OMEManager;
import loci.visbio.overlays.OverlayTransform;
import loci.visbio.util.DialogPane;
import loci.visbio.util.DisplayUtil;
import loci.visbio.util.LAFUtil;
import loci.visbio.util.SwingUtil;
import loci.visbio.view.DisplayManager;
import loci.visbio.view.DisplayWindow;

import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * DataControls is the control panel for managing data.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/visbio/src/loci/visbio/data/DataControls.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/visbio/src/loci/visbio/data/DataControls.java">SVN</a></dd></dl>
 */
public class DataControls extends ControlPanel
  implements ActionListener, TransformListener, TreeSelectionListener
{

  // -- GUI components --

  /** Tree of data transforms. */
  private JTree dataTree;

  /** Model for data transform tree. */
  private DefaultTreeModel dataModel;

  /** Root node of tree structure. */
  private DefaultMutableTreeNode dataRoot;

  /** Button for displaying data. */
  private JButton display;

  /** Button for editing data parameters. */
  private JButton editData;

  /** Button for exporting data. */
  private JButton export;

  /** Button for removing data from the tree. */
  private JButton removeData;

  /** Data information display panel. */
  private JEditorPane dataInfo;

  /** New 2D display menu item for Displays popup menu. */
  private JMenuItem display2D;

  /** New 3D display menu item for Displays popup menu. */
  private JMenuItem display3D;

  /** Pane for configuring disk export parameters. */
  private ExportPane exporter;

  /** Pane for configuring ImageJ export parameters. */
  private SendToIJPane sender;

  // -- Other fields --

  /** Table of control frames corresponding to each data transform. */
  private Hashtable frameTable;

  // -- Constructor --

  /** Constructs a tool panel for adjusting data parameters. */
  public DataControls(LogicManager logic) {
    super(logic, "Data", "Controls for managing data");
    frameTable = new Hashtable();

    // list of data objects
    dataRoot = new DefaultMutableTreeNode("Data objects");
    dataModel = new DefaultTreeModel(dataRoot);
    dataTree = new JTree(dataModel);
    dataTree.setRootVisible(false);
    dataTree.getSelectionModel().setSelectionMode(
      TreeSelectionModel.SINGLE_TREE_SELECTION);
    dataTree.setVisibleRowCount(8);
    dataTree.addTreeSelectionListener(this);
    JScrollPane treePane = new JScrollPane(dataTree);
    SwingUtil.configureScrollPane(treePane);

    // add data button
    JButton addData = new JButton("Add >");
    addData.setActionCommand("addData");
    addData.addActionListener(this);
    if (!LAFUtil.isMacLookAndFeel()) addData.setMnemonic('a');
    addData.setToolTipText("Adds a new data object to the list");

    // display button
    display = new JButton("Display >");
    display.setActionCommand("display");
    display.addActionListener(this);
    if (!LAFUtil.isMacLookAndFeel()) display.setMnemonic('d');
    display.setToolTipText("Visualizes the selected data object");

    // controls button
    editData = new JButton("Edit");
    editData.setActionCommand("edit");
    editData.addActionListener(this);
    if (!LAFUtil.isMacLookAndFeel()) editData.setMnemonic('i');
    editData.setToolTipText(
      "Shows controls for editing the selected data object");

    // export button
    export = new JButton("Export >");
    export.setActionCommand("export");
    export.addActionListener(this);
    if (!LAFUtil.isMacLookAndFeel()) export.setMnemonic('x');
    export.setToolTipText("Exports the selected data object");

    // remove data button
    removeData = new JButton("Remove");
    removeData.setActionCommand("removeData");
    removeData.addActionListener(this);
    if (!LAFUtil.isMacLookAndFeel()) removeData.setMnemonic('r');
    removeData.setToolTipText(
      "Removes the selected data object from the list");

    // data information display panel
    dataInfo = new JEditorPane();
    dataInfo.setPreferredSize(new java.awt.Dimension(0, 0));
    dataInfo.setEditable(false);
    dataInfo.setContentType("text/html");
    dataInfo.setToolTipText(
      "Reports information about the selected data object");
    JScrollPane infoPane = new JScrollPane(dataInfo);
    SwingUtil.configureScrollPane(infoPane);

    doDataInfo(null);

    // new 2D display menu item
    display2D = new JMenuItem("New 2D display...");
    display2D.setMnemonic('2');
    display2D.setActionCommand("new2D");
    display2D.addActionListener(this);

    // new 3D display menu item
    display3D = new JMenuItem("New 3D display...");
    display3D.setMnemonic('3');
    display3D.setActionCommand("new3D");
    display3D.addActionListener(this);
    display3D.setEnabled(DisplayUtil.canDo3D());

    // export panes
    VisBioFrame bio = lm.getVisBio();
    exporter = new ExportPane(bio);
    sender = new SendToIJPane(bio);

    // lay out buttons
    ButtonStackBuilder bsb = new ButtonStackBuilder();
    bsb.addGridded(addData);
    bsb.addRelatedGap();
    bsb.addGridded(display);
    bsb.addRelatedGap();
    bsb.addGridded(editData);
    bsb.addRelatedGap();
    bsb.addGridded(export);
    bsb.addRelatedGap();
    bsb.addGridded(removeData);
    JPanel buttons = bsb.getPanel();

    // lay out components
    PanelBuilder builder = new PanelBuilder(new FormLayout(
      "pref:grow, 3dlu, pref", "pref, 5dlu, fill:200:grow"));
    CellConstraints cc = new CellConstraints();
    builder.add(treePane, cc.xy(1, 1));
    builder.add(buttons, cc.xy(3, 1));
    builder.add(infoPane, cc.xyw(1, 3, 3));
    add(builder.getPanel());

    // handle file drag and drop
    BioDropHandler dropHandler = new BioDropHandler(bio);
    ((JComponent) bio.getContentPane()).setTransferHandler(dropHandler);
    dataTree.setTransferHandler(dropHandler);
    dataInfo.setTransferHandler(dropHandler);
  }

  // -- DataControls API methods --

  /** Adds a data object to the data object tree. */
  public void addData(DataTransform data) {
    // find parent node
    DefaultMutableTreeNode parent = findNode(data.getParent());
    if (parent == null) parent = dataRoot;

    // add new data node beneath its parent
    DefaultMutableTreeNode node = new DefaultMutableTreeNode(data);
    dataModel.insertNodeInto(node, parent, parent.getChildCount());

    // create frame for housing data's controls
    JComponent dataControls = data.getControls();
    if (dataControls != null) {
      JFrame frame = new JFrame("Data - " + data.getName());
      JPanel pane = new JPanel();
      pane.setLayout(new BorderLayout());
      frame.setContentPane(pane);
      WindowManager wm = (WindowManager)
        lm.getVisBio().getManager(WindowManager.class);
      wm.addWindow(frame);

      // lay out frame components
      PanelBuilder builder = new PanelBuilder(new FormLayout(
        "pref:grow", "fill:pref:grow"));
      builder.setDefaultDialogBorder();
      CellConstraints cc = new CellConstraints();
      builder.add(dataControls, cc.xy(1, 1));
      JScrollPane scroll = new JScrollPane(builder.getPanel());
      SwingUtil.configureScrollPane(scroll);
      pane.add(scroll, BorderLayout.CENTER);

      // add data's controls to table
      frameTable.put(data, frame);
    }

    // start thumbnail auto-generation, if applicable
    ThumbnailHandler th = data.getThumbHandler();
    if (th != null) {
      DataManager dm = (DataManager) lm;
      th.setResolution(dm.getThumbnailResolution());
      if (dm.getAutoThumbGen()) th.toggleGeneration(true);
      else th.loadThumb(0); // HACK - need first thumbnail for proper colors
    }

    data.addTransformListener(this);

    selectNode(node);
  }

  /**
   * Removes a data object from the data object tree.
   *
   * @return true if the data object was successfully removed
   */
  public boolean removeData(DataTransform data) {
    return removeData(data, false);
  }

  /**
   * Removes a data object from the data object tree, confirming with the user
   * first if confirm flag is set and the object has derivative objects.
   *
   * @return true if the data object was successfully removed
   */
  public boolean removeData(DataTransform data, boolean confirm) {
    DefaultMutableTreeNode node = findNode(data);
    if (node == null) return false;
    if (confirm && !node.isLeaf()) {
      VisBioFrame bio = lm.getVisBio();
      int ans = JOptionPane.showConfirmDialog(this,
        "The derivative data objects depending on this one " +
        "will also be removed. Are you sure?", "VisBio",
        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      if (ans != JOptionPane.YES_OPTION) return false;
    }

    // recursively remove derivative data objects first
    int count = node.getChildCount();
    for (int i=0; i<count; i++) {
      DefaultMutableTreeNode child =
        (DefaultMutableTreeNode) node.getChildAt(i);
      if (child == null) continue;
      removeData((DataTransform) child.getUserObject(), false);
    }

    dataModel.removeNodeFromParent(node);

    // remove data's controls from window manager and frame table
    WindowManager wm = (WindowManager)
      lm.getVisBio().getManager(WindowManager.class);
    JFrame frame = (JFrame) frameTable.get(data);
    frameTable.remove(data);
    wm.disposeWindow(frame);

    // discard data object
    data.discard();

    return true;
  }

  /** Selects the given data object in the tree. */
  public void setSelectedData(DataTransform data) {
    selectNode(findNode(data));
  }

  /** Gets the root node of the data object tree. */
  public DefaultMutableTreeNode getDataRoot() { return dataRoot; }

  /** Gets the currently selected data object. */
  public DataTransform getSelectedData() {
    TreePath path = dataTree.getSelectionPath();
    if (path == null) return null;
    DefaultMutableTreeNode node =
      (DefaultMutableTreeNode) path.getLastPathComponent();
    if (node == null) return null;
    Object obj = node.getUserObject();
    if (!(obj instanceof DataTransform)) return null;
    return (DataTransform) obj;
  }

  /** Shows frame containing controls for the given data object. */
  public void showControls(DataTransform data) {
    JFrame frame = (JFrame) frameTable.get(data);
    if (frame == null) return;
    WindowManager wm = (WindowManager)
      lm.getVisBio().getManager(WindowManager.class);
    wm.showWindow(frame);
  }

  /** Hides frame containing controls for the given data object. */
  public void hideControls(DataTransform data) {
    JFrame frame = (JFrame) frameTable.get(data);
    if (frame == null) return;
    frame.setVisible(false);
  }

  /** Exports a data object to disk. */
  public void exportData(ImageTransform data) {
    exporter.setData(data);
    int rval = exporter.showDialog(this);
    if (rval == DialogPane.APPROVE_OPTION) exporter.export();
  }

  /** Sends part of a data object to an instance of ImageJ. */
  public void sendDataToImageJ(ImageTransform data) {
    sender.setData(data);
    if (data.getLengths().length > 0) {
      int rval = sender.showDialog(this);
      if (rval != DialogPane.APPROVE_OPTION) return;
    }
    sender.send();
  }

  /** Creates a new display and adds the selected data object to it. */
  public void doNewDisplay(boolean threeD) {
    DataTransform data = getSelectedData();
    if (data == null) return;
    DisplayManager disp = (DisplayManager)
      lm.getVisBio().getManager(DisplayManager.class);
    if (disp == null) return;
    DisplayWindow window = disp.createDisplay(this, data.getName(), threeD);
    if (window == null) return;
    window.addTransform(data);
    WindowManager wm = (WindowManager)
      lm.getVisBio().getManager(WindowManager.class);
    wm.showWindow(window);
  }

  /** Do new display with overlays */
  public void doNewDisplayWithOverlays() {
    DataTransform data = getSelectedData();
    if (data == null) return;
    DisplayManager disp = (DisplayManager)
      lm.getVisBio().getManager(DisplayManager.class);
    if (disp == null) return;
    DisplayWindow window = disp.createDisplay(this, data.getName(), false);
    if (window == null) return;

    // construct overlays and add to data list and display window
    String overlaysName = data.getName() + "  overlays";
    OverlayTransform overlays = new OverlayTransform(data, overlaysName);
    DataManager dm = (DataManager)
      lm.getVisBio().getManager(DataManager.class);
    if (dm == null) return;
    dm.addData(overlays);
    window.addTransform(data);
    window.addTransform(overlays);

    // show windows
    WindowManager wm = (WindowManager)
      lm.getVisBio().getManager(WindowManager.class);
    wm.showWindow(window);
    dm.showControls(overlays);
  }

  // -- ActionListener API methods --

  /** Handles button presses. */
  public void actionPerformed(ActionEvent e) {
    DataManager dm = (DataManager) lm;
    String cmd = e.getActionCommand();

    if (cmd.equals("addData")) {
      DataTransform data = getSelectedData();
      Class[] dataTypes = dm.getRegisteredDataTypes();
      String[] dataLabels = dm.getRegisteredDataLabels();

      // build popup menu from registered data transform types
      JPopupMenu menu = new JPopupMenu();
      for (int i=0; i<dataTypes.length; i++) {
        // check data transform compatibility via reflection
        String clas = dataTypes[i].getName();
        boolean validParent = false;
        boolean needParent = true;
        try {
          ReflectedUniverse r = new ReflectedUniverse();
          r.exec("import " + clas);
          r.setVar("data", data);
          String n = clas.substring(clas.lastIndexOf(".") + 1);
          r.exec("valid = " + n + ".isValidParent(data)");
          validParent = ((Boolean) r.getVar("valid")).booleanValue();
          r.exec("need = " + n + ".isParentRequired()");
          needParent = ((Boolean) r.getVar("need")).booleanValue();
        }
        catch (ReflectException exc) { exc.printStackTrace(); }

        // add menu item for compatible transform type
        JMenuItem item = new JMenuItem(dataLabels[i]);
        item.setMnemonic(dataLabels[i].charAt(0));
        item.setActionCommand(clas);
        item.addActionListener(this);
        item.setEnabled(validParent || !needParent);
        menu.add(item);
      }

      // show popup menu
      JButton source = (JButton) e.getSource();
      menu.show(source, source.getWidth(), 0);
    }
    else if (cmd.equals("display")) {
      DataTransform data = getSelectedData();
      if (data == null) return;
      DisplayManager disp = (DisplayManager)
        lm.getVisBio().getManager(DisplayManager.class);
      if (disp == null) return;
      DisplayWindow[] dd = disp.getDisplays();

      boolean canDisplay2D = data.canDisplay2D();
      boolean canDisplay3D = data.canDisplay3D();

      // build popup menu from display list
      JPopupMenu menu = new JPopupMenu();
      for (int i=0; i<dd.length; i++) {
        final DisplayWindow window = dd[i];
        JMenuItem item = new JMenuItem(window.toString());
        item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            // add selected data object to chosen display
            DataTransform data = getSelectedData();
            if (data != null) window.addTransform(data);
            WindowManager wm = (WindowManager)
              lm.getVisBio().getManager(WindowManager.class);
            wm.showWindow(window);
          }
        });
        boolean threeD = window.is3D();
        boolean valid = (!threeD && canDisplay2D) || (threeD && canDisplay3D);
        item.setEnabled(valid && !window.hasTransform(data));
        menu.add(item);
      }
      if (dd.length > 0) menu.addSeparator();
      menu.add(display2D);
      menu.add(display3D);

      // display popup menu
      JButton source = (JButton) e.getSource();
      menu.show(source, source.getWidth(), 0);
    }
    else if (cmd.equals("edit")) showControls(getSelectedData());
    else if (cmd.equals("export")) {
      // build popup menu
      JPopupMenu menu = new JPopupMenu();
      DataTransform data = getSelectedData();

      JMenuItem saveToDisk = new JMenuItem("Save to disk...");
      saveToDisk.setMnemonic('s');
      saveToDisk.setActionCommand("saveToDisk");
      saveToDisk.addActionListener(this);
      menu.add(saveToDisk);

      JMenuItem sendToIJ = new JMenuItem("Send to ImageJ...");
      sendToIJ.setMnemonic('i');
      sendToIJ.setActionCommand("sendToIJ");
      sendToIJ.addActionListener(this);
      menu.add(sendToIJ);

      JMenuItem uploadToOME = new JMenuItem("Upload to OME...");
      uploadToOME.setMnemonic('u');
      uploadToOME.setActionCommand("uploadToOME");
      uploadToOME.addActionListener(this);
      uploadToOME.setEnabled(data instanceof Dataset);
      menu.add(uploadToOME);

      // show popup menu
      JButton source = (JButton) e.getSource();
      menu.show(source, source.getWidth(), 0);
    }
    else if (cmd.equals("removeData")) dm.removeData(getSelectedData(), true);
    else if (cmd.equals("new2D")) doNewDisplay(false);
    else if (cmd.equals("new3D")) doNewDisplay(true);
    else if (cmd.equals("saveToDisk")) dm.exportData();
    else if (cmd.equals("sendToIJ")) dm.sendDataToImageJ();
    else if (cmd.equals("uploadToOME")) {
      OMEManager om = (OMEManager) lm.getVisBio().getManager(OMEManager.class);
      if (om == null) return;
      om.upload(this);
    }
    else {
      // command represents a class to instantiate via reflection
      DataTransform data = null;
      try {
        ReflectedUniverse r = new ReflectedUniverse();
        r.exec("import " + cmd);
        r.setVar("dm", dm);
        String n = cmd.substring(cmd.lastIndexOf(".") + 1);
        r.exec("data = " + n + ".makeTransform(dm)");
        data = (DataTransform) r.getVar("data");
      }
      catch (ReflectException exc) { exc.printStackTrace(); }
      if (data != null) dm.addData(data);
    }
  }

  // -- TransformListener API methods --

  /** Updates data info panel if a data transform changes while selected. */
  public void transformChanged(TransformEvent e) {
    int id = e.getId();
    if (id != TransformEvent.DATA_CHANGED) return;
    DataTransform data = (DataTransform) e.getSource();
    if (data != getSelectedData()) return;
    //System.out.println("transform changed"); // TEMP
    //System.out.println("e.getSource = " + data); // TEMP
    //System.out.println("e.getSource().getClass().getName() = " +
    //    data.getClass().getName()); // TEMP
    doDataInfo(data);
  }

  // -- TreeSelectionListener API methods --

  /** Handles tree selection changes. */
  public void valueChanged(TreeSelectionEvent e) {
    doDataInfo(getSelectedData());
  }

  // -- Helper methods --

  /** Finds the tree node representing this data object. */
  protected DefaultMutableTreeNode findNode(DataTransform data) {
    if (data == null) return null;
    Enumeration list = dataRoot.breadthFirstEnumeration();
    while (list.hasMoreElements()) {
      DefaultMutableTreeNode node =
        (DefaultMutableTreeNode) list.nextElement();
      Object obj = node.getUserObject();
      if (obj instanceof DataTransform) {
        DataTransform dt = (DataTransform) obj;
        if (dt == data) return node;
      }
    }
    return null;
  }

  /** Selects the given tree node. */
  protected void selectNode(DefaultMutableTreeNode node) {
    if (node == null) dataTree.clearSelection();
    else {
      TreePath path = new TreePath(node.getPath());
      dataTree.setSelectionPath(path);
      dataTree.scrollPathToVisible(path);
    }
  }

  /** Updates data information panel, and toggles button availability. */
  protected void doDataInfo(DataTransform data) {
    // update data information panel
    StringBuffer sb = new StringBuffer("<html><body>\n");
    if (data == null) {
      if (dataRoot.isLeaf()) {
        sb.append("<center>\nNo datasets have been loaded.<br>\n" +
          "&nbsp;&nbsp;Press the \"Add\" button " +
          "to import a dataset.&nbsp;&nbsp;\n</center>");
      }
      else {
        sb.append("<center>\nNo data is selected.<br>\n" +
          "&nbsp;&nbsp;Click a data object to see " +
          "its information.&nbsp;&nbsp;\n</center>");
      }
    }
    else sb.append(data.getHTMLDescription());
    sb.append("\n</body></html>");
    dataInfo.setText(sb.toString());

    // toggle button availability
    VisBioFrame bio = lm.getVisBio();
    boolean isData = data != null;
    boolean isImage = data instanceof ImageTransform;
    boolean canDisplay2D = isData && data.canDisplay2D();
    boolean canDisplay3D = isData && data.canDisplay3D();
    boolean canDisplay = canDisplay2D || canDisplay3D;
    boolean hasControls = isData && frameTable.get(data) != null;
    display.setEnabled(canDisplay);
    editData.setEnabled(hasControls);
    export.setEnabled(isImage);
    JMenuItem exportItem = bio.getMenuItem("File", "Export data...");
    if (exportItem != null) exportItem.setEnabled(isImage);
    removeData.setEnabled(isData);
    if (display2D != null) display2D.setEnabled(canDisplay2D);
    if (display3D != null) {
      display3D.setEnabled(DisplayUtil.canDo3D() && canDisplay3D);
    }
  }

}
