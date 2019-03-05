package com.kaartgroup.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.AbstractAction;

import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;

public class ForceClear extends AbstractAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4472400258489788312L;
	final String CACHE_DIR;

	public ForceClear(String CACHE_DIR) {
        new ImageProvider("dialogs", "delete").getResource().attachImageIcon(this, true);
        putValue(SHORT_DESCRIPTION, tr("Clear cached information for OpenQA."));
        putValue(NAME, tr("Clear"));
        this.CACHE_DIR = CACHE_DIR;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		File directory = new File(CACHE_DIR);
		Utils.deleteDirectory(directory);
		directory.mkdirs();
		OpenQALayerChangeListener.updateKeepRightLayer(CACHE_DIR);
	}

}
