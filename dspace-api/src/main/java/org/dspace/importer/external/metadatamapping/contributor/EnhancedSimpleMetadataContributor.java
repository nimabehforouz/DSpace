package org.dspace.importer.external.metadatamapping.contributor;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.components.dto.PlainMetadataKeyValueItem;
import org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto;


/**
 * This class implements functionalities to handle common situation regarding plain metadata.
 * In some scenario, like csv or tsv, the format don't allow lists.
 * We can use this MetadataContribut to parse a given plain metadata and split it into
 * related list, based on the delimiter. No escape character is present.
 * Default values are comma (,) for delimiter, and double quote (") for escape character
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class EnhancedSimpleMetadataContributor extends SimpleMetadataContributor {

    private char delimiter = ',';

    private char escape = '"';

    private boolean useEnhancer;

    /**
     * This method could be used to set the delimiter used during parse
     * If no delimiter is set, comma will be used
     */
    public void setDelimiter(int delimiter) {
        this.delimiter = (char)delimiter;
    }

    /**
     * This method could be used to get the delimiter used in this class
     */
    public char getDelimiter() {
        return delimiter;
    }

    /**
     * Method to inject the escape character.
     * This must be the ASCII integer
     * related to the char.
     * In example, 9 for tab, 44 for comma
     * If no escape is set, double quote will be used
     */
    public void setEscape(int escape) {
        this.escape = (char)escape;
    }

    /**
     * Method to get the escape character.
     * 
     */
    public char getEscape() {
        return escape;
    }

    /**
     * Method to set up the enhancer. If set to false, enhancing will be not used
     * In this case, the metadata value will
     * As default, it is valued as false
     * 
     */
    public void setUseEnhancer(boolean useEnhancer) {
        this.useEnhancer = useEnhancer;
    }

    /**
     * 
     * @return true if the enhancer is set up, false otherwise.
     */
    public boolean isUseEnhancer() {
        return useEnhancer;
    }

    @Override
    public Collection<MetadatumDTO> contributeMetadata(PlainMetadataSourceDto t) {
        Collection<MetadatumDTO> values = null;
        if (!useEnhancer) {
            values = super.contributeMetadata(t);
        } else {
            values = new LinkedList<>();
            for (PlainMetadataKeyValueItem metadatum : t.getMetadata()) {
                if (getKey().equals(metadatum.getKey())) {
                    String[] splitted = splitToRecord(metadatum.getValue());
                    for (String value : splitted) {
                        MetadatumDTO dcValue = new MetadatumDTO();
                        dcValue.setValue(value);
                        dcValue.setElement(getField().getElement());
                        dcValue.setQualifier(getField().getQualifier());
                        dcValue.setSchema(getField().getSchema());
                        values.add(dcValue);
                    }
                }
            }
        }
        return values;
    }

    private String[] splitToRecord(String value) {
        List<String[]> rows;
        try (CSVReader csvReader = new CSVReader(new StringReader(value),
            delimiter, escape);) {
            rows = csvReader.readAll();
        } catch (IOException e) {
            //fallback, use the inpu as value
            return new String[] { value };
        }
        //must be one row
        return rows.get(0);
    }

}
