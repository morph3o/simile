package de.uni.mannheim.simile.services;

import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class DirectoryExplorer {
	private final FileHandler fileHandler;
	private final Filter filter;

	public void explore(File root) {
		explore(0, "", root);
	}

	private void explore(int level, String path, File file) {
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				explore(level + 1, path + "/" + child.getName(), child);
			}
		} else {
			if (filter.interested(level, path, file)) {
				fileHandler.handle(level, path, file);
			}
		}
	}
}
