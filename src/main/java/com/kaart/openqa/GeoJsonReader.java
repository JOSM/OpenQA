// License: GPL. For details, see LICENSE file.
package com.kaart.openqa;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.tools.Logging;

import com.kaart.openqa.profiles.OpenQANode;

/**
 * Reader that reads GeoJSON files. See
 * <a href="https://tools.ietf.org/html/rfc7946">RFC 7946</a> for more
 * information.
 */
public class GeoJsonReader<I, N extends OpenQANode<I>, D extends OpenQADataSet<I, N>> {

    // this could be a record class
    private static final class LL implements ILatLon {
        private final double lon;
        private final double lat;

        LL(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public double lon() {
            return this.lon;
        }

        @Override
        public double lat() {
            return this.lat;
        }
    }

    private static final String COORDINATES = "coordinates";
    private static final String FEATURES = "features";
    private static final String PROPERTIES = "properties";
    private static final String GEOMETRY = "geometry";
    private static final String TYPE = "type";
    private final Supplier<D> dataSetSupplier;
    private final BiFunction<Map<String, String>, ILatLon, N> nodeCreator;
    private JsonParser parser;
    private D ds;

    GeoJsonReader(Supplier<D> dataSetSupplier, BiFunction<Map<String, String>, ILatLon, N> nodeCreator) {
        // Restricts visibility
        this.dataSetSupplier = dataSetSupplier;
        this.nodeCreator = nodeCreator;
    }

    private void setParser(final JsonParser parser) {
        this.parser = parser;
    }

    private void parse() {
        while (parser.hasNext()) {
            Event event = parser.next();
            if (event == Event.START_OBJECT) {
                parseRoot(parser.getObject());
            }
        }
        parser.close();
    }

    private void parseRoot(final JsonObject object) {
        switch (object.getString(TYPE)) {
        case "FeatureCollection":
            parseFeatureCollection(object.getJsonArray(FEATURES));
            break;
        case "Feature":
            parseFeature(object);
            break;
        case "GeometryCollection":
            parseGeometryCollection(null, object);
            break;
        default:
            parseGeometry(null, object);
        }
    }

    private void parseFeatureCollection(final JsonArray features) {
        for (JsonValue feature : features) {
            if (feature instanceof JsonObject) {
                JsonObject item = (JsonObject) feature;
                parseFeature(item);
            }
        }
    }

    private void parseFeature(final JsonObject feature) {
        JsonObject geometry = feature.getJsonObject(GEOMETRY);
        parseGeometry(feature, geometry);
    }

    private void parseGeometryCollection(final JsonObject feature, final JsonObject geometry) {
        JsonArray geometries = geometry.getJsonArray("geometries");
        for (JsonValue jsonValue : geometries) {
            parseGeometry(feature, jsonValue.asJsonObject());
        }
    }

    private void parseGeometry(final JsonObject feature, final JsonObject geometry) {
        switch (geometry.getString(TYPE)) {
        case "Point":
            parsePoint(feature, geometry.getJsonArray(COORDINATES));
            break;
        case "MultiPoint":
        case "LineString":
        case "MultiLineString":
        case "Polygon":
        case "MultiPolygon":
            throw new UnsupportedOperationException(geometry.getString(TYPE) + " not supported");
        case "GeometryCollection":
            parseGeometryCollection(feature, geometry);
            break;
        default:
            parseUnknown(geometry);
        }
    }

    private void parsePoint(final JsonObject feature, final JsonArray coordinates) {
        double lat = coordinates.getJsonNumber(1).doubleValue();
        double lon = coordinates.getJsonNumber(0).doubleValue();
        createNode(getTags(feature), lat, lon);
    }

    private void createNode(final Map<String, String> tags, final double lat, final double lon) {
        final N node = this.nodeCreator.apply(tags, new LL(lat, lon));
        if (node != null) {
            node.setKeys(tags);
        }
        getDataSet().addPrimitive(node);
    }

    private void parseUnknown(final JsonObject object) {
        Logging.warn(tr("Unknown json object found {0}", object));
    }

    private Map<String, String> getTags(final JsonObject feature) {
        final Map<String, String> tags = new TreeMap<>();

        if (feature.containsKey(PROPERTIES) && !feature.isNull(PROPERTIES)) {
            JsonObject properties = feature.getJsonObject(PROPERTIES);
            for (Map.Entry<String, JsonValue> stringJsonValueEntry : properties.entrySet()) {
                final JsonValue value = stringJsonValueEntry.getValue();

                if (value instanceof JsonString) {
                    tags.put(stringJsonValueEntry.getKey(), ((JsonString) value).getString());
                } else if (value instanceof JsonStructure) {
                    Logging.warn("The GeoJSON contains an object with property '" + stringJsonValueEntry.getKey()
                            + "' whose value has the unsupported type '" + value.getClass().getSimpleName()
                            + "'. That key-value pair is ignored!");
                } else if (value.getValueType() != JsonValue.ValueType.NULL) {
                    // WARNING: DO NOT ADD NULL TO THE TAG MAP!
                    tags.put(stringJsonValueEntry.getKey(), value.toString());
                }
            }
        }
        return tags;
    }

    protected D doParseDataSet(InputStream source) {
        this.ds = this.dataSetSupplier.get();
        setParser(Json.createParser(source));
        parse();

        return getDataSet();
    }

    private D getDataSet() {
        return this.ds;
    }

    /**
     * Parse the given input source and return the dataset.
     *
     * @param source the source input stream. Must not be null.
     * @return the dataset with the parsed data
     */
    public static <I, N extends OpenQANode<I>, D extends OpenQADataSet<I, N>> D parseDataSet(InputStream source,
            final Supplier<D> dataSetSupplier, BiFunction<Map<String, String>, ILatLon, N> nodeCreator) {
        final GeoJsonReader<I, N, D> gjr = new GeoJsonReader<>(dataSetSupplier, nodeCreator);
        return gjr.doParseDataSet(source);
    }

}
