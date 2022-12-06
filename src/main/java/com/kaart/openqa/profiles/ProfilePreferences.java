// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles;

import javax.swing.JCheckBox;

import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;

/**
 * The base class for the different error source preferences
 */
public abstract class ProfilePreferences extends DefaultTabPreferenceSetting implements SubPreferenceSetting {

    protected final HashMap<String, List<JCheckBox>> checkBoxes = new HashMap<>();

    protected ProfilePreferences(String image, String title, String description) {
        super(image, title, description);
    }

    /**
     * Create the sub tab to add to the main OpenQA preference panel
     *
     * @return The sub tab to add
     */
    public abstract Component createSubTab();

    public Map<String, List<JCheckBox>> getCheckBoxes() {
        return checkBoxes;
    }

}
