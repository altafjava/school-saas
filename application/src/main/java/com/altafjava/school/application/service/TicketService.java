package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.reference.UserReferenceResolver;
import com.altafjava.school.domain.helpdesk.model.Ticket;
import com.altafjava.school.domain.helpdesk.model.TicketCategory;
import com.altafjava.school.domain.helpdesk.model.TicketStatus;
import com.altafjava.school.domain.helpdesk.repository.TicketRepository;

@Service
public class TicketService {

	private final TicketRepository ticketRepository;
	private final UserReferenceResolver userReferenceResolver;

	public TicketService(TicketRepository ticketRepository, UserReferenceResolver userReferenceResolver) {
		this.ticketRepository = ticketRepository;
		this.userReferenceResolver = userReferenceResolver;
	}

	@Transactional(readOnly = true)
	public Page<Ticket> search(TicketStatus status, TicketCategory category, String assignedToUserPublicId,
			Pageable pageable) {
		Long assignedToUserId = assignedToUserPublicId == null ? null
				: userReferenceResolver.requireUserId(assignedToUserPublicId);
		return ticketRepository.search(TenantContext.getCurrentTenantId(), status, category, assignedToUserId,
				pageable);
	}

	@Transactional(readOnly = true)
	public Page<Ticket> listMine(Pageable pageable) {
		return ticketRepository.findAllByTenantIdAndRaisedByUserId(TenantContext.getCurrentTenantId(),
				resolveCurrentUserId(), pageable);
	}

	@Transactional(readOnly = true)
	public Ticket get(String publicId) {
		return findByPublicId(publicId);
	}

	@Transactional
	public Ticket raise(TicketCategory category, String subject, String description) {
		Ticket ticket = Ticket.raise(resolveCurrentUserId(), category, subject, description);
		return ticketRepository.save(ticket);
	}

	@Transactional
	public Ticket assign(String publicId, String assignedToUserPublicId) {
		Ticket ticket = findByPublicId(publicId);
		ticket.assign(userReferenceResolver.requireUserId(assignedToUserPublicId));
		return ticketRepository.save(ticket);
	}

	@Transactional
	public Ticket resolve(String publicId, String resolutionText) {
		Ticket ticket = findByPublicId(publicId);
		ticket.resolve(resolutionText);
		return ticketRepository.save(ticket);
	}

	@Transactional
	public Ticket close(String publicId) {
		Ticket ticket = findByPublicId(publicId);
		ticket.close();
		return ticketRepository.save(ticket);
	}

	@Transactional
	public Ticket reopen(String publicId) {
		Ticket ticket = findByPublicId(publicId);
		ticket.reopen();
		return ticketRepository.save(ticket);
	}

	private Ticket findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return ticketRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + publicId));
	}

	private Long resolveCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return user.getId();
		}
		throw new AccessDeniedException("Authenticated principal missing — cannot resolve current user");
	}
}
