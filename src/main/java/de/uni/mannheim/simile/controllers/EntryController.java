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