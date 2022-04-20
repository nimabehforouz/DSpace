/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemdbstatus;

import static org.dspace.discovery.indexobject.ItemIndexFactoryImpl.STATUS_FIELD;
import static org.dspace.discovery.indexobject.ItemIndexFactoryImpl.STATUS_FIELD_PREDB;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Optional;

import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.factory.IndexObjectFactoryFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.SolrUtils;
import org.dspace.utils.DSpace;

/**
 * {@link DSpaceRunnable} implementation to update solr items with "predb" status to either:
 * - Delete them from solr if they're not present in the database
 * - Remove their status if they're present in the database
 */
public class ItemDatabaseStatusCli extends DSpaceRunnable<ItemDatabaseStatusCliScriptConfiguration> {
    /* Log4j logger */
    private static final Logger log = Logger.getLogger(ItemDatabaseStatusCli.class);

    public static final String TIME_UNTIL_REINDEX_PROPERTY = "item-database-status.time-until-reindex";

    private IndexingService indexingService;
    private SolrSearchCore solrSearchCore;
    private IndexObjectFactoryFactory indexObjectServiceFactory;
    private ConfigurationService configurationService;

    private int timeUntilReindex = 0;
    private String maxTime;

    @Override
    public ItemDatabaseStatusCliScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager()
                .getServiceByName("item-database-status", ItemDatabaseStatusCliScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        indexingService = DSpaceServicesFactory.getInstance().getServiceManager()
                            .getServiceByName(IndexingService.class.getName(), IndexingService.class);
        solrSearchCore = DSpaceServicesFactory.getInstance().getServiceManager()
                            .getServiceByName(SolrSearchCore.class.getName(), SolrSearchCore.class);
        indexObjectServiceFactory = IndexObjectFactoryFactory.getInstance();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    @Override
    public void internalRun() throws Exception {
        logInfoAndOut("Starting Item Database Status update...");

        timeUntilReindex = getTimeUntilReindex();
        maxTime = getMaxTime();

        Context context = new Context();

        try {
            context.turnOffAuthorisationSystem();
            performStatusUpdate(context);
        } finally {
            context.restoreAuthSystemState();
            context.complete();
        }
    }

    private void performStatusUpdate(Context context) throws SearchServiceException, SolrServerException, IOException {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(STATUS_FIELD + ":" + STATUS_FIELD_PREDB);
        solrQuery.addFilterQuery(SearchUtils.RESOURCE_TYPE_FIELD + ":" + IndexableItem.TYPE);
        String dateRangeFilter = SearchUtils.LAST_INDEXED_FIELD + ":[* TO " + maxTime + "]";
        logDebugAndOut("Date range filter used; " + dateRangeFilter);
        solrQuery.addFilterQuery(dateRangeFilter);
        solrQuery.addField(SearchUtils.RESOURCE_ID_FIELD);
        solrQuery.addField(SearchUtils.RESOURCE_UNIQUE_ID);
        QueryResponse response = solrSearchCore.getSolr().query(solrQuery, solrSearchCore.REQUEST_METHOD);

        if (response != null) {
            logInfoAndOut(response.getResults().size() + " items found to process");

            for (SolrDocument doc : response.getResults()) {
                String uuid = (String) doc.getFirstValue(SearchUtils.RESOURCE_ID_FIELD);
                String uniqueId = (String) doc.getFirstValue(SearchUtils.RESOURCE_UNIQUE_ID);
                logDebugAndOut("Processing item with UUID: " + uuid);

                Optional<IndexableObject> indexableObject = Optional.empty();
                try {
                    indexableObject = indexObjectServiceFactory
                            .getIndexableObjectFactory(uniqueId).findIndexableObject(context, uuid);
                } catch (SQLException e) {
                    log.warn("An exception occurred when attempting to retrieve item with UUID \"" + uuid +
                            "\" from the database, removing related solr document", e);
                }

                try {
                    if (indexableObject.isPresent()) {
                        logDebugAndOut("Item exists in DB, updating solr document");
                        updateItem(context, indexableObject.get());
                    } else {
                        logDebugAndOut("Item doesn't exist in DB, removing solr document");
                        removeItem(context, uniqueId);
                    }
                } catch (SQLException | IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        indexingService.commit();
    }

    private void updateItem(Context context, IndexableObject indexableObject) throws SQLException {
        indexingService.indexContent(context, indexableObject, true);
    }

    private void removeItem(Context context, String uniqueId) throws IOException, SQLException {
        indexingService.unIndexContent(context, uniqueId);
    }

    private String getMaxTime() {
        Calendar cal = Calendar.getInstance();
        if (timeUntilReindex > 0) {
            cal.add(Calendar.MILLISECOND, -timeUntilReindex);
        }
        return SolrUtils.getDateFormatter().format(cal.getTime());
    }

    private int getTimeUntilReindex() {
        return configurationService.getIntProperty(TIME_UNTIL_REINDEX_PROPERTY, 0);
    }

    private void logInfoAndOut(String message) {
        log.info(message);
        System.out.println(message);
    }

    private void logDebugAndOut(String message) {
        log.debug(message);
        System.out.println(message);
    }
}
