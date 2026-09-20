package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.ActivityLogService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.counseling.model.CounselingSession;
import com.altafjava.school.domain.counseling.repository.CounselingSessionRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

@Service
public class CounselingSessionService {

	private final CounselingSessionRepository counselingSessionRepository;
	private final StudentRepository studentRepository;
	private final TeacherRepository teacherRepository;
	private final ActivityLogService activityLogService;

	public CounselingSessionService(CounselingSessionRepository counselingSessionRepository,
			StudentRepository studentRepository, TeacherRepository teacherRepository,
			ActivityLogService activityLogService) {
		this.counselingSessionRepository = counselingSessionRepository;
		this.studentRepository = studentRepository;
		this.teacherRepository = teacherRepository;
		this.activityLogService = activityLogService;
	}

	@Transactional(readOnly = true)
	public Page<CounselingSession> listAll(Pageable pageable) {
		return counselingSessionRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Page<CounselingSession> listForStudent(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = resolveStudent(studentPublicId, tenantId);
		return counselingSessionRepository.findAllByStudentIdAndTenantId(student.getId(), tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public CounselingSession get(String publicId) {
		return findByPublicId(publicId);
	}

	@Transactional
	public CounselingSession schedule(String studentPublicId, String counselorTeacherPublicId, LocalDate sessionDate,
			String notes, boolean followUpRequired) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = resolveStudent(studentPublicId, tenantId);
		Teacher counselor = teacherRepository
				.findByPublicIdAndTenantId(UUID.fromString(counselorTeacherPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Teacher not found: " + counselorTeacherPublicId));

		CounselingSession session = CounselingSession.schedule(student.getId(), counselor.getId(), sessionDate,
				notes, followUpRequired);
		CounselingSession saved = counselingSessionRepository.save(session);
		logAction(tenantId, "CREATE", String.valueOf(saved.getId()), "Counseling session scheduled");
		return saved;
	}

	@Transactional
	public CounselingSession updateNotes(String publicId, String notes, boolean followUpRequired) {
		CounselingSession session = findByPublicId(publicId);
		session.updateNotes(notes, followUpRequired);
		CounselingSession saved = counselingSessionRepository.save(session);
		// Note content is deliberately excluded from the audit trail — confidential counseling notes.
		logAction(TenantContext.getCurrentTenantId(), "UPDATE", publicId, "Counseling session notes updated");
		return saved;
	}

	private void logAction(Long tenantId, String action, String resourceId, String details) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String actorId = authentication != null ? authentication.getName() : "system";
		activityLogService.log(tenantId, action, "CounselingSession", resourceId, actorId, null, null, details,
				null, null);
	}

	private CounselingSession findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return counselingSessionRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Counseling session not found: " + publicId));
	}

	private Student resolveStudent(String studentPublicId, Long tenantId) {
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
	}
}
