package com.kaart.openqa.profiles;

import java.awt.Component;
import java.util.HashMap;
import java.util.List;

import javax.swing.JCheckBox;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;

public abstract class ProfilePreferences extends DefaultTabPreferenceSetting implements SubPreferenceSetting {

	protected HashMap<String, List<JCheckBox>> checkBoxes = new HashMap<>();

	public ProfilePreferences(String image, String title, String description) {
		super(image, title, description);
	}

	@Override
	public abstract void addGui(PreferenceTabbedPane gui);

	@Override
	public abstract boolean ok();

	@Override
	public abstract TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui);

	public abstract Component createSubTab();

	public HashMap<String, List<JCheckBox>> getCheckBoxes() {
		final HashMap<String, List<JCheckBox>> returnBoxes = checkBoxes;
		return returnBoxes;
	}

}
