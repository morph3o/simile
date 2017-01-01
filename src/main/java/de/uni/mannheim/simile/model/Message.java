package de.uni.mannheim.simile.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Data
public class Message {
	private final String message;
	private final int status;
}
