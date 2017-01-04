package de.uni.mannheim.simile.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.Charset;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EntryControllerTests {

	public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(
																														MediaType.APPLICATION_JSON.getType(),
																														MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Before
	public void setup(){
		this.mockMvc = webAppContextSetup(webApplicationContext).build();
	}

	@Test
	public void itShouldSetRepositoryWithRepoAndBranch() throws Exception {
		this.mockMvc.perform(post("/repository")
														.param("repo", "repo test")
														.param("branch", "branch test")
														.param("email", "email test")
														.contentType(APPLICATION_JSON_UTF8))
			.andExpect(status().isOk());
	}

	@Test
	public void itShouldReturnErrorForRequiredParameters() throws Exception {
		this.mockMvc.perform(post("/repository"))
			.andExpect(status().is4xxClientError());
	}

	@Test
	public void itShouldReturnErrorForEmptyEmail() throws Exception {
		this.mockMvc.perform(post("/repository")
			.param("repo", "")
			.param("branch", "branch test")
			.param("email", "email test")
			.contentType(APPLICATION_JSON_UTF8))
			.andExpect(status().is4xxClientError());
	}

	@Test
	public void itShouldReturnErrorForEmptyRepo() throws Exception {
		this.mockMvc.perform(post("/repository")
			.param("repo", "repo test")
			.param("branch", "branch test")
			.param("email", "")
			.contentType(APPLICATION_JSON_UTF8))
			.andExpect(status().is4xxClientError());
	}

}
