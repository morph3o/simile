package de.uni.mannheim.simile.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.Charset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.hamcrest.core.StringContains.containsString;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EntryControllerTests {

	private static final Logger LOG = LoggerFactory.getLogger(EntryControllerTests.class);

	public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
		MediaType.APPLICATION_JSON.getType(),
		MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

	private static final String VALID_GIT_URL = "https://github.com/morph3o/simile.git";
	private static final String INVALID_GIT_URL = "invalid github url";

	private static final String VALID_EMAIL = "pdivasto@gmail.com";
	private static final String INVALID_EMAIL = "invalid email";

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Before
	public void setup() {
		this.mockMvc = webAppContextSetup(webApplicationContext).build();
	}

	@Test
	public void itShouldSetRepositoryWithRepoAndBranch() throws Exception {
		this.mockMvc.perform(post("/repository")
			.param("repo", VALID_GIT_URL)
			.param("branch", "branch test")
			.param("email", VALID_EMAIL)
			.contentType(APPLICATION_JSON_UTF8))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("\"Repository set successfully")));
	}

	@Test
	public void itShouldReturnErrorForRequiredParameters() throws Exception {
		this.mockMvc.perform(post("/repository"))
			.andExpect(status().is4xxClientError());
	}

	@Test
	public void itShouldReturnErrorForEmptyRepo() throws Exception {
		this.mockMvc.perform(post("/repository")
			.param("repo", "")
			.param("branch", "branch test")
			.param("email", VALID_EMAIL)
			.contentType(APPLICATION_JSON_UTF8))
			.andExpect(status().is4xxClientError());
	}

	@Test
	public void itShouldReturnErrorForEmptyEmail() throws Exception {
		this.mockMvc.perform(post("/repository")
			.param("repo", VALID_GIT_URL)
			.param("branch", "branch test")
			.param("email", "")
			.contentType(APPLICATION_JSON_UTF8))
			.andExpect(status().is4xxClientError());
	}

	@Test
	public void itShouldReturnErrorForInvalidRepo() throws Exception {
		this.mockMvc.perform(post("/repository")
			.param("repo", INVALID_GIT_URL)
			.param("branch", "branch test")
			.param("email", VALID_EMAIL)
			.contentType(APPLICATION_JSON_UTF8))
			.andExpect(status().is4xxClientError());
	}

	@Test
	public void itShouldReturnErrorForInvalidEmail() throws Exception {
		this.mockMvc.perform(post("/repository")
			.param("repo", VALID_GIT_URL)
			.param("branch", "branch test")
			.param("email", INVALID_EMAIL)
			.contentType(APPLICATION_JSON_UTF8))
			.andExpect(status().is4xxClientError());
	}

}
