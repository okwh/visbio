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

package loci.visbio.view;

import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import loci.visbio.ControlPanel;
import loci.visbio.LogicManager;
import loci.visbio.WindowManager;
import loci.visbio.util.DisplayUtil;
import loci.visbio.util.LAFUtil;
import loci.visbio.util.SwingUtil;

/**
 * DisplayControls is the control panel for managing displays.
 */
public class DisplayControls extends ControlPanel implements ActionListener,
	ListSelectionListener
{

	// -- GUI components --

	/** List of displays. */
	protected JList displayList;

	/** Display list model. */
	protected DefaultListModel listModel;

	/** Button for adding a 2D display to the list. */
	protected JButton add2D;

	/** Button for adding a 3D display to the list. */
	protected JButton add3D;

	/** Button for showing a display onscreen. */
	protected JButton show;

	/** Button for removing a display from the list. */
	protected JButton remove;

	// -- Constructor --

	/** Constructs a tool panel for adjusting data parameters. */
	public DisplayControls(final LogicManager logic) {
		super(logic, "Displays", "Controls for managing displays");

		// list of displays
		listModel = new DefaultListModel();
		displayList = new JList(listModel);
		displayList.setFixedCellWidth(250);
		displayList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		displayList.addListSelectionListener(this);
		final JScrollPane listPane = new JScrollPane(displayList);
		SwingUtil.configureScrollPane(listPane);

		// add 2D button
		add2D = new JButton("Add 2D...");
		add2D.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) add2D.setMnemonic('2');
		add2D.setToolTipText("Creates a new 2D display");

		// add 3D button
		add3D = new JButton("Add 3D...");
		add3D.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) add3D.setMnemonic('3');
		add3D.setToolTipText("Creates a new 3D display");
		add3D.setEnabled(DisplayUtil.canDo3D());

		// show button
		show = new JButton("Show");
		show.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) show.setMnemonic('s');
		show.setToolTipText("Displays the selected display onscreen");
		show.setEnabled(false);

		// remove data button
		remove = new JButton("Remove");
		remove.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) remove.setMnemonic('m');
		remove.setToolTipText("Deletes the selected display");
		remove.setEnabled(false);

		// lay out buttons
		final ButtonStackBuilder bsb = new ButtonStackBuilder();
		bsb.addGridded(add2D);
		bsb.addRelatedGap();
		bsb.addGridded(add3D);
		bsb.addUnrelatedGap();
		bsb.addGridded(show);
		bsb.addRelatedGap();
		bsb.addGridded(remove);
		final JPanel buttons = bsb.getPanel();

		// lay out components
		final PanelBuilder builder =
			new PanelBuilder(new FormLayout("pref:grow, 3dlu, pref", "pref"));
		final CellConstraints cc = new CellConstraints();
		builder.add(listPane, cc.xy(1, 1));
		builder.add(buttons, cc.xy(3, 1));
		add(builder.getPanel());
	}

	// -- DisplayControls API methods --

	/** Adds a display to the list of current displays. */
	public void addDisplay(final DisplayWindow d) {
		listModel.addElement(d);
		displayList.setSelectedValue(d, true);
	}

	/** Removes a display from the list of current displays. */
	public void removeDisplay(final DisplayWindow d) {
		if (d == null) return;
		listModel.removeElement(d);

		// remove display window from window manager
		final WindowManager wm =
			(WindowManager) lm.getVisBio().getManager(WindowManager.class);
		wm.disposeWindow(d);

		// discard object
		d.discard();
	}

	/** Gets the current list of displays. */
	public DisplayWindow[] getDisplays() {
		final DisplayWindow[] d = new DisplayWindow[listModel.size()];
		listModel.copyInto(d);
		return d;
	}

	/** Refreshes GUI components based on current selection. */
	public void refresh() {
		final DisplayWindow d = (DisplayWindow) displayList.getSelectedValue();
		show.setEnabled(isShowable(d));
		remove.setEnabled(d != null);
	}

	// -- ActionListener API methods --

	/** Handles button presses. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final DisplayManager dm = (DisplayManager) lm;
		final Object src = e.getSource();
		if (src == add2D) dm.createDisplay(this, false);
		else if (src == add3D) dm.createDisplay(this, true);
		else if (src == show) showDisplay();
		else if (src == remove) {
			dm.removeDisplay((DisplayWindow) displayList.getSelectedValue());
		}
	}

	// -- ListSelectionListener API methods --

	/** Handles list selection changes. */
	@Override
	public void valueChanged(final ListSelectionEvent e) {
		refresh();
		showDisplay();
	}

	// -- Helper methods --

	/** Shows the selected display onscreen. */
	protected void showDisplay() {
		final DisplayWindow d = (DisplayWindow) displayList.getSelectedValue();
		if (isShowable(d)) {
			final WindowManager wm =
				(WindowManager) lm.getVisBio().getManager(WindowManager.class);
			wm.showWindow(d);
		}
	}

	/** Gets whether the given display should be allowed to be shown onscreen. */
	protected boolean isShowable(final DisplayWindow d) {
		return d != null && d.getTransformHandler().getTransformCount() > 0;
	}

}
