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
