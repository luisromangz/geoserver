/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.map.LRUMap;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;

public class GeoIPPostProcessor implements RequestPostProcessor {

    static Logger LOGGER = Logging.getLogger("org.geoserver.montior");

    private final LRUMap cache;

    private final LookupService geoIPLookup;

    private static final String MONITOR_PATH = "monitoring";

    public GeoIPPostProcessor(GeoServerResourceLoader loader, String lookupDBFileName, int cacheSize) {
        geoIPLookup = lookupGeoIPDatabase(loader, lookupDBFileName);
        cache = new LRUMap(cacheSize);
    }

    public void run(RequestData data, HttpServletRequest request, HttpServletResponse response) {
        if (geoIPLookup == null) {
            return;
        }

        String remoteAddr = data.getRemoteAddr();
        if (remoteAddr == null) {
            LOGGER.info("Request data did not contain ip address. Unable to perform GeoIP lookup.");
            return;
        }

        Location loc = null;
        // we also cache the fact that we didn't find the remoteAddr
        if (cache.containsKey(remoteAddr)) {
            loc = (Location) cache.get(remoteAddr);
            if (loc == null) {
                return;
            }
        } else {
            loc = geoIPLookup.getLocation(remoteAddr);
            cache.put(remoteAddr, loc);
            if (loc == null) {
                LOGGER.fine("Unable to obtain location for " + remoteAddr);
                return;
            }
        }

        data.setRemoteCountry(loc.countryName);
        data.setRemoteCity(loc.city);
        data.setRemoteLat(loc.latitude);
        data.setRemoteLon(loc.longitude);
    }

    private static LookupService lookupGeoIPDatabase(GeoServerResourceLoader loader, String filename) {
        try {
            File f = loader.find(MONITOR_PATH, filename);
            if (f != null) {
                return new LookupService(f);
            }

            // monitor db not found, log and return null
            File monitorDir = new File(loader.getBaseDirectory(), MONITOR_PATH);
            String path = new File(monitorDir, filename).getAbsolutePath();
            LOGGER.warning("GeoIP database " + path + " is not available. "
                    + "Please install the file to enable GeoIP lookups.");
            return null;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error occured looking up GeoIP database", e);
            return null;
        }
    }

}
