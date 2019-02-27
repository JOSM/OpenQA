/**
 * 
 */
package com.kaartgroup.keepright;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.tools.Logging;

import org.openstreetmap.josm.gui.layer.OsmDataLayer;

/**
 * @author Taylor Smock
 *
 */
public class KeepRightLayerChangeListener implements LayerChangeListener {
	HashMap<OsmDataLayer, KeepRightDataSetListener> listeners = new HashMap<>();
	
	public KeepRightLayerChangeListener() {
		super();
	}

	/**
	 * Listen for added layers
	 */
	@Override
	public void layerAdded(LayerAddEvent e) {
		if (e.getAddedLayer() instanceof OsmDataLayer) {
			OsmDataLayer layer = (OsmDataLayer) e.getAddedLayer();
			KeepRightDataSetListener listener = new KeepRightDataSetListener();
			layer.data.addDataSetListener(listener);
			Layer toAdd = listener.getKeepRightErrors(layer.getDataSet().getDataSourceBounds(), "gpx");
			
			int time = 0;
			while (!MainApplication.getLayerManager().containsLayer(e.getAddedLayer()) && time < 10) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e1) {
					Logging.debug(e1.getMessage());
				}
				time++;
			}
			MainApplication.getLayerManager().addLayer(toAdd);
			listeners.put(layer, listener);
		}
	}

	/**
	 * Listen for removed layers
	 */
	@Override
	public void layerRemoving(LayerRemoveEvent e) {
		if (e.getRemovedLayer() instanceof OsmDataLayer) {
			OsmDataLayer layer = (OsmDataLayer) e.getRemovedLayer();
			layer.data.removeDataSetListener(listeners.get(layer));
			listeners.remove(layer);
		}
	}
	
	@Override
	public void layerOrderChanged(LayerOrderChangeEvent e) {
		// Don't care
	}

}
