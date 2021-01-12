// License: GPL. For details, see LICENSE file.
package com.kaart.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

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

    private class OpenQAAction extends JosmAction {
        private static final long serialVersionUID = 1L;

        OpenQAAction() {
            super(OpenQA.NAME.concat(tr(" layer")), OPENQA_IMAGE, tr("OpenQA Layer"), Shortcut
                    .registerShortcut("openqa:layer", tr("OpenQA Layer"), KeyEvent.CHAR_UNDEFINED, Shortcut.NONE),
                    false, "openqa:layer", false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            OpenQALayerChangeListener.updateOpenQALayers(cacheDir);
        }
    }

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
        OpenQAAction openqaAction = new OpenQAAction();
        MainMenu.add(MainApplication.getMenu().dataMenu, openqaAction);
    }

    @Override
    public PreferenceSetting getPreferenceSetting() {
        return new OpenQAPreferences(cacheDir);
    }

    public static String getVersion() {
        // TODO get the version dynamically
        return "v0.1.4";
    }
}
