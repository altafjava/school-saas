package com.altafjava.school.application.exam;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.school.domain.error.AcademicErrorCode;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamStatus;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import lombok.RequiredArgsConstructor;

/** Refuses an exam slot that overlaps another live exam of the same classroom or in the same venue. */
@Component
@RequiredArgsConstructor
public class ExamClashGuard {

	private final ExamRepository examRepository;

	public void assertNoClash(Long tenantId, Exam candidate) {
		examRepository
				.findClashCandidates(tenantId, ExamStatus.CANCELLED,
						candidate.getScheduledAt().minusMinutes(Exam.MAX_DURATION_MINUTES), candidate.endsAt(),
						candidate.getClassroomId(), candidate.getVenueId())
				.stream()
				.filter(candidate::clashesWith)
				.findFirst()
				.ifPresent(clash -> {
					throw new BusinessException(AcademicErrorCode.EXAM_CLASH,
							"The exam overlaps \"" + clash.getTitle() + "\" ("
									+ clash.getScheduledAt() + " to " + clash.endsAt().toLocalTime() + ") "
									+ (clash.getClassroomId().equals(candidate.getClassroomId())
											? "in the same classroom"
											: "in the same venue"),
							Map.of("exam", clash.getTitle()));
				});
	}
}
