/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.stereotype.Component;

/**
 * Implementation for EPerson netid patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /netid", "value": "newNetId"]'
 * </code>
 */
@Component
public class EPersonNetidReplaceOperation<R> extends PatchOperation<R> {

    /**
     * Path in json body of patch that uses this operation
     */
    private static final String OPERATION_PATH_NETID = "/netid";

    @Override
    public R perform(Context context, R object, Operation operation) {
        checkOperationValue(operation.getValue());
        if (supports(object, operation.getPath())) {
            EPerson eperson = (EPerson) object;
            eperson.setNetid((String) operation.getValue());
            return object;
        } else {
            throw new DSpaceBadRequestException("EPersonNetidReplaceOperation does not support this operation");
        }
    }

    @Override
    public boolean supports(R objectToMatch, String path) {
        return (objectToMatch instanceof EPerson && path.trim().equalsIgnoreCase(OPERATION_PATH_NETID));
    }
}
