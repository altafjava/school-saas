package com.altafjava.school.application.home;

import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.security.OwnStudentResolver;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/** The parent and student home screens: one response each, drawn only from the caller's own records. */
@Service
@RequiredArgsConstructor
public class FamilyHomeService {

	private final HomeCallerResolver homeCallerResolver;
	private final HomeDayResolver homeDayResolver;
	private final GuardianRepository guardianRepository;
	private final OwnStudentResolver ownStudentResolver;
	private final StudentRepository studentRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final StudentOverviewLoader studentOverviewLoader;
	private final TodayTimetableBuilder todayTimetableBuilder;
	private final NotificationService notificationService;

	@Transactional(readOnly = true)
	public ParentHome parent() {
		Long tenantId = TenantContext.getCurrentTenantId();
		HomeCaller caller = homeCallerResolver.current();
		Guardian guardian = guardianRepository.findByUserIdAndTenantId(caller.userId(), tenantId)
				.orElseThrow(() -> new AccessDeniedException("No guardian record linked to the current user"));
		List<Long> childIds = List.copyOf(ownStudentResolver.childrenOf(guardian.getId(), tenantId));
		List<Student> children = childIds.isEmpty() ? List.of()
				: studentRepository.findAllByIdInAndTenantId(childIds, tenantId);
		HomeDay day = homeDayResolver.today(tenantId);
		return new ParentHome(day.date(), day.schoolDay(),
				studentOverviewLoader.load(tenantId, children, caller, day), inboxOf(caller));
	}

	@Transactional(readOnly = true)
	public StudentHome student() {
		Long tenantId = TenantContext.getCurrentTenantId();
		HomeCaller caller = homeCallerResolver.current();
		Student student = studentRepository.findByUserIdAndTenantId(caller.userId(), tenantId)
				.orElseThrow(() -> new AccessDeniedException("No student record linked to the current user"));
		HomeDay day = homeDayResolver.today(tenantId);
		return new StudentHome(day.date(), day.schoolDay(), timetableOf(tenantId, student, caller, day),
				studentOverviewLoader.load(tenantId, List.of(student), caller, day).getFirst(), inboxOf(caller));
	}

	private List<TimetableSlot> timetableOf(Long tenantId, Student student, HomeCaller caller, HomeDay day) {
		if (!caller.holds(SchoolPermissions.TIMETABLE_READ)) {
			return null;
		}
		if (!day.hasAcademicYear()) {
			return List.of();
		}
		return studentClassroomLinkRepository
				.findByStudentIdAndAcademicYearId(tenantId, student.getId(), day.academicYearId())
				.map(StudentClassroomLink::getClassroomId)
				.map(classroomId -> todayTimetableBuilder.forClassroom(tenantId, classroomId, day))
				.orElse(List.of());
	}

	private Inbox inboxOf(HomeCaller caller) {
		return new Inbox(notificationService.getUnreadCount(caller.userId().toString()));
	}
}
