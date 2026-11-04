package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.search.LikePattern;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.curriculum.model.Board;
import com.altafjava.school.domain.curriculum.repository.BoardRepository;
import com.altafjava.school.domain.error.SchoolErrorCode;

@Service
public class BoardService {

	/** Board reference data changes rarely and is read on nearly every admission/roster path. */
	private final BoardRepository boardRepository;

	public BoardService(BoardRepository boardRepository) {
		this.boardRepository = boardRepository;
	}

	@Transactional(readOnly = true)
	public Page<Board> list(String q, Pageable pageable) {
		return boardRepository.search(TenantContext.getCurrentTenantId(), LikePattern.contains(q), pageable);
	}

	@Transactional(readOnly = true)
	public Board findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return boardRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Board not found: " + publicId));
	}

	@Transactional
	public Board create(String name, String code, String description) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (boardRepository.existsByCodeAndTenantId(code, tenantId)) {
			throw new BusinessException(SchoolErrorCode.CODE_TAKEN, "Board code already exists: " + code);
		}
		return boardRepository.save(Board.create(name, code, description));
	}

	@Transactional
	public Board updateDetails(String publicId, String name, String code, String description,
			ExpectedVersion expectedVersion) {
		Board board = findByPublicId(publicId);
		expectedVersion.verify(board);
		board.updateDetails(name, code, description);
		return boardRepository.save(board);
	}

	@Transactional
	public Board deactivate(String publicId) {
		Board board = findByPublicId(publicId);
		board.deactivate();
		return boardRepository.save(board);
	}
}
