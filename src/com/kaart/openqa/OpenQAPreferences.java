package com.kaart.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JPanel;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;

import com.kaart.openqa.profiles.ProfilePreferences;
import com.kaart.openqa.profiles.keepright.KeepRightPreferences;
import com.kaart.openqa.profiles.osmose.OsmosePreferences;

public class OpenQAPreferences extends DefaultTabPreferenceSetting implements SubPreferenceSetting {

	JPanel testPanel;

	final String CACHE_DIR;

	ArrayList<ProfilePreferences> tests = new ArrayList<>();

	public OpenQAPreferences(String directory) {
		super("keepright.png", tr("OpenQA"), tr("OpenQA Settings"));
		CACHE_DIR = directory;
	}

	@Override
	public void addGui(PreferenceTabbedPane gui) {
		testPanel = new VerticallyScrollablePanel(new GridBagLayout());
		tests.add(new KeepRightPreferences(CACHE_DIR));
		tests.add(new OsmosePreferences(CACHE_DIR));

		for (ProfilePreferences preference : tests) {
			gui.getValidatorPreference().addSubTab(preference, preference.getTitle(), preference.createSubTab());
		}
	}

	@Override
	public boolean ok() {
		boolean ok = false;
		for (ProfilePreferences preference : tests) {
			if (preference == null) continue;
			boolean nok = preference.ok();
			if (!ok) ok = nok;
		}
		OpenQALayerChangeListener.updateOpenQALayers(CACHE_DIR);
		return ok;
	}

	@Override
	public TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui) {
		return gui.getValidatorPreference();
	}
}
