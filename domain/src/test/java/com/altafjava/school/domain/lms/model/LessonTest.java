package com.altafjava.school.domain.lms.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class LessonTest {

	@Test
	void post_setsFieldsAndPostedAt() {
		Lesson lesson = Lesson.post(1L, 2L, 3L, "Photosynthesis", "Chapter 4 overview", "tenants/1/lessons/key.pdf",
				LocalDateTime.now());

		assertEquals(1L, lesson.getClassroomId());
		assertEquals(2L, lesson.getSubjectId());
		assertEquals(3L, lesson.getTeacherId());
		assertEquals("Photosynthesis", lesson.getTitle());
		assertEquals("Chapter 4 overview", lesson.getDescription());
		assertEquals("tenants/1/lessons/key.pdf", lesson.getStorageKey());
		assertNotNull(lesson.getPostedAt());
	}

	@Test
	void post_withoutStorageKey_leavesStorageKeyNull() {
		Lesson lesson = Lesson.post(1L, 2L, 3L, "Photosynthesis", null, null, LocalDateTime.now());

		assertNull(lesson.getStorageKey());
		assertNull(lesson.getDescription());
	}
}
