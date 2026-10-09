package com.altafjava.school.api.support;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import com.altafjava.school.application.service.AdmissionService;
import com.altafjava.school.application.service.BookCatalogService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.FeeStructureService;
import com.altafjava.school.application.service.GuardianService;
import com.altafjava.school.application.service.HolidayService;
import com.altafjava.school.application.service.LeaveTypeService;
import com.altafjava.school.application.service.RouteService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.SubjectService;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.application.service.VehicleService;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;

/**
 * Resolves the public id of an entity that can carry custom fields to its row id, through that
 * entity's own service so tenant scoping and not-found handling stay where they already live.
 * Adding a {@link CustomFieldEntityType} means adding one line here.
 */
@Component
public class CustomFieldEntityResolver {

	private final Map<CustomFieldEntityType, Function<String, Long>> resolvers = new EnumMap<>(
			CustomFieldEntityType.class);

	public CustomFieldEntityResolver(StudentService students, TeacherService teachers, GuardianService guardians,
			AdmissionService admissions, ClassroomService classrooms, SubjectService subjects,
			FeeStructureService feeStructures, LeaveTypeService leaveTypes, HolidayService holidays,
			RouteService routes, VehicleService vehicles, BookCatalogService books) {
		resolvers.put(CustomFieldEntityType.STUDENT, id -> students.findByPublicId(id).getId());
		resolvers.put(CustomFieldEntityType.TEACHER, id -> teachers.findByPublicId(id).getId());
		resolvers.put(CustomFieldEntityType.GUARDIAN, id -> guardians.findByPublicId(id).getId());
		resolvers.put(CustomFieldEntityType.ADMISSION, id -> admissions.findByPublicId(id).getId());
		resolvers.put(CustomFieldEntityType.CLASSROOM, id -> classrooms.findByPublicId(id).getId());
		resolvers.put(CustomFieldEntityType.SUBJECT, id -> subjects.findByPublicId(id).getId());
		resolvers.put(CustomFieldEntityType.FEE_STRUCTURE, id -> feeStructures.findByPublicId(id).getId());
		resolvers.put(CustomFieldEntityType.LEAVE_TYPE, id -> leaveTypes.findByPublicId(id).getId());
		resolvers.put(CustomFieldEntityType.HOLIDAY, id -> holidays.findByPublicId(id).getId());
		resolvers.put(CustomFieldEntityType.TRANSPORT_ROUTE, id -> routes.findByPublicId(id).getId());
		resolvers.put(CustomFieldEntityType.TRANSPORT_VEHICLE, id -> vehicles.findByPublicId(id).getId());
		resolvers.put(CustomFieldEntityType.BOOK, id -> books.findByPublicId(id).getId());
	}

	public Long resolveId(CustomFieldEntityType type, String publicId) {
		return resolvers.get(type).apply(publicId);
	}
}
