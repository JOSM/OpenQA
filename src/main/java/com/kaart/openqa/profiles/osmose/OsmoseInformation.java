// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles.osmose;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.imageio.ImageIO;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParsingException;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.jcs3.access.CacheAccess;
import org.apache.commons.jcs3.engine.behavior.ICompositeCacheAttributes;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.TagMap;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.date.DateUtils;

import com.kaart.openqa.OpenQACache;
import com.kaart.openqa.OpenQADataSet;
import com.kaart.openqa.profiles.GenericInformation;

/**
 * The information class for
 * <a href="https://osmose.openstreetmap.fr">OSMOSE</a>.
 *
 * @author Taylor Smock
 */
public class OsmoseInformation extends GenericInformation<UUID, OsmoseNode, OpenQADataSet<UUID, OsmoseNode>> {
    public static final String NAME = "Osmose";

    public static final String LAYER_NAME = "Osmose Errors";
    public static final String BASE_API = "https://osmose.openstreetmap.fr/{0}/api/0.3/";
    public static final String BASE_IMG = "https://osmose.openstreetmap.fr/images/markers/marker-b-%s.png";
    public static final String BASE_ERROR_URL = "https://osmose.openstreetmap.fr/{0}/error/";

    private static final String ADDITIONAL_INFORMATION = "ADDITIONAL_INFORMATION";
    private static final CacheAccess<String, ImageIcon> LARGE_ICON_CACHE = JCSCacheManager
            .getCache("openqa:osmose:largeicons");
    static {
        final ICompositeCacheAttributes attributes = LARGE_ICON_CACHE.getCacheAttributes();
        attributes.setUseMemoryShrinker(true);
        attributes.setMaxMemoryIdleTimeSeconds(10);
        LARGE_ICON_CACHE.setCacheAttributes(attributes);
    }

    private static final NavigableMap<String, String> ERROR_MAP = new TreeMap<>();

    @Override
    public String getName() {
        return NAME;
    }

    private OsmoseDataSet getGeoJsonErrors(Bounds bound) {
        OsmoseDataSet ds = createNewDataSet();
        try (JsonParser json = Json.createParser(getFile(bound))) {
            while (json.hasNext()) {
                if (json.next() == Event.START_OBJECT) {
                    JsonObject jobject = json.getObject();
                    JsonArray errors = jobject.getJsonArray("issues");
                    for (JsonObject error : errors.getValuesAs(JsonObject.class)) {
                        final TagMap tagMap = new TagMap();
                        double lat = Double.MAX_VALUE;
                        double lon = Double.MAX_VALUE;
                        for (Map.Entry<String, JsonValue> entry : error.entrySet()) {
                            String field = entry.getKey();
                            if ("lat".equals(field)) {
                                lat = Double.parseDouble(entry.getValue().toString());
                            } else if ("lon".equals(field)) {
                                lon = Double.parseDouble(entry.getValue().toString());
                            } else if (entry.getValue() instanceof JsonString) {
                                tagMap.put(field, ((JsonString) entry.getValue()).getString());
                            } else if (!JsonValue.NULL.equals(entry.getValue())) {
                                tagMap.put(field, entry.getValue().toString());
                            }
                        }
                        tagMap.put(ERROR_ID, tagMap.remove("id"));
                        final OsmoseNode node = new OsmoseNode(UUID.fromString(tagMap.get(ERROR_ID)), lat, lon);
                        node.setKeys(tagMap);
                        if (!ds.containsNode(node)) {
                            ds.addPrimitive(node);
                        }
                    }
                }
            }
        }
        return ds;
    }

    private InputStream getFile(Bounds bound) {
        String enabled = buildDownloadErrorList();
        String url = getBaseApi().concat("issues?full=true").concat("&item=").concat(enabled);
        url = url.concat("&bbox=").concat(Double.toString(bound.getMinLon()));
        url = url.concat(",").concat(Double.toString(bound.getMinLat()));
        url = url.concat(",").concat(Double.toString(bound.getMaxLon()));
        url = url.concat(",").concat(Double.toString(bound.getMaxLat()));
        Logging.info("Downloading {0}", url);
        return OpenQACache.getUrl(url);
    }

    @Override
    public OpenQADataSet<UUID, OsmoseNode> getErrors(List<Bounds> bounds, ProgressMonitor monitor) {
        ProgressMonitor subTask = monitor.createSubTaskMonitor(0, false);
        subTask.beginTask(tr("Getting {0} errors", NAME));
        OpenQADataSet<UUID, OsmoseNode> returnDataSet = null;
        String windowTitle = tr("Updating {0} information", NAME);
        if (bounds.size() > 10) {
            subTask.subTask(windowTitle);
            subTask.setTicksCount(bounds.size());
            subTask.setTicks(0);
        } else {
            subTask.indeterminateSubTask(windowTitle);
        }
        for (Bounds bound : bounds) {
            if (subTask.isCanceled())
                break;
            OpenQADataSet<UUID, OsmoseNode> ds = getGeoJsonErrors(bound);
            if (ds != null) {
                if (returnDataSet == null) {
                    returnDataSet = ds;
                } else {
                    returnDataSet.mergeFrom(ds);
                }
            }
        }
        return returnDataSet;
    }

    @Override
    public String buildDownloadErrorList() {
        List<String> enabled = Config.getPref().getList("openqa.osmose-tests", buildDefaultPref());
        return String.join(",", enabled);
    }

    @Override
    public List<String> buildDefaultPref() {
        return new ArrayList<>(getErrors().keySet());
    }

    /**
     * Get all the possible errors
     *
     * @return NavigableMap&lt;String errorNumber, String errorName&gt;
     */
    @Override
    public NavigableMap<String, String> getErrors() {
        return realGetErrors();
    }

    private static NavigableMap<String, String> realGetErrors() {
        if (ERROR_MAP.isEmpty()) {
            TreeMap<String, String> tErrors = new TreeMap<>();

            try (InputStream is = OpenQACache.getUrl(MessageFormat.format(BASE_API + "items", getLocale()));
                    JsonParser parser = Json.createParser(is)) {
                while (parser.hasNext()) {
                    if (parser.next() == Event.START_OBJECT) {
                        JsonObject value = parser.getValue().asJsonObject();
                        JsonArray categories = value.getJsonArray("categories");
                        for (JsonObject category : categories.getValuesAs(JsonObject.class)) {
                            JsonArray items = category.getJsonArray("items");
                            for (JsonObject item : items.getValuesAs(JsonObject.class)) {
                                String errorNumber = item.getJsonNumber("item").toString();
                                String name;
                                if (item.get("title") == JsonValue.NULL) {
                                    name = tr("(name missing)");
                                } else if (item.getJsonObject("title").containsKey(getLocale())) {
                                    name = item.getJsonObject("title").getString(getLocale());
                                } else {
                                    name = item.getJsonObject("title").getString("auto");
                                }
                                tErrors.put(errorNumber, name);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Logging.debug(e);
                tErrors.put("xxxx", "All");
            }
            ERROR_MAP.clear();
            ERROR_MAP.putAll(tErrors);
        }
        return ERROR_MAP;
    }

    /**
     * Get the errors and their categories
     *
     * @return NavigableMap&lt;String category_number, TreeMap&lt;String category,
     *         TreeMap&lt;String errorNumber, String errorName&gt;&gt;&gt;
     */
    public static NavigableMap<String, NavigableMap<String, NavigableMap<String, String>>> getCategories() {
        NavigableMap<String, NavigableMap<String, NavigableMap<String, String>>> categories = new TreeMap<>();
        NavigableMap<String, String> errors = realGetErrors();
        try (InputStream is = OpenQACache.getUrl(MessageFormat.format(BASE_API + "items", getLocale()));
                JsonParser parser = Json.createParser(is)) {
            while (parser.hasNext()) {
                if (parser.next() == Event.START_OBJECT) {
                    JsonArray array = parser.getObject().getJsonArray("categories");
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject info = array.getJsonObject(i);
                        String category = Integer.toString(info.getInt("categ"));
                        final String name;
                        if (!info.containsKey("title")) {
                            name = tr("No name for this category");
                        } else if (info.getJsonObject("title").containsKey(getLocale())) {
                            name = info.getJsonObject("title").getString(getLocale());
                        } else {
                            name = info.getJsonObject("title").getString("auto");
                        }
                        TreeMap<String, String> catErrors = new TreeMap<>();
                        JsonArray items = info.getJsonArray("items");
                        for (int j = 0; j < items.size(); j++) {
                            JsonObject item = items.getJsonObject(j);
                            String nItem = Integer.toString(item.getInt("item"));
                            catErrors.put(nItem, errors.get(nItem));
                        }
                        NavigableMap<String, NavigableMap<String, String>> tMap = new TreeMap<>();
                        tMap.put(name, catErrors);
                        categories.put(category, tMap);
                    }
                }
            }
        } catch (IOException e) {
            Logging.debug(e);
            NavigableMap<String, NavigableMap<String, String>> tMap = new TreeMap<>();
            tMap.put(tr("No categories found"), errors);
            categories.put("-1", tMap);
        }
        return categories;
    }

    /**
     * Get additional information in a separate thread
     *
     * @author Taylor Smock
     */
    private static class AdditionalInformation implements Runnable {
        final OsmoseNode node;

        AdditionalInformation(OsmoseNode node) {
            this.node = node;
        }

        @Override
        public void run() {
            try (InputStream is = OpenQACache.getUrl(getBaseApiReal() + "issue/" + node.get(ERROR_ID));
                    JsonParser parser = Json.createParser(is)) {
                while (parser.hasNext()) {
                    if (parser.next() == Event.START_OBJECT) {
                        JsonObject info = parser.getObject();
                        for (Map.Entry<String, JsonValue> entry : info.entrySet()) {
                            String key = entry.getKey();
                            JsonValue value = entry.getValue();
                            if ("elems".equals(key))
                                continue;// TODO actually deal with it in json format...
                            if (value.getValueType() == ValueType.STRING) {
                                node.put(key, info.getString(key));
                            } else if (!JsonValue.NULL.equals(value)) {
                                node.put(key, value.toString());
                            }
                        }
                    }
                }
                node.put(ADDITIONAL_INFORMATION, "true");
            } catch (IOException e) {
                Logging.debug(e);
            }
            synchronized (this) {
                notifyAll();
            }
        }
    }

    private static void getAdditionalInformation(OsmoseNode node) {
        if (!node.hasKey(ADDITIONAL_INFORMATION) || !"true".equals(node.get(ADDITIONAL_INFORMATION))) {
            AdditionalInformation info = new AdditionalInformation(node);
            info.run();
        }
    }

    @Override
    public boolean cacheAdditionalInformation(OsmoseNode node) {
        getAdditionalInformation(node);
        return true;
    }

    @Override
    public String getNodeToolTip(OsmoseNode node) {
        StringBuilder sb = new StringBuilder("<html>");
        sb.append(tr(NAME)).append(": ").append(getTranslatedText(node.get("title"))).append(" - <a href=")
                .append(getBaseErrorUrl()).append(node.get(ERROR_ID)).append('>').append(node.get(ERROR_ID))
                .append("</a>");

        sb.append("<hr/>");
        String subtitle = node.get("subtitle");
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            sb.append(getTranslatedText(subtitle));
            sb.append("<hr/>");
        }
        String elements = node.get("elems");
        if (elements != null && !elements.trim().isEmpty()) {
            String startText = "Elements: ";
            String htmlText = "".concat(startText);
            String[] element = elements.split("_");
            for (int i = 0; i < element.length; i++) {
                String pOsm = element[i];
                if (pOsm.startsWith("node")) {
                    htmlText = htmlText.concat("node ").concat(pOsm.replace("node", ""));
                } else if (pOsm.startsWith("way")) {
                    htmlText = htmlText.concat("way ").concat(pOsm.replace("way", ""));
                } else if (pOsm.startsWith("relation")) {
                    htmlText = htmlText.concat("relation ").concat(pOsm.replace("relation", ""));
                }

                if (i < element.length - 2) {
                    htmlText = htmlText.concat(", ");
                } else if (i == element.length - 2) {
                    htmlText = htmlText.concat(" and ");
                }
            }
            if (!startText.equals(htmlText)) {
                sb.append(htmlText);
                sb.append("<hr/>");
            }
        }

        String suggestions = node.get("new_elems");
        if (suggestions != null && !suggestions.trim().isEmpty() && !"[]".equals(suggestions)) {
            String htmlText = "Possible additions: ";
            htmlText = htmlText.concat(suggestions); // TODO check if we can parse this with JSON
            sb.append(htmlText);
            sb.append("<hr/>");
        }
        final Collection<OsmPrimitive> primitives = node.hasKey("osm_ids")
                ? getLatestServerPrimitive(parseOsmIds(node.get("osm_ids")))
                : Collections.emptyList();

        sb.append("Issue last updated on ".concat(node.get("update")));

        if (!primitives.isEmpty()) {
            sb.append("<br/>").append(tr("Last modified on "));
            sb.append(primitives.stream().map(OsmPrimitive::getInstant).distinct().map(Date::from)
                    .map(date -> DateUtils.formatDateTime(date, DateFormat.DEFAULT, DateFormat.DEFAULT))
                    .collect(Collectors.joining(", ")));
        }
        if (node.hasKey("username")) {
            sb.append("<br/>").append(tr("Last modified by ")).append(getUserName(node.get("username")));
        } else if (node.hasKey("usernames")) {
            final List<String> users;
            try (JsonReader reader = Json
                    .createReader(new ByteArrayInputStream(node.get("usernames").getBytes(StandardCharsets.UTF_8)))) {
                JsonArray jsonArray = reader.readArray();
                users = new ArrayList<>(jsonArray.size());
                for (JsonString user : jsonArray.getValuesAs(JsonString.class)) {
                    if (!Utils.isBlank(user.getString())) {
                        users.add(getUserName(user.getString()));
                    }
                }
            }
            if (users.isEmpty()) {
                users.addAll(primitives.stream().map(OsmPrimitive::getUser).distinct().filter(User::isOsmUser)
                        .map(User::getName).collect(Collectors.toList()));
            }
            sb.append("<br/>").append(tr("Last modified by ")).append(String.join(", ", users));
        }
        sb.append("</html>");
        String result = sb.toString();
        Logging.debug(result);
        return result;
    }

    /**
     * Get the latest *downloaded, non-modified* server primitive.
     *
     * @param primitiveIds The primitive ids to search for
     * @return The latest primitives
     */
    private static Collection<OsmPrimitive> getLatestServerPrimitive(final Collection<PrimitiveId> primitiveIds) {
        final Collection<DataSet> dataSets = MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class)
                .stream().filter(layer -> layer.isDownloadable() && layer.isUploadable()).map(OsmDataLayer::getDataSet)
                .collect(Collectors.toSet());
        return primitiveIds.stream()
                .map(primitive -> dataSets.stream().map(dataset -> dataset.getPrimitiveById(primitive))
                        .filter(Objects::nonNull).filter(p -> !p.isModified())
                        .max(Comparator.comparing(OsmPrimitive::getVersion)).orElse(null))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Convert a json into OSM ids
     *
     * @param json The json to convert
     * @return The osm ids
     */
    private static Collection<PrimitiveId> parseOsmIds(final String json) {
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            final JsonObject object = reader.readObject();
            if (object.containsKey("relations")) {
                final JsonArray relations = object.getJsonArray("relations");
                return relations.stream().filter(JsonNumber.class::isInstance).map(JsonNumber.class::cast)
                        .map(JsonNumber::longValue).map(i -> new SimplePrimitiveId(i, OsmPrimitiveType.RELATION))
                        .collect(Collectors.toList());
            }
            if (object.containsKey("ways")) {
                final JsonArray ways = object.getJsonArray("ways");
                return ways.stream().filter(JsonNumber.class::isInstance).map(JsonNumber.class::cast)
                        .map(JsonNumber::longValue).map(i -> new SimplePrimitiveId(i, OsmPrimitiveType.WAY))
                        .collect(Collectors.toList());
            }
            if (object.containsKey("nodes")) {
                final JsonArray nodes = object.getJsonArray("nodes");
                return nodes.stream().filter(JsonNumber.class::isInstance).map(JsonNumber.class::cast)
                        .map(JsonNumber::longValue).map(i -> new SimplePrimitiveId(i, OsmPrimitiveType.NODE))
                        .collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    @Override
    public List<JButton> getActions(OsmoseNode node) {
        final String apiUrl = getBaseApi() + "issue/" + node.get(ERROR_ID) + "/";

        JButton fixed = new JButton();
        JButton falsePositive = new JButton();

        String sTrue = "true";
        String sFalse = "false";
        String actionTaken = "actionTaken";

        fixed.setAction(new AbstractAction() {
            private static final long serialVersionUID = 3020815442282939509L;

            @Override
            public void actionPerformed(ActionEvent e) {
                new SendInformation(apiUrl.concat("done")).run();
                node.put(actionTaken, sTrue);
                fixed.setEnabled(false);
                falsePositive.setEnabled(true);
                node.put("item", "fixed");
                redrawErrorLayers(tr(LAYER_NAME));
                addChangeSetTag("osmose", node.get(ERROR_ID));
            }
        });

        falsePositive.setAction(new AbstractAction() {
            private static final long serialVersionUID = -5379628459847724662L;

            @Override
            public void actionPerformed(ActionEvent e) {
                new SendInformation(apiUrl.concat(sFalse)).run();
                node.put(actionTaken, sFalse);
                fixed.setEnabled(true);
                falsePositive.setEnabled(false);
                node.put("item", "falsePositive");
                redrawErrorLayers(tr(LAYER_NAME));
            }
        });

        fixed.setText(tr("Fixed"));
        falsePositive.setText(tr("False Positive"));
        if (node.hasKey(actionTaken)) {
            if (sTrue.equals(node.get(actionTaken))) {
                fixed.setEnabled(false);
                falsePositive.setEnabled(true);
            } else if (sFalse.equals(node.get(actionTaken))) {
                fixed.setEnabled(true);
                falsePositive.setEnabled(false);
            }
        }

        ArrayList<JButton> buttons = new ArrayList<>();
        buttons.add(fixed);
        buttons.add(falsePositive);
        return buttons;
    }

    @Override
    public OsmoseDataSet createNewDataSet() {
        return new OsmoseDataSet();
    }

    @Override
    public ImageIcon getIcon(String errorValue, ImageSizes size) {
        if (ImageSizes.LARGEICON == size) {
            return LARGE_ICON_CACHE.get(errorValue, () -> this.realGetIcon(errorValue, size));
        }
        return realGetIcon(errorValue, size);
    }

    private ImageIcon realGetIcon(String errorValue, ImageSizes size) {
        try {
            ImageIcon icon;
            if ("fixed".equals(errorValue)) {
                icon = ImageProvider.get("dialogs/notes", "note_closed", size);
            } else if ("falsePositive".equals(errorValue)) {
                icon = ImageProvider.get("dialogs/notes", "note_comment", size);
            } else {
                icon = new ImageIcon(ImageIO.read(OpenQACache.getUrl(String.format(getBaseImg(), errorValue))));
            }
            return icon;
        } catch (IOException e) {
            Logging.error(e);
            return super.getIcon("-1", size);
        }
    }

    @Override
    public String getLayerName() {
        return LAYER_NAME;
    }

    @Override
    public String getError(OsmoseNode node) {
        return node.get("item");
    }

    @Override
    public String getBaseApi() {
        return getBaseApiReal();
    }

    private static String getBaseApiReal() {
        return MessageFormat.format(BASE_API, getLocale());
    }

    @Override
    public String getBaseImg() {
        return MessageFormat.format(BASE_IMG, getLocale());
    }

    @Override
    public String getBaseErrorUrl() {
        return MessageFormat.format(BASE_ERROR_URL, getLocale());
    }

    /**
     * Get the current locale
     *
     * @return The locale to use
     */
    private static String getLocale() {
        String[] valid = new String[] { "eu", "pt_BR", "uk", "zh_TW", "hu", "en", "ro", "ru", "de", "nb", "lt", "pl",
                "cs", "it", "es", "fa", "fi", "zh_CN", "ca", "el", "ja", "nl", "fr", "sv", "gl", "pt" };
        if (Stream.of(valid).anyMatch(s -> s.equalsIgnoreCase(LanguageInfo.getJOSMLocaleCode()))) {
            return LanguageInfo.getJOSMLocaleCode();
        }
        return "en";
    }

    /**
     * Get the translated text, if available
     *
     * @param json The json string to parse
     * @return The translated text
     */
    private static String getTranslatedText(final String json) {
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))) {
            final JsonObject object = reader.readObject();
            final String locale = getLocale();
            if (object.containsKey(locale)) {
                return object.getString(locale);
            }
            if (object.containsKey("auto")) {
                return object.getString("auto");
            }
        } catch (JsonParsingException exception) {
            throw new JosmRuntimeException(json, exception);
        }
        return json;
    }
}
