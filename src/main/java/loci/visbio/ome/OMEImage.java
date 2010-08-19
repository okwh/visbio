//
// OMEImage.java
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

import java.awt.Component;
import java.awt.Dialog;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Vector;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import loci.common.ReflectException;
import loci.formats.FormatException;
import loci.formats.gui.AWTImageTools;
import loci.formats.gui.BufferedImageReader;
import loci.ome.io.OMECredentials;
import loci.ome.io.OMEReader;
import loci.ome.io.OMEUtils;
import loci.visbio.BioTask;
import loci.visbio.TaskManager;
import loci.visbio.data.DataCache;
import loci.visbio.data.DataManager;
import loci.visbio.data.DataTransform;
import loci.visbio.data.ImageTransform;
import loci.visbio.data.ThumbnailHandler;
import loci.visbio.state.Dynamic;
import loci.visbio.util.DialogPane;
import loci.visbio.util.ObjectUtil;
import visad.Data;
import visad.FlatField;
import visad.FunctionType;
import visad.Linear2DSet;
import visad.RealTupleType;
import visad.VisADException;

/**
 * A OMEImage object encompasses a multidimensional biological image series
 * obtained from an OME database.
 *
 * OMEImage keeps no data in memory itself, leaving that management to the
 * application, and just downloads data as necessary to return whatever the
 * application requests, according to the DataTransform API.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="https://skyking.microscopy.wisc.edu/trac/java/browser/trunk/components/visbio/src/loci/visbio/ome/OMEImage.java">Trac</a>,
 * <a href="https://skyking.microscopy.wisc.edu/svn/java/trunk/components/visbio/src/loci/visbio/ome/OMEImage.java">SVN</a></dd></dl>
 */
public class OMEImage extends ImageTransform {

  // -- Static fields --

  /** OMEImage import dialog. */
  protected static OMELoginPane login;

  // -- Data fields --

  /** Associated OME server. */
  protected String server;

  /** Session key for OME server. */
  protected String sessionKey;

  /** Username for OME server. */
  protected String user;

  /** Password for OME server. */
  protected String password;

  /** ID for this image. */
  protected long imageId;

  /** Optional task for constructor progress. */
  protected BioTask task;

  // -- Computed fields --

  /** Associated download helper object. */
  protected BufferedImageReader downloader;

  /** ID passed to the OMEReader object. */
  protected String id;

  /** Length of X dimension in this image. */
  protected int sizeX;

  /** Length of Y dimension in this image. */
  protected int sizeY;

  /** Number of focal planes in this image. */
  protected int sizeZ;

  /** Number of time points in this image. */
  protected int sizeT;

  /** Number of channels in this image. */
  protected int sizeC;

  /** Index for Slice dimension. */
  protected int indexZ;

  /** Index for Time dimension. */
  protected int indexT;

  // -- Constructors --

  /** Constructs an uninitialized OME image object. */
  public OMEImage() { super(); }

  /**
   * Constructs a new multidimensional data object from the given
   * OME server, session key and image ID.
   */
  public OMEImage(String server, String sessionKey,
    int imageId, BioTask task)
  {
    super(null, null);
    this.server = server;
    this.sessionKey = sessionKey;
    this.imageId = imageId;
    this.task = task;
    initState(null);
  }

  /**
   * Constructs a new multidimensional data object from the given
   * OME server, username, password and image ID.
   */
  public OMEImage(String server, String user, String password,
    int imageId, BioTask task)
  {
    super(null, null);
    this.server = server;
    this.user = user;
    this.password = password;
    this.imageId = imageId;
    this.task = task;
    initState(null);
  }

  // -- OMEImage API methods --

  /** Gets the full URL of the OME server from which to access the image. */
  public String getServer() { return server; }

  /**
   * Gets the name of the OME server (i.e., without
   * any trimmings such as the http:// prefix).
   */
  public String getServerName() {
    String s = server;
    if (s.startsWith("http://")) s = s.substring(7);
    int slash = s.indexOf("/");
    if (slash >= 0) s = s.substring(0, slash);
    return s;
  }

  /** Gets the session key to use when accessing the OME server. */
  public String getSessionKey() { return sessionKey; }

  /** Gets the username to use when accessing the OME server. */
  public String getUsername() { return user; }

  /** Gets the password to use when accessing the OME server. */
  public String getPassword() { return password; }

  /** Gets the image ID for the associated OME image. */
  public long getImageId() { return imageId; }

  // -- ImageTransform API methods --

  /** Gets width of each image. */
  public int getImageWidth() { return sizeX; }

  /** Gets height of each image. */
  public int getImageHeight() { return sizeY; }

  /** Gets number of range components at each pixel. */
  public int getRangeCount() { return sizeC; }

  // -- Static DataTransform API methods --

  /** Creates a new OME image, with user interaction. */
  public static DataTransform makeTransform(DataManager dm) {
    return makeTransform(dm, null, null, null, -1);
  }

  /**
   * Creates a new OME image, with user interaction,
   * using the given defaults.
   */
  public static DataTransform makeTransform(DataManager dm,
    String server, String sessionKey, String user, long imageId)
  {
    String password = null;
    Component parent = dm.getControls();

    if (sessionKey == null) {
      // create OME login dialog if it doesn't already exist
      if (login == null) login = new OMELoginPane();
      if (server != null) login.setServer(server);
      if (user != null) login.setUser(user);

      // get login information from login dialog
      int rval = login.showDialog(parent);
      if (rval != OMELoginPane.APPROVE_OPTION) return null;
      server = login.getServer();
      user = login.getUser();
      password = login.getPassword();

      // get image ID to download
      if (imageId < 0) {
        try {
          OMECredentials cred = new OMECredentials(user, server, password);
          OMEUtils.login(cred);

          // TODO : find a better way of handling multiple IDs
          long[] results = showTable(cred);
          if (results == null) results = new long[0];
          if (results.length > 0) {
            imageId = results[0];
          }
          else imageId = -1;
        }
        catch (Exception e) {
          e.printStackTrace();
          JOptionPane.showMessageDialog(parent, "Sorry, there has been a " +
            "problem downloading from the server. Please try again.",
            "VisBio", JOptionPane.ERROR_MESSAGE);
          imageId = -1;
        }
      }
      if (imageId < 0) return null;

      // confirm download before proceeding
      int val = JOptionPane.showConfirmDialog(parent, "Download image #" +
        imageId + " from server " + server + " as user " + user + "?",
        "VisBio", JOptionPane.YES_NO_OPTION);
      if (val != JOptionPane.YES_OPTION) return null;
    }
    else server = OMEManager.getProperServer(server);

    // set up task listener
    TaskManager tm = (TaskManager)
      dm.getVisBio().getManager(TaskManager.class);
    BioTask task = tm.createTask("OME image");
    task.setStoppable(false);

    // make sure everything goes ok
    try {
      // TODO
      return sessionKey != null ?
        new OMEImage(server, sessionKey, (int) imageId, task) :
        new OMEImage(server, user, password, (int) imageId, task);
    }
    catch (Exception exc) {
      exc.printStackTrace();
      JOptionPane.showMessageDialog(parent, "Sorry, there has been a " +
        "problem downloading from the server. Please try again.",
        "VisBio", JOptionPane.ERROR_MESSAGE);
    }
    return null;
  }

  /**
   * Indicates whether this transform type would accept
   * the given transform as its parent transform.
   */
  public static boolean isValidParent(DataTransform data) { return false; }

  /** Indicates whether this transform type requires a parent transform. */
  public static boolean isParentRequired() { return false; }

  // -- DataTransform API methods --

  /** Obtains an image from the source(s) at the given dimensional position. */
  public Data getData(int[] pos, int dim, DataCache cache) {
    if (dim != 2) return null;
    if (cache != null) return cache.getData(this, pos, null, dim);

    int z = indexZ < 0 ? 0 : pos[indexZ];
    int t = indexT < 0 ? 0 : pos[indexT];
    float[][] samples = new float[sizeC][];
    try {
      for (int c=0; c<sizeC; c++) {
        int ndx = downloader.getIndex(z, c, t);
        samples[c] = AWTImageTools.getFloats(downloader.openImage(ndx))[0];
      }
    }
    catch (FormatException exc) { exc.printStackTrace(); }
    catch (IOException exc) { exc.printStackTrace(); }

    FunctionType fieldType = getType();
    RealTupleType fieldDomain = fieldType.getDomain();
    try {
      Linear2DSet fieldSet = new Linear2DSet(fieldDomain,
        0, sizeX - 1, sizeX, sizeY - 1, 0, sizeY);
      FlatField field = new FlatField(fieldType, fieldSet);
      if (samples != null) field.setSamples(samples, false);
      return field;
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
    return null;
  }

  /** Gets whether this transform provides data of the given dimensionality. */
  public boolean isValidDimension(int dim) { return dim == 2; }

  /**
   * Gets a string id uniquely describing this data transform at the given
   * dimensional position, for the purposes of thumbnail caching.
   * If global flag is true, the id is suitable for use in the default,
   * global cache file.
   */
  public String getCacheId(int[] pos, boolean global) {
    if (pos == null) return null;
    return server + ";" + imageId + ";" + ObjectUtil.arrayToString(pos);
  }

  /** Gets a description of this dataset, with HTML markup. */
  public String getHTMLDescription() {
    StringBuffer sb = new StringBuffer();
    sb.append("OME server: ");
    sb.append(getServerName());
    sb.append("<br>\nImage ID: ");
    sb.append(imageId);
    sb.append("<br>\n");
    sb.append(super.getHTMLDescription());
    return sb.toString();
  }

  // -- Dynamic API methods --

  /** Tests whether two dynamic objects have matching states. */
  public boolean matches(Dynamic dyn) {
    if (!super.matches(dyn) || !isCompatible(dyn)) return false;
    OMEImage data = (OMEImage) dyn;

    return ObjectUtil.objectsEqual(server, data.server) &&
      ObjectUtil.objectsEqual(user, data.user) &&
      ObjectUtil.objectsEqual(password, data.password) &&
      imageId == data.imageId;
  }

  /**
   * Tests whether the given dynamic object can be used as an argument to
   * initState, for initializing this dynamic object.
   */
  public boolean isCompatible(Dynamic dyn) { return dyn instanceof OMEImage; }

  /** Modifies this object's state to match that of the given object. */
  public void initState(Dynamic dyn) {
    if (dyn != null && !isCompatible(dyn)) return;
    super.initState(dyn);
    OMEImage data = (OMEImage) dyn;

    if (data != null) {
      server = data.server;
      sessionKey = data.sessionKey;
      user = data.user;
      password = data.password;
      imageId = data.imageId;
    }

    if (downloader != null) {
      try {
        downloader.close();
      }
      catch (Exception e) { e.printStackTrace(); }
    }

    downloader = new BufferedImageReader(new OMEReader());
    if (task != null) {
      task.setStatus("Downloading image");
    }

    id = server;
    if (sessionKey != null && sessionKey.trim().length() != 0) {
      id += "?key=" + sessionKey + "&id=" + imageId;
    }
    else {
      id += "?user=" + user + "&password=" + password + "&id=" + imageId;
    }

    try {
      downloader.setId(id);
      sizeX = downloader.getSizeX();
      sizeY = downloader.getSizeY();
      sizeZ = downloader.getSizeZ();
      sizeC = downloader.getSizeC();
      sizeT = downloader.getSizeT();
      name = downloader.getCurrentFile();
    }
    catch (FormatException exc) { exc.printStackTrace(); }
    catch (IOException exc) { exc.printStackTrace(); }

    // populate lengths and dims arrays
    if (sizeZ == 1 && sizeT == 1) {
      lengths = new int[0];
      dims = new String[0];
      indexZ = indexT = -1;
    }
    else if (sizeZ == 1) {
      lengths = new int[] {sizeT};
      dims = new String[] {"Time"};
      indexZ = -1; indexT = 0;
    }
    else if (sizeT == 1) {
      lengths = new int[] {sizeZ};
      dims = new String[] {"Slice"};
      indexZ = 0; indexT = -1;
    }
    else {
      lengths = new int[] {sizeZ, sizeT};
      dims = new String[] {"Slice", "Time"};
      indexZ = 0; indexT = 1;
    }
    makeLabels();

    // construct thumbnail handler
    thumbs = new ThumbnailHandler(this, "cache.visbio"); // use global cache

    if (task != null) task.setCompleted();
  }

  /**
   * Called when this object is being discarded in favor of
   * another object with a matching state.
   */
  public void discard() {
    try {
      downloader.close();
    }
    catch (Exception exc) { exc.printStackTrace(); }
  }

  // -- Helper methods --

  private static long[] showTable(OMECredentials cred) throws ReflectException {
    long[] ids = OMEUtils.getAllIDs(cred.isOMERO);
    int[] x = OMEUtils.getAllWidths(cred.isOMERO);
    int[] y = OMEUtils.getAllHeights(cred.isOMERO);
    int[] z = OMEUtils.getAllZs(cred.isOMERO);
    int[] c = OMEUtils.getAllChannels(cred.isOMERO);
    int[] t = OMEUtils.getAllTs(cred.isOMERO);
    String[] types = OMEUtils.getAllTypes(cred.isOMERO);
    String[] names = OMEUtils.getAllNames(cred.isOMERO);
    String[] descr = OMEUtils.getAllDescriptions(cred.isOMERO);
    String[] created = OMEUtils.getAllDates(cred.isOMERO);
    BufferedImage[] thumbs = OMEUtils.getAllThumbnails(cred.isOMERO);

    Object[][] tableData = new Object[ids.length][11];
    for (int row=0; row<ids.length; row++) {
      tableData[row][0] = new Boolean(false);
      tableData[row][1] = new Long(ids[row]);
      tableData[row][2] = names[row];
      tableData[row][3] = descr[row];
      tableData[row][4] = new Integer(x[row]);
      tableData[row][5] = new Integer(y[row]);
      tableData[row][6] = new Integer(z[row]);
      tableData[row][7] = new Integer(c[row]);
      tableData[row][8] = new Integer(t[row]);
      tableData[row][9] = types[row];
      tableData[row][10] = created[row];
    }
    String[] columnNames = new String[] {"", "ID", "Name", "Description", "X",
      "Y", "Z", "C", "T", "Pixel type", "Creation date"};

    DefaultTableModel model = new DefaultTableModel(tableData, columnNames) {
      public boolean isCellEditable(int row, int col) {
        return col == 0;
      }

      public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
      }
    };
    JTable table = new JTable(model);
    table.setVisible(true);
    JScrollPane scroll = new JScrollPane(table);
    scroll.setVisible(true);
    DialogPane window = new DialogPane("OME Images");
    window.add(scroll);
    window.showDialog((Dialog) null);

    Vector idList = new Vector();
    for (int i=0; i<ids.length; i++) {
      boolean download = ((Boolean) table.getValueAt(i, 0)).booleanValue();
      if (download) idList.add(new Long(ids[i]));
    }

    long[] rtn = new long[idList.size()];
    for (int i=0; i<rtn.length; i++) {
      rtn[i] = ((Long) idList.get(i)).longValue();
    }
    return rtn;
  }

}
