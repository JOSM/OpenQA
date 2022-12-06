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

/**
 * A dataset for OpenQA data
 *
 * @param <I> The id for the primitives
 * @param <N> The node type
 */
public class OpenQADataSet<I, N extends OpenQANode<I>> {
    private final QuadBuckets<N> store = new QuadBuckets<>();
    private final Map<I, N> allPrimitives = new HashMap<>();
    private final ListenerList<ErrorLayer> highlightListeners = ListenerList.create();

    /**
     * Get all primitives
     *
     * @return the primitives
     */
    public Collection<N> allPrimitives() {
        return store;
    }

    /**
     * Get all nodes
     *
     * @return The nodes
     */
    public Collection<N> getNodes() {
        return store;
    }

    /**
     * Merge two datasets
     *
     * @param mergeFrom The dataset to merge from
     */
    public synchronized void mergeFrom(OpenQADataSet<I, N> mergeFrom) {
        if (mergeFrom == null) {
            return;
        }
        for (final N node : mergeFrom.allPrimitives()) {
            if (!this.containsNode(node)) {
                this.addPrimitive(node);
            } else {
                this.allPrimitives.get(node.getIdentifier()).setKeys(node.getKeys());
            }
        }
    }

    /**
     * Remove highlight listener
     *
     * @param errorLayer The layer to remove the listener from
     */
    public void removeHighlightUpdateListener(ErrorLayer errorLayer) {
        highlightListeners.removeListener(errorLayer);
    }

    /**
     * Add highlight listener
     *
     * @param errorLayer The layer to add the listener to
     */
    public void addHighlightUpdateListener(ErrorLayer errorLayer) {
        highlightListeners.addListener(errorLayer);
    }

    /**
     * Check if any of the nodes is modified
     *
     * @return {@code true} if a node was modified
     */
    public boolean isModified() {
        return this.allPrimitives().stream().anyMatch(OpenQANode::isModified);
    }

    /**
     * Get the selected nodes
     *
     * @return the selected nodes
     */
    public Collection<N> getSelectedNodes() {
        return this.store.stream().filter(OpenQANode::isSelected).collect(Collectors.toList());
    }

    /**
     * Clear the current selection
     */
    public void clearSelection() {
        this.getSelectedNodes().forEach(node -> node.setSelected(false));
    }

    /**
     * Check if this dataset contains a node
     *
     * @param displayedNode The node to look for
     * @return {@code true} if the dataset contains the node
     */
    public boolean containsNode(OpenQANode<?> displayedNode) {
        return this.allPrimitives.containsKey(displayedNode.getIdentifier());
    }

    /**
     * Set the selected nodes
     *
     * @param openQANodes The nodes to select
     */
    public void setSelected(List<N> openQANodes) {
        openQANodes.stream().filter(this::containsNode).forEach(n -> n.setSelected(true));
    }

    /**
     * Add a primitive to the dataset
     *
     * @param node the node to add
     */
    public synchronized void addPrimitive(N node) {
        this.store.add(node);
        this.allPrimitives.put(node.getIdentifier(), node);
    }

    /**
     * Remove a primitive to the dataset
     *
     * @param node the node to remove
     */
    public synchronized void removePrimitive(OpenQANode<?> node) {
        this.store.remove(node);
        this.allPrimitives.remove(node.getIdentifier());
    }

    /**
     * Clear this dataset
     */
    public synchronized void clear() {
        this.store.clear();
        this.allPrimitives.clear();
    }

    /**
     * Search for nodes
     *
     * @param location The area to search
     * @return The nodes that were found
     */
    public Collection<N> searchNodes(BBox location) {
        return this.store.search(location);
    }
}
