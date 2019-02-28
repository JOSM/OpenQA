package com.kaartgroup.keepright;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 *
 * @author Taylor Smock
 *
 */
public class KeepRight extends Plugin {
	public static final String KEEP_RIGHT_LAYER_NAME = "Keep Right Errors";
	public KeepRight(PluginInformation info) {
		super(info);
		MainApplication.getLayerManager().addLayerChangeListener(new KeepRightLayerChangeListener());
		if (Config.getPref().get(KeepRightPreferences.PREF_FILETYPE) == "") {
			Config.getPref().put(KeepRightPreferences.PREF_FILETYPE, "geojson");
		}
		KeepRightLayerChangeListener.updateKeepRightLayer();
	}

	@Override
	public PreferenceSetting getPreferenceSetting() {
		return new KeepRightPreferences();
	}
}
