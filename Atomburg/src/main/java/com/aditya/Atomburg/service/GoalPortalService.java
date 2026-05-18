package com.aditya.Atomburg.service;

import com.aditya.Atomburg.api.ApiException;
import com.aditya.Atomburg.api.PortalDtos;
import com.aditya.Atomburg.domain.PortalDomain.GoalDirection;
import com.aditya.Atomburg.domain.PortalDomain.GoalSheetStatus;
import com.aditya.Atomburg.domain.PortalDomain.GoalStatus;
import com.aditya.Atomburg.domain.PortalDomain.ReviewPeriod;
import com.aditya.Atomburg.domain.PortalDomain.Role;
import com.aditya.Atomburg.domain.PortalDomain.SharedGoalOwnership;
import com.aditya.Atomburg.domain.PortalDomain.UomType;
import com.aditya.Atomburg.persistence.entity.AppUserEntity;
import com.aditya.Atomburg.persistence.entity.AuditEntryEntity;
import com.aditya.Atomburg.persistence.entity.CycleWindowEntity;
import com.aditya.Atomburg.persistence.entity.GoalCheckInEntity;
import com.aditya.Atomburg.persistence.entity.GoalCycleEntity;
import com.aditya.Atomburg.persistence.entity.GoalEntity;
import com.aditya.Atomburg.persistence.entity.GoalSheetEntity;
import com.aditya.Atomburg.persistence.entity.PeriodReviewEntity;
import com.aditya.Atomburg.persistence.entity.SharedGoalEntity;
import com.aditya.Atomburg.repository.AuditEntryRepository;
import com.aditya.Atomburg.repository.GoalCycleRepository;
import com.aditya.Atomburg.repository.GoalRepository;
import com.aditya.Atomburg.repository.GoalSheetRepository;
import com.aditya.Atomburg.repository.SharedGoalRepository;
import com.aditya.Atomburg.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class GoalPortalService {
    private static final int MAX_GOALS_PER_EMPLOYEE = 8;
    private static final int MIN_WEIGHTAGE = 10;
    private static final int REQUIRED_TOTAL_WEIGHTAGE = 100;
    private static final List<ReviewPeriod> ORDERED_PERIODS = List.of(
            ReviewPeriod.GOAL_SETTING,
            ReviewPeriod.Q1,
            ReviewPeriod.Q2,
            ReviewPeriod.Q3,
            ReviewPeriod.Q4
    );

    private final Clock clock;
    private final UserRepository userRepository;
    private final GoalCycleRepository goalCycleRepository;
    private final GoalSheetRepository goalSheetRepository;
    private final GoalRepository goalRepository;
    private final SharedGoalRepository sharedGoalRepository;
    private final AuditEntryRepository auditEntryRepository;
    private final GoalPortalMapper mapper;
    private final ProgressScoreCalculator progressScoreCalculator;

    public GoalPortalService(Clock clock,
                             UserRepository userRepository,
                             GoalCycleRepository goalCycleRepository,
                             GoalSheetRepository goalSheetRepository,
                             GoalRepository goalRepository,
                             SharedGoalRepository sharedGoalRepository,
                             AuditEntryRepository auditEntryRepository,
                             GoalPortalMapper mapper,
                             ProgressScoreCalculator progressScoreCalculator) {
        this.clock = clock;
        this.userRepository = userRepository;
        this.goalCycleRepository = goalCycleRepository;
        this.goalSheetRepository = goalSheetRepository;
        this.goalRepository = goalRepository;
        this.sharedGoalRepository = sharedGoalRepository;
        this.auditEntryRepository = auditEntryRepository;
        this.mapper = mapper;
        this.progressScoreCalculator = progressScoreCalculator;
    }

    @Transactional
    public void seedDemoData() {
        if (userRepository.count() > 0) {
            return;
        }

        AppUserEntity admin = saveUser("admin-1", "Ava HR", Role.ADMIN, null, "People Operations");
        AppUserEntity manager = saveUser("mgr-1", "Maya Manager", Role.MANAGER, admin, "Digital");
        AppUserEntity employeeOne = saveUser("emp-1", "Ethan Employee", Role.EMPLOYEE, manager, "Digital");
        AppUserEntity employeeTwo = saveUser("emp-2", "Priya Performer", Role.EMPLOYEE, manager, "Digital");
        AppUserEntity employeeThree = saveUser("emp-3", "Noah Newjoiner", Role.EMPLOYEE, manager, "Digital");

        int activeCycle = activeCycleYear(today());
        ensureCycle(activeCycle - 1);
        ensureCycle(activeCycle);
        ensureCycle(activeCycle + 1);

        GoalSheetEntity draftSheet = getOrCreateSheet(employeeOne, activeCycle);
        draftSheet.addGoal(newGoal(employeeOne.getId(),
                "Revenue Acceleration",
                "Grow inside-sales pipeline",
                "Build a stronger monthly qualified lead pipeline for the digital product line.",
                UomType.NUMERIC,
                GoalDirection.HIGHER_IS_BETTER,
                "120",
                null,
                40));
        draftSheet.addGoal(newGoal(employeeOne.getId(),
                "Customer Delivery",
                "Reduce onboarding turnaround time",
                "Shrink onboarding turnaround time for new enterprise accounts.",
                UomType.NUMERIC,
                GoalDirection.LOWER_IS_BETTER,
                "5",
                null,
                20));
        goalSheetRepository.save(draftSheet);

        GoalSheetEntity submittedSheet = getOrCreateSheet(employeeTwo, activeCycle);
        submittedSheet.addGoal(newGoal(employeeTwo.getId(),
                "Margin Expansion",
                "Improve service margin",
                "Increase service margin through better utilization planning.",
                UomType.PERCENTAGE,
                GoalDirection.HIGHER_IS_BETTER,
                "18",
                null,
                50));
        submittedSheet.addGoal(newGoal(employeeTwo.getId(),
                "Execution Excellence",
                "Maintain zero critical audit defects",
                "Track critical delivery defects across the quarter.",
                UomType.ZERO_BASED,
                GoalDirection.ZERO_BASED,
                "0",
                null,
                50));
        submittedSheet.setStatus(GoalSheetStatus.SUBMITTED);
        submittedSheet.setSubmittedAt(now());
        submittedSheet.setSubmittedBy(employeeTwo.getId());
        goalSheetRepository.save(submittedSheet);

        int priorCycle = activeCycle - 1;
        GoalSheetEntity priorSheetOne = getOrCreateSheet(employeeOne, priorCycle);
        GoalSheetEntity priorSheetTwo = getOrCreateSheet(employeeTwo, priorCycle);

        SharedGoalEntity sharedGoal = new SharedGoalEntity();
        sharedGoal.setReferenceKey(nextSharedGoalReference());
        sharedGoal.setYear(priorCycle);
        sharedGoal.setPrimaryOwner(employeeOne);
        sharedGoalRepository.save(sharedGoal);

        GoalEntity ownerSharedGoal = newGoal(employeeOne.getId(),
                "Department KPI",
                "Improve NPS",
                "Lift team NPS through proactive success reviews.",
                UomType.PERCENTAGE,
                GoalDirection.HIGHER_IS_BETTER,
                "85",
                null,
                50);
        ownerSharedGoal.setSharedGoal(sharedGoal);
        ownerSharedGoal.setSharedGoalOwnership(SharedGoalOwnership.PRIMARY_OWNER);
        priorSheetOne.addGoal(ownerSharedGoal);
        priorSheetOne.addGoal(newGoal(employeeOne.getId(),
                "Delivery Governance",
                "Complete quarterly playbooks",
                "Publish operational playbooks before quarter-end.",
                UomType.TIMELINE,
                GoalDirection.TIMELINE,
                null,
                LocalDate.of(priorCycle + 1, 3, 31),
                50));
        approveSeedSheet(priorSheetOne, manager.getId());
        goalSheetRepository.saveAndFlush(priorSheetOne);
        updateCheckInSeed(priorSheetOne, ReviewPeriod.Q1, ownerSharedGoal, "88", null, GoalStatus.COMPLETED, employeeOne.getId());
        updateCheckInSeed(priorSheetOne, ReviewPeriod.Q1, priorSheetOne.getGoals().get(1), null,
                LocalDate.of(priorCycle, 8, 20), GoalStatus.COMPLETED, employeeOne.getId());
        commentSeedReview(priorSheetOne, ReviewPeriod.Q1, manager.getId(),
                "Solid first quarter. Keep the operating rhythm steady.");
        updateCheckInSeed(priorSheetOne, ReviewPeriod.Q2, ownerSharedGoal, "86", null, GoalStatus.ON_TRACK, employeeOne.getId());
        commentSeedReview(priorSheetOne, ReviewPeriod.Q2, manager.getId(),
                "Momentum is good. Tighten follow-up on detractor accounts.");
        goalSheetRepository.saveAndFlush(priorSheetOne);

        GoalEntity recipientSharedGoal = newGoal(employeeTwo.getId(),
                "Department KPI",
                "Improve NPS",
                "Lift team NPS through proactive success reviews.",
                UomType.PERCENTAGE,
                GoalDirection.HIGHER_IS_BETTER,
                "85",
                null,
                40);
        recipientSharedGoal.setSharedGoal(sharedGoal);
        recipientSharedGoal.setSharedGoalOwnership(SharedGoalOwnership.RECIPIENT);
        priorSheetTwo.addGoal(recipientSharedGoal);
        GoalEntity priorGoalTwo = newGoal(employeeTwo.getId(),
                "Cost Control",
                "Reduce refund leakage",
                "Reduce avoidable refund leakage through root-cause actions.",
                UomType.NUMERIC,
                GoalDirection.LOWER_IS_BETTER,
                "12",
                null,
                60);
        priorSheetTwo.addGoal(priorGoalTwo);
        approveSeedSheet(priorSheetTwo, manager.getId());
        goalSheetRepository.saveAndFlush(priorSheetTwo);
        syncSharedCheckIn(sharedGoal.getReferenceKey(), ReviewPeriod.Q1, "88", null, GoalStatus.COMPLETED, employeeOne.getId(), now().minusDays(90));
        updateCheckInSeed(priorSheetTwo, ReviewPeriod.Q1, priorGoalTwo, "10", null, GoalStatus.COMPLETED, employeeTwo.getId());
        commentSeedReview(priorSheetTwo, ReviewPeriod.Q1, manager.getId(),
                "Nice job converting the shared KPI into local action.");
        logAudit(admin, priorSheetTwo, priorGoalTwo.getId(), "GOAL_EDIT_AFTER_LOCK", "weightage", "50", "60",
                now().minusDays(35));

        goalSheetRepository.saveAndFlush(priorSheetOne);
        goalSheetRepository.saveAndFlush(priorSheetTwo);
    }

    @Transactional
    public PortalDtos.SessionResponse getSession(Integer year) {
        int cycleYear = year == null ? activeCycleYear(today()) : year;
        GoalCycleEntity cycle = ensureCycle(cycleYear);
        return new PortalDtos.SessionResponse(
                "Use the X-User-Id header or actorId query parameter with one of the seeded IDs.",
                today(),
                userRepository.findAll().stream()
                        .sorted(Comparator.comparing(AppUserEntity::getRole).thenComparing(AppUserEntity::getName))
                        .map(mapper::toUserResponse)
                        .toList(),
                mapper.toGoalCycleResponse(cycle)
        );
    }

    @Transactional(readOnly = true)
    public List<PortalDtos.UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(AppUserEntity::getRole).thenComparing(AppUserEntity::getName))
                .map(mapper::toUserResponse)
                .toList();
    }

    @Transactional
    public PortalDtos.GoalCycleResponse getCycle(int year) {
        return mapper.toGoalCycleResponse(ensureCycle(year));
    }

    @Transactional
    public PortalDtos.GoalCycleResponse updateCycle(String actorId, int year, PortalDtos.UpdateCycleRequest request) {
        AppUserEntity actor = requireActor(actorId);
        requireAdmin(actor);
        if (request == null || request.windows() == null || request.windows().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cycle windows are required.");
        }
        Map<ReviewPeriod, CycleWindowEntity> windows = new LinkedHashMap<>();
        for (PortalDtos.CycleWindowRequest requestWindow : request.windows()) {
            if (requestWindow.period() == null || requestWindow.month() == null
                    || requestWindow.dayOfMonth() == null || requestWindow.yearOffset() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Each cycle window must include period, month, dayOfMonth, and yearOffset.");
            }
            CycleWindowEntity window = new CycleWindowEntity();
            window.setPeriod(requestWindow.period());
            window.setMonth(requestWindow.month());
            window.setDayOfMonth(requestWindow.dayOfMonth());
            window.setYearOffset(requestWindow.yearOffset());
            window.setAction(blankToDefault(requestWindow.action(), defaultAction(requestWindow.period())));
            windows.put(requestWindow.period(), window);
        }
        if (!windows.keySet().containsAll(EnumSet.allOf(ReviewPeriod.class))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cycle config must define all required periods.");
        }

        GoalCycleEntity cycle = goalCycleRepository.findById(year).orElse(null);
        if (cycle == null) {
            cycle = new GoalCycleEntity();
            cycle.setYear(year);
        } else {
            cycle.replaceWindows(List.of());
            goalCycleRepository.saveAndFlush(cycle);
        }
        cycle.setYear(year);
        cycle.replaceWindows(new ArrayList<>(windows.values()));
        goalCycleRepository.saveAndFlush(cycle);
        return mapper.toGoalCycleResponse(cycle);
    }

    @Transactional
    public PortalDtos.UserResponse upsertUser(String actorId, PortalDtos.UpsertUserRequest request) {
        AppUserEntity actor = requireActor(actorId);
        requireAdmin(actor);
        if (request == null || isBlank(request.userId()) || isBlank(request.name()) || request.role() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "userId, name, and role are required.");
        }
        if (Objects.equals(trimToNull(request.userId()), trimToNull(request.managerId()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A user cannot report to themselves.");
        }
        AppUserEntity user = userRepository.findById(request.userId().trim()).orElseGet(AppUserEntity::new);
        user.setId(request.userId().trim());
        user.setName(request.name().trim());
        user.setRole(request.role());
        user.setDepartment(trimToNull(request.department()));
        user.setManager(resolveManager(request.managerId()));
        userRepository.saveAndFlush(user);
        return mapper.toUserResponse(user);
    }

    @Transactional
    public PortalDtos.UserResponse updateManager(String actorId, String userId, PortalDtos.UpdateManagerRequest request) {
        AppUserEntity actor = requireActor(actorId);
        requireAdmin(actor);
        AppUserEntity user = requireUser(userId);
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Manager update payload is required.");
        }
        if (Objects.equals(user.getId(), trimToNull(request.managerId()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A user cannot report to themselves.");
        }
        user.setManager(resolveManager(request.managerId()));
        userRepository.saveAndFlush(user);
        return mapper.toUserResponse(user);
    }

    @Transactional
    public PortalDtos.GoalSheetResponse getMyGoalSheet(String actorId, int year) {
        AppUserEntity actor = requireActor(actorId);
        GoalSheetEntity sheet = getOrCreateSheet(actor, year);
        return mapper.toGoalSheetResponse(sheet);
    }

    @Transactional
    public PortalDtos.GoalSheetResponse addGoal(String actorId, int year, PortalDtos.CreateGoalRequest request) {
        AppUserEntity actor = requireActor(actorId);
        GoalSheetEntity sheet = getOrCreateSheet(actor, year);
        ensureEmployeeGoalEditAllowed(actor, sheet);
        if (sheet.getGoals().size() >= MAX_GOALS_PER_EMPLOYEE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "An employee can have at most 8 goals.");
        }
        GoalEntity goal = buildGoalFromRequest(actor.getId(), request);
        sheet.addGoal(goal);
        goalSheetRepository.saveAndFlush(sheet);
        if (sheet.hasEverBeenLocked()) {
            logAudit(actor, sheet, goal.getId(), "GOAL_ADDED_AFTER_LOCK", "goal", null, goal.getTitle(), now());
        }
        return mapper.toGoalSheetResponse(sheet);
    }

    @Transactional
    public PortalDtos.GoalSheetResponse updateGoal(String actorId, int year, long goalId, PortalDtos.UpdateGoalRequest request) {
        AppUserEntity actor = requireActor(actorId);
        GoalSheetEntity sheet = getOrCreateSheet(actor, year);
        ensureEmployeeGoalEditAllowed(actor, sheet);
        GoalEntity goal = requireGoal(sheet, goalId);
        applyEmployeeGoalUpdate(actor, sheet, goal, request);
        goalSheetRepository.saveAndFlush(sheet);
        return mapper.toGoalSheetResponse(sheet);
    }

    @Transactional
    public PortalDtos.GoalSheetResponse deleteGoal(String actorId, int year, long goalId) {
        AppUserEntity actor = requireActor(actorId);
        GoalSheetEntity sheet = getOrCreateSheet(actor, year);
        ensureEmployeeGoalEditAllowed(actor, sheet);
        GoalEntity goal = requireGoal(sheet, goalId);
        if (goal.isSharedGoal()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Shared goals cannot be deleted from individual sheets.");
        }
        if (sheet.hasEverBeenLocked()) {
            logAudit(actor, sheet, goal.getId(), "GOAL_REMOVED_AFTER_LOCK", "goal", goal.getTitle(), null, now());
        }
        sheet.removeGoal(goal);
        goalSheetRepository.saveAndFlush(sheet);
        return mapper.toGoalSheetResponse(sheet);
    }

    @Transactional
    public PortalDtos.GoalSheetResponse submitGoalSheet(String actorId, int year) {
        AppUserEntity actor = requireActor(actorId);
        GoalSheetEntity sheet = getOrCreateSheet(actor, year);
        ensureGoalSettingWindowOpen(actor, year, sheet.isEditableAfterUnlock());
        if (sheet.getGoals().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Add at least one goal before submission.");
        }
        validateSubmissionRules(sheet);
        sheet.setStatus(GoalSheetStatus.SUBMITTED);
        sheet.setSubmittedAt(now());
        sheet.setSubmittedBy(actor.getId());
        sheet.setEditableAfterUnlock(false);
        goalSheetRepository.saveAndFlush(sheet);
        return mapper.toGoalSheetResponse(sheet);
    }

    @Transactional
    public List<PortalDtos.TeamMemberSummaryResponse> getManagerTeam(String actorId, int year, ReviewPeriod period) {
        AppUserEntity actor = requireActor(actorId);
        if (actor.getRole() == Role.EMPLOYEE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only managers or admins can view team dashboards.");
        }
        return scopedEmployees(actor).stream()
                .map(employee -> {
                    GoalSheetEntity sheet = getOrCreateSheet(employee, year);
                    return new PortalDtos.TeamMemberSummaryResponse(
                            mapper.toUserResponse(employee),
                            sheet.getStatus(),
                            sheet.getGoals().size(),
                            sheet.totalWeightage(),
                            sheet.getSubmittedAt(),
                            sheet.getApprovedAt(),
                            period != null && mapper.isEmployeeReviewComplete(sheet, period),
                            period != null && mapper.isManagerReviewComplete(sheet, period)
                    );
                })
                .sorted(Comparator.comparing(summary -> summary.employee().name()))
                .toList();
    }

    @Transactional
    public PortalDtos.GoalSheetResponse getEmployeeSheet(String actorId, String employeeId, int year) {
        AppUserEntity actor = requireActor(actorId);
        AppUserEntity employee = requireUser(employeeId);
        ensureCanManageEmployee(actor, employee);
        return mapper.toGoalSheetResponse(getOrCreateSheet(employee, year));
    }

    @Transactional
    public PortalDtos.GoalSheetResponse updateGoalDuringApproval(String actorId, String employeeId, int year,
                                                                 long goalId, PortalDtos.ManagerGoalUpdateRequest request) {
        AppUserEntity actor = requireActor(actorId);
        AppUserEntity employee = requireUser(employeeId);
        ensureCanManageEmployee(actor, employee);
        GoalSheetEntity sheet = getOrCreateSheet(employee, year);
        if (sheet.getStatus() != GoalSheetStatus.SUBMITTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Manager inline edits are only allowed on submitted sheets.");
        }
        GoalEntity goal = requireGoal(sheet, goalId);
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goal update payload is required.");
        }
        if (request.weightage() != null) {
            if (request.weightage() < MIN_WEIGHTAGE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Minimum weightage per goal is 10%.");
            }
            maybeLogGoalFieldChange(actor, sheet, goal, "weightage",
                    Integer.toString(goal.getWeightage()), Integer.toString(request.weightage()));
            goal.setWeightage(request.weightage());
        }
        if (goal.isSharedGoal()) {
            if (!Objects.equals(trimToNull(request.targetValue()), trimToNull(goal.getTargetValue()))
                    || !Objects.equals(request.targetDate(), goal.getTargetDate())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Shared goal targets are read-only once pushed.");
            }
        } else if (goal.getUomType() == UomType.TIMELINE) {
            if (request.targetDate() != null) {
                maybeLogGoalFieldChange(actor, sheet, goal, "targetDate",
                        stringify(goal.getTargetDate()), stringify(request.targetDate()));
                goal.setTargetDate(request.targetDate());
            }
        } else if (!isBlank(request.targetValue())) {
            validateNumericTarget(goal.getUomType(), goal.getDirection(), request.targetValue());
            maybeLogGoalFieldChange(actor, sheet, goal, "targetValue", goal.getTargetValue(), request.targetValue().trim());
            goal.setTargetValue(request.targetValue().trim());
        }
        stampUpdate(actor.getId(), goal);
        goalSheetRepository.saveAndFlush(sheet);
        return mapper.toGoalSheetResponse(sheet);
    }

    @Transactional
    public PortalDtos.GoalSheetResponse approveSheet(String actorId, String employeeId, int year, PortalDtos.DecisionRequest request) {
        AppUserEntity actor = requireActor(actorId);
        AppUserEntity employee = requireUser(employeeId);
        ensureCanManageEmployee(actor, employee);
        GoalSheetEntity sheet = getOrCreateSheet(employee, year);
        if (sheet.getStatus() != GoalSheetStatus.SUBMITTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only submitted sheets can be approved.");
        }
        validateSubmissionRules(sheet);
        sheet.setStatus(GoalSheetStatus.APPROVED);
        sheet.setApprovedBy(actor.getId());
        sheet.setApprovedAt(now());
        if (sheet.getLockedAt() == null) {
            sheet.setLockedAt(now());
        }
        sheet.setEditableAfterUnlock(false);
        sheet.setLastManagerComment(request == null ? null : trimToNull(request.comment()));
        goalSheetRepository.saveAndFlush(sheet);
        return mapper.toGoalSheetResponse(sheet);
    }

    @Transactional
    public PortalDtos.GoalSheetResponse returnForRework(String actorId, String employeeId, int year, PortalDtos.DecisionRequest request) {
        AppUserEntity actor = requireActor(actorId);
        AppUserEntity employee = requireUser(employeeId);
        ensureCanManageEmployee(actor, employee);
        GoalSheetEntity sheet = getOrCreateSheet(employee, year);
        if (sheet.getStatus() != GoalSheetStatus.SUBMITTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only submitted sheets can be returned for rework.");
        }
        if (request == null || isBlank(request.comment())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A manager comment is required when returning a sheet.");
        }
        sheet.setStatus(GoalSheetStatus.RETURNED_FOR_REWORK);
        sheet.setLastManagerComment(request.comment().trim());
        sheet.setEditableAfterUnlock(true);
        goalSheetRepository.saveAndFlush(sheet);
        return mapper.toGoalSheetResponse(sheet);
    }

    @Transactional
    public PortalDtos.GoalSheetResponse unlockSheet(String actorId, String employeeId, int year, PortalDtos.UnlockSheetRequest request) {
        AppUserEntity actor = requireActor(actorId);
        requireAdmin(actor);
        GoalSheetEntity sheet = getOrCreateSheet(requireUser(employeeId), year);
        if (!sheet.hasEverBeenLocked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only approved sheets can be unlocked.");
        }
        sheet.setStatus(GoalSheetStatus.RETURNED_FOR_REWORK);
        sheet.setEditableAfterUnlock(true);
        sheet.setUnlockedBy(actor.getId());
        sheet.setUnlockedAt(now());
        sheet.setLastUnlockedReason(request == null ? "Admin unlock" : blankToDefault(request.reason(), "Admin unlock"));
        goalSheetRepository.saveAndFlush(sheet);
        logAudit(actor, sheet, null, "GOAL_SHEET_UNLOCKED", "sheetStatus", "APPROVED", "RETURNED_FOR_REWORK", now());
        return mapper.toGoalSheetResponse(sheet);
    }

    @Transactional
    public PortalDtos.GoalSheetResponse updateCheckIn(String actorId, int year, ReviewPeriod period, long goalId,
                                                      PortalDtos.CheckInRequest request) {
        AppUserEntity actor = requireActor(actorId);
        if (period == null || !period.isCheckInPeriod()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Check-ins are only available for Q1 to Q4.");
        }
        GoalSheetEntity sheet = getOrCreateSheet(actor, year);
        if (sheet.getStatus() != GoalSheetStatus.APPROVED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goals must be approved before achievements can be updated.");
        }
        ensureCheckInWindowOpen(actor, year, period);
        GoalEntity goal = requireGoal(sheet, goalId);
        if (goal.isSharedGoal() && goal.getSharedGoalOwnership() != SharedGoalOwnership.PRIMARY_OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only the primary owner can update achievements for shared goals.");
        }
        GoalCheckInEntity draftCheckIn = buildCheckIn(actor.getId(), goal, request);
        if (goal.isSharedGoal()) {
            syncSharedCheckIn(goal.getSharedGoal().getReferenceKey(), period,
                    draftCheckIn.getActualValue(), draftCheckIn.getActualDate(), draftCheckIn.getStatus(),
                    draftCheckIn.getUpdatedBy(), draftCheckIn.getUpdatedAt());
        } else {
            upsertCheckIn(sheet, period, goal, draftCheckIn.getActualValue(), draftCheckIn.getActualDate(),
                    draftCheckIn.getStatus(), draftCheckIn.getProgressScore(), draftCheckIn.getUpdatedBy(),
                    draftCheckIn.getUpdatedAt());
            goalSheetRepository.saveAndFlush(sheet);
        }
        return mapper.toGoalSheetResponse(sheet);
    }

    @Transactional
    public PortalDtos.GoalSheetResponse addManagerComment(String actorId, String employeeId, int year, ReviewPeriod period,
                                                          PortalDtos.ManagerCommentRequest request) {
        AppUserEntity actor = requireActor(actorId);
        AppUserEntity employee = requireUser(employeeId);
        ensureCanManageEmployee(actor, employee);
        if (period == null || !period.isCheckInPeriod()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Manager comments are only supported for Q1 to Q4.");
        }
        if (request == null || isBlank(request.comment())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Comment is required.");
        }
        GoalSheetEntity sheet = getOrCreateSheet(employee, year);
        if (!mapper.isEmployeeReviewComplete(sheet, period)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Employees must finish the quarter's check-in before manager sign-off.");
        }
        ensureCheckInWindowOpen(actor, year, period);
        PeriodReviewEntity review = sheet.getOrCreateReview(period);
        review.setManagerComment(request.comment().trim());
        review.setManagerId(actor.getId());
        review.setManagerReviewedAt(now());
        goalSheetRepository.saveAndFlush(sheet);
        return mapper.toGoalSheetResponse(sheet);
    }

    @Transactional
    public PortalDtos.GoalSheetResponse pushSharedGoal(String actorId, PortalDtos.PushSharedGoalRequest request) {
        AppUserEntity actor = requireActor(actorId);
        if (actor.getRole() == Role.EMPLOYEE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only managers or admins can push shared goals.");
        }
        if (request == null || request.year() == null || isBlank(request.primaryOwnerId())
                || request.recipientIds() == null || request.recipientIds().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "year, primaryOwnerId, and recipientIds are required.");
        }
        int year = request.year();
        ensureGoalSettingWindowOpen(actor, year, false);
        AppUserEntity primaryOwner = requireUser(request.primaryOwnerId());
        ensureCanManageEmployee(actor, primaryOwner);

        Set<String> participants = new LinkedHashSet<>();
        participants.add(primaryOwner.getId());
        participants.addAll(request.recipientIds());
        participants.forEach(userId -> ensureCanManageEmployee(actor, requireUser(userId)));

        SharedGoalEntity sharedGoal = new SharedGoalEntity();
        sharedGoal.setReferenceKey(nextSharedGoalReference());
        sharedGoal.setYear(year);
        sharedGoal.setPrimaryOwner(primaryOwner);
        sharedGoalRepository.saveAndFlush(sharedGoal);

        createSharedGoalAssignment(actor, primaryOwner, year, request, request.primaryOwnerWeightage(),
                SharedGoalOwnership.PRIMARY_OWNER, sharedGoal);
        for (String recipientId : request.recipientIds()) {
            Integer weightage = request.recipientWeightages() == null ? null : request.recipientWeightages().get(recipientId);
            createSharedGoalAssignment(actor, requireUser(recipientId), year, request, weightage,
                    SharedGoalOwnership.RECIPIENT, sharedGoal);
        }
        return mapper.toGoalSheetResponse(getOrCreateSheet(primaryOwner, year));
    }

    @Transactional
    public PortalDtos.CompletionDashboardResponse getCompletionDashboard(String actorId, int year, ReviewPeriod period) {
        AppUserEntity actor = requireActor(actorId);
        if (period == null || !period.isCheckInPeriod()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Dashboard requires a quarterly period (Q1 to Q4).");
        }
        List<AppUserEntity> employees = scopedEmployees(actor);
        List<PortalDtos.CompletionRowResponse> rows = employees.stream()
                .map(employee -> {
                    GoalSheetEntity sheet = getOrCreateSheet(employee, year);
                    return new PortalDtos.CompletionRowResponse(
                            mapper.toUserResponse(employee),
                            mapper.toUserResponse(employee.getManager()),
                            sheet.getGoals().size(),
                            mapper.isEmployeeReviewComplete(sheet, period),
                            mapper.isManagerReviewComplete(sheet, period)
                    );
                })
                .sorted(Comparator.comparing(row -> row.employee().name()))
                .toList();

        long employeesCompleted = rows.stream().filter(PortalDtos.CompletionRowResponse::employeeCompleted).count();
        long managersCompleted = rows.stream().filter(PortalDtos.CompletionRowResponse::managerCompleted).count();
        BigDecimal total = BigDecimal.valueOf(Math.max(rows.size(), 1));

        return new PortalDtos.CompletionDashboardResponse(
                year,
                period,
                rows.size(),
                employeesCompleted,
                managersCompleted,
                percentage(employeesCompleted, total),
                percentage(managersCompleted, total),
                rows
        );
    }

    @Transactional(readOnly = true)
    public List<PortalDtos.AuditEntryResponse> getAuditEntries(String actorId, String employeeId, Integer year) {
        AppUserEntity actor = requireActor(actorId);
        String requestedEmployeeId = employeeId;
        if (actor.getRole() == Role.EMPLOYEE) {
            if (requestedEmployeeId == null) {
                requestedEmployeeId = actor.getId();
            } else if (!Objects.equals(actor.getId(), requestedEmployeeId)) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Employees can only view their own audit trail.");
            }
        }
        if (requestedEmployeeId != null && actor.getRole() == Role.MANAGER) {
            ensureCanManageEmployee(actor, requireUser(requestedEmployeeId));
        }

        List<AuditEntryEntity> auditEntries;
        if (requestedEmployeeId != null && year != null) {
            auditEntries = auditEntryRepository.findByEmployeeIdAndYearOrderByTimestampDesc(requestedEmployeeId, year);
        } else if (requestedEmployeeId != null) {
            auditEntries = auditEntryRepository.findByEmployeeIdOrderByTimestampDesc(requestedEmployeeId);
        } else if (year != null) {
            auditEntries = auditEntryRepository.findByYearOrderByTimestampDesc(year);
        } else {
            auditEntries = auditEntryRepository.findAllByOrderByTimestampDesc();
        }
        if (actor.getRole() == Role.MANAGER && requestedEmployeeId == null) {
            Set<String> directReportIds = scopedEmployees(actor).stream().map(AppUserEntity::getId).collect(java.util.stream.Collectors.toSet());
            auditEntries = auditEntries.stream()
                    .filter(entry -> directReportIds.contains(entry.getEmployeeId()))
                    .toList();
        }
        return auditEntries.stream().map(mapper::toAuditEntryResponse).toList();
    }

    @Transactional
    public String exportAchievementReport(String actorId, int year, ReviewPeriod period) {
        AppUserEntity actor = requireActor(actorId);
        if (period == null || !period.isCheckInPeriod()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The achievement report requires a quarterly period.");
        }
        List<AppUserEntity> employees = scopedEmployees(actor);
        StringBuilder csv = new StringBuilder();
        csv.append("Employee Id,Employee Name,Manager Name,Department,Cycle Year,Period,Goal Title,Thrust Area,UoM,Direction,Weightage,Target,Actual,Status,Progress Score,Manager Comment,Last Updated At\n");
        for (AppUserEntity employee : employees) {
            GoalSheetEntity sheet = getOrCreateSheet(employee, year);
            PeriodReviewEntity review = sheet.findReview(period).orElse(null);
            for (GoalEntity goal : sheet.getGoals()) {
                GoalCheckInEntity checkIn = review == null ? null : review.findCheckIn(goal.getId()).orElse(null);
                csv.append(csv(employee.getId())).append(',')
                        .append(csv(employee.getName())).append(',')
                        .append(csv(employee.getManager() == null ? "" : employee.getManager().getName())).append(',')
                        .append(csv(nullToEmpty(employee.getDepartment()))).append(',')
                        .append(year).append(',')
                        .append(period).append(',')
                        .append(csv(goal.getTitle())).append(',')
                        .append(csv(goal.getThrustArea())).append(',')
                        .append(goal.getUomType()).append(',')
                        .append(goal.getDirection()).append(',')
                        .append(goal.getWeightage()).append(',')
                        .append(csv(targetDisplay(goal))).append(',')
                        .append(csv(checkIn == null ? "" : actualDisplay(checkIn))).append(',')
                        .append(csv(checkIn == null || checkIn.getStatus() == null ? "" : checkIn.getStatus().name())).append(',')
                        .append(csv(checkIn == null || checkIn.getProgressScore() == null ? "" : checkIn.getProgressScore().toPlainString())).append(',')
                        .append(csv(review == null ? "" : nullToEmpty(review.getManagerComment()))).append(',')
                        .append(csv(checkIn == null || checkIn.getUpdatedAt() == null ? "" : checkIn.getUpdatedAt().toString()))
                        .append('\n');
            }
        }
        return csv.toString();
    }

    private void createSharedGoalAssignment(AppUserEntity actor, AppUserEntity employee, int year,
                                            PortalDtos.PushSharedGoalRequest request, Integer weightage,
                                            SharedGoalOwnership ownership, SharedGoalEntity sharedGoal) {
        GoalSheetEntity sheet = getOrCreateSheet(employee, year);
        if (sheet.getGoals().size() >= MAX_GOALS_PER_EMPLOYEE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot push shared goal. " + employee.getId() + " already has 8 goals.");
        }
        if (sheet.getStatus() == GoalSheetStatus.APPROVED && !sheet.isEditableAfterUnlock()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot push shared goals into approved sheets without admin unlock.");
        }
        GoalEntity goal = buildGoalFromRequest(actor.getId(), new PortalDtos.CreateGoalRequest(
                request.thrustArea(),
                request.title(),
                request.description(),
                request.uomType(),
                request.direction(),
                request.targetValue(),
                request.targetDate(),
                weightage
        ));
        goal.setSharedGoal(sharedGoal);
        goal.setSharedGoalOwnership(ownership);
        sheet.addGoal(goal);
        goalSheetRepository.saveAndFlush(sheet);
        if (sheet.hasEverBeenLocked()) {
            logAudit(actor, sheet, goal.getId(), "GOAL_ADDED_AFTER_LOCK", "goal", null, goal.getTitle(), now());
        }
    }

    private void syncSharedCheckIn(String sharedGoalReference, ReviewPeriod period, String actualValue,
                                   LocalDate actualDate, GoalStatus status, String updatedBy, LocalDateTime updatedAt) {
        List<GoalEntity> linkedGoals = goalRepository.findBySharedGoal_ReferenceKey(sharedGoalReference);
        Set<GoalSheetEntity> touchedSheets = new LinkedHashSet<>();
        for (GoalEntity linkedGoal : linkedGoals) {
            GoalSheetEntity participantSheet = linkedGoal.getGoalSheet();
            PeriodReviewEntity review = participantSheet.getOrCreateReview(period);
            GoalCheckInEntity checkIn = review.findCheckIn(linkedGoal.getId()).orElseGet(() -> {
                GoalCheckInEntity newCheckIn = new GoalCheckInEntity();
                newCheckIn.setGoal(linkedGoal);
                return review.addCheckIn(newCheckIn);
            });
            checkIn.setGoal(linkedGoal);
            checkIn.setActualValue(actualValue);
            checkIn.setActualDate(actualDate);
            checkIn.setStatus(status);
            checkIn.setProgressScore(progressScoreCalculator.computeProgress(linkedGoal, actualValue, actualDate, status));
            checkIn.setUpdatedBy(updatedBy);
            checkIn.setUpdatedAt(updatedAt);
            touchedSheets.add(participantSheet);
        }
        goalSheetRepository.saveAll(touchedSheets);
        goalSheetRepository.flush();
    }

    private void updateCheckInSeed(GoalSheetEntity sheet, ReviewPeriod period, GoalEntity goal, String actualValue,
                                   LocalDate actualDate, GoalStatus status, String actorId) {
        upsertCheckIn(sheet, period, goal, actualValue, actualDate, status,
                progressScoreCalculator.computeProgress(goal, actualValue, actualDate, status),
                actorId, now().minusDays(90));
    }

    private void commentSeedReview(GoalSheetEntity sheet, ReviewPeriod period, String managerId, String comment) {
        PeriodReviewEntity review = sheet.getOrCreateReview(period);
        review.setManagerComment(comment);
        review.setManagerId(managerId);
        review.setManagerReviewedAt(now().minusDays(80));
    }

    private void approveSeedSheet(GoalSheetEntity sheet, String managerId) {
        sheet.setStatus(GoalSheetStatus.APPROVED);
        sheet.setSubmittedBy(sheet.getEmployee().getId());
        sheet.setSubmittedAt(now().minusMonths(10));
        sheet.setApprovedBy(managerId);
        sheet.setApprovedAt(now().minusMonths(10).plusDays(2));
        sheet.setLockedAt(now().minusMonths(10).plusDays(2));
        sheet.setEditableAfterUnlock(false);
    }

    private void upsertCheckIn(GoalSheetEntity sheet, ReviewPeriod period, GoalEntity goal, String actualValue,
                               LocalDate actualDate, GoalStatus status, BigDecimal progressScore,
                               String updatedBy, LocalDateTime updatedAt) {
        PeriodReviewEntity review = sheet.getOrCreateReview(period);
        GoalCheckInEntity checkIn = review.findCheckIn(goal.getId()).orElseGet(() -> {
            GoalCheckInEntity newCheckIn = new GoalCheckInEntity();
            newCheckIn.setGoal(goal);
            return review.addCheckIn(newCheckIn);
        });
        checkIn.setGoal(goal);
        checkIn.setActualValue(actualValue);
        checkIn.setActualDate(actualDate);
        checkIn.setStatus(status);
        checkIn.setProgressScore(progressScore);
        checkIn.setUpdatedBy(updatedBy);
        checkIn.setUpdatedAt(updatedAt);
    }

    private GoalCheckInEntity buildCheckIn(String actorId, GoalEntity goal, PortalDtos.CheckInRequest request) {
        if (request == null || request.status() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "actuals and status payload is required.");
        }
        GoalCheckInEntity checkIn = new GoalCheckInEntity();
        checkIn.setGoal(goal);
        checkIn.setStatus(request.status());
        checkIn.setUpdatedBy(actorId);
        checkIn.setUpdatedAt(now());
        switch (goal.getUomType()) {
            case TIMELINE -> {
                if (request.actualDate() == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Timeline check-ins require actualDate.");
                }
                checkIn.setActualDate(request.actualDate());
            }
            case ZERO_BASED -> {
                String actualValue = required(request.actualValue(), "Zero-based goals require actualValue.");
                checkIn.setActualValue(actualValue);
            }
            default -> {
                String actualValue = required(request.actualValue(), "Numeric goals require actualValue.");
                validateDecimal(actualValue, "Actual achievement must be numeric.");
                checkIn.setActualValue(actualValue.trim());
            }
        }
        checkIn.setProgressScore(progressScoreCalculator.computeProgress(
                goal,
                checkIn.getActualValue(),
                checkIn.getActualDate(),
                checkIn.getStatus()
        ));
        return checkIn;
    }

    private GoalEntity buildGoalFromRequest(String actorId, PortalDtos.CreateGoalRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goal payload is required.");
        }
        GoalEntity goal = new GoalEntity();
        goal.setThrustArea(required(request.thrustArea(), "Thrust area is required."));
        goal.setTitle(required(request.title(), "Goal title is required."));
        goal.setDescription(blankToDefault(request.description(), ""));
        goal.setUomType(requireValue(request.uomType(), "UoM type is required."));
        goal.setDirection(normalizeDirection(goal.getUomType(), request.direction()));
        goal.setWeightage(requirePositiveWeightage(request.weightage()));
        setTarget(goal, request.targetValue(), request.targetDate());
        goal.setCreatedBy(actorId);
        goal.setCreatedAt(now());
        goal.setUpdatedBy(actorId);
        goal.setUpdatedAt(now());
        return goal;
    }

    private GoalEntity newGoal(String actorId, String thrustArea, String title, String description, UomType uomType,
                               GoalDirection direction, String targetValue, LocalDate targetDate, int weightage) {
        GoalEntity goal = new GoalEntity();
        goal.setThrustArea(thrustArea);
        goal.setTitle(title);
        goal.setDescription(description);
        goal.setUomType(uomType);
        goal.setDirection(direction);
        goal.setWeightage(weightage);
        goal.setTargetValue(targetValue);
        goal.setTargetDate(targetDate);
        goal.setCreatedBy(actorId);
        goal.setCreatedAt(now());
        goal.setUpdatedBy(actorId);
        goal.setUpdatedAt(now());
        return goal;
    }

    private void applyEmployeeGoalUpdate(AppUserEntity actor, GoalSheetEntity sheet, GoalEntity goal,
                                         PortalDtos.UpdateGoalRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goal payload is required.");
        }
        if (goal.isSharedGoal()) {
            if (request.weightage() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Shared goals can only update weightage.");
            }
            int weightage = requirePositiveWeightage(request.weightage());
            maybeLogGoalFieldChange(actor, sheet, goal, "weightage",
                    Integer.toString(goal.getWeightage()), Integer.toString(weightage));
            goal.setWeightage(weightage);
            stampUpdate(actor.getId(), goal);
            return;
        }

        String newThrustArea = required(request.thrustArea(), "Thrust area is required.");
        maybeLogGoalFieldChange(actor, sheet, goal, "thrustArea", goal.getThrustArea(), newThrustArea);
        goal.setThrustArea(newThrustArea);

        String newTitle = required(request.title(), "Goal title is required.");
        maybeLogGoalFieldChange(actor, sheet, goal, "title", goal.getTitle(), newTitle);
        goal.setTitle(newTitle);

        String newDescription = blankToDefault(request.description(), "");
        maybeLogGoalFieldChange(actor, sheet, goal, "description", goal.getDescription(), newDescription);
        goal.setDescription(newDescription);

        UomType uomType = requireValue(request.uomType(), "UoM type is required.");
        GoalDirection direction = normalizeDirection(uomType, request.direction());
        maybeLogGoalFieldChange(actor, sheet, goal, "uomType", stringify(goal.getUomType()), stringify(uomType));
        maybeLogGoalFieldChange(actor, sheet, goal, "direction", stringify(goal.getDirection()), stringify(direction));
        goal.setUomType(uomType);
        goal.setDirection(direction);

        int weightage = requirePositiveWeightage(request.weightage());
        maybeLogGoalFieldChange(actor, sheet, goal, "weightage", Integer.toString(goal.getWeightage()), Integer.toString(weightage));
        goal.setWeightage(weightage);

        String oldTargetValue = goal.getTargetValue();
        LocalDate oldTargetDate = goal.getTargetDate();
        setTarget(goal, request.targetValue(), request.targetDate());
        maybeLogGoalFieldChange(actor, sheet, goal, "targetValue", oldTargetValue, goal.getTargetValue());
        maybeLogGoalFieldChange(actor, sheet, goal, "targetDate", stringify(oldTargetDate), stringify(goal.getTargetDate()));
        stampUpdate(actor.getId(), goal);
    }

    private void validateSubmissionRules(GoalSheetEntity sheet) {
        if (sheet.getGoals().size() > MAX_GOALS_PER_EMPLOYEE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "An employee can have at most 8 goals.");
        }
        if (sheet.totalWeightage() != REQUIRED_TOTAL_WEIGHTAGE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Total weightage across all goals must equal 100%.");
        }
        if (sheet.getGoals().stream().anyMatch(goal -> goal.getWeightage() < MIN_WEIGHTAGE)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Minimum weightage per individual goal is 10%.");
        }
    }

    private void setTarget(GoalEntity goal, String targetValue, LocalDate targetDate) {
        if (goal.getUomType() == UomType.TIMELINE) {
            if (targetDate == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Timeline goals require a targetDate.");
            }
            goal.setTargetDate(targetDate);
            goal.setTargetValue(null);
            return;
        }
        if (goal.getUomType() == UomType.ZERO_BASED) {
            goal.setTargetValue("0");
            goal.setTargetDate(null);
            return;
        }
        validateNumericTarget(goal.getUomType(), goal.getDirection(), targetValue);
        goal.setTargetValue(targetValue.trim());
        goal.setTargetDate(null);
    }

    private void validateNumericTarget(UomType uomType, GoalDirection direction, String targetValue) {
        if (uomType == UomType.TIMELINE || uomType == UomType.ZERO_BASED) {
            return;
        }
        if (direction != GoalDirection.HIGHER_IS_BETTER && direction != GoalDirection.LOWER_IS_BETTER) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Numeric and percentage goals must define whether higher or lower is better.");
        }
        validateDecimal(required(targetValue, "Target value is required."), "Target value must be numeric.");
    }

    private GoalDirection normalizeDirection(UomType uomType, GoalDirection direction) {
        return switch (uomType) {
            case TIMELINE -> GoalDirection.TIMELINE;
            case ZERO_BASED -> GoalDirection.ZERO_BASED;
            case NUMERIC, PERCENTAGE -> requireValue(direction, "Goal direction is required for numeric goals.");
        };
    }

    private int requirePositiveWeightage(Integer weightage) {
        if (weightage == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Weightage is required.");
        }
        if (weightage < MIN_WEIGHTAGE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Minimum weightage per goal is 10%.");
        }
        return weightage;
    }

    private void ensureEmployeeGoalEditAllowed(AppUserEntity actor, GoalSheetEntity sheet) {
        boolean editableState = sheet.getStatus() == GoalSheetStatus.DRAFT
                || sheet.getStatus() == GoalSheetStatus.RETURNED_FOR_REWORK;
        if (!editableState) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This goal sheet is locked for employee edits.");
        }
        ensureGoalSettingWindowOpen(actor, sheet.getYear(), sheet.isEditableAfterUnlock());
    }

    private void ensureGoalSettingWindowOpen(AppUserEntity actor, int year, boolean allowOverride) {
        if (actor.getRole() == Role.ADMIN || allowOverride) {
            return;
        }
        GoalCycleEntity cycle = ensureCycle(year);
        if (!isWindowOpen(cycle, ReviewPeriod.GOAL_SETTING, today())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Goal setting is currently outside the active window.");
        }
    }

    private void ensureCheckInWindowOpen(AppUserEntity actor, int year, ReviewPeriod period) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        GoalCycleEntity cycle = ensureCycle(year);
        if (!isWindowOpen(cycle, period, today())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, period + " check-in is currently outside the active window.");
        }
    }

    private boolean isWindowOpen(GoalCycleEntity cycle, ReviewPeriod period, LocalDate currentDate) {
        LocalDate start = windowStart(cycle, period);
        LocalDate endExclusive = windowEndExclusive(cycle, period);
        return !currentDate.isBefore(start) && currentDate.isBefore(endExclusive);
    }

    private LocalDate windowStart(GoalCycleEntity cycle, ReviewPeriod period) {
        CycleWindowEntity window = requireWindow(cycle, period);
        return LocalDate.of(cycle.getYear() + window.getYearOffset(), window.getMonth(), window.getDayOfMonth());
    }

    private LocalDate windowEndExclusive(GoalCycleEntity cycle, ReviewPeriod period) {
        int index = ORDERED_PERIODS.indexOf(period);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown period " + period);
        }
        if (index == ORDERED_PERIODS.size() - 1) {
            CycleWindowEntity nextCycleGoalSetting = requireWindow(cycle, ReviewPeriod.GOAL_SETTING);
            return LocalDate.of(cycle.getYear() + 1 + nextCycleGoalSetting.getYearOffset(),
                    nextCycleGoalSetting.getMonth(), nextCycleGoalSetting.getDayOfMonth());
        }
        return windowStart(cycle, ORDERED_PERIODS.get(index + 1));
    }

    private CycleWindowEntity requireWindow(GoalCycleEntity cycle, ReviewPeriod period) {
        return cycle.getWindows().stream()
                .filter(window -> window.getPeriod() == period)
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Cycle window not configured for " + period));
    }

    private GoalCycleEntity ensureCycle(int year) {
        GoalCycleEntity cycle = goalCycleRepository.findById(year).orElse(null);
        if (cycle != null) {
            return cycle;
        }
        return goalCycleRepository.saveAndFlush(defaultCycle(year));
    }

    private GoalCycleEntity defaultCycle(int year) {
        GoalCycleEntity cycle = new GoalCycleEntity();
        cycle.setYear(year);
        cycle.replaceWindows(List.of(
                newWindow(ReviewPeriod.GOAL_SETTING, 5, 1, 0, "Goal Creation, Submission & Approval"),
                newWindow(ReviewPeriod.Q1, 7, 1, 0, "Progress Update - Planned vs. Actual"),
                newWindow(ReviewPeriod.Q2, 10, 1, 0, "Progress Update - Planned vs. Actual"),
                newWindow(ReviewPeriod.Q3, 1, 1, 1, "Progress Update - Planned vs. Actual"),
                newWindow(ReviewPeriod.Q4, 3, 1, 1, "Final Achievement Capture")
        ));
        return cycle;
    }

    private CycleWindowEntity newWindow(ReviewPeriod period, int month, int dayOfMonth, int yearOffset, String action) {
        CycleWindowEntity window = new CycleWindowEntity();
        window.setPeriod(period);
        window.setMonth(month);
        window.setDayOfMonth(dayOfMonth);
        window.setYearOffset(yearOffset);
        window.setAction(action);
        return window;
    }

    private GoalSheetEntity getOrCreateSheet(AppUserEntity employee, int year) {
        ensureCycle(year);
        return goalSheetRepository.findByEmployee_IdAndYear(employee.getId(), year).orElseGet(() -> {
            GoalSheetEntity sheet = new GoalSheetEntity();
            sheet.setEmployee(employee);
            sheet.setYear(year);
            sheet.setStatus(GoalSheetStatus.DRAFT);
            return goalSheetRepository.saveAndFlush(sheet);
        });
    }

    private GoalEntity requireGoal(GoalSheetEntity sheet, long goalId) {
        return sheet.getGoals().stream()
                .filter(goal -> goal.getId() == goalId)
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Goal not found."));
    }

    private void ensureCanManageEmployee(AppUserEntity actor, AppUserEntity employee) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        if (actor.getRole() != Role.MANAGER || employee.getManager() == null
                || !Objects.equals(actor.getId(), employee.getManager().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only manage your direct reports.");
        }
    }

    private void requireAdmin(AppUserEntity actor) {
        if (actor.getRole() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin access is required.");
        }
    }

    private AppUserEntity requireActor(String actorId) {
        if (isBlank(actorId)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Pass X-User-Id header or actorId query parameter.");
        }
        return requireUser(actorId.trim());
    }

    private AppUserEntity requireUser(String userId) {
        return userRepository.findById(trimToNull(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + userId));
    }

    private AppUserEntity resolveManager(String managerId) {
        String trimmedManagerId = trimToNull(managerId);
        return trimmedManagerId == null ? null : requireUser(trimmedManagerId);
    }

    private List<AppUserEntity> scopedEmployees(AppUserEntity actor) {
        return switch (actor.getRole()) {
            case ADMIN -> userRepository.findByRoleOrderByName(Role.EMPLOYEE);
            case MANAGER -> userRepository.findByManager_IdAndRoleOrderByName(actor.getId(), Role.EMPLOYEE);
            case EMPLOYEE -> List.of(actor);
        };
    }

    private AppUserEntity saveUser(String id, String name, Role role, AppUserEntity manager, String department) {
        AppUserEntity user = new AppUserEntity();
        user.setId(id);
        user.setName(name);
        user.setRole(role);
        user.setManager(manager);
        user.setDepartment(department);
        return userRepository.save(user);
    }

    private void stampUpdate(String actorId, GoalEntity goal) {
        goal.setUpdatedBy(actorId);
        goal.setUpdatedAt(now());
    }

    private void maybeLogGoalFieldChange(AppUserEntity actor, GoalSheetEntity sheet, GoalEntity goal,
                                         String fieldName, String oldValue, String newValue) {
        if (!sheet.hasEverBeenLocked()) {
            return;
        }
        if (Objects.equals(blankToDefault(oldValue, ""), blankToDefault(newValue, ""))) {
            return;
        }
        logAudit(actor, sheet, goal.getId(), "GOAL_EDIT_AFTER_LOCK", fieldName, oldValue, newValue, now());
    }

    private void logAudit(AppUserEntity actor, GoalSheetEntity sheet, Long goalId, String action, String fieldName,
                          String oldValue, String newValue, LocalDateTime timestamp) {
        AuditEntryEntity entry = new AuditEntryEntity();
        entry.setActorId(actor.getId());
        entry.setActorName(actor.getName());
        entry.setEmployeeId(sheet.getEmployee().getId());
        entry.setYear(sheet.getYear());
        entry.setGoalId(goalId);
        entry.setAction(action);
        entry.setFieldName(fieldName);
        entry.setOldValue(blankToDefault(oldValue, ""));
        entry.setNewValue(blankToDefault(newValue, ""));
        entry.setTimestamp(timestamp);
        auditEntryRepository.save(entry);
    }

    private String nextSharedGoalReference() {
        return "shared-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private BigDecimal percentage(long completed, BigDecimal total) {
        return BigDecimal.valueOf(completed)
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
    }

    private String targetDisplay(GoalEntity goal) {
        return goal.getUomType() == UomType.TIMELINE ? stringify(goal.getTargetDate()) : nullToEmpty(goal.getTargetValue());
    }

    private String actualDisplay(GoalCheckInEntity checkIn) {
        return checkIn.getActualValue() != null ? checkIn.getActualValue() : stringify(checkIn.getActualDate());
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private int activeCycleYear(LocalDate date) {
        return date.getMonthValue() < 5 ? date.getYear() - 1 : date.getYear();
    }

    private <T> T requireValue(T value, String message) {
        if (value == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        return value;
    }

    private String required(String value, String message) {
        if (isBlank(value)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private void validateDecimal(String value, String message) {
        if (value == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
        String clean = value.trim();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("[0-9]");
        if (!pattern.matcher(clean).find()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String blankToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String defaultAction(ReviewPeriod period) {
        return switch (period) {
            case GOAL_SETTING -> "Goal Creation, Submission & Approval";
            case Q1, Q2, Q3 -> "Progress Update - Planned vs. Actual";
            case Q4 -> "Final Achievement Capture";
        };
    }

    private String stringify(Object value) {
        return value == null ? null : value.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
