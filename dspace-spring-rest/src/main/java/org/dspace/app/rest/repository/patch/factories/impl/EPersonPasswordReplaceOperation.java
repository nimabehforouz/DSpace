/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.factories.impl;

import org.dspace.app.rest.exception.PatchBadRequestException;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.model.patch.Operation;
import org.springframework.stereotype.Component;

/**
 * Implementation for EPerson password patches.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.url}/api/epersons/eperson/<:id-eperson> -H "
 * Content-Type: application/json" -d '[{ "op": "replace", "path": "
 * /password", "value": "newpassword"]'
 * </code>
 *
 * @author Michael Spalti
 */
@Component
public class EPersonPasswordReplaceOperation extends ReplacePatchOperation<EPersonRest, String> {

    @Override
    public EPersonRest perform( EPersonRest resource, Operation operation)
            throws PatchBadRequestException {

        return replace(resource, operation);
    }

    @Override
    EPersonRest replace(EPersonRest eperson, Operation operation) throws PatchBadRequestException {

        // The value must not be null.
        checkOperationValue(operation.getValue());
        /*
         * TODO: the password field in eperson rest model is always null (initially) since
         * the password value is not set by eperson converter.fromModel() method. So for now,
         * not calling checkModelForExistingValue() or throwing error when replacing what
         * appears to be a non-existent value.
         */
        eperson.setPassword((String) operation.getValue());
        return eperson;
    }

    @Override
    protected Class<String[]> getArrayClassForEvaluation() {
        return String[].class;
    }

    @Override
    protected Class<String> getClassForEvaluation() {
        return String.class;
    }
}
