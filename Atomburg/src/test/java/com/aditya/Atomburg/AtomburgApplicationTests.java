package com.aditya.Atomburg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AtomburgApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void employeeManagerAndReportingJourneyWorks() throws Exception {
        MvcResult addGoalResult = mockMvc.perform(post("/api/goal-sheets/me/goals")
                        .header("X-User-Id", "emp-1")
                        .queryParam("year", "2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "thrustArea": "Pipeline",
                                  "title": "Launch channel partner program",
                                  "description": "Activate the first set of referral partners.",
                                  "uomType": "NUMERIC",
                                  "direction": "HIGHER_IS_BETTER",
                                  "targetValue": "30",
                                  "weightage": 40
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalWeightage").value(100))
                .andReturn();

        JsonNode addGoalBody = objectMapper.readTree(addGoalResult.getResponse().getContentAsString());
        JsonNode goals = addGoalBody.get("goals");
        long newGoalId = findGoalId(goals, "Launch channel partner program");
        long pipelineGoalId = findGoalId(goals, "Grow inside-sales pipeline");
        long onboardingGoalId = findGoalId(goals, "Reduce onboarding turnaround time");

        mockMvc.perform(post("/api/goal-sheets/me/submit")
                        .header("X-User-Id", "emp-1")
                        .queryParam("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        mockMvc.perform(post("/api/manager/goal-sheets/emp-1/approve")
                        .header("X-User-Id", "mgr-1")
                        .queryParam("year", "2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment": "Approved for execution."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(put("/api/admin/cycles/2026")
                        .header("X-User-Id", "admin-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "windows": [
                                    { "period": "GOAL_SETTING", "month": 1, "dayOfMonth": 1, "yearOffset": 0, "action": "Goal Setting" },
                                    { "period": "Q1", "month": 5, "dayOfMonth": 1, "yearOffset": 0, "action": "Q1 Check-in" },
                                    { "period": "Q2", "month": 8, "dayOfMonth": 1, "yearOffset": 0, "action": "Q2 Check-in" },
                                    { "period": "Q3", "month": 11, "dayOfMonth": 1, "yearOffset": 0, "action": "Q3 Check-in" },
                                    { "period": "Q4", "month": 2, "dayOfMonth": 1, "yearOffset": 1, "action": "Q4 Check-in" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026));

        updateNumericCheckIn("emp-1", 2026, "Q1", pipelineGoalId, "90", "COMPLETED");
        updateNumericCheckIn("emp-1", 2026, "Q1", onboardingGoalId, "4", "COMPLETED");
        updateNumericCheckIn("emp-1", 2026, "Q1", newGoalId, "24", "ON_TRACK");

        mockMvc.perform(post("/api/manager/check-ins/emp-1/Q1/comment")
                        .header("X-User-Id", "mgr-1")
                        .queryParam("year", "2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "comment": "Good first checkpoint. Keep partner activation moving."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews[?(@.period=='Q1')].managerCompleted").value(org.hamcrest.Matchers.contains(true)));

        mockMvc.perform(get("/api/dashboard/completion")
                        .header("X-User-Id", "mgr-1")
                        .queryParam("year", "2026")
                        .queryParam("period", "Q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees").value(3))
                .andExpect(jsonPath("$.employeesCompleted").value(1))
                .andExpect(jsonPath("$.managersCompleted").value(1));

        MvcResult reportResult = mockMvc.perform(get("/api/reports/achievement")
                        .header("X-User-Id", "mgr-1")
                        .queryParam("year", "2026")
                        .queryParam("period", "Q1"))
                .andExpect(status().isOk())
                .andReturn();

        String reportCsv = reportResult.getResponse().getContentAsString();
        assertThat(reportCsv).contains("Ethan Employee");
        assertThat(reportCsv).contains("Launch channel partner program");
        assertThat(reportCsv).contains("Good first checkpoint. Keep partner activation moving.");
    }

    @Test
    void sharedGoalPrimaryOwnerUpdatesSyncToRecipients() throws Exception {
        mockMvc.perform(put("/api/admin/cycles/2025")
                        .header("X-User-Id", "admin-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "windows": [
                                    { "period": "GOAL_SETTING", "month": 8, "dayOfMonth": 1, "yearOffset": 0, "action": "Goal Setting" },
                                    { "period": "Q1", "month": 9, "dayOfMonth": 1, "yearOffset": 0, "action": "Q1 Check-in" },
                                    { "period": "Q2", "month": 11, "dayOfMonth": 1, "yearOffset": 0, "action": "Q2 Check-in" },
                                    { "period": "Q3", "month": 1, "dayOfMonth": 1, "yearOffset": 1, "action": "Q3 Check-in" },
                                    { "period": "Q4", "month": 3, "dayOfMonth": 1, "yearOffset": 1, "action": "Q4 Check-in" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult ownerSheetResult = mockMvc.perform(get("/api/goal-sheets/me")
                        .header("X-User-Id", "emp-1")
                        .queryParam("year", "2025"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode ownerSheet = objectMapper.readTree(ownerSheetResult.getResponse().getContentAsString());
        long sharedGoalId = findGoalIdByOwnership(ownerSheet.get("goals"), "PRIMARY_OWNER");

        mockMvc.perform(put("/api/check-ins/me/Q4/goals/{goalId}", sharedGoalId)
                        .header("X-User-Id", "emp-1")
                        .queryParam("year", "2025")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actualValue": "90",
                                  "status": "COMPLETED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews[?(@.period=='Q4')].checkIns[0].actualValue").value(org.hamcrest.Matchers.contains("90")));

        mockMvc.perform(get("/api/manager/goal-sheets/emp-2")
                        .header("X-User-Id", "mgr-1")
                        .queryParam("year", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviews[?(@.period=='Q4')].checkIns[0].actualValue").value(org.hamcrest.Matchers.contains("90")))
                .andExpect(jsonPath("$.reviews[?(@.period=='Q4')].checkIns[0].status").value(org.hamcrest.Matchers.contains("COMPLETED")));
    }

    private void updateNumericCheckIn(String actorId, int year, String period, long goalId, String actualValue, String status)
            throws Exception {
        mockMvc.perform(put("/api/check-ins/me/{period}/goals/{goalId}", period, goalId)
                        .header("X-User-Id", actorId)
                        .queryParam("year", Integer.toString(year))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "actualValue": "%s",
                                  "status": "%s"
                                }
                                """.formatted(actualValue, status)))
                .andExpect(status().isOk());
    }

    private long findGoalId(JsonNode goals, String title) {
        for (JsonNode goal : goals) {
            if (title.equals(goal.get("title").asText())) {
                return goal.get("id").asLong();
            }
        }
        throw new IllegalStateException("Goal not found: " + title);
    }

    private long findGoalIdByOwnership(JsonNode goals, String ownership) {
        for (JsonNode goal : goals) {
            if (ownership.equals(goal.get("sharedGoalOwnership").asText())) {
                return goal.get("id").asLong();
            }
        }
        throw new IllegalStateException("Shared goal not found for ownership: " + ownership);
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-05-17T04:30:00Z"), ZoneId.of("Asia/Calcutta"));
        }
    }
}
