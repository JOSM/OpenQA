// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles.osmose;

import java.util.UUID;

import com.kaart.openqa.profiles.OpenQANode;

/**
 * The node class for Osmose
 */
public class OsmoseNode extends OpenQANode<UUID> {

    protected OsmoseNode(UUID identifier, double lat, double lon) {
        super(identifier, lat, lon);
    }
}
