/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.caching;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Statistics;
import org.dspace.kernel.ServiceManager;
import org.dspace.kernel.mixins.ConfigChangeListener;
import org.dspace.kernel.mixins.ServiceChangeListener;
import org.dspace.providers.CacheProvider;
import org.dspace.services.CachingService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.caching.model.EhcacheCache;
import org.dspace.services.caching.model.MapCache;
import org.dspace.services.model.Cache;
import org.dspace.services.model.CacheConfig;
import org.dspace.services.model.CacheConfig.CacheScope;
import org.dspace.services.model.RequestInterceptor;
import org.dspace.utils.servicemanager.ProviderHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of the core caching service, which is available for
 * anyone who is writing code for DSpace to use.
 *
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public final class CachingServiceImpl
    implements CachingService, ConfigChangeListener, ServiceChangeListener {

    private static final Logger log = LoggerFactory.getLogger(CachingServiceImpl.class);

    /**
     * This is the event key for a full cache reset.
     */
    protected static final String EVENT_RESET = "caching.reset";
    /**
     * The default configuration location.
     */
    protected static final String DEFAULT_CONFIG = "org/dspace/services/caching/ehcache-config.xml";

    /**
     * All the non-thread caches that we know about.
     * Mostly used for tracking purposes.
     */
    private final Map<String, EhcacheCache> cacheRecord = new ConcurrentHashMap<>();

    /**
     * All the request caches.  This is bound to the thread.
     * The initial value of this TL is set automatically when it is
     * created.
     */
    private final Map<String, Map<String, MapCache>> requestCachesMap = new ConcurrentHashMap<>();

    /**
     * @return the current request map which is bound to the current thread
     */
    protected Map<String, MapCache> getRequestCaches() {
        if (requestService == null || requestService.getCurrentRequestId() == null) {
            return null;
        }

        Map<String, MapCache> requestCaches = requestCachesMap.get(requestService.getCurrentRequestId());
        if (requestCaches == null) {
            requestCaches = new HashMap<>();
            requestCachesMap.put(requestService.getCurrentRequestId(), requestCaches);
        }

        return requestCaches;
    }

    /**
     * Unbinds all request caches.  Destroys the caches completely.
     */
    @Override
    public void unbindRequestCaches() {
        if (requestService != null) {
            requestCachesMap.remove(requestService.getCurrentRequestId());
        }
    }

    private ConfigurationService configurationService;

    @Autowired(required = true)
    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    private RequestService requestService;

    @Autowired
    public void setRequestService(RequestService requestService) {
        this.requestService = requestService;
    }

    private ServiceManager serviceManager;

    @Autowired(required = true)
    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    /**
     * The underlying cache manager; injected.
     */
    protected net.sf.ehcache.CacheManager cacheManager;

    @Autowired(required = true)
    public void setCacheManager(net.sf.ehcache.CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public net.sf.ehcache.CacheManager getCacheManager() {
        return cacheManager;
    }

    private boolean useClustering = false;
    private boolean useDiskStore = true;
    private int maxElementsInMemory = 2000;
    private int timeToLiveSecs = 3600;
    private int timeToIdleSecs = 600;

    /**
     * Reloads the configuration settings from the configuration service.
     */
    protected void reloadConfig() {
        // Reload caching configurations, but have sane default values if unspecified in configs
        useClustering = configurationService.getPropertyAsType(knownConfigNames[0], false);
        useDiskStore = configurationService.getPropertyAsType(knownConfigNames[1], true);
        maxElementsInMemory = configurationService.getPropertyAsType(knownConfigNames[2], 2000);
        timeToLiveSecs = configurationService.getPropertyAsType(knownConfigNames[3], 3600);
        timeToIdleSecs = configurationService.getPropertyAsType(knownConfigNames[4], 600);
    }

    /**
     * WARNING: Do not change the order of these! <br/>
     * If you do, you have to fix the {@link #reloadConfig()} method -AZ
     */
    private final String[] knownConfigNames = {
        "caching.use.clustering", // bool - whether to use clustering
        "caching.default.use.disk.store", // whether to use the disk store
        "caching.default.max.elements", // the maximum number of elements in memory, before they are evicted
        "caching.default.time.to.live.secs", // the default amount of time to live for an element from its creation date
        "caching.default.time.to.idle.secs", // the default amount of time to live for an element from its last
        // accessed or modified date
    };

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ConfigChangeListener#notifyForConfigNames()
     */
    @Override
    public String[] notifyForConfigNames() {
        return knownConfigNames.clone();
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ConfigChangeListener#configurationChanged(java.util.List, java.util.Map)
     */
    @Override
    public void configurationChanged(List<String> changedSettingNames, Map<String, String> changedSettings) {
        reloadConfig();
    }

    /**
     * This will make it easier to handle a provider which might go away
     * because the classloader is gone.
     */
    private final ProviderHolder<CacheProvider> provider = new ProviderHolder<>();

    public CacheProvider getCacheProvider() {
        return provider.getProvider();
    }

    private void reloadProvider() {
        boolean current = (getCacheProvider() != null);
        CacheProvider cacheProvider = serviceManager
            .getServiceByName(CacheProvider.class.getName(), CacheProvider.class);
        provider.setProvider(cacheProvider);
        if (cacheProvider != null) {
            log.info("Cache Provider loaded: " + cacheProvider.getClass().getName());
        } else {
            if (current) {
                log.info("Cache Provider unloaded");
            }
        }
    }


    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ServiceChangeListener#notifyForTypes()
     */
    @Override
    public Class<?>[] notifyForTypes() {
        return new Class<?>[] {CacheProvider.class};
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ServiceChangeListener#serviceRegistered(java.lang.String, java.lang.Object, java
     * .util.List)
     */
    @Override
    public void serviceRegistered(String serviceName, Object service, List<Class<?>> implementedTypes) {
        provider.setProvider((CacheProvider) service);
    }

    /* (non-Javadoc)
     * @see org.dspace.kernel.mixins.ServiceChangeListener#serviceUnregistered(java.lang.String, java.lang.Object)
     */
    @Override
    public void serviceUnregistered(String serviceName, Object service) {
        provider.setProvider(null);
    }

    @PostConstruct
    public void init() {
        log.info("init()");
        // get settings
        reloadConfig();

        // get all caches out of the cachemanager and load them into the cache list
        List<Ehcache> ehcaches = getAllEhCaches(false);
        for (Ehcache ehcache : ehcaches) {
            EhcacheCache cache = new EhcacheCache(ehcache, null);
            cacheRecord.put(cache.getName(), cache);
        }

        // load provider
        reloadProvider();

        if (requestService != null) {
            requestService.registerRequestInterceptor(new CachingServiceRequestInterceptor());
        }

        log.info("Caching service initialized:\n" + getStatus(null));
    }

    @PreDestroy
    public void shutdown() {
        log.info("destroy()");
        // for some reason this causes lots of errors so not using it for now -AZ
        //ehCacheManagementService.dispose();
        try {
            cacheRecord.clear();
        } catch (RuntimeException e) {
            // whatever
        }
        try {
            requestCachesMap.clear();
        } catch (RuntimeException e) {
            // whatever
        }
        try {
            cacheManager.removalAll();
        } catch (RuntimeException e) {
            // whatever
        }
        try {
            cacheManager.shutdown();
        } catch (RuntimeException e) {
            // whatever
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.services.CachingService#destroyCache(java.lang.String)
     */
    @Override
    public void destroyCache(String cacheName) {
        if (cacheName == null || "".equals(cacheName)) {
            throw new IllegalArgumentException("cacheName cannot be null or empty string");
        }

        // handle provider first
        if (getCacheProvider() != null) {
            try {
                getCacheProvider().destroyCache(cacheName);
            } catch (Exception e) {
                log.warn("Failure in provider (" + getCacheProvider() + "): " + e.getMessage());
            }
        }

        EhcacheCache cache = cacheRecord.get(cacheName);
        if (cache != null) {
            cacheManager.removeCache(cacheName);
            cacheRecord.remove(cacheName);
        } else {
            Map<String, MapCache> caches = getRequestCaches();
            if (caches != null) {
                caches.remove(cacheName);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.dspace.services.CachingService#getCache(java.lang.String, org.dspace.services.model.CacheConfig)
     */
    @Override
    public Cache getCache(String cacheName, CacheConfig cacheConfig) {
        Cache cache = null;

        if (cacheName == null || "".equals(cacheName)) {
            throw new IllegalArgumentException("cacheName cannot be null or empty string");
        }

        if (cacheConfig != null && CacheScope.REQUEST.equals(cacheConfig.getCacheScope())) {
            Map<String, MapCache> caches = getRequestCaches();
            if (caches != null) {
                cache = caches.get(cacheName);
            }

            if (cache == null) {
                cache = instantiateMapCache(cacheName, cacheConfig);
            }
        } else {
            // find the cache in the records if possible
            cache = this.cacheRecord.get(cacheName);

            // handle provider
            if (cache == null && getCacheProvider() != null) {
                try {
                    cache = getCacheProvider().getCache(cacheName, cacheConfig);
                } catch (Exception e) {
                    log.warn("Failure in provider (" + getCacheProvider() + "): " + e.getMessage());
                }
            }

            if (cache == null) {
                cache = instantiateEhCache(cacheName, cacheConfig);
            }
        }

        return cache;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.CachingService#getCaches()
     */
    @Override
    public List<Cache> getCaches() {
        List<Cache> caches = new ArrayList<>(this.cacheRecord.values());
        if (getCacheProvider() != null) {
            try {
                caches.addAll(getCacheProvider().getCaches());
            } catch (Exception e) {
                log.warn("Failure in provider (" + getCacheProvider() + "): " + e.getMessage());
            }
        }
//        TODO implement reporting on request caches?
//        caches.addAll(this.requestMap.values());
        Collections.sort(caches, new NameComparator());
        return caches;
    }

    /* (non-Javadoc)
     * @see org.dspace.services.CachingService#getStatus(java.lang.String)
     */
    @Override
    public String getStatus(String cacheName) {
        final StringBuilder sb = new StringBuilder();

        if (cacheName == null || "".equals(cacheName)) {
            // add in overall memory stats
            sb.append("** Memory report\n");
            sb.append(" freeMemory: ").append(Runtime.getRuntime().freeMemory());
            sb.append("\n");
            sb.append(" totalMemory: ").append(Runtime.getRuntime().totalMemory());
            sb.append("\n");
            sb.append(" maxMemory: ").append(Runtime.getRuntime().maxMemory());
            sb.append("\n");

            // caches summary report
            List<Cache> allCaches = getCaches();
            sb.append("\n** Full report of all known caches (").append(allCaches.size()).append("):\n");
            for (Cache cache : allCaches) {
                sb.append(" * ");
                sb.append(cache.toString());
                sb.append("\n");
                if (cache instanceof EhcacheCache) {
                    Ehcache ehcache = ((EhcacheCache) cache).getCache();
                    sb.append(generateCacheStats(ehcache));
                    sb.append("\n");
                }
            }
        } else {
            // report for a single cache
            sb.append("\n** Report for cache (").append(cacheName).append("):\n");
            Cache cache = this.cacheRecord.get(cacheName);
            if (cache == null) {
                Map<String, MapCache> caches = getRequestCaches();
                if (caches != null) {
                    cache = caches.get(cacheName);
                }
            }
            if (cache == null) {
                sb.append(" * Could not find cache by this name: ").append(cacheName);
            } else {
                sb.append(" * ");
                sb.append(cache.toString());
                sb.append("\n");
                if (cache instanceof EhcacheCache) {
                    Ehcache ehcache = ((EhcacheCache) cache).getCache();
                    sb.append(generateCacheStats(ehcache));
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see org.dspace.services.CachingService#resetCaches()
     */
    @Override
    public void resetCaches() {
        log.debug("resetCaches()");

        List<Cache> allCaches = getCaches();
        for (Cache cache : allCaches) {
            cache.clear();
        }

        if (getCacheProvider() != null) {
            try {
                getCacheProvider().resetCaches();
            } catch (Exception e) {
                log.warn("Failure in provider (" + getCacheProvider() + "): " + e.getMessage());
            }
        }

        System.runFinalization(); // force the JVM to try to clean up any remaining objects
        // DO NOT CALL System.gc() here or I will have you shot -AZ

        log.info("doReset(): Memory Recovery to: " + Runtime.getRuntime().freeMemory());
    }

    /**
     * Return all caches from the CacheManager.
     *
     * @param sorted if true then sort by name
     * @return the list of all known ehcaches
     */
    protected List<Ehcache> getAllEhCaches(boolean sorted) {
        log.debug("getAllCaches()");

        final String[] cacheNames = cacheManager.getCacheNames();
        if (sorted) {
            Arrays.sort(cacheNames);
        }
        final List<Ehcache> caches = new ArrayList<>(cacheNames.length);
        for (String cacheName : cacheNames) {
            caches.add(cacheManager.getEhcache(cacheName));
        }
        return caches;
    }

    /**
     * Create an EhcacheCache (and the associated EhCache) using the
     * supplied name (with default settings), or get the cache out of
     * Spring or the current configured cache.
     * <p>
     * This expects that the cacheRecord has already been checked and
     * will not check it again.
     * <p>
     * Will proceed in this order:
     * <ol>
     * <li>Attempt to load a bean with the name of the cache</li>
     * <li>Attempt to load cache from caching system</li>
     * <li>Create a new cache by this name</li>
     * <li>Put the cache in the cache record</li>
     * </ol>
     *
     * @param cacheName   the name of the cache
     * @param cacheConfig the config for this cache
     * @return a cache instance
     */
    protected EhcacheCache instantiateEhCache(String cacheName, CacheConfig cacheConfig) {
        if (log.isDebugEnabled()) {
            log.debug("instantiateEhCache(String " + cacheName + ")");
        }

        if (cacheName == null || "".equals(cacheName)) {
            throw new IllegalArgumentException("String cacheName must not be null or empty!");
        }

        // try to locate a named cache in the service manager
        Ehcache ehcache = serviceManager.getServiceByName(cacheName, Ehcache.class);

        // try to locate the cache in the cacheManager by name
        if (ehcache == null) {
            // if this cache name is created or already in use then we just get it
            if (!cacheManager.cacheExists(cacheName)) {
                // did not find the cache
                if (cacheConfig == null) {
                    cacheManager.addCache(cacheName); // create a new cache using ehcache defaults
                } else {
                    if (useClustering) {
                        // TODO
                        throw new UnsupportedOperationException("Still need to do this");
                    } else {
                        cacheManager.addCache(cacheName); // create a new cache using ehcache defaults
                    }
                }
                log.info("Created new Cache (from default settings): " + cacheName);
            }
            ehcache = cacheManager.getEhcache(cacheName);
        }
        // wrap the ehcache in the cache impl
        EhcacheCache cache = new EhcacheCache(ehcache, cacheConfig);
        cacheRecord.put(cacheName, cache);
        return cache;
    }

    /**
     * Create a thread map cache using the supplied name with supplied
     * settings.
     * <p>
     * This expects that the cacheRecord has already been checked and
     * will not check it again.  It also places the cache into the
     * request map.
     *
     * @param cacheName   the name of the cache
     * @param cacheConfig the config for this cache
     * @return a cache instance
     */
    protected MapCache instantiateMapCache(String cacheName, CacheConfig cacheConfig) {
        if (log.isDebugEnabled()) {
            log.debug("instantiateMapCache(String " + cacheName + ")");
        }

        if (cacheName == null || "".equals(cacheName)) {
            throw new IllegalArgumentException("String cacheName must not be null or empty!");
        }

        // check for existing cache
        MapCache cache = null;
        CacheScope scope = CacheScope.REQUEST;
        if (cacheConfig != null) {
            scope = cacheConfig.getCacheScope();
        }

        Map<String, MapCache> caches = getRequestCaches();
        if (caches != null) {
            if (CacheScope.REQUEST.equals(scope)) {
                cache = caches.get(cacheName);
            }

            if (cache == null) {
                cache = new MapCache(cacheName, cacheConfig);
                // place cache into the right TL
                if (CacheScope.REQUEST.equals(scope)) {
                    caches.put(cacheName, cache);
                }
            }
        }

        return cache;
    }

    /**
     * Generate some stats for this cache.
     * Note that this is not cheap so do not use it very often.
     *
     * @param cache an Ehcache
     * @return the stats of this cache as a string
     */
    protected static String generateCacheStats(Ehcache cache) {
        StringBuilder sb = new StringBuilder();
        sb.append(cache.getName()).append(":");
        // this will make this costly but it is important to get accurate settings
        cache.setStatisticsAccuracy(Statistics.STATISTICS_ACCURACY_GUARANTEED);
        Statistics stats = cache.getStatistics();
        final long memSize = cache.getMemoryStoreSize();
        final long diskSize = cache.getDiskStoreSize();
        final long size = memSize + diskSize;
        final long hits = stats.getCacheHits();
        final long misses = stats.getCacheMisses();
        final String hitPercentage = ((hits + misses) > 0) ? ((100l * hits) / (hits + misses)) + "%" : "N/A";
        final String missPercentage = ((hits + misses) > 0) ? ((100l * misses) / (hits + misses)) + "%" : "N/A";
        sb.append("  Size: ").append(size).append(" [memory:").append(memSize).append(", disk:").append(diskSize)
          .append("]");
        sb.append(",  Hits: ").append(hits).append(" [memory:").append(stats.getInMemoryHits()).append(", disk:")
          .append(stats.getOnDiskHits()).append("] (").append(hitPercentage).append(")");
        sb.append(",  Misses: ").append(misses).append(" (").append(missPercentage).append(")");
        return sb.toString();
    }

    /**
     * Compare two Cache objects by name.
     */
    public static final class NameComparator implements Comparator<Cache>, Serializable {
        public static final long serialVersionUID = 1l;

        @Override
        public int compare(Cache o1, Cache o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

    private class CachingServiceRequestInterceptor implements RequestInterceptor {

        @Override
        public void onStart(String requestId) {
            if (requestId != null) {
                Map<String, MapCache> requestCaches = requestCachesMap.get(requestId);
                if (requestCaches == null) {
                    requestCaches = new HashMap<>();
                    requestCachesMap.put(requestId, requestCaches);
                }
            }
        }

        @Override
        public void onEnd(String requestId, boolean succeeded, Exception failure) {
            if (requestId != null) {
                requestCachesMap.remove(requestId);
            }
        }

        @Override
        public int getOrder() {
            return 1;
        }
    }
}
