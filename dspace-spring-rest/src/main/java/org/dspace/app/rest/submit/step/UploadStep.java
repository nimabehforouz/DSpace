/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step;

import java.util.ArrayList;
import java.util.List;

import org.atteo.evo.inflector.English;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.CheckSumRest;
import org.dspace.app.rest.model.MetadataEntryRest;
import org.dspace.app.rest.model.step.DataUpload;
import org.dspace.app.rest.model.step.UploadBitstreamRest;
import org.dspace.app.rest.submit.AbstractRestProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.services.ConfigurationService;

/**
 * Upload step for DSpace Spring Rest. Expose information about the bitstream uploaded for the in progress submission. 
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public class UploadStep extends org.dspace.submit.step.UploadStep implements AbstractRestProcessingStep {
	
	@Override
	public DataUpload getData(WorkspaceItem obj, SubmissionStepConfig config) throws Exception {
		
		DataUpload result = new DataUpload(); 
		List<Bundle> bundles = itemService.getBundles(obj.getItem(), Constants.CONTENT_BUNDLE_NAME);
		for(Bundle bundle : bundles) {
			for(Bitstream source : bundle.getBitstreams()) {
				UploadBitstreamRest b = SubmissionService.buildUploadBitstream(configurationService, source);
				result.getFiles().add(b);
			}
		}
		return result; 
	}

}
