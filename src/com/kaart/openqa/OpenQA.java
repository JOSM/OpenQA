package com.kaart.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 *
 * @author Taylor Smock
 *
 */
public class OpenQA extends Plugin {
	public static final String NAME = "OpenQA";
	public String CACHE_DIR;
	public static String PREF_PREFIX = NAME.toLowerCase().concat(".");
	public static String PREF_FILETYPE = PREF_PREFIX.concat("filetype");

	public static String OPENQA_IMAGE = "keepright.png";

	public OpenQA(PluginInformation info) {
		super(info);
		try {
			CACHE_DIR = getPluginDirs().getCacheDirectory(true).getCanonicalPath();
		} catch (IOException e) {
			CACHE_DIR = "openqa";
			Logging.debug(e.getMessage());
		}
		MainApplication.getLayerManager().addLayerChangeListener(new OpenQALayerChangeListener(CACHE_DIR));
		if (Config.getPref().get(PREF_FILETYPE) == "") {
			Config.getPref().put(PREF_FILETYPE, "geojson");
		}
		OpenQALayerChangeListener.updateOpenQALayers(CACHE_DIR);
		AbstractAction openqaAction = new AbstractAction(NAME.concat(tr(" layer")),
				ImageProvider.get(OPENQA_IMAGE, ImageProvider.ImageSizes.MENU)) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				OpenQALayerChangeListener.updateOpenQALayers(CACHE_DIR);
			}
		};
		MainApplication.getMenu().dataMenu.add(openqaAction);
	}

	@Override
	public PreferenceSetting getPreferenceSetting() {
		OpenQAPreferences openQA = new OpenQAPreferences(CACHE_DIR);
		return openQA;
	}
}
