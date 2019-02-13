/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.xoai.solr;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.xoai.solr.exceptions.DSpaceSolrException;
import org.dspace.xoai.solr.exceptions.SolrSearchEmptyException;

/**
 * @author Lyncode Development Team (dspace at lyncode dot com)
 */
public class DSpaceSolrSearch {

    /**
     * Default constructor
     */
    private DSpaceSolrSearch() { }

    public static SolrDocumentList query(SolrClient server, SolrQuery solrParams)
        throws DSpaceSolrException, IOException {
        solrParams.addSort("item.id", ORDER.asc);
        // No longer can set default search field in the schema
        if (null == solrParams.get("df")) {
            solrParams.set("df", "item.handle");
        }
        // No longer can set default match operator in the schema
        if (null == solrParams.get("q.op")) {
            solrParams.set("q.op", "OR");
        }
        try {
            QueryResponse response = server.query(solrParams);
            return response.getResults();
        } catch (SolrServerException ex) {
            throw new DSpaceSolrException(ex.getMessage(), ex);
        }
    }

    public static SolrDocument querySingle(SolrClient server, SolrQuery solrParams)
        throws SolrSearchEmptyException, IOException {
        solrParams.addSort("item.id", ORDER.asc);
        if (null == solrParams.get("df")) {
            solrParams.set("df", "item.handle");
        }
        // No longer can set default match operator in the schema
        if (null == solrParams.get("q.op")) {
            solrParams.set("q.op", "OR");
        }
        try {
        // No longer can set default search field in the schema
            QueryResponse response = server.query(solrParams);
            if (response.getResults().getNumFound() > 0) {
                return response.getResults().get(0);
            } else {
                throw new SolrSearchEmptyException();
            }
        } catch (SolrServerException ex) {
            throw new SolrSearchEmptyException(ex.getMessage(), ex);
        }
    }
}
