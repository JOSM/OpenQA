// License: GPL. For details, see LICENSE file.
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
    public String cacheDir;
    public static final String PREF_PREFIX = NAME.toLowerCase().concat(".");
    public static final String PREF_FILETYPE = PREF_PREFIX.concat("filetype");

    public static final String OPENQA_IMAGE = "openqa.svg";

    public OpenQA(PluginInformation info) {
        super(info);
        try {
            cacheDir = getPluginDirs().getCacheDirectory(true).getCanonicalPath();
        } catch (IOException e) {
            cacheDir = "openqa";
            Logging.debug(e.getMessage());
        }
        MainApplication.getLayerManager().addLayerChangeListener(new OpenQALayerChangeListener(cacheDir));
        if (Config.getPref().get(PREF_FILETYPE).equals("")) {
            Config.getPref().put(PREF_FILETYPE, "geojson");
        }
        AbstractAction openqaAction = new AbstractAction(NAME.concat(tr(" layer")),
                ImageProvider.get(OPENQA_IMAGE, ImageProvider.ImageSizes.MENU)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                OpenQALayerChangeListener.updateOpenQALayers(cacheDir);
            }
        };
        MainApplication.getMenu().dataMenu.add(openqaAction);
    }

    @Override
    public PreferenceSetting getPreferenceSetting() {
        OpenQAPreferences openQA = new OpenQAPreferences(cacheDir);
        return openQA;
    }

    public static String getVersion() {
        // TODO get the version dynamically
        return "v0.1.4";
    }
}
