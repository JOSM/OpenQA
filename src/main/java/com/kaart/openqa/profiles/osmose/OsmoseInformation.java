/**
 *
 */
package com.kaart.openqa.profiles.osmose;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;

import com.kaart.openqa.CachedFile;
import com.kaart.openqa.profiles.GenericInformation;

/**
 * @author Taylor Smock
 *
 */
public class OsmoseInformation extends GenericInformation {
    public static final String NAME = "Osmose";

    public static final String LAYER_NAME = "Osmose Errors";
    public static final String BASE_API = "http://osmose.openstreetmap.fr/en/api/0.2/";
    public static final String BASE_IMG = "http://osmose.openstreetmap.fr/en/images/markers/marker-b-%s.png";
    public static final String BASE_ERROR_URL = "http://osmose.openstreetmap.fr/en/error/";

    private static final String ADDITIONAL_INFORMATION = "ADDITIONAL_INFORMATION";

    protected static SortedMap<String, String> formats = new TreeMap<>();

    public OsmoseInformation(String cacheDir) {
        super(cacheDir);
    }

    @Override
    public String getName() {
        return NAME;
    }

    private DataSet getGeoJsonErrors(Bounds bound) {
        CachedFile cache = getFile(bound);
        DataSet ds = new DataSet();
        try (JsonParser json = Json.createParser(cache.getInputStream())) {
            ArrayList<String> fields = new ArrayList<>();
            while (json.hasNext()) {
                if (json.next() == Event.START_OBJECT) {
                    JsonObject jobject = json.getObject();
                    if (fields.isEmpty()) {
                        JsonArray tArray = jobject.getJsonArray("description");
                        for (JsonValue value : tArray) {
                            if (value.getValueType() == ValueType.STRING) {
                                fields.add(value.toString());
                            }
                        }
                    }
                    JsonArray errors = jobject.getJsonArray("errors");
                    for (int i = 0; i < errors.size(); i++) {
                        JsonArray error = errors.getJsonArray(i);
                        Node node = new Node();
                        double lat = Double.MAX_VALUE;
                        double lon = Double.MAX_VALUE;
                        for (int j = 0; j < fields.size(); j++) {
                            String field = fields.get(j).replace("\"", "");
                            String errorValue = error.getString(j);
                            if (field.equals("lat")) {
                                lat = Double.parseDouble(errorValue);
                            } else if (field.equals("lon")) {
                                lon = Double.parseDouble(errorValue);
                            } else {
                                node.put(field, errorValue);
                            }
                        }
                        node.setCoor(new LatLon(lat, lon));

                        if (!node.isOutSideWorld()) {
                            ds.addPrimitive(node);
                        }
                    }
                }
            }
            for (OsmPrimitive osmPrimitive : ds.allPrimitives()) {
                osmPrimitive.setOsmId(Long.parseLong(osmPrimitive.get(ERROR_ID)), 1);
            }
        } catch (IOException e) {
            Logging.error(e);
        }
        cache.close();
        return ds;
    }

    private CachedFile getFile(Bounds bound) {
        String type = "json";
        String enabled = buildDownloadErrorList();
        String url = BASE_API.concat("errors?full=true").concat("&item=").concat(enabled);
        url = url.concat("&bbox=").concat(Double.toString(bound.getMinLon()));
        url = url.concat(",").concat(Double.toString(bound.getMinLat()));
        url = url.concat(",").concat(Double.toString(bound.getMaxLon()));
        url = url.concat(",").concat(Double.toString(bound.getMaxLat()));
        CachedFile cache;
        try {
            cache = GenericInformation.getFile(url, formats.get(type),
                    new File(cacheDir, DATA_SUB_DIR).getCanonicalPath());
        } catch (IOException e) {
            Logging.error(e);
            cache = GenericInformation.getFile(url, formats.get(type), cacheDir);
        }
        cache.setDeleteOnExit(true);
        return cache;
    }

    @Override
    public DataSet getErrors(List<Bounds> bounds, ProgressMonitor monitor) {
        ProgressMonitor subTask = monitor.createSubTaskMonitor(0, false);
        subTask.beginTask(tr("Getting {0} errors", NAME));
        DataSet returnDataSet = null;
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
            DataSet ds = getGeoJsonErrors(bound);
            if (ds != null) {
                if (returnDataSet == null) {
                    returnDataSet = ds;
                } else {
                    returnDataSet.mergeFrom(ds, subTask.createSubTaskMonitor(1, false));
                }
            }
        }
        return returnDataSet;
    }

    @Override
    public String buildDownloadErrorList() {
        StringBuilder list = new StringBuilder();
        List<String> enabled = Config.getPref().getList("openqa.osmose-tests", buildDefaultPref());
        for (int i = 0; i < enabled.size(); i++) {
            list.append(enabled.get(i));
            if (i < enabled.size() - 1) {
                list.append(",");
            }
        }
        return list.toString();
    }

    @Override
    public ArrayList<String> buildDefaultPref() {
        ArrayList<String> rArray = new ArrayList<>();
        for (String error : getErrors(cacheDir).keySet()) {
            rArray.add(error);
        }
        return rArray;
    }

    /**
     * Get all the possible errors
     *
     * @param cacheDir Directory to store error defaultDownloadTypes file
     * @return SortedMap&lt;String errorNumber, String errorName&gt;
     */
    public static SortedMap<String, String> getErrors(String cacheDir) {
        TreeMap<String, String> tErrors = new TreeMap<>();
        try (CachedFile cache = new CachedFile(BASE_API + "meta/items")) {
            cache.setDestDir(cacheDir);
            JsonParser parser = Json.createParser(cache.getInputStream());
            while (parser.hasNext()) {
                if (parser.next() == Event.START_OBJECT) {
                    JsonValue value = parser.getValue();
                    if (JsonValue.ValueType.OBJECT == value.getValueType()) {
                        JsonArray array = value.asJsonObject().getJsonArray("items");
                        for (int i = 0; i < array.size(); i++) {
                            JsonArray info = array.getJsonArray(i);
                            String errorNumber = info.getJsonNumber(0).toString();
                            String name;
                            if (info.get(1) == JsonValue.NULL) {
                                name = tr("(name missing)");
                            } else {
                                name = info.getJsonObject(1).getString("en");
                            }
                            tErrors.put(errorNumber, name);
                        }
                    }
                }
            }
            parser.close();
        } catch (IOException e) {
            Logging.debug(e.getMessage());
            tErrors.put("xxxx", "All");
        }
        return tErrors;
    }

    /**
     * Get the errors and their categories
     *
     * @param cacheDir directory to cache information in
     * @return SortedMap&lt;String category_number, TreeMap&lt;String category,
     *         TreeMap&lt;String errorNumber, String errorName&gt;&gt;&gt;
     */
    public static SortedMap<String, SortedMap<String, SortedMap<String, String>>> getCategories(String cacheDir) {
        SortedMap<String, SortedMap<String, SortedMap<String, String>>> categories = new TreeMap<>();
        SortedMap<String, String> errors = getErrors(cacheDir);
        JsonParser parser = null;
        try (CachedFile cache = new CachedFile(BASE_API + "meta/categories")) {
            cache.setDestDir(cacheDir);
            parser = Json.createParser(cache.getInputStream());
            while (parser.hasNext()) {
                if (parser.next() == Event.START_OBJECT) {
                    JsonArray array = parser.getObject().getJsonArray("categories");
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject info = array.getJsonObject(i);
                        String category = Integer.toString(info.getInt("categ"));
                        String name = info.getJsonObject("menu_lang").getString("en");
                        TreeMap<String, String> catErrors = new TreeMap<>();
                        JsonArray items = info.getJsonArray("item");
                        for (int j = 0; j < items.size(); j++) {
                            JsonObject item = items.getJsonObject(j);
                            String nItem = Integer.toString(item.getInt("item"));
                            catErrors.put(nItem, errors.get(nItem));
                        }
                        SortedMap<String, SortedMap<String, String>> tMap = new TreeMap<>();
                        tMap.put(name, catErrors);
                        categories.put(category, tMap);
                    }
                }
            }
        } catch (IOException e) {
            Logging.debug(e.getMessage());
            SortedMap<String, SortedMap<String, String>> tMap = new TreeMap<>();
            tMap.put(tr("No categories found"), errors);
            categories.put("-1", tMap);
        } finally {
            if (parser != null)
                parser.close();
        }
        return categories;
    }

    /**
     * Get additional information in a separate thread
     *
     * @author Taylor Smock
     */
    private static class AdditionalInformation implements Runnable {
        Node node;

        AdditionalInformation(Node node) {
            this.node = node;
        }

        @Override
        public void run() {
            JsonParser parser = null;
            try {
                URL url = new URL(BASE_API + "error/" + node.get(ERROR_ID));
                Logging.info(url.toExternalForm());
                parser = Json.createParser(url.openStream());
                while (parser.hasNext()) {
                    if (parser.next() == Event.START_OBJECT) {
                        JsonObject info = parser.getObject();
                        for (Entry<String, JsonValue> entry : info.entrySet()) {
                            String key = entry.getKey();
                            JsonValue value = entry.getValue();
                            if ("elems".equals(key))
                                continue;// TODO actually deal with it in json format...
                            if (value.getValueType() == ValueType.STRING) {
                                node.put(key, info.getString(key));
                            } else {
                                node.put(key, value.toString());
                            }
                        }
                    }
                }
                node.put(ADDITIONAL_INFORMATION, "true");
            } catch (IOException e) {
                Logging.debug(e.getMessage());
            } finally {
                if (parser != null)
                    parser.close();
            }
            synchronized (this) {
                notifyAll();
            }
        }
    }

    private static void getAdditionalInformation(Node node) {
        if (!node.hasKey(ADDITIONAL_INFORMATION) || !node.get(ADDITIONAL_INFORMATION).equals("true")) {
            AdditionalInformation info = new AdditionalInformation(node);
            info.run();
        }
    }

    @Override
    public boolean cacheAdditionalInformation(Node node) {
        getAdditionalInformation(node);
        return true;
    }

    @Override
    public String getNodeToolTip(Node node) {
        StringBuilder sb = new StringBuilder("<html>");
        sb.append(tr(NAME)).append(": ").append(node.get("title")).append(" - <a href=")
                .append(BASE_ERROR_URL + node.get(ERROR_ID)).append(">").append(node.get(ERROR_ID)).append("</a>");

        sb.append("<hr/>");
        String subtitle = node.get("subtitle");
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            sb.append(node.get("subtitle"));
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
        if (suggestions != null && !suggestions.trim().isEmpty() && !suggestions.equals("[]")) {
            String htmlText = "Possible additions: ";
            htmlText = htmlText.concat(suggestions); // TODO check if we can parse this with JSON
            sb.append(htmlText);
            sb.append("<hr/>");
        }

        sb.append("Last updated on ".concat(node.get("update")));

        sb.append("<br/> by ".concat(getUserName(node.get("username"))));
        sb.append("</html>");
        String result = sb.toString();
        Logging.debug(result);
        return result;
    }

    @Override
    public List<JButton> getActions(Node node) {
        final String apiUrl = BASE_API + "error/" + node.get(ERROR_ID) + "/";

        JButton fixed = new JButton();
        JButton falsePositive = new JButton();

        String sTrue = "true";
        String sFalse = "false";
        String actionTaken = "actionTaken";

        fixed.setAction(new AbstractAction() {
            private static final long serialVersionUID = 3020815442282939509L;

            @Override
            public void actionPerformed(ActionEvent e) {
                new SendInformation(apiUrl.concat("done"), cacheDir).run();
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
                new SendInformation(apiUrl.concat(sFalse), cacheDir).run();
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
    public ImageIcon getIcon(String errorValue, ImageSizes size) {
        try {
            ImageIcon icon;
            if ("fixed".equals(errorValue)) {
                icon = ImageProvider.get("dialogs/notes", "note_closed", size);
            } else if ("falsePositive".equals(errorValue)) {
                icon = ImageProvider.get("dialogs/notes", "note_comment", size);
            } else {
                CachedFile image = GenericInformation.getFile(String.format(BASE_IMG, errorValue), "image/*",
                        new File(cacheDir, IMG_SUB_DIR).getCanonicalPath());
                image.setMaxAge(30 * (long) 86400);
                image.getFile();
                icon = ImageProvider.get(image.getFile().getAbsolutePath());
                icon = new ImageIcon(ImageProvider.createBoundedImage(icon.getImage(), size.getAdjustedHeight()));
                image.close();
            }
            return icon;
        } catch (NullPointerException | IOException e) {
            return super.getIcon("-1", size);
        }
    }

    @Override
    public String getLayerName() {
        return LAYER_NAME;
    }

    @Override
    public String getError(Node node) {
        return node.get("item");
    }

    @Override
    public String getCacheDir() {
        return this.cacheDir;
    }

    @Override
    public String getBaseApi() {
        return BASE_API;
    }

    @Override
    public String getBaseImg() {
        return BASE_IMG;
    }

    @Override
    public String getBaseErrorUrl() {
        return BASE_ERROR_URL;
    }

    @Override
    public SortedMap<String, String> getErrors() {
        return getErrors(getCacheDir());
    }
}
