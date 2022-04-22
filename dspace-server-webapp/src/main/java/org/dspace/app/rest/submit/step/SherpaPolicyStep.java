/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.util.Objects;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.SherpaPolicy;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.sherpa.cache.SherpaCacheEvictBeanLocator;
import org.dspace.app.sherpa.cache.SherpaCacheEvictService;
import org.dspace.app.sherpa.v2.SHERPAResponse;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.web.ContextUtil;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class SherpaPolicyStep extends AbstractProcessingStep {

    @Override
    @SuppressWarnings("unchecked")
    public SherpaPolicy getData(SubmissionService submissionService, InProgressSubmission obj,
            SubmissionStepConfig config) throws Exception {
        Context context = ContextUtil.obtainCurrentRequestContext();
        SHERPAResponse response = sherpaSubmitService.searchRelatedJournals(context, obj.getItem());
        if (Objects.nonNull(response)) {
            SherpaPolicy result = new SherpaPolicy();
            result.setSherpaResponse(response);
            return result;
        }
        return null;
    }

    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest, InProgressSubmission source,
            Operation op, SubmissionStepConfig stepConf) throws Exception {
        String path = op.getPath();
        SherpaCacheEvictService sherpaCacheEvictService = SherpaCacheEvictBeanLocator.getSherpaCacheEvictService();
        if (path.contains(SHERPA_RETRIEVAL_TIME)) {
            sherpaCacheEvictService.evictCacheValues(context, source.getItem());
        }
    }

}