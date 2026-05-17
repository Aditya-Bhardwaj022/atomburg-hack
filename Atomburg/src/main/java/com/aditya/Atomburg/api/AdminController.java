package com.aditya.Atomburg.api;

import com.aditya.Atomburg.service.GoalPortalService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final GoalPortalService goalPortalService;

    public AdminController(GoalPortalService goalPortalService) {
        this.goalPortalService = goalPortalService;
    }


    @PostMapping("/goal-sheets/{employeeId}/{year}/unlock")
    public PortalDtos.GoalSheetResponse unlockSheet(HttpServletRequest request, @PathVariable String employeeId,
                                                    @PathVariable int year,
                                                    @RequestBody(required = false) PortalDtos.UnlockSheetRequest payload) {
        return goalPortalService.unlockSheet(actorId(request), employeeId, year, payload);
    }

    @PutMapping("/cycles/{year}")
    public PortalDtos.GoalCycleResponse updateCycle(HttpServletRequest request, @PathVariable int year,
                                                    @RequestBody PortalDtos.UpdateCycleRequest payload) {
        return goalPortalService.updateCycle(actorId(request), year, payload);
    }

    @PostMapping("/users")
    public PortalDtos.UserResponse upsertUser(HttpServletRequest request,
                                              @RequestBody PortalDtos.UpsertUserRequest payload) {
        return goalPortalService.upsertUser(actorId(request), payload);
    }

    @PutMapping("/users/{userId}/manager")
    public PortalDtos.UserResponse updateManager(HttpServletRequest request, @PathVariable String userId,
                                                 @RequestBody PortalDtos.UpdateManagerRequest payload) {
        return goalPortalService.updateManager(actorId(request), userId, payload);
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
