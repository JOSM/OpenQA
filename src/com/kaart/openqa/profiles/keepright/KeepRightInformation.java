/**
 *
 */
package com.kaart.openqa.profiles.keepright;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;

import com.kaart.openqa.CachedFile;
import com.kaart.openqa.GeoJsonReader;
import com.kaart.openqa.OpenQA;
import com.kaart.openqa.profiles.GenericInformation;

/**
 * @author Taylor Smock
 *
 */
public class KeepRightInformation extends GenericInformation {
	public static final String LAYER_NAME = "Keep Right Errors";
	public static String baseApi = "https://www.keepright.at/export.php?";
	public static String baseImg = "https://www.keepright.at/img/%s.png";
	public static String baseErrorUrl = "https://www.keepright.at/report_map.php?schema=%s&error=%s";

	public static String commentUrl = "https://www.keepright.at/comment.php?st=%s&co=%s&schema=%s&id=%s";
	public static String FIXED = "ignore_t";
	public static String FALSE_POSITIVE = "ignore";

	public static TreeMap<String, String> formats = new TreeMap<>();
	public static TreeMap<String, String> errors = new TreeMap<>();
	static {
		errors.put("0", tr("default"));
		errors.put("20", tr("multiple nodes on the same spot"));
		errors.put("30", tr("non-closed areas"));
		errors.put("40", tr("dead-ended one-ways"));
		errors.put("50", tr("almost-junctions"));
		errors.put("60", tr("deprecated tags"));
		errors.put("70", tr("missing tags"));
		errors.put("90", tr("motorways without ref"));
		errors.put("100", tr("places of worship without religion"));
		errors.put("110", tr("point of interest without name"));
		errors.put("120", tr("ways without nodes"));
		errors.put("130", tr("floating islands"));
		errors.put("150", tr("railway crossings without tag"));
		errors.put("160", tr("wrongly used railway crossing tag"));
		errors.put("170", tr("fixme-tagged items"));
		errors.put("180", tr("relations without type"));

		errors.put("190", tr("intersections without junctions"));
		errors.put("191", tr("highway-highway"));
		errors.put("192", tr("highway-waterway"));
		errors.put("193", tr("highway-riverbank"));
		errors.put("194", tr("waterway-waterway"));
		errors.put("195", tr("cycleway-cycleway"));
		errors.put("196", tr("highway-cycleway"));
		errors.put("197", tr("cycleway-waterway"));
		errors.put("198", tr("cycleway-riverbank"));

		errors.put("200", tr("overlapping ways"));
		errors.put("201", tr("highway-highway"));
		errors.put("202", tr("highway-waterway"));
		errors.put("203", tr("highway-riverbank"));
		errors.put("204", tr("waterway-waterway"));
		errors.put("205", tr("cycleway-cycleway"));
		errors.put("206", tr("highway-cycleway"));
		errors.put("207", tr("cycleway-waterway"));
		errors.put("208", tr("cycleway-riverbank"));

		errors.put("210", tr("loopings"));
		errors.put("220", tr("misspelled tags"));

		errors.put("230", tr("layer conflicts"));
		errors.put("231", tr("mixed layers intersections"));
		errors.put("232", tr("strange layers"));

		errors.put("270", tr("motorways connected directly"));

		errors.put("280", tr("boundaries"));
		errors.put("281", tr("missing name"));
		errors.put("282", tr("missing admin_level"));
		errors.put("283", tr("not closed loop"));
		errors.put("284", tr("splitting boundary"));
		errors.put("285", tr("admin_level too high"));

		errors.put("290", tr("restrictions"));
		errors.put("291", tr("missing type"));
		errors.put("292", tr("missing from way"));
		errors.put("293", tr("missing to way"));
		errors.put("294", tr("from or to not a way"));
		errors.put("295", tr("via is not on the way ends"));
		errors.put("296", tr("wrong restriction angle"));
		errors.put("297", tr("wrong direction of to member"));
		errors.put("298", tr("already restricted by oneway"));

		errors.put("300", tr("missing maxspeed"));

		errors.put("310", tr("roundabouts"));
		errors.put("311", tr("not closed loop"));
		errors.put("312", tr("wrong direction"));
		errors.put("313", tr("faintly connected"));

		errors.put("320", tr("*_link-connections"));
		errors.put("350", tr("bridge-tags"));
		errors.put("360", tr("language unknown"));
		errors.put("370", tr("doubled places"));
		errors.put("380", tr("non-physical use of sport-tag"));
		errors.put("390", tr("missing tracktype"));

		errors.put("400", tr("geometry glitches"));
		errors.put("401", tr("missing turn restriction"));
		errors.put("402", tr("impossible angles"));

		errors.put("410", tr("website"));
		errors.put("411", tr("http error"));
		errors.put("412", tr("domain hijacking"));
		errors.put("413", tr("non-match"));
		formats.put("geojson", "application/json");
		formats.put("gpx", "application/gpx+xml");
		formats.put("rss", "application/rss+xml");
	}

	/** the difference between groups (integer numbers) */
	public static final int GROUP_DIFFERENCE = 10;

	public KeepRightInformation(String CACHE_DIR) {
		super(CACHE_DIR);
	}

	@Override
	public String getName() {
		return "KeepRight";
	}

	private CachedFile getFile(String type, Bounds bound) {
		String enabled = buildDownloadErrorList();
		String url = baseApi + "format=" + type + "&ch=" + enabled;
		url += "&left=" + Double.toString(bound.getMinLon());
		url += "&bottom=" + Double.toString(bound.getMinLat());
		url += "&right=" + Double.toString(bound.getMaxLon());
		url += "&top=" + Double.toString(bound.getMaxLat());
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

	private DataSet getGeoJsonErrors(Bounds bound) {
		CachedFile cache = getFile("geojson", bound);
		DataSet ds = new DataSet();
		try {
			ds = GeoJsonReader.parseDataSet(cache.getInputStream(), null);
			for (OsmPrimitive osmPrimitive : ds.allPrimitives()) {
				osmPrimitive.setOsmId(Long.parseLong(osmPrimitive.get("error_id")), 1);
			}
		} catch (IllegalDataException | IOException e) {
			Logging.debug(e.getMessage());
			e.printStackTrace();
		}
		return ds;
	}

	@Override
	public DataSet getErrors(List<Bounds> bounds, ProgressMonitor monitor) {
		monitor.subTask(tr("Getting {0} errors", getName()));
		DataSet returnDataSet = null;
		String windowTitle = tr("Updating {0} information", getName());
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
	public ImageIcon getIcon(String errorValue, ImageSizes size) {
		try {
			String realErrorValue = "";
			try {
				realErrorValue = "zap" + Integer.toString((Integer.parseInt(errorValue) / 10) * 10);
			} catch (NumberFormatException e) {
				realErrorValue = errorValue;
				Logging.debug(e.getMessage());
			}
			CachedFile image = GenericInformation.getFile(String.format(baseImg, realErrorValue), "image/*", new File(CACHE_DIR, IMG_SUB_DIR).getCanonicalPath());
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
	public String buildDownloadErrorList() {
		String list = "";
		List<String> enabled = Config.getPref().getList(OpenQA.PREF_PREFIX.concat(getName().toLowerCase()).concat("-tests"), buildDefaultPref());
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
		ArrayList<String> pref = new ArrayList<>();
		errors.forEach((key, value) -> pref.add(key));
		return pref;
	}

	@Override
	public String getNodeToolTip(Node node) {
		StringBuilder sb = new StringBuilder("<html>");
		sb.append(tr(getName()))
		  .append(": ").append(node.get("title"))
		  .append(" - <a href=")
		  .append(String.format(baseErrorUrl, node.get("schema"), node.get("error_id")))
		  .append(">").append(node.get("error_id"))
		  .append("</a>");

		sb.append("<hr/>");
		sb.append(node.get("description"));
		sb.append("<hr/>");
		sb.append("Last edited by ".concat(getUserName(Long.parseLong(node.get("object_id")))));
		String commentText = node.get("comment");
		if (commentText != null && !commentText.trim().isEmpty()) {
			sb.append("<hr/>");
			String htmlText = XmlWriter.encode(commentText, true);
			// encode method leaves us with entity instead of \n
			htmlText = htmlText.replace("&#xA;", "<br>");
			sb.append(htmlText);
		}

		sb.append("</html>");
		String result = sb.toString();
		Logging.debug(result);
		return result;
	}

	@Override
	public List<JButton> getActions(Node node) {
		JButton fixed = new JButton();
		JButton falsePositive = new JButton();

		fixed.setAction(new AbstractAction() {
			private static final long serialVersionUID = 8849423098553429237L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new SendInformation(String.format(commentUrl, FIXED, "", node.get("schema"), node.get("error_id")), CACHE_DIR).run();
				node.put("actionTaken", "true");
				fixed.setEnabled(false);
				falsePositive.setEnabled(true);
				node.put("error_type", "zapangel");
				redrawErrorLayers(tr(LAYER_NAME));
				addChangeSetTag(getName().toLowerCase(), node.get("error_id"));
			}
		});

		falsePositive.setAction(new AbstractAction() {
			private static final long serialVersionUID = 1047757091731416301L;

			@Override
			public void actionPerformed(ActionEvent e) {
				new SendInformation(String.format(commentUrl, FALSE_POSITIVE, "", node.get("schema"), node.get("error_id")), CACHE_DIR).run();
				node.put("actionTaken", "false");
				fixed.setEnabled(true);
				falsePositive.setEnabled(false);
				node.put("error_type", "zapdevil");
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
	public String getLayerName() {
		return LAYER_NAME;
	}

	@Override
	public String getError(Node node) {
		return node.get("error_type");
	}

	@Override
	public String getCacheDir() {
		return this.CACHE_DIR;
	}
}
