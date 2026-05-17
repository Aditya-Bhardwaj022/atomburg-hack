package com.aditya.Atomburg.api;

import com.aditya.Atomburg.domain.PortalDomain.ReviewPeriod;
import com.aditya.Atomburg.service.GoalPortalService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PortalController {
    private final GoalPortalService goalPortalService;

    public PortalController(GoalPortalService goalPortalService) {
        this.goalPortalService = goalPortalService;
    }

    @GetMapping("/session")
    public PortalDtos.SessionResponse session(@RequestParam(required = false) Integer year) {
        return goalPortalService.getSession(year);
    }

    @GetMapping("/users")
    public List<PortalDtos.UserResponse> users() {
        return goalPortalService.listUsers();
    }

    @GetMapping("/cycles/{year}")
    public PortalDtos.GoalCycleResponse cycle(@PathVariable int year) {
        return goalPortalService.getCycle(year);
    }

    @GetMapping("/goal-sheets/me")
    public PortalDtos.GoalSheetResponse myGoalSheet(HttpServletRequest request, @RequestParam int year) {
        return goalPortalService.getMyGoalSheet(actorId(request), year);
    }

    @PostMapping("/goal-sheets/me/goals")
    public PortalDtos.GoalSheetResponse addGoal(HttpServletRequest request, @RequestParam int year,
                                     @RequestBody PortalDtos.CreateGoalRequest payload) {
        return goalPortalService.addGoal(actorId(request), year, payload);
    }

    @PutMapping("/goal-sheets/me/goals/{goalId}")
    public PortalDtos.GoalSheetResponse updateGoal(HttpServletRequest request, @RequestParam int year,
                                                   @PathVariable long goalId,
                                                   @RequestBody PortalDtos.UpdateGoalRequest payload) {
        return goalPortalService.updateGoal(actorId(request), year, goalId, payload);
    }

    @DeleteMapping("/goal-sheets/me/goals/{goalId}")
    public PortalDtos.GoalSheetResponse deleteGoal(HttpServletRequest request, @RequestParam int year,
                                                   @PathVariable long goalId) {
        return goalPortalService.deleteGoal(actorId(request), year, goalId);
    }

    @PostMapping("/goal-sheets/me/submit")
    public PortalDtos.GoalSheetResponse submitGoalSheet(HttpServletRequest request, @RequestParam int year) {
        return goalPortalService.submitGoalSheet(actorId(request), year);
    }

    @GetMapping("/manager/team")
    public List<PortalDtos.TeamMemberSummaryResponse> team(HttpServletRequest request, @RequestParam int year,
                                                @RequestParam(required = false) ReviewPeriod period) {
        return goalPortalService.getManagerTeam(actorId(request), year, period);
    }

    @GetMapping("/manager/goal-sheets/{employeeId}")
    public PortalDtos.GoalSheetResponse employeeGoalSheet(HttpServletRequest request, @PathVariable String employeeId,
                                                          @RequestParam int year) {
        return goalPortalService.getEmployeeSheet(actorId(request), employeeId, year);
    }

    @PutMapping("/manager/goal-sheets/{employeeId}/goals/{goalId}")
    public PortalDtos.GoalSheetResponse updateGoalDuringApproval(HttpServletRequest request,
                                                                 @PathVariable String employeeId,
                                                                 @RequestParam int year,
                                                                 @PathVariable long goalId,
                                                                 @RequestBody PortalDtos.ManagerGoalUpdateRequest payload) {
        return goalPortalService.updateGoalDuringApproval(actorId(request), employeeId, year, goalId, payload);
    }

    @PostMapping("/manager/goal-sheets/{employeeId}/approve")
    public PortalDtos.GoalSheetResponse approveSheet(HttpServletRequest request, @PathVariable String employeeId,
                                                     @RequestParam int year,
                                                     @RequestBody(required = false) PortalDtos.DecisionRequest payload) {
        return goalPortalService.approveSheet(actorId(request), employeeId, year, payload);
    }

    @PostMapping("/manager/goal-sheets/{employeeId}/return")
    public PortalDtos.GoalSheetResponse returnForRework(HttpServletRequest request, @PathVariable String employeeId,
                                                        @RequestParam int year,
                                                        @RequestBody PortalDtos.DecisionRequest payload) {
        return goalPortalService.returnForRework(actorId(request), employeeId, year, payload);
    }

    @PostMapping("/manager/shared-goals")
    public PortalDtos.GoalSheetResponse pushSharedGoal(HttpServletRequest request,
                                                       @RequestBody PortalDtos.PushSharedGoalRequest payload) {
        return goalPortalService.pushSharedGoal(actorId(request), payload);
    }

    @PutMapping("/check-ins/me/{period}/goals/{goalId}")
    public PortalDtos.GoalSheetResponse updateCheckIn(HttpServletRequest request, @PathVariable ReviewPeriod period,
                                                      @RequestParam int year, @PathVariable long goalId,
                                                      @RequestBody PortalDtos.CheckInRequest payload) {
        return goalPortalService.updateCheckIn(actorId(request), year, period, goalId, payload);
    }

    @PostMapping("/manager/check-ins/{employeeId}/{period}/comment")
    public PortalDtos.GoalSheetResponse addManagerComment(HttpServletRequest request, @PathVariable String employeeId,
                                                          @PathVariable ReviewPeriod period, @RequestParam int year,
                                                          @RequestBody PortalDtos.ManagerCommentRequest payload) {
        return goalPortalService.addManagerComment(actorId(request), employeeId, year, period, payload);
    }

    @GetMapping("/dashboard/completion")
    public PortalDtos.CompletionDashboardResponse completionDashboard(HttpServletRequest request,
                                                                      @RequestParam int year,
                                                                      @RequestParam ReviewPeriod period) {
        return goalPortalService.getCompletionDashboard(actorId(request), year, period);
    }

    @GetMapping("/audit-logs")
    public List<PortalDtos.AuditEntryResponse> auditLogs(HttpServletRequest request,
                                                         @RequestParam(required = false) String employeeId,
                                                         @RequestParam(required = false) Integer year) {
        return goalPortalService.getAuditEntries(actorId(request), employeeId, year);
    }

    @GetMapping(value = "/reports/achievement", produces = "text/csv")
    public ResponseEntity<String> achievementReport(HttpServletRequest request, @RequestParam int year,
                                                    @RequestParam ReviewPeriod period) {
        String csv = goalPortalService.exportAchievementReport(actorId(request), year, period);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=achievement-report-" + year + "-" + period.name().toLowerCase() + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    private String actorId(HttpServletRequest request) {
        String headerActor = request.getHeader("X-User-Id");
        if (headerActor != null && !headerActor.isBlank()) {
            return headerActor;
        }
        String queryActor = request.getParameter("actorId");
        return queryActor == null ? null : queryActor.trim();
    }
}
