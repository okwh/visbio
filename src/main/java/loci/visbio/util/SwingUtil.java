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

package loci.visbio.util;

import com.jgoodies.looks.LookUtils;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Enumeration;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import loci.formats.ImageReader;
import loci.formats.gui.GUITools;

/**
 * SwingUtil contains useful Swing functions.
 */
public final class SwingUtil {

	// -- Constructor --

	private SwingUtil() {}

	// -- Utility methods --

	/** Constructs a JButton with an icon from the given file id. */
	public static JButton makeButton(final Object owner, final String id,
		final String altText, final int wpad, final int hpad)
	{
		final URL url = owner.getClass().getResource(id);
		ImageIcon icon = null;
		if (url != null) icon = new ImageIcon(url);
		JButton button;
		if (icon == null) button = new JButton(altText);
		else {
			button = new JButton(icon);
			button.setPreferredSize(new Dimension(icon.getIconWidth() + wpad, icon
				.getIconHeight() +
				hpad));
		}
		return button;
	}

	/** Constructs a JToggleButton with an icon from the given file id. */
	public static JToggleButton makeToggleButton(final Object owner,
		final String id, final String altText, final int wpad, final int hpad)
	{
		final URL url = owner.getClass().getResource(id);
		ImageIcon icon = null;
		if (url != null) icon = new ImageIcon(url);
		JToggleButton button;
		if (icon == null) button = new JToggleButton(altText);
		else {
			button = new JToggleButton(icon);
			button.setPreferredSize(new Dimension(icon.getIconWidth() + wpad, icon
				.getIconHeight() +
				hpad));
		}
		return button;
	}

	/** Fully expands the given JTree from the specified node. */
	public static void expandTree(final JTree tree,
		final DefaultMutableTreeNode node)
	{
		if (node.isLeaf()) return;
		tree.expandPath(new TreePath(node.getPath()));
		final Enumeration e = node.children();
		while (e.hasMoreElements()) {
			expandTree(tree, (DefaultMutableTreeNode) e.nextElement());
		}
	}

	/**
	 * Toggles the cursor for the given component and all child components between
	 * the wait cursor and the normal one.
	 */
	public static void setWaitCursor(final Component c, final boolean wait) {
		setCursor(c, wait ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
	}

	/**
	 * Sets the cursor for the given component and all child components to the
	 * given cursor.
	 */
	public static void setCursor(final Component c, final Cursor cursor) {
		if (c == null) return;
		c.setCursor(cursor);
		if (c instanceof JFrame) setCursor(((JFrame) c).getContentPane(), cursor);
		else if (c instanceof JDialog) {
			setCursor(((JDialog) c).getContentPane(), cursor);
		}
		else if (c instanceof Container) {
			final Container contain = (Container) c;
			final Component[] sub = contain.getComponents();
			for (int i = 0; i < sub.length; i++)
				setCursor(sub[i], cursor);
		}
	}

	/** Gets the containing window for the given component. */
	public static Window getWindow(Component c) {
		while (c != null) {
			if (c instanceof Window) return (Window) c;
			c = c.getParent();
		}
		return null;
	}

	/**
	 * Packs a window, with hacks to correct for inaccuracies in certain cases.
	 */
	public static void pack(final Window w) {
		w.pack();
		// HACK - work around a layout issue where panel is slightly too narrow and
		// short; this hack also appears in loci.visbio.view.DisplayWindow.repack()
		if (LookUtils.IS_OS_LINUX) {
			final Dimension size = w.getSize();
			size.width += 18;
			size.height += 10;
			w.setSize(size);
		}
	}

	/**
	 * Enlarges a window to its preferred width and/or height if it is too small.
	 */
	public static void repack(final Window w) {
		final Dimension size = getRepackSize(w);
		if (!w.getSize().equals(size)) w.setSize(size);
	}

	/** Gets the dimension of this window were it to be repacked. */
	public static Dimension getRepackSize(final Window w) {
		final Dimension size = w.getSize();
		final Dimension pref = w.getPreferredSize();
		if (size.width >= pref.width && size.height >= pref.height) return size;
		return new Dimension(size.width < pref.width ? pref.width : size.width,
			size.height < pref.height ? pref.height : size.height);
	}

	/** Centers the given window on the screen. */
	public static void centerWindow(final Window window) {
		final Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
		centerWindow(new Rectangle(0, 0, s.width, s.height), window);
	}

	/** Centers the given window within the specified parent window. */
	public static void centerWindow(final Window parent, final Window window) {
		centerWindow(parent.getBounds(), window);
	}

	/** Centers the given window within the specified bounds. */
	public static void centerWindow(final Rectangle bounds, final Window window) {
		final Dimension w = window.getSize();
		int x = bounds.x + (bounds.width - w.width) / 2;
		int y = bounds.y + (bounds.height - w.height) / 2;
		if (x < 0) x = 0;
		if (y < 0) y = 0;
		window.setLocation(x, y);
	}

	/** Sets the title of the given window. */
	public static void setWindowTitle(final Window w, final String title) {
		if (w instanceof Frame) ((Frame) w).setTitle(title);
		else if (w instanceof Dialog) ((Dialog) w).setTitle(title);
		else w.setName(title);
	}

	/** Gets the title of the given window. */
	public static String getWindowTitle(final Window w) {
		if (w instanceof Frame) return ((Frame) w).getTitle();
		else if (w instanceof Dialog) return ((Dialog) w).getTitle();
		else return w.getName();
	}

	/**
	 * Creates a copy of this menu bar, whose contents update automatically
	 * whenever the original menu bar changes.
	 */
	public static JMenuBar cloneMenuBar(final JMenuBar menubar) {
		if (menubar == null) return null;
		final JMenuBar jmb = new JMenuBar();
		final int count = menubar.getMenuCount();
		for (int i = 0; i < count; i++)
			jmb.add(cloneMenuItem(menubar.getMenu(i)));
		return jmb;
	}

	/**
	 * Creates a copy of this menu item, whose contents update automatically
	 * whenever the original menu item changes.
	 */
	public static JMenuItem cloneMenuItem(final JMenuItem item) {
		if (item == null) return null;
		JMenuItem jmi;
		if (item instanceof JMenu) {
			final JMenu menu = (JMenu) item;
			final JMenu jm = new JMenu();
			final int count = menu.getItemCount();
			for (int i = 0; i < count; i++) {
				final JMenuItem ijmi = cloneMenuItem(menu.getItem(i));
				if (ijmi == null) jm.addSeparator();
				else jm.add(ijmi);
			}
			jmi = jm;
		}
		else jmi = new JMenuItem();
		final ActionListener[] l = item.getActionListeners();
		for (int i = 0; i < l.length; i++)
			jmi.addActionListener(l[i]);
		jmi.setActionCommand(item.getActionCommand());
		syncMenuItem(item, jmi);
		linkMenuItem(item, jmi);
		return jmi;
	}

	/**
	 * Configures a scroll pane's properties to always show horizontal and
	 * vertical scroll bars. This method only exists to match the Macintosh Aqua
	 * Look and Feel as closely as possible.
	 */
	public static void configureScrollPane(final JScrollPane scroll) {
		if (!LAFUtil.isMacLookAndFeel()) return;
		scroll
			.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll
			.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
	}

	protected static JFileChooser chooser;

	/** Constructs a JFileChooser that recognizes accepted VisBio file types. */
	public static JFileChooser getVisBioFileChooser() {
		if (chooser == null) chooser = GUITools.buildFileChooser(new ImageReader());
		return chooser;
	}

	/** Pops up a message box, blocking the current thread. */
	public static void pause(final String msg) {
		JOptionPane.showMessageDialog(null, msg, "VisBio",
			JOptionPane.PLAIN_MESSAGE);
	}

	/** Pops up a message box, blocking the current thread. */
	public static void pause(final Throwable t) {
		final CharArrayWriter caw = new CharArrayWriter();
		t.printStackTrace(new PrintWriter(caw));
		pause(caw.toString());
	}

	// -- Helper methods --

	/**
	 * Forces slave menu item to reflect master menu item using a property change
	 * listener.
	 */
	protected static void linkMenuItem(final JMenuItem master,
		final JMenuItem slave)
	{
		final JMenuItem source = master, dest = slave;
		source.addPropertyChangeListener(new PropertyChangeListener() {

			@Override
			public void propertyChange(final PropertyChangeEvent e) {
				syncMenuItem(source, dest);
			}
		});
	}

	/** Brings the destination menu item into sync with the source item. */
	protected static void syncMenuItem(final JMenuItem source,
		final JMenuItem dest)
	{
		final boolean enabled = source.isEnabled();
		if (dest.isEnabled() != enabled) dest.setEnabled(enabled);
		final int mnemonic = source.getMnemonic();
		if (dest.getMnemonic() != mnemonic) dest.setMnemonic(mnemonic);
		final String text = source.getText();
		if (dest.getText() != text) dest.setText(text);
		final KeyStroke accel = source.getAccelerator();
		if (dest.getAccelerator() != accel) dest.setAccelerator(accel);
	}

}
