package de.uni.mannheim.simile.services;

import java.io.File;

public class JavaClassHandler implements FileHandler {
	@Override
	public void handle(int level, String path, File file) {
		path.endsWith(".java");
	}
}
