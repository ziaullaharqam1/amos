package com.amos.ams.agent;

import com.amos.ams.domain.User;
import com.amos.ams.dto.Dtos;
import com.amos.ams.repository.AircraftRepository;
import com.amos.ams.repository.CheckTypeRepository;
import com.amos.ams.repository.UserRepository;
import com.amos.ams.service.ActivityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class WorkflowAgentToolsTest {

    @Test
    void assignIsDryRunWhenApplyIsFalse() {
        ActivityService activities = mock(ActivityService.class);
        UserRepository users = mock(UserRepository.class);
        WorkflowAgentTools tools = new WorkflowAgentTools(activities, users, mock(AircraftRepository.class),
                mock(CheckTypeRepository.class), new ObjectMapper());
        User actor = new User();
        actor.setUsername("manager");

        String json = tools.execute("assign_work_order", "{\"activityId\":9,\"userId\":2}", actor, false);

        assertTrue(json.contains("dryRun"));
        verify(activities, never()).assign(anyLong(), any(), any());
    }

    @Test
    void assignCallsServiceWhenApplyIsTrue() {
        ActivityService activities = mock(ActivityService.class);
        UserRepository users = mock(UserRepository.class);
        WorkflowAgentTools tools = new WorkflowAgentTools(activities, users, mock(AircraftRepository.class),
                mock(CheckTypeRepository.class), new ObjectMapper());
        User actor = new User();
        when(activities.assign(eq(9L), any(), eq(actor))).thenReturn(new Dtos.ActivityDetail(
                9L, "WO-2026-1001", "Leak", null, 1L, "A6-ABC", null, null, null, null, null, null, null,
                "ASSIGNED", "HIGH", null, null, null, null, null, 2L, "Omar Haddad", null, null, null, null,
                "DXB", null, null, java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of()
        ));

        String json = tools.execute("assign_work_order", "{\"activityId\":9,\"userId\":2}", actor, true);

        assertTrue(json.contains("WO-2026-1001"));
        verify(activities).assign(eq(9L), any(), eq(actor));
    }

    @Test
    void createCallsServiceWhenApplyIsTrue() {
        ActivityService activities = mock(ActivityService.class);
        AircraftRepository aircraft = mock(AircraftRepository.class);
        CheckTypeRepository checkTypes = mock(CheckTypeRepository.class);
        WorkflowAgentTools tools = new WorkflowAgentTools(activities, mock(UserRepository.class), aircraft,
                checkTypes, new ObjectMapper());
        User actor = new User();
        actor.setStation("DXB");
        com.amos.ams.domain.Aircraft ac = new com.amos.ams.domain.Aircraft();
        ac.setId(4L);
        ac.setRegistration("A6-DREAM");
        when(aircraft.findByRegistration("A6-DREAM")).thenReturn(java.util.Optional.of(ac));
        when(aircraft.findById(4L)).thenReturn(java.util.Optional.of(ac));
        when(checkTypes.findByCode("UNSCHE")).thenReturn(java.util.Optional.empty());
        when(activities.create(any(), eq(actor))).thenReturn(new Dtos.ActivityDetail(
                12L, "WO-2026-1010", "Pack 2 inop", null, 4L, "A6-DREAM", null, null, null, null, null, null, null,
                "PENDING", "AOG", null, null, null, null, null, null, null, null, null, null, null,
                "AUH", null, null, java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of()
        ));

        String json = tools.execute("create_work_order",
                "{\"title\":\"Pack 2 inop\",\"registration\":\"A6-DREAM\",\"priority\":\"AOG\"}", actor, true);

        assertTrue(json.contains("WO-2026-1010"));
        assertTrue(json.contains("created"));
        verify(activities).create(any(), eq(actor));
    }
}
