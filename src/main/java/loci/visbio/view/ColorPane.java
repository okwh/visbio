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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.rmi.RemoteException;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import loci.formats.gui.ExtensionFileFilter;
import loci.visbio.util.BioComboBox;
import loci.visbio.util.ColorUtil;
import loci.visbio.util.DialogPane;
import loci.visbio.util.DisplayUtil;
import loci.visbio.util.FormsUtil;
import loci.visbio.util.LAFUtil;
import visad.BaseColorControl;
import visad.DataReferenceImpl;
import visad.DataRenderer;
import visad.Display;
import visad.DisplayImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.GriddedSet;
import visad.ProjectionControl;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarMap;
import visad.VisADException;
import visad.browser.Convert;
import visad.java2d.DisplayImplJ2D;
import visad.util.ColorMapWidget;

/**
 * ColorPane is a dialog pane for adjusting color settings.
 */
public class ColorPane extends DialogPane implements ChangeListener,
	DocumentListener, ItemListener
{

	// -- Constants --

	/** Names for preset color look-up table. */
	public static final String[] LUT_NAMES = { "Grayscale", "HSV", "RGB", null,
		"Red", "Green", "Blue", null, "Cyan", "Magenta", "Yellow", null, "Fire",
		"Ice" };

	/** Preset color look-up tables. */
	public static final float[][][] LUTS = { ColorUtil.LUT_GRAY,
		ColorUtil.LUT_HSV, ColorUtil.LUT_RGB, null, ColorUtil.LUT_RED,
		ColorUtil.LUT_GREEN, ColorUtil.LUT_BLUE, null, ColorUtil.LUT_CYAN,
		ColorUtil.LUT_MAGENTA, ColorUtil.LUT_YELLOW, null, ColorUtil.LUT_FIRE,
		ColorUtil.LUT_ICE };

	// -- GUI components --

	/** LUT file chooser. */
	protected JFileChooser fileBox;

	/** Color preview display. */
	protected DisplayImpl preview;

	/** GUI panel for color table widget. */
	protected JPanel widgetPane;

	/** Slider for level of brightness. */
	protected JSlider brightness;

	/** Label for current brightness value. */
	protected JLabel brightnessValue;

	/** Slider for level of contrast. */
	protected JSlider contrast;

	/** Label for current contrast value. */
	protected JLabel contrastValue;

	/** Slider for level of opacity. */
	protected JSlider opacity;

	/** Label for current opacity value. */
	protected JLabel opacityValue;

	/** Opacity model options. */
	protected JRadioButton constant, curved;

	/** Color model options. */
	protected JRadioButton rgb, hsv, composite;

	/** Color map widgets. */
	protected BioColorWidget red, green, blue;

	/** Combo box for choosing color widgets. */
	protected BioComboBox selector;

	/** Option for fixed color scaling. */
	protected JCheckBox fixed;

	/** Text field for low color scale value. */
	protected JTextField loVal;

	/** Label for fixed color scale. */
	protected JLabel toLabel;

	/** Text field for high color scale value. */
	protected JTextField hiVal;

	/** Button for loading color look-up table. */
	protected JButton lutLoad;

	/** Button for saving color look-up table. */
	protected JButton lutSave;

	/** Popup menu for selecting a preset color look-up table. */
	protected JPopupMenu lutsMenu;

	/** Button for selecting a color look-up table from a list of presets. */
	protected JButton lutPresets;

	// -- Other fields --

	/** Color handler for this color pane. */
	protected ColorHandler handler;

	/** List of mappings from range components to RGB. */
	protected ScalarMap[] maps;

	/** Should changes to the color components be ignored? */
	protected boolean ignore = false;

	// -- Constructor --

	/** Constructs a dialog for adjusting color parameters. */
	public ColorPane(final ColorHandler h) {
		super("Edit colors - " + h.getWindow().getName());
		handler = h;

		// LUT file chooser
		fileBox = new JFileChooser(System.getProperty("user.dir"));
		fileBox.addChoosableFileFilter(new ExtensionFileFilter("lut",
			"Binary color table files"));

		// preview display
		preview = DisplayUtil.makeDisplay("bio_color_preview", false);

		// lay out left-hand panel
		PanelBuilder builder =
			new PanelBuilder(new FormLayout("pref:grow",
				"pref:grow, 3dlu, pref, 3dlu, " + "pref, 3dlu, pref"));
		final CellConstraints cc = new CellConstraints();
		builder.add(preview.getComponent(), cc.xy(1, 1, "fill, fill"));
		builder.add(makeSliderPanel(), cc.xy(1, 3, "fill, center"));
		builder.add(makeModelPanel(), cc.xy(1, 5, "center, center"));
		builder.add(makeMappingsPanel(), cc.xy(1, 7, "center, center"));
		final JPanel left = builder.getPanel();

		// lay out right-hand panel
		final JPanel right = makeTablesPanel();

		// lay out main panel
		builder =
			new PanelBuilder(new FormLayout("pref:grow, 9dlu, pref:grow",
				"fill:pref:grow"));
		builder.add(left, cc.xy(1, 1));
		builder.add(right, cc.xy(3, 1));
		add(builder.getPanel());
	}

	// -- ColorPane API methods - mutators --

	/** Sets the data displayed in the preview display to that given. */
	public void setPreviewData(final FlatField ff) throws VisADException,
		RemoteException
	{
		DisplayUtil.setDisplayDisabled(preview, true);

		// clear old preview data
		preview.removeAllReferences();
		preview.clearMaps();
		widgetPane.removeAll();
		selector.removeActionListener(this);
		selector.removeAllItems();

		if (ff != null) {
			// guess at some default color mappings
			final FunctionType ftype = (FunctionType) ff.getType();
			final RealTupleType domain = ftype.getDomain();
			final RealType[] xy = domain.getRealComponents();
			if (xy.length != 2) throw new VisADException("Invalid preview data");
			final RealType rtX = xy[0];
			final RealType rtY = xy[1];
			guessTypes();

			// create data reference
			final DataReferenceImpl ref = new DataReferenceImpl("color_preview_ref");
			ref.setData(ff);

			// add scalar maps and data reference
			preview.addMap(new ScalarMap(rtX, Display.XAxis));
			preview.addMap(new ScalarMap(rtY, Display.YAxis));

			final ScalarMap[] sm = handler.getMaps();
			maps = new ScalarMap[sm.length];
			for (int i = 0; i < maps.length; i++) {
				maps[i] = (ScalarMap) sm[i].clone();
				preview.addMap(maps[i]);
				widgetPane.add(new ColorMapWidget(maps[i]));
				selector.addItem("#" + (i + 1));
			}
			DataRenderer dr = null;
			if (sm.length == 1) {
				// use ImageRendererJ3D when possible
				try {
					final Class<?> c = Class.forName("visad.bom.ImageRendererJ3D");
					dr = (DataRenderer) c.newInstance();
				}
				catch (final NoClassDefFoundError err) {}
				catch (final ClassNotFoundException exc) {
					exc.printStackTrace();
				}
				catch (final IllegalAccessException exc) {
					exc.printStackTrace();
				}
				catch (final InstantiationException exc) {
					exc.printStackTrace();
				}
				catch (final RuntimeException exc) {
					// HACK: workaround for bug in Apache Axis2
					final String msg = exc.getMessage();
					if (msg != null && msg.indexOf("ClassNotFound") < 0) throw exc;
					exc.printStackTrace();
				}
			}
			if (dr == null) {
				// ImageRendererJ3D does not support multiple Display.RGBA mappings
				preview.addReference(ref);
			}
			else preview.addReferences(dr, ref);

			// set aspect ratio
			final GriddedSet set = (GriddedSet) ff.getDomainSet();
			final float[] lo = set.getLow();
			final float[] hi = set.getHi();
			final double x = hi[0] - lo[0];
			final double y = hi[1] - lo[1];
			final double d = x > y ? x : y;
			final double xasp = x / d;
			final double yasp = y / d;
			final ProjectionControl pc = preview.getProjectionControl();
			if (preview instanceof DisplayImplJ2D) {
				pc.setAspectCartesian(new double[] { xasp, yasp });
			}
			else pc.setAspectCartesian(new double[] { xasp, yasp, 1.0 });

			// set zoom
			final double[] zoom =
				preview.make_matrix(0, 0, 0, ViewHandler.DEFAULT_ZOOM_2D, 0, 0, 0);
			pc.setMatrix(zoom);
		}
		selector.addActionListener(this);

		DisplayUtil.setDisplayDisabled(preview, false);
	}

	/** Guesses reasonable mappings from range RealTypes to color components. */
	public void guessTypes() {
		red.removeItemListener(this);
		green.removeItemListener(this);
		blue.removeItemListener(this);

		final ScalarMap[] sm = handler.getMaps();
		final RealType[] rtypes = new RealType[sm.length];
		for (int i = 0; i < sm.length; i++)
			rtypes[i] = (RealType) sm[i].getScalar();
		red.guessType(rtypes);
		green.guessType(rtypes);
		blue.guessType(rtypes);

		red.addItemListener(this);
		green.addItemListener(this);
		blue.addItemListener(this);
	}

	/** Sets the currently selected range component's color widget table. */
	public void setWidgetTable(float[][] table) {
		final ColorMapWidget cmw =
			(ColorMapWidget) widgetPane.getComponent(selector.getSelectedIndex());
		final float[][] oldTable = cmw.getTableView();
		final float[] alpha = oldTable.length > 3 ? oldTable[3] : null;
		table = ColorUtil.adjustColorTable(table, alpha, true);
		cmw.setTableView(table);
	}

	// -- ColorPane API methods - accessors --

	/** Gets current brightness value. */
	public int getBrightness() {
		return brightness.getValue();
	}

	/** Gets current contrast value. */
	public int getContrast() {
		return contrast.getValue();
	}

	/** Gets current opacity value. */
	public int getOpacityValue() {
		return opacity.getValue();
	}

	/** Gets current opacity model. */
	public int getOpacityModel() {
		return curved.isSelected() ? ColorUtil.CURVED_ALPHA
			: ColorUtil.CONSTANT_ALPHA;
	}

	/** Gets current color model. */
	public int getColorMode() {
		return rgb.isSelected() ? ColorUtil.RGB_MODEL : (hsv.isSelected()
			? ColorUtil.HSV_MODEL : ColorUtil.COMPOSITE_MODEL);
	}

	/** Gets current red RealType. */
	public RealType getRed() {
		return red.getSelectedItem();
	}

	/** Gets current green RealType. */
	public RealType getGreen() {
		return green.getSelectedItem();
	}

	/** Gets current blue RealType. */
	public RealType getBlue() {
		return blue.getSelectedItem();
	}

	/** Gets color table range minimums. */
	public double[] getLo() {
		final double[] lo = new double[maps.length];
		for (int i = 0; i < maps.length; i++)
			lo[i] = maps[i].getRange()[0];
		return lo;
	}

	/** Gets color table range maximums. */
	public double[] getHi() {
		final double[] hi = new double[maps.length];
		for (int i = 0; i < maps.length; i++)
			hi[i] = maps[i].getRange()[1];
		return hi;
	}

	/** Gets whether each color table has a fixed color range. */
	public boolean[] getFixed() {
		final boolean[] fix = new boolean[maps.length];
		final int ndx = selector.getSelectedIndex();
		for (int i = 0; i < maps.length; i++)
			fix[i] = !maps[i].isAutoScale();
		return fix;
	}

	/** Gets color table values. */
	public float[][][] getTables() {
		final float[][][] tables = new float[maps.length][][];
		for (int i = 0; i < maps.length; i++) {
			final ColorMapWidget cmw = (ColorMapWidget) widgetPane.getComponent(i);
			tables[i] = cmw.getTableView();
		}
		return tables;
	}

	// -- DialogPane API methods --

	/** Resets the color pane's components to their default states. */
	@Override
	public void resetComponents() {
		final int bright = handler.getBrightness();
		final int cont = handler.getContrast();
		final int opac = handler.getOpacityValue();
		final int om = handler.getOpacityModel();
		final int cm = handler.getColorModel();
		final RealType rType = handler.getRed();
		final RealType gType = handler.getGreen();
		final RealType bType = handler.getBlue();
		final double[] lo = handler.getLo();
		final double[] hi = handler.getHi();
		final boolean[] fix = handler.getFixed();

		ignore = true;
		brightness.setValue(bright);
		brightnessValue.setText("" + bright);
		contrast.setValue(cont);
		contrastValue.setText("" + cont);
		opacity.setValue(opac);
		opacityValue.setText("" + opac);
		if (om == ColorUtil.CONSTANT_ALPHA) constant.setSelected(true);
		else if (om == ColorUtil.CURVED_ALPHA) curved.setSelected(true);
		if (cm == ColorUtil.RGB_MODEL) rgb.setSelected(true);
		else if (cm == ColorUtil.HSV_MODEL) hsv.setSelected(true);
		else if (cm == ColorUtil.COMPOSITE_MODEL) composite.setSelected(true);
		red.setSelectedItem(rType);
		green.setSelectedItem(gType);
		blue.setSelectedItem(bType);
		final float[][][] tables = handler.getTables();
		if (maps != null) {
			DisplayUtil.setDisplayDisabled(preview, true);
			ColorUtil.setColorMode(preview, cm);
			for (int i = 0; i < maps.length; i++) {
				if (fix[i]) {
					try {
						maps[i].setRange(lo[i], hi[i]);
					}
					catch (final VisADException exc) {
						exc.printStackTrace();
					}
					catch (final RemoteException exc) {
						exc.printStackTrace();
					}
				}
				else maps[i].resetAutoScale();
				final BaseColorControl cc = (BaseColorControl) maps[i].getControl();
				if (cc != null) {
					try {
						cc.setTable(tables[i]);
					}
					catch (final VisADException exc) {
						exc.printStackTrace();
					}
					catch (final RemoteException exc) {
						exc.printStackTrace();
					}
				}
			}
			DisplayUtil.setDisplayDisabled(preview, false);
			if (selector.getItemCount() > 0) selector.setSelectedIndex(0);
		}
		ignore = false;
	}

	// -- ActionListener API methods --

	/** Handles GUI events. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final Object o = e.getSource();
		if (o == constant || o == curved) doAlphas();
		else if (o == rgb || o == hsv || o == composite) {
			final int colorModel =
				o == rgb ? ColorUtil.RGB_MODEL : o == hsv ? ColorUtil.HSV_MODEL
					: ColorUtil.COMPOSITE_MODEL;
			red.setModel(colorModel);
			green.setModel(colorModel);
			blue.setModel(colorModel);
			guessTypes();
			doColorTables();
		}
		else if (o == selector) {
			final int ndx = selector.getSelectedIndex();
			for (int i = 0; i < widgetPane.getComponentCount(); i++) {
				widgetPane.getComponent(i).setVisible(i == ndx);
			}
			if (ndx >= 0) {
				ignore = true;
				fixed.setSelected(!maps[ndx].isAutoScale());
				final double[] range = maps[ndx].getRange();
				loVal.setText("" + Convert.shortString(range[0]));
				hiVal.setText("" + Convert.shortString(range[1]));
				ignore = false;
			}
		}
		else if (o == lutLoad) {
			// ask user to specify the file
			final int returnVal = fileBox.showOpenDialog(dialog);
			if (returnVal != JFileChooser.APPROVE_OPTION) return;
			final File file = fileBox.getSelectedFile();

			final float[][] table = ColorUtil.loadColorTable(file);
			if (table == null) {
				JOptionPane.showMessageDialog(dialog, "Error reading LUT file.",
					"Cannot load color table", JOptionPane.ERROR_MESSAGE);
			}
			else setWidgetTable(table);
		}
		else if (o == lutSave) {
			// ask user to specify the file
			final int returnVal = fileBox.showSaveDialog(dialog);
			if (returnVal != JFileChooser.APPROVE_OPTION) return;
			File file = fileBox.getSelectedFile();
			final String s = file.getAbsolutePath();
			if (!s.toLowerCase().endsWith(".lut")) file = new File(s + ".lut");

			final ColorMapWidget cmw =
				(ColorMapWidget) widgetPane.getComponent(selector.getSelectedIndex());
			final boolean success =
				ColorUtil.saveColorTable(cmw.getTableView(), file);
			if (!success) {
				JOptionPane.showMessageDialog(dialog, "Error writing LUT file.",
					"Cannot save color table", JOptionPane.ERROR_MESSAGE);
			}
		}
		else if (o == lutPresets) {
			lutsMenu.show(lutPresets, lutPresets.getWidth(), 0);
		}
		else {
			// apply the chosen LUT preset
			final String cmd = e.getActionCommand();
			if (cmd != null && cmd.startsWith("lut")) {
				setWidgetTable(LUTS[Integer.parseInt(cmd.substring(3))]);
			}
		}
		super.actionPerformed(e);
	}

	// -- ChangeListener API methods --

	/** Handles slider changes. */
	@Override
	public void stateChanged(final ChangeEvent e) {
		if (e.getSource() == opacity) doAlphas();
		else doColorTables();
	}

	// -- DocumentListener API methods --

	/** Handles text field changes. */
	@Override
	public void changedUpdate(final DocumentEvent e) {
		doColorRanges();
	}

	/** Handles text field additions. */
	@Override
	public void insertUpdate(final DocumentEvent e) {
		doColorRanges();
	}

	/** Handles text field deletions. */
	@Override
	public void removeUpdate(final DocumentEvent e) {
		doColorRanges();
	}

	// -- ItemListener API methods --

	/** ItemListener method for handling color mapping changes. */
	@Override
	public void itemStateChanged(final ItemEvent e) {
		if (e.getSource() == fixed) {
			final boolean b = fixed.isSelected();
			loVal.setEnabled(b);
			toLabel.setEnabled(b);
			hiVal.setEnabled(b);
			doColorRanges();
		}
		else doColorTables();
	}

	// -- Helper methods --

	/** Constructs panel with brightness, contrast and transparency sliders. */
	protected JPanel makeSliderPanel() {
		// brightness slider
		brightness = new JSlider(0, ColorUtil.COLOR_DETAIL, 0);
		brightness.addChangeListener(this);
		brightness.setAlignmentY(Component.TOP_ALIGNMENT);
		brightness.setToolTipText("Adjusts the brightness of the data");

		// current brightness value
		brightnessValue = new JLabel("999");
		brightnessValue.setToolTipText("Current brightness value");

		// contrast slider
		contrast = new JSlider(0, ColorUtil.COLOR_DETAIL, 0);
		contrast.addChangeListener(this);
		contrast.setAlignmentY(Component.TOP_ALIGNMENT);
		contrast.setToolTipText("Adjusts the contrast of the data");

		// current contrast value
		contrastValue = new JLabel("999");
		contrastValue.setToolTipText("Current contrast value");

		// transparency slider
		opacity = new JSlider(0, ColorUtil.COLOR_DETAIL, ColorUtil.COLOR_DETAIL);
		opacity.addChangeListener(this);
		opacity.setAlignmentY(Component.TOP_ALIGNMENT);
		opacity.setMajorTickSpacing(ColorUtil.COLOR_DETAIL / 4);
		opacity.setMinorTickSpacing(ColorUtil.COLOR_DETAIL / 16);
		opacity.setPaintTicks(true);
		opacity.setToolTipText("Adjusts the transparency of the data.");

		// current transparency value
		opacityValue = new JLabel("999");
		opacityValue.setToolTipText("Current transparency value");

		// constant transparency option
		constant = new JRadioButton("Constant", true);
		constant.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) constant.setMnemonic('a');
		constant.setToolTipText("Switches to a constant value transparency");

		// curved transparency option
		curved = new JRadioButton("Curved");
		curved.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) curved.setMnemonic('d');
		curved.setToolTipText("Switches to a curved function transparency");

		// transparency model button group
		final ButtonGroup group = new ButtonGroup();
		group.add(constant);
		group.add(curved);

		// lay out components
		final PanelBuilder builder =
			new PanelBuilder(new FormLayout("pref, 3dlu, pref:grow, 3dlu, pref",
				"pref, 3dlu, pref, 3dlu, top:pref, 3dlu, pref"));
		final CellConstraints cc = new CellConstraints();

		builder.addLabel("&Brightness", cc.xy(1, 1)).setLabelFor(brightness);
		builder.add(brightness, cc.xy(3, 1));
		builder.add(brightnessValue, cc.xy(5, 1));

		builder.addLabel("Contra&st", cc.xy(1, 3)).setLabelFor(contrast);
		builder.add(contrast, cc.xy(3, 3));
		builder.add(contrastValue, cc.xy(5, 3));

		builder.addLabel("O&pacity", cc.xy(1, 5)).setLabelFor(opacity);
		builder.add(opacity, cc.xy(3, 5));
		builder.add(opacityValue, cc.xy(5, 5));

		builder.add(FormsUtil.makeRow(new Object[] { "Transparency model",
			constant, curved }), cc.xyw(1, 7, 5, "center, default"));

		return builder.getPanel();
	}

	/** Constructs panel containing color model radio buttons. */
	protected JPanel makeModelPanel() {
		// RGB color model option
		rgb = new JRadioButton("RGB", true);
		rgb.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) rgb.setMnemonic('r');
		rgb.setToolTipText("Switches to a Red-Green-Blue color model");

		// HSV color model option
		hsv = new JRadioButton("HSV");
		hsv.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) hsv.setMnemonic('h');
		hsv.setToolTipText("Switches to a Hue-Saturation-Value color model");

		// composite color model option
		composite = new JRadioButton("Composite");
		composite.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) composite.setMnemonic('i');
		composite
			.setToolTipText("Combines range values into a composite RGB color table");

		// color model button group
		final ButtonGroup group = new ButtonGroup();
		group.add(rgb);
		group.add(hsv);
		group.add(composite);

		return FormsUtil
			.makeRow(new Object[] { "Color model", rgb, hsv, composite });
	}

	/** Constructs panel containing color mapping combo boxes. */
	protected JPanel makeMappingsPanel() {
		// red/hue color map widget
		red = new BioColorWidget(0);
		red.addItemListener(this);
		if (!LAFUtil.isMacLookAndFeel()) red.setMnemonic('e');
		red.setToolTipText("Range mapping to Red/Hue color component");

		// green/saturation color map widget
		green = new BioColorWidget(1);
		green.addItemListener(this);
		if (!LAFUtil.isMacLookAndFeel()) green.setMnemonic('n');
		green.setToolTipText("Range mapping to Green/Saturation color component");

		// blue/value color map widget
		blue = new BioColorWidget(2);
		blue.addItemListener(this);
		if (!LAFUtil.isMacLookAndFeel()) blue.setMnemonic('u');
		blue.setToolTipText("Range mapping to Blue/Value color component");

		// lay out components
		return FormsUtil.makeRow(red, green, blue);
	}

	/** Constructs panel containing color table components. */
	protected JPanel makeTablesPanel() {
		// color widget selector
		// BaseRGBMap.USE_COLOR_CURSORS = true;
		selector = new BioComboBox();
		selector.addActionListener(this);
		selector.setToolTipText("List of color tables for color components");

		// fixed color scaling option
		fixed = new JCheckBox("Fixed color range");
		fixed.addItemListener(this);
		if (!LAFUtil.isMacLookAndFeel()) fixed.setMnemonic('f');
		fixed.setToolTipText("Fixes color range between the given values");

		// low color scale value text field
		loVal = new JTextField("-", 6);
		loVal.getDocument().addDocumentListener(this);
		loVal.setToolTipText("Minimum color range value");

		// color scale label
		toLabel = new JLabel("to");

		// high color scale value text field
		hiVal = new JTextField("-", 6);
		hiVal.getDocument().addDocumentListener(this);
		hiVal.setToolTipText("Maximum color range value");

		// color widget pane
		widgetPane = new JPanel();
		widgetPane.setLayout(new BoxLayout(widgetPane, BoxLayout.X_AXIS));

		// load LUT button
		lutLoad = new JButton("Load LUT...");
		lutLoad.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) lutLoad.setMnemonic('l');
		lutLoad.setToolTipText("Loads a color table from disk");

		// save LUT button
		lutSave = new JButton("Save LUT...");
		lutSave.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) lutSave.setMnemonic('s');
		lutSave.setToolTipText("Saves this color table to disk");

		// LUTs popup menu
		lutsMenu = new JPopupMenu();
		for (int i = 0; i < LUT_NAMES.length; i++) {
			if (LUT_NAMES[i] == null) lutsMenu.addSeparator();
			else {
				final JMenuItem item = new JMenuItem(LUT_NAMES[i]);
				item.setActionCommand("lut" + i);
				item.addActionListener(this);
				lutsMenu.add(item);
			}
		}

		// LUTs button
		lutPresets = new JButton("LUTs >");
		lutPresets.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) lutPresets.setMnemonic('t');
		lutPresets.setToolTipText("Selects a LUT from the list of presets");

		// lay out components
		final PanelBuilder builder =
			new PanelBuilder(new FormLayout("center:pref:grow",
				"pref, 3dlu, pref, 3dlu, pref:grow, 3dlu, pref"));
		builder.setDefaultDialogBorder();
		final CellConstraints cc = new CellConstraints();
		builder.add(FormsUtil.makeRow("Ran&ge component", selector), cc.xy(1, 1));
		builder.add(FormsUtil
			.makeRow(new Object[] { fixed, loVal, toLabel, hiVal }), cc.xy(1, 3));
		builder.add(widgetPane, cc.xy(1, 5, "fill, fill"));
		builder.add(
			ButtonBarFactory.buildCenteredBar(lutLoad, lutSave, lutPresets), cc.xy(1,
				7));
		final JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.setBorder(new TitledBorder("Color tables"));
		p.add(builder.getPanel());
		return p;
	}

	/** Updates image color tables, when settings are adjusted. */
	protected void doColorTables() {
		if (ignore) return;

		final int bright = getBrightness();
		final int cont = getContrast();
		final int cm = getColorMode();
		final RealType rType = getRed();
		final RealType gType = getGreen();
		final RealType bType = getBlue();

		brightnessValue.setText("" + bright);
		contrastValue.setText("" + cont);

		if (maps != null) {
			DisplayUtil.setDisplayDisabled(preview, true);
			final float[][][] tables =
				ColorUtil.computeColorTables(maps, bright, cont, cm, rType, gType,
					bType);
			ColorUtil.setColorMode(preview, cm);
			ColorUtil.setColorTables(maps, tables);
			DisplayUtil.setDisplayDisabled(preview, false);
		}
	}

	/** Updates image transparencies, when settings are adjusted. */
	protected void doAlphas() {
		if (ignore) return;

		final int opac = getOpacityValue();
		final int om = getOpacityModel();

		opacityValue.setText("" + opac);

		if (maps != null) {
			DisplayUtil.setDisplayDisabled(preview, true);
			final float[] alpha = ColorUtil.computeAlphaTable(opac, om);
			ColorUtil.setAlphaTable(maps, alpha);
			DisplayUtil.setDisplayDisabled(preview, false);
		}
	}

	/** Updates image color ranges, when settings are adjusted. */
	protected void doColorRanges() {
		if (ignore) return;

		DisplayUtil.setDisplayDisabled(preview, true);

		final int ndx = selector.getSelectedIndex();
		if (ndx < 0) return;

		double lo;
		try {
			lo = Double.parseDouble(loVal.getText());
		}
		catch (final NumberFormatException exc) {
			lo = maps[ndx].getRange()[0];
		}
		double hi;
		try {
			hi = Double.parseDouble(hiVal.getText());
		}
		catch (final NumberFormatException exc) {
			hi = maps[ndx].getRange()[1];
		}
		final boolean fix = fixed.isSelected();
		ColorUtil.setColorRange(preview, maps[ndx], lo, hi, fix);

		DisplayUtil.setDisplayDisabled(preview, false);
	}

}
