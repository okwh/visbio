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

package loci.visbio;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import loci.visbio.util.SwingUtil;

/**
 * TaskControls is the control panel for managing tasks.
 */
public class TaskControls extends ControlPanel {

	// -- GUI components --

	/** Pane containing task widgets. */
	private final JPanel pane;

	/** Scroll pane wrapping pane. */
	private final JScrollPane scroll;

	// -- Constructor --

	/** Constructs a tool panel for controlling tasks. */
	public TaskControls(final LogicManager logic) {
		super(logic, "Tasks", "Controls for managing tasks");

		// pane containing list of tasks
		pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		pane.setBorder(new EmptyBorder(5, 5, 5, 5));
		pane.add(Box.createVerticalGlue());

		// scroll pane
		scroll = new JScrollPane(pane);
		SwingUtil.configureScrollPane(scroll);

		add(scroll);
	}

	// -- TaskControls API methods --

	/** Adds a task to the tasks pane. */
	public void addTask(final BioTask task) {
		pane.add(task, 0);
	}

	/** Removes a task from the tasks pane. */
	public void removeTask(final BioTask task) {
		pane.remove(task);
		scroll.validate();
		scroll.repaint();
	}

	/** Gets the preferred width of tasks. */
	public int getPreferredTaskWidth() {
		return scroll.getSize().width;
	}

}
