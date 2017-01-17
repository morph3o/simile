package de.uni.mannheim.simile.services;

import com.merobase.socora.engine.index.repository.candidate.CandidateDocument;
import com.merobase.socora.engine.index.repository.candidate.CandidateListResult;
import com.merobase.socora.engine.search.*;
import com.merobase.socora.engine.search.filter.Filters;
import com.merobase.socora.engine.search.ranking.Rankings;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SocoraRequester {
	public static String TEST_DRIVEN_SEARCH = "TEST_DRIVEN_SEARCH";
	public static String INTERFACE_DRIVEN_SEARCH = "INTERFACE_DRIVEN_SEARCH";
	public static String KEYWORD_SEARCH = "KEYWORD_SEARCH";

	private static final Logger LOG = LoggerFactory.getLogger(SocoraRequester.class);

	private static String baseURI = "http://socora.merobase.com/socora";

	private static String user = "socora";
	private static String pass = "d3fudj983r223dhs23";

	public void searchComponent(String query, String searchType) throws IOException, InterruptedException {
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

	private void textualSearchComponent(String method) throws IOException {
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

		// all rows
		result.getCandidates().stream().sorted(new SocoraRequester.SortByRank(queryParams.getRankingStrategy(), true)).forEach(doc -> {
			LOG.debug("Rank " + getRank(doc.getRanking(), queryParams.getRankingStrategy(), false) + "/"
				+ getRank(doc.getRanking(), queryParams.getRankingStrategy(), true) + ": " + doc.getFQName() + " "
				+ doc.getUri() + ". Safe ranking criteria ? "
				+ doc.getSafeRankingCriteria().get(queryParams.getRankingStrategy()) + " . Metrics: "
				+ prettify(doc.getMetrics(), queryParams.getRankingCriteria()) + ". Artifact Info = "
				+ ToStringBuilder.reflectionToString(doc.getArtifactInfo()));
		});

		LOG.debug("Total size " + result.getTotal());
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
