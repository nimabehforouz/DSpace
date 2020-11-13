/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.migration;

import java.io.File;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Script (config {@link SubmissionFormsMigrationScriptConfiguration}) to transform the old input-forms.xml and
 * item-submission.xml via XSLT (xsl files in dspace/config/migration) into respectively the new submission-forms.xsl
 * and item-submissions.xsl files
 *
 * @author Maria Verdonck (Atmire) on 13/11/2020
 */
public class SubmissionFormsMigration extends DSpaceRunnable<SubmissionFormsMigrationScriptConfiguration> {

    private boolean help = false;
    private String inputFormsFilePath = null;
    private String itemSubmissionsFilePath = null;

    private static final String PATH_OUT_CONFIG =
        DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir")
        + File.separator + "config";

    private static final String PATH_XSL_SUBMISSION_FORMS =
        PATH_OUT_CONFIG + File.separator + "migration" + File.separator + "submission-forms.xsl";
    private static final String PATH_XSL_ITEM_SUBMISSION =
        PATH_OUT_CONFIG + File.separator + "migration" + File.separator + "item-submissions.xsl";

    private static final String PATH_OUT_INPUT_FORMS = PATH_OUT_CONFIG + File.separator + "submission-forms.xml";
    private static final String PATH_OUT_ITEM_SUBMISSION = PATH_OUT_CONFIG + File.separator + "item-submission.xml";

    /**
     * We need to force this, because some dependency elsewhere interferes.
     */
    private static final String TRANSFORMER_FACTORY_CLASS
        = "org.apache.xalan.processor.TransformerFactoryImpl";

    @Override
    public void internalRun() throws TransformerException {
        if (help) {
            printHelp();
            return;
        }
        if (this.inputFormsFilePath != null) {
            this.transformToSubmissionForms(this.inputFormsFilePath);
        }
        if (this.itemSubmissionsFilePath != null) {
            this.transformToItemSubmission(this.itemSubmissionsFilePath);
        }
    }

    private void transformToSubmissionForms(String inputFormsFilePath) throws TransformerException {
        this.transform(inputFormsFilePath, PATH_XSL_SUBMISSION_FORMS, PATH_OUT_INPUT_FORMS);
    }

    private void transformToItemSubmission(String itemSubmissionsFilePath) throws TransformerException {
        this.transform(itemSubmissionsFilePath, PATH_XSL_ITEM_SUBMISSION, PATH_OUT_ITEM_SUBMISSION);
    }

    /**
     * Transforms an input xml file to an output xml file with the given xsl file
     *
     * @param sourceFilePath Input XML
     * @param xsltFilePath   Transforming XSL
     * @param outputPath     Output XML
     */
    private void transform(String sourceFilePath, String xsltFilePath, String outputPath)
        throws TransformerException {
        super.handler
            .logInfo("Transforming " + sourceFilePath + " with xsl: " + xsltFilePath + " to output: " + outputPath);

        Source xmlSource = new StreamSource(new File(sourceFilePath));
        Source xsltSource = new StreamSource(new File(xsltFilePath));
        Result result = new StreamResult(new File(outputPath));

        // create an instance of TransformerFactory
        TransformerFactory transformerFactory = TransformerFactory.newInstance(
            TRANSFORMER_FACTORY_CLASS, null);

        Transformer trans;
        try {
            trans = transformerFactory.newTransformer(xsltSource);
        } catch (TransformerConfigurationException e) {
            super.handler.logError("Error: the stylesheet at '" + xsltFilePath + "' couldn't be used");
            throw e;
        }

        try {
            trans.transform(xmlSource, result);
        } catch (Throwable t) {
            super.handler.logError("Error: couldn't convert the metadata file at '" + sourceFilePath);
            throw t;
        }
    }

    @Override
    public void setup() {
        if (commandLine.hasOption('h')) {
            help = true;
            return;
        }
        if (commandLine.hasOption('f')) {
            inputFormsFilePath = commandLine.getOptionValue('f');
            checkIfValidXMLFile(inputFormsFilePath);
        }
        if (commandLine.hasOption('s')) {
            itemSubmissionsFilePath = commandLine.getOptionValue('s');
            checkIfValidXMLFile(itemSubmissionsFilePath);
        }
        if (!(commandLine.hasOption('s') || commandLine.hasOption('f'))) {
            this.throwIllegalArgumentException("Please fill in either -f <source-input-forms-path> or -s " +
                                               "<source-item-submissions-path>; or both.");
        }
    }

    private void checkIfValidXMLFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            this.throwIllegalArgumentException("There is no file at path: " + filePath);
        }
        if (!file.isFile() && file.isDirectory()) {
            this.throwIllegalArgumentException("This is a dir, not a file: " + filePath);
        }
        if (!file.getName().endsWith(".xml")) {
            this.throwIllegalArgumentException("This is not an XML file (doesn't end in .xml): " + filePath);
        }
    }

    private void throwIllegalArgumentException(String message) {
        super.handler.logError(message);
        throw new IllegalArgumentException(message);
    }

    @Override
    public SubmissionFormsMigrationScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("submission-forms-migrate",
            SubmissionFormsMigrationScriptConfiguration.class);
    }
}
