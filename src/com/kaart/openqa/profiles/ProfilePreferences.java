package com.kaart.openqa.profiles;

import java.awt.Component;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;

public abstract class ProfilePreferences extends DefaultTabPreferenceSetting implements SubPreferenceSetting {

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

}
