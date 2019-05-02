/**
 *
 */
package com.kaart.openqa;

import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.tools.Logging;

/**
 * @author Taylor Smock
 *
 */
public class OpenQADataSetListener implements DataSetListener {

	private List<Bounds> bounds;

	private final String CACHE_DIR;

	public OpenQADataSetListener(String CACHE_DIR) {
		this.CACHE_DIR = CACHE_DIR;
	}
	/* (non-Javadoc)
	 * @see org.openstreetmap.josm.data.osm.event.DataSetListener#dataChanged(org.openstreetmap.josm.data.osm.event.DataChangedEvent)
	 */
	@Override
	public void dataChanged(DataChangedEvent e) {
		Logging.error(e.toString());
		List<Bounds> tBounds = e.getDataset().getDataSourceBounds();
		if (bounds == null || bounds.containsAll(tBounds)) {
			bounds = tBounds;
			OpenQALayerChangeListener.updateOpenQALayers(CACHE_DIR);
		}
	}

	@Override
	public void otherDatasetChange(AbstractDatasetChangedEvent e) {
		// Don't care
	}

	@Override
	public void nodeMoved(NodeMovedEvent e) {
		// Don't care
	}

	@Override
	public void primitivesAdded(PrimitivesAddedEvent e) {
		// Don't care

	}

	@Override
	public void primitivesRemoved(PrimitivesRemovedEvent e) {
		// Don't care

	}

	@Override
	public void relationMembersChanged(RelationMembersChangedEvent e) {
		// Don't care

	}

	@Override
	public void tagsChanged(TagsChangedEvent e) {
		// Don't care

	}

	@Override
	public void wayNodesChanged(WayNodesChangedEvent e) {
		// Don't care

	}

}
