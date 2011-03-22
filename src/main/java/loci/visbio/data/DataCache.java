//
// DataCache.java
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

package loci.visbio.data;

import java.util.Hashtable;

import loci.formats.FormatTools;
import visad.Data;

/**
 * Provides a simple caching mechanism for full-resolution data in memory.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://dev.loci.wisc.edu/trac/software/browser/trunk/projects/visbio/src/main/java/loci/visbio/data/DataCache.java">Trac</a>,
 * <a href="http://dev.loci.wisc.edu/svn/software/trunk/projects/visbio/src/main/java/loci/visbio/data/DataCache.java">SVN</a></dd></dl>
 */
public class DataCache {

  // -- Constants --

  /** Debugging flag. */
  protected static final boolean DEBUG = false;

  // -- Fields --

  /** Hashtable backing this cache of full-resolution data. */
  protected Hashtable hash;

  // -- Constructor --

  /** Constructs a cache for managing full-resolution data in memory. */
  public DataCache() { hash = new Hashtable(); }

  // -- DataCache API methods --

  /** Gets the data object from the cache, computing it if the cache misses. */
  public synchronized Data getData(DataTransform trans,
    int[] pos, String append, int dim)
  {
    String key = getKey(trans, pos, append);
    Data d = getCachedData(key);
    if (d == null) { // do not compute for non-null append
      if (append == null || append.equals("")) {
        // compute automatically for null append string
        d = trans.getData(null, pos, dim, null);
        putCachedData(key, d);
      }
      if (DEBUG) System.out.println("DataCache: cache miss for " + key);
    }
    else if (DEBUG) System.out.println("DataCache: cache hit for " + key);
    return d;
  }

  /**
   * Puts the given data object into the cache for the specified transform
   * at the given dimensional position.
   */
  public synchronized void putData(DataTransform trans, int[] pos,
    String append, Data d)
  {
    putCachedData(getKey(trans, pos, append), d);
  }

  /**
   * Gets whether the cache has data for the given transform
   * at the specified dimensional position.
   */
  public synchronized boolean hasData(DataTransform trans, int[] pos,
    String append)
  {
    return getCachedData(getKey(trans, pos, append)) != null;
  }

  /**
   * Removes the data object at the specified
   * dimensional position from the cache.
   */
  public synchronized void dump(DataTransform trans, int[] pos,
    String append)
  {
    dump(getKey(trans, pos, append));
  }

  /**
   * Removes from the cache data objects at all dimensional positions
   * for the given data object.
   */
  public synchronized void dump(DataTransform trans, String append) {
    int[] lengths = trans.getLengths();
    int len = FormatTools.getRasterLength(lengths);
    for (int i=0; i<len; i++) {
      int[] pos = FormatTools.rasterToPosition(lengths, i);
      dump(getKey(trans, pos, append));
    }
  }

  /** Removes everything from the cache. */
  public synchronized void dumpAll() { hash.clear(); }

  // -- Internal DataCache API methods --

  /** Gets the data in the cache at the specified key. */
  protected Data getCachedData(String key) {
    if (key == null) return null;
    Object o = hash.get(key);
    if (!(o instanceof Data)) return null;
    return (Data) o;
  }

  /** Sets the data in the cache at the specified key. */
  protected void putCachedData(String key, Data d) {
    if (key != null && d != null) hash.put(key, d);
  }

  /** Removes the data object at the specified key from the cache. */
  protected void dump(String key) {
    if (key != null) {
      hash.remove(key);
      if (DEBUG) System.out.println("DataCache: dumped " + key);
    }
  }

  // -- Helper methods --

  /**
   * Gets a key string suitable for hashing for the given transform at the
   * specified position. Changing the append string allows storage of multiple
   * data objects at the same dimensional position for the same transform.
   */
  protected String getKey(DataTransform trans, int[] pos, String append) {
    if (append == null) append = "";
    String id = pos == null ? null : trans.getCacheId(pos, true);
    return id + append;
  }

}
