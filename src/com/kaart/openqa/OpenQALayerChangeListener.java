/**
 *
 */
package com.kaart.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
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

import com.kaart.openqa.profiles.keepright.KeepRightInformation;
import com.kaart.openqa.profiles.osmose.OsmoseInformation;

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
		MainApplication.worker.submit(new UpdateLayersTask(CACHE_DIR, new PleaseWaitProgressMonitor()));
	}

	private static class UpdateLayersTask extends PleaseWaitRunnable {
		private boolean isCanceled;
		String CACHE_DIR;
		ErrorLayer layer;

		public UpdateLayersTask(String CACHE_DIR, PleaseWaitProgressMonitor monitor) {
			this(tr("Update {0} Layers", OpenQA.NAME), monitor, true);
			this.CACHE_DIR = CACHE_DIR;
		}
		public UpdateLayersTask(String title, ProgressMonitor progressMonitor, boolean ignoreException) {
			super(title, progressMonitor, ignoreException);
		}

		@Override
		protected void cancel() {
			isCanceled = true;
			layer.cancel();
		}

		@Override
		protected void realRun() throws SAXException, IOException, OsmTransferException {
			if (isCanceled) return;
			List<ErrorLayer> errorLayers = MainApplication.getLayerManager().getLayersOfType(ErrorLayer.class);
			layer = null;
			if (errorLayers.isEmpty()) {
				layer = new ErrorLayer(CACHE_DIR);
				layer.setErrorClasses(KeepRightInformation.class, OsmoseInformation.class);
				MainApplication.getLayerManager().addLayer(layer);
			} else {
				layer = errorLayers.get(0);
			}
			layer.update();
		}

		@Override
		protected void finish() {
			// TODO Auto-generated method stub

		}
	}

	@Override
	public void layerOrderChanged(LayerOrderChangeEvent e) {
		// Don't care
	}

}
