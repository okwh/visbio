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

package loci.visbio.state;

/**
 * Strings related to the SpreadsheetLaunchOption option.
 */
public final class SpreadsheetOptionStrategy {

  private SpreadsheetOptionStrategy() {}

  /** Returns the text for the spreadsheet option. */
  public static String getText() {
    return "Automatically launch spreadsheet application " +
      "when exporting overlays";
  }

  /** Returns the text tooltip for the spreadsheet option. */
  public static String getTextTip() {
    return "The path to the spreadsheet application to launch";
  }

  /** Returns the checkbox tool tip for the spreadsheet option. */
  public static String getBoxTip() {
    return  "Toggles whether spreadsheet application is automatically" +
      " launched when overlays are exported";
  }

  /** Returns the label for the spreadsheet option. */
  public static String getLabel() {
    return "Path to Spreadsheet Application:";
  }

  /** Returns the button tip for the spreadsheet option. */
  public static String getButtonTip() {
    return "Restore the default predicted path to spreadsheet application";
  }
}

