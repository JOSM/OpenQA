/**
 * 
 */
package com.kaartgroup.keepright;

import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.tools.Logging;

/**
 * @author Taylor Smock
 *
 */
public class KeepRightDataSetListener implements DataSetListener {

	/* (non-Javadoc)
	 * @see org.openstreetmap.josm.data.osm.event.DataSetListener#dataChanged(org.openstreetmap.josm.data.osm.event.DataChangedEvent)
	 */
	@Override
	public void dataChanged(DataChangedEvent arg0) {
		Layer toAdd = getKeepRightErrors(arg0.getDataset().getDataSourceBounds(), "gpx");
		List<MarkerLayer> relevantLayers = MainApplication.getLayerManager().getLayersOfType(MarkerLayer.class);
		for (MarkerLayer layer : relevantLayers) {
			if (layer.getName().equals(KeepRight.KEEP_RIGHT_LAYER_NAME)) {
				toAdd.mergeFrom(layer);
				MainApplication.getLayerManager().removeLayer(layer);
				break;
			}
		}
		MainApplication.getLayerManager().addLayer(toAdd);
	}
	
	public Layer getKeepRightErrors(List<Bounds> bounds, String type) {
		KeepRightInformation info = new KeepRightInformation();
		for (Bounds bound : bounds) {
			GpxData gpxData = info.getErrors(bound);
			if (gpxData != null) {
				Logging.debug("The gpxData is not empty");
				GpxLayer layer = new GpxLayer(gpxData);
				MarkerLayer mlayer = new MarkerLayer(gpxData, KeepRight.KEEP_RIGHT_LAYER_NAME, layer.getAssociatedFile(), layer);
				return mlayer;
			} else {
				Logging.debug("The gpxData was empty?");
			}
		}
		return null;
	}
	
	@Override
	public void otherDatasetChange(AbstractDatasetChangedEvent arg0) {
		// Don't care
	}
	
	@Override
	public void nodeMoved(NodeMovedEvent arg0) {
		// Don't care
	}

	@Override
	public void primitivesAdded(PrimitivesAddedEvent arg0) {
		// Don't care

	}

	@Override
	public void primitivesRemoved(PrimitivesRemovedEvent arg0) {
		// Don't care

	}

	@Override
	public void relationMembersChanged(RelationMembersChangedEvent arg0) {
		// Don't care

	}

	@Override
	public void tagsChanged(TagsChangedEvent arg0) {
		// Don't care

	}

	@Override
	public void wayNodesChanged(WayNodesChangedEvent arg0) {
		// Don't care

	}

}
