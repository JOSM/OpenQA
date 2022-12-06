// License: GPL. For details, see LICENSE file.
package com.kaart.openqa;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;

import org.apache.commons.jcs3.access.CacheAccess;
import org.apache.commons.jcs3.engine.behavior.ICacheElement;
import org.apache.commons.jcs3.engine.behavior.IElementAttributes;
import org.openstreetmap.josm.data.cache.JCSCacheManager;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.Utils;

/**
 * The class for cache
 */
public final class OpenQACache {

    private OpenQACache() {
        // Hide the constructor
    }

    private static final CacheAccess<String, byte[]> CACHE = JCSCacheManager.getCache(OpenQA.NAME);

    /**
     * Get the data from a URL (cache data for 1 day)
     *
     * @param url The URL to get data from
     * @return The InputStream to read
     */
    public static InputStream getUrl(String url) {
        return getUrl(url, Duration.ofDays(1));
    }

    /**
     * Get the data from a URL
     *
     * @param url        The URL to get data from
     * @param timeToKeep The time to keep the data
     * @return The InputStream to read
     */
    public static InputStream getUrl(String url, Duration timeToKeep) {
        final byte[] bytes = CACHE.get(url, () -> {
            HttpClient client = null;
            try {
                client = HttpClient.create(URI.create(url).toURL());
                HttpClient.Response response = client.connect();
                try (InputStream is = response.getContent()) {
                    return Utils.readBytesFromStream(is);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                if (client != null) {
                    client.disconnect();
                }
            }
        });
        ICacheElement<String, byte[]> cacheElement = CACHE.getCacheElement(url);
        IElementAttributes attribs = cacheElement.getElementAttributes();
        attribs.setIdleTime(timeToKeep.toMillis());
        cacheElement.setElementAttributes(attribs);
        return new ByteArrayInputStream(bytes);
    }
}
