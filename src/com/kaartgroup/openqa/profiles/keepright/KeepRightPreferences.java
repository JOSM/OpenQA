/**
 *
 */
package com.kaartgroup.openqa.profiles.keepright;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;

import com.kaartgroup.openqa.profiles.ProfilePreferences;

/**
 * @author Taylor Smock
 *
 */
public class KeepRightPreferences extends ProfilePreferences {

	JPanel testPanel;

	final String CACHE_DIR;
	final static String PREF_TESTS = "openqa.keepright-tests";

	public KeepRightPreferences(String directory) {
		super("keepright.png", tr("Keep Right"), tr("Keep Right Settings"));
		CACHE_DIR = directory;
	}

	@Override
	public void addGui(PreferenceTabbedPane gui) {
		gui.getValidatorPreference().addSubTab(this, tr("Keep Right"), createSubTab());
	}

	@Override
	public boolean ok() {
		ArrayList<String> prefs = new ArrayList<>();
		prefs.add("0");
		ArrayList<String> values = new ArrayList<String>(KeepRightInformation.errors.keySet());
		for (Component component : testPanel.getComponents()) {
			if (!(component instanceof JCheckBox)) continue;
			JCheckBox preference = (JCheckBox) component;
			if (preference.isSelected()) {
				String[] parts = preference.getText().split("/");
				for (String value : values) {
					if (parts.length == 1 && KeepRightInformation.errors.get(value).equals(parts[0])) {
						prefs.add(value);
						break;
					} else if (parts.length == 2 && KeepRightInformation.errors.get(value).equals(parts[0])){
						for (int i = 0; i < 10; i++) {
							if (KeepRightInformation.errors.get(value + i).equals(parts[1])) {
								prefs.add(Integer.toString(Integer.parseInt(value) + i));
								break;
							}
						}
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
		KeepRightInformation info = new KeepRightInformation(CACHE_DIR);
		ArrayList<String> prefs = new ArrayList<>(Config.getPref().getList(PREF_TESTS, info.buildDefaultPref()));
		for (String error : KeepRightInformation.errors.keySet()) {
			if ("0".equals(error)) continue;
			boolean checked = false;
			if (prefs.contains(error)) {
				checked = true;
			}
			String errorMessage = "";
			if (Integer.parseInt(error) % 10 == 0) {
				errorMessage = KeepRightInformation.errors.get(error);
			} else {
				errorMessage = KeepRightInformation.errors.get(Integer.toString((Integer.parseInt(error) / 10) * 10));
				errorMessage += "/" + KeepRightInformation.errors.get(error);
			}
			JCheckBox toAdd = new JCheckBox(tr(errorMessage), checked);
			testPanel.add(toAdd, GBC.eol());
		}
		return new JScrollPane(testPanel);
	}
}
