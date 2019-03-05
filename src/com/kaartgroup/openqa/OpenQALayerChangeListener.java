/**
 * 
 */
package com.kaartgroup.openqa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Logging;

import com.kaartgroup.openqa.profiles.keepright.KeepRightInformation;

/**
 * @author Taylor Smock
 *
 */
public class OpenQALayerChangeListener implements LayerChangeListener {
	HashMap<OsmDataLayer, OpenQADataSetListener> listeners = new HashMap<>();
	private final String CACHE_DIR;
	
	public OpenQALayerChangeListener(String CACHE_DIR) {
		super();
		this.CACHE_DIR = CACHE_DIR;
	}

	/**
	 * Listen for added layers
	 */
	@Override
	public void layerAdded(LayerAddEvent e) {
		if (e.getAddedLayer() instanceof OsmDataLayer) {
			OsmDataLayer layer = (OsmDataLayer) e.getAddedLayer();
			if (layer.getName().equals(KeepRightInformation.LAYER_NAME)) return;
			int time = 0;
			while (!MainApplication.getLayerManager().containsLayer(e.getAddedLayer()) && time < 10) {
				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (InterruptedException e1) {
					Logging.debug(e1.getMessage());
				}
				time++;
			}

			OpenQADataSetListener listener = new OpenQADataSetListener(CACHE_DIR);
			layer.data.addDataSetListener(listener);
			listeners.put(layer, listener);
			
			updateKeepRightLayer(CACHE_DIR);
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
	
	public static void updateKeepRightLayer(String CACHE_DIR) {
		List<OsmDataLayer> osmDataLayers = MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class);
		if (osmDataLayers.size() > 0) {
			ArrayList<Layer> layers = new ArrayList<>(MainApplication.getLayerManager().getLayers());
			for (Layer layer : layers) {
				if (layer instanceof ErrorLayer) {
					MainApplication.getLayerManager().removeLayer(layer);
					osmDataLayers.remove(layer);
				}
			}
			KeepRightInformation info = new KeepRightInformation(CACHE_DIR);
			Layer toAdd = null;
			for (OsmDataLayer layer : osmDataLayers) {
				Layer tlayer = info.getErrors(layer.getDataSet().getDataSourceBounds());
				if (toAdd != null) {
					toAdd.mergeFrom(tlayer);
				} else {
					toAdd = tlayer;
				}
			}
			if (toAdd != null) {
				MainApplication.getLayerManager().addLayer(toAdd, false);
			}
		}
	}
	
	@Override
	public void layerOrderChanged(LayerOrderChangeEvent e) {
		// Don't care
	}

}
