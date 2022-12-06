// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles.keepright;

import org.openstreetmap.josm.data.coor.ILatLon;

import com.kaart.openqa.profiles.OpenQANode;

/**
 * The base node class for KeepRight
 */
public class KeepRightNode extends OpenQANode<Long> {
    protected KeepRightNode(Long identifier, ILatLon latLon) {
        super(identifier, latLon.lat(), latLon.lon());
    }

}
