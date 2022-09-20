/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.dspace.app.itemexport.factory.ItemExportServiceFactory;
import org.dspace.app.itemexport.service.ItemExportService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * CLI variant for the {@link ItemExport} class.
 * This was done to specify the specific behaviors for the CLI.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemExportCLI extends ItemExport {

    @Override
    public void setup() throws ParseException {
        help = commandLine.hasOption('h');

        if (commandLine.hasOption('t')) { // type
            typeString = commandLine.getOptionValue('t');

            if ("ITEM".equals(typeString)) {
                type = Constants.ITEM;
            } else if ("COLLECTION".equals(typeString)) {
                type = Constants.COLLECTION;
            }
        }

        if (commandLine.hasOption('i')) { // id
            idString = commandLine.getOptionValue('i');
        }

        if (commandLine.hasOption('d')) { // dest
            destDirName = commandLine.getOptionValue('d');
        }

        if (commandLine.hasOption('n')) { // number
            seqStart = Integer.parseInt(commandLine.getOptionValue('n'));
        }

        if (commandLine.hasOption('m')) { // number
            migrate = true;
        }

        if (commandLine.hasOption('z')) {
            zip = true;
            zipFileName = commandLine.getOptionValue('z');
        }

        if (commandLine.hasOption('x')) {
            excludeBitstreams = true;
        }
    }

    @Override
    public void internalRun() throws Exception {
        if (help) {
            printHelp();
            handler.logInfo("full collection: ItemExport -t COLLECTION -i ID -d dest -n number");
            handler.logInfo("single item: ItemExport -t ITEM -i ID -d dest -n number");
            return;
        }

        // validation
        if (type == -1) {
            handler.logError("The type must be either COLLECTION or ITEM (run with -h flag for details)");
            throw new UnsupportedOperationException("The type must be either COLLECTION or ITEM");
        }

        if (destDirName == null) {
            handler.logError("The destination directory must be set (run with -h flag for details)");
            throw new UnsupportedOperationException("The destination directory must be set");
        }

        if (seqStart == -1) {
            handler.logError("The sequence start number must be set (run with -h flag for details)");
            throw new UnsupportedOperationException("The sequence start number must be set");
        }

        if (idString == null) {
            handler.logError("The ID must be set to either a database ID or a handle (run with -h flag for details)");
            throw new UnsupportedOperationException("The ID must be set to either a database ID or a handle");
        }

        Context context = new Context(Context.Mode.READ_ONLY);
        context.turnOffAuthorisationSystem();

        if (type == Constants.ITEM) {
            // first, is myIDString a handle?
            if (idString.indexOf('/') != -1) {
                item = (Item) handleService.resolveToObject(context, idString);

                if ((item == null) || (item.getType() != Constants.ITEM)) {
                    item = null;
                }
            } else {
                item = itemService.find(context, UUID.fromString(idString));
            }

            if (item == null) {
                handler.logError("The item cannot be found: " + idString + " (run with -h flag for details)");
                throw new UnsupportedOperationException("The item cannot be found: " + idString);
            }
        } else {
            if (idString.indexOf('/') != -1) {
                // has a / must be a handle
                collection = (Collection) handleService.resolveToObject(context,
                                                                          idString);

                // ensure it's a collection
                if ((collection == null)
                    || (collection.getType() != Constants.COLLECTION)) {
                    collection = null;
                }
            } else if (idString != null) {
                collection = collectionService.find(context, UUID.fromString(idString));
            }

            if (collection == null) {
                handler.logError("The collection cannot be found: " + idString + " (run with -h flag for details)");
                throw new UnsupportedOperationException("The collection cannot be found: " + idString);
            }
        }

        ItemExportService itemExportService = ItemExportServiceFactory.getInstance()
                .getItemExportService();
        try {
            if (zip) {
                Iterator<Item> items;
                if (item != null) {
                    List<Item> myItems = new ArrayList<>();
                    myItems.add(item);
                    items = myItems.iterator();
                } else {
                    handler.logInfo("Exporting from collection: " + idString);
                    items = itemService.findByCollection(context, collection);
                }
                itemExportService.exportAsZip(context, items, destDirName, zipFileName,
                        seqStart, migrate, excludeBitstreams);
            } else {
                if (item != null) {
                    // it's only a single item
                    itemExportService
                        .exportItem(context, Collections.singletonList(item).iterator(), destDirName,
                                seqStart, migrate, excludeBitstreams);
                } else {
                    handler.logInfo("Exporting from collection: " + idString);

                    // it's a collection, so do a bunch of items
                    Iterator<Item> i = itemService.findByCollection(context, collection);
                    itemExportService.exportItem(context, i, destDirName, seqStart, migrate, excludeBitstreams);
                }
            }

            context.complete();
        } catch (Exception e) {
            context.abort();
            throw new Exception(e);
        }
    }
}
