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
import org.apache.commons.lang3.text.StrBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

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

		// set ranking criteria
		queryParams.setRankingStrategy(Rankings.HYBRID_NON_DOMINATED_SORTING);
		RankingCriterion fsCriterion = new RankingCriterion();
		// FS dynamic
		fsCriterion.setName("sf_instruction_leanness");
		fsCriterion.setObjective(RankingCriterion.MAX);
		// performance criterion
		RankingCriterion performanceCriterion = new RankingCriterion();
		performanceCriterion.setName("jmh_thrpt_score_mean");
		performanceCriterion.setObjective(RankingCriterion.MAX);
		// LCOM
		RankingCriterion lcomCriterion = new RankingCriterion();
		lcomCriterion.setName("entryClass_ckjm_ext_lcom3");
		lcomCriterion.setObjective(RankingCriterion.MIN);
		// Coupling
		RankingCriterion couplingCriterion = new RankingCriterion();
		couplingCriterion.setName("entryClass_ckjm_ext_ce");
		couplingCriterion.setObjective(RankingCriterion.MIN);

		queryParams.setRankingCriteria(Arrays.asList(
			fsCriterion,
			performanceCriterion,
			lcomCriterion,
			couplingCriterion
		));

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
			getTestDrivenSearchResult(jobId, recipient);
	}

	/**
	 * In charge of fetching the result of a job with given id, handle it and send the result to the recipient.
	 *
	 * @param jobId of the job to fetch the result.
	 * @param recipient (email) to where we will send the result.
	 * */
	private void getTestDrivenSearchResult(String jobId, String recipient) throws IOException {
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

		// set ranking criteria
		queryParams.setRankingStrategy(Rankings.HYBRID_NON_DOMINATED_SORTING);
		RankingCriterion fsCriterion = new RankingCriterion();
		// FS dynamic
		fsCriterion.setName("sf_instruction_leanness");
		fsCriterion.setObjective(RankingCriterion.MAX);
		// performance criterion
		RankingCriterion performanceCriterion = new RankingCriterion();
		performanceCriterion.setName("jmh_thrpt_score_mean");
		performanceCriterion.setObjective(RankingCriterion.MAX);
		// LCOM
		RankingCriterion lcomCriterion = new RankingCriterion();
		lcomCriterion.setName("entryClass_ckjm_ext_lcom3");
		lcomCriterion.setObjective(RankingCriterion.MIN);
		// Coupling
		RankingCriterion couplingCriterion = new RankingCriterion();
		couplingCriterion.setName("entryClass_ckjm_ext_ce");
		couplingCriterion.setObjective(RankingCriterion.MIN);

		queryParams.setRankingCriteria(Arrays.asList(
			fsCriterion,
			performanceCriterion,
			lcomCriterion,
			couplingCriterion
		));

		// Getting Test-driven result.
		CandidateSearchResponse response = candidateSearchClient.getResults(jobId, queryParams);

		CandidateListResult result = response.getCandidates();

		this.sendResult(
			this.handleTestDrivenSearchResult(result, queryParams, jobId),
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
		queryParams.setFilters(Arrays.asList(
			Filters.HASH_CODE_CLONE_FILTER,
			Filters.NO_ABSTRACT_CLASS_FILTER,
			Filters.NO_INTERFACE_FILTER,
			Filters.LATEST_VERSION_FILTER,
			Filters.FUNCTIONAL_SUFFICIENCY_FILTER
		));

		request.setQueryParams(queryParams);
		// no test-driven search
		request.setTestDrivenSearch(Boolean.FALSE);

		// set ranking
		queryParams.setRankingStrategy(Rankings.SINGLE_OBJECTIVE);

		// set ranking criteria
		List<RankingCriterion> rankingCriteria = Collections.singletonList(
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
			this.handleTextualSearchResult(result, queryParams),
			recipient,
			"Textual search"
		);
	}

	/**
	 * Handles the result of a test-driven search by transforming it into human-readable format.
	 *
	 * @param result to be transformed into human-readable format.
	 * @param queryParams to show more data.
	 *
	 * @return result in a human-readable format.
	 * */
	private String handleTestDrivenSearchResult(CandidateListResult result, QueryParams queryParams, String jobId) {
		StrBuilder strBuilder = new StrBuilder();
		strBuilder.appendln("Similar components - Test-driven search result");
		strBuilder.appendln(Strings.repeat("=", "Similar components - Test-driven search result".length()));

		// If the searchType is test-driven, we add a link to SOCORA with jobId
		strBuilder.appendNewLine();
		strBuilder.appendln("In the following link you can visualize the result in SOCORA:");
		strBuilder.appendln(String.format("\thttp://socora.merobase.com/socora/app/index.html#?result_hash=%s", jobId));
		strBuilder.appendNewLine();

		this.extractTestDrivenSearchCandidates(result, queryParams, strBuilder);

		strBuilder.appendln(Strings.repeat("=", "Similar components - Test-driven search result".length()));

		return strBuilder.toString();
	}

	/**
	 * Handles the result of a textual search by transforming it into human-readable format.
	 *
	 * @param result to be transformed into human-readable format.
	 * @param queryParams to show more data.
	 *
	 * @return result in a human-readable format.
	 * */
	private String handleTextualSearchResult(CandidateListResult result, QueryParams queryParams) {
		StrBuilder strBuilder = new StrBuilder();
		strBuilder.appendln("Similar components - Textual search result");
		strBuilder.appendln(Strings.repeat("=", "Similar components - Textual search result".length()));

		this.extractTextSearchCandidates(result, queryParams, strBuilder);

		strBuilder.appendln(Strings.repeat("=", "Similar components - Textual search result".length()));

		return strBuilder.toString();
	}

	/**
	 * Transforms result into a human-readable format for test-driven search result.
	 * */
	private void extractTestDrivenSearchCandidates(CandidateListResult result, QueryParams queryParams, StrBuilder strBuilder) {
		result.getCandidates().forEach(doc -> {
			strBuilder.appendln(String.format("Component: %s", doc.getArtifactInfo().getName()));
			strBuilder.appendln(String.format("\tFQ Name: %s", doc.getFQName()));
			strBuilder.appendln("\tMetrics:");
			strBuilder.append(String.format("\t\t%s", prettify(doc.getMetrics(), queryParams.getRankingCriteria())));
			strBuilder.appendln("\tDetails:");
			strBuilder.appendln(String.format("\t%s", doc.getArtifactInfo().getDescription()));
			strBuilder.appendln(String.format("\tRepository: %s", doc.getArtifactInfo().getRepository()));
			strBuilder.appendln(String.format("\tVersion: %s", doc.getVersion()));
			strBuilder.appendNewLine();
		});
	}

	/**
	 * Transforms result into a human-readable format for textual search result.
	 * */
	private void extractTextSearchCandidates(CandidateListResult result, QueryParams queryParams, StrBuilder strBuilder) {
		result.getCandidates().stream().sorted(new SortByRank(queryParams.getRankingStrategy(), true)).forEach(doc -> {
			strBuilder.appendln(String.format("Component: %s", doc.getArtifactInfo().getName()));
			strBuilder.appendln(String.format("\tFQ Name: %s", doc.getFQName()));
			strBuilder.appendln(String.format("\tRank: %s/%s", getRank(doc.getRanking(), queryParams.getRankingStrategy(), false), getRank(doc.getRanking(), queryParams.getRankingStrategy(), true)));
			strBuilder.appendln("\tMetrics:");
			strBuilder.append(String.format("%s", prettify(doc.getMetrics(), queryParams.getRankingCriteria())));
			strBuilder.appendln("\tDetails:");
			strBuilder.appendln(String.format("\t%s", doc.getArtifactInfo().getDescription()));
			strBuilder.appendln(String.format("\tRepository: %s", doc.getArtifactInfo().getRepository()));
			strBuilder.appendln(String.format("\tVersion: %s", doc.getVersion()));
			strBuilder.appendNewLine();
		});
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
		StrBuilder sb = new StrBuilder();
		for (String key : metrics.keySet()) {
			for (RankingCriterion criterion : criteria) {
				if (StringUtils.equals(criterion.getName(), key)) {
					sb.appendln(String.format("\t\t%s = %s", key, metrics.get(key)));
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
