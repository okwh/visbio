//
// ImageUploader.java
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

package loci.visbio.ome;

import java.util.Vector;

import loci.formats.FormatTools;
import loci.formats.MetadataTools;
import loci.formats.StatusEvent;
import loci.formats.StatusListener;
import loci.formats.gui.BufferedImageWriter;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.meta.MetadataStore;
import loci.ome.io.OMEWriter;

/**
 * ImageUploader is a helper class for uploading VisBio datasets
 * (OME images) to the Open Microscopy Environment.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/visbio/src/loci/visbio/ome/ImageUploader.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/visbio/src/loci/visbio/ome/ImageUploader.java">SVN</a></dd></dl>
 */
public class ImageUploader {

  // -- Fields --

  /** List of objects listening for updates to upload tasks. */
  protected Vector listeners;

  // -- Constructor --

  /** Constructs a new OME image uploader. */
  public ImageUploader() { listeners = new Vector(); }

  // -- ImageUploader API methods --

  /**
   * Uploads the given VisBio dataset (OME image) to the specified
   * OME server, using the given username and password.
   */
  public void upload(loci.visbio.data.Dataset data,
    String server, String username, String password)
  {
    try {
      OMEWriter writer = new OMEWriter();
      BufferedImageWriter biWriter = new BufferedImageWriter(writer);
      MetadataStore store = MetadataTools.createOMEXMLMetadata();
      store.setRoot(data.getOMEXMLRoot());
      MetadataRetrieve retrieve = (MetadataRetrieve) store;
      writer.setMetadataRetrieve(retrieve);

      String id = server + "?user=" + username + "&password=" + password;
      writer.setId(id);

      String order = retrieve.getPixelsDimensionOrder(0, 0);
      int sizeZ = retrieve.getPixelsSizeZ(0, 0).intValue();
      int sizeC = retrieve.getPixelsSizeZ(0, 0).intValue();
      int sizeT = retrieve.getPixelsSizeZ(0, 0).intValue();

      int[] len = data.getLengths();
      int total = FormatTools.getRasterLength(len);
      int[] cLen = new int[len.length - 2];
      System.arraycopy(len, 2, cLen, 0, cLen.length);

      for (int i=0; i<total; i++) {
        int[] zct = FormatTools.getZCTCoords(order,
          sizeZ, sizeC, sizeT, total, i);
        int[] cPos = FormatTools.rasterToPosition(cLen, zct[1]);
        int[] pos = new int[2 + cPos.length];
        pos[0] = zct[2];
        pos[1] = zct[0];
        System.arraycopy(cPos, 0, pos, 2, cPos.length);
        biWriter.saveImage(data.getImage(pos), i == total - 1);
      }

      writer.close();
    }
    catch (Exception exc) {
      notifyListeners(new StatusEvent(1, 1,
        "Error uploading (see error console for details)"));
      exc.printStackTrace();
    }
  }

  /** Adds an upload task listener. */
  public void addStatusListener(StatusListener l) {
    synchronized (listeners) { listeners.addElement(l); }
  }

  /** Removes an upload task listener. */
  public void removeStatusListener(StatusListener l) {
    synchronized (listeners) { listeners.removeElement(l); }
  }

  /** Removes all upload task listeners. */
  public void removeAllStatusListeners() {
    synchronized (listeners) { listeners.removeAllElements(); }
  }

  /** Notifies listeners of an upload task update. */
  protected void notifyListeners(StatusEvent e) {
    synchronized (listeners) {
      for (int i=0; i<listeners.size(); i++) {
        StatusListener l = (StatusListener) listeners.elementAt(i);
        l.statusUpdated(e);
      }
    }
  }

}
