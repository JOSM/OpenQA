package com.kaart.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane.PreferencePanel;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.widgets.VerticallyScrollablePanel;
import org.openstreetmap.josm.tools.GBC;

import com.kaart.openqa.profiles.ProfilePreferences;
import com.kaart.openqa.profiles.keepright.KeepRightPreferences;
import com.kaart.openqa.profiles.osmose.OsmosePreferences;

public class OpenQAPreferences extends DefaultTabPreferenceSetting implements SubPreferenceSetting {

	JPanel testPanel;

	final String CACHE_DIR;

	ArrayList<ProfilePreferences> tests = new ArrayList<>();

	public OpenQAPreferences(String directory) {
		super(OpenQA.OPENQA_IMAGE, tr("OpenQA"), tr("OpenQA Settings"));
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
			selectAll.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					toggleBoxes(preference, true);
				}
			});

			JButton selectNone = new JButton(tr("Select none"));
			selectNone.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					toggleBoxes(preference, false);
				}
			});
			JPanel tPanel = new VerticallyScrollablePanel(new GridBagLayout());
			tPanel.add(selectAll);
			tPanel.add(selectNone, GBC.eol());
			tPanel.add(subTab);
			tp.add(preference.getTitle(), new JScrollPane(tPanel));
		}
		testPanel.add(tp, BorderLayout.CENTER);
		PreferencePanel preferenceTab = gui.createPreferenceTab(this);
		preferenceTab.add(testPanel, GBC.eol().fill(GBC.BOTH));
	}

	private void toggleBoxes(ProfilePreferences preference, boolean checked) {
		HashMap<String, List<JCheckBox>> boxes = preference.getCheckBoxes();
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
