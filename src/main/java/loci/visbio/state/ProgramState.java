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
import org.w3c.dom.Document;

/**
 * A VisBio state wrapper.
 */
public class ProgramState {

  // -- Fields --

  /** Message to be displayed as part of "Undo" and "Redo" menu items. */
  public String msg;

  /** State object for use in state restoration. */
  public Document state;

  // -- Constructor --

  /** Constructs a VisBio state wrapper. */
  public ProgramState(String msg, Document state) {
    this.msg = msg;
    this.state = state;
  }

}
