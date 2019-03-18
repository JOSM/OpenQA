/**
 *
 */
package com.kaart.openqa.profiles;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;

import com.kaart.openqa.CachedFile;
import com.kaart.openqa.ErrorLayer;
import com.kaart.openqa.OpenQA;

/**
 * @author Taylor Smock
 *
 */
public abstract class GenericInformation {
	/** The API URL, usually something like https://www.example.org/api/0.2 */
	public static String baseApi;
	/** The base URL for images */
	public static String baseImg;
	/** The base URL for error data or error manipulation */
	public static String baseErrorUrl;
	/** The possible errors for the class {@code TreeMap<Integer errorValue, String description>} */
	public static TreeMap<String, String> errors;
	/** The layer name */
	protected String LAYER_NAME = "FIXME";

	/** The subdirectory to store the data. This can be deleted at any time. */
	public static String DATA_SUB_DIR = "data";
	/** The subdirectory to store the images. This is usually not deleted. */
	public static String IMG_SUB_DIR = "img";

	/** the difference between groups (integer numbers) */
	public static final int GROUP_DIFFERENCE = 10;

	protected String CACHE_DIR;
	public GenericInformation(String CACHE_DIR) {
		this.CACHE_DIR = CACHE_DIR;
	}

	public abstract String getName();

	/**
	 * Cache a file for 24 hours
	 * @param url The URL to cache
	 * @param type The accepted times for {@code CachedFile.setHttpAccept}
	 * @param directory The directory to store the file in
	 * @return The @{code CachedFile} object with which to get a file
	 */
	public static CachedFile getFile(String url, String type, String directory) {
		CachedFile cache = new CachedFile(url);
		cache.setMaxAge(86400);
		cache.setHttpAccept(type);
		cache.setDestDir(directory);
		return cache;
	}

	/**
	 * Get an icon for a string/size combination
	 * @param string The string with which to get information -- defaults to "dialogs/notes"
	 * @param size The size of the note
	 * @return {@code ImageIcon} to associate with a {@code String string}
	 */
	public ImageIcon getIcon(String string, ImageSizes size) {
		return ImageProvider.get("dialogs/notes", "note_open", size);
	}

	/**
	 * Get the errors for a layer
	 * @param dataSet {@code DataSet} to get errors for
	 * @param progressMonitor The {@code ProgressMonitor} with which to monitor progress
	 * @return A new {@code DataSet} that has error information for the {@code bounds}
	 */
	public DataSet getErrors(DataSet dataSet, ProgressMonitor progressMonitor) {
		List<Bounds> bounds = dataSet.getDataSourceBounds();
		if (bounds.isEmpty()) {
			bounds = new ArrayList<>();
			Bounds tBound = getDefaultBounds(dataSet);
			bounds.add(tBound);
		}
		return getErrors(bounds, progressMonitor);
	}

	/**
	 * Get errors given a list of bounds
	 * @param bounds {@code List<Bounds>} to get data for
	 * @param progressMonitor The {@code ProgressMonitor} with which to monitor progress
	 * @return A new {@code Layer} that has error information for the {@code bounds}
	 */
	public abstract DataSet getErrors(List<Bounds> bounds, ProgressMonitor progressMonitor);

	/**
	 * Get the bounds for a dataSet
	 * @param dataSet with the data of interest
	 * @return The bounds that encompasses the @{code DataSet}
	 */
	public static Bounds getDefaultBounds(DataSet dataSet) {
		BBox bound = new BBox();
		for (OsmPrimitive osm : dataSet.allPrimitives()) {
			// Don't look at relations -- they can get really large, really fast.
			if (!(osm instanceof Relation)) {
				bound.add(osm.getBBox());
			}
		}

		Bounds rBound = new Bounds(bound.getBottomRight());
		rBound.extend(bound.getTopLeft());
		return rBound;
	}

	/**
	 * Build an error list to download
	 * @return {@code String} of errors to pass to a download method
	 */
	public abstract String buildDownloadErrorList();

	/**
	 * Build an {@code ArrayList<String>} of default preferences
	 * @return An {@code ArrayList<String>} of default preferences
	 */
	public abstract ArrayList<String> buildDefaultPref();

	/**
	 * Get the tooltip for a node
	 * @param node {@code Node} to get information from
	 * @return {@code String} with the information in HTML format
	 */
	public abstract String getNodeToolTip(Node node);

	/**
	 * Get a username from an {@code OsmPrimitiveId} as a {@code Long}
	 * @param obj_id The id to find in the dataset
	 * @return The username
	 */
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

	/**
	 * Convert a userName to be compatible with XML 1.0
	 * @param userName a username to ensure we can display
	 * @return userName in XML 1.0 encoding
	 */
	protected static String getUserName(String userName) {
		if (userName == null || userName.trim().isEmpty()) {
			userName = "&lt;Anonymous&gt;";
		} else {
			userName = XmlWriter.encode(userName);
		}
		return userName;
	}

	/**
	 * Get the cache directory
	 * @return the directory in which we cache files
	 */
	public abstract String getCacheDir();

	/**
	 * Get the Layer name
	 * @return The Layer name
	 */
	public abstract String getLayerName();

	/**
	 * Get the extra error information for a node
	 * @param node {@code Node} with the information
	 * @return a {@code String} with the error id
	 */
	public abstract String getError(Node node);

	/**
	 * Get the possible actions for a error node
	 * @param selectedNode {@code Node} that has error information
	 * @return A list of {@code JButton}s with associated actions to add to a dialog
	 */
	public abstract List<JButton> getActions(Node selectedNode);

	/**
	 * Redraw error layers
	 * @param name of the layer to redraw
	 */
	public static void redrawErrorLayers(String name) {
		List<ErrorLayer> layers = MainApplication.getLayerManager().getLayersOfType(ErrorLayer.class);
		for (ErrorLayer layer : layers) {
			layer.invalidate();
		}
	}

	protected static class SendInformation implements Runnable {
		final String url;
		final String directory;

		public SendInformation(String url, String CACHE_DIR) {
			this.url = url;
			directory = CACHE_DIR;
		}

		@Override
		public void run() {
			try (CachedFile cache = new CachedFile(url)) {
				File dir = new File(directory, DATA_SUB_DIR);
				cache.setDestDir(dir.getPath());
				cache.getFile();
				cache.close();
				cache.clear();
			} catch (IOException e) {
				Logging.debug(e.getMessage());
			}
		}
	}

	public static void addChangeSetTag(String source, String id) {
		DataSet data = MainApplication.getLayerManager().getActiveDataSet();
		// TODO figure out if we want to keep this
		boolean addChangesetTags = Config.getPref().getBoolean(OpenQA.PREF_PREFIX.concat("changesetTags"), false);
		if (addChangesetTags && data != null && !data.isEmpty()) {
			Map<String, String> tags = data.getChangeSetTags();
			String key = OpenQA.NAME.toLowerCase();
			// Clear the changeset tag if needed
			if (source == null || id == null) {
				data.addChangeSetTag(key, "");
				return;
			}
			if (data.isModified()) {
				String addTag = source.concat("-").concat(id);
				if (tags.containsKey(key) && !tags.get(key).trim().isEmpty()) {
					addTag = tags.get(key).concat(",").concat(addTag);
				}
				data.addChangeSetTag(key, addTag);
			} else {
				data.addChangeSetTag(key, "");
			}
		}
	}
}
