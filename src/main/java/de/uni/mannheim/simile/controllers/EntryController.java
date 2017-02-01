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

package de.uni.mannheim.simile.controllers;

import com.sparkpost.exception.SparkPostException;
import cool.graph.cuid.Cuid;
import de.uni.mannheim.simile.Simile;
import de.uni.mannheim.simile.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
public class EntryController {

	private static final Logger LOG = LoggerFactory.getLogger(EntryController.class);

	@Autowired
	private Simile simile;

	@RequestMapping(name = "/repository", method = RequestMethod.POST)
	public ResponseEntity<?> setupRepository(@RequestParam(name = "repo") String repo,
																				@RequestParam(name = "branch", defaultValue = "master", required = false) String branch,
																				@RequestParam(name = "email") String email) throws IOException, InterruptedException, SparkPostException {
		if(validateRequest(repo, email).isPresent()) {
			LOG.debug("The request is not valid. Request -> repo: %s - branch: %s - email: %s", repo, branch, email);
			return ResponseEntity.badRequest().body(validateRequest(repo, email).get());
		} else {
			LOG.debug("The request is valid. Request -> repo: %s - branch: %s - email: %s", repo, branch, email);
			simile.searchForComponents(repo, branch, Cuid.createCuid());
			return ResponseEntity.ok(new Message("Repository set successfully", 200));
		}
	}

	private Optional<Message> validateRequest(String repo, String email) {
		LOG.debug("Validating required parameters in request");
		StringBuilder str = new StringBuilder();
		if(repo.isEmpty()) str.append("repo is required");
		if(email.isEmpty()) str.append("email is required");
		if(!this.isValidEmail(email)) str.append("email is not valid");
		if(!this.isValidGithubWebURL(repo)) str.append("repo must be a valid Git Web URL (e.g. https://github.com/...)");
		if(!str.toString().isEmpty()) {
			return Optional.ofNullable(new Message(str.toString(), 400));
		}
		return Optional.empty();
	}

	/**
	 * Validates if a Git repository url is a valid Git Web URL.
	 *
	 * @param gitRepo Git web URL to be validated.
	 * @return true if the git url is valid, otherwise false.
	 * */
	private boolean isValidGithubWebURL(String gitRepo) {
		final Pattern pattern = Pattern.compile("(http(s)?)(:(//)?)([\\w\\.@\\:/\\-~]+)(\\.git)?");
		return pattern.matcher(gitRepo).matches();
	}

	/**
	 * Validates if an email address is valid.
	 *
	 * @see <a href="http://www.regexr.com/3c0ol">http://www.regexr.com/3c0ol</a>
	 *
	 * @param email email address to be validated.
	 * @return true in case the email address is valid, otherwise false.
	 * */
	private boolean isValidEmail(String email) {
		final Pattern pattern = Pattern.compile("[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?");
		return pattern.matcher(email).matches();
	}

}