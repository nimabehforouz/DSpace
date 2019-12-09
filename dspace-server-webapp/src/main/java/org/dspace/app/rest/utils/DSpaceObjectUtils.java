package org.dspace.app.rest.utils;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * 
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
@Component
public class DSpaceObjectUtils {

    @Autowired
    private ContentServiceFactory contentServiceFactory;

    public DSpaceObject findDSpaceObject(Context context, UUID uuid) throws SQLException {
        for (DSpaceObjectService<? extends DSpaceObject> dSpaceObjectService :
                              contentServiceFactory.getDSpaceObjectServices()) {
            DSpaceObject dso = dSpaceObjectService.find(context, uuid);
            if (dso != null) {
                return dso;
            }
        }
        return null;
    }

}
