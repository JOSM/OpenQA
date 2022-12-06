// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;

import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.josm.data.IQuadBucketType;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.Tagged;

/**
 * The base QA node type
 *
 * @param <I> The unique identifier
 */
public class OpenQANode<I> implements Comparable<OpenQANode<?>>, IQuadBucketType, ICoordinate, ILatLon, Tagged {

    private enum Flags {
        SELECTED, MODIFIED
    }

    private final I identifier;
    private final EnumSet<Flags> flags = EnumSet.noneOf(Flags.class);
    private final double lon;
    private final double lat;
    private volatile BBox cachedBBox;
    private final TagMap tagMap = new TagMap();

    protected OpenQANode(I identifier, ILatLon latLon) {
        this(identifier, latLon.lat(), latLon.lon());
    }

    protected OpenQANode(I identifier, ICoordinate latLon) {
        this(identifier, latLon.getLat(), latLon.getLon());
    }

    protected OpenQANode(I identifier, double lat, double lon) {
        Objects.requireNonNull(identifier);
        this.identifier = identifier;
        this.lat = lat;
        this.lon = lon;
    }

    @Override
    public BBox getBBox() {
        if (this.cachedBBox == null) {
            this.createCachedBBox();
        }
        return this.cachedBBox;
    }

    private synchronized void createCachedBBox() {
        this.cachedBBox = new BBox(this.getLon(), this.getLat()).toImmutable();
    }

    @Override
    public double getLat() {
        return this.lat;
    }

    @Override
    public double lat() {
        return this.lat;
    }

    @Override
    public void setLat(double lat) {
        throw new UnsupportedOperationException("Cannot set lat/lon after creation");
    }

    @Override
    public double getLon() {
        return this.lon;
    }

    @Override
    public double lon() {
        return this.lon;
    }

    @Override
    public void setLon(double lon) {
        throw new UnsupportedOperationException("Cannot set lat/lon after creation");
    }

    public I getIdentifier() {
        return this.identifier;
    }

    private boolean setFlag(Flags flag, boolean value) {
        if (value) {
            return this.flags.add(flag);
        }
        return this.flags.remove(flag);
    }

    public boolean isModified() {
        return this.flags.contains(Flags.MODIFIED);
    }

    public boolean setModified(boolean modified) {
        return setFlag(Flags.MODIFIED, modified);
    }

    public boolean isSelected() {
        return this.flags.contains(Flags.SELECTED);
    }

    public boolean setSelected(boolean selected) {
        return setFlag(Flags.SELECTED, selected);
    }

    @Override
    public void setKeys(Map<String, String> keys) {
        this.tagMap.clear();
        this.tagMap.putAll(keys);
    }

    @Override
    public Map<String, String> getKeys() {
        return Collections.unmodifiableMap(this.tagMap);
    }

    @Override
    public void put(String key, String value) {
        this.tagMap.put(key, value);
    }

    @Override
    public String get(String key) {
        return this.tagMap.get(key);
    }

    @Override
    public void remove(String key) {
        this.tagMap.remove(key);
    }

    @Override
    public boolean hasKeys() {
        return !this.tagMap.isEmpty();
    }

    @Override
    public Collection<String> keySet() {
        return this.tagMap.keySet();
    }

    @Override
    public int getNumKeys() {
        return this.tagMap.size();
    }

    @Override
    public void removeAll() {
        this.tagMap.clear();
    }

    @Override
    public int compareTo(OpenQANode<?> o) {
        int oHash = o.hashCode();
        int tHash = this.hashCode();
        if (oHash == tHash && this.equals(o)) {
            return 0;
        }
        return Integer.compare(tHash, oHash);
    }

    @Override
    public int hashCode() {
        return this.getIdentifier().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this.getClass().isInstance(other)) {
            return this.getIdentifier().equals(this.getClass().cast(other).getIdentifier());
        }
        return false;
    }

}
