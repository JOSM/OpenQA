// License: GPL. For details, see LICENSE file.
package com.kaart.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;

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
import org.openstreetmap.josm.tools.Logging;

import com.kaart.openqa.profiles.keepright.KeepRightInformation;
import com.kaart.openqa.profiles.osmose.OsmoseInformation;

/**
 * Listen to QA layer changes
 *
 * @author Taylor Smock
 */
public class OpenQALayerChangeListener implements LayerChangeListener {
    final HashMap<OsmDataLayer, OpenQADataSetListener> listeners = new HashMap<>();

    /**
     * Listen for added layers
     */
    @Override
    public void layerAdded(LayerAddEvent e) {
        if (e.getAddedLayer() instanceof OsmDataLayer) {
            OsmDataLayer layer = (OsmDataLayer) e.getAddedLayer();
            if (layer.getName().equals(KeepRightInformation.LAYER_NAME))
                return;
            int time = 0;
            while (!MainApplication.getLayerManager().containsLayer(e.getAddedLayer()) && time < 10) {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    Logging.debug(e1.getMessage());
                }
                time++;
            }

            OpenQADataSetListener listener = new OpenQADataSetListener();
            layer.data.addDataSetListener(listener);
            listeners.put(layer, listener);
            List<ErrorLayer> errorLayers = MainApplication.getLayerManager().getLayersOfType(ErrorLayer.class);
            if (!errorLayers.isEmpty()) {
                updateOpenQALayers();
            }
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
     *
     */
    public static void updateOpenQALayers() {
        List<OsmDataLayer> osmDataLayers = MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class);
        if (osmDataLayers.isEmpty())
            return;
        MainApplication.worker.submit(new UpdateLayersTask(new PleaseWaitProgressMonitor()));
    }

    private static class UpdateLayersTask extends PleaseWaitRunnable {
        private boolean isCanceled;
        ErrorLayer layer;

        public UpdateLayersTask(PleaseWaitProgressMonitor monitor) {
            this(tr("Update {0} Layers", OpenQA.NAME), monitor, false);
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
        protected void realRun() {
            if (isCanceled)
                return;
            List<ErrorLayer> errorLayers = MainApplication.getLayerManager().getLayersOfType(ErrorLayer.class);
            layer = null;
            if (errorLayers.isEmpty()) {
                layer = new ErrorLayer();
                layer.setErrorClasses(KeepRightInformation.class, OsmoseInformation.class);
                MainApplication.getLayerManager().addLayer(layer);
            } else {
                layer = errorLayers.get(0);
            }
            layer.update(progressMonitor);
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
