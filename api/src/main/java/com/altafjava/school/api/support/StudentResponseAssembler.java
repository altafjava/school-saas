package com.altafjava.school.api.support;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.api.mapper.StudentMapper;
import com.altafjava.school.application.service.StudentPlacementService;
import com.altafjava.school.application.student.StudentPlacement;
import com.altafjava.school.domain.student.model.Student;

/** Builds {@link StudentResponse}s with their current classroom, looked up once for a whole page. */
@Component
public class StudentResponseAssembler {

	private final StudentMapper studentMapper;
	private final StudentPlacementService studentPlacementService;

	public StudentResponseAssembler(StudentMapper studentMapper, StudentPlacementService studentPlacementService) {
		this.studentMapper = studentMapper;
		this.studentPlacementService = studentPlacementService;
	}

	public StudentResponse toResponse(Student student) {
		return assemble(student, studentPlacementService.currentPlacements(List.of(student)).get(student.getId()));
	}

	public Page<StudentResponse> toResponses(Page<Student> students) {
		var placements = studentPlacementService.currentPlacements(students.getContent());
		return students.map(student -> assemble(student, placements.get(student.getId())));
	}

	private StudentResponse assemble(Student student, StudentPlacement placement) {
		return StudentResponse.placedIn(studentMapper.toResponseWithoutPlacement(student),
				placement == null ? null : studentMapper.toCurrentClassroom(placement));
	}
}
