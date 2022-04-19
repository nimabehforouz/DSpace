/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.dspace.content.Relationship.LatestVersionStatus;
import static org.dspace.content.Relationship.LatestVersionStatus.BOTH;
import static org.dspace.content.Relationship.LatestVersionStatus.LEFT_ONLY;
import static org.dspace.content.Relationship.LatestVersionStatus.RIGHT_ONLY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.content.virtual.Collected;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.versioning.Version;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersioningService;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class VersioningWithRelationshipsTest extends AbstractIntegrationTestWithDatabase {

    private final RelationshipService relationshipService =
        ContentServiceFactory.getInstance().getRelationshipService();
    private final VersioningService versioningService =
        VersionServiceFactory.getInstance().getVersionService();
    private final WorkspaceItemService workspaceItemService =
        ContentServiceFactory.getInstance().getWorkspaceItemService();
    private final InstallItemService installItemService =
        ContentServiceFactory.getInstance().getInstallItemService();
    private final ItemService itemService =
        ContentServiceFactory.getInstance().getItemService();
    private final MetadataValueService metadataValueService =
        ContentServiceFactory.getInstance().getMetadataValueService();

    protected Community community;
    protected Collection collection;
    protected EntityType publicationEntityType;
    protected EntityType personEntityType;
    protected EntityType projectEntityType;
    protected EntityType orgUnitEntityType;
    protected EntityType journalIssueEntityType;
    protected EntityType journalVolumeEntityType;
    protected RelationshipType isAuthorOfPublication;
    protected RelationshipType isProjectOfPublication;
    protected RelationshipType isOrgUnitOfPublication;
    protected RelationshipType isMemberOfProject;
    protected RelationshipType isMemberOfOrgUnit;
    protected RelationshipType isIssueOfJournalVolume;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        community = CommunityBuilder.createCommunity(context)
            .withName("community")
            .build();

        collection = CollectionBuilder.createCollection(context, community)
            .withName("collection")
            .build();

        publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication")
            .build();

        personEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Person")
            .build();

        projectEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Project")
            .build();

        orgUnitEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit")
            .build();

        journalIssueEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalIssue")
            .build();

        journalVolumeEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalVolume")
            .build();

        isAuthorOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, personEntityType,
                "isAuthorOfPublication", "isPublicationOfAuthor",
                null, null, null, null
            )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        isProjectOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, projectEntityType,
                "isProjectOfPublication", "isPublicationOfProject",
                null, null, null, null
            )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        isOrgUnitOfPublication = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, publicationEntityType, orgUnitEntityType,
                "isOrgUnitOfPublication", "isPublicationOfOrgUnit",
                null, null, null, null
            )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        isMemberOfProject = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, projectEntityType, personEntityType,
                "isMemberOfProject", "isProjectOfMember",
                null, null, null, null
            )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        isMemberOfOrgUnit = RelationshipTypeBuilder.createRelationshipTypeBuilder(
                context, orgUnitEntityType, personEntityType,
                "isMemberOfOrgUnit", "isOrgUnitOfMember",
                null, null, null, null
            )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();

        isIssueOfJournalVolume = RelationshipTypeBuilder.createRelationshipTypeBuilder(
            context, journalVolumeEntityType, journalIssueEntityType,
            "isIssueOfJournalVolume", "isJournalVolumeOfIssue",
            null, null, 1, 1
        )
            .withCopyToLeft(false)
            .withCopyToRight(false)
            .build();
    }

    protected Matcher<Object> isRel(
        Item leftItem, RelationshipType relationshipType, Item rightItem, LatestVersionStatus latestVersionStatus,
        int leftPlace, int rightPlace
    ) {
        return isRel(leftItem, relationshipType, rightItem, latestVersionStatus, null, null, leftPlace, rightPlace);
    }

    protected Matcher<Object> isRel(
        Item leftItem, RelationshipType relationshipType, Item rightItem, LatestVersionStatus latestVersionStatus,
        String leftwardValue, String rightwardValue, int leftPlace, int rightPlace
    ) {
        return allOf(
            hasProperty("leftItem", is(leftItem)),
            // NOTE: this is a painful one... class RelationshipType does not implement the equals method, so we cannot
            //       rely on object equality and have to compare ids instead. It has to be in capital letters,
            //       because the getter has been implemented inconsistently (#id vs #setId() vs #getID()).
            hasProperty("relationshipType", hasProperty("ID", is(relationshipType.getID()))),
            hasProperty("rightItem", is(rightItem)),
            hasProperty("leftPlace", is(leftPlace)),
            hasProperty("rightPlace", is(rightPlace)),
            hasProperty("leftwardValue", leftwardValue == null ? nullValue() : is(leftwardValue)),
            hasProperty("rightwardValue", rightwardValue == null ? nullValue() : is(rightwardValue)),
            hasProperty("latestVersionStatus", is(latestVersionStatus))
        );
    }

    protected Relationship getRelationship(
        Item leftItem, RelationshipType relationshipType, Item rightItem
    ) throws Exception {
        List<Relationship> rels = relationshipService.findByRelationshipType(context, relationshipType).stream()
            .filter(rel -> leftItem.getID().equals(rel.getLeftItem().getID()))
            .filter(rel -> rightItem.getID().equals(rel.getRightItem().getID()))
            .collect(Collectors.toList());

        if (rels.size() == 0) {
            return null;
        }

        if (rels.size() == 1) {
            return rels.get(0);
        }

        // NOTE: this shouldn't be possible because of database constraints
        throw new IllegalStateException();
    }

    @Test
    public void test_createNewVersionOfItemOnLeftSideOfRelationships() throws Exception {
        ///////////////////////////////////////////////
        // create a publication with 3 relationships //
        ///////////////////////////////////////////////

        Item person1 = ItemBuilder.createItem(context, collection)
            .withTitle("person 1")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .build();

        Item project1 = ItemBuilder.createItem(context, collection)
            .withTitle("project 1")
            .withMetadata("dspace", "entity", "type", projectEntityType.getLabel())
            .build();

        Item orgUnit1 = ItemBuilder.createItem(context, collection)
            .withTitle("org unit 1")
            .withMetadata("dspace", "entity", "type", orgUnitEntityType.getLabel())
            .build();

        Item originalPublication = ItemBuilder.createItem(context, collection)
            .withTitle("original publication")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, person1, isAuthorOfPublication)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, project1, isProjectOfPublication)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, orgUnit1, isOrgUnitOfPublication)
            .build();

        /////////////////////////////////////////////////////////
        // verify that the relationships were properly created //
        /////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, BOTH, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, BOTH, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        /////////////////////////////////////////////
        // create a new version of the publication //
        /////////////////////////////////////////////

        Version newVersion = versioningService.createNewVersion(context, originalPublication);
        Item newPublication = newVersion.getItem();
        assertNotSame(originalPublication, newPublication);

        ///////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = true) //
        ///////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, BOTH, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, BOTH, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, RIGHT_ONLY, 0, 0),
                isRel(newPublication, isProjectOfPublication, project1, RIGHT_ONLY, 0, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, RIGHT_ONLY, 0, 0)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, BOTH, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, BOTH, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, BOTH, 0, 0),
                isRel(newPublication, isAuthorOfPublication, person1, RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, BOTH, 0, 0),
                isRel(newPublication, isProjectOfPublication, project1, RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, RIGHT_ONLY, 0, 0),
                isRel(newPublication, isProjectOfPublication, project1, RIGHT_ONLY, 0, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, RIGHT_ONLY, 0, 0)
            ))
        );

        ////////////////////////////////////////
        // do item install on new publication //
        ////////////////////////////////////////

        WorkspaceItem newPublicationWSI = workspaceItemService.findByItem(context, newPublication);
        installItemService.installItem(context, newPublicationWSI);
        context.dispatchEvents();

        ///////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = true) //
        ///////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isProjectOfPublication, project1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, BOTH, 0, 0),
                isRel(newPublication, isProjectOfPublication, project1, BOTH, 0, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, RIGHT_ONLY, 0, 0),
                isRel(newPublication, isAuthorOfPublication, person1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, RIGHT_ONLY, 0, 0),
                isRel(newPublication, isProjectOfPublication, project1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, RIGHT_ONLY, 0, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, BOTH, 0, 0),
                isRel(newPublication, isProjectOfPublication, project1, BOTH, 0, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        //////////////
        // clean up //
        //////////////

        // need to manually delete all relationships to avoid SQL constraint violation exception
        List<Relationship> relationships = relationshipService.findAll(context);
        for (Relationship relationship : relationships) {
            relationshipService.delete(context, relationship);
        }
    }

    @Test
    public void test_createNewVersionOfItemAndModifyRelationships() throws Exception {
        ///////////////////////////////////////////////
        // create a publication with 3 relationships //
        ///////////////////////////////////////////////

        Item person1 = ItemBuilder.createItem(context, collection)
            .withTitle("person 1")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .build();

        Item project1 = ItemBuilder.createItem(context, collection)
            .withTitle("project 1")
            .withMetadata("dspace", "entity", "type", projectEntityType.getLabel())
            .build();

        Item orgUnit1 = ItemBuilder.createItem(context, collection)
            .withTitle("org unit 1")
            .withMetadata("dspace", "entity", "type", orgUnitEntityType.getLabel())
            .build();

        Item originalPublication = ItemBuilder.createItem(context, collection)
            .withTitle("original publication")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, person1, isAuthorOfPublication)
            .build();

        RelationshipBuilder
            .createRelationshipBuilder(context, originalPublication, project1, isProjectOfPublication)
            .build();

        RelationshipBuilder
            .createRelationshipBuilder(context, originalPublication, orgUnit1, isOrgUnitOfPublication)
            .build();

        /////////////////////////////////////////////////////////
        // verify that the relationships were properly created //
        /////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, BOTH, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, BOTH, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        /////////////////////////////////////////////
        // create a new version of the publication //
        /////////////////////////////////////////////

        Version newVersion = versioningService.createNewVersion(context, originalPublication);
        Item newPublication = newVersion.getItem();
        assertNotSame(originalPublication, newPublication);

        /////////////////////////////////////////////
        // modify relationships on new publication //
        /////////////////////////////////////////////

        Item person2 = ItemBuilder.createItem(context, collection)
            .withTitle("person 2")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .build();

        Item orgUnit2 = ItemBuilder.createItem(context, collection)
            .withTitle("org unit 2")
            .withMetadata("dspace", "entity", "type", orgUnitEntityType.getLabel())
            .build();

        // on new item, remove relationship with project 1
        List<Relationship> newProjectRels = relationshipService
            .findByItemAndRelationshipType(context, newPublication, isProjectOfPublication);
        assertEquals(1, newProjectRels.size());
        relationshipService.delete(context, newProjectRels.get(0));

        // on new item remove relationship with org unit 1
        List<Relationship> newOrgUnitRels = relationshipService
            .findByItemAndRelationshipType(context, newPublication, isOrgUnitOfPublication);
        assertEquals(1, newOrgUnitRels.size());
        relationshipService.delete(context, newOrgUnitRels.get(0));

        RelationshipBuilder.createRelationshipBuilder(context, newPublication, person2, isAuthorOfPublication)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, newPublication, orgUnit2, isOrgUnitOfPublication)
            .build();

        ///////////////////////////////////////////////////////////////////////
        // verify the relationships of all 7 items (excludeNonLatest = true) //
        ///////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, BOTH, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, BOTH, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person2, -1, -1, false, true),
            containsInAnyOrder(List.of(
                // NOTE: BOTH because new relationship
                isRel(newPublication, isAuthorOfPublication, person2, BOTH, 1, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit2, -1, -1, false, true),
            containsInAnyOrder(List.of(
                // NOTE: BOTH because new relationship
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, RIGHT_ONLY, 0, 0),
                // NOTE: BOTH because new relationship
                isRel(newPublication, isAuthorOfPublication, person2, BOTH, 1, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, BOTH, 0, 0)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 7 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, BOTH, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, BOTH, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, BOTH, 0, 0),
                isRel(newPublication, isAuthorOfPublication, person1, RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                // NOTE: BOTH because new relationship
                isRel(newPublication, isAuthorOfPublication, person2, BOTH, 1, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                // NOTE: BOTH because new relationship
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, RIGHT_ONLY, 0, 0),
                // NOTE: BOTH because new relationship
                isRel(newPublication, isAuthorOfPublication, person2, BOTH, 1, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, BOTH, 0, 0)
            ))
        );

        ////////////////////////////////////////
        // do item install on new publication //
        ////////////////////////////////////////

        WorkspaceItem newPublicationWSI = workspaceItemService.findByItem(context, newPublication);
        installItemService.installItem(context, newPublicationWSI);
        context.dispatchEvents();

        ///////////////////////////////////////////////////////////////////////
        // verify the relationships of all 7 items (excludeNonLatest = true) //
        ///////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person2, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person2, BOTH, 1, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            empty()
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            empty()
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit2, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, BOTH, 0, 0),
                isRel(newPublication, isAuthorOfPublication, person2, BOTH, 1, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, BOTH, 0, 0)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 7 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isProjectOfPublication, project1, RIGHT_ONLY, 0, 0),
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isAuthorOfPublication, person1, RIGHT_ONLY, 0, 0),
                isRel(newPublication, isAuthorOfPublication, person1, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, person2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person2, BOTH, 1, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isProjectOfPublication, project1, RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(originalPublication, isOrgUnitOfPublication, orgUnit1, RIGHT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPublication, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(newPublication, isAuthorOfPublication, person1, BOTH, 0, 0),
                isRel(newPublication, isAuthorOfPublication, person2, BOTH, 1, 0),
                isRel(newPublication, isOrgUnitOfPublication, orgUnit2, BOTH, 0, 0)
            ))
        );

        //////////////
        // clean up //
        //////////////

        // need to manually delete all relationships to avoid SQL constraint violation exception
        List<Relationship> relationships = relationshipService.findAll(context);
        for (Relationship relationship : relationships) {
            relationshipService.delete(context, relationship);
        }
    }

    @Test
    public void test_createNewVersionOfItemOnRightSideOfRelationships() throws Exception {
        //////////////////////////////////////////
        // create a person with 3 relationships //
        //////////////////////////////////////////

        Item publication1 = ItemBuilder.createItem(context, collection)
            .withTitle("publication 1")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        Item project1 = ItemBuilder.createItem(context, collection)
            .withTitle("project 1")
            .withMetadata("dspace", "entity", "type", projectEntityType.getLabel())
            .build();

        Item orgUnit1 = ItemBuilder.createItem(context, collection)
            .withTitle("org unit 1")
            .withMetadata("dspace", "entity", "type", orgUnitEntityType.getLabel())
            .build();

        Item originalPerson = ItemBuilder.createItem(context, collection)
            .withTitle("original person")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, publication1, originalPerson, isAuthorOfPublication)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, project1, originalPerson, isMemberOfProject)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, orgUnit1, originalPerson, isMemberOfOrgUnit)
            .build();

        /////////////////////////////////////////////////////////
        // verify that the relationships were properly created //
        /////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, BOTH, 0, 0),
                isRel(project1, isMemberOfProject, originalPerson, BOTH, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(project1, isMemberOfProject, originalPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, BOTH, 0, 0)
            ))
        );

        ////////////////////////////////////////
        // create a new version of the person //
        ////////////////////////////////////////

        Version newVersion = versioningService.createNewVersion(context, originalPerson);
        Item newPerson = newVersion.getItem();
        assertNotSame(originalPerson, newPerson);

        ///////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = true) //
        ///////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPerson, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, BOTH, 0, 0),
                isRel(project1, isMemberOfProject, originalPerson, BOTH, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(project1, isMemberOfProject, originalPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPerson, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, newPerson, LEFT_ONLY, 0, 0),
                isRel(project1, isMemberOfProject, newPerson, LEFT_ONLY, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, LEFT_ONLY, 0, 0)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, BOTH, 0, 0),
                isRel(project1, isMemberOfProject, originalPerson, BOTH, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, BOTH, 0, 0),
                isRel(publication1, isAuthorOfPublication, newPerson, LEFT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(project1, isMemberOfProject, originalPerson, BOTH, 0, 0),
                isRel(project1, isMemberOfProject, newPerson, LEFT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, BOTH, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, LEFT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, newPerson, LEFT_ONLY, 0, 0),
                isRel(project1, isMemberOfProject, newPerson, LEFT_ONLY, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, LEFT_ONLY, 0, 0)
            ))
        );

        ///////////////////////////////////
        // do item install on new person //
        ///////////////////////////////////

        WorkspaceItem newPersonWSI = workspaceItemService.findByItem(context, newPerson);
        installItemService.installItem(context, newPersonWSI);
        context.dispatchEvents();

        ///////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = true) //
        ///////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPerson, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, LEFT_ONLY, 0, 0),
                isRel(project1, isMemberOfProject, originalPerson, LEFT_ONLY, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, LEFT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, newPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(project1, isMemberOfProject, newPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPerson, -1, -1, false, true),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, newPerson, BOTH, 0, 0),
                isRel(project1, isMemberOfProject, newPerson, BOTH, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, BOTH, 0, 0)
            ))
        );

        ////////////////////////////////////////////////////////////////////////
        // verify the relationships of all 5 items (excludeNonLatest = false) //
        ////////////////////////////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, originalPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, LEFT_ONLY, 0, 0),
                isRel(project1, isMemberOfProject, originalPerson, LEFT_ONLY, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, LEFT_ONLY, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, publication1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, originalPerson, LEFT_ONLY, 0, 0),
                isRel(publication1, isAuthorOfPublication, newPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, project1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(project1, isMemberOfProject, originalPerson, LEFT_ONLY, 0, 0),
                isRel(project1, isMemberOfProject, newPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, orgUnit1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(orgUnit1, isMemberOfOrgUnit, originalPerson, LEFT_ONLY, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, BOTH, 0, 0)
            ))
        );

        assertThat(
            relationshipService.findByItem(context, newPerson, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1, isAuthorOfPublication, newPerson, BOTH, 0, 0),
                isRel(project1, isMemberOfProject, newPerson, BOTH, 0, 0),
                isRel(orgUnit1, isMemberOfOrgUnit, newPerson, BOTH, 0, 0)
            ))
        );

        //////////////
        // clean up //
        //////////////

        // need to manually delete all relationships to avoid SQL constraint violation exception
        List<Relationship> relationships = relationshipService.findAll(context);
        for (Relationship relationship : relationships) {
            relationshipService.delete(context, relationship);
        }
    }

    @Test
    public void test_createNewVersionOfItemAndVerifyMetadataOrder() throws Exception {
        /////////////////////////////////////////
        // create a publication with 6 authors //
        /////////////////////////////////////////

        Item originalPublication = ItemBuilder.createItem(context, collection)
            .withTitle("original publication")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        // author 1 (plain metadata)
        itemService.addMetadata(context, originalPublication, "dc", "contributor", "author", null, "author 1 (plain)");

        // author 2 (virtual)
        Item author2 = ItemBuilder.createItem(context, collection)
            .withTitle("author 2 (item)")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("2 (item)")
            .withPersonIdentifierLastName("author")
            .build();
        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, author2, isAuthorOfPublication)
            .build();

        // author 3 (virtual)
        Item author3 = ItemBuilder.createItem(context, collection)
            .withTitle("author 3 (item)")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("3 (item)")
            .withPersonIdentifierLastName("author")
            .build();
        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, author3, isAuthorOfPublication)
            .build();

        // author 4 (virtual)
        Item author4 = ItemBuilder.createItem(context, collection)
            .withTitle("author 4 (item)")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("4 (item)")
            .withPersonIdentifierLastName("author")
            .build();
        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, author4, isAuthorOfPublication)
            .build();

        // author 5 (virtual)
        Item author5 = ItemBuilder.createItem(context, collection)
            .withTitle("author 5 (item)")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("5 (item)")
            .withPersonIdentifierLastName("author")
            .build();
        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, author5, isAuthorOfPublication)
            .build();

        // author 6 (plain metadata)
        itemService.addMetadata(context, originalPublication, "dc", "contributor", "author", null, "author 6 (plain)");

        // author 7 (virtual)
        Item author7 = ItemBuilder.createItem(context, collection)
            .withTitle("author 7 (item)")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("7 (item)")
            .withPersonIdentifierLastName("author")
            .build();
        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, author7, isAuthorOfPublication)
            .build();

        // author 8 (plain metadata)
        itemService.addMetadata(context, originalPublication, "dc", "contributor", "author", null, "author 8 (plain)");

        // author 9 (virtual)
        Item author9 = ItemBuilder.createItem(context, collection)
            .withTitle("author 9 (item)")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("9 (item)")
            .withPersonIdentifierLastName("author")
            .build();
        RelationshipBuilder.createRelationshipBuilder(context, originalPublication, author9, isAuthorOfPublication)
            .build();

        ////////////////////////////////
        // test dc.contributor.author //
        ////////////////////////////////

        List<MetadataValue> oldMdvs = itemService.getMetadata(
            originalPublication, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(9, oldMdvs.size());

        assertFalse(oldMdvs.get(0) instanceof RelationshipMetadataValue);
        assertEquals("author 1 (plain)", oldMdvs.get(0).getValue());
        assertEquals(0, oldMdvs.get(0).getPlace());

        assertTrue(oldMdvs.get(1) instanceof RelationshipMetadataValue);
        assertEquals("author, 2 (item)", oldMdvs.get(1).getValue());
        assertEquals(1, oldMdvs.get(1).getPlace());

        assertTrue(oldMdvs.get(2) instanceof RelationshipMetadataValue);
        assertEquals("author, 3 (item)", oldMdvs.get(2).getValue());
        assertEquals(2, oldMdvs.get(2).getPlace());

        assertTrue(oldMdvs.get(3) instanceof RelationshipMetadataValue);
        assertEquals("author, 4 (item)", oldMdvs.get(3).getValue());
        assertEquals(3, oldMdvs.get(3).getPlace());

        assertTrue(oldMdvs.get(4) instanceof RelationshipMetadataValue);
        assertEquals("author, 5 (item)", oldMdvs.get(4).getValue());
        assertEquals(4, oldMdvs.get(4).getPlace());

        assertFalse(oldMdvs.get(5) instanceof RelationshipMetadataValue);
        assertEquals("author 6 (plain)", oldMdvs.get(5).getValue());
        assertEquals(5, oldMdvs.get(5).getPlace());

        assertTrue(oldMdvs.get(6) instanceof RelationshipMetadataValue);
        assertEquals("author, 7 (item)", oldMdvs.get(6).getValue());
        assertEquals(6, oldMdvs.get(6).getPlace());

        assertFalse(oldMdvs.get(7) instanceof RelationshipMetadataValue);
        assertEquals("author 8 (plain)", oldMdvs.get(7).getValue());
        assertEquals(7, oldMdvs.get(7).getPlace());

        assertTrue(oldMdvs.get(8) instanceof RelationshipMetadataValue);
        assertEquals("author, 9 (item)", oldMdvs.get(8).getValue());
        assertEquals(8, oldMdvs.get(8).getPlace());

        /////////////////////////////////////////////
        // test relationship isAuthorOfPublication //
        /////////////////////////////////////////////

        List<Relationship> oldRelationships = relationshipService.findByItem(context, originalPublication);
        assertEquals(6, oldRelationships.size());

        assertEquals(originalPublication, oldRelationships.get(0).getLeftItem());
        assertEquals(isAuthorOfPublication, oldRelationships.get(0).getRelationshipType());
        assertEquals(author2, oldRelationships.get(0).getRightItem());
        assertEquals(1, oldRelationships.get(0).getLeftPlace());
        assertEquals(0, oldRelationships.get(0).getRightPlace());

        assertEquals(originalPublication, oldRelationships.get(1).getLeftItem());
        assertEquals(isAuthorOfPublication, oldRelationships.get(1).getRelationshipType());
        assertEquals(author3, oldRelationships.get(1).getRightItem());
        assertEquals(2, oldRelationships.get(1).getLeftPlace());
        assertEquals(0, oldRelationships.get(1).getRightPlace());

        assertEquals(originalPublication, oldRelationships.get(2).getLeftItem());
        assertEquals(isAuthorOfPublication, oldRelationships.get(2).getRelationshipType());
        assertEquals(author4, oldRelationships.get(2).getRightItem());
        assertEquals(3, oldRelationships.get(2).getLeftPlace());
        assertEquals(0, oldRelationships.get(2).getRightPlace());

        assertEquals(originalPublication, oldRelationships.get(3).getLeftItem());
        assertEquals(isAuthorOfPublication, oldRelationships.get(3).getRelationshipType());
        assertEquals(author5, oldRelationships.get(3).getRightItem());
        assertEquals(4, oldRelationships.get(3).getLeftPlace());
        assertEquals(0, oldRelationships.get(3).getRightPlace());

        assertEquals(originalPublication, oldRelationships.get(4).getLeftItem());
        assertEquals(isAuthorOfPublication, oldRelationships.get(4).getRelationshipType());
        assertEquals(author7, oldRelationships.get(4).getRightItem());
        assertEquals(6, oldRelationships.get(4).getLeftPlace());
        assertEquals(0, oldRelationships.get(4).getRightPlace());

        assertEquals(originalPublication, oldRelationships.get(5).getLeftItem());
        assertEquals(isAuthorOfPublication, oldRelationships.get(5).getRelationshipType());
        assertEquals(author9, oldRelationships.get(5).getRightItem());
        assertEquals(8, oldRelationships.get(5).getLeftPlace());
        assertEquals(0, oldRelationships.get(5).getRightPlace());

        ///////////////////////////////////////
        // create new version of publication //
        ///////////////////////////////////////

        Version newVersion = versioningService.createNewVersion(context, originalPublication);
        Item newPublication = newVersion.getItem();
        assertNotSame(originalPublication, newPublication);

        ////////////////////////////////
        // test dc.contributor.author //
        ////////////////////////////////

        List<MetadataValue> newMdvs = itemService.getMetadata(
            newPublication, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(9, newMdvs.size());

        assertFalse(newMdvs.get(0) instanceof RelationshipMetadataValue);
        assertEquals("author 1 (plain)", newMdvs.get(0).getValue());
        assertEquals(0, newMdvs.get(0).getPlace());

        assertTrue(newMdvs.get(1) instanceof RelationshipMetadataValue);
        assertEquals("author, 2 (item)", newMdvs.get(1).getValue());
        assertEquals(1, newMdvs.get(1).getPlace());

        assertTrue(newMdvs.get(2) instanceof RelationshipMetadataValue);
        assertEquals("author, 3 (item)", newMdvs.get(2).getValue());
        assertEquals(2, newMdvs.get(2).getPlace());

        assertTrue(newMdvs.get(3) instanceof RelationshipMetadataValue);
        assertEquals("author, 4 (item)", newMdvs.get(3).getValue());
        assertEquals(3, newMdvs.get(3).getPlace());

        assertTrue(newMdvs.get(4) instanceof RelationshipMetadataValue);
        assertEquals("author, 5 (item)", newMdvs.get(4).getValue());
        assertEquals(4, newMdvs.get(4).getPlace());

        assertFalse(newMdvs.get(5) instanceof RelationshipMetadataValue);
        assertEquals("author 6 (plain)", newMdvs.get(5).getValue());
        assertEquals(5, newMdvs.get(5).getPlace());

        assertTrue(newMdvs.get(6) instanceof RelationshipMetadataValue);
        assertEquals("author, 7 (item)", newMdvs.get(6).getValue());
        assertEquals(6, newMdvs.get(6).getPlace());

        assertFalse(newMdvs.get(7) instanceof RelationshipMetadataValue);
        assertEquals("author 8 (plain)", newMdvs.get(7).getValue());
        assertEquals(7, newMdvs.get(7).getPlace());

        assertTrue(newMdvs.get(8) instanceof RelationshipMetadataValue);
        assertEquals("author, 9 (item)", newMdvs.get(8).getValue());
        assertEquals(8, newMdvs.get(8).getPlace());

        /////////////////////////////////////////////
        // test relationship isAuthorOfPublication //
        /////////////////////////////////////////////

        List<Relationship> newRelationships = relationshipService.findByItem(context, newPublication);
        assertEquals(6, newRelationships.size());

        assertEquals(newPublication, newRelationships.get(0).getLeftItem());
        assertEquals(isAuthorOfPublication, newRelationships.get(0).getRelationshipType());
        assertEquals(author2, newRelationships.get(0).getRightItem());
        assertEquals(1, newRelationships.get(0).getLeftPlace());
        assertEquals(0, newRelationships.get(0).getRightPlace());

        assertEquals(newPublication, newRelationships.get(1).getLeftItem());
        assertEquals(isAuthorOfPublication, newRelationships.get(1).getRelationshipType());
        assertEquals(author3, newRelationships.get(1).getRightItem());
        assertEquals(2, newRelationships.get(1).getLeftPlace());
        assertEquals(0, newRelationships.get(1).getRightPlace());

        assertEquals(newPublication, newRelationships.get(2).getLeftItem());
        assertEquals(isAuthorOfPublication, newRelationships.get(2).getRelationshipType());
        assertEquals(author4, newRelationships.get(2).getRightItem());
        assertEquals(3, newRelationships.get(2).getLeftPlace());
        assertEquals(0, newRelationships.get(2).getRightPlace());

        assertEquals(newPublication, newRelationships.get(3).getLeftItem());
        assertEquals(isAuthorOfPublication, newRelationships.get(3).getRelationshipType());
        assertEquals(author5, newRelationships.get(3).getRightItem());
        assertEquals(4, newRelationships.get(3).getLeftPlace());
        assertEquals(0, newRelationships.get(3).getRightPlace());

        assertEquals(newPublication, newRelationships.get(4).getLeftItem());
        assertEquals(isAuthorOfPublication, newRelationships.get(4).getRelationshipType());
        assertEquals(author7, newRelationships.get(4).getRightItem());
        assertEquals(6, newRelationships.get(4).getLeftPlace());
        assertEquals(0, newRelationships.get(4).getRightPlace());

        assertEquals(newPublication, newRelationships.get(5).getLeftItem());
        assertEquals(isAuthorOfPublication, newRelationships.get(5).getRelationshipType());
        assertEquals(author9, newRelationships.get(5).getRightItem());
        assertEquals(8, newRelationships.get(5).getLeftPlace());
        assertEquals(0, newRelationships.get(5).getRightPlace());

        //////////////
        // clean up //
        //////////////

        // need to manually delete all relationships to avoid SQL constraint violation exception
        List<Relationship> relationships = relationshipService.findAll(context);
        for (Relationship relationship : relationships) {
            relationshipService.delete(context, relationship);
        }
    }

    /**
     * This test will
     * - create a publication with 10 projects
     * - Remove, move, add projects
     * - Verify the order remains correct
     * @throws Exception
     */
    @Test
    public void test_createNewVersionOfItemWithAddRemoveMove() throws Exception {
        ///////////////////////////////////////////
        // create a publication with 10 projects //
        ///////////////////////////////////////////

        Item originalPublication = ItemBuilder.createItem(context, collection)
            .withTitle("original publication")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        List<Item> projects = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Item project = ItemBuilder.createItem(context, collection)
                    .withTitle("project " + i)
                    .withMetadata("dspace", "entity", "type", projectEntityType.getLabel())
                    .build();
            projects.add(project);

            RelationshipBuilder
                    .createRelationshipBuilder(context, originalPublication, project, isProjectOfPublication)
                    .build();
        }

        AtomicInteger counterOriginalPublication = new AtomicInteger();
        List<Matcher<Object>> listOriginalPublication = projects.stream().map(
                project -> isRel(originalPublication, isProjectOfPublication, project, BOTH,
                        counterOriginalPublication.getAndIncrement(), 0)
        ).collect(Collectors.toCollection(ArrayList::new));

        /////////////////////////////////////////////////////////////////////
        // verify the relationships of all items (excludeNonLatest = true) //
        /////////////////////////////////////////////////////////////////////

        assertThat(
                relationshipService.findByItem(context, originalPublication, -1, -1, false, true),
                containsInAnyOrder(listOriginalPublication)
        );

        //////////////////////////////////////////////////////////////////////
        // verify the relationships of all items (excludeNonLatest = false) //
        //////////////////////////////////////////////////////////////////////

        assertThat(
                relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
                containsInAnyOrder(listOriginalPublication)
        );

        /////////////////////////////////////////////
        // create a new version of the publication //
        /////////////////////////////////////////////

        Version newVersion = versioningService.createNewVersion(context, originalPublication);
        Item newPublication = newVersion.getItem();
        assertNotSame(originalPublication, newPublication);

        verifyProjectsMatch(originalPublication, projects, newPublication, projects, false);//

        /////////////////////////////////////////////
        // modify relationships on new publication //
        /////////////////////////////////////////////

        List<Item> newProjects = new ArrayList<>(projects);
        assertEquals(newProjects.size(), 10);

        removeProject(newPublication, 5, newProjects);

        assertEquals(projects.size(), 10);
        assertEquals(newProjects.size(), 9);
        verifyProjectsMatch(originalPublication, projects, newPublication, newProjects, false);

        Item project6 = newProjects.get(6);
        moveProject(newPublication, 6, 2, newProjects);
        assertEquals(newProjects.size(), 9);
        assertEquals(newProjects.get(2), project6);
        assertNotEquals(projects.get(2), project6);
        verifyProjectsMatch(originalPublication, projects, newPublication, newProjects, false);

        Item project1 = newProjects.get(1);
        moveProject(newPublication, 1, 5, newProjects);
        assertEquals(newProjects.size(), 9);
        assertEquals(newProjects.get(5), project1);
        assertNotEquals(projects.get(5), project1);
        verifyProjectsMatch(originalPublication, projects, newPublication, newProjects, false);

        Item project = ItemBuilder.createItem(context, collection)
                .withTitle("project 10")
                .withMetadata("dspace", "entity", "type", projectEntityType.getLabel())
                .build();
        newProjects.add(4, project);

        RelationshipBuilder
                .createRelationshipBuilder(context, newPublication, project, isProjectOfPublication, 4, -1)
                .build();

        verifyProjectsMatch(originalPublication, projects, newPublication, newProjects, false);

        ////////////////////////////////////////
        // do item install on new publication //
        ////////////////////////////////////////

        WorkspaceItem newPublicationWSI = workspaceItemService.findByItem(context, newPublication);
        installItemService.installItem(context, newPublicationWSI);
        context.dispatchEvents();

        verifyProjectsMatch(originalPublication, projects, newPublication, newProjects, true);

        //////////////
        // clean up //
        //////////////

        // need to manually delete all relationships to avoid SQL constraint violation exception
        List<Relationship> relationships = relationshipService.findAll(context);
        for (Relationship relationship : relationships) {
            relationshipService.delete(context, relationship);
        }
    }

    public void removeProject(Item newPublication, int place, List<Item> newProjects)
            throws SQLException, AuthorizeException {
        List<Relationship> projectRels = relationshipService
                .findByItemAndRelationshipType(context, newProjects.get(place), isProjectOfPublication, -1, -1, false)
                .stream()
                .filter(
                        relationship -> relationship.getLeftItem().equals(newPublication)
                )
                .collect(Collectors.toCollection(ArrayList::new));
        assertEquals(1, projectRels.size());
        relationshipService.delete(context, projectRels.get(0));
        newProjects.remove(newProjects.get(place));
    }

    public void moveProject(Item newPublication, int oldPlace, int newPlace, List<Item> newProjects)
            throws SQLException, AuthorizeException {
        Item project = newProjects.get(oldPlace);
        List<Relationship> projectRels = relationshipService
                .findByItemAndRelationshipType(context, project, isProjectOfPublication, -1, -1, false)
                .stream()
                .filter(
                        relationship -> relationship.getLeftItem().equals(newPublication)
                )
                .collect(Collectors.toCollection(ArrayList::new));
        assertEquals(1, projectRels.size());
        relationshipService.move(context, projectRels.get(0), newPlace, null);
        newProjects.remove(project);
        newProjects.add(newPlace, project);
    }

    private void verifyProjectsMatch(Item originalPublication, List<Item> originalProjects,
                                     Item newPublication, List<Item> newProjects, boolean newPublicationArchived)
            throws SQLException {

        /////////////////////////////////////////////////////////
        // verify that the relationships were properly created //
        /////////////////////////////////////////////////////////

        AtomicInteger counterOriginalPublication = new AtomicInteger();
        List<Matcher<Object>> listOriginalPublication = originalProjects.stream().map(
                project -> isRel(originalPublication, isProjectOfPublication, project,
                        newPublicationArchived ? RIGHT_ONLY : BOTH,
                        counterOriginalPublication.getAndIncrement(), 0)
        ).collect(Collectors.toCollection(ArrayList::new));

        AtomicInteger counterNewPublication = new AtomicInteger();
        List<Matcher<Object>> listNewPublication = newProjects.stream().map(
                project -> isRel(newPublication, isProjectOfPublication, project,
                        newPublicationArchived || !originalProjects.contains(project) ?
                                BOTH : RIGHT_ONLY,
                        counterNewPublication.getAndIncrement(), 0)
        ).collect(Collectors.toCollection(ArrayList::new));

        /////////////////////////////////////////////////////////////////////
        // verify the relationships of all items (excludeNonLatest = true) //
        /////////////////////////////////////////////////////////////////////

        assertEquals(
                relationshipService.countByItem(context, originalPublication, false, true),
                originalProjects.size()
        );

        assertThat(
                relationshipService.findByItem(context, originalPublication, -1, -1, false, true),
                containsInAnyOrder(listOriginalPublication)
        );

        assertEquals(
                relationshipService.countByItem(context, newPublication, false, true),
                newProjects.size()
        );

        assertThat(
                relationshipService.findByItem(context, newPublication, -1, -1, false, true),
                containsInAnyOrder(listNewPublication)
        );

        //////////////////////////////////////////////////////////////////////
        // verify the relationships of all items (excludeNonLatest = false) //
        //////////////////////////////////////////////////////////////////////

        assertThat(
                relationshipService.findByItem(context, originalPublication, -1, -1, false, false),
                containsInAnyOrder(listOriginalPublication)
        );

        assertThat(
                relationshipService.findByItem(context, newPublication, -1, -1, false, false),
                containsInAnyOrder(listNewPublication)
        );
    }

    @Test
    public void test_placeRecalculationAfterDelete() throws Exception {
        // NOTE: this test uses relationship isIssueOfJournalVolume, because it adds virtual metadata
        //       on both sides of the relationship

        /////////////////////////////////////////
        // properly configure virtual metadata //
        /////////////////////////////////////////

        ServiceManager serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();

        // virtual metadata field publicationissue.issueNumber needs to be used in place calculations
        Collected issueVmd = serviceManager.getServiceByName("journalIssue_number", Collected.class);
        assertNotNull(issueVmd);
        boolean ogIssueVmdUseForPlace = issueVmd.getUseForPlace();
        issueVmd.setUseForPlace(true);

        //////////////////
        // create items //
        //////////////////

        // journal volume 1.1
        Item v1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal volume 1")
            .withMetadata("dspace", "entity", "type", journalVolumeEntityType.getLabel())
            .withMetadata("publicationvolume", "volumeNumber", null, "volume nr 3 (rel)")
            .build();

        // journal issue 1.1
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal issue 1")
            .withMetadata("dspace", "entity", "type", journalIssueEntityType.getLabel())
            .withMetadata("publicationissue", "issueNumber", null, "issue nr 1 (rel)")
            .build();

        // journal issue 3.1
        Item i3_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal issue 3")
            .withMetadata("dspace", "entity", "type", journalIssueEntityType.getLabel())
            .withMetadata("publicationissue", "issueNumber", null, "issue nr 3 (rel)")
            .build();

        // journal issue 5.1
        Item i5_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal issue 5")
            .withMetadata("dspace", "entity", "type", journalIssueEntityType.getLabel())
            .withMetadata("publicationissue", "issueNumber", null, "issue nr 5 (rel)")
            .build();

        //////////////////////////////////////////////
        // create relationships and metadata values //
        //////////////////////////////////////////////

        // relationship - volume 1 & issue 1
        RelationshipBuilder.createRelationshipBuilder(context, v1_1, i1_1, isIssueOfJournalVolume).build();

        // metadata - volume 3 & issue 2
        itemService.addMetadata(context, v1_1, "publicationissue", "issueNumber", null, null, "issue nr 2 (plain)");

        // relationship - volume 1 & issue 3
        RelationshipBuilder.createRelationshipBuilder(context, v1_1, i3_1, isIssueOfJournalVolume).build();

        // metadata - volume 3 & issue 4
        itemService.addMetadata(context, v1_1, "publicationissue", "issueNumber", null, null, "issue nr 4 (plain)");

        // relationship - volume 1 & issue 5
        RelationshipBuilder.createRelationshipBuilder(context, v1_1, i5_1, isIssueOfJournalVolume).build();

        // metadata - volume 3 & issue 6
        itemService.addMetadata(context, v1_1, "publicationissue", "issueNumber", null, null, "issue nr 6 (plain)");

        // SUMMARY
        //
        // volume 3
        // - pos 0: issue 1 (rel)
        // - pos 1: issue 2 (plain)
        // - pos 2: issue 3 (rel)     [A]
        // - pos 3: issue 4 (plain)
        // - pos 4: issue 5 (rel)     [B]
        // - pos 5: issue 6 (plain)

        /////////////////////////////////
        // initial - verify volume 3.1 //
        /////////////////////////////////

        List<MetadataValue> mdvs1 = itemService.getMetadata(
            v1_1, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(6, mdvs1.size());

        assertTrue(mdvs1.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs1.get(0).getValue());
        assertEquals(0, mdvs1.get(0).getPlace());

        assertFalse(mdvs1.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (plain)", mdvs1.get(1).getValue());
        assertEquals(1, mdvs1.get(1).getPlace());

        assertTrue(mdvs1.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs1.get(2).getValue());
        assertEquals(2, mdvs1.get(2).getPlace());

        assertFalse(mdvs1.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (plain)", mdvs1.get(3).getValue());
        assertEquals(3, mdvs1.get(3).getPlace());

        assertTrue(mdvs1.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs1.get(4).getValue());
        assertEquals(4, mdvs1.get(4).getPlace());

        assertFalse(mdvs1.get(5) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 6 (plain)", mdvs1.get(5).getValue());
        assertEquals(5, mdvs1.get(5).getPlace());

        /////////////////////////////////////
        // create new version - volume 3.2 //
        /////////////////////////////////////

        //TODO: This currently fails because we're creating a second relationship
        //This is rejected in org.dspace.content.RelationshipServiceImpl.verifyMaxCardinality
        Item v1_2 = versioningService.createNewVersion(context, v1_1).getItem();
        installItemService.installItem(context, workspaceItemService.findByItem(context, v1_2));
        context.commit();

        ////////////////////////////////////
        // create new version - issue 3.2 //
        ////////////////////////////////////

        Item i3_2 = versioningService.createNewVersion(context, i3_1).getItem();
        installItemService.installItem(context, workspaceItemService.findByItem(context, i3_2));
        context.commit();

        ////////////////////////////////////////////////
        // after version creation - verify volume 3.1 //
        ////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, v1_1, -1, -1, false, false),
                containsInAnyOrder(List.of(
                    isRel(v1_1, isIssueOfJournalVolume, i1_1, RIGHT_ONLY, 0, 0),
                    isRel(v1_1, isIssueOfJournalVolume, i3_1, RIGHT_ONLY, 2, 0),
                    isRel(v1_1, isIssueOfJournalVolume, i5_1, RIGHT_ONLY, 4, 0)
                ))
        );

        List<MetadataValue> mdvs4 = itemService.getMetadata(
            v1_1, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(6, mdvs4.size());

        assertTrue(mdvs4.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs4.get(0).getValue());
        assertEquals(0, mdvs4.get(0).getPlace());

        assertFalse(mdvs4.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (plain)", mdvs4.get(1).getValue());
        assertEquals(1, mdvs4.get(1).getPlace());

        assertTrue(mdvs4.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs4.get(2).getValue());
        assertEquals(2, mdvs4.get(2).getPlace());

        assertFalse(mdvs4.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (plain)", mdvs4.get(3).getValue());
        assertEquals(3, mdvs4.get(3).getPlace());

        assertTrue(mdvs4.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs4.get(4).getValue());
        assertEquals(4, mdvs4.get(4).getPlace());

        assertFalse(mdvs4.get(5) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 6 (plain)", mdvs4.get(5).getValue());
        assertEquals(5, mdvs4.get(5).getPlace());

        ////////////////////////////////////////////////
        // after version creation - verify volume 3.2 //
        ////////////////////////////////////////////////

        assertThat(
                relationshipService.findByItem(context, v1_2, -1, -1, false, false),
                containsInAnyOrder(List.of(
                    isRel(v1_2, isIssueOfJournalVolume, i1_1, BOTH, 0, 0),
                    isRel(v1_2, isIssueOfJournalVolume, i3_1, LEFT_ONLY, 2, 0),
                    isRel(v1_2, isIssueOfJournalVolume, i5_1, BOTH, 4, 0),
                    isRel(v1_2, isIssueOfJournalVolume, i3_2, BOTH, 2, 0)
                ))
        );

        List<MetadataValue> mdvs7 = itemService.getMetadata(
            v1_2, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(6, mdvs7.size());

        assertTrue(mdvs7.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs7.get(0).getValue());
        assertEquals(0, mdvs7.get(0).getPlace());

        assertFalse(mdvs7.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (plain)", mdvs7.get(1).getValue());
        assertEquals(1, mdvs7.get(1).getPlace());

        assertTrue(mdvs7.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs7.get(2).getValue());
        assertEquals(2, mdvs7.get(2).getPlace());

        assertFalse(mdvs7.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (plain)", mdvs7.get(3).getValue());
        assertEquals(3, mdvs7.get(3).getPlace());

        assertTrue(mdvs7.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs7.get(4).getValue());
        assertEquals(4, mdvs7.get(4).getPlace());

        assertFalse(mdvs7.get(5) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 6 (plain)", mdvs7.get(5).getValue());
        assertEquals(5, mdvs7.get(5).getPlace());

        ///////////////////////////////////////////////////////////
        // remove relationship - volume 3.2 & issue 3.2          //
        // since an issue needs a relationship, delete the issue //
        ///////////////////////////////////////////////////////////

        Relationship rel1 = getRelationship(v1_2, isIssueOfJournalVolume, i3_2);
        assertNotNull(rel1);

        itemService.delete(context, context.reloadEntity(i3_2));

        context.commit();

        ////////////////////////////////////
        // after remove 1 - cache busting //
        ////////////////////////////////////

        v1_2.setMetadataModified();
        v1_2 = context.reloadEntity(v1_2);

        ////////////////////////////////////////
        // after remove 1 - verify volume 3.1 //
        ////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, v1_1, -1, -1, false, false),
                containsInAnyOrder(List.of(
                    isRel(v1_1, isIssueOfJournalVolume, i1_1, RIGHT_ONLY, 0, 0),
                    isRel(v1_1, isIssueOfJournalVolume, i3_1, RIGHT_ONLY, 2, 0),
                    isRel(v1_1, isIssueOfJournalVolume, i5_1, RIGHT_ONLY, 4, 0)
                ))
        );

        List<MetadataValue> mdvs9 = itemService.getMetadata(
            v1_1, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(6, mdvs9.size());

        assertTrue(mdvs9.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs9.get(0).getValue());
        assertEquals(0, mdvs9.get(0).getPlace());

        assertFalse(mdvs9.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (plain)", mdvs9.get(1).getValue());
        assertEquals(1, mdvs9.get(1).getPlace());

        assertTrue(mdvs9.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs9.get(2).getValue());
        assertEquals(2, mdvs9.get(2).getPlace());

        assertFalse(mdvs9.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (plain)", mdvs9.get(3).getValue());
        assertEquals(3, mdvs9.get(3).getPlace());

        assertTrue(mdvs9.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs9.get(4).getValue());
        assertEquals(4, mdvs9.get(4).getPlace());

        assertFalse(mdvs9.get(5) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 6 (plain)", mdvs9.get(5).getValue());
        assertEquals(5, mdvs9.get(5).getPlace());

        ////////////////////////////////////////
        // after remove 1 - verify volume 3.2 //
        ////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, v1_2, -1, -1, false, false),
                containsInAnyOrder(List.of(
                    isRel(v1_2, isIssueOfJournalVolume, i1_1, BOTH, 0, 0),
                    isRel(v1_2, isIssueOfJournalVolume, i3_1, LEFT_ONLY, 2, 0),
                    // NOTE: left place was reduced by one
                    isRel(v1_2, isIssueOfJournalVolume, i5_1, BOTH, 3, 0)
                ))
        );

        List<MetadataValue> mdvs12 = itemService.getMetadata(
            v1_2, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(5, mdvs12.size());

        assertTrue(mdvs12.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs12.get(0).getValue());
        assertEquals(0, mdvs12.get(0).getPlace());

        assertFalse(mdvs12.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (plain)", mdvs12.get(1).getValue());
        assertEquals(1, mdvs12.get(1).getPlace());

        assertFalse(mdvs12.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (plain)", mdvs12.get(2).getValue());
        assertEquals(2, mdvs12.get(2).getPlace());

        assertTrue(mdvs12.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs12.get(3).getValue());
        assertEquals(3, mdvs12.get(3).getPlace());

        assertFalse(mdvs12.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 6 (plain)", mdvs12.get(4).getValue());
        assertEquals(4, mdvs12.get(4).getPlace());

        ////////////////////////////////////////
        // remove metadata value - volume 3.2 //
        ////////////////////////////////////////

        MetadataValue removeMdv1 = mdvs12.get(2);

        // let's make sure we have the metadata value that we intended to remove
        assertFalse(removeMdv1 instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (plain)", removeMdv1.getValue());
        assertEquals(2, removeMdv1.getPlace());
        assertEquals(v1_2, removeMdv1.getDSpaceObject());

        metadataValueService.delete(context, removeMdv1);
        // NOTE: needed to avoid Hibernate Exception: javax.persistence.EntityNotFoundException:
        //       "deleted object would be re-saved by cascade (remove deleted object from associations)"
        if (v1_2.getMetadata().removeIf(removeMdv1::equals)) {
            v1_2.setMetadataModified();
        }
        context.commit();

        // TODO cache busting?

        ////////////////////////////////////////
        // after remove 2 - verify volume 3.1 //
        ////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, v1_1, -1, -1, false, false),
                containsInAnyOrder(List.of(
                    isRel(v1_1, isIssueOfJournalVolume, i1_1, RIGHT_ONLY, 0, 0),
                    isRel(v1_1, isIssueOfJournalVolume, i3_1, RIGHT_ONLY, 2, 0),
                    isRel(v1_1, isIssueOfJournalVolume, i5_1, RIGHT_ONLY, 4, 0)
                ))
        );

        List<MetadataValue> mdvs14 = itemService.getMetadata(
            v1_1, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(6, mdvs14.size());

        assertTrue(mdvs14.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs14.get(0).getValue());
        assertEquals(0, mdvs14.get(0).getPlace());

        assertFalse(mdvs14.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (plain)", mdvs14.get(1).getValue());
        assertEquals(1, mdvs14.get(1).getPlace());

        assertTrue(mdvs14.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs14.get(2).getValue());
        assertEquals(2, mdvs14.get(2).getPlace());

        assertFalse(mdvs14.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (plain)", mdvs14.get(3).getValue());
        assertEquals(3, mdvs14.get(3).getPlace());

        assertTrue(mdvs14.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs14.get(4).getValue());
        assertEquals(4, mdvs14.get(4).getPlace());

        assertFalse(mdvs14.get(5) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 6 (plain)", mdvs14.get(5).getValue());
        assertEquals(5, mdvs14.get(5).getPlace());

        ////////////////////////////////////////
        // after remove 2 - verify volume 3.2 //
        ////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, v1_2, -1, -1, false, false),
                containsInAnyOrder(List.of(
                    isRel(v1_2, isIssueOfJournalVolume, i1_1, BOTH, 0, 0),
                    isRel(v1_2, isIssueOfJournalVolume, i3_1, LEFT_ONLY, 2, 0),
                    // NOTE: left place was reduced by one
                    isRel(v1_2, isIssueOfJournalVolume, i5_1, BOTH, 3, 0)
                ))
        );

        List<MetadataValue> mdvs17 = itemService.getMetadata(
            v1_2, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(4, mdvs17.size());

        assertTrue(mdvs17.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs17.get(0).getValue());
        assertEquals(0, mdvs17.get(0).getPlace());

        assertFalse(mdvs17.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (plain)", mdvs17.get(1).getValue());
        assertEquals(1, mdvs17.get(1).getPlace());

        assertTrue(mdvs17.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs17.get(2).getValue());
        assertEquals(3, mdvs17.get(2).getPlace());

        assertFalse(mdvs17.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 6 (plain)", mdvs17.get(3).getValue());
        assertEquals(4, mdvs17.get(3).getPlace());

        // TODO
        // delete mdv from older
        // delete rel from older

        /////////////////////////////////////////////
        // delete volume first for min cardinality //
        /////////////////////////////////////////////

        itemService.delete(context, context.reloadEntity(v1_1));
        itemService.delete(context, context.reloadEntity(v1_2));

        /////////////////////////////
        // clean up config changes //
        /////////////////////////////

        issueVmd.setUseForPlace(ogIssueVmdUseForPlace);
    }

    @Test
    public void test_placeRecalculationNoUseForPlace() throws Exception {
        // NOTE: this test uses relationship isIssueOfJournalVolume, because it adds virtual metadata
        //       on both sides of the relationship

        //////////////////
        // create items //
        //////////////////

        // journal volume 1.1
        Item v1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal volume 1")
            .withMetadata("dspace", "entity", "type", journalVolumeEntityType.getLabel())
            .withMetadata("publicationvolume", "volumeNumber", null, "volume nr 3 (rel)")
            .build();

        // journal issue 1.1
        Item i1_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal issue 1")
            .withMetadata("dspace", "entity", "type", journalIssueEntityType.getLabel())
            .withMetadata("publicationissue", "issueNumber", null, "issue nr 1 (rel)")
            .build();

        // journal issue 2.1
        Item i2_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal issue 2")
            .withMetadata("dspace", "entity", "type", journalIssueEntityType.getLabel())
            .withMetadata("publicationissue", "issueNumber", null, "issue nr 2 (rel)")
            .build();

        // journal issue 3.1
        Item i3_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal issue 3")
            .withMetadata("dspace", "entity", "type", journalIssueEntityType.getLabel())
            .withMetadata("publicationissue", "issueNumber", null, "issue nr 3 (rel)")
            .build();

        // journal issue 4.1
        Item i4_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal issue 4")
            .withMetadata("dspace", "entity", "type", journalIssueEntityType.getLabel())
            .withMetadata("publicationissue", "issueNumber", null, "issue nr 4 (rel)")
            .build();

        // journal issue 5.1
        Item i5_1 = ItemBuilder.createItem(context, collection)
            .withTitle("journal issue 5")
            .withMetadata("dspace", "entity", "type", journalIssueEntityType.getLabel())
            .withMetadata("publicationissue", "issueNumber", null, "issue nr 5 (rel)")
            .build();

        //////////////////////////////////////////////
        // create relationships and metadata values //
        //////////////////////////////////////////////

        // relationship - volume 1 & issue 1
        RelationshipBuilder.createRelationshipBuilder(context, v1_1, i1_1, isIssueOfJournalVolume)
            .build();

        // relationship - volume 1 & issue 2
        RelationshipBuilder.createRelationshipBuilder(context, v1_1, i2_1, isIssueOfJournalVolume)
            .build();

        // relationship - volume 1 & issue 3
        RelationshipBuilder.createRelationshipBuilder(context, v1_1, i3_1, isIssueOfJournalVolume)
            .build();

        // relationship - volume 1 & issue 4
        RelationshipBuilder.createRelationshipBuilder(context, v1_1, i4_1, isIssueOfJournalVolume)
            .build();

        // relationship - volume 1 & issue 5
        RelationshipBuilder.createRelationshipBuilder(context, v1_1, i5_1, isIssueOfJournalVolume)
            .build();

        /////////////////////////////////
        // initial - verify volume 3.1 //
        /////////////////////////////////

        List<MetadataValue> mdvs1 = itemService.getMetadata(
            v1_1, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(5, mdvs1.size());

        assertTrue(mdvs1.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs1.get(0).getValue());
        assertEquals(0, mdvs1.get(0).getPlace());

        assertTrue(mdvs1.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (rel)", mdvs1.get(1).getValue());
        assertEquals(1, mdvs1.get(1).getPlace());

        assertTrue(mdvs1.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs1.get(2).getValue());
        assertEquals(2, mdvs1.get(2).getPlace());

        assertTrue(mdvs1.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (rel)", mdvs1.get(3).getValue());
        assertEquals(3, mdvs1.get(3).getPlace());

        assertTrue(mdvs1.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs1.get(4).getValue());
        assertEquals(4, mdvs1.get(4).getPlace());

        /////////////////////////////////////
        // create new version - volume 3.2 //
        /////////////////////////////////////

        Item v1_2 = versioningService.createNewVersion(context, v1_1).getItem();
        installItemService.installItem(context, workspaceItemService.findByItem(context, v1_2));
        context.commit();

        ////////////////////////////////////
        // create new version - issue 3.2 //
        ////////////////////////////////////

        Item i3_2 = versioningService.createNewVersion(context, i3_1).getItem();
        installItemService.installItem(context, workspaceItemService.findByItem(context, i3_2));
        context.commit();

        ////////////////////////////////////////////////
        // after version creation - verify volume 3.1 //
        ////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, v1_1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(v1_1, isIssueOfJournalVolume, i1_1, RIGHT_ONLY, 0, 0),
                isRel(v1_1, isIssueOfJournalVolume, i2_1, RIGHT_ONLY, 1, 0),
                isRel(v1_1, isIssueOfJournalVolume, i3_1, RIGHT_ONLY, 2, 0),
                isRel(v1_1, isIssueOfJournalVolume, i4_1, RIGHT_ONLY, 3, 0),
                isRel(v1_1, isIssueOfJournalVolume, i5_1, RIGHT_ONLY, 4, 0)
            ))
        );

        List<MetadataValue> mdvs4 = itemService.getMetadata(
            v1_1, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(5, mdvs4.size());

        assertTrue(mdvs4.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs4.get(0).getValue());
        assertEquals(0, mdvs4.get(0).getPlace());

        assertTrue(mdvs4.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (rel)", mdvs4.get(1).getValue());
        assertEquals(1, mdvs4.get(1).getPlace());

        assertTrue(mdvs4.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs4.get(2).getValue());
        assertEquals(2, mdvs4.get(2).getPlace());

        assertTrue(mdvs4.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (rel)", mdvs4.get(3).getValue());
        assertEquals(3, mdvs4.get(3).getPlace());

        assertTrue(mdvs4.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs4.get(4).getValue());
        assertEquals(4, mdvs4.get(4).getPlace());

        ////////////////////////////////////////////////
        // after version creation - verify volume 3.2 //
        ////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, v1_2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(v1_2, isIssueOfJournalVolume, i1_1, BOTH, 0, 0),
                isRel(v1_2, isIssueOfJournalVolume, i2_1, BOTH, 1, 0),
                isRel(v1_2, isIssueOfJournalVolume, i3_1, LEFT_ONLY, 2, 0),
                isRel(v1_2, isIssueOfJournalVolume, i3_2, BOTH, 2, 0),
                isRel(v1_2, isIssueOfJournalVolume, i4_1, BOTH, 3, 0),
                isRel(v1_2, isIssueOfJournalVolume, i5_1, BOTH, 4, 0)
            ))
        );

        List<MetadataValue> mdvs7 = itemService.getMetadata(
            v1_2, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(5, mdvs7.size());

        assertTrue(mdvs7.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs7.get(0).getValue());
        assertEquals(0, mdvs7.get(0).getPlace());

        assertTrue(mdvs7.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (rel)", mdvs7.get(1).getValue());
        assertEquals(1, mdvs7.get(1).getPlace());

        assertTrue(mdvs7.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs7.get(2).getValue());
        assertEquals(2, mdvs7.get(2).getPlace());

        assertTrue(mdvs7.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (rel)", mdvs7.get(3).getValue());
        assertEquals(3, mdvs7.get(3).getPlace());

        assertTrue(mdvs7.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs7.get(4).getValue());
        assertEquals(4, mdvs7.get(4).getPlace());

        ///////////////////////////////////////////////////////////
        // remove relationship - volume 1.2 & issue 3.2          //
        // since an issue needs a relationship, delete the issue //
        ///////////////////////////////////////////////////////////

        Relationship rel1 = getRelationship(v1_2, isIssueOfJournalVolume, i3_2);
        assertNotNull(rel1);

        itemService.delete(context, context.reloadEntity(i3_2));

        context.commit();

        ////////////////////////////////////
        // after remove 1 - cache busting //
        ////////////////////////////////////

        v1_2.setMetadataModified();
        v1_2 = context.reloadEntity(v1_2);

        i3_2.setMetadataModified();
        i3_2 = context.reloadEntity(i3_2);

        ////////////////////////////////////////
        // after remove 1 - verify volume 3.1 //
        ////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, v1_1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(v1_1, isIssueOfJournalVolume, i1_1, RIGHT_ONLY, 0, 0),
                isRel(v1_1, isIssueOfJournalVolume, i2_1, RIGHT_ONLY, 1, 0),
                isRel(v1_1, isIssueOfJournalVolume, i3_1, RIGHT_ONLY, 2, 0),
                isRel(v1_1, isIssueOfJournalVolume, i4_1, RIGHT_ONLY, 3, 0),
                isRel(v1_1, isIssueOfJournalVolume, i5_1, RIGHT_ONLY, 4, 0)
            ))
        );

        List<MetadataValue> mdvs9 = itemService.getMetadata(
            v1_1, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(5, mdvs9.size());

        assertTrue(mdvs9.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs9.get(0).getValue());
        assertEquals(0, mdvs9.get(0).getPlace());

        assertTrue(mdvs9.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (rel)", mdvs9.get(1).getValue());
        assertEquals(1, mdvs9.get(1).getPlace());

        assertTrue(mdvs9.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs9.get(2).getValue());
        assertEquals(2, mdvs9.get(2).getPlace());

        assertTrue(mdvs9.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (rel)", mdvs9.get(3).getValue());
        assertEquals(3, mdvs9.get(3).getPlace());

        assertTrue(mdvs9.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs9.get(4).getValue());
        assertEquals(4, mdvs9.get(4).getPlace());

        ////////////////////////////////////////
        // after remove 1 - verify volume 3.2 //
        ////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, v1_2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(v1_2, isIssueOfJournalVolume, i1_1, BOTH, 0, 0),
                isRel(v1_2, isIssueOfJournalVolume, i2_1, BOTH, 1, 0),
                isRel(v1_2, isIssueOfJournalVolume, i3_1, LEFT_ONLY, 2, 0),
                // NOTE: left place was reduced by one
                isRel(v1_2, isIssueOfJournalVolume, i4_1, BOTH, 2, 0),
                isRel(v1_2, isIssueOfJournalVolume, i5_1, BOTH, 3, 0)
            ))
        );

        List<MetadataValue> mdvs12 = itemService.getMetadata(
            v1_2, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(4, mdvs12.size());

        assertTrue(mdvs12.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs12.get(0).getValue());
        assertEquals(0, mdvs12.get(0).getPlace());

        assertTrue(mdvs12.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (rel)", mdvs12.get(1).getValue());
        assertEquals(1, mdvs12.get(1).getPlace());

        assertTrue(mdvs12.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (rel)", mdvs12.get(2).getValue());
        assertEquals(2, mdvs12.get(2).getPlace());

        assertTrue(mdvs12.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs12.get(3).getValue());
        assertEquals(3, mdvs12.get(3).getPlace());


        ///////////////////////////////////////////////
        // add relationship - volume 3.2 & issue 3.2 //
        ///////////////////////////////////////////////

        ////////////////////////////////////
        // create new version - issue 3.2 //
        ////////////////////////////////////

        // journal issue 3.3
        Item i3_3 = ItemBuilder.createItem(context, collection)
                .withTitle("journal issue 3")
                .withMetadata("dspace", "entity", "type", journalIssueEntityType.getLabel())
                .withMetadata("publicationissue", "issueNumber", null, "issue nr 3 (rel)")
                .build();

        // relationship - volume 1 & issue 3
        RelationshipBuilder.createRelationshipBuilder(context, v1_2, i3_3, isIssueOfJournalVolume, 2, -1)
                .build();

        context.commit();

        ////////////////////////////////////
        // after remove 1 - cache busting //
        ////////////////////////////////////

        v1_2.setMetadataModified();
        v1_2 = context.reloadEntity(v1_2);

        i3_3.setMetadataModified();
        i3_3 = context.reloadEntity(i3_3);

        ////////////////////////////////////////////////
        // after add relationship - verify volume 3.1 //
        ////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, v1_1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(v1_1, isIssueOfJournalVolume, i1_1, RIGHT_ONLY, 0, 0),
                isRel(v1_1, isIssueOfJournalVolume, i2_1, RIGHT_ONLY, 1, 0),
                isRel(v1_1, isIssueOfJournalVolume, i3_1, RIGHT_ONLY, 2, 0),
                isRel(v1_1, isIssueOfJournalVolume, i4_1, RIGHT_ONLY, 3, 0),
                isRel(v1_1, isIssueOfJournalVolume, i5_1, RIGHT_ONLY, 4, 0)
            ))
        );

        List<MetadataValue> mdvs14 = itemService.getMetadata(
            v1_1, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(5, mdvs14.size());

        assertTrue(mdvs14.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs14.get(0).getValue());
        assertEquals(0, mdvs14.get(0).getPlace());

        assertTrue(mdvs14.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (rel)", mdvs14.get(1).getValue());
        assertEquals(1, mdvs14.get(1).getPlace());

        assertTrue(mdvs14.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs14.get(2).getValue());
        assertEquals(2, mdvs14.get(2).getPlace());

        assertTrue(mdvs14.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (rel)", mdvs14.get(3).getValue());
        assertEquals(3, mdvs14.get(3).getPlace());

        assertTrue(mdvs14.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs14.get(4).getValue());
        assertEquals(4, mdvs14.get(4).getPlace());

        ////////////////////////////////////////
        // after add relationship - verify volume 3.2 //
        ////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, v1_2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(v1_2, isIssueOfJournalVolume, i1_1, BOTH, 0, 0),
                isRel(v1_2, isIssueOfJournalVolume, i2_1, BOTH, 1, 0),
                isRel(v1_2, isIssueOfJournalVolume, i3_1, LEFT_ONLY, 2, 0), //TODO: this test is broken
                    //It uses leftPlace 3, which implies it has been moved while that shouldn't happen
                isRel(v1_2, isIssueOfJournalVolume, i3_3, BOTH, 2, 0),
                isRel(v1_2, isIssueOfJournalVolume, i4_1, BOTH, 3, 0),
                isRel(v1_2, isIssueOfJournalVolume, i5_1, BOTH, 4, 0)
            ))
        );

        assertEquals(
            6,
            relationshipService.countByItem(context, v1_2, false, false)
        );

        List<MetadataValue> mdvs17 = itemService.getMetadata(
            v1_2, "publicationissue", "issueNumber", null, Item.ANY
        );
        assertEquals(5, mdvs17.size());

        assertTrue(mdvs17.get(0) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 1 (rel)", mdvs17.get(0).getValue());
        assertEquals(0, mdvs17.get(0).getPlace());

        assertTrue(mdvs17.get(1) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 2 (rel)", mdvs17.get(1).getValue());
        assertEquals(1, mdvs17.get(1).getPlace());

        assertTrue(mdvs7.get(2) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 3 (rel)", mdvs7.get(2).getValue());
        assertEquals(2, mdvs7.get(2).getPlace());

        assertTrue(mdvs17.get(3) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 4 (rel)", mdvs17.get(3).getValue());
        assertEquals(3, mdvs17.get(3).getPlace());

        assertTrue(mdvs17.get(4) instanceof RelationshipMetadataValue);
        assertEquals("issue nr 5 (rel)", mdvs17.get(4).getValue());
        assertEquals(4, mdvs17.get(4).getPlace());

        // TODO
        // delete mdv from older
        // delete rel from older

        /////////////////////////////////////////////
        // delete volume first for min cardinality //
        /////////////////////////////////////////////

        itemService.delete(context, context.reloadEntity(v1_1));
        itemService.delete(context, context.reloadEntity(v1_2));

    }

    @Test
    public void test_virtualMetadataPreserved() throws Exception {
        //////////////////////////////////////////////
        // create a publication and link two people //
        //////////////////////////////////////////////

        Item publication1V1 = ItemBuilder.createItem(context, collection)
            .withTitle("publication 1V1")
            .withMetadata("dspace", "entity", "type", publicationEntityType.getLabel())
            .build();

        Item person1V1 = ItemBuilder.createItem(context, collection)
            .withTitle("person 1V1")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("Donald")
            .withPersonIdentifierLastName("Smith")
            .build();

        Item person2V1 = ItemBuilder.createItem(context, collection)
            .withTitle("person 2V1")
            .withMetadata("dspace", "entity", "type", personEntityType.getLabel())
            .withPersonIdentifierFirstName("Jane")
            .withPersonIdentifierLastName("Doe")
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, publication1V1, person1V1, isAuthorOfPublication)
            .build();

        RelationshipBuilder.createRelationshipBuilder(context, publication1V1, person2V1, isAuthorOfPublication)
            .withRightwardValue("Doe, J.")
            .build();

        ///////////////////////////////////////////////
        // test dc.contributor.author of publication //
        ///////////////////////////////////////////////

        List<MetadataValue> mdvs1 = itemService.getMetadata(
            publication1V1, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs1.size());

        assertTrue(mdvs1.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, Donald", mdvs1.get(0).getValue());
        assertEquals(0, mdvs1.get(0).getPlace());

        assertTrue(mdvs1.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, J.", mdvs1.get(1).getValue());
        assertEquals(1, mdvs1.get(1).getPlace());

        ////////////////////////////////////////////////////////
        // test relation.isAuthorOfPublication of publication //
        ////////////////////////////////////////////////////////

        List<MetadataValue> mdvsR1 = itemService.getMetadata(
            publication1V1, "relation", "isAuthorOfPublication", null, Item.ANY
        );
        assertEquals(2, mdvsR1.size());

        assertTrue(mdvsR1.get(0) instanceof RelationshipMetadataValue);
        assertEquals(person1V1.getID().toString(), mdvsR1.get(0).getValue());
        assertEquals(0, mdvsR1.get(0).getPlace());

        assertTrue(mdvsR1.get(1) instanceof RelationshipMetadataValue);
        assertEquals(person2V1.getID().toString(), mdvsR1.get(1).getValue());
        assertEquals(1, mdvsR1.get(1).getPlace());

        ///////////////////////////////////////////////////////
        // create a new version of publication 1 and archive //
        ///////////////////////////////////////////////////////

        Item publication1V2 = versioningService.createNewVersion(context, publication1V1).getItem();
        installItemService.installItem(context, workspaceItemService.findByItem(context, publication1V2));
        context.dispatchEvents();

        ////////////////////////////////////
        // create new version of person 1 //
        ////////////////////////////////////

        Item person1V2 = versioningService.createNewVersion(context, person1V1).getItem();
        // update "Smith, Donald" to "Smith, D."
        itemService.replaceMetadata(
            context, person1V2, "person", "givenName", null, null, "D.",
            null, -1, 0
        );
        itemService.update(context, person1V2);

        ///////////////////
        // cache busting //
        ///////////////////

        publication1V1.setMetadataModified();
        publication1V1 = context.reloadEntity(publication1V1);

        publication1V2.setMetadataModified();
        publication1V2 = context.reloadEntity(publication1V2);

        ///////////////////////////////////////////////////
        // test dc.contributor.author of old publication //
        ///////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, publication1V1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1V1, isAuthorOfPublication, person1V1, RIGHT_ONLY, 0, 0),
                isRel(publication1V1, isAuthorOfPublication, person2V1, RIGHT_ONLY, null, "Doe, J.", 1, 0)
            ))
        );

        List<MetadataValue> mdvs2 = itemService.getMetadata(
            publication1V1, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs2.size());

        assertTrue(mdvs2.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, Donald", mdvs2.get(0).getValue());
        assertEquals(0, mdvs2.get(0).getPlace());

        assertTrue(mdvs2.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, J.", mdvs2.get(1).getValue());
        assertEquals(1, mdvs2.get(1).getPlace());

        ////////////////////////////////////////////////////////////
        // test relation.isAuthorOfPublication of old publication //
        ////////////////////////////////////////////////////////////

        List<MetadataValue> mdvsR2 = itemService.getMetadata(
            publication1V1, "relation", "isAuthorOfPublication", null, Item.ANY
        );
        assertEquals(2, mdvsR2.size());

        assertTrue(mdvsR2.get(0) instanceof RelationshipMetadataValue);
        assertEquals(person1V1.getID().toString(), mdvsR2.get(0).getValue());
        assertEquals(0, mdvsR2.get(0).getPlace());

        assertTrue(mdvsR2.get(1) instanceof RelationshipMetadataValue);
        assertEquals(person2V1.getID().toString(), mdvsR2.get(1).getValue());
        assertEquals(1, mdvsR2.get(1).getPlace());

        ///////////////////////////////////////////////////
        // test dc.contributor.author of new publication //
        ///////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, publication1V2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1V2, isAuthorOfPublication, person1V1, BOTH, 0, 0),
                isRel(publication1V2, isAuthorOfPublication, person1V2, LEFT_ONLY, 0, 0),
                isRel(publication1V2, isAuthorOfPublication, person2V1, BOTH, null, "Doe, J.", 1, 0)
            ))
        );

        List<MetadataValue> mdvs3 = itemService.getMetadata(
            publication1V2, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs3.size());

        assertTrue(mdvs3.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, Donald", mdvs3.get(0).getValue());
        assertEquals(0, mdvs3.get(0).getPlace());

        assertTrue(mdvs3.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, J.", mdvs3.get(1).getValue());
        assertEquals(1, mdvs3.get(1).getPlace());

        ////////////////////////////////////////////////////////////
        // test relation.isAuthorOfPublication of new publication //
        ////////////////////////////////////////////////////////////

        List<MetadataValue> mdvsR3 = itemService.getMetadata(
            publication1V2, "relation", "isAuthorOfPublication", null, Item.ANY
        );
        assertEquals(2, mdvsR3.size());// TODO expect 3

        assertTrue(mdvsR3.get(0) instanceof RelationshipMetadataValue);
        assertEquals(person1V1.getID().toString(), mdvsR3.get(0).getValue());
        assertEquals(0, mdvsR3.get(0).getPlace());

        assertTrue(mdvsR3.get(1) instanceof RelationshipMetadataValue);
        assertEquals(person2V1.getID().toString(), mdvsR3.get(1).getValue());
        assertEquals(1, mdvsR3.get(1).getPlace());

        /////////////////////////////////////
        // archive new version of person 1 //
        /////////////////////////////////////

        installItemService.installItem(context, workspaceItemService.findByItem(context, person1V2));
        context.dispatchEvents();

        ///////////////////
        // cache busting //
        ///////////////////

        publication1V1.setMetadataModified();
        publication1V1 = context.reloadEntity(publication1V1);

        publication1V2.setMetadataModified();
        publication1V2 = context.reloadEntity(publication1V2);

        ///////////////////////////////////////////////////
        // test dc.contributor.author of old publication //
        ///////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, publication1V1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1V1, isAuthorOfPublication, person1V1, RIGHT_ONLY, 0, 0),
                isRel(publication1V1, isAuthorOfPublication, person2V1, RIGHT_ONLY, null, "Doe, J.", 1, 0)
            ))
        );

        List<MetadataValue> mdvs4 = itemService.getMetadata(
            publication1V1, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs4.size());

        assertTrue(mdvs4.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, Donald", mdvs4.get(0).getValue());
        assertEquals(0, mdvs4.get(0).getPlace());

        assertTrue(mdvs4.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, J.", mdvs4.get(1).getValue());
        assertEquals(1, mdvs4.get(1).getPlace());

        ////////////////////////////////////////////////////////////
        // test relation.isAuthorOfPublication of old publication //
        ////////////////////////////////////////////////////////////

        // TODO

        ///////////////////////////////////////////////////
        // test dc.contributor.author of new publication //
        ///////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, publication1V2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1V2, isAuthorOfPublication, person1V1, LEFT_ONLY, 0, 0),
                isRel(publication1V2, isAuthorOfPublication, person1V2, BOTH, 0, 0),
                isRel(publication1V2, isAuthorOfPublication, person2V1, BOTH, null, "Doe, J.", 1, 0)
            ))
        );

        List<MetadataValue> mdvs5 = itemService.getMetadata(
            publication1V2, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs5.size());

        assertTrue(mdvs5.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, D.", mdvs5.get(0).getValue());
        assertEquals(0, mdvs5.get(0).getPlace());

        assertTrue(mdvs5.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, J.", mdvs5.get(1).getValue());
        assertEquals(1, mdvs5.get(1).getPlace());

        ////////////////////////////////////////////////////////////
        // test relation.isAuthorOfPublication of new publication //
        ////////////////////////////////////////////////////////////

        // TODO

        ////////////////////////////////////
        // create new version of person 2 //
        ////////////////////////////////////

        Item person2V2 = versioningService.createNewVersion(context, person2V1).getItem();
        Relationship rel1 = getRelationship(publication1V2, isAuthorOfPublication, person2V2);
        assertNotNull(rel1);
        rel1.setRightwardValue("Doe, Jane Jr");
        relationshipService.update(context, rel1);

        ///////////////////
        // cache busting //
        ///////////////////

        publication1V1.setMetadataModified();
        publication1V1 = context.reloadEntity(publication1V1);

        publication1V2.setMetadataModified();
        publication1V2 = context.reloadEntity(publication1V2);

        ///////////////////////////////////////////////////
        // test dc.contributor.author of old publication //
        ///////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, publication1V1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1V1, isAuthorOfPublication, person1V1, RIGHT_ONLY, 0, 0),
                isRel(publication1V1, isAuthorOfPublication, person2V1, RIGHT_ONLY, null, "Doe, J.", 1, 0)
            ))
        );

        List<MetadataValue> mdvs6 = itemService.getMetadata(
            publication1V1, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs6.size());

        assertTrue(mdvs6.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, Donald", mdvs6.get(0).getValue());
        assertEquals(0, mdvs6.get(0).getPlace());

        assertTrue(mdvs6.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, J.", mdvs6.get(1).getValue());
        assertEquals(1, mdvs6.get(1).getPlace());

        ////////////////////////////////////////////////////////////
        // test relation.isAuthorOfPublication of old publication //
        ////////////////////////////////////////////////////////////

        // TODO

        ///////////////////////////////////////////////////
        // test dc.contributor.author of new publication //
        ///////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, publication1V2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1V2, isAuthorOfPublication, person1V1, LEFT_ONLY, 0, 0),
                isRel(publication1V2, isAuthorOfPublication, person1V2, BOTH, 0, 0),
                isRel(publication1V2, isAuthorOfPublication, person2V1, BOTH, null, "Doe, J.", 1, 0),
                isRel(publication1V2, isAuthorOfPublication, person2V2, LEFT_ONLY, null, "Doe, Jane Jr", 1, 0)
            ))
        );

        List<MetadataValue> mdvs7 = itemService.getMetadata(
            publication1V2, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs7.size());

        assertTrue(mdvs7.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, D.", mdvs7.get(0).getValue());
        assertEquals(0, mdvs7.get(0).getPlace());

        assertTrue(mdvs7.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, J.", mdvs7.get(1).getValue());
        assertEquals(1, mdvs7.get(1).getPlace());

        ////////////////////////////////////////////////////////////
        // test relation.isAuthorOfPublication of new publication //
        ////////////////////////////////////////////////////////////

        // TODO

        /////////////////////////////////////
        // archive new version of person 2 //
        /////////////////////////////////////

        installItemService.installItem(context, workspaceItemService.findByItem(context, person2V2));
        context.dispatchEvents();

        ///////////////////
        // cache busting //
        ///////////////////

        publication1V1.setMetadataModified();
        publication1V1 = context.reloadEntity(publication1V1);

        publication1V2.setMetadataModified();
        publication1V2 = context.reloadEntity(publication1V2);

        ///////////////////////////////////////////////////
        // test dc.contributor.author of old publication //
        ///////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, publication1V1, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1V1, isAuthorOfPublication, person1V1, RIGHT_ONLY, 0, 0),
                isRel(publication1V1, isAuthorOfPublication, person2V1, RIGHT_ONLY, null, "Doe, J.", 1, 0)
            ))
        );

        List<MetadataValue> mdvs8 = itemService.getMetadata(
            publication1V1, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs8.size());

        assertTrue(mdvs8.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, Donald", mdvs8.get(0).getValue());
        assertEquals(0, mdvs8.get(0).getPlace());

        assertTrue(mdvs8.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, J.", mdvs8.get(1).getValue());
        assertEquals(1, mdvs8.get(1).getPlace());

        ////////////////////////////////////////////////////////////
        // test relation.isAuthorOfPublication of old publication //
        ////////////////////////////////////////////////////////////

        // TODO

        ///////////////////////////////////////////////////
        // test dc.contributor.author of new publication //
        ///////////////////////////////////////////////////

        assertThat(
            relationshipService.findByItem(context, publication1V2, -1, -1, false, false),
            containsInAnyOrder(List.of(
                isRel(publication1V2, isAuthorOfPublication, person1V1, LEFT_ONLY, 0, 0),
                isRel(publication1V2, isAuthorOfPublication, person1V2, BOTH, 0, 0),
                isRel(publication1V2, isAuthorOfPublication, person2V1, LEFT_ONLY, null, "Doe, J.", 1, 0),
                isRel(publication1V2, isAuthorOfPublication, person2V2, BOTH, null, "Doe, Jane Jr", 1, 0)
            ))
        );

        List<MetadataValue> mdvs9 = itemService.getMetadata(
            publication1V2, "dc", "contributor", "author", Item.ANY
        );
        assertEquals(2, mdvs9.size());

        assertTrue(mdvs9.get(0) instanceof RelationshipMetadataValue);
        assertEquals("Smith, D.", mdvs9.get(0).getValue());
        assertEquals(0, mdvs9.get(0).getPlace());

        assertTrue(mdvs9.get(1) instanceof RelationshipMetadataValue);
        assertEquals("Doe, Jane Jr", mdvs9.get(1).getValue());
        assertEquals(1, mdvs9.get(1).getPlace());

        ////////////////////////////////////////////////////////////
        // test relation.isAuthorOfPublication of new publication //
        ////////////////////////////////////////////////////////////

        // TODO
    }

    // TODO
}
