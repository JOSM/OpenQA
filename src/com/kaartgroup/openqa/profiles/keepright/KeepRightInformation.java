/**
 * 
 */
package com.kaartgroup.openqa.profiles.keepright;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.markerlayer.MarkerLayer;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.xml.sax.SAXException;

import com.kaartgroup.openqa.ErrorLayer;
import com.kaartgroup.openqa.GeoJsonReader;
import com.kaartgroup.openqa.profiles.GenericInformation;

/**
 * @author Taylor Smock
 *
 */
public class KeepRightInformation extends GenericInformation {
	public static final String LAYER_NAME = "Keep Right Errors";
	public static String baseApi = "https://www.keepright.at/export.php?";
	public static String baseImg = "https://www.keepright.at/img/zap%d.png";
	public static String baseErrorUrl = "https://www.keepright.at/report_map.php?schema=%s&error=%s";
	
	public static String commentUrl = "https://www.keepright.at/comment.php?st=%s&co=%s&schema=%s&id=%s";
	public static String FIXED = "ignore_t";
	public static String FALSE_POSITIVE = "ignore";
	public static enum COMMENT {
		 
		// Fixed
		// https://www.keepright.at/comment.php?st=ignore_t&co=<+COMMENT+>&schema=<+SCHEMA+>&id=<+ERROR_ID+>
		// Keep open
		// https://www.keepright.at/comment.php?st=&co=<+COMMENT+>&schema=<+SCHEMA+>&id=<+ERROR_ID+>
		// False positive
		// https://www.keepright.at/comment.php?st=ignore&co=<+COMMENT+>&schema=<+SCHEMA+>&id=<+ERROR_ID+>
	}
	public static TreeMap<String, String> formats = new TreeMap<>();
	public static TreeMap<Integer, String> errors = new TreeMap<>();
	static {
		errors.put(0, tr("default"));
		errors.put(20, tr("multiple nodes on the same spot"));
		errors.put(30, tr("non-closed areas"));
		errors.put(40, tr("dead-ended one-ways"));
		errors.put(50, tr("almost-junctions"));
		errors.put(60, tr("deprecated tags"));
		errors.put(70, tr("missing tags"));
		errors.put(90, tr("motorways without ref"));
		errors.put(100, tr("places of worship without religion"));
		errors.put(110, tr("point of interest without name"));
		errors.put(120, tr("ways without nodes"));
		errors.put(130, tr("floating islands"));
		errors.put(150, tr("railway crossings without tag"));
		errors.put(160, tr("wrongly used railway crossing tag"));
		errors.put(170, tr("fixme-tagged items"));
		errors.put(180, tr("relations without type"));
		
		errors.put(190, tr("intersections without junctions"));
		errors.put(191, tr("highway-highway"));
		errors.put(192, tr("highway-waterway"));
		errors.put(193, tr("highway-riverbank"));
		errors.put(194, tr("waterway-waterway"));
		errors.put(195, tr("cycleway-cycleway"));
		errors.put(196, tr("highway-cycleway"));
		errors.put(197, tr("cycleway-waterway"));
		errors.put(198, tr("cycleway-riverbank"));
		
		errors.put(200, tr("overlapping ways"));
		errors.put(201, tr("highway-highway"));
		errors.put(202, tr("highway-waterway"));
		errors.put(203, tr("highway-riverbank"));
		errors.put(204, tr("waterway-waterway"));
		errors.put(205, tr("cycleway-cycleway"));
		errors.put(206, tr("highway-cycleway"));
		errors.put(207, tr("cycleway-waterway"));
		errors.put(208, tr("cycleway-riverbank"));
		
		errors.put(210, tr("loopings"));
		errors.put(220, tr("misspelled tags"));
		
		errors.put(230, tr("layer conflicts"));
		errors.put(231, tr("mixed layers intersections"));
		errors.put(232, tr("strange layers"));
		
		errors.put(270, tr("motorways connected directly"));
		
		errors.put(280, tr("boundaries"));
		errors.put(281, tr("missing name"));
		errors.put(282, tr("missing admin_level"));
		errors.put(283, tr("not closed loop"));
		errors.put(284, tr("splitting boundary"));
		errors.put(285, tr("admin_level too high"));
		
		errors.put(290, tr("restrictions"));
		errors.put(291, tr("missing type"));
		errors.put(292, tr("missing from way"));
		errors.put(293, tr("missing to way"));
		errors.put(294, tr("from or to not a way"));
		errors.put(295, tr("via is not on the way ends"));
		errors.put(296, tr("wrong restriction angle"));
		errors.put(297, tr("wrong direction of to member"));
		errors.put(298, tr("already restricted by oneway"));
		
		errors.put(300, tr("missing maxspeed"));
		
		errors.put(310, tr("roundabouts"));
		errors.put(311, tr("not closed loop"));
		errors.put(312, tr("wrong direction"));
		errors.put(313, tr("faintly connected"));
		
		errors.put(320, tr("*_link-connections"));
		errors.put(350, tr("bridge-tags"));
		errors.put(360, tr("language unknown"));
		errors.put(370, tr("doubled places"));
		errors.put(380, tr("non-physical use of sport-tag"));
		errors.put(390, tr("missing tracktype"));
		
		errors.put(400, tr("geometry glitches"));
		errors.put(401, tr("missing turn restriction"));
		errors.put(402, tr("impossible angles"));
		
		errors.put(410, tr("website"));
		errors.put(411, tr("http error"));
		errors.put(412, tr("domain hijacking"));
		errors.put(413, tr("non-match"));
		formats.put("geojson", "application/json");
		formats.put("gpx", "application/gpx+xml");
		formats.put("rss", "application/rss+xml");
	}
	
	/** the difference between groups (integer numbers) */
	public static final int GROUP_DIFFERENCE = 10;
	
	final String CACHE_DIR;
	
	public KeepRightInformation(String CACHE_DIR) {
		this.CACHE_DIR = CACHE_DIR;
	}
	
	public static String getImage(String description) {
		Object[] keys = errors.entrySet().stream().filter(e -> description.equals(e.getValue())).map(e -> e.getKey()).toArray();
		if (keys.length == 1 && keys[0] instanceof Integer){
			return getImage((Integer) keys[0]);
		} else {
			return null;
		}
	}
	
	public static  String getImage(int code) {
		return String.format(baseImg,code);
	}
	
	private CachedFile getFile(String type, Bounds bound) {
		String enabled = buildDownloadErrorList();
		String url = baseApi + "format=" + type + "&ch=" + enabled;
		url += "&left=" + Double.toString(bound.getMinLon());
		url += "&bottom=" + Double.toString(bound.getMinLat());
		url += "&right=" + Double.toString(bound.getMaxLon());
		url += "&top=" + Double.toString(bound.getMaxLat());
		CachedFile cache = GenericInformation.getFile(url, formats.get(type), CACHE_DIR);
		return cache;
	}
	
	private GpxData getGpxErrors(Bounds bound) {
		CachedFile cache = getFile("gpx", bound);
		try {
			GpxReader reader = new GpxReader(cache.getInputStream());
			reader.parse(true);
			cache.close();
			return reader.getGpxData();
		} catch (IOException e) {
			Logging.debug(e.getMessage());
		} catch (SAXException e) {
			Logging.debug(e.getMessage());
		}
		cache.close();
		return null;
	}
	
	private Layer getGeoJsonErrors(Bounds bound) {
		CachedFile cache = getFile("geojson", bound);
		ErrorLayer layer = new ErrorLayer(CACHE_DIR);
		try {
			DataSet ds = GeoJsonReader.parseDataSet(cache.getInputStream(), null);
			layer.addNotes(ds);
		} catch (IllegalDataException | IOException e) {
			Logging.debug(e.getMessage());
			e.printStackTrace();
		}
		return layer;
	}
	
	public Layer getErrors(List<Bounds> bounds) {
		String type = Config.getPref().get(KeepRightPreferences.PREF_FILETYPE);
		Layer mlayer = null;
		if (type.equals("geojson")) {
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
		} else {
			// Default to GPX 
			for (Bounds bound : bounds) {
				GpxData gpxData = getGpxErrors(bound);
				if (gpxData != null) {
					GpxLayer layer = new GpxLayer(gpxData);
					MarkerLayer tlayer = new MarkerLayer(gpxData, LAYER_NAME, layer.getAssociatedFile(), layer);
					if (mlayer == null) {
						mlayer = tlayer;
					} else {
						mlayer.mergeFrom(tlayer);
					}
				}
			}
		}
		return mlayer;
	}
	
	public static ImageIcon getIcon(int errorValue, ImageSizes size, String CACHE_DIR) {
		try {
			CachedFile image = GenericInformation.getFile(String.format(baseImg, errorValue), "image/*", CACHE_DIR);
			image.setMaxAge(30 * 86400);
			image.getFile();
			ImageIcon icon = ImageProvider.get(image.getFile().getAbsolutePath(), size);
			image.close();
			return icon;
		} catch (NullPointerException | IOException e) {
			return GenericInformation.getIcon(-1, size, CACHE_DIR);
		}
	}
	public String buildDownloadErrorList() {
		String list = "";
		List<String> enabled = Config.getPref().getList("keepright-tests", buildDefaultPref());
		for (int i = 0; i < enabled.size(); i++) {
			list += enabled.get(i);
			if (i < enabled.size() - 1) {
				list += ",";
			}
		}
		return list;
	}
	public ArrayList<String> buildDefaultPref() {
		ArrayList<String> pref = new ArrayList<>();
		errors.forEach((key, value) -> pref.add(Integer.toString(key)));
		return pref;
	}
}
