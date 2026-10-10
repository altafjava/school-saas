package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.reference.UserReferenceResolver;
import com.altafjava.school.domain.helpdesk.model.Ticket;
import com.altafjava.school.domain.helpdesk.model.TicketCategory;
import com.altafjava.school.domain.helpdesk.repository.TicketRepository;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

	private static final Long CURRENT_USER_ID = 55L;

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private UserReferenceResolver userReferenceResolver;

	private TicketService ticketService;

	@BeforeEach
	void setUp() {
		ticketService = new TicketService(ticketRepository, userReferenceResolver);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private void authenticateAsUser(Long userId) {
		AuthenticatedUser principal = mock(AuthenticatedUser.class);
		when(principal.getId()).thenReturn(userId);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
	}

	@Test
	void raise_resolvesRaisingUserFromSecurityContext() {
		authenticateAsUser(CURRENT_USER_ID);
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

		Ticket ticket = assertDoesNotThrow(() -> ticketService.raise(TicketCategory.TECHNICAL, "Cannot log in",
				"Login page shows a blank screen"));

		assertEquals(CURRENT_USER_ID, ticket.getRaisedByUserId());
	}

	@Test
	void raise_noAuthenticatedPrincipal_throwsAccessDeniedException() {
		assertThrows(AccessDeniedException.class,
				() -> ticketService.raise(TicketCategory.TECHNICAL, "Cannot log in", "Blank screen"));
	}

	@Test
	void get_unknownPublicId_throwsResourceNotFoundException() {
		UUID publicId = UUID.randomUUID();
		when(ticketRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> ticketService.get(publicId.toString()));
	}

	@Test
	void assign_setsAssigneeAndInProgressStatus() {
		UUID publicId = UUID.randomUUID();
		Ticket ticket = Ticket.raise(1L, TicketCategory.TECHNICAL, "Cannot log in", "Blank screen");
		when(ticketRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(ticket));
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

		String assigneePublicId = UUID.randomUUID().toString();
		when(userReferenceResolver.requireUserId(assigneePublicId)).thenReturn(99L);

		Ticket assigned = ticketService.assign(publicId.toString(), assigneePublicId);

		assertEquals(99L, assigned.getAssignedToUserId());
	}

	@Test
	void resolve_setsResolutionText() {
		UUID publicId = UUID.randomUUID();
		Ticket ticket = Ticket.raise(1L, TicketCategory.TECHNICAL, "Cannot log in", "Blank screen");
		when(ticketRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(ticket));
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

		Ticket resolved = ticketService.resolve(publicId.toString(), "Reset the password");

		assertEquals("Reset the password", resolved.getResolution());
	}

	@Test
	void close_afterResolve_succeeds() {
		UUID publicId = UUID.randomUUID();
		Ticket ticket = Ticket.raise(1L, TicketCategory.TECHNICAL, "Cannot log in", "Blank screen");
		ticket.resolve("Reset the password");
		when(ticketRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(ticket));
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

		Ticket closed = assertDoesNotThrow(() -> ticketService.close(publicId.toString()));

		assertEquals(com.altafjava.school.domain.helpdesk.model.TicketStatus.CLOSED, closed.getStatus());
	}

	@Test
	void reopen_afterClose_setsOpenStatus() {
		UUID publicId = UUID.randomUUID();
		Ticket ticket = Ticket.raise(1L, TicketCategory.TECHNICAL, "Cannot log in", "Blank screen");
		ticket.resolve("Reset the password");
		ticket.close();
		when(ticketRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(ticket));
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));

		Ticket reopened = ticketService.reopen(publicId.toString());

		assertEquals(com.altafjava.school.domain.helpdesk.model.TicketStatus.OPEN, reopened.getStatus());
	}

	@Test
	void listMine_scopesToCurrentRaisingUser() {
		authenticateAsUser(CURRENT_USER_ID);
		when(ticketRepository.findAllByTenantIdAndRaisedByUserId(1L, CURRENT_USER_ID, PageRequest.of(0, 20)))
				.thenReturn(org.springframework.data.domain.Page.empty());

		assertDoesNotThrow(() -> ticketService.listMine(PageRequest.of(0, 20)));
	}
}
