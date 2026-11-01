package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.bulk.BulkEntryResult;
import com.altafjava.school.application.bulk.BulkEntryResult.Outcome;
import com.altafjava.school.application.bulk.BulkEntryResult.Row;
import com.altafjava.school.application.security.AcademicAccessGuard;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceCorrection;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceCorrectionRepository;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.student.model.Student;
import lombok.RequiredArgsConstructor;

/** Marks a whole class in one request. Sending the same sheet twice changes nothing. */
@Service
@RequiredArgsConstructor
public class AttendanceBulkService {

	public record Entry(String studentPublicId, AttendanceStatus status) {
	}

	private final AttendanceRepository attendanceRepository;
	private final AttendanceCorrectionRepository attendanceCorrectionRepository;
	private final ClassroomRepository classroomRepository;
	private final ClassRoster classRoster;
	private final AcademicAccessGuard academicAccessGuard;

	@Transactional
	public BulkEntryResult mark(String classroomPublicId, LocalDate date, List<Entry> entries, String markedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Long classroomId = classroomRepository.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomPublicId))
				.getId();
		academicAccessGuard.assertCanWriteClassroom(tenantId, classroomId);

		Map<String, Student> roster = classRoster.byPublicId(tenantId, classroomId);
		Map<Long, Attendance> marked = attendanceRepository.findByClassroomAndDate(tenantId, classroomId, date).stream()
				.collect(Collectors.toMap(Attendance::getStudentId, Function.identity()));
		List<Row> rows = new ArrayList<>();
		for (Entry entry : entries) {
			Student student = roster.get(entry.studentPublicId());
			if (student == null) {
				rows.add(Row.failed(entry.studentPublicId(), "Student is not enrolled in this classroom"));
				continue;
			}
			Attendance existing = marked.get(student.getId());
			rows.add(existing == null
					? create(student, classroomId, date, entry, markedBy, marked)
					: reconcile(existing, entry));
		}
		return BulkEntryResult.of(rows);
	}

	private Row create(Student student, Long classroomId, LocalDate date, Entry entry, String markedBy,
			Map<Long, Attendance> marked) {
		Attendance saved = attendanceRepository
				.save(Attendance.create(student.getId(), classroomId, date, entry.status(), markedBy));
		marked.put(student.getId(), saved);
		return Row.written(entry.studentPublicId(), Outcome.CREATED, saved.getPublicId().toString());
	}

	private Row reconcile(Attendance existing, Entry entry) {
		String recordPublicId = existing.getPublicId().toString();
		if (existing.getStatus() == entry.status()) {
			return Row.written(entry.studentPublicId(), Outcome.UNCHANGED, recordPublicId);
		}
		attendanceCorrectionRepository
				.save(AttendanceCorrection.record(existing.getId(), existing.getStatus(), entry.status()));
		existing.updateStatus(entry.status());
		attendanceRepository.save(existing);
		return Row.written(entry.studentPublicId(), Outcome.UPDATED, recordPublicId);
	}
}
