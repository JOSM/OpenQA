// License: GPL. For details, see LICENSE file.
package com.kaart.openqa.profiles;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.Data;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;

import com.kaart.openqa.ErrorLayer;
import com.kaart.openqa.OpenQA;
import com.kaart.openqa.OpenQADataSet;

/**
 * A class storing information for the different error sources
 *
 * @param <I> The identifier class
 * @param <N> The node class
 * @param <D> The dataset class
 * @author Taylor Smock
 */
public abstract class GenericInformation<I, N extends OpenQANode<I>, D extends OpenQADataSet<I, N>> {
    /** The subdirectory to store the data. This can be deleted at any time. */
    public static final String DATA_SUB_DIR = "data";
    /** The subdirectory to store the images. This is usually not deleted. */
    public static final String IMG_SUB_DIR = "img";

    /** The key to store unique the error id */
    public static final String ERROR_ID = "error_id";

    /** the difference between groups (integer numbers) */
    public static final int GROUP_DIFFERENCE = 10;

    protected final String cacheDir;

    protected GenericInformation(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * The layer name
     *
     * @return the layer name
     */
    public abstract String getName();

    /**
     * The API URL, usually something like
     * <a href="https://www.example.org/api/0.2">https://www.example.org/api/0.2</a>
     *
     * @return the base api URL
     */
    public abstract String getBaseApi();

    /**
     * The base URL for images
     *
     * @return the base URL
     */
    public abstract String getBaseImg();

    /**
     * The base URL for error data or error manipulation
     *
     * @return the base url
     */
    public abstract String getBaseErrorUrl();

    /**
     * The possible errors for the class
     * {@code NavigableMap<Integer errorValue, String description>}
     *
     * @return the errors
     */
    public abstract NavigableMap<String, String> getErrors();

    /**
     * Get an icon for a string/size combination
     *
     * @param string The string with which to get information -- defaults to
     *               "dialogs/notes"
     * @param size   The size of the note
     * @return {@code ImageIcon} to associate with a {@code String string}
     */
    public ImageIcon getIcon(String string, ImageSizes size) {
        return ImageProvider.get("dialogs/notes", "note_open", size);
    }

    /**
     * Get the errors for a layer
     *
     * @param dataSet         {@code DataSet} to get errors for
     * @param progressMonitor The {@code ProgressMonitor} with which to monitor
     *                        progress
     * @return A new {@code DataSet} that has error information for the
     *         {@code bounds}
     */
    public D getErrors(Data dataSet, ProgressMonitor progressMonitor) {
        List<Bounds> bounds = dataSet.getDataSourceBounds();
        if (bounds.isEmpty()) {
            bounds = getDefaultBounds(dataSet, progressMonitor.createSubTaskMonitor(0, false));
        }
        return getErrors(bounds, progressMonitor);
    }

    /**
     * Get errors given a defaultDownloadTypes of bounds
     *
     * @param bounds          {@code List<Bounds>} to get data for
     * @param progressMonitor The {@code ProgressMonitor} with which to monitor
     *                        progress
     * @return A new {@code Layer} that has error information for the {@code bounds}
     */
    public abstract D getErrors(List<Bounds> bounds, ProgressMonitor progressMonitor);

    /**
     * Get the bounds for a dataSet
     *
     * @param dataSet with the data of interest
     * @param monitor the ProgressMonitor with which to see progress with
     * @return The bounds that encompasses the {@link Data}
     */
    public static List<Bounds> getDefaultBounds(Data dataSet, ProgressMonitor monitor) {
        return dataSet.getDataSourceBounds();
    }

    /**
     * Build an error defaultDownloadTypes to download
     *
     * @return {@code String} of errors to pass to a download method
     */
    public abstract String buildDownloadErrorList();

    /**
     * Build an {@code ArrayList<String>} of default preferences
     *
     * @return An {@code ArrayList<String>} of default preferences
     */
    public abstract List<String> buildDefaultPref();

    /**
     * Get the tooltip for a node
     *
     * @param node {@code Node} to get information from
     * @return {@code String} with the information in HTML format
     */
    public abstract String getNodeToolTip(N node);

    /**
     * Cache additional information for a node
     *
     * @param node to get information from
     * @return true if there is additional information
     */
    public boolean cacheAdditionalInformation(N node) {
        return false;
    }

    /**
     * Get a username from an {@code OsmPrimitiveId} as a {@code Long}
     *
     * @param objId The id to find in the dataset
     * @return The username
     */
    protected static String getUserName(long objId) {
        OsmPrimitive osm = new Node();
        osm.setOsmId(objId, 1);
        ArrayList<OsmDataLayer> layers = new ArrayList<>(
                MainApplication.getLayerManager().getLayersOfType(OsmDataLayer.class));
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
     *
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
     *
     * @return the directory in which we cache files
     */
    public abstract String getCacheDir();

    /**
     * Get the Layer name
     *
     * @return The Layer name
     */
    public abstract String getLayerName();

    /**
     * Get the extra error information for a node
     *
     * @param node {@code Node} with the information
     * @return a {@code String} with the error id
     */
    public abstract String getError(N node);

    /**
     * Get the possible actions for a error node
     *
     * @param selectedNode {@code Node} that has error information
     * @return A defaultDownloadTypes of {@code JButton}s with associated actions to
     *         add to a dialog
     */
    public abstract List<JButton> getActions(N selectedNode);

    /**
     * Redraw error layers
     *
     * @param name of the layer to redraw
     */
    public static void redrawErrorLayers(String name) {
        List<ErrorLayer> layers = MainApplication.getLayerManager().getLayersOfType(ErrorLayer.class);
        for (ErrorLayer layer : layers) {
            layer.invalidate();
        }
    }

    /**
     * Create a new dataset
     *
     * @return The dataset to store information in
     */
    public abstract D createNewDataSet();

    protected static class SendInformation implements Runnable {
        final String url;

        public SendInformation(String url, String cacheDir) {
            this.url = url;
        }

        @Override
        public void run() {
            HttpClient client = null;
            try {
                client = HttpClient.create(URI.create(this.url).toURL());
                client.connect();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                if (client != null) {
                    client.disconnect();
                }
            }
        }
    }

    /**
     * Add a changeset tag to a layer
     *
     * @param source The source to add
     * @param id     The id from the source
     */
    public static void addChangeSetTag(String source, String id) {
        DataSet data = MainApplication.getLayerManager().getActiveDataSet();
        // TODO figure out if we want to keep this
        boolean addChangesetTags = Config.getPref().getBoolean(OpenQA.PREF_PREFIX.concat("changesetTags"), false);
        if (addChangesetTags && data != null && !data.allPrimitives().isEmpty()) {
            Map<String, String> tags = data.getChangeSetTags();
            String key = OpenQA.NAME.toLowerCase(Locale.US);
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
