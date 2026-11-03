package com.altafjava.school.domain.exam.model;

/** Where an exam's marks stand on the way to publication. Only {@link #ENTERED} marks can change. */
public enum MarksStage {
	/** Marks are being recorded and corrected. */
	ENTERED,
	/** Someone other than the markers has checked them; they are frozen unless returned. */
	VERIFIED,
	/** Final. Results can be published only from here. */
	LOCKED
}
