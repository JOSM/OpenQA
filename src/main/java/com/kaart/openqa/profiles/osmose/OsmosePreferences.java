// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles.osmose;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

import com.kaart.openqa.OpenQA;
import com.kaart.openqa.profiles.ProfilePreferences;

/**
 * @author Taylor Smock
 *
 */
public class OsmosePreferences extends ProfilePreferences {
    JPanel testPanel;

    final String cacheDir;
    static final String PREF_TESTS = "openqa.osmose-tests";

    public OsmosePreferences(String directory) {
        super(OpenQA.OPENQA_IMAGE, tr("Osmose"), tr("osmose Settings"));
        cacheDir = directory;
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        gui.getValidatorPreference().addSubTab(this, tr("Osmose"), createSubTab());
    }

    @Override
    public boolean ok() {
        ArrayList<String> prefs = new ArrayList<>();
        SortedMap<String, SortedMap<String, SortedMap<String, String>>> categories = OsmoseInformation
                .getCategories(cacheDir);
        SortedMap<String, String> errors = new TreeMap<>();
        for (SortedMap<String, SortedMap<String, String>> value : categories.values()) {
            value.values().forEach(errors::putAll);
        }
        String category = "";
        for (Component component : testPanel.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                category = label.getText();
                for (SortedMap<String, SortedMap<String, String>> descriptiveCategory : categories.values()) {
                    if (descriptiveCategory.keySet().contains(category)) {
                        errors = descriptiveCategory.get(category);
                        Logging.info("Category: {0}", category);
                        break;
                    }
                }
            }
            if (!(component instanceof JCheckBox) || (component instanceof JLabel))
                continue;
            JCheckBox preference = (JCheckBox) component;
            if (preference.isSelected()) {
                for (Entry<String, String> entry : errors.entrySet()) {
                    if (preference.getText().equals(entry.getValue())) {
                        prefs.add(entry.getKey());
                        break;
                    }
                }
            }
        }
        Config.getPref().putList(PREF_TESTS, prefs);
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui) {
        return gui.getValidatorPreference();
    }

    @Override
    public Component createSubTab() {
        testPanel = new VerticallyScrollablePanel(new GridBagLayout());
        OsmoseInformation info = new OsmoseInformation(cacheDir);
        ArrayList<String> prefs = new ArrayList<>(Config.getPref().getList(PREF_TESTS, info.buildDefaultPref()));
        SortedMap<String, SortedMap<String, SortedMap<String, String>>> errors = OsmoseInformation
                .getCategories(cacheDir);
        for (Entry<String, SortedMap<String, SortedMap<String, String>>> entry : errors.entrySet()) {
            String categoryNumber = entry.getKey();
            for (String category : entry.getValue().keySet()) {
                JLabel label = new JLabel(category + " (" + categoryNumber + ")");
                testPanel.add(label, GBC.eol());
                Logging.info("Category: {0} {1}", category, categoryNumber);
                for (String errorNumber : errors.get(categoryNumber).get(category).keySet()) {
                    String baseMessage = "";
                    boolean checked = false;
                    if (prefs.contains(errorNumber)) {
                        checked = true;
                    }
                    String errorMessage = errors.get(categoryNumber).get(category).get(errorNumber);
                    JCheckBox toAdd = new JCheckBox(tr(errorMessage), checked);
                    List<JCheckBox> list = (checkBoxes.get(baseMessage) != null) ? checkBoxes.get(baseMessage)
                            : new ArrayList<>();
                    list.add(toAdd);
                    checkBoxes.put(baseMessage, list);
                    testPanel.add(toAdd, GBC.std().fill(GBC.HORIZONTAL));
                    testPanel.add(new JLabel(errorNumber), GBC.eol());
                }
            }
        }
        return new JScrollPane(testPanel);
    }
}
