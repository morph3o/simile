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

import com.google.common.base.Strings;
import com.merobase.socora.engine.index.repository.candidate.CandidateDocument;
import com.merobase.socora.engine.index.repository.candidate.CandidateListResult;
import com.merobase.socora.engine.search.*;
import com.merobase.socora.engine.search.filter.Filters;
import com.merobase.socora.engine.search.ranking.Rankings;
import com.sparkpost.exception.SparkPostException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
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

	private static final Logger LOG = LoggerFactory.getLogger(SocoraRequester.class);

	private static String baseURI = "http://socora.merobase.com/socora";

	private static String user = "socora";
	private static String pass = "d3fudj983r223dhs23";

	@Autowired
	private EmailSender emailSender;

	public void searchComponent(String query, String searchType) throws IOException, InterruptedException, SparkPostException {
		Validate.notBlank(query, "Query parameter is required and cannot be blank");
		if (StringUtils.compare(searchType, INTERFACE_DRIVEN_SEARCH) == 0 ||
			StringUtils.compare(searchType, KEYWORD_SEARCH) == 0 ||
			StringUtils.isBlank(searchType)) {
			this.textualSearchComponent(query);
		} else if (StringUtils.compare(searchType, TEST_DRIVEN_SEARCH) == 0) {
			this.testDrivenSearchComponent(query);
		} else {
			this.textualSearchComponent(query);
		}
	}

	private void testDrivenSearchComponent(String testClass) throws IOException, InterruptedException {
		// create client
		CandidateSearchClient candidateSearchClient = new CandidateSearchClient(baseURI, auth(user, pass));

		// search request
		CandidateSearchRequest request = new CandidateSearchRequest();

		// maximum no. of candidates to retrieve
		int maxResults = 10;

		//
		String testClassSourceQuery = testClass;

		request.setInput(testClassSourceQuery);
		QueryParams queryParams = new QueryParams();
		queryParams.setRows(maxResults);

		// inclusions
		queryParams.setArtifactInformation(true/* maven metadata */);
		queryParams.setContent(true/* source code */);

		// FILTERS
		queryParams.setFilters(Arrays.asList(Filters.HASH_CODE_CLONE_FILTER, Filters.NO_ABSTRACT_CLASS_FILTER,
			Filters.NO_INTERFACE_FILTER, Filters.LATEST_VERSION_FILTER, Filters.FUNCTIONAL_SUFFICIENCY_FILTER));
		request.setQueryParams(queryParams);
		// MUST be set to true for test-driven search
		request.setTestDrivenSearch(Boolean.TRUE);

		// set ranking
		queryParams.setRankingStrategy(Rankings.SINGLE_OBJECTIVE);

		// set ranking criteria
		List<RankingCriterion> rankingCriteria = Arrays.asList(
			// textual score (Lucene/SolR)
			RankingCriterionBuilder.rankingCriterion().withName("luceneScore").withObjective(RankingCriterion.MAX)
				.build());

		queryParams.setRankingCriteria(rankingCriteria);

		CandidateSearchResponse response = candidateSearchClient.search(request);

		// response, here the returned jobId of the TDS job is important!
		String jobId = response.getJobId();

		// now TDS is running, poll job status, wait until finished (NOTE: do
		// something useful here)
		JobStatus jobStatus = null;
		while (jobStatus == null || jobStatus.getStatusType() == JobStatusType.RUNNING) {
			//
			jobStatus = candidateSearchClient.getJobStatus(jobId);

			// sleep
			Thread.sleep(30 * 1000L);
		}
	}

	private void textualSearchComponent(String method) throws IOException, SparkPostException {
		// create client
		CandidateSearchClient candidateSearchClient = new CandidateSearchClient(baseURI, auth(user, pass));

		// search request
		CandidateSearchRequest request = new CandidateSearchRequest();

		//
		int maxResults = 10;

		// can be one of: JUnit test class, MQL interface query, keyword query
		String testClassSourceQuery = method;

		request.setInput(testClassSourceQuery);
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
			RankingCriterionBuilder.rankingCriterion().withName("luceneScore").withObjective(RankingCriterion.MAX)
				.build());

		queryParams.setRankingCriteria(rankingCriteria);

		CandidateSearchResponse response = candidateSearchClient.search(request);

		CandidateListResult result = response.getCandidates();

		StringBuffer strBuilder = new StringBuffer();
		strBuilder.append("Result from SOCORA \n");
		strBuilder.append(Strings.repeat("=", "Result from SOCORA".length()));
		strBuilder.append("\n");

		// all rows
		result.getCandidates().stream().sorted(new SocoraRequester.SortByRank(queryParams.getRankingStrategy(), true)).forEach(doc -> {
			LOG.debug("Rank " + getRank(doc.getRanking(), queryParams.getRankingStrategy(), false) + "/"
				+ getRank(doc.getRanking(), queryParams.getRankingStrategy(), true) + ": " + doc.getFQName() + " "
				+ doc.getUri() + ". Safe ranking criteria ? "
				+ doc.getSafeRankingCriteria().get(queryParams.getRankingStrategy()) + " . Metrics: "
				+ prettify(doc.getMetrics(), queryParams.getRankingCriteria()) + ". Artifact Info = "
				+ ToStringBuilder.reflectionToString(doc.getArtifactInfo()));
			strBuilder.append(String.format("- %s \n", ToStringBuilder.reflectionToString(doc.getArtifactInfo())));
		});
		strBuilder.append(Strings.repeat("=", "Result from SOCORA".length()));
		LOG.debug("Total size " + result.getTotal());
		String[] recipients = {"pdivasto@gmail.com"};

		emailSender.sendEmail(recipients, strBuilder.toString());
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
