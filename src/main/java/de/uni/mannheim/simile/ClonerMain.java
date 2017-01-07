package de.uni.mannheim.simile;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.Strings;
import com.merobase.socora.engine.index.repository.candidate.CandidateDocument;
import com.merobase.socora.engine.index.repository.candidate.CandidateListResult;
import com.merobase.socora.engine.search.*;
import com.merobase.socora.engine.search.filter.Filters;
import com.merobase.socora.engine.search.ranking.Rankings;
import de.uni.mannheim.simile.services.Cloner;
import de.uni.mannheim.simile.services.DirectoryExplorer;
import de.uni.mannheim.simile.services.JavaClassHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClonerMain {

	private static final Logger LOG = LoggerFactory.getLogger(ClonerMain.class);

	private static final String REPO = "https://github.com/morph3o/simile.git";
	private static final String BRANCH = "master";
	private static final String FOLDER = "tmp";

	private static String baseURI = "http://socora.merobase.com/socora";

	private static String user = "socora";
	private static String pass = "d3fudj983r223dhs23";

	// --
	private static String MY_JOB_ID = "0508e01e-27f4-4488-8b55-4a7d01832e5d";

	static final ArrayList<String> methods = new ArrayList<String>();

	public static void listMethods(File projectDir) {
		new DirectoryExplorer(new JavaClassHandler(), (level, path, file) -> {
			System.out.println(path);
			System.out.println(Strings.repeat("=", path.length()));
			try {
				new VoidVisitorAdapter<Object>() {
					@Override
					public void visit(MethodDeclaration n, Object arg) {
						super.visit(n, arg);
						System.out.println(String.format("L[%s] - %s", n.getBegin().get(), n.getDeclarationAsString()));
						methods.add(n.getDeclarationAsString());
					}
				}.visit(JavaParser.parse(file), null);
				System.out.println(); // empty line
			} catch (IOException e) {
				new RuntimeException(e);
			}
			return true;
		}).explore(projectDir);
	}

	public static void searchComponent(String method) throws IOException {
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

	public static void main(String[] args) throws IOException {
		Cloner cloner = new Cloner(REPO, BRANCH, FOLDER);

		cloner.cloneRepository();

		File projectDir = new File(String.format("%s/src", FOLDER));
		listMethods(projectDir);

		searchComponent(methods.get(0));
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
