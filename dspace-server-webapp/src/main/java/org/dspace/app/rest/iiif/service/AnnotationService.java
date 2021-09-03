/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.util.UUID;

public interface AnnotationService {

    void initializeQuery(String endpoint, String manifestId, boolean validationEnabled);

    String getSolrSearchResponse(UUID uuid, String query);

}
