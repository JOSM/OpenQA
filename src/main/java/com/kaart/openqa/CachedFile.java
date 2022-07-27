// License: GPL. For details, see LICENSE file.
package com.kaart.openqa;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import org.openstreetmap.josm.data.Version;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Downloads a file and caches it on disk in order to reduce network load.
 * <p>
 * Supports URLs, local files, and a custom scheme (<code>resource:</code>) to
 * get resources from the current *.jar file. (Local caching is only done for
 * URLs.)
 * <p>
 * The mirrored file is only downloaded if it has been more than 7 days since
 * last download. (Time can be configured.)
 * <p>
 * The file content is normally accessed with {@link #getInputStream()}, but you
 * can also get the mirrored copy with {@link #getFile()}.
 */
public class CachedFile extends org.openstreetmap.josm.io.CachedFile {
    protected boolean deleteOnExit;

    /**
     * Constructs a CachedFile object from a given filename, URL or internal
     * resource.
     *
     * @param name can be:
     *             <ul>
     *             <li>relative or absolute file name</li>
     *             <li>{@code file:///SOME/FILE} the same as above</li>
     *             <li>{@code http://...} a URL. It will be cached on disk.</li>
     *             <li>{@code resource://SOME/FILE} file from the classpath (usually
     *             in the current *.jar)</li>
     *             <li>{@code josmdir://SOME/FILE} file inside josm user data
     *             directory (since r7058)</li>
     *             <li>{@code josmplugindir://SOME/FILE} file inside josm plugin
     *             directory (since r7834)</li>
     *             </ul>
     */
    public CachedFile(String name) {
        super(name);
        setDefaultHttpHeaders();
    }

    private void setDefaultHttpHeaders() {
        String userAgent = "Josm/".concat(Version.getInstance().getVersionString()).concat("(")
                .concat(System.getProperty("os.name")).concat(") OpenQA/").concat(OpenQA.getVersion());
        super.setHttpHeaders(Collections.singletonMap("User-Agent", userAgent));
    }

    /**
     * Get local file for the requested resource.
     *
     * @return The local cache file for URLs. If the resource is a local file,
     *         returns just that file.
     * @throws IOException when the resource with the given name could not be
     *                     retrieved
     */
    @Override
    public synchronized File getFile() throws IOException {
        if (initialized)
            return cacheFile;
        super.getFile();
        if (deleteOnExit && name != null && name.matches("^(http|ftp)")) {
            cacheFile.deleteOnExit();
            String prefKey = getPrefKey(new URL(name), destDir);
            Config.getPref().putList(prefKey, null);
        }
        return cacheFile;
    }

    /**
     * Get preference key to store the location and age of the cached file. 2
     * resources that point to the same url, but that are to be stored in different
     * directories will not share a cache file.
     *
     * @param url     URL
     * @param destDir destination directory
     * @return Preference key
     */
    private static String getPrefKey(URL url, String destDir) {
        StringBuilder prefKey = new StringBuilder("mirror.");
        if (destDir != null) {
            prefKey.append(destDir).append('.');
        }
        prefKey.append(url.toString().replaceAll("%<(.*)>", ""));
        return prefKey.toString().replace("=", "_");
    }

    /**
     * Should the file be deleted on exit? Must be called before download.
     *
     * @param delete true if we want to delete on exit.
     */
    public void setDeleteOnExit(boolean delete) {
        deleteOnExit = delete;
    }

    /**
     * @return true if the file will be deleted on program exit
     *         {@code File.deleteOnExit()} The file will only be deleted if it is a
     *         remote file.
     */
    public boolean getDeleteOnExit() {
        return deleteOnExit;
    }
}
