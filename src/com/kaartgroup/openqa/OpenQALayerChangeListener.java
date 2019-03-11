/**
 *
 */
package com.kaartgroup.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerAddEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerOrderChangeEvent;
import org.openstreetmap.josm.gui.layer.LayerManager.LayerRemoveEvent;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Logging;
import org.xml.sax.SAXException;

import com.kaartgroup.openqa.profiles.GenericInformation;
import com.kaartgroup.openqa.profiles.keepright.KeepRightInformation;
import com.kaartgroup.openqa.profiles.osmose.OsmoseInformation;

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
			updateOpenQALayers(CACHE_DIR);
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

	/**
	 * Update all the OpenQA layers
	 * @param CACHE_DIR The directory to cache files in
	 */
	public static void updateOpenQALayers(String CACHE_DIR) {
		List<OsmDataLayer> osmDataLayers = MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class);
		if (osmDataLayers.size() == 0) return;
		ArrayList<Layer> layers = new ArrayList<>(MainApplication.getLayerManager().getLayers());
		for (Layer layer : layers) {
			if (layer instanceof ErrorLayer) {
				MainApplication.getLayerManager().removeLayer(layer);
			}
		}
		UpdateLayersTask osmose = new UpdateLayersTask(new OsmoseInformation(CACHE_DIR), new PleaseWaitProgressMonitor());
		UpdateLayersTask keepright = new UpdateLayersTask(new KeepRightInformation(CACHE_DIR), new PleaseWaitProgressMonitor());
		MainApplication.worker.submit(osmose);
		MainApplication.worker.submit(keepright);
	}

	private static class UpdateLayersTask extends PleaseWaitRunnable {
		private boolean isCanceled;
		GenericInformation type;

		public UpdateLayersTask(GenericInformation type, PleaseWaitProgressMonitor monitor) {
			this(tr("Update {0} Layers", OpenQA.NAME), monitor, true);
			this.type = type;
		}
		public UpdateLayersTask(String title, ProgressMonitor progressMonitor, boolean ignoreException) {
			super(title, progressMonitor, ignoreException);
		}

		@Override
		protected void cancel() {
			isCanceled = true;
		}

		@Override
		protected void realRun() throws SAXException, IOException, OsmTransferException {
			List<OsmDataLayer> osmDataLayers = MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class);
			Layer toAdd = null;
			for (OsmDataLayer layer : osmDataLayers) {
				if (isCanceled) break;
				getProgressMonitor().indeterminateSubTask(tr("Updating layers"));
				Layer tlayer = type.getErrors(layer.getDataSet(), getProgressMonitor());
				if (toAdd != null) {
					toAdd.mergeFrom(tlayer);
				} else {
					toAdd = tlayer;
				}
			}
			if (toAdd != null) {
				MainApplication.getLayerManager().addLayer(toAdd, false);
			}

			List<ErrorLayer> errorLayers = MainApplication.getLayerManager().getLayersOfType(ErrorLayer.class);
			for (int i = 0; i < errorLayers.size(); i++) {
				ErrorLayer layer = errorLayers.get(i);
				if (layer == null || !MainApplication.getLayerManager().containsLayer(layer)) continue;
				String name = layer.getName();
				for (int j = i + 1; j < errorLayers.size(); j++) {
					ErrorLayer jLayer = errorLayers.get(j);
					if (jLayer == null || !MainApplication.getLayerManager().containsLayer(jLayer)) continue;
					String nextName = jLayer.getName();
					if (name.equals(nextName)) {
						layer.mergeFrom(jLayer);
						MainApplication.getLayerManager().removeLayer(jLayer);
					}
				}
			}
		}

		@Override
		protected void finish() {
			// Do nothing
		}
	}

	@Override
	public void layerOrderChanged(LayerOrderChangeEvent e) {
		// Don't care
	}

}
