// License: GPL. For details, see LICENSE file.
package com.kaart.openqa;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.QuadBuckets;
import org.openstreetmap.josm.tools.ListenerList;

import com.kaart.openqa.profiles.OpenQANode;

public class OpenQADataSet<I, N extends OpenQANode<I>> {
    private final QuadBuckets<N> store = new QuadBuckets<>();
    private final Map<I, N> allPrimitives = new HashMap<>();
    private final ListenerList<ErrorLayer> highlightListeners = ListenerList.create();

    public Collection<N> allPrimitives() {
        return store;
    }

    public Collection<N> getNodes() {
        return store;
    }

    public synchronized void mergeFrom(OpenQADataSet<I, N> mergeFrom) {
        for (final N node : mergeFrom.allPrimitives()) {
            if (!this.containsNode(node)) {
                this.addPrimitive(node);
            } else {
                this.allPrimitives.get(node.getIdentifier()).setKeys(node.getKeys());
            }
        }
    }

    public void removeHighlightUpdateListener(ErrorLayer errorLayer) {
        highlightListeners.removeListener(errorLayer);
    }

    public void addHighlightUpdateListener(ErrorLayer errorLayer) {
        highlightListeners.addListener(errorLayer);
    }

    public boolean isModified() {
        return this.allPrimitives().stream().anyMatch(OpenQANode::isModified);
    }

    public Collection<N> getSelectedNodes() {
        return this.store.stream().filter(OpenQANode::isSelected).collect(Collectors.toList());
    }

    public void clearSelection() {
        this.getSelectedNodes().forEach(node -> node.setSelected(false));
    }

    public boolean containsNode(OpenQANode<?> displayedNode) {
        return this.allPrimitives.containsKey(displayedNode.getIdentifier());
    }

    public void setSelected(List<N> openQANodes) {
        openQANodes.stream().filter(this::containsNode).forEach(n -> n.setSelected(true));
    }

    public synchronized void addPrimitive(N node) {
        this.store.add(node);
        this.allPrimitives.put(node.getIdentifier(), node);
    }

    public synchronized void removePrimitive(OpenQANode<?> node) {
        this.store.remove(node);
        this.allPrimitives.remove(node.getIdentifier());
    }

    public synchronized void clear() {
        this.store.clear();
        this.allPrimitives.clear();
    }

    public Collection<N> searchNodes(BBox location) {
        return this.store.search(location);
    }
}
