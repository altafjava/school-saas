package com.altafjava.school.application.security;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.repository.TimetableEntryRepository;
import lombok.RequiredArgsConstructor;

/**
 * Resolves a platform user to the {@link TeachingAssignments} of the teacher record linked to it.
 * A user with no teacher record teaches nothing, so every check built on the result fails closed.
 */
@Component
@RequiredArgsConstructor
public class TeachingAssignmentResolver {

	private final TeacherRepository teacherRepository;
	private final ClassroomRepository classroomRepository;
	private final TimetableEntryRepository timetableEntryRepository;

	public TeachingAssignments forUser(Long userId, Long tenantId) {
		if (userId == null) {
			return TeachingAssignments.NONE;
		}
		return teacherRepository.findByUserIdAndTenantId(userId, tenantId)
				.map(teacher -> forTeacher(teacher.getId(), tenantId))
				.orElse(TeachingAssignments.NONE);
	}

	private TeachingAssignments forTeacher(Long teacherId, Long tenantId) {
		Set<Long> homeroomClassroomIds = classroomRepository.findAllByClassTeacherIdAndTenantId(teacherId, tenantId)
				.stream()
				.map(Classroom::getId)
				.collect(Collectors.toUnmodifiableSet());
		Map<Long, Set<Long>> subjectIdsByClassroomId = timetableEntryRepository
				.findAllByTenantIdAndTeacherId(tenantId, teacherId).stream()
				.collect(Collectors.groupingBy(TimetableEntry::getClassroomId,
						Collectors.mapping(TimetableEntry::getSubjectId, Collectors.toUnmodifiableSet())));
		return new TeachingAssignments(teacherId, homeroomClassroomIds, subjectIdsByClassroomId);
	}
}
