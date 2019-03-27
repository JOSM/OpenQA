package com.kaart.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.tools.GBC;

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
		testPanel = new JPanel();
		testPanel.setLayout(new BorderLayout());
		JTabbedPane tp = new JTabbedPane();
		tests.add(new KeepRightPreferences(CACHE_DIR));
		tests.add(new OsmosePreferences(CACHE_DIR));
		for (ProfilePreferences preference : tests) {
			Component subTab = preference.createSubTab();
			JButton selectAll = new JButton(tr("Select all"));
			tp.add(preference.getTitle(), subTab);
			//addSubTab(preference, preference.getTitle(), preference.createSubTab());
		}
		testPanel.add(tp, BorderLayout.CENTER);
		gui.createPreferenceTab(this).add(testPanel, GBC.eol().fill(GBC.BOTH));
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
