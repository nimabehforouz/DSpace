/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.BrowseIndexRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * This is the repository responsible to Browse Index Rest object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component(BrowseIndexRest.CATEGORY + "." + BrowseIndexRest.NAME)
public class BrowseIndexRestRepository extends DSpaceRestRepository<BrowseIndexRest, String> {

    @Autowired
    ConverterService converter;

    @Override
    public BrowseIndexRest findOne(Context context, String name) {
        BrowseIndexRest bi = null;
        BrowseIndex bix;
        try {
            bix = BrowseIndex.getBrowseIndex(name);
        } catch (BrowseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (bix != null) {
            bi = converter.toRest(bix, utils.obtainProjection());
        }
        return bi;
    }

    @Override
    public Page<BrowseIndexRest> findAll(Context context, Pageable pageable) {
        List<BrowseIndexRest> it = null;
        List<BrowseIndex> indexesList = new ArrayList<BrowseIndex>();
        int total = 0;
        try {
            BrowseIndex[] indexes = BrowseIndex.getBrowseIndices();
            total = indexes.length;
            for (BrowseIndex bix : indexes) {
                indexesList.add(bix);
            }
        } catch (BrowseException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Projection projection = utils.obtainProjection(true);
        Page<BrowseIndexRest> page = new PageImpl<>(indexesList, pageable, total)
                .map((object) -> converter.toRest(object, projection));
        return page;
    }

    @Override
    public Class<BrowseIndexRest> getDomainClass() {
        return BrowseIndexRest.class;
    }
}
