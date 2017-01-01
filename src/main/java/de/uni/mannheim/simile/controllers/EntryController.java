package de.uni.mannheim.simile.controllers;

import de.uni.mannheim.simile.model.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class EntryController {

	@RequestMapping(name = "/repository", method = RequestMethod.POST)
	public ResponseEntity<?> setupRepository(@RequestParam(name = "repo") String repo,
																				@RequestParam(name = "branch", defaultValue = "master", required = false) String branch,
																				@RequestParam(name = "email") String email) {
		if(validateRequest(repo, email).isPresent()) {
			return ResponseEntity.badRequest().body(validateRequest(repo, email).get());
		} else {
			return ResponseEntity.ok(new Message("Repository set successfully", 200));
		}
	}

	private Optional<Message> validateRequest(String repo, String email) {
		StringBuilder str = new StringBuilder();
		if(repo.isEmpty()) str.append("Repo is required");
		if(email.isEmpty()) str.append("email is required");
		if(!str.toString().isEmpty()) {
			return Optional.ofNullable(new Message(str.toString(), 400));
		}
		return Optional.empty();
	}

}