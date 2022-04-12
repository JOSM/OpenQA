// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles.osmose;

import java.util.UUID;

import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.josm.data.coor.ILatLon;

import com.kaart.openqa.profiles.OpenQANode;

public class OsmoseNode extends OpenQANode<UUID> {

    protected OsmoseNode(UUID identifier, ILatLon latLon) {
        super(identifier, latLon);
    }

    protected OsmoseNode(UUID identifier, ICoordinate latLon) {
        super(identifier, latLon);
    }

    protected OsmoseNode(UUID identifier, double lat, double lon) {
        super(identifier, lat, lon);
    }
}
