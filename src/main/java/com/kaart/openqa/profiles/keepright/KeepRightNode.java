// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles.keepright;

import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.josm.data.coor.ILatLon;

import com.kaart.openqa.profiles.OpenQANode;

public class KeepRightNode extends OpenQANode<Long> {
    protected KeepRightNode(Long identifier, ILatLon latLon) {
        super(identifier, latLon);
    }

    protected KeepRightNode(Long identifier, ICoordinate latLon) {
        super(identifier, latLon);
    }

    protected KeepRightNode(Long identifier, double lat, double lon) {
        super(identifier, lat, lon);
    }
}
