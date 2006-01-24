//
// DatasetWidget.java
//

/*
VisBio application for visualization of multidimensional
biological image data. Copyright (C) 2002-2006 Curtis Rueden.

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
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.*;
import loci.ome.xml.DOMUtil;
import loci.ome.xml.OMENode;
import loci.visbio.util.SwingUtil;
import org.w3c.dom.*;

/** DatasetWidget is a set of GUI controls for a Dataset transform. */
public class DatasetWidget extends JPanel
  implements ListSelectionListener, TreeSelectionListener
{

  // -- Constants --

  /** Column headings for OME-XML attributes table. */
  protected static final String[] TREE_COLUMNS = {"Attribute", "Value"};

  /** Column headings for metadata table. */
  protected static final String[] META_COLUMNS = {"Name", "Value"};


  // -- Fields --

  /** Associated dataset. */
  protected Dataset dataset;

  /** Dataset's associated metadata in OME-XML format. */
  protected OMENode[] ome;

  /** Dataset's associated metadata. */
  protected Hashtable[] metadata;

  /** Metadata hashtables' sorted key lists. */
  protected String[][] keys;

  /** List of dataset source files. */
  protected JList list;

  /** Tree displaying metadata in OME-XML format. */
  protected JTree tree;

  /** Root of tree displaying OME-XML metadata. */
  protected DefaultMutableTreeNode treeRoot;

  /** Table listing OME-XML attributes. */
  protected JTable treeTable;

  /** Table model backing OME-XML attributes table. */
  protected DefaultTableModel treeTableModel;

  /** Table listing metadata fields. */
  protected JTable metaTable;

  /** Table model backing metadata table. */
  protected DefaultTableModel metaTableModel;


  // -- Constructor --

  /** Constructs widget for display of dataset's associated metadata. */
  public DatasetWidget(Dataset dataset) {
    super();
    this.dataset = dataset;

    // get dataset's metadata
    ome = dataset.getOMENodes();
    metadata = dataset.getMetadata();
    keys = new String[metadata.length][];

    // sort metadata keys
    for (int i=0; i<metadata.length; i++) {
      if (metadata[i] == null) {
        keys[i] = new String[0];
        continue;
      }
      Enumeration e = metadata[i].keys();
      Vector v = new Vector();
      while (e.hasMoreElements()) v.add(e.nextElement());
      keys[i] = new String[v.size()];
      v.copyInto(keys[i]);
      Arrays.sort(keys[i]);
    }

    // list of filenames
    String[] ids = dataset.getFilenames();
    String[] names = new String[ids.length];
    for (int i=0; i<names.length; i++) names[i] = new File(ids[i]).getName();
    list = new JList(names);
    JScrollPane scrollList = new JScrollPane(list);
    SwingUtil.configureScrollPane(scrollList);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.addListSelectionListener(this);

    // -- First tab --

    // metadata table
    metaTableModel = new DefaultTableModel(META_COLUMNS, 0);
    metaTable = new JTable(metaTableModel);
    JScrollPane scrollMetaTable = new JScrollPane(metaTable);
    SwingUtil.configureScrollPane(scrollMetaTable);

    // -- Second tab --

    // OME-XML tree
    treeRoot = new DefaultMutableTreeNode();
    tree = new JTree(treeRoot);
    JScrollPane scrollTree = new JScrollPane(tree);
    SwingUtil.configureScrollPane(scrollTree);
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);
    DefaultTreeSelectionModel treeSelModel = new DefaultTreeSelectionModel();
    treeSelModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setSelectionModel(treeSelModel);
    tree.addTreeSelectionListener(this);

    // OME-XML attributes table
    treeTableModel = new DefaultTableModel(TREE_COLUMNS, 0);
    treeTable = new JTable(treeTableModel);
    JScrollPane scrollTreeTable = new JScrollPane(treeTable);
    SwingUtil.configureScrollPane(scrollTreeTable);

    // OME-XML split pane
    JSplitPane treeSplit =
      new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollTree, scrollTreeTable);

    // -- Main GUI --

    // tabbed pane
    JTabbedPane tabbed = new JTabbedPane();
    tabbed.addTab("Original metadata", scrollMetaTable);
    tabbed.addTab("OME-XML", treeSplit);

    // split pane
    JSplitPane mainSplit =
      new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollList, tabbed);

    // lay out components
    setLayout(new BorderLayout());
    add(mainSplit);
    list.setSelectedIndex(0);
  }


  // -- ListSelectionListener API methods --

  /** Called when the filename list selection changes. */
  public void valueChanged(ListSelectionEvent e) {
    treeRoot.removeAllChildren();

    int ndx = list.getSelectedIndex();
    if (ndx < 0 || ndx >= keys.length) {
      metaTableModel.setRowCount(0);
      return;
    }

    // update metadata table
    int len = keys[ndx].length;
    metaTableModel.setRowCount(len);
    for (int i=0; i<len; i++) {
      metaTableModel.setValueAt(keys[ndx][i], i, 0);
      metaTableModel.setValueAt(metadata[ndx].get(keys[ndx][i]), i, 1);
    }

    // update OME-XML tree
    Document doc = null;
    try { doc = ome[ndx] == null ? null : ome[ndx].getOMEDocument(false); }
    catch (Exception exc) { }
    if (doc != null) {
      buildTree(doc.getDocumentElement(), treeRoot);
      SwingUtil.expandTree(tree, treeRoot);
    }
  }


  // -- TreeSelectionListener API methods --

  /** Called when the OME-XML tree selection changes. */
  public void valueChanged(TreeSelectionEvent e) {
    DefaultMutableTreeNode node =
      (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
    if (node == null) {
      treeTableModel.setRowCount(0);
      return;
    }

    // update OME-XML attributes table
    Element el = ((ElementWrapper) node.getUserObject()).el;
    String[] names = DOMUtil.getAttributeNames(el);
    String[] values = DOMUtil.getAttributeValues(el);
    treeTableModel.setRowCount(names.length);
    for (int i=0; i<names.length; i++) {
      treeTableModel.setValueAt(names[i], i, 0);
      treeTableModel.setValueAt(values[i], i, 1);
    }
  }


  // -- Helper methods --

  /** Builds a tree by wrapping XML elements with JTree nodes. */
  protected void buildTree(Element el, DefaultMutableTreeNode node) {
    DefaultMutableTreeNode child =
      new DefaultMutableTreeNode(new ElementWrapper(el));
    node.add(child);
    NodeList nodeList = el.getChildNodes();
    for (int i=0; i<nodeList.getLength(); i++) {
      Node n = (Node) nodeList.item(i);
      if (n instanceof Element) buildTree((Element) n, child);
    }
  }


  // -- Helper classes --

  /** Helper class for OME-XML metadata tree view. */
  public class ElementWrapper {
    public Element el;
    public ElementWrapper(Element el) { this.el = el; }
    public String toString() { return el == null ? "null" : el.getTagName(); }
  }

}
