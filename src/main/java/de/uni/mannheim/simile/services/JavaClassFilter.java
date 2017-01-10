package de.uni.mannheim.simile.services;

import java.io.File;

public class JavaClassFilter implements Filter {
	@Override
	public boolean interested(int level, String path, File file) {
		return path.endsWith(".java");
	}
}
