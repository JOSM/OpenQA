/**
 * 
 */
package com.kaartgroup.openqa.profiles;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;

/**
 * @author Taylor Smock
 *
 */
public abstract class GenericInformation {
	public static String baseApi;
	public static String baseImg;
	public static String baseErrorUrl;
	public static TreeMap<Integer, String> errors;
	public static final String LAYER_NAME = "FIXME";
	
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
	
	public static ImageIcon getIcon(int errorValue, ImageSizes size, String CACHE_DIR) {
		return ImageProvider.get("dialogs/notes", "note_open", size);
	}
	
	public abstract Layer getErrors(List<Bounds> bounds);
	public abstract String buildDownloadErrorList();
	public abstract ArrayList<String> buildDefaultPref();
}
