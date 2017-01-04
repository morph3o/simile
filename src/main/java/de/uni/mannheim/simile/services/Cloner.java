package de.uni.mannheim.simile.services;

import de.uni.mannheim.simile.util.StreamGobbler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class Cloner {

	@Getter
	private final String repo;
	@Getter
	private final String branch;
	@Getter
	private final String folder;

	public int cloneRepository() throws IOException {
		int exitVal = 0;
		try {
			String command = this.setCommand(repo, branch, folder);
			Process p = Runtime.getRuntime().exec(command);
			StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "INFO");
			errorGobbler.start();
			exitVal = p.waitFor();
			return exitVal;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return exitVal;
	}

	String setCommand(String repo, String branch, String folder) {
		if(!branch.isEmpty()) {
			return String.format("git clone %s --branch %s %s", repo, branch, folder);
		} else {
			return String.format("git clone %s %s", repo, folder);
		}
	}

}
