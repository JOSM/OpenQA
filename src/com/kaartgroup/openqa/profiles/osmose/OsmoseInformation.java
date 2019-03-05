/**
 *
 */
package com.kaartgroup.openqa.profiles.osmose;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;

import com.kaartgroup.openqa.ErrorLayer;
import com.kaartgroup.openqa.profiles.GenericInformation;

/**
 * @author Taylor Smock
 *
 */
public class OsmoseInformation extends GenericInformation {
	public static final String LAYER_NAME = "Osmose Errors";
	public static String baseApi = "http://osmose.openstreetmap.fr/api/0.2/";
	public static String baseImg = "http://osmose.openstreetmap.fr/en/images/markers/marker-b-%s.png";
	public static String baseErrorUrl = "http://osmose.openstreetmap.fr/en/error/";

	public static TreeMap<String, String> formats = new TreeMap<>();

	public OsmoseInformation(String CACHE_DIR) {
		this.CACHE_DIR = CACHE_DIR;
	}

	private Layer getGeoJsonErrors(Bounds bound) {
		CachedFile cache = getFile(bound);
		ErrorLayer layer = new ErrorLayer(this);
		try {
			JsonParser json = Json.createParser(cache.getInputStream());
			ArrayList<String> fields = new ArrayList<>();
			DataSet ds = new DataSet();
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

						if (!node.getCoor().isOutSideWorld()){
							ds.addPrimitive(node);
						}
					}
				}
			}
			layer.addNotes(ds);
		} catch (IOException e) {
			Logging.debug(e.getMessage());
			e.printStackTrace();
		}
		cache.close();
		return layer;
	}
	private CachedFile getFile(Bounds bound) {
		String type = "json";
		String enabled = buildDownloadErrorList();
		String url = baseApi + "errors?full=true" + "&item=" + enabled;
		url += "&bbox=" + Double.toString(bound.getMinLon());
		url += "," + Double.toString(bound.getMinLat());
		url += "," + Double.toString(bound.getMaxLon());
		url += "," + Double.toString(bound.getMaxLat());
		CachedFile cache = GenericInformation.getFile(url, formats.get(type), CACHE_DIR);
		return cache;
	}

	@Override
	public Layer getErrors(List<Bounds> bounds) {
		Layer mlayer = null;
		for (Bounds bound : bounds) {
			Layer layer = getGeoJsonErrors(bound);
			if (layer != null) {
				if (mlayer == null) {
					mlayer = layer;
				} else {
					mlayer.mergeFrom(layer);
				}
			}
		}
		return mlayer;
	}

	@Override
	public String buildDownloadErrorList() {
		String list = "";
		List<String> enabled = Config.getPref().getList("openqa.osmose-tests", buildDefaultPref());
		for (int i = 0; i < enabled.size(); i++) {
			list += enabled.get(i);
			if (i < enabled.size() - 1) {
				list += ",";
			}
		}
		return list;
	}

	@Override
	public ArrayList<String> buildDefaultPref() {
		ArrayList<String> rArray = new ArrayList<>();
		rArray.add("xxxx");
		return rArray;
	}

	public static TreeMap<String, String> getErrors(String CACHE_DIR) {
		TreeMap<String, String> tErrors = new TreeMap<>();
		try {
			CachedFile cache = new CachedFile(baseApi + "meta/items");
			cache.setDestDir(CACHE_DIR);
			JsonParser parser = Json.createParser(cache.getInputStream());
			while (parser.hasNext()) {
				if (parser.next() == Event.START_OBJECT) {
					JsonArray array = parser.getObject().getJsonArray("items");
					for (int i = 0; i < array.size(); i++) {
						JsonArray info = array.getJsonArray(i);
						String errorNumber = info.getJsonNumber(0).toString();
						String name;
						if (info.get(1) == JsonValue.NULL) {
							name = tr("(name missing)");
						}
						else {
							name = info.getJsonObject(1).getString("en");
						}
						tErrors.put(errorNumber, name);
					}
				}
			}
			cache.close();
			parser.close();
		} catch (IOException e) {
			Logging.debug(e.getMessage());
			tErrors.put("xxxx", "All");
		}
		return tErrors;
	}

	public static TreeMap<String, TreeMap<String, TreeMap<String, String>>> getCategories(String CACHE_DIR) {
		TreeMap<String, TreeMap<String, TreeMap<String, String>>> categories = new TreeMap<>();
		TreeMap<String, String> errors = getErrors(CACHE_DIR);
		try {
			CachedFile cache = new CachedFile(baseApi + "meta/categories");
			cache.setDestDir(CACHE_DIR);
			JsonParser parser;
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
						TreeMap<String, TreeMap<String, String>> tMap = new TreeMap<>();
						tMap.put(name, catErrors);
						categories.put(category, tMap);
					}
				}
			}
			cache.close();
			parser.close();
		} catch (IOException e) {
			Logging.debug(e.getMessage());
			TreeMap<String, TreeMap<String, String>> tMap = new TreeMap<>();
			tMap.put(tr("No categories found"), errors);
			categories.put("-1", tMap);
		}
		return categories;
	}

	private static Node getAdditionalInformation(Node node) {
		if (!node.hasKey("additionalInformation") || !node.get("additionalInformation").equals("true")) {
			try {
				URL url = new URL(baseApi + "error/" + node.get("error_id"));
				JsonParser parser = Json.createParser(url.openStream());
				while (parser.hasNext()) {
					if (parser.next() == Event.START_OBJECT) {
						node.setModified(true);
						JsonObject info = parser.getObject();
						for (String key : info.keySet()) {
							Logging.debug("Looking at {0}", key);
							if (key.equals("elems")) continue;
							if (info.get(key).getValueType() == ValueType.STRING) {
								Logging.debug("{0} = {1}", key, info.getString(key));
								node.put(key, info.getString(key));
							}
						}
					}
				}
				node.put("additionalInformation", "true");
			} catch (IOException e) {
				Logging.debug(e.getMessage());
			}
		}
		return node;
	}

	@Override
	public String getNodeToolTip(Node node) {
		Logging.setLogLevel(Logging.LEVEL_DEBUG);
		node = getAdditionalInformation(node);
		Logging.debug("Current keys in tooltip");
		for (String key : node.getKeys().keySet()) {
			Logging.debug("OpenQA: {0}={1}", key, node.get(key));
		}
		StringBuilder sb = new StringBuilder("<html>");
		sb.append(tr("Osmose"))
		  .append(": ").append(node.get("title"))
		  .append(" - <a href=")
		  .append(baseErrorUrl + node.get("error_id"))
		  .append(">").append(node.get("error_id"))
		  .append("</a>");

		sb.append("<hr/>");
		sb.append(node.get("subtitle"));
		sb.append("<hr/>");
		String elements = node.get("elems");
		if (elements != null && !elements.trim().isEmpty()) {
			String htmlText = "Elements: ";
			String[] element = elements.split("_");
			for (int i = 0; i < element.length; i++) {
				String pOsm = element[i];
				if (pOsm.startsWith("node")) {
					htmlText += "node " + pOsm.replace("node", "");
				} else if (pOsm.startsWith("way")) {
					htmlText += "way " + pOsm.replace("way", "");
				} else if (pOsm.startsWith("relation")) {
					htmlText += "relation " + pOsm.replace("relation", "");
				}

				if (i < element.length - 2) {
					htmlText += ", ";
				} else if (i == element.length - 2) {
					htmlText += " and ";
				}
			}
			sb.append(htmlText);
			sb.append("<hr/>");
		}

		sb.append("Last updated on " + node.get("update"));

		sb.append("<br/> by " + getUserName(node.get("username")));
		sb.append("<hr/>");
		sb.append("<a href=");
		String apiUrl = baseApi + "meta/error/" + node.get("error_id") + "/";
		sb.append(apiUrl + "done");
		sb.append(">Fixed</a> <a href=");
		sb.append(apiUrl + "false");
		sb.append(">False Positive</a>");
		sb.append("</html>");
		String result = sb.toString();
		Logging.debug(result);
		return result;
	}

	public ImageIcon getIcon(String errorValue, ImageSizes size) {
		try {
			CachedFile image = GenericInformation.getFile(String.format(baseImg, errorValue), "image/*", CACHE_DIR);
			image.setMaxAge(30 * 86400);
			image.getFile();
			ImageIcon icon = ImageProvider.get(image.getFile().getAbsolutePath(), size);
			image.close();
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
}
