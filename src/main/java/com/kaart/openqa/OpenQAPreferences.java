// License: GPL. For details, see LICENSE file.
package com.kaart.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane.PreferencePanel;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.tools.GBC;

import com.kaart.openqa.profiles.ProfilePreferences;
import com.kaart.openqa.profiles.keepright.KeepRightPreferences;
import com.kaart.openqa.profiles.osmose.OsmosePreferences;

/**
 * The OpenQA preferences class
 */
public class OpenQAPreferences extends DefaultTabPreferenceSetting implements SubPreferenceSetting {

    JPanel testPanel;

    final ArrayList<ProfilePreferences> tests = new ArrayList<>();

    /**
     * Create a new preferences
     *
     */
    public OpenQAPreferences() {
        super(OpenQA.OPENQA_IMAGE, tr("OpenQA"), tr("OpenQA Settings"));
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        testPanel = new JPanel();
        testPanel.setLayout(new GridBagLayout());
        JTabbedPane tp = new JTabbedPane();
        tests.add(new KeepRightPreferences());
        tests.add(new OsmosePreferences());
        for (ProfilePreferences preference : tests) {
            Component subTab = preference.createSubTab();
            JButton selectAll = new JButton(tr("Select all"));
            selectAll.addActionListener(e -> toggleBoxes(preference, true));

            JButton selectNone = new JButton(tr("Select none"));
            selectNone.addActionListener(e -> toggleBoxes(preference, false));
            JPanel tPanel = new JPanel(new GridBagLayout());
            tPanel.add(selectAll, GBC.std());
            tPanel.add(selectNone, GBC.eol());
            tPanel.add(subTab, GBC.eol().fill());
            tp.add(preference.getTitle(), tPanel);
        }
        testPanel.add(tp, GBC.eol().fill().anchor(GBC.LINE_START));
        PreferencePanel preferenceTab = gui.createPreferenceTab(this);
        preferenceTab.add(testPanel, GBC.eol().fill(GBC.BOTH));
    }

    private static void toggleBoxes(ProfilePreferences preference, boolean checked) {
        Map<String, List<JCheckBox>> boxes = preference.getCheckBoxes();
        for (List<JCheckBox> boxList : boxes.values()) {
            for (JCheckBox checkBox : boxList) {
                checkBox.setSelected(checked);
            }
        }
    }

    @Override
    public boolean ok() {
        boolean ok = false;
        for (ProfilePreferences preference : tests) {
            if (preference == null)
                continue;
            boolean nok = preference.ok();
            if (!ok)
                ok = nok;
        }
        OpenQALayerChangeListener.updateOpenQALayers();
        return ok;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui) {
        return gui.getValidatorPreference();
    }
}
