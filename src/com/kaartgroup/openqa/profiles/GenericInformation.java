/**
 *
 */
package com.kaartgroup.openqa.profiles;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;

import com.kaartgroup.openqa.CachedFile;

/**
 * @author Taylor Smock
 *
 */
public abstract class GenericInformation {
	public static String baseApi;
	public static String baseImg;
	public static String baseErrorUrl;
	public static TreeMap<Integer, String> errors;
	protected String LAYER_NAME = "FIXME";
	protected String CACHE_DIR = "";

	/** the difference between groups (integer numbers) */
	public static final int GROUP_DIFFERENCE = 10;

	public static String getImage(String description) {
		Object[] keys = errors.entrySet().stream().filter(e -> description.equals(e.getValue())).map(e -> e.getKey()).toArray();
		if (keys.length == 1 && keys[0] instanceof Integer){
			return getImage((Integer) keys[0]);
		} else {
			return null;
		}
	}

	public static String getImage(int code) {
		return String.format(baseImg,code);
	}

	public static CachedFile getFile(String url, String type, String directory) {
		CachedFile cache = new CachedFile(url);
		cache.setMaxAge(86400);
		cache.setHttpAccept(type);
		cache.setDestDir(directory);
		return cache;
	}

	public ImageIcon getIcon(String string, ImageSizes size) {
		return ImageProvider.get("dialogs/notes", "note_open", size);
	}

	public abstract Layer getErrors(List<Bounds> bounds);
	public abstract String buildDownloadErrorList();
	public abstract ArrayList<String> buildDefaultPref();

	public abstract String getNodeToolTip(Node node);
	protected static String getUserName(long obj_id) {
		OsmPrimitive osm = new Node();
		osm.setOsmId(obj_id, 1);
		ArrayList<OsmDataLayer> layers = new ArrayList<>(MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class));
		for (OsmDataLayer layer : layers) {
			OsmPrimitive rosm = layer.data.getPrimitiveById(osm.getOsmPrimitiveId());
			if (rosm != null) {
				osm = rosm;
				break;
			}
		}
		User user = osm.getUser();
		String userName;
		if (user == null) {
			userName = null;
		} else {
			userName = user.getName();
		}
		return getUserName(userName);
	}

	protected static String getUserName(String userName) {
		if (userName == null || userName.trim().isEmpty()) {
			userName = "&lt;Anonymous&gt;";
		} else {
			userName = XmlWriter.encode(userName);
		}
		return userName;
	}


	public abstract String getCacheDir();

	public abstract String getLayerName();

	public abstract String getError(Node node);

	public abstract JPanel getActions(Node selectedNode);
}
