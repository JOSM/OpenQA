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
	public static final String LAYER_NAME = "Osmose Errors";
	public static String baseApi = "http://osmose.openstreetmap.fr/api/0.2/";
	public static String baseImg = "http://osmose.openstreetmap.fr/en/images/markers/marker-b-%s.png";
	public static String baseErrorUrl = "http://osmose.openstreetmap.fr/en/error/";

	public static TreeMap<String, String> formats = new TreeMap<>();

	public OsmoseInformation(String CACHE_DIR) {
		super(CACHE_DIR);
	}

	public String getName() {
		return "Osmose";
	}

	private DataSet getGeoJsonErrors(Bounds bound) {
		CachedFile cache = getFile(bound);
		DataSet ds = new DataSet();
		try {
			JsonParser json = Json.createParser(cache.getInputStream());
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

						if (!node.getCoor().isOutSideWorld()){
							ds.addPrimitive(node);
						}
					}
				}
			}
			for (OsmPrimitive osmPrimitive : ds.allPrimitives()) {
				osmPrimitive.setOsmId(Long.parseLong(osmPrimitive.get("error_id")), 1);
			}
		} catch (IOException e) {
			Logging.debug(e.getMessage());
			e.printStackTrace();
		}
		cache.close();
		return ds;
	}

	private CachedFile getFile(Bounds bound) {
		String type = "json";
		String enabled = buildDownloadErrorList();
		String url = baseApi.concat("errors?full=true").concat("&item=").concat(enabled);
		url = url.concat("&bbox=").concat(Double.toString(bound.getMinLon()));
		url = url.concat(",").concat(Double.toString(bound.getMinLat()));
		url = url.concat(",").concat(Double.toString(bound.getMaxLon()));
		url = url.concat(",").concat(Double.toString(bound.getMaxLat()));
		CachedFile cache;
		try {
			cache = GenericInformation.getFile(url, formats.get(type), new File(CACHE_DIR, DATA_SUB_DIR).getCanonicalPath());
		} catch (IOException e) {
			Logging.debug(e.getMessage());
			e.printStackTrace();
			cache = GenericInformation.getFile(url, formats.get(type), CACHE_DIR);
		}
		cache.setDeleteOnExit(true);
		return cache;
	}

	@Override
	public DataSet getErrors(List<Bounds> bounds, ProgressMonitor monitor) {
		monitor.subTask(tr("Getting {0} errors", "Osmose"));
		DataSet returnDataSet = null;
		String windowTitle = tr("Updating {0} information", "Osmose");
		if (bounds.size() > 10) {
			monitor.subTask(windowTitle);
			monitor.setTicksCount(bounds.size());
			monitor.setTicks(0);
		} else {
			monitor.indeterminateSubTask(windowTitle);
		}
		for (Bounds bound : bounds) {
			if (monitor.isCanceled()) break;
			monitor.worked(1);
			DataSet ds = getGeoJsonErrors(bound);
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
		for (String error : getErrors(CACHE_DIR).keySet()) {
			rArray.add(error);
		}
		return rArray;
	}

	/**
	 * Get all the possible errors
	 * @param CACHE_DIR Directory to store error list file
	 * @return TreeMap&lt;String errorNumber, String errorName&gt;
	 */
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

	/**
	 * Get the errors and their categories
	 * @param CACHE_DIR directory to cache information in
	 * @return TreeMap&lt;String category_number, TreeMap&lt;String category, TreeMap&lt;String errorNumber, String errorName&gt;&gt;&gt;
	 */
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

	/**
	 * Get additional information in a separate thread
	 * @author Taylor Smock
	 */
	private static class AdditionalInformation implements Runnable {
		Node node;
		AdditionalInformation(Node node) {
			this.node = node;
		}

		@Override
		public void run() {
			try {
				URL url = new URL(baseApi + "error/" + node.get("error_id"));
				JsonParser parser = Json.createParser(url.openStream());
				while (parser.hasNext()) {
					if (parser.next() == Event.START_OBJECT) {
						JsonObject info = parser.getObject();
						for (String key : info.keySet()) {
							if ("elems".equals(key)) continue;// TODO actually deal with it in json format...
							if (info.get(key).getValueType() == ValueType.STRING) {
								node.put(key, info.getString(key));
							} else {
								node.put(key, info.get(key).toString());
							}
						}
					}
				}
				node.put("additionalInformation", "true");
			} catch (IOException e) {
				Logging.debug(e.getMessage());
			}
			synchronized (this) {
				notifyAll();
			}
		}
	}

	private static void getAdditionalInformation(Node node) {
		if (!node.hasKey("additionalInformation") || !node.get("additionalInformation").equals("true")) {
			AdditionalInformation info = new AdditionalInformation(node);
			info.run();
		}
	}

	@Override
	public String getNodeToolTip(Node node) {
		getAdditionalInformation(node);
		StringBuilder sb = new StringBuilder("<html>");
		sb.append(tr("Osmose"))
		  .append(": ").append(node.get("title"))
		  .append(" - <a href=")
		  .append(baseErrorUrl + node.get("error_id"))
		  .append(">").append(node.get("error_id"))
		  .append("</a>");

		sb.append("<hr/>");
		String subtitle = node.get("subtitle");
		if (subtitle != null && !subtitle.isBlank()) {
			sb.append(node.get("subtitle"));
			sb.append("<hr/>");
		}
		String elements = node.get("elems");
		if (elements != null && !elements.isBlank()) {
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
		if (suggestions != null && !suggestions.trim().isEmpty() && !suggestions.equals("[]") ) {
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
		final String apiUrl = baseApi + "error/" + node.get("error_id") + "/";

		JButton fixed = new JButton();
		JButton falsePositive = new JButton();
		fixed.setAction(new AbstractAction() {
			private static final long serialVersionUID = 3020815442282939509L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new SendInformation(apiUrl.concat("done"), CACHE_DIR).run();
				node.put("actionTaken", "true");
				fixed.setEnabled(false);
				falsePositive.setEnabled(true);
				node.put("item", "fixed");
				redrawErrorLayers(tr(LAYER_NAME));
				addChangeSetTag("osmose", node.get("error_id"));
			}
		});

		falsePositive.setAction(new AbstractAction() {
			private static final long serialVersionUID = -5379628459847724662L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new SendInformation(apiUrl.concat("false"), CACHE_DIR).run();
				node.put("actionTaken", "false");
				fixed.setEnabled(true);
				falsePositive.setEnabled(false);
				node.put("item", "falsePositive");
				redrawErrorLayers(tr(LAYER_NAME));
			}
		});

		fixed.setText(tr("Fixed"));
		falsePositive.setText(tr("False Positive"));
		if (node.hasKey("actionTaken")) {
			if ("true".equals(node.get("actionTaken"))) {
				fixed.setEnabled(false);
				falsePositive.setEnabled(true);
			} else if ("false".equals(node.get("actionTaken"))) {
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
				CachedFile image = GenericInformation.getFile(String.format(baseImg, errorValue), "image/*", new File(CACHE_DIR, IMG_SUB_DIR).getCanonicalPath());
				image.setMaxAge(30 * 86400);
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
		return this.CACHE_DIR;
	}
}
