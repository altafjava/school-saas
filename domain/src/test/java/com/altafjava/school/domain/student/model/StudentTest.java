package com.altafjava.school.domain.student.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.exception.BusinessException;

class StudentTest {

	private Student newStudent() {
		return Student.create("STU-001", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
	}

	@Test
	void withdraw_fromActive_transitionsToWithdrawn() {
		Student student = newStudent();

		student.withdraw();

		assertEquals(EnrollmentStatus.WITHDRAWN, student.getEnrollmentStatus());
	}

	@Test
	void withdraw_alreadyGraduated_throwsBusinessException() {
		Student student = newStudent();
		student.graduate();

		assertThrows(BusinessException.class, student::withdraw);
	}

	@Test
	void graduate_fromActive_transitionsToGraduated() {
		Student student = newStudent();

		student.graduate();

		assertEquals(EnrollmentStatus.GRADUATED, student.getEnrollmentStatus());
	}

	@Test
	void graduate_alreadyWithdrawn_throwsBusinessException() {
		Student student = newStudent();
		student.withdraw();

		assertThrows(BusinessException.class, student::graduate);
	}

	@Test
	void transfer_fromActive_transitionsToTransferred() {
		Student student = newStudent();

		student.transfer();

		assertEquals(EnrollmentStatus.TRANSFERRED, student.getEnrollmentStatus());
	}

	@Test
	void transfer_alreadyGraduated_throwsBusinessException() {
		Student student = newStudent();
		student.graduate();

		assertThrows(BusinessException.class, student::transfer);
	}

	@Test
	void graduate_alreadyTransferred_throwsBusinessException() {
		Student student = newStudent();
		student.transfer();

		assertThrows(BusinessException.class, student::graduate);
	}

	@Test
	void updateContactDetails_replacesMutableFields() {
		Student student = newStudent();

		student.updateContactDetails("Alicia", "Jones", "alicia@school.test", LocalDate.of(2010, 2, 2));

		assertEquals("Alicia", student.getFirstName());
		assertEquals("Jones", student.getLastName());
		assertEquals("alicia@school.test", student.getEmail());
		assertEquals(LocalDate.of(2010, 2, 2), student.getDateOfBirth());
	}

	@Test
	void erasePii_clearsContactPiiButKeepsOperationalIdentifiers() {
		Student student = newStudent();
		student.updatePhone("+14155552671");

		student.erasePii();

		assertEquals("[erased]", student.getFirstName());
		assertEquals("[erased]", student.getLastName());
		assertEquals(null, student.getEmail());
		assertEquals(null, student.getPhone());
		assertEquals(null, student.getAddress());
		assertEquals("STU-001", student.getStudentCode());
		assertEquals(LocalDate.of(2010, 1, 1), student.getDateOfBirth());
	}

	@Test
	void suspend_thenReinstate_roundTripsToActive() {
		Student student = newStudent();

		student.suspend();
		assertEquals(EnrollmentStatus.SUSPENDED, student.getEnrollmentStatus());
		student.reinstate();

		assertEquals(EnrollmentStatus.ACTIVE, student.getEnrollmentStatus());
	}

	@Test
	void suspend_whenNotActive_throws() {
		Student student = newStudent();
		student.suspend();

		assertThrows(BusinessException.class, student::suspend);
	}

	@Test
	void reinstate_whenNotSuspended_throws() {
		assertThrows(BusinessException.class, newStudent()::reinstate);
	}

	@Test
	void withdraw_fromSuspended_isAllowed() {
		Student student = newStudent();
		student.suspend();

		student.withdraw();

		assertEquals(EnrollmentStatus.WITHDRAWN, student.getEnrollmentStatus());
	}

	@Test
	void graduate_fromSuspended_throws() {
		Student student = newStudent();
		student.suspend();

		assertThrows(BusinessException.class, student::graduate);
	}

	@Test
	void leavingTwice_isRejected_soTheRetentionClockCannotBeReset() {
		for (java.util.function.Consumer<Student> exit : java.util.List.<java.util.function.Consumer<Student>>of(
				Student::withdraw, Student::transfer)) {
			Student student = newStudent();
			student.withdraw();
			java.time.Instant leftAt = student.getEnrollmentStatusChangedAt();

			assertThrows(BusinessException.class, () -> exit.accept(student));
			assertEquals(leftAt, student.getEnrollmentStatusChangedAt());
		}
	}

	private Student studentWithId(long id) {
		Student student = newStudent();
		student.setId(id);
		return student;
	}

	@Test
	void isSiblingOf_studentsInTheSameGroup_isTrueBothWays() {
		Student alice = studentWithId(1L);
		Student bob = studentWithId(2L);
		alice.joinSiblingGroup(9L);
		bob.joinSiblingGroup(9L);

		assertTrue(alice.isSiblingOf(bob));
		assertTrue(bob.isSiblingOf(alice));
	}

	@Test
	void isSiblingOf_studentsInDifferentGroupsOrNone_isFalse() {
		Student alice = studentWithId(1L);
		Student bob = studentWithId(2L);

		assertFalse(alice.isSiblingOf(bob));

		alice.joinSiblingGroup(9L);
		bob.joinSiblingGroup(10L);
		assertFalse(alice.isSiblingOf(bob));
	}

	@Test
	void isSiblingOf_aStudentIsNeverTheirOwnSibling() {
		Student alice = studentWithId(1L);
		alice.joinSiblingGroup(9L);

		assertFalse(alice.isSiblingOf(alice));
	}

	@Test
	void leaveSiblingGroup_clearsMembership() {
		Student alice = studentWithId(1L);
		alice.joinSiblingGroup(9L);

		alice.leaveSiblingGroup();

		assertNull(alice.getSiblingGroupId());
	}

	@Test
	void erasePii_removesTheStudentFromTheirFamily() {
		Student alice = studentWithId(1L);
		alice.joinSiblingGroup(9L);

		alice.erasePii();

		assertNull(alice.getSiblingGroupId());
	}
}
