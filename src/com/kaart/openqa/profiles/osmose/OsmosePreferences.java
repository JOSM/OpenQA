/**
 *
 */
package com.kaart.openqa.profiles.osmose;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;
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

import com.kaart.openqa.profiles.ProfilePreferences;

/**
 * @author Taylor Smock
 *
 */
public class OsmosePreferences extends ProfilePreferences {
	JPanel testPanel;

	final String CACHE_DIR;
	final static String PREF_TESTS = "openqa.osmose-tests";

	public OsmosePreferences(String directory) {
		super("keepright.png", tr("Osmose"), tr("osmose Settings"));
		CACHE_DIR = directory;
	}

	@Override
	public void addGui(PreferenceTabbedPane gui) {
		gui.getValidatorPreference().addSubTab(this, tr("Osmose"), createSubTab());
	}

	@Override
	public boolean ok() {
		ArrayList<String> prefs = new ArrayList<>();
		TreeMap<String, TreeMap<String, TreeMap<String, String>>> categories = OsmoseInformation.getCategories(CACHE_DIR);
		TreeMap<String, String> errors = categories.get(categories.firstKey()).firstEntry().getValue();
		String category = categories.get(categories.firstKey()).firstKey();
		for (Component component : testPanel.getComponents()) {
			if (component instanceof JLabel) {
				JLabel label = (JLabel) component;
				category = label.getText();
				for (TreeMap<String, TreeMap<String, String>> descriptiveCategory : categories.values()) {
					if (descriptiveCategory.keySet().contains(category)) {
						errors = descriptiveCategory.get(category);
						Logging.info("Category: {0}", category);
						break;
					}
				}
				continue;
			} else if (!(component instanceof JCheckBox)) continue;
			JCheckBox preference = (JCheckBox) component;
			if (preference.isSelected()) {
				for (String key : errors.keySet()) {
					if (preference.getText().equals(errors.get(key))) {
						prefs.add(key);
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
		OsmoseInformation info = new OsmoseInformation(CACHE_DIR);
		ArrayList<String> prefs = new ArrayList<>(Config.getPref().getList(PREF_TESTS, info.buildDefaultPref()));
		TreeMap<String, TreeMap<String, TreeMap<String, String>>> errors = OsmoseInformation.getCategories(CACHE_DIR);
		for (String categoryNumber : errors.keySet()) {
			for (String category : errors.get(categoryNumber).keySet()) {
				JLabel label = new JLabel(category);
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
					List<JCheckBox> list = (checkBoxes.get(baseMessage) != null) ? checkBoxes.get(baseMessage) : new ArrayList<JCheckBox>();
					list.add(toAdd);
					checkBoxes.put(baseMessage, list);
					testPanel.add(toAdd, GBC.eol());
				}
			}
		}
		return new JScrollPane(testPanel);
	}
}
