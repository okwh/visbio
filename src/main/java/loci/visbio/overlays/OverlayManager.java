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

package loci.visbio.overlays;

import loci.visbio.LogicManager;
import loci.visbio.VisBioEvent;
import loci.visbio.VisBioFrame;
import loci.visbio.data.DataManager;
import loci.visbio.help.HelpManager;
import loci.visbio.state.OptionManager;
import loci.visbio.state.SpreadsheetLaunchOption;

/**
 * OverlayManager is the manager encapsulating VisBio's overlay logic.
 */
public class OverlayManager extends LogicManager {

	// -- Constructor --

	/** Constructs a window manager. */
	public OverlayManager(final VisBioFrame bio) {
		super(bio);
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
	}

	/** Gets the number of tasks required to initialize this logic manager. */
	@Override
	public int getTasks() {
		return 2;
	}

	// -- Helper methods --

	/** Adds overlay-related GUI components to VisBio. */
	protected void doGUI() {
		// overlay transform registration
		bio.setSplashStatus("Initializing overlay logic");
		final DataManager dm = (DataManager) bio.getManager(DataManager.class);
		dm.registerDataType(OverlayTransform.class, "Overlays");

		// register Overlay options
		final OptionManager om =
			(OptionManager) bio.getManager(OptionManager.class);
		final String[] overlayTypes = OverlayUtil.getOverlayTypes();
		for (int i = 0; i < overlayTypes.length; i++) {
			final String[] statTypes = OverlayUtil.getStatTypes(overlayTypes[i]);
			for (int j = 0; j < statTypes.length; j++) {
				final String name = overlayTypes[i] + "." + statTypes[j];
				om.addBooleanOption("Overlays", name, '|', "Toggles whether the " +
					name + " statistic is exported or saved", true);
			}
		}

		// add option for launching spreadsheet automatically
		String path = "";
		try {
			path = SpreadsheetLauncher.getDefaultApplicationPath();
		}
		catch (final SpreadsheetLaunchException ex) {}
		om.addOption("General", new SpreadsheetLaunchOption('s', path, true));

		// help window
		bio.setSplashStatus(null);
		final HelpManager hm = (HelpManager) bio.getManager(HelpManager.class);
		hm.addHelpTopic("Data transforms/Overlays", "overlays.html");
	}

}
