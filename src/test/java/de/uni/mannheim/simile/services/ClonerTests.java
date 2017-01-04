package de.uni.mannheim.simile.services;

import org.junit.Before;
import org.junit.Test;

import javax.rmi.PortableRemoteObject;
import java.io.IOException;

import static org.assertj.core.api.Assertions.*;

public class ClonerTests {

	private static final String REPO = "https://github.com/morph3o/simile.git";
	private static final String BRANCH = "master";
	private static final String FOLDER = "project";

	private Cloner cloner;

	@Before
	public void setUp(){
		this.cloner = new Cloner(REPO, BRANCH, FOLDER);
	}

	@Test
	public void itShouldReturnStringWithRepoAndBranch() {
		assertThat(this.cloner.getRepo()).isEqualTo(REPO);
		assertThat(this.cloner.getBranch()).isEqualTo(BRANCH);
		assertThat(this.cloner.getFolder()).isEqualTo(FOLDER);
	}

	@Test
	public void itShouldSetCommandWithRepoAndBranch() {
		assertThat(this.cloner.setCommand(REPO, BRANCH, FOLDER)).isEqualTo(String.format("git clone %s --branch %s %s", REPO, BRANCH, FOLDER));
	}

	@Test
	public void itShouldSetCommandWithRepo() {
		assertThat(this.cloner.setCommand(REPO, "", FOLDER)).isEqualTo(String.format("git clone %s %s", REPO, FOLDER));
	}

	@Test
	public void itShouldCloneRepo() throws IOException {
		assertThat(this.cloner.cloneRepository()).isEqualTo(0);
	}

}
