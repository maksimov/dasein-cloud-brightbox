/**
 * Copyright (C) 2009-2015 Dell, Inc.
 * See annotations for authorship information
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */
package org.dasein.cloud.brightbox.dc;

import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.ProviderContext;
import org.dasein.cloud.brightbox.BrightBoxCloud;
import org.dasein.cloud.brightbox.NoContextException;
import org.dasein.cloud.brightbox.api.model.Zone;
import org.dasein.cloud.dc.AbstractDataCenterServices;
import org.dasein.cloud.dc.DataCenter;
import org.dasein.cloud.dc.DataCenterCapabilities;
import org.dasein.cloud.dc.Region;
import org.dasein.cloud.util.APITrace;
import org.dasein.cloud.util.Cache;
import org.dasein.cloud.util.CacheLevel;
import org.dasein.util.uom.time.Day;
import org.dasein.util.uom.time.Hour;
import org.dasein.util.uom.time.TimePeriod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Data center services
 * @author stas
 * @version 2015.03.1 initial version
 * @since 2015.03.1
 */
public class Zones extends AbstractDataCenterServices<BrightBoxCloud> {
    static private final Logger logger = BrightBoxCloud.getLogger(Zones.class);

    public Zones(@Nonnull BrightBoxCloud provider) {
        super(provider);
    }

    private transient volatile ZonesCapabilities capabilities;

    @Override
    public @Nonnull DataCenterCapabilities getCapabilities() throws InternalException, CloudException {
        if( capabilities == null ) {
            capabilities = new ZonesCapabilities(getProvider());
        }
        return capabilities;
    }

    @Override
    public @Nullable DataCenter getDataCenter(@Nonnull String dataCenterId) throws InternalException, CloudException {
        for( Region region : listRegions() ) {
            for( DataCenter dc : listDataCenters(region.getProviderRegionId()) ) {
                if( dataCenterId.equals(dc.getProviderDataCenterId()) ) {
                    return dc;
                }
            }
        }
        return null;
    }

    @Override
    public @Nullable Region getRegion(@Nonnull String providerRegionId) throws InternalException, CloudException {
        for( Region r : listRegions() ) {
            if( providerRegionId.equals(r.getProviderRegionId()) ) {
                return r;
            }
        }
        return null;
    }

    @Override
    public @Nonnull Collection<DataCenter> listDataCenters(@Nonnull String providerRegionId) throws InternalException, CloudException {
        APITrace.begin(getProvider(), "listDataCenters");
        try {
            Region region = getRegion(providerRegionId);

            if( region == null ) {
                throw new CloudException("No such region: " + providerRegionId);
            }
            ProviderContext ctx = getProvider().getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            Cache<DataCenter> cache = Cache.getInstance(getProvider(), "dataCenters", DataCenter.class, CacheLevel.REGION_ACCOUNT, new TimePeriod<Day>(1, TimePeriod.DAY));
            Collection<DataCenter> dcList = ( Collection<DataCenter> ) cache.get(ctx);

            if( dcList != null ) {
                return dcList;
            }
            List<DataCenter> dataCenters = new ArrayList<DataCenter>();
            List<Zone> zones = getProvider().getCloudApiService().listZones();
            for( Zone zone : zones ) {
                dataCenters.add(new DataCenter(zone.getId(), zone.getHandle(), zoneHandleToRegionId(zone.getHandle()), true, true));
            }
            cache.put(ctx, dataCenters);
            return dataCenters;
        }
        finally {
            APITrace.end();
        }
    }

    private String zoneHandleToRegionId(String zoneHandle) {
        String [] parts = zoneHandle.split("-");
        String regionId = "gb1";
        if( parts.length == 2 ) {
            regionId = parts[0];
        }
        return regionId;
    }

    @Override
    public Collection<Region> listRegions() throws InternalException, CloudException {
        APITrace.begin(getProvider(), "listRegions");
        try {
            ProviderContext ctx = getProvider().getContext();

            if( ctx == null ) {
                throw new NoContextException();
            }
            Cache<Region> cache = Cache.getInstance(getProvider(), "regions", Region.class, CacheLevel.CLOUD_ACCOUNT, new TimePeriod<Hour>(10, TimePeriod.HOUR));
            Collection<Region> regions = (Collection<Region>)cache.get(ctx);

            if( regions != null ) {
                return regions;
            }
            regions = new ArrayList<Region>();
            List<Zone> zones = getProvider().getCloudApiService().listZones();
            for( Zone zone : zones ) {
                String regionId = zoneHandleToRegionId(zone.getHandle());
                Region region = new Region(regionId, regionId, true, true);
                region.setJurisdiction(regionId.substring(0, 2));
                regions.add(region);
            }
            cache.put(ctx, regions);
            return regions;
        }
        finally {
            APITrace.end();
        }
    }

}
