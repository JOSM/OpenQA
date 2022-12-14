// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles.osmose;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

import com.kaart.openqa.OpenQA;
import com.kaart.openqa.profiles.GenericInformation;
import com.kaart.openqa.profiles.ProfilePreferences;

/**
 * The preference class for Osmose
 *
 * @author Taylor Smock
 */
public class OsmosePreferences extends ProfilePreferences {
    JPanel testPanel;

    static final String PREF_TESTS = "openqa.osmose-tests";

    /**
     * Create a new Osmose preference object
     *
     */
    public OsmosePreferences() {
        super(OpenQA.OPENQA_IMAGE, tr("Osmose"), tr("osmose Settings"));
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        gui.getValidatorPreference().addSubTab(this, tr("Osmose"), createSubTab());
    }

    @Override
    public boolean ok() {
        ArrayList<String> prefs = new ArrayList<>();
        NavigableMap<String, NavigableMap<String, NavigableMap<String, String>>> categories = OsmoseInformation
                .getCategories();
        NavigableMap<String, String> errors = new TreeMap<>();
        for (NavigableMap<String, NavigableMap<String, String>> value : categories.values()) {
            value.values().forEach(errors::putAll);
        }
        String category;
        for (Component component : testPanel.getComponents()) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                category = label.getText();
                for (NavigableMap<String, NavigableMap<String, String>> descriptiveCategory : categories.values()) {
                    if (descriptiveCategory.containsKey(category)) {
                        errors = descriptiveCategory.get(category);
                        Logging.info("Category: {0}", category);
                        break;
                    }
                }
            }
            if (!(component instanceof JCheckBox))
                continue;
            JCheckBox preference = (JCheckBox) component;
            if (preference.isSelected()) {
                final String errorNumber = Optional.of(preference.getClientProperty(GenericInformation.ERROR_ID))
                        .filter(String.class::isInstance).map(String.class::cast)
                        .orElseThrow(NullPointerException::new);
                if (errors.containsKey(errorNumber)) {
                    prefs.add(errorNumber);
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
        OsmoseInformation info = new OsmoseInformation();
        ArrayList<String> prefs = new ArrayList<>(Config.getPref().getList(PREF_TESTS, info.buildDefaultPref()));
        NavigableMap<String, NavigableMap<String, NavigableMap<String, String>>> errors = OsmoseInformation
                .getCategories();
        for (Map.Entry<String, NavigableMap<String, NavigableMap<String, String>>> entry : errors.entrySet()) {
            String categoryNumber = entry.getKey();
            for (String category : entry.getValue().keySet()) {
                JLabel label = new JLabel(category + " (" + categoryNumber + ")");
                testPanel.add(label, GBC.eol());
                Logging.info("Category: {0} {1}", category, categoryNumber);
                for (String errorNumber : errors.get(categoryNumber).get(category).keySet()) {
                    String baseMessage = "";
                    boolean checked = prefs.contains(errorNumber);
                    String errorMessage = errors.get(categoryNumber).get(category).get(errorNumber);
                    JCheckBox toAdd = new JCheckBox(tr(errorMessage), checked);
                    toAdd.putClientProperty(GenericInformation.ERROR_ID, errorNumber);
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
