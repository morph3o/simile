/*
 * Copyright (c) 2017, Chair of Software Technology
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * •	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 * •	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 * •	Neither the name of the University Mannheim nor the names of its
 * 	contributors may be used to endorse or promote products derived from
 * 	this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package de.unimannheim.informatik.swt.simile.services;

import org.junit.Before;
import org.junit.Test;

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
