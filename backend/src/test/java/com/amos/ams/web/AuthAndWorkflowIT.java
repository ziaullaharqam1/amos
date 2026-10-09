package com.amos.ams.web;

import com.amos.ams.AmsApplication;
import com.amos.ams.dto.Dtos;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.amos.ams.repository.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = AmsApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndWorkflowIT {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void passwords() {
        users.findAll().forEach(u -> {
            u.setPasswordHash(encoder.encode("Password123!"));
            users.save(u);
        });
    }

    @Test
    void loginAndDashboard() throws Exception {
        String token = login("manager");
        mvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openActivities").exists());
    }

    @Test
    void technicianCannotAccessAudit() throws Exception {
        String token = login("tech1");
        mvc.perform(get("/api/audit-logs").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void qaCanAccessAudit() throws Exception {
        String token = login("qa");
        mvc.perform(get("/api/audit-logs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void createAssignAndWorkActivity() throws Exception {
        String manager = login("manager");
        String tech = login("tech1");
        JsonNode fleet = mapper.readTree(mvc.perform(get("/api/aircraft")
                .header("Authorization", "Bearer " + manager)).andReturn().getResponse().getContentAsString());
        JsonNode checks = mapper.readTree(mvc.perform(get("/api/check-types")
                .header("Authorization", "Bearer " + manager)).andReturn().getResponse().getContentAsString());
        JsonNode tasks = mapper.readTree(mvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + manager)).andReturn().getResponse().getContentAsString());
        long aircraftId = fleet.get(0).get("id").asLong();
        long checkId = 0;
        for (JsonNode n : checks) {
            if ("UNSCHE".equals(n.get("code").asText())) checkId = n.get("id").asLong();
        }
        long taskId = tasks.get(0).get("id").asLong();
        String body = mapper.writeValueAsString(new Dtos.ActivityCreate(
                "Oil leak investigation", "Left engine seep", aircraftId, checkId, taskId, null, null, null,
                "HIGH", java.time.Instant.now().plusSeconds(86400), null, null, "DXB"));
        String created = mvc.perform(post("/api/activities")
                        .header("Authorization", "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).get("id").asLong();

        JsonNode usersJson = mapper.readTree(mvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + login("admin"))).andReturn().getResponse().getContentAsString());
        long techId = 0;
        for (JsonNode n : usersJson) {
            if ("tech1".equals(n.get("username").asText())) techId = n.get("id").asLong();
        }
        mvc.perform(post("/api/activities/" + id + "/assign")
                        .header("Authorization", "Bearer " + manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + techId + ",\"roleOnJob\":\"LEAD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ASSIGNED"));

        mvc.perform(post("/api/activities/" + id + "/transition")
                        .header("Authorization", "Bearer " + tech)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toState\":\"IN_PROGRESS\",\"comment\":\"Started\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("IN_PROGRESS"));
    }

    @Test
    void generateWorkOrderFromSchedule() throws Exception {
        String manager = login("manager");
        JsonNode schedules = mapper.readTree(mvc.perform(get("/api/schedules")
                .header("Authorization", "Bearer " + manager)).andReturn().getResponse().getContentAsString());
        long scheduleId = schedules.get(0).get("id").asLong();
        mvc.perform(post("/api/schedules/" + scheduleId + "/generate")
                        .header("Authorization", "Bearer " + manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PENDING"))
                .andExpect(jsonPath("$.scheduleId").value(scheduleId));
        mvc.perform(post("/api/schedules/" + scheduleId + "/generate")
                        .header("Authorization", "Bearer " + manager))
                .andExpect(status().isConflict());
    }

    private String login(String username) throws Exception {
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("token").asText();
    }
}
