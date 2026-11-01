package com.altafjava.school.application.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/** The students placed in a classroom, keyed by the public id a client sends. */
@Component
@RequiredArgsConstructor
class ClassRoster {

	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final StudentRepository studentRepository;

	Map<String, Student> byPublicId(Long tenantId, Long classroomId) {
		List<Long> studentIds = studentClassroomLinkRepository.findAllByClassroomId(tenantId, classroomId).stream()
				.map(StudentClassroomLink::getStudentId)
				.toList();
		return studentRepository.findAllByIdInAndTenantId(studentIds, tenantId).stream()
				.collect(Collectors.toMap(student -> student.getPublicId().toString(), Function.identity()));
	}
}
