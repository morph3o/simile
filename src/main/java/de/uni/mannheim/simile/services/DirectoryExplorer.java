package de.uni.mannheim.simile.services;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.File;

@RequiredArgsConstructor
public class DirectoryExplorer {
	@Setter
	private final FileHandler fileHandler;
	@Setter
	private final Filter filter;
	@Setter
	private File projectDir;

	public void explore() {
		explore(0, "", this.projectDir);
	}

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
