package de.uni.mannheim.simile.services;

import java.io.File;

public interface FileHandler {
	void handle(int level, String path, File file);
}
