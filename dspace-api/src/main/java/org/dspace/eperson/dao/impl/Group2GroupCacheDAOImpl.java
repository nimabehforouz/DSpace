/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dao.impl;

import org.dspace.core.Context;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.eperson.Group;
import org.dspace.eperson.Group2GroupCache;
import org.dspace.eperson.Group2GroupCache_;
import org.dspace.eperson.dao.Group2GroupCacheDAO;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.hibernate.Criteria;
import javax.persistence.Query;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Hibernate implementation of the Database Access Object interface class for the Group2GroupCache object.
 * This class is responsible for all database calls for the Group2GroupCache object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class Group2GroupCacheDAOImpl extends AbstractHibernateDAO<Group2GroupCache> implements Group2GroupCacheDAO
{
    protected Group2GroupCacheDAOImpl()
    {
        super();
    }

    @Override
    public List<Group2GroupCache> findByParent(Context context, Group group) throws SQLException {
//        Criteria criteria = createCriteria(context, Group2GroupCache.class);
//        criteria.add(Restrictions.eq("parent.id", group.getID()));
//        criteria.setCacheable(true);
//
//        return list(criteria);
//
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Group2GroupCache.class);
        Root<Group2GroupCache> group2GroupCacheRoot = criteriaQuery.from(Group2GroupCache.class);
        criteriaQuery.select(group2GroupCacheRoot);
        criteriaQuery.where(criteriaBuilder.equal(group2GroupCacheRoot.get(Group2GroupCache_.parent), group));
        return list(context, criteriaQuery, true, Group2GroupCache.class, -1, -1);
    }

    @Override
    public List<Group2GroupCache> findByChildren(Context context, Iterable<Group> groups) throws SQLException {
//        Criteria criteria = createCriteria(context, Group2GroupCache.class);
//
//        Disjunction orDisjunction = Restrictions.or();
//        for(Group group : groups)
//        {
//            orDisjunction.add(Restrictions.eq("child.id", group.getID()));
//        }
//
//        criteria.add(orDisjunction);
//        criteria.setCacheable(true);
//
//        return list(criteria);
//
//
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Group2GroupCache.class);
        Root<Group2GroupCache> group2GroupCacheRoot = criteriaQuery.from(Group2GroupCache.class);
        List<Predicate> eqPredicates = new LinkedList<>();
        for(Group group : groups){
            eqPredicates.add(criteriaBuilder.equal(group2GroupCacheRoot.get(Group2GroupCache_.child), group));
        }
        Predicate orPredicate = criteriaBuilder.or(eqPredicates.toArray(new Predicate[]{}));
        criteriaQuery.select(group2GroupCacheRoot);
        criteriaQuery.where(orPredicate);
        return list(context, criteriaQuery, true, Group2GroupCache.class, -1, -1);
    }

    @Override
    public Group2GroupCache findByParentAndChild(Context context, Group parent, Group child) throws SQLException {
        Query query = createQuery(context,
                "FROM Group2GroupCache g WHERE g.parent = :parentGroup AND g.child = :childGroup");

        query.setParameter("parentGroup", parent);
        query.setParameter("childGroup", child);
        query.setHint("org.hibernate.cacheable", Boolean.TRUE);

        return singleResult(query);
    }

    @Override
    public Group2GroupCache find(Context context, Group parent, Group child) throws SQLException {
//        Criteria criteria = createCriteria(context, Group2GroupCache.class);
//        criteria.add(Restrictions.eq("parent.id", parent.getID()));
//        criteria.add(Restrictions.eq("child.id", child.getID()));
//        criteria.setCacheable(true);
//        return uniqueResult(criteria);
//
        CriteriaBuilder criteriaBuilder = getCriteriaBuilder(context);
        CriteriaQuery criteriaQuery = getCriteriaQuery(criteriaBuilder, Group2GroupCache.class);
        Root<Group2GroupCache> group2GroupCacheRoot = criteriaQuery.from(Group2GroupCache.class);
        criteriaQuery.select(group2GroupCacheRoot);
        criteriaQuery.where(criteriaBuilder.and(criteriaBuilder.equal(group2GroupCacheRoot.get(Group2GroupCache_.parent), parent),
                                                criteriaBuilder.equal(group2GroupCacheRoot.get(Group2GroupCache_.child), child)
                                                )
                            );
        return uniqueResult(context, criteriaQuery, true, Group2GroupCache.class, -1, -1);
    }

    @Override
    public void deleteAll(Context context) throws SQLException {
        createQuery(context, "delete from Group2GroupCache").executeUpdate();
    }
}
