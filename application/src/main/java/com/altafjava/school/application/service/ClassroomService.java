package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.classroom.event.StudentEnrolledInClassroomEvent;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.curriculum.repository.CurriculumRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@Service
public class ClassroomService {

	private final ClassroomRepository classroomRepository;
	private final TeacherRepository teacherRepository;
	private final AcademicYearRepository academicYearRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final StudentRepository studentRepository;
	private final CurriculumRepository curriculumRepository;
	private final EventPublisher eventPublisher;

	public ClassroomService(ClassroomRepository classroomRepository, TeacherRepository teacherRepository,
			AcademicYearRepository academicYearRepository,
			StudentClassroomLinkRepository studentClassroomLinkRepository, StudentRepository studentRepository,
			CurriculumRepository curriculumRepository, EventPublisher eventPublisher) {
		this.classroomRepository = classroomRepository;
		this.teacherRepository = teacherRepository;
		this.academicYearRepository = academicYearRepository;
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.studentRepository = studentRepository;
		this.curriculumRepository = curriculumRepository;
		this.eventPublisher = eventPublisher;
	}

	@Transactional(readOnly = true)
	public Page<Classroom> listClassrooms(Pageable pageable) {
		return classroomRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Classroom findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return classroomRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + publicId));
	}

	@Transactional
	public Classroom create(String classCode, String grade, String section,
			String academicYearPublicId, Long classTeacherId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		AcademicYear academicYear = academicYearRepository
				.findByPublicIdAndTenantId(UUID.fromString(academicYearPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + academicYearPublicId));
		if (classTeacherId != null && !teacherRepository.existsByIdAndTenantId(classTeacherId, tenantId)) {
			throw new ResourceNotFoundException("Teacher not found: " + classTeacherId);
		}
		Classroom classroom = Classroom.create(classCode, grade, section, academicYear.getId(),
				academicYear.getName(), classTeacherId);
		return classroomRepository.save(classroom);
	}

	@Transactional
	public Classroom reassignTeacher(String publicId, Long classTeacherId) {
		Classroom classroom = findByPublicId(publicId);
		Long tenantId = TenantContext.getCurrentTenantId();
		if (classTeacherId != null && !teacherRepository.existsByIdAndTenantId(classTeacherId, tenantId)) {
			throw new ResourceNotFoundException("Teacher not found: " + classTeacherId);
		}
		classroom.reassignTeacher(classTeacherId);
		return classroomRepository.save(classroom);
	}

	@Transactional
	public Classroom moveToAcademicYear(String publicId, String academicYearPublicId) {
		Classroom classroom = findByPublicId(publicId);
		Long tenantId = TenantContext.getCurrentTenantId();
		AcademicYear academicYear = academicYearRepository
				.findByPublicIdAndTenantId(UUID.fromString(academicYearPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + academicYearPublicId));
		classroom.reassignAcademicYear(academicYear.getId(), academicYear.getName());
		return classroomRepository.save(classroom);
	}

	@Transactional
	public Classroom updateCapacity(String publicId, Integer capacity) {
		Classroom classroom = findByPublicId(publicId);
		classroom.updateCapacity(capacity);
		return classroomRepository.save(classroom);
	}

	// Changes which grading scale this classroom resolves to (via the new curriculum) — see
	// GradingScaleService.CACHE_GRADING_SCALE_THRESHOLDS for why this can only evict wholesale.
	@Transactional
	@CacheEvict(cacheNames = GradingScaleService.CACHE_GRADING_SCALE_THRESHOLDS, allEntries = true)
	public Classroom assignCurriculum(String publicId, String curriculumPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Classroom classroom = findByPublicId(publicId);
		var curriculum = curriculumRepository.findByPublicIdAndTenantId(UUID.fromString(curriculumPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Curriculum not found: " + curriculumPublicId));
		classroom.assignCurriculum(curriculum.getId());
		return classroomRepository.save(classroom);
	}

	@Transactional
	public StudentClassroomLink enrollStudent(String classroomPublicId, String studentPublicId,
			String academicYearPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Classroom classroom = classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomPublicId));
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		AcademicYear academicYear = academicYearRepository
				.findByPublicIdAndTenantId(UUID.fromString(academicYearPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + academicYearPublicId));
		if (studentClassroomLinkRepository.existsByStudentIdAndAcademicYearIdAndTenantId(student.getId(),
				academicYear.getId(), tenantId)) {
			throw new BusinessException(
					"Student " + studentPublicId + " is already enrolled in a classroom for academic year "
							+ academicYearPublicId);
		}
		if (classroom.getCapacity() != null
				&& studentClassroomLinkRepository.countByClassroomId(tenantId, classroom.getId()) >= classroom
						.getCapacity()) {
			throw new BusinessException("Classroom " + classroomPublicId + " is at full capacity");
		}
		StudentClassroomLink link = StudentClassroomLink.create(student.getId(), classroom.getId(),
				academicYear.getId(), LocalDate.now());
		StudentClassroomLink saved;
		try {
			saved = studentClassroomLinkRepository.save(link);
		} catch (DataIntegrityViolationException e) {
			// The pre-check above is a fast-path convenience, not the real guard — the
			// uq_scl_student_active_academic_year DB constraint (051-classroom-link-soft-delete-
			// unique-fix.xml) is what actually prevents two concurrent enrollments for the same
			// student/academic year from both passing the check and both inserting a row.
			throw new BusinessException(
					"Student " + studentPublicId + " is already enrolled in a classroom for academic year "
							+ academicYearPublicId);
		}
		eventPublisher.publish(new StudentEnrolledInClassroomEvent(tenantId, student.getId(), classroom.getId(),
				academicYear.getId()));
		return saved;
	}

	@Transactional
	public void withdrawStudentFromClassroom(String classroomPublicId, String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Classroom classroom = classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomPublicId));
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		StudentClassroomLink link = studentClassroomLinkRepository
				.findByStudentIdAndClassroomId(tenantId, student.getId(), classroom.getId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Student " + studentPublicId + " is not enrolled in classroom " + classroomPublicId));
		link.softDelete("classroom-withdrawal");
		studentClassroomLinkRepository.save(link);
	}

	@Transactional(readOnly = true)
	public Page<Student> listRoster(String classroomPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Classroom classroom = findByPublicId(classroomPublicId);
		Page<StudentClassroomLink> links = studentClassroomLinkRepository.findByClassroomId(tenantId,
				classroom.getId(), pageable);
		List<Student> students = links.getContent().stream()
				.map(link -> studentRepository.findByIdAndTenantId(link.getStudentId(), tenantId)
						.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + link.getStudentId())))
				.toList();
		return new PageImpl<>(students, pageable, links.getTotalElements());
	}
}
