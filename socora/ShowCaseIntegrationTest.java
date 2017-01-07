package com.merobase.socora.engine.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merobase.socora.engine.index.repository.candidate.CandidateDocument;
import com.merobase.socora.engine.index.repository.candidate.CandidateListResult;
import com.merobase.socora.engine.search.filter.Filters;
import com.merobase.socora.engine.search.ranking.Rankings;

/**
 * SOCORA Client show case.
 * 
 * @author Marcus Kessel
 *
 * @see CandidateSearchClient
 */
public class ShowCaseIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(ShowCaseIntegrationTest.class);

    private String baseURI = "http://socora.merobase.com/socora";

    private String user = "socora";
    private String pass = "d3fudj983r223dhs23";

    // --
    private String MY_JOB_ID = "0508e01e-27f4-4488-8b55-4a7d01832e5d";

    @Test
    public void testGetJobStatus() throws IOException {
        // create client
        CandidateSearchClient candidateSearchClient = new CandidateSearchClient(baseURI, auth(user, pass));

        // check
        JobStatus jobStatus = candidateSearchClient.getJobStatus(MY_JOB_ID);

        LOG.debug("Status : " + ToStringBuilder.reflectionToString(jobStatus));
    }

    /**
     * Textual search
     * 
     * @throws IOException
     * 
     * @see <a href=
     *      "http://swtvm9.informatik.uni-mannheim.de:8080/socora/docs/api-guide.html">API
     *      Guide</a>
     */
    @Test
    public void testTextualSearch() throws IOException {
        // create client
        CandidateSearchClient candidateSearchClient = new CandidateSearchClient(baseURI, auth(user, pass));

        // search request
        CandidateSearchRequest request = new CandidateSearchRequest();

        //
        int maxResults = 10;

        // can be one of: JUnit test class, MQL interface query, keyword query
        String testClassSourceQuery = "Base64(encode(byte[]):byte[];)";

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
        result.getCandidates().stream().sorted(new SortByRank(queryParams.getRankingStrategy(), true)).forEach(doc -> {
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
     * Test-Driven Search
     * 
     * @throws IOException
     * @throws InterruptedException
     * 
     * @see <a href=
     *      "http://swtvm9.informatik.uni-mannheim.de:8080/socora/docs/api-guide.html">API
     *      Guide</a>
     */
    @Test
    public void testTestDrivenSearch() throws IOException, InterruptedException {
        // create client
        CandidateSearchClient candidateSearchClient = new CandidateSearchClient(baseURI, auth(user, pass));

        // search request
        CandidateSearchRequest request = new CandidateSearchRequest();

        // maximum no. of candidates to retrieve
        int maxResults = 10;

        //
        String testClassSourceQuery = "TODO JUNIT TEST CLASS";

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

    /**
     * TDS search scenario (show existing results).
     * 
     * Base64 Example
     */
    @Test
    public void testShowResults() throws InterruptedException, IOException {
        // create client
        CandidateSearchClient candidateSearchClient = new CandidateSearchClient(baseURI, auth(user, pass));

        // successful tds candidates
        QueryParams queryParams = new QueryParams();
        queryParams.setRows(2000);

        // inclusions
        queryParams.setArtifactInformation(true/* maven metadata */);
        queryParams.setContent(true/* source code */);

        // FILTERS
        queryParams.setFilters(Arrays.asList(Filters.HASH_CODE_CLONE_FILTER, Filters.NO_ABSTRACT_CLASS_FILTER,
                Filters.NO_INTERFACE_FILTER, Filters.LATEST_VERSION_FILTER, Filters.FUNCTIONAL_SUFFICIENCY_FILTER));

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

        queryParams.setRankingCriteria(Arrays.asList(fsCriterion, performanceCriterion, lcomCriterion,
                couplingCriterion/*
                                  * , calledUncalledMethodRatioJavaCriterion
                                  */));

        String jobId = MY_JOB_ID;

        // get TDS results
        CandidateSearchResponse response = candidateSearchClient.getResults(jobId, queryParams);

        CandidateListResult result = response.getCandidates();

        // all rows
        result.getCandidates().stream().sorted(new SortByRank(queryParams.getRankingStrategy(), true)).forEach(doc -> {
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
     * @return rank for given {@link CandidateItem} based on passed
     *         CandidateRankingStrategy
     */
    public static int getRank(Map<String, Double> ranking, String candidateRankingStrategy, boolean partialOrder) {
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
}
