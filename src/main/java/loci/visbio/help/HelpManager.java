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

package loci.visbio.help;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.looks.LookUtils;

import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;

import loci.visbio.LogicManager;
import loci.visbio.VisBio;
import loci.visbio.VisBioEvent;
import loci.visbio.VisBioFrame;
import loci.visbio.WindowManager;
import loci.visbio.data.DataControls;
import loci.visbio.data.DataManager;
import loci.visbio.data.DataTransform;
import loci.visbio.state.BooleanOption;
import loci.visbio.state.OptionManager;
import loci.visbio.state.StateManager;
import loci.visbio.util.DisplayUtil;
import loci.visbio.util.LAFUtil;
import visad.util.GUIFrame;

/**
 * HelpManager is the manager encapsulating VisBio's help window logic.
 */
public class HelpManager extends LogicManager {

	// -- Constants --

	/** URL of VisBio web page. */
	public static final String URL_VISBIO =
		"http://www.loci.wisc.edu/software/visbio";

	/** String for displaying new data. */
	public static final String DISPLAY_DATA =
		"Ask about displaying new data objects";

	// -- Fields --

	/** Help dialog for detailing basic program usage. */
	protected HelpWindow helpWindow;

	// -- Constructor --

	/** Constructs an exit manager. */
	public HelpManager(final VisBioFrame bio) {
		super(bio);
	}

	// -- HelpManager API methods --

	/** Adds a new help topic. */
	public void addHelpTopic(final String name, final String source) {
		helpWindow.addTopic(name, source);
	}

	/**
	 * Prompts user for whether to visualize the currently selected data object.
	 */
	public void checkVisualization() {
		final DataManager dm = (DataManager) bio.getManager(DataManager.class);
		final DataTransform data = dm.getSelectedData();
		if (data.getParent() != null) return; // ask for top-level objects only

		// determine whether data can be displayed in 2D and/or 3D
		final boolean isData = data != null;
		final boolean canDisplay2D = data != null && data.canDisplay2D();
		final boolean canDisplay3D =
			data != null && data.canDisplay3D() && DisplayUtil.canDo3D();
		final boolean canDisplay = canDisplay2D || canDisplay3D;

		if (canDisplay) {
			// create option for 3D visualization
			final ButtonGroup buttons = new ButtonGroup();
			final JRadioButton vis3D = new JRadioButton("In 3D", canDisplay3D);
			vis3D.setEnabled(canDisplay3D);
			if (!LAFUtil.isMacLookAndFeel()) vis3D.setMnemonic('3');
			buttons.add(vis3D);

			// create option for 2D visualization
			final JRadioButton vis2D = new JRadioButton("In 2D", !canDisplay3D);
			vis2D.setEnabled(canDisplay2D);
			if (!LAFUtil.isMacLookAndFeel()) vis2D.setMnemonic('2');
			buttons.add(vis2D);

			// create option for 2D visualization w/ overlays
			final JRadioButton visOver =
				new JRadioButton("In 2D with overlays", !canDisplay3D);
			visOver.setEnabled(canDisplay2D);
			if (!LAFUtil.isMacLookAndFeel()) visOver.setMnemonic('o');
			buttons.add(visOver);

			// create option for no visualization
			final JRadioButton visNot = new JRadioButton("Not now");
			if (!LAFUtil.isMacLookAndFeel()) visNot.setMnemonic('n');
			buttons.add(visNot);

			// create panel for asking user about immediate visualization
			final PanelBuilder builder =
				new PanelBuilder(new FormLayout("15dlu, pref:grow, 15dlu",
					"pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref"));
			final CellConstraints cc = new CellConstraints();
			builder.addLabel("Would you like to visualize the \"" + data +
				"\" object now?", cc.xyw(1, 1, 3));
			builder.add(vis3D, cc.xy(2, 3));
			builder.add(vis2D, cc.xy(2, 5));
			builder.add(visOver, cc.xy(2, 7));
			builder.add(visNot, cc.xy(2, 9));

			final JPanel visPanel = builder.getPanel();

			// display message pane
			final DataControls dc = dm.getControls();
			final OptionManager om =
				(OptionManager) bio.getManager(OptionManager.class);
			final boolean success =
				om.checkMessage(dc, DISPLAY_DATA, false, visPanel, "VisBio");
			if (success && !visNot.isSelected()) {
				if (!visOver.isSelected()) dc.doNewDisplay(vis3D.isSelected());
				else dc.doNewDisplayWithOverlays();
			}
		}
	}

	// -- LogicManager API methods --

	/** Called to notify the logic manager of a VisBio event. */
	@Override
	public void doEvent(final VisBioEvent evt) {
		final int eventType = evt.getEventType();
		if (eventType == VisBioEvent.LOGIC_ADDED) {
			final LogicManager lm = (LogicManager) evt.getSource();
			if (lm == this) doGUI();
		}
		else if (eventType == VisBioEvent.STATE_CHANGED) {
			final String msg = evt.getMessage();
			if ("add data".equals(msg)) {
				final OptionManager om =
					(OptionManager) bio.getManager(OptionManager.class);
				final StateManager sm =
					(StateManager) bio.getManager(StateManager.class);
				final BooleanOption option = (BooleanOption) om.getOption(DISPLAY_DATA);
				if (option.getValue() && !sm.isRestoring()) {
					// ask about displaying new data objects
					checkVisualization();
				}
			}
		}
	}

	/** Gets the number of tasks required to initialize this logic manager. */
	@Override
	public int getTasks() {
		return 4;
	}

	// -- Helper methods --

	/** Adds help-related GUI components to VisBio. */
	private void doGUI() {
		bio.setSplashStatus("Initializing help logic");
		helpWindow = new HelpWindow();
		final WindowManager wm =
			(WindowManager) bio.getManager(WindowManager.class);
		wm.addWindow(helpWindow);

		// help menu
		bio.setSplashStatus(null);

		final JMenuItem help =
			bio.addMenuItem("Help", "VisBio Help",
				"loci.visbio.help.HelpManager.helpHelp", 'h');
		final KeyStroke helpStroke =
			LookUtils.IS_OS_MAC ? KeyStroke.getKeyStroke(new Character('?'),
				GUIFrame.MENU_MASK) : KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0);
		help.setAccelerator(helpStroke);

		if (!LookUtils.IS_OS_MAC) {
			bio.addMenuSeparator("Help");
			bio.addMenuItem("Help", "About",
				"loci.visbio.help.HelpManager.helpAbout", 'a');
			bio.setMenuShortcut("Help", "About", KeyEvent.VK_A);
		}

		// options
		bio.setSplashStatus(null);
		final OptionManager om =
			(OptionManager) bio.getManager(OptionManager.class);
		om.addBooleanOption("General", DISPLAY_DATA, 'd',
			"Toggles whether VisBio asks about automatically "
				+ "displaying new data objects", true);

		// help topics
		bio.setSplashStatus(null);
		addHelpTopic("Introduction", "introduction.html");
		addHelpTopic("Tutorials", "tutorials.html"); // tutorials index
		addHelpTopic("Tutorials/Getting started with measuring", "measuring.html");
	}

	private void makeVisPanel() {}

	// -- Menu commands --

	/** Brings up a window detailing basic program usage. */
	public void helpHelp() {
		final WindowManager wm =
			(WindowManager) bio.getManager(WindowManager.class);
		wm.showWindow(helpWindow);
	}

	/** Brings up VisBio's about dialog. */
	public void helpAbout() {
		final String about =
			VisBio.TITLE + " " + VisBio.VERSION +
				(VisBio.DATE.equals("@da" + "te@") ? "" : (", built " + VisBio.DATE)) +
				"\nWritten by " + VisBio.AUTHOR + "\n" + URL_VISBIO;
		JOptionPane.showMessageDialog(bio, about, "About VisBio",
			JOptionPane.INFORMATION_MESSAGE);
	}

}
