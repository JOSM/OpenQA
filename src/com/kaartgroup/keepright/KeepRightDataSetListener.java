/**
 * 
 */
package com.kaartgroup.keepright;

import java.util.List;

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
		KeepRightLayerChangeListener.updateKeepRightLayer();
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
