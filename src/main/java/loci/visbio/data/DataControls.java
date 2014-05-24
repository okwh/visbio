/*
 * #%L
 * VisBio application for visualization of multidimensional biological
 * image data.
 * %%
 * Copyright (C) 2002 - 2014 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package loci.visbio.data;

import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

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
import loci.visbio.overlays.OverlayTransform;
import loci.visbio.util.DialogPane;
import loci.visbio.util.DisplayUtil;
import loci.visbio.util.LAFUtil;
import loci.visbio.util.SwingUtil;
import loci.visbio.view.DisplayManager;
import loci.visbio.view.DisplayWindow;

/**
 * DataControls is the control panel for managing data.
 */
public class DataControls extends ControlPanel implements ActionListener,
	TransformListener, TreeSelectionListener
{

	// -- GUI components --

	/** Tree of data transforms. */
	private final JTree dataTree;

	/** Model for data transform tree. */
	private final DefaultTreeModel dataModel;

	/** Root node of tree structure. */
	private final DefaultMutableTreeNode dataRoot;

	/** Button for displaying data. */
	private final JButton display;

	/** Button for editing data parameters. */
	private final JButton editData;

	/** Button for exporting data. */
	private final JButton export;

	/** Button for removing data from the tree. */
	private final JButton removeData;

	/** Data information display panel. */
	private final JEditorPane dataInfo;

	/** New 2D display menu item for Displays popup menu. */
	private final JMenuItem display2D;

	/** New 3D display menu item for Displays popup menu. */
	private final JMenuItem display3D;

	/** Pane for configuring disk export parameters. */
	private final ExportPane exporter;

	/** Pane for configuring ImageJ export parameters. */
	private final SendToIJPane sender;

	// -- Other fields --

	/** Table of control frames corresponding to each data transform. */
	private final Hashtable frameTable;

	// -- Constructor --

	/** Constructs a tool panel for adjusting data parameters. */
	public DataControls(final LogicManager logic) {
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
		final JScrollPane treePane = new JScrollPane(dataTree);
		SwingUtil.configureScrollPane(treePane);

		// add data button
		final JButton addData = new JButton("Add >");
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
		editData
			.setToolTipText("Shows controls for editing the selected data object");

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
		removeData.setToolTipText("Removes the selected data object from the list");

		// data information display panel
		dataInfo = new JEditorPane();
		dataInfo.setPreferredSize(new java.awt.Dimension(0, 0));
		dataInfo.setEditable(false);
		dataInfo.setContentType("text/html");
		dataInfo
			.setToolTipText("Reports information about the selected data object");
		final JScrollPane infoPane = new JScrollPane(dataInfo);
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
		final VisBioFrame bio = lm.getVisBio();
		exporter = new ExportPane(bio);
		sender = new SendToIJPane(bio);

		// lay out buttons
		final ButtonStackBuilder bsb = new ButtonStackBuilder();
		bsb.addGridded(addData);
		bsb.addRelatedGap();
		bsb.addGridded(display);
		bsb.addRelatedGap();
		bsb.addGridded(editData);
		bsb.addRelatedGap();
		bsb.addGridded(export);
		bsb.addRelatedGap();
		bsb.addGridded(removeData);
		final JPanel buttons = bsb.getPanel();

		// lay out components
		final PanelBuilder builder =
			new PanelBuilder(new FormLayout("pref:grow, 3dlu, pref",
				"pref, 5dlu, fill:200:grow"));
		final CellConstraints cc = new CellConstraints();
		builder.add(treePane, cc.xy(1, 1));
		builder.add(buttons, cc.xy(3, 1));
		builder.add(infoPane, cc.xyw(1, 3, 3));
		add(builder.getPanel());

		// handle file drag and drop
		final BioDropHandler dropHandler = new BioDropHandler(bio);
		((JComponent) bio.getContentPane()).setTransferHandler(dropHandler);
		dataTree.setTransferHandler(dropHandler);
		dataInfo.setTransferHandler(dropHandler);
	}

	// -- DataControls API methods --

	/** Adds a data object to the data object tree. */
	public void addData(final DataTransform data) {
		// find parent node
		DefaultMutableTreeNode parent = findNode(data.getParent());
		if (parent == null) parent = dataRoot;

		// add new data node beneath its parent
		final DefaultMutableTreeNode node = new DefaultMutableTreeNode(data);
		dataModel.insertNodeInto(node, parent, parent.getChildCount());

		// create frame for housing data's controls
		final JComponent dataControls = data.getControls();
		if (dataControls != null) {
			final JFrame frame = new JFrame("Data - " + data.getName());
			final JPanel pane = new JPanel();
			pane.setLayout(new BorderLayout());
			frame.setContentPane(pane);
			final WindowManager wm =
				(WindowManager) lm.getVisBio().getManager(WindowManager.class);
			wm.addWindow(frame);

			// lay out frame components
			final PanelBuilder builder =
				new PanelBuilder(new FormLayout("pref:grow", "fill:pref:grow"));
			builder.setDefaultDialogBorder();
			final CellConstraints cc = new CellConstraints();
			builder.add(dataControls, cc.xy(1, 1));
			final JScrollPane scroll = new JScrollPane(builder.getPanel());
			SwingUtil.configureScrollPane(scroll);
			pane.add(scroll, BorderLayout.CENTER);

			// add data's controls to table
			frameTable.put(data, frame);
		}

		// start thumbnail auto-generation, if applicable
		final ThumbnailHandler th = data.getThumbHandler();
		if (th != null) {
			final DataManager dm = (DataManager) lm;
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
	public boolean removeData(final DataTransform data) {
		return removeData(data, false);
	}

	/**
	 * Removes a data object from the data object tree, confirming with the user
	 * first if confirm flag is set and the object has derivative objects.
	 * 
	 * @return true if the data object was successfully removed
	 */
	public boolean removeData(final DataTransform data, final boolean confirm) {
		final DefaultMutableTreeNode node = findNode(data);
		if (node == null) return false;
		if (confirm && !node.isLeaf()) {
			final VisBioFrame bio = lm.getVisBio();
			final int ans =
				JOptionPane.showConfirmDialog(this,
					"The derivative data objects depending on this one "
						+ "will also be removed. Are you sure?", "VisBio",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (ans != JOptionPane.YES_OPTION) return false;
		}

		// recursively remove derivative data objects first
		final int count = node.getChildCount();
		for (int i = 0; i < count; i++) {
			final DefaultMutableTreeNode child =
				(DefaultMutableTreeNode) node.getChildAt(i);
			if (child == null) continue;
			removeData((DataTransform) child.getUserObject(), false);
		}

		dataModel.removeNodeFromParent(node);

		// remove data's controls from window manager and frame table
		final WindowManager wm =
			(WindowManager) lm.getVisBio().getManager(WindowManager.class);
		final JFrame frame = (JFrame) frameTable.get(data);
		frameTable.remove(data);
		wm.disposeWindow(frame);

		// discard data object
		data.discard();

		return true;
	}

	/** Selects the given data object in the tree. */
	public void setSelectedData(final DataTransform data) {
		selectNode(findNode(data));
	}

	/** Gets the root node of the data object tree. */
	public DefaultMutableTreeNode getDataRoot() {
		return dataRoot;
	}

	/** Gets the currently selected data object. */
	public DataTransform getSelectedData() {
		final TreePath path = dataTree.getSelectionPath();
		if (path == null) return null;
		final DefaultMutableTreeNode node =
			(DefaultMutableTreeNode) path.getLastPathComponent();
		if (node == null) return null;
		final Object obj = node.getUserObject();
		if (!(obj instanceof DataTransform)) return null;
		return (DataTransform) obj;
	}

	/** Shows frame containing controls for the given data object. */
	public void showControls(final DataTransform data) {
		final JFrame frame = (JFrame) frameTable.get(data);
		if (frame == null) return;
		final WindowManager wm =
			(WindowManager) lm.getVisBio().getManager(WindowManager.class);
		wm.showWindow(frame);
	}

	/** Hides frame containing controls for the given data object. */
	public void hideControls(final DataTransform data) {
		final JFrame frame = (JFrame) frameTable.get(data);
		if (frame == null) return;
		frame.setVisible(false);
	}

	/** Exports a data object to disk. */
	public void exportData(final ImageTransform data) {
		exporter.setData(data);
		final int rval = exporter.showDialog(this);
		if (rval == DialogPane.APPROVE_OPTION) exporter.export();
	}

	/** Sends part of a data object to an instance of ImageJ. */
	public void sendDataToImageJ(final ImageTransform data) {
		sender.setData(data);
		if (data.getLengths().length > 0) {
			final int rval = sender.showDialog(this);
			if (rval != DialogPane.APPROVE_OPTION) return;
		}
		sender.send();
	}

	/** Creates a new display and adds the selected data object to it. */
	public void doNewDisplay(final boolean threeD) {
		final DataTransform data = getSelectedData();
		if (data == null) return;
		final DisplayManager disp =
			(DisplayManager) lm.getVisBio().getManager(DisplayManager.class);
		if (disp == null) return;
		final DisplayWindow window =
			disp.createDisplay(this, data.getName(), threeD);
		if (window == null) return;
		window.addTransform(data);
		final WindowManager wm =
			(WindowManager) lm.getVisBio().getManager(WindowManager.class);
		wm.showWindow(window);
	}

	/** Do new display with overlays */
	public void doNewDisplayWithOverlays() {
		final DataTransform data = getSelectedData();
		if (data == null) return;
		final DisplayManager disp =
			(DisplayManager) lm.getVisBio().getManager(DisplayManager.class);
		if (disp == null) return;
		final DisplayWindow window =
			disp.createDisplay(this, data.getName(), false);
		if (window == null) return;

		// construct overlays and add to data list and display window
		final String overlaysName = data.getName() + "  overlays";
		final OverlayTransform overlays = new OverlayTransform(data, overlaysName);
		final DataManager dm =
			(DataManager) lm.getVisBio().getManager(DataManager.class);
		if (dm == null) return;
		dm.addData(overlays);
		window.addTransform(data);
		window.addTransform(overlays);

		// show windows
		final WindowManager wm =
			(WindowManager) lm.getVisBio().getManager(WindowManager.class);
		wm.showWindow(window);
		dm.showControls(overlays);
	}

	// -- ActionListener API methods --

	/** Handles button presses. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final DataManager dm = (DataManager) lm;
		final String cmd = e.getActionCommand();

		if (cmd.equals("addData")) {
			final DataTransform data = getSelectedData();
			final Class[] dataTypes = dm.getRegisteredDataTypes();
			final String[] dataLabels = dm.getRegisteredDataLabels();

			// build popup menu from registered data transform types
			final JPopupMenu menu = new JPopupMenu();
			for (int i = 0; i < dataTypes.length; i++) {
				// check data transform compatibility via reflection
				final String clas = dataTypes[i].getName();
				boolean validParent = false;
				boolean needParent = true;
				try {
					final ReflectedUniverse r = new ReflectedUniverse();
					r.exec("import " + clas);
					r.setVar("data", data);
					final String n = clas.substring(clas.lastIndexOf(".") + 1);
					r.exec("valid = " + n + ".isValidParent(data)");
					validParent = ((Boolean) r.getVar("valid")).booleanValue();
					r.exec("need = " + n + ".isParentRequired()");
					needParent = ((Boolean) r.getVar("need")).booleanValue();
				}
				catch (final ReflectException exc) {
					exc.printStackTrace();
				}

				// add menu item for compatible transform type
				final JMenuItem item = new JMenuItem(dataLabels[i]);
				item.setMnemonic(dataLabels[i].charAt(0));
				item.setActionCommand(clas);
				item.addActionListener(this);
				item.setEnabled(validParent || !needParent);
				menu.add(item);
			}

			// show popup menu
			final JButton source = (JButton) e.getSource();
			menu.show(source, source.getWidth(), 0);
		}
		else if (cmd.equals("display")) {
			final DataTransform data = getSelectedData();
			if (data == null) return;
			final DisplayManager disp =
				(DisplayManager) lm.getVisBio().getManager(DisplayManager.class);
			if (disp == null) return;
			final DisplayWindow[] dd = disp.getDisplays();

			final boolean canDisplay2D = data.canDisplay2D();
			final boolean canDisplay3D = data.canDisplay3D();

			// build popup menu from display list
			final JPopupMenu menu = new JPopupMenu();
			for (int i = 0; i < dd.length; i++) {
				final DisplayWindow window = dd[i];
				final JMenuItem item = new JMenuItem(window.toString());
				item.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(final ActionEvent e) {
						// add selected data object to chosen display
						final DataTransform data = getSelectedData();
						if (data != null) window.addTransform(data);
						final WindowManager wm =
							(WindowManager) lm.getVisBio().getManager(WindowManager.class);
						wm.showWindow(window);
					}
				});
				final boolean threeD = window.is3D();
				final boolean valid =
					(!threeD && canDisplay2D) || (threeD && canDisplay3D);
				item.setEnabled(valid && !window.hasTransform(data));
				menu.add(item);
			}
			if (dd.length > 0) menu.addSeparator();
			menu.add(display2D);
			menu.add(display3D);

			// display popup menu
			final JButton source = (JButton) e.getSource();
			menu.show(source, source.getWidth(), 0);
		}
		else if (cmd.equals("edit")) showControls(getSelectedData());
		else if (cmd.equals("export")) {
			// build popup menu
			final JPopupMenu menu = new JPopupMenu();
			final DataTransform data = getSelectedData();

			final JMenuItem saveToDisk = new JMenuItem("Save to disk...");
			saveToDisk.setMnemonic('s');
			saveToDisk.setActionCommand("saveToDisk");
			saveToDisk.addActionListener(this);
			menu.add(saveToDisk);

			final JMenuItem sendToIJ = new JMenuItem("Send to ImageJ...");
			sendToIJ.setMnemonic('i');
			sendToIJ.setActionCommand("sendToIJ");
			sendToIJ.addActionListener(this);
			menu.add(sendToIJ);

			final JMenuItem uploadToOME = new JMenuItem("Upload to OME...");
			uploadToOME.setMnemonic('u');
			uploadToOME.setActionCommand("uploadToOME");
			uploadToOME.addActionListener(this);
			uploadToOME.setEnabled(data instanceof Dataset);
			menu.add(uploadToOME);

			// show popup menu
			final JButton source = (JButton) e.getSource();
			menu.show(source, source.getWidth(), 0);
		}
		else if (cmd.equals("removeData")) dm.removeData(getSelectedData(), true);
		else if (cmd.equals("new2D")) doNewDisplay(false);
		else if (cmd.equals("new3D")) doNewDisplay(true);
		else if (cmd.equals("saveToDisk")) dm.exportData();
		else if (cmd.equals("sendToIJ")) dm.sendDataToImageJ();
		else {
			// command represents a class to instantiate via reflection
			DataTransform data = null;
			try {
				final ReflectedUniverse r = new ReflectedUniverse();
				r.exec("import " + cmd);
				r.setVar("dm", dm);
				final String n = cmd.substring(cmd.lastIndexOf(".") + 1);
				r.exec("data = " + n + ".makeTransform(dm)");
				data = (DataTransform) r.getVar("data");
			}
			catch (final ReflectException exc) {
				exc.printStackTrace();
			}
			if (data != null) dm.addData(data);
		}
	}

	// -- TransformListener API methods --

	/** Updates data info panel if a data transform changes while selected. */
	@Override
	public void transformChanged(final TransformEvent e) {
		final int id = e.getId();
		if (id != TransformEvent.DATA_CHANGED) return;
		final DataTransform data = (DataTransform) e.getSource();
		if (data != getSelectedData()) return;
		// System.out.println("transform changed"); // TEMP
		// System.out.println("e.getSource = " + data); // TEMP
		// System.out.println("e.getSource().getClass().getName() = " +
		// data.getClass().getName()); // TEMP
		doDataInfo(data);
	}

	// -- TreeSelectionListener API methods --

	/** Handles tree selection changes. */
	@Override
	public void valueChanged(final TreeSelectionEvent e) {
		doDataInfo(getSelectedData());
	}

	// -- Helper methods --

	/** Finds the tree node representing this data object. */
	protected DefaultMutableTreeNode findNode(final DataTransform data) {
		if (data == null) return null;
		final Enumeration list = dataRoot.breadthFirstEnumeration();
		while (list.hasMoreElements()) {
			final DefaultMutableTreeNode node =
				(DefaultMutableTreeNode) list.nextElement();
			final Object obj = node.getUserObject();
			if (obj instanceof DataTransform) {
				final DataTransform dt = (DataTransform) obj;
				if (dt == data) return node;
			}
		}
		return null;
	}

	/** Selects the given tree node. */
	protected void selectNode(final DefaultMutableTreeNode node) {
		if (node == null) dataTree.clearSelection();
		else {
			final TreePath path = new TreePath(node.getPath());
			dataTree.setSelectionPath(path);
			dataTree.scrollPathToVisible(path);
		}
	}

	/** Updates data information panel, and toggles button availability. */
	protected void doDataInfo(final DataTransform data) {
		// update data information panel
		final StringBuffer sb = new StringBuffer("<html><body>\n");
		if (data == null) {
			if (dataRoot.isLeaf()) {
				sb.append("<center>\nNo datasets have been loaded.<br>\n"
					+ "&nbsp;&nbsp;Press the \"Add\" button "
					+ "to import a dataset.&nbsp;&nbsp;\n</center>");
			}
			else {
				sb.append("<center>\nNo data is selected.<br>\n"
					+ "&nbsp;&nbsp;Click a data object to see "
					+ "its information.&nbsp;&nbsp;\n</center>");
			}
		}
		else sb.append(data.getHTMLDescription());
		sb.append("\n</body></html>");
		dataInfo.setText(sb.toString());

		// toggle button availability
		final VisBioFrame bio = lm.getVisBio();
		final boolean isData = data != null;
		final boolean isImage = data instanceof ImageTransform;
		final boolean canDisplay2D = isData && data.canDisplay2D();
		final boolean canDisplay3D = isData && data.canDisplay3D();
		final boolean canDisplay = canDisplay2D || canDisplay3D;
		final boolean hasControls = isData && frameTable.get(data) != null;
		display.setEnabled(canDisplay);
		editData.setEnabled(hasControls);
		export.setEnabled(isImage);
		final JMenuItem exportItem = bio.getMenuItem("File", "Export data...");
		if (exportItem != null) exportItem.setEnabled(isImage);
		removeData.setEnabled(isData);
		if (display2D != null) display2D.setEnabled(canDisplay2D);
		if (display3D != null) {
			display3D.setEnabled(DisplayUtil.canDo3D() && canDisplay3D);
		}
	}

}
