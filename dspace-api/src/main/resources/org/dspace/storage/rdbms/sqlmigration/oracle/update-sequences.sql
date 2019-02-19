--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--
-- update-sequences.sql
--
-- Copyright (c) 2002-2016, The DSpace Foundation.  All rights reserved.
-- 
-- Redistribution and use in source and binary forms, with or without
-- modification, are permitted provided that the following conditions are
-- met:
-- 
-- - Redistributions of source code must retain the above copyright
-- notice, this list of conditions and the following disclaimer.
-- 
-- - Redistributions in binary form must reproduce the above copyright
-- notice, this list of conditions and the following disclaimer in the
-- documentation and/or other materials provided with the distribution.
-- 
-- - Neither the name of the DSpace Foundation nor the names of its
-- contributors may be used to endorse or promote products derived from
-- this software without specific prior written permission.
-- 
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
-- ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
-- LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
-- A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
-- HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
-- INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
-- BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
-- OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
-- ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
-- TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
-- USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
-- DAMAGE.

-- SQL code to update the ID (primary key) generating sequences, if some
-- import operation has set explicit IDs.
--
-- Sequences are used to generate IDs for new rows in the database.  If a
-- bulk import operation, such as an SQL dump, specifies primary keys for
-- imported data explicitly, the sequences are out of sync and need updating.
-- This SQL code does just that.
--
-- This should rarely be needed; any bulk import should be performed using the
-- org.dspace.content API which is safe to use concurrently and in multiple
-- JVMs.  The SQL code below will typically only be required after a direct
-- SQL data dump from a backup or somesuch.

-- Depends on being run from sqlplus with incseq.sql in the current path
-- you can find incseq.sql at: http://www.akadia.com/services/scripts/incseq.sql
-- Here that script was renamed to updateseq.sql.

DECLARE
 PROCEDURE updateseq ( seq IN VARCHAR,
                       tbl IN VARCHAR,
                       attr IN VARCHAR,
                       cond IN VARCHAR ) IS
   curr NUMBER := 0;
   BEGIN
     EXECUTE IMMEDIATE 'SELECT max('
             || attr
             || ') INTO curr FROM '
             || tbl || ' ' || cond;
     curr := curr + 1;
     EXECUTE IMMEDIATE 'DROP SEQUENCE ' || seq;
     EXECUTE IMMEDIATE 'CREATE SEQUENCE '
             || seq
             || ' START WITH '
             || NVL(curr, 1);
   END;
BEGIN
 updateseq('seq', 'tbl', 'attr', '');
END;
/

execute updateseq(bitstreamformatregistry_seq, bitstreamformatregistry, bitstream_format_id, "");
execute updateseq(fileextension_seq, fileextension, file_extension_id, "");
execute updateseq(resourcepolicy_seq, resourcepolicy, policy_id, "");
execute updateseq(workspaceitem_seq, workspaceitem, workspace_item_id, "");
execute updateseq(workflowitem_seq, workflowitem, workflow_id, "");
execute updateseq(tasklistitem_seq, tasklistitem, tasklist_id, "");
execute updateseq(registrationdata_seq, registrationdata, registrationdata_id, "");
execute updateseq(subscription_seq, subscription, subscription_id, "");
execute updateseq(metadatafieldregistry_seq, metadatafieldregistry, metadata_field_id, "");
execute updateseq(metadatavalue_seq, metadatavalue, metadata_value_id, "");
execute updateseq(metadataschemaregistry_seq, metadataschemaregistry, metadata_schema_id, "");
execute updateseq(harvested_collection_seq, harvested_collection, id, "");
execute updateseq(harvested_item_seq, harvested_item, id, "");
execute updateseq(webapp_seq, webapp, webapp_id, "");
execute updateseq(requestitem_seq, requestitem, requestitem_id, "");
execute updateseq(handle_id_seq, handle, handle_id, "");

-- Handle Sequence is a special case.  Since Handles minted by DSpace use the 'handle_seq',
-- we need to ensure the next assigned handle will *always* be unique.  So, 'handle_seq'
-- always needs to be set to the value of the *largest* handle suffix.  That way when the
-- next handle is assigned, it will use the next largest number. This query does the following:
--  For all 'handle' values which have a number in their suffix (after '/'), find the maximum
--  suffix value, convert it to a number, and set the 'handle_seq' to start at the next value
-- (see updateseq.sql script for more)
execute updateseq(handle_seq, handle, "to_number(regexp_replace(handle, '.*/', ''), '999999999999')", "WHERE REGEXP_LIKE(handle, '^.*/[0123456789]*$')");
