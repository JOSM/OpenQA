package com.kaart.openqa.profiles;

import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;

public abstract class ProfilePreferences extends DefaultTabPreferenceSetting implements SubPreferenceSetting {

	protected HashMap<String, List<JCheckBox>> checkBoxes = new HashMap<>();

	public ProfilePreferences(String image, String title, String description) {
		super(image, title, description);
	}

	public abstract Component createSubTab();

	public Map<String, List<JCheckBox>> getCheckBoxes() {
		return checkBoxes;
	}

}
