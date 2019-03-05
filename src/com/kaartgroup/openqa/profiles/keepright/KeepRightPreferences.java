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

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;

import com.kaartgroup.openqa.OpenQALayerChangeListener;

/**
 * @author Taylor Smock
 *
 */
public class KeepRightPreferences extends DefaultTabPreferenceSetting implements SubPreferenceSetting {
	
	JPanel testPanel;
	
	public static String PREF_FILETYPE = "keepright.filetype";
	final String CACHE_DIR;
	
	public KeepRightPreferences(String directory) {
		super("keepright.png", "Keep Right", "Keep Right Settings");
		CACHE_DIR = directory;
	}

	@Override
	public void addGui(PreferenceTabbedPane gui) {
		testPanel = new VerticallyScrollablePanel(new GridBagLayout());
		KeepRightInformation info = new KeepRightInformation(CACHE_DIR);
		ArrayList<String> prefs = new ArrayList<>(Config.getPref().getList("keepright-tests", info.buildDefaultPref()));
		for (int error : KeepRightInformation.errors.keySet()) {
			if (error == 0) continue;
			boolean checked = false;
			if (prefs.contains(Integer.toString(error))) {
				checked = true;
			}
			String errorMessage = "";
			if (error % 10 == 0) {
				errorMessage = KeepRightInformation.errors.get(error);
			} else {
				errorMessage = KeepRightInformation.errors.get((error / 10) * 10);
				errorMessage += "/" + KeepRightInformation.errors.get(error);
			}
			JCheckBox toAdd = new JCheckBox(tr(errorMessage), checked);
			testPanel.add(toAdd, GBC.eol());
		}
		gui.getValidatorPreference().addSubTab(this, tr("Keep Right"), new JScrollPane(testPanel));
	}

	@Override
	public boolean ok() {
		ArrayList<String> prefs = new ArrayList<>();
		prefs.add("0");
		ArrayList<Integer> values = new ArrayList<Integer>(KeepRightInformation.errors.keySet());
		for (Component component : testPanel.getComponents()) {
			if (!(component instanceof JCheckBox)) continue;
			JCheckBox preference = (JCheckBox) component;
			if (preference.isSelected()) {
				String[] parts = preference.getText().split("/");
				for (int value : values) {
					if (parts.length == 1 && KeepRightInformation.errors.get(value).equals(parts[0])) {
						prefs.add(Integer.toString(value));
						break;
					} else if (parts.length == 2 && KeepRightInformation.errors.get(value).equals(parts[0])){
						for (int i = 0; i < 10; i++) {
							if (KeepRightInformation.errors.get(value + i).equals(parts[1])) {
								prefs.add(Integer.toString(value + i));
								break;
							}
						}
						break;
					}
				}
			}
		}
		Config.getPref().putList("keepright-tests", prefs);
		OpenQALayerChangeListener.updateKeepRightLayer(CACHE_DIR);
		return false;
	}

	@Override
	public TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui) {
		return gui.getValidatorPreference();
	}
}