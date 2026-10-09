package com.amos.ams.workflow;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowEngineTest {

    @Test
    void technicianCanCompleteAssignedWork() {
        assertDoesNotThrow(() -> WorkflowEngine.assertTransition("IN_PROGRESS", "COMPLETED", Set.of("ACTIVITY_WORK")));
    }

    @Test
    void technicianCannotVerify() {
        assertThrows(Exception.class, () ->
                WorkflowEngine.assertTransition("COMPLETED", "VERIFIED", Set.of("ACTIVITY_WORK")));
    }

    @Test
    void qaCanVerifyCompletedWork() {
        assertDoesNotThrow(() -> WorkflowEngine.assertTransition("COMPLETED", "VERIFIED", Set.of("ACTIVITY_VERIFY")));
    }

    @Test
    void illegalJumpIsRejected() {
        assertThrows(Exception.class, () ->
                WorkflowEngine.assertTransition("PENDING", "VERIFIED", Set.of("ROLE_ADMIN")));
    }

    @Test
    void allowedTargetsHonorPermissions() {
        assertTrue(WorkflowEngine.allowedTargets("COMPLETED", Set.of("ACTIVITY_VERIFY")).contains("VERIFIED"));
        assertFalse(WorkflowEngine.allowedTargets("COMPLETED", Set.of("ACTIVITY_WORK")).contains("VERIFIED"));
    }
}
