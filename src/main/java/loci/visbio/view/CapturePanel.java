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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import loci.visbio.VisBioFrame;
import loci.visbio.util.FormsUtil;
import loci.visbio.util.LAFUtil;
import loci.visbio.util.SwingUtil;
import visad.DisplayImpl;
import visad.ProjectionControl;
import visad.VisADException;

/**
 * Provides GUI controls for a display capture handler.
 */
public class CapturePanel extends JPanel implements ActionListener,
	ChangeListener, ItemListener, ListSelectionListener
{

	// -- Fields --

	/** Capture handler for this capture window. */
	protected CaptureHandler handler;

	/** Position list. */
	protected JList posList;

	/** Position list model. */
	protected DefaultListModel posListModel;

	/** Button for removing selected position. */
	protected JButton remove;

	/** Button for moving selected position upward. */
	protected JButton moveUp;

	/** Button for moving selected position downward. */
	protected JButton moveDown;

	/** Slider for adjusting movie speed. */
	protected JSlider speed;

	/** Output movie frames per second. */
	protected JSpinner fps;

	/** Check box for animation smoothness. */
	protected JCheckBox smooth;

	/** Progress bar for movie capture operation. */
	protected JProgressBar progress;

	// -- Constructor --

	/** Constructs a window for capturing display screenshots and movies. */
	public CapturePanel(final CaptureHandler h) {
		super();
		handler = h;

		// positions list
		posListModel = new DefaultListModel();
		posList = new JList(posListModel);
		posList.setFixedCellWidth(120);
		posList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		posList.addListSelectionListener(this);
		posList.setToolTipText("List of captured display positions");

		// add button
		final JButton add = new JButton("Add");
		add.setActionCommand("Add");
		add.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) add.setMnemonic('a');
		add.setToolTipText("Adds the current display position to the list");

		// remove button
		remove = new JButton("Remove");
		remove.setActionCommand("Remove");
		remove.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) remove.setMnemonic('r');
		remove.setToolTipText("Removes the selected position from the list");
		remove.setEnabled(false);

		// up button
		moveUp = new JButton("Up");
		moveUp.setActionCommand("Up");
		moveUp.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) moveUp.setMnemonic('u');
		moveUp.setToolTipText("Moves the selected position up in the list");
		moveUp.setEnabled(false);

		// down button
		moveDown = new JButton("Down");
		moveDown.setActionCommand("Down");
		moveDown.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) moveDown.setMnemonic('d');
		moveDown.setToolTipText("Moves the selected position down in the list");
		moveDown.setEnabled(false);

		// snapshot button
		final JButton snapshot = new JButton("Snapshot");
		snapshot.setActionCommand("Snapshot");
		snapshot.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) snapshot.setMnemonic('n');
		snapshot.setToolTipText("Saves display snapshot to an image file");

		// send to ImageJ button
		final JButton sendToImageJ = new JButton("Send to ImageJ");
		sendToImageJ.setActionCommand("SendImageJ");
		sendToImageJ.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) sendToImageJ.setMnemonic('j');
		sendToImageJ.setToolTipText("Sends display snapshot to ImageJ program");

		// speed slider
		speed = new JSlider(0, 16, 8);
		speed.setAlignmentY(Component.TOP_ALIGNMENT);
		speed.setMajorTickSpacing(4);
		speed.setMinorTickSpacing(1);
		final Hashtable speedHash = new Hashtable();
		speedHash.put(new Integer(0), new JLabel("4 (slow)"));
		speedHash.put(new Integer(4), new JLabel("2"));
		speedHash.put(new Integer(8), new JLabel("1"));
		speedHash.put(new Integer(12), new JLabel("1/2"));
		speedHash.put(new Integer(16), new JLabel("1/4 (fast)"));
		speed.setLabelTable(speedHash);
		speed.setSnapToTicks(true);
		speed.setPaintTicks(true);
		speed.setPaintLabels(true);
		speed.addChangeListener(this);
		speed.setToolTipText("Adjusts seconds per transition");

		// frames per second spinner
		fps = new JSpinner(new SpinnerNumberModel(10, 1, 600, 1));
		fps.addChangeListener(this);
		fps.setToolTipText("Adjusts output movie's frames per second");

		// smoothness checkbox
		smooth =
			new JCheckBox("Emphasize transition at each display position", true);
		smooth.addItemListener(this);
		if (!LAFUtil.isMacLookAndFeel()) smooth.setMnemonic('e');
		smooth.setToolTipText("Use smooth sine function transitions");

		// record button
		final JButton record = new JButton("Record >");
		record.setActionCommand("Record");
		record.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) record.setMnemonic('r');
		record
			.setToolTipText("Records a movie of transitions between display positions");

		// progress bar
		progress = new JProgressBar(0, 100);
		progress.setString("");
		progress.setStringPainted(true);
		progress.setToolTipText("Displays movie recording progress");

		// lay out buttons
		final ButtonStackBuilder bsb = new ButtonStackBuilder();
		bsb.addGridded(add);
		bsb.addRelatedGap();
		bsb.addGridded(remove);
		bsb.addUnrelatedGap();
		bsb.addGridded(moveUp);
		bsb.addRelatedGap();
		bsb.addGridded(moveDown);
		final JPanel buttons = bsb.getPanel();

		// lay out position list
		final PanelBuilder positionList =
			new PanelBuilder(new FormLayout("default:grow, 3dlu, pref",
				"fill:pref:grow"));
		final CellConstraints cc = new CellConstraints();
		final JScrollPane posScroll = new JScrollPane(posList);
		SwingUtil.configureScrollPane(posScroll);
		positionList.add(posScroll, cc.xy(1, 1));
		positionList.add(buttons, cc.xy(3, 1));

		// lay out transition speed slider
		final PanelBuilder transitionSpeed =
			new PanelBuilder(new FormLayout(
				"pref, 3dlu, pref, 3dlu, pref:grow, 3dlu, pref", "pref"));
		final JLabel speedLabel =
			transitionSpeed.addLabel("&Seconds per transition", cc.xy(1, 1));
		speedLabel.setLabelFor(speed);
		transitionSpeed.add(speed, cc.xy(5, 1));

		// lay out movie recording button and progress bar
		final PanelBuilder movieRecord =
			new PanelBuilder(new FormLayout("pref, 3dlu, pref:grow", "pref"));
		movieRecord.add(record, cc.xy(1, 1));
		movieRecord.add(progress, cc.xy(3, 1));

		// lay out components
		final JPanel pane =
			FormsUtil.makeColumn(new Object[] { "Display positions",
				positionList.getPanel(), "Screenshots",
				FormsUtil.makeRow(snapshot, sendToImageJ), "Movies",
				FormsUtil.makeRow("&Frames per second", fps),
				transitionSpeed.getPanel(), smooth, movieRecord.getPanel() },
				"pref:grow", false);
		setLayout(new BorderLayout());
		add(pane);
	}

	// -- CapturePanel API methods --

	/** Sets the progress bar's value. */
	public void setProgressValue(final int value) {
		progress.setValue(value);
	}

	/** Sets the progress bar's message. */
	public void setProgressMessage(final String msg) {
		progress.setString(msg);
	}

	/** Sets positions on the list. */
	public void setPositions(final Vector v) {
		posListModel.removeAllElements();
		if (v == null) return;
		final int size = v.size();
		for (int i = 0; i < size; i++)
			posListModel.addElement(v.elementAt(i));
	}

	/** Sets movie speed. */
	public void setSpeed(final int speed) {
		if (speed != getSpeed()) this.speed.setValue(speed);
	}

	/** Sets movie frames per second. */
	public void setFPS(final int fps) {
		if (fps != getFPS()) {
			final SpinnerNumberModel fpsModel =
				(SpinnerNumberModel) this.fps.getModel();
			fpsModel.setValue(new Integer(fps));
		}
	}

	/** Sets whether transitions use a smoothing sine function. */
	public void setSmooth(final boolean smooth) {
		if (smooth != isSmooth()) this.smooth.setSelected(smooth);
	}

	/** Gets positions on the list. */
	public Vector getPositions() {
		final int size = posListModel.size();
		final Vector v = new Vector(size);
		for (int i = 0; i < size; i++)
			v.add(posListModel.elementAt(i));
		return v;
	}

	/** Gets movie speed. */
	public int getSpeed() {
		return speed.getValue();
	}

	/** Gets movie frames per second. */
	public int getFPS() {
		return ((Integer) fps.getValue()).intValue();
	}

	/** Gets whether transitions use a smoothing sine function. */
	public boolean isSmooth() {
		return smooth.isSelected();
	}

	// -- ActionListener API methods --

	/** Called when a button is pressed. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final DisplayWindow display = handler.getWindow();
		final VisBioFrame bio = display.getVisBio();

		final String cmd = e.getActionCommand();
		if (cmd.equals("Add")) {
			final DisplayImpl d = display.getDisplay();
			if (d == null) return;
			final ProjectionControl pc = d.getProjectionControl();
			final double[] matrix = pc.getMatrix();
			final String nextPos = "position" + (posListModel.getSize() + 1);
			final String value =
				(String) JOptionPane.showInputDialog(this, "Position name:",
					"Add display position", JOptionPane.INFORMATION_MESSAGE, null, null,
					nextPos);
			if (value == null) return;
			posListModel.addElement(new DisplayPosition(value, matrix));
			bio.generateEvent(bio.getManager(DisplayManager.class),
				"position addition for " + display.getName(), true);
		}
		else if (cmd.equals("Remove")) {
			final int ndx = posList.getSelectedIndex();
			if (ndx >= 0) {
				posListModel.removeElementAt(ndx);
				if (posListModel.size() > ndx) posList.setSelectedIndex(ndx);
				else if (ndx > 0) posList.setSelectedIndex(ndx - 1);
			}
			bio.generateEvent(bio.getManager(DisplayManager.class),
				"position removal for " + display.getName(), true);
		}
		else if (cmd.equals("Up")) {
			final int ndx = posList.getSelectedIndex();
			if (ndx >= 1) {
				final Object o = posListModel.getElementAt(ndx);
				posListModel.removeElementAt(ndx);
				posListModel.insertElementAt(o, ndx - 1);
				posList.setSelectedIndex(ndx - 1);
			}
			bio.generateEvent(bio.getManager(DisplayManager.class),
				"position list modification for " + display.getName(), true);
		}
		else if (cmd.equals("Down")) {
			final int ndx = posList.getSelectedIndex();
			if (ndx >= 0 && ndx < posListModel.size() - 1) {
				final Object o = posListModel.getElementAt(ndx);
				posListModel.removeElementAt(ndx);
				posListModel.insertElementAt(o, ndx + 1);
				posList.setSelectedIndex(ndx + 1);
			}
			bio.generateEvent(bio.getManager(DisplayManager.class),
				"position list modification for " + display.getName(), true);
		}
		else if (cmd.equals("Snapshot")) handler.saveSnapshot();
		else if (cmd.equals("SendImageJ")) handler.sendToImageJ();
		else if (cmd.equals("Record")) {
			// build popup menu
			final JPopupMenu menu = new JPopupMenu();

			final JMenuItem aviMovie = new JMenuItem("AVI movie...");
			aviMovie.setMnemonic('m');
			aviMovie.setActionCommand("AviMovie");
			aviMovie.addActionListener(this);
			menu.add(aviMovie);

			final JMenuItem imageSequence = new JMenuItem("Image sequence...");
			imageSequence.setMnemonic('s');
			imageSequence.setActionCommand("ImageSequence");
			imageSequence.addActionListener(this);
			menu.add(imageSequence);

			// show popup menu
			final JButton source = (JButton) e.getSource();
			menu.show(source, source.getWidth(), 0);
		}
		else if (cmd.equals("AviMovie") || cmd.equals("ImageSequence")) {
			final int size = posListModel.size();
			final Vector matrices = new Vector(size);
			for (int i = 0; i < size; i++) {
				final DisplayPosition pos = (DisplayPosition) posListModel.elementAt(i);
				matrices.add(pos.getMatrix());
			}
			final double secPerTrans = Math.pow(2, 2 - speed.getValue() / 4.0);
			final int framesPerSec = getFPS();
			final boolean sine = smooth.isSelected();
			final boolean movie = cmd.equals("AviMovie");
			handler.captureMovie(matrices, secPerTrans, framesPerSec, sine, movie);
		}
	}

	// -- ChangeListener API methods --

	/** Called when slider or spinner is adjusted. */
	@Override
	public void stateChanged(final ChangeEvent e) {
		final DisplayWindow display = handler.getWindow();
		final VisBioFrame bio = display.getVisBio();

		final Object src = e.getSource();
		if (src == speed) {
			if (!speed.getValueIsAdjusting()) {
				bio.generateEvent(bio.getManager(DisplayManager.class),
					"transition speed adjustment for " + display.getName(), true);
			}
		}
		else if (src == fps) {
			bio.generateEvent(bio.getManager(DisplayManager.class),
				"capture FPS adjustment for " + display.getName(), true);
		}
	}

	// -- ItemListener API methods --

	/** Called when check box is toggled. */
	@Override
	public void itemStateChanged(final ItemEvent e) {
		final DisplayWindow display = handler.getWindow();
		final VisBioFrame bio = display.getVisBio();
		bio.generateEvent(bio.getManager(DisplayManager.class), (smooth
			.isSelected() ? "en" : "dis") +
			"able transition emphasis for " + display.getName(), true);
	}

	// -- ListSelectionListener API methods --

	/** Called when the a new display position is selected. */
	@Override
	public void valueChanged(final ListSelectionEvent e) {
		final int ndx = posList.getSelectedIndex();
		remove.setEnabled(ndx >= 0);
		moveUp.setEnabled(ndx > 0);
		moveDown.setEnabled(ndx < posListModel.getSize() - 1);
		if (ndx < 0) return;
		final DisplayPosition pos =
			(DisplayPosition) posListModel.getElementAt(ndx);
		final double[] matrix = pos.getMatrix();
		final DisplayWindow display = handler.getWindow();
		final DisplayImpl d = display.getDisplay();
		if (d == null) return;
		final ProjectionControl pc = d.getProjectionControl();
		try {
			pc.setMatrix(matrix);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
	}

}
