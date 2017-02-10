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

import com.google.common.base.Strings;
import com.merobase.socora.engine.index.repository.candidate.CandidateDocument;
import com.merobase.socora.engine.index.repository.candidate.CandidateListResult;
import com.merobase.socora.engine.search.*;
import com.merobase.socora.engine.search.filter.Filters;
import com.merobase.socora.engine.search.ranking.Rankings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.text.StrBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SocoraRequester {
	public static String TEST_DRIVEN_SEARCH = "TEST_DRIVEN_SEARCH";
	public static String INTERFACE_DRIVEN_SEARCH = "INTERFACE_DRIVEN_SEARCH";
	public static String KEYWORD_SEARCH = "KEYWORD_SEARCH";

	private static final Logger logger = LoggerFactory.getLogger(SocoraRequester.class);

	private static String baseURI = "http://socora.merobase.com/socora";

	private static String user = "socora";
	private static String pass = "d3fudj983r223dhs23";

	private final EmailSender emailSender;

	@Autowired
	public SocoraRequester(EmailSender emailSender) {
		this.emailSender = emailSender;
	}

	public void searchComponent(String query, String searchType, String recipient) throws IOException, InterruptedException {
		Validate.notBlank(query, "Query parameter is required and cannot be blank");
		if (StringUtils.compare(searchType, INTERFACE_DRIVEN_SEARCH) == 0 ||
			StringUtils.compare(searchType, KEYWORD_SEARCH) == 0 ||
			StringUtils.isBlank(searchType)) {
			this.textualSearchComponent(query, recipient);
		} else if (StringUtils.compare(searchType, TEST_DRIVEN_SEARCH) == 0) {
			this.testDrivenSearchComponent(query, recipient);
		} else {
			this.textualSearchComponent(query, recipient);
		}
	}

	/**
	 * In charge of sending the test class to SOCORA to search for components.
	 *
	 * @param testClassSourceQuery which is sent to SOCORA to search for components.
	 * @param recipient (email) where the result will be sent.
	 * */
	private void testDrivenSearchComponent(String testClassSourceQuery, String recipient) throws IOException, InterruptedException {
		// Create client
		CandidateSearchClient candidateSearchClient = new CandidateSearchClient(baseURI, auth(user, pass));

		// Search request
		CandidateSearchRequest request = new CandidateSearchRequest();

		// Maximum no. of candidates to retrieve
		int maxResults = 400;

		request.setInput(testClassSourceQuery);
		QueryParams queryParams = new QueryParams();
		queryParams.setRows(maxResults);

		// inclusions
		queryParams.setArtifactInformation(true);
		queryParams.setContent(true);

		// FILTERS
		queryParams.setFilters(Arrays.asList(
			Filters.HASH_CODE_CLONE_FILTER,
			Filters.NO_ABSTRACT_CLASS_FILTER,
			Filters.NO_INTERFACE_FILTER,
			Filters.LATEST_VERSION_FILTER,
			Filters.FUNCTIONAL_SUFFICIENCY_FILTER
		));

		request.setQueryParams(queryParams);

		request.setTestDrivenSearch(Boolean.TRUE);

		// Set ranking
		queryParams.setRankingStrategy(Rankings.SINGLE_OBJECTIVE);

		// Set ranking criteria
		List<RankingCriterion> rankingCriteria = Arrays.asList(
			RankingCriterionBuilder
				.rankingCriterion()
				.withName("luceneScore")
				.withObjective(RankingCriterion.MAX)
				.build()
		);

		queryParams.setRankingCriteria(rankingCriteria);

		CandidateSearchResponse response = candidateSearchClient.search(request);

		// JobId of the test-driven search, which is used to check the status of the job.
		String jobId = response.getJobId();

		// In order to retrieve the result, we need to check the status.
		JobStatus jobStatus = null;
		while (jobStatus == null || jobStatus.getStatusType() != JobStatusType.FINISHED) {
			jobStatus = candidateSearchClient.getJobStatus(jobId);
			logger.info("Status of jobId " + jobId + " is " + jobStatus.getStatusType().name());
			Thread.sleep(30 * 1000L);
		}

		// Once the search is FINISHED, we retrieve the result with the candidates.
		if(jobStatus.getStatusType() == JobStatusType.FINISHED)
			getTextualSearchResult(jobId, recipient);
	}

	/**
	 * In charge of fetching the result of a job with given id, handle it and send the result to the recipient.
	 *
	 * @param jobId of the job to fetch the result.
	 * @param recipient (email) to where we will send the result.
	 * */
	private void getTextualSearchResult(String jobId, String recipient) throws IOException {
		// Create client
		CandidateSearchClient candidateSearchClient = new CandidateSearchClient(baseURI, auth(user, pass));

		// Successful tds candidates
		QueryParams queryParams = new QueryParams();
		queryParams.setRows(2000);

		// Inclusions
		queryParams.setArtifactInformation(true);
		queryParams.setContent(true);

		// FILTERS
		queryParams.setFilters(Arrays.asList(
			Filters.HASH_CODE_CLONE_FILTER,
			Filters.NO_ABSTRACT_CLASS_FILTER,
			Filters.NO_INTERFACE_FILTER,
			Filters.LATEST_VERSION_FILTER,
			Filters.FUNCTIONAL_SUFFICIENCY_FILTER
		));

		// Set ranking criteria
		queryParams.setRankingStrategy(Rankings.SINGLE_OBJECTIVE);

		// Set ranking criteria
		List<RankingCriterion> rankingCriteria = Arrays.asList(
			RankingCriterionBuilder
				.rankingCriterion()
				.withName("luceneScore")
				.withObjective(RankingCriterion.MAX)
				.build()
		);

		queryParams.setRankingCriteria(rankingCriteria);

		// Getting Test-driven result.
		CandidateSearchResponse response = candidateSearchClient.getResults(jobId, queryParams);

		CandidateListResult result = response.getCandidates();

		result.getCandidates().stream().sorted(new SortByRank(queryParams.getRankingStrategy(), true)).forEach(doc -> {
			logger.info("Rank " + getRank(doc.getRanking(), queryParams.getRankingStrategy(), false) + "/"
				+ getRank(doc.getRanking(), queryParams.getRankingStrategy(), true) + ": " + doc.getFQName() + " "
				+ doc.getUri() + ". Safe ranking criteria ? "
				+ doc.getSafeRankingCriteria().get(queryParams.getRankingStrategy()) + " . Metrics: "
				+ prettify(doc.getMetrics(), queryParams.getRankingCriteria()) + ". Artifact Info = "
				+ ToStringBuilder.reflectionToString(doc.getArtifactInfo()));
		});

		logger.info("Total size " + result.getTotal());

		this.sendResult(
			this.handleSearchResult(result, queryParams, "Test-driven search"),
			recipient,
			"Test-driven search"
		);
	}

	/**
	 * In charge of making the request to SOCORA using textual search. The result is sent to the recipient.
	 *
	 * @param method in MQL format that will be sent to SOCORA.
	 * @param recipient (email) to which the result will be sent.
	 * */
	private void textualSearchComponent(String method, String recipient) throws IOException {
		// create client
		CandidateSearchClient candidateSearchClient = new CandidateSearchClient(baseURI, auth(user, pass));

		// search request
		CandidateSearchRequest request = new CandidateSearchRequest();

		int maxResults = 10;

		request.setInput(method);
		QueryParams queryParams = new QueryParams();
		queryParams.setRows(maxResults);

		// inclusions
		queryParams.setArtifactInformation(true);
		queryParams.setContent(true);

		// FILTERS
		queryParams.setFilters(Arrays.asList(Filters.HASH_CODE_CLONE_FILTER, Filters.NO_ABSTRACT_CLASS_FILTER,
			Filters.NO_INTERFACE_FILTER, Filters.LATEST_VERSION_FILTER, Filters.FUNCTIONAL_SUFFICIENCY_FILTER));
		request.setQueryParams(queryParams);
		// no test-driven search
		request.setTestDrivenSearch(Boolean.FALSE);

		// set ranking
		queryParams.setRankingStrategy(Rankings.SINGLE_OBJECTIVE);

		// set ranking criteria
		List<RankingCriterion> rankingCriteria = Arrays.asList(
			// textual score (Lucene/SolR)
			RankingCriterionBuilder
				.rankingCriterion()
				.withName("luceneScore")
				.withObjective(RankingCriterion.MAX)
				.build()
		);

		queryParams.setRankingCriteria(rankingCriteria);

		CandidateSearchResponse response = candidateSearchClient.search(request);

		CandidateListResult result = response.getCandidates();

		this.sendResult(
			this.handleSearchResult(result, queryParams, "Textual search"),
			recipient,
			"Textual search"
		);
	}

	/**
	 * Handles the result transforming it to human-readable format.
	 *
	 * @param result to be transformed into human-readable format.
	 * @param queryParams to show more data.
	 *
	 * @return result in a human-readable format.
	 * */
	private String handleSearchResult(CandidateListResult result, QueryParams queryParams, String searchType) {
		StrBuilder strBuilder = new StrBuilder();
		strBuilder.appendln(String.format("Similar components - %s result", searchType));
		strBuilder.appendln(Strings.repeat("=", String.format("Similar components - %s result", searchType).length()));

		result.getCandidates().stream().sorted(new SortByRank(queryParams.getRankingStrategy(), true)).forEach(doc -> {
			strBuilder.appendln(String.format("Component: %s", doc.getArtifactInfo().getName()));
			strBuilder.appendln(String.format("\tFQ Name: %s", doc.getFQName()));
			strBuilder.appendln(String.format("\tRank: %s/%s", getRank(doc.getRanking(), queryParams.getRankingStrategy(), false), getRank(doc.getRanking(), queryParams.getRankingStrategy(), true)));
			strBuilder.appendln(String.format("\tMetrics:"));
			strBuilder.appendln(String.format("\t\t%s", prettify(doc.getMetrics(), queryParams.getRankingCriteria())));
			strBuilder.appendln(String.format("\tDetails:"));
			strBuilder.appendln(String.format("\t%s", doc.getArtifactInfo().getDescription()));
			strBuilder.appendln(String.format("\tRepository: %s", doc.getArtifactInfo().getRepository()));
			strBuilder.appendln(String.format("\tVersion: %s", doc.getVersion()));
		});

		strBuilder.appendln(Strings.repeat("=", String.format("Similar components - %s result", searchType).length()));

		return strBuilder.toString();
	}

	/**
	 * Sends the result of a search to the email.
	 *
	 * @param result which will be sent.
	 * @param email to which the result will be sent.
	 * */
	private void sendResult(String result, String email, String searchType) {
		emailSender.sendEmail(email, String.format("Feedback from Simile - %s", searchType), result);
	}

	/**
	 * Return rank for passed {@link Map} based on given
	 * CandidateRankingStrategy
	 *
	 * @param ranking
	 *            {@link Map} instance
	 * @param candidateRankingStrategy
	 *            CandidateRankingStrategy ID {@link Rankings}
	 * @param partialOrder
	 *            true if partial order should be returned (disables strict
	 *            order!). Candidates in non-distinguishable sets possess the
	 *            same ranks.
	 * @return rank for given {@link } based on passed
	 *         CandidateRankingStrategy
	 */
	private static int getRank(Map<String, Double> ranking, String candidateRankingStrategy, boolean partialOrder) {
		Validate.notBlank(candidateRankingStrategy, "CandidateRankingStrategy ID cannot be blank");

		String key = candidateRankingStrategy;
		if (partialOrder) {
			key += "_po";
		}

		return ranking.get(key).intValue();
	}

	private static Auth auth(String user, String pass) {
		Auth auth = new Auth();
		auth.setUser(user);
		auth.setPassword(pass);

		return auth;
	}

	private static String prettify(Map<String, Double> metrics, List<RankingCriterion> criteria) {
		StringBuffer sb = new StringBuffer();

		for (String key : metrics.keySet()) {
			for (RankingCriterion criterion : criteria) {
				if (StringUtils.equals(criterion.getName(), key)) {
					sb.append(" ");
					sb.append(key + " = " + metrics.get(key));
				}
			}
		}

		return sb.toString();
	}

	/**
	 * Sort {@link CandidateDocument} by rank.
	 *
	 * @author Marcus Kessel
	 *
	 */
	private static class SortByRank implements Comparator<CandidateDocument> {

		private final String candidateRankingStrategy;
		private final boolean partialOrder;

		private SortByRank(String candidateRankingStrategy, boolean partialOrder) {
			Validate.notBlank(candidateRankingStrategy);
			this.candidateRankingStrategy = candidateRankingStrategy;
			this.partialOrder = partialOrder;
		}

		@Override
		public int compare(CandidateDocument o1, CandidateDocument o2) {
			int rank1 = getRank(o1.getRanking(), candidateRankingStrategy, partialOrder);
			int rank2 = getRank(o2.getRanking(), candidateRankingStrategy, partialOrder);

			return Comparator.<Integer>naturalOrder().compare(rank1, rank2);
		}

	}

}
