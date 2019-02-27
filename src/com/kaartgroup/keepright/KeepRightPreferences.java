/**
 * 
 */
package com.kaartgroup.keepright;

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
import org.openstreetmap.josm.tools.Logging;

/**
 * @author Taylor Smock
 *
 */
public class KeepRightPreferences extends DefaultTabPreferenceSetting implements SubPreferenceSetting {
	
	JPanel testPanel;
	
	public KeepRightPreferences() {
		super("keepright.png", "Keep Right", "Keep Right Settings");
	}

	@Override
	public void addGui(PreferenceTabbedPane gui) {
		Logging.setLogLevel(Logging.LEVEL_DEBUG);
		testPanel = new VerticallyScrollablePanel(new GridBagLayout());
		KeepRightInformation info = new KeepRightInformation();
		ArrayList<String> prefs = new ArrayList<>(Config.getPref().getList("keepright-tests", info.buildDefaultPref()));
		for (int error : info.errors.keySet()) {
			if (error == 0) continue;
			boolean checked = false;
			if (prefs.contains(Integer.toString(error))) {
				checked = true;
			}
			String errorMessage = "";
			if (error % 10 == 0) {
				errorMessage = info.errors.get(error);
			} else {
				errorMessage = info.errors.get((error / 10) * 10);
				errorMessage += "/" + info.errors.get(error);
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
		KeepRightInformation info = new KeepRightInformation();
		ArrayList<Integer> values = new ArrayList<Integer>(info.errors.keySet());
		for (Component component : testPanel.getComponents()) {
			if (!(component instanceof JCheckBox)) continue;
			JCheckBox preference = (JCheckBox) component;
			if (preference.isSelected()) {
				String[] parts = preference.getText().split("/");
				for (int value : values) {
					if (parts.length == 1 && info.errors.get(value).equals(parts[0])) {
						prefs.add(Integer.toString(value));
						break;
					} else if (parts.length == 2 && info.errors.get(value).equals(parts[0])){
						for (int i = 0; i < 10; i++) {
							if (info.errors.get(value + i).equals(parts[1])) {
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
		KeepRightLayerChangeListener.updateKeepRightLayer();
		return false;
	}

	@Override
	public TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui) {
		return gui.getValidatorPreference();
	}
}
