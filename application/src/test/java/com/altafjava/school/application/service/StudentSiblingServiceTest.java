package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.student.model.SiblingGroup;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.SiblingGroupRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentSiblingServiceTest {

	private static final Long TENANT_ID = 1L;

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private SiblingGroupRepository siblingGroupRepository;

	private StudentSiblingService service;
	private Student alice;
	private Student bob;
	private Student cara;

	@BeforeEach
	void setUp() {
		service = new StudentSiblingService(studentRepository, siblingGroupRepository);
		TenantContext.ForTesting.setCurrentTenant(TENANT_ID, null, null, TenantType.SHARED);
		alice = student(1L, "Alice");
		bob = student(2L, "Bob");
		cara = student(3L, "Cara");
		lenient().when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
		lenient().when(studentRepository.saveAndFlush(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Student student(long id, String firstName) {
		Student student = Student.create("STU-" + id, firstName, "Smith", firstName + "@school.test",
				LocalDate.of(2010, 1, 1));
		student.setId(id);
		student.setPublicId(UUID.randomUUID());
		lenient().when(studentRepository.findByPublicIdAndTenantId(student.getPublicId(), TENANT_ID))
				.thenReturn(Optional.of(student));
		return student;
	}

	private String id(Student student) {
		return student.getPublicId().toString();
	}

	private SiblingGroup groupWithId(long id) {
		SiblingGroup group = SiblingGroup.create();
		group.setId(id);
		lenient().when(siblingGroupRepository.findByIdAndTenantId(id, TENANT_ID)).thenReturn(Optional.of(group));
		return group;
	}

	private void membersOf(long groupId, Student... members) {
		lenient().when(studentRepository.findAllBySiblingGroupIdAndTenantId(groupId, TENANT_ID))
				.thenReturn(List.of(members));
	}

	@Test
	void link_twoStudentsWithNoFamily_createsOneAndPutsBothInIt() {
		SiblingGroup created = groupWithId(50L);
		when(siblingGroupRepository.save(any(SiblingGroup.class))).thenReturn(created);
		membersOf(50L, alice, bob);

		List<Student> siblings = service.link(id(alice), id(bob));

		assertEquals(50L, alice.getSiblingGroupId());
		assertEquals(50L, bob.getSiblingGroupId());
		assertEquals(List.of(bob), siblings);
	}

	@Test
	void link_toAStudentWhoAlreadyHasSiblings_joinsTheirFamily() {
		bob.joinSiblingGroup(50L);
		cara.joinSiblingGroup(50L);
		membersOf(50L, alice, bob, cara);

		List<Student> siblings = service.link(id(alice), id(bob));

		assertEquals(50L, alice.getSiblingGroupId());
		assertEquals(List.of(bob, cara), siblings);
		verify(siblingGroupRepository, never()).save(any());
	}

	@Test
	void link_aStudentWhoAlreadyHasSiblingsToANewcomer_bringsTheNewcomerIn() {
		alice.joinSiblingGroup(50L);
		bob.joinSiblingGroup(50L);
		membersOf(50L, alice, bob, cara);

		service.link(id(alice), id(cara));

		assertEquals(50L, cara.getSiblingGroupId());
	}

	@Test
	void link_twoExistingFamilies_mergesThemAndRetiresTheOtherGroup() {
		alice.joinSiblingGroup(50L);
		bob.joinSiblingGroup(50L);
		cara.joinSiblingGroup(60L);
		Student dan = student(4L, "Dan");
		dan.joinSiblingGroup(60L);
		SiblingGroup retired = groupWithId(60L);
		membersOf(60L, cara, dan);
		membersOf(50L, alice, bob, cara, dan);

		service.link(id(alice), id(cara));

		assertEquals(50L, cara.getSiblingGroupId());
		assertEquals(50L, dan.getSiblingGroupId());
		assertTrue(retired.isDeleted());
	}

	@Test
	void link_studentsAlreadySiblings_throwsBusinessException() {
		alice.joinSiblingGroup(50L);
		bob.joinSiblingGroup(50L);

		assertThrows(BusinessException.class, () -> service.link(id(alice), id(bob)));
	}

	@Test
	void link_aStudentToThemselves_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> service.link(id(alice), id(alice)));
	}

	@Test
	void link_anUnknownSibling_throwsResourceNotFound() {
		assertThrows(ResourceNotFoundException.class, () -> service.link(id(alice), UUID.randomUUID().toString()));
	}

	@Test
	void leave_fromAFamilyOfThree_leavesTheOtherTwoTogether() {
		alice.joinSiblingGroup(50L);
		bob.joinSiblingGroup(50L);
		cara.joinSiblingGroup(50L);
		SiblingGroup group = groupWithId(50L);
		membersOf(50L, bob, cara);

		service.leave(id(alice));

		assertNull(alice.getSiblingGroupId());
		assertEquals(50L, bob.getSiblingGroupId());
		assertEquals(false, group.isDeleted());
	}

	@Test
	void leave_fromAFamilyOfTwo_dissolvesTheFamily() {
		alice.joinSiblingGroup(50L);
		bob.joinSiblingGroup(50L);
		SiblingGroup group = groupWithId(50L);
		membersOf(50L, bob);

		service.leave(id(alice));

		assertNull(alice.getSiblingGroupId());
		assertNull(bob.getSiblingGroupId());
		assertTrue(group.isDeleted());
	}

	@Test
	void leave_aStudentWithNoSiblings_throwsBusinessException() {
		assertThrows(BusinessException.class, () -> service.leave(id(alice)));
	}

	@Test
	void listSiblings_excludesTheStudentThemselves() {
		alice.joinSiblingGroup(50L);
		membersOf(50L, bob, alice);

		assertEquals(List.of(bob), service.listSiblings(id(alice)));
	}

	@Test
	void listSiblings_withNoFamily_isEmpty() {
		assertTrue(service.listSiblings(id(alice)).isEmpty());
	}

	@Test
	void suggestSiblings_offersGuardianSharersNotAlreadySiblings() {
		alice.joinSiblingGroup(50L);
		bob.joinSiblingGroup(50L);
		when(studentRepository.findStudentsSharingAGuardianWith(TENANT_ID, 1L)).thenReturn(List.of(bob, cara));

		assertEquals(List.of(cara), service.suggestSiblings(id(alice)));
	}
}
