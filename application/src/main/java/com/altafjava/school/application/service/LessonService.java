package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.filter.CourseworkFilter;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.application.security.CourseworkReach;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.lms.model.Lesson;
import com.altafjava.school.domain.lms.repository.LessonRepository;
import com.altafjava.school.domain.subject.repository.SubjectRepository;

@Service
public class LessonService {

	private final LessonRepository lessonRepository;
	private final ClassroomRepository classroomRepository;
	private final SubjectRepository subjectRepository;
	private final AcademicAccessGuard academicAccessGuard;
	private final PublicIdLookup publicIdLookup;

	public LessonService(LessonRepository lessonRepository, ClassroomRepository classroomRepository,
			SubjectRepository subjectRepository, AcademicAccessGuard academicAccessGuard,
			PublicIdLookup publicIdLookup) {
		this.publicIdLookup = publicIdLookup;
		this.lessonRepository = lessonRepository;
		this.classroomRepository = classroomRepository;
		this.subjectRepository = subjectRepository;
		this.academicAccessGuard = academicAccessGuard;
	}

	@Transactional
	public Lesson post(String classroomPublicId, String subjectPublicId, String title, String description,
			String storageKey) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Classroom classroom = classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomPublicId));
		Long subjectId = subjectRepository.findByPublicIdAndTenantId(UUID.fromString(subjectPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Subject not found: " + subjectPublicId))
				.getId();
		Long teacherId = academicAccessGuard.requireTeacherOfSubject(tenantId, classroom.getId(), subjectId);
		Lesson lesson = Lesson.post(classroom.getId(), subjectId, teacherId, title, description, storageKey);
		return lessonRepository.save(lesson);
	}

	@Transactional(readOnly = true)
	public Page<Lesson> listLessons(CourseworkFilter filter, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Long classroomId = publicIdLookup.idOrNull(EntityRef.CLASSROOM, filter.classroomPublicId());
		CourseworkReach reach = academicAccessGuard.courseworkReach(tenantId, classroomId);
		LocalDate from = filter.dates().from();
		LocalDate to = filter.dates().to();
		return lessonRepository.search(tenantId, reach.everyClassroom(), reach.classroomIds(), classroomId,
				publicIdLookup.idOrNull(EntityRef.SUBJECT, filter.subjectPublicId()),
				from != null ? from.atStartOfDay() : null, to != null ? to.plusDays(1).atStartOfDay() : null,
				pageable);
	}
}
