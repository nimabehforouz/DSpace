/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.state.actions.userassignment;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.dspace.core.Context;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.RoleMembers;
import org.dspace.xmlworkflow.WorkflowConfigurationException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;

/**
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class AssignAction extends UserSelectionAction {

    @Override
    public void activate(Context c, WorkflowItem wfItem) {
    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) {
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<>();
    }

    public void generateTasks() {
    }

    @Override
    public boolean isFinished(WorkflowItem wfi) {
        return false;
    }

    @Override
    public void regenerateTasks(Context c, WorkflowItem wfi, RoleMembers roleMembers) throws SQLException {
    }

    @Override
    public boolean isValidUserSelection(Context context, WorkflowItem wfi, boolean hasUI)
        throws WorkflowConfigurationException, SQLException {
        return false;
    }

    @Override
    public boolean usesTaskPool() {
        return false;
    }
}
