/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.dao.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.persistence.Query;

import org.dspace.app.orcid.OrcidHistory;
import org.dspace.app.orcid.dao.OrcidHistoryDAO;
import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;

/**
 * Implementation of {@link OrcidHistoryDAO}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@SuppressWarnings("unchecked")
public class OrcidHistoryDAOImpl extends AbstractHibernateDAO<OrcidHistory> implements OrcidHistoryDAO {

    @Override
    public List<OrcidHistory> findByOwnerAndEntity(Context context, UUID ownerId, UUID entityId) throws SQLException {
        Query query = createQuery(context, "FROM OrcidHistory WHERE owner.id = :ownerId AND entity.id = :entityId ");
        query.setParameter("ownerId", ownerId);
        query.setParameter("entityId", entityId);
        return query.getResultList();
    }

    @Override
    public List<OrcidHistory> findByOwnerOrEntity(Context context, Item item) throws SQLException {
        Query query = createQuery(context, "FROM OrcidHistory WHERE owner.id = :itemId OR entity.id = :itemId");
        query.setParameter("itemId", item.getID());
        return query.getResultList();
    }

    @Override
    public List<OrcidHistory> findByEntity(Context context, Item entity) throws SQLException {
        Query query = createQuery(context, "FROM OrcidHistory WHERE entity.id = :entityId ");
        query.setParameter("entityId", entity.getID());
        return query.getResultList();
    }

    @Override
    public List<OrcidHistory> findSuccessfullyRecordsByEntityAndType(Context context, Item entity,
        String recordType) throws SQLException {
        Query query = createQuery(context, "FROM OrcidHistory WHERE entity = :entity AND recordType = :type "
            + "AND status BETWEEN 200 AND 300");
        query.setParameter("entity", entity);
        query.setParameter("type", recordType);
        return query.getResultList();
    }

}
