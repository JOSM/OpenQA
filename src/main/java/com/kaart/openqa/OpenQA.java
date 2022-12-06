// License: GPL. For details, see LICENSE file.
package com.kaart.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Locale;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * The POJO entry point for the plugin
 *
 * @author Taylor Smock
 *
 */
public class OpenQA extends Plugin {
    /** The name of the plugin */
    public static final String NAME = "OpenQA";
    /** The preference prefix */
    public static final String PREF_PREFIX = NAME.toLowerCase(Locale.US).concat(".");
    /** The filetype config key */
    public static final String PREF_FILETYPE = PREF_PREFIX.concat("filetype");

    public static final String OPENQA_IMAGE = "openqa.svg";

    private class OpenQAAction extends JosmAction {
        private static final long serialVersionUID = 1L;

        OpenQAAction() {
            super(tr("{0} layer", OpenQA.NAME), OPENQA_IMAGE, tr("{0} layer", OpenQA.NAME),
                    Shortcut.registerShortcut("openqa:layer", tr("{0} layer", OpenQA.NAME), KeyEvent.CHAR_UNDEFINED,
                            Shortcut.NONE),
                    false, "openqa:layer", false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            OpenQALayerChangeListener.updateOpenQALayers();
        }
    }

    public OpenQA(PluginInformation info) {
        super(info);
        MainApplication.getLayerManager().addLayerChangeListener(new OpenQALayerChangeListener());
        if ("".equals(Config.getPref().get(PREF_FILETYPE))) {
            Config.getPref().put(PREF_FILETYPE, "geojson");
        }
        OpenQAAction openqaAction = new OpenQAAction();
        MainMenu.add(MainApplication.getMenu().dataMenu, openqaAction);
    }

    @Override
    public PreferenceSetting getPreferenceSetting() {
        return new OpenQAPreferences();
    }
}
