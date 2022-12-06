// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles.keepright;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;

import com.kaart.openqa.OpenQA;
import com.kaart.openqa.profiles.ProfilePreferences;

/**
 * The preferences for keep right
 *
 * @author Taylor Smock
 */
public class KeepRightPreferences extends ProfilePreferences {

    JPanel testPanel;

    static final String PREF_TESTS = "openqa.keepright-tests";

    /**
     * Create a new KeepRightPreferences object
     *
     */
    public KeepRightPreferences() {
        super(OpenQA.OPENQA_IMAGE, tr("Keep Right"), tr("Keep Right Settings"));
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        gui.getValidatorPreference().addSubTab(this, tr("Keep Right"), createSubTab());
    }

    @Override
    public boolean ok() {
        ArrayList<String> prefs = new ArrayList<>();
        prefs.add("0");
        ArrayList<String> values = new ArrayList<>(KeepRightInformation.errors.keySet());
        for (Component component : testPanel.getComponents()) {
            if (!(component instanceof JCheckBox))
                continue;
            JCheckBox preference = (JCheckBox) component;
            if (preference.isSelected()) {
                String[] parts = preference.getText().split("/");
                for (String value : values) {
                    boolean toBreak = false;
                    if (parts.length == 1 && KeepRightInformation.errors.get(value).equals(parts[0])) {
                        prefs.add(value);
                        toBreak = true;
                    } else if (parts.length == 2 && KeepRightInformation.errors.get(value).equals(parts[0])) {
                        for (int i = 0; i < 10; i++) {
                            String toGet = Integer.toString(Integer.parseInt(value) + i);
                            if (parts[1].equals(KeepRightInformation.errors.get(toGet))) {
                                prefs.add(toGet);
                                break;
                            }
                        }
                        toBreak = true;
                    }
                    if (toBreak)
                        break;
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
        KeepRightInformation info = new KeepRightInformation();
        ArrayList<String> prefs = new ArrayList<>(Config.getPref().getList(PREF_TESTS, info.buildDefaultPref()));
        for (String error : KeepRightInformation.errors.keySet()) {
            if ("0".equals(error))
                continue;
            boolean checked = prefs.contains(error);
            String errorMessage;
            String baseMessage = "";
            if (Integer.parseInt(error) % 10 == 0) {
                errorMessage = KeepRightInformation.errors.get(error);
            } else {
                errorMessage = KeepRightInformation.errors.get(Integer.toString((Integer.parseInt(error) / 10) * 10));
                baseMessage = errorMessage.trim();
                errorMessage += "/" + KeepRightInformation.errors.get(error);
            }
            JCheckBox toAdd = new JCheckBox(tr(errorMessage), checked);
            List<JCheckBox> list = (checkBoxes.get(baseMessage) != null) ? checkBoxes.get(baseMessage)
                    : new ArrayList<>();
            list.add(toAdd);
            checkBoxes.put(baseMessage, list);
            testPanel.add(toAdd, GBC.eol().anchor(GBC.LINE_START).fill(GBC.HORIZONTAL));
        }
        return new JScrollPane(testPanel);
    }
}
