package ir.ac.ut.fml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.checkerframework.checker.nullness.qual.Nullable;

import br.usp.icmc.labes.mealyInference.Infer_LearnLib;
import br.usp.icmc.labes.mealyInference.utils.ExperimentAndLearner;
import br.usp.icmc.labes.mealyInference.utils.LearnLibProperties;
import br.usp.icmc.labes.mealyInference.utils.MyObservationTable;
import br.usp.icmc.labes.mealyInference.utils.OTUtils;
import br.usp.icmc.labes.mealyInference.utils.Utils;
import br.usp.icmc.labes.mealyInference.utils.EquivEQOracle.RandomWMethodHypEQOracle;
import br.usp.icmc.labes.mealyInference.utils.EquivEQOracle.WMethodHypEQOracle;
import br.usp.icmc.labes.mealyInference.utils.EquivEQOracle.WpMethodHypEQOracle;
import de.learnlib.algorithms.dlstar.mealy.ExtensibleDLStarMealy;
import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandler;
import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstar.closing.ClosingStrategies;
import de.learnlib.algorithms.lstar.closing.ClosingStrategy;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealy;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.api.SUL;
import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.oracle.EquivalenceOracle;
import de.learnlib.api.oracle.MembershipOracle;
import de.learnlib.api.oracle.MembershipOracle.MealyMembershipOracle;
import de.learnlib.api.statistic.StatisticSUL;
import de.learnlib.datastructure.observationtable.ObservationTable;
import de.learnlib.datastructure.observationtable.writer.ObservationTableASCIIWriter;
import de.learnlib.driver.util.MealySimulatorSUL;
import de.learnlib.filter.cache.sul.SULCache;
import de.learnlib.filter.statistic.sul.ResetCounterSUL;
import de.learnlib.filter.statistic.sul.SymbolCounterSUL;
import de.learnlib.oracle.equivalence.RandomWMethodEQOracle;
import de.learnlib.oracle.equivalence.RandomWordsEQOracle;
import de.learnlib.oracle.equivalence.WMethodEQOracle;
import de.learnlib.oracle.equivalence.WpMethodEQOracle;
import de.learnlib.oracle.equivalence.mealy.RandomWalkEQOracle;
import de.learnlib.oracle.membership.SULOracle;
import de.learnlib.util.ExperimentDebug.MealyExperiment;
import de.learnlib.util.statistics.SimpleProfiler;
import net.automatalib.automata.transducers.MealyMachine;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.commons.util.Pair;
import net.automatalib.serialization.InputModelDeserializer;
import net.automatalib.serialization.dot.DOTParsers;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.visualization.VisualizationHelper.EdgeAttrs;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import uk.le.ac.ffsm.FeaturedMealyUtils;

public class CompareLearningMethods {

	public static final String SUL1 = "sul1";
	public static final String SUL2 = "sul2";
	public static final String SOT = "sot";
	public static final String SOT2 = "sot2";
	public static final String HELP = "help";
	public static final String HELP_SHORT = "h";
	public static final String OT = "ot";
	public static final String CEXH = "cexh";
	public static final String CLOS = "clos";
	public static final String EQ = "eq";
	public static final String CACHE = "cache";
	public static final String SEED = "seed";
	public static final String OUT = "out";
	public static final String LEARN = "learn";
	public static final String INFO = "info";
	private static final String FM = "fm";

	public static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

	public static final String[] eqMethodsAvailable = { "rndWalk", "rndWords", "wp", "wphyp", "w", "whyp", "wrnd",
			"wrndhyp" };
	public static final String[] closingStrategiesAvailable = { "CloseFirst", "CloseShortest" };
	private static final String RIVEST_SCHAPIRE_ALLSUFFIXES = "RivestSchapireAllSuffixes";
	public static final String[] cexHandlersAvailable = { "ClassicLStar", "MalerPnueli", "RivestSchapire",
			RIVEST_SCHAPIRE_ALLSUFFIXES, "Shahbaz", "Suffix1by1" };
	public static final String[] learningMethodsAvailable = { "lstar", "l1", "adaptive", "dlstar_v2", "dlstar_v1",
			"dlstar_v0", "ttt" };

	public static final Function<Map<String, String>, Pair<@Nullable String, @Nullable Word<String>>> MEALY_EDGE_WORD_STR_PARSER = attr -> {
		final String label = attr.get(EdgeAttrs.LABEL);
		if (label == null) {
			return Pair.of(null, null);
		}

		final String[] tokens = label.split("/");

		if (tokens.length != 2) {
			return Pair.of(null, null);
		}

		Word<String> token2 = Word.epsilon();
		token2 = token2.append(tokens[1]);
		return Pair.of(tokens[0], token2);
	};

	// This program compares the learning process of SUL2 using L-star and using
	// dynamic learning (based on the SUL1 learned ot).

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// create the command line parser
		CommandLineParser parser = new BasicParser();
		// create the Options
		Options options = createOptions();
		// automatically generate the help statement
		HelpFormatter formatter = new HelpFormatter();

		long tstamp = System.currentTimeMillis();
		// random seed
		Random rnd_seed = new Random(tstamp);

		// timestamp
		Timestamp timestamp = new Timestamp(tstamp);

		int array_length = 5;

		int[] sul1_nonadaptive = new int[array_length];
		Arrays.fill(sul1_nonadaptive, 0);

		int[] sul2_nonadaptive = new int[array_length];
		Arrays.fill(sul2_nonadaptive, 0);

		int[] sul2_adaptive_v0 = new int[array_length];
		Arrays.fill(sul2_adaptive_v0, 0);

		int[] sul2_adaptive_v1 = new int[array_length];
		Arrays.fill(sul2_adaptive_v1, 0);

		int[] sul2_adaptive_v2 = new int[array_length];
		Arrays.fill(sul2_adaptive_v2, 0);

		try {

			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			if (line.hasOption(HELP)) {
				formatter.printHelp("Infer_LearnLib", options);
				System.exit(0);
			}

			if (!line.hasOption(SUL1) || !line.hasOption(SUL2)) {
				throw new IllegalArgumentException("must provide a SUL");
			}

			// set SUL path
			File sul_1 = new File(line.getOptionValue(SUL1));
			File sul_2 = new File(line.getOptionValue(SUL2));

			File obsTable = null;
			File out_dir = new File(line.getOptionValue(OUT));
			String learningAlgorithm = "lstar";

			// create log
			System.setProperty("logdir", out_dir.getAbsolutePath());
			LearnLogger logger_1 = LearnLogger.getLogger(Infer_LearnLib.class);

			System.out.println("\nSUL1_Nonadaptive method:");
			sul1_nonadaptive = LearnProductFSM(sul_1, obsTable, learningAlgorithm, out_dir, line, rnd_seed,
					sul1_nonadaptive, 1, logger_1);

			System.out.println("\nSUL2_Nonadaptive method:");
			sul2_nonadaptive = LearnProductFSM(sul_2, obsTable, learningAlgorithm, out_dir, line, rnd_seed,
					sul2_nonadaptive, 0, logger_1);

			String sul_1_string = sul_1.getName();
			String ot_file_name = "";
			if (sul_1_string.endsWith("txt")) {
				obsTable = new File(out_dir, sul_1_string.replaceAll("_text.txt", "_ot"));
			} else {
				obsTable = new File(out_dir, sul_1_string.replaceAll(".dot", "_ot"));
			}
//			System.out.println(obsTable);

			String learningAlgorithm_2 = "";

//			learningAlgorithm_2 = "dlstar_v0";
//			System.out.println("\nSUL2_Adaptive method:");
//			sul2_adaptive_v0 = LearnProductFSM(sul_2, obsTable, learningAlgorithm_2, out_dir, line, rnd_seed,
//					sul2_adaptive_v0, 0);
//			
//			learningAlgorithm_2 = "dlstar_v1";
//			System.out.println("\nSUL2_Adaptive method:");
//			sul2_adaptive_v1 = LearnProductFSM(sul_2, obsTable, learningAlgorithm_2, out_dir, line, rnd_seed,
//					sul2_adaptive_v1, 0);

			learningAlgorithm_2 = "dlstar_v2";
			System.out.println("\nSUL2_Adaptive method:");
			sul2_adaptive_v2 = LearnProductFSM(sul_2, obsTable, learningAlgorithm_2, out_dir, line, rnd_seed,
					sul2_adaptive_v2, 0, logger_1);

		}

		catch (Exception exp) {
			// automatically generate the help statement
			formatter.printHelp("Options:", options);
			System.err.println("Unexpected Exception");
			exp.printStackTrace();
		}

		System.out.println("Finished");

	}

	private static List<String> Intersection(List<String> list_1, List<String> list_2) {
		List<String> intersection_list = new ArrayList<>();
		for (String f : list_1) {
			if (list_2.contains(f)) {
				intersection_list.add(f);
			}
		}
		return intersection_list;
	}

	private static ClosingStrategy<Object, Object> getClosingStrategy(String optionValue) {
		if (optionValue != null) {
			if (optionValue.equals(ClosingStrategies.CLOSE_FIRST.toString())) {
				return ClosingStrategies.CLOSE_FIRST;
			} else if (optionValue.equals(ClosingStrategies.CLOSE_SHORTEST.toString())) {
				return ClosingStrategies.CLOSE_SHORTEST;
			}
		}
		return ClosingStrategies.CLOSE_FIRST;
	}

	private static ObservationTableCEXHandler<Object, Object> getCEXHandler(String optionValue) {
		if (optionValue != null) {
			if (optionValue.equals(ObservationTableCEXHandlers.RIVEST_SCHAPIRE.toString())) {
				return ObservationTableCEXHandlers.RIVEST_SCHAPIRE;

			} else if (optionValue.equals(RIVEST_SCHAPIRE_ALLSUFFIXES)) {
				return ObservationTableCEXHandlers.RIVEST_SCHAPIRE_ALLSUFFIXES;
			} else if (optionValue.equals(ObservationTableCEXHandlers.SUFFIX1BY1.toString())) {
				return ObservationTableCEXHandlers.SUFFIX1BY1;
			} else if (optionValue.equals(ObservationTableCEXHandlers.CLASSIC_LSTAR.toString())) {
				return ObservationTableCEXHandlers.CLASSIC_LSTAR;
			} else if (optionValue.equals(ObservationTableCEXHandlers.MALER_PNUELI.toString())) {
				return ObservationTableCEXHandlers.MALER_PNUELI;
			} else if (optionValue.equals(ObservationTableCEXHandlers.SHAHBAZ.toString())) {
				return ObservationTableCEXHandlers.SHAHBAZ;
			}
		}
		return ObservationTableCEXHandlers.RIVEST_SCHAPIRE;
	}

	private static EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> buildEqOracle(
			Random rnd_seed, CommandLine line, LearnLogger logger, CompactMealy<String, Word<String>> mealyss,
			SUL<String, Word<String>> eq_sul) {
		MembershipOracle<String, Word<Word<String>>> oracleForEQoracle = new SULOracle<>(eq_sul);

		EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle;
		if (!line.hasOption(EQ)) {
			logger.logEvent("EquivalenceOracle: WpMethodEQOracle(" + 2 + ")");
			return new WpMethodEQOracle<>(oracleForEQoracle, 2);
		}

		double restartProbability;
		int maxSteps, maxTests, maxLength, minLength, maxDepth, minimalSize, rndLength, bound;
		long rnd_long;
		boolean resetStepCount;

		LearnLibProperties learn_props = LearnLibProperties.getInstance();

		switch (line.getOptionValue(EQ)) {
		case "rndWalk":
			// create RandomWalkEQOracle
			restartProbability = learn_props.getRndWalk_restartProbability();
			maxSteps = learn_props.getRndWalk_maxSteps();
			resetStepCount = learn_props.getRndWalk_resetStepsCount();

			eqOracle = new RandomWalkEQOracle<String, Word<String>>(eq_sul, // sul
					restartProbability, // reset SUL w/ this probability before a step
					maxSteps, // max steps (overall)
					resetStepCount, // reset step count after counterexample
					rnd_seed // make results reproducible
			);
			logger.logEvent("EquivalenceOracle: RandomWalkEQOracle(" + restartProbability + "," + maxSteps + ","
					+ resetStepCount + ")");
			break;
		case "rndWords":
			// create RandomWordsEQOracle
			maxTests = learn_props.getRndWords_maxTests();
			maxLength = learn_props.getRndWords_maxLength();
			minLength = learn_props.getRndWords_minLength();
			rnd_long = rnd_seed.nextLong();
			rnd_seed.setSeed(rnd_long);

			eqOracle = new RandomWordsEQOracle<>(oracleForEQoracle, minLength, maxLength, maxTests, rnd_seed);
			logger.logEvent("EquivalenceOracle: RandomWordsEQOracle(" + minLength + ", " + maxLength + ", " + maxTests
					+ ", " + rnd_long + ")");
			break;
		case "wp":
			maxDepth = learn_props.getW_maxDepth();
			eqOracle = new WpMethodEQOracle<>(oracleForEQoracle, maxDepth);
			logger.logEvent("EquivalenceOracle: WpMethodEQOracle(" + maxDepth + ")");
			break;
		case "wphyp":
			maxDepth = learn_props.getW_maxDepth();
			eqOracle = new WpMethodHypEQOracle((MealyMembershipOracle) oracleForEQoracle, maxDepth, mealyss);
			logger.logEvent("EquivalenceOracle: WpMethodHypEQOracle(" + maxDepth + ")");
			break;
		case "w":
			maxDepth = learn_props.getW_maxDepth();
			eqOracle = new WMethodEQOracle<>(oracleForEQoracle, maxDepth);
			logger.logEvent("EquivalenceOracle: WMethodQsizeEQOracle(" + maxDepth + ")");
			break;
		case "whyp":
			maxDepth = learn_props.getW_maxDepth();
			eqOracle = new WMethodHypEQOracle((MealyMembershipOracle) oracleForEQoracle, maxDepth, mealyss);
			logger.logEvent("EquivalenceOracle: WMethodHypEQOracle(" + maxDepth + ")");
			break;
		case "wrnd":
			minimalSize = learn_props.getWhyp_minLen();
			rndLength = learn_props.getWhyp_rndLen();
			bound = learn_props.getWhyp_bound();
			rnd_long = rnd_seed.nextLong();
			rnd_seed.setSeed(rnd_long);

			eqOracle = new RandomWMethodEQOracle<>(oracleForEQoracle, minimalSize, rndLength, bound, rnd_seed, 1);
			logger.logEvent("EquivalenceOracle: RandomWMethodEQOracle(" + minimalSize + "," + rndLength + "," + bound
					+ "," + rnd_long + ")");
			break;
		case "wrndhyp":
			minimalSize = learn_props.getWhyp_minLen();
			rndLength = learn_props.getWhyp_rndLen();
			bound = learn_props.getWhyp_bound();
			rnd_long = rnd_seed.nextLong();
			rnd_seed.setSeed(rnd_long);

			eqOracle = new RandomWMethodHypEQOracle((MealyMembershipOracle) oracleForEQoracle, minimalSize, rndLength,
					bound, rnd_seed, 1, mealyss);
			logger.logEvent("EquivalenceOracle: RandomWMethodHypEQOracle(" + minimalSize + "," + rndLength + "," + bound
					+ "," + rnd_long + "," + 1 + ")");
			break;
		default:
			maxDepth = 2;
			eqOracle = new WMethodEQOracle<>(oracleForEQoracle, maxDepth);
			logger.logEvent("EquivalenceOracle: WMethodEQOracle(" + maxDepth + ")");
			break;
		}
		return eqOracle;
	}

	private static ExperimentAndLearner learningLStarM(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<? super MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy) {
		List<Word<String>> initPrefixes = new ArrayList<>();
		initPrefixes.add(Word.epsilon());
		List<Word<String>> initSuffixes = new ArrayList<>();
		Word<String> word = Word.epsilon();
		for (String symbol : mealyss.getInputAlphabet()) {
			initSuffixes.add(word.append(symbol));
		}

		// construct standard L*M instance
		ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
		builder.setAlphabet(mealyss.getInputAlphabet());
		builder.setOracle(mqOracle);
		builder.setInitialPrefixes(initPrefixes);
		builder.setInitialSuffixes(initSuffixes);
		builder.setCexHandler(handler);
		builder.setClosingStrategy(strategy);

		ExtensibleLStarMealy<String, Word<String>> learner = builder.create();

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	private static ExperimentAndLearner learningDLStarM_v0(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<? super MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy, File ot_file)
			throws IOException {
		// create log
		LearnLogger logger = LearnLogger.getLogger(Infer_LearnLib.class);

		MyObservationTable my_ot = loadObservationTable(mealyss, ot_file);

		int tpi = my_ot.getPrefixes().size();
		int tsi = my_ot.getSuffixes().size();

		logger.logEvent("Revalidate OT");
		ObservationTable<String, Word<Word<String>>> reval_ot = OTUtils.getInstance().revalidateObservationTable(my_ot,
				mqOracle, mealyss, false);

		StringBuffer sb = new StringBuffer();
		sb.append("\n");
		new ObservationTableASCIIWriter<>().write(reval_ot, sb);
		logger.logEvent(sb.toString());

		List<Word<String>> initPrefixes = new ArrayList<>();
		List<Word<String>> initSuffixes = new ArrayList<>();

		initPrefixes.addAll(my_ot.getPrefixes());
		initSuffixes.addAll(my_ot.getSuffixes());

		int tpf = my_ot.getPrefixes().size();
		int tsf = my_ot.getSuffixes().size();

		logger.logEvent("Reused prefixes: " + tpf + "/" + tpi);
		logger.logEvent("Reused suffixes: " + tsf + "/" + tsi);

		// construct DL*M v0 instance
		ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
		builder.setAlphabet(mealyss.getInputAlphabet());
		builder.setOracle(mqOracle);
		builder.setInitialPrefixes(initPrefixes);
		builder.setInitialSuffixes(initSuffixes);
		builder.setCexHandler(handler);
		builder.setClosingStrategy(strategy);
		ExtensibleLStarMealy<String, Word<String>> learner = builder.create();

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	private static ExperimentAndLearner learningDLStarM_v1(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<? super MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy, File ot_file)
			throws IOException {
		// create log
		LearnLogger logger = LearnLogger.getLogger(Infer_LearnLib.class);

		MyObservationTable my_ot = loadObservationTable(mealyss, ot_file);

		int tpi = my_ot.getPrefixes().size();
		int tsi = my_ot.getSuffixes().size();

		logger.logEvent("Revalidate OT");
		ObservationTable<String, Word<Word<String>>> reval_ot = OTUtils.getInstance().revalidateObservationTable(my_ot,
				mqOracle, mealyss, true);

		StringBuffer sb = new StringBuffer();
		sb.append("\n");
		new ObservationTableASCIIWriter<>().write(reval_ot, sb);
		logger.logEvent(sb.toString());

		List<Word<String>> initPrefixes = new ArrayList<>();
		List<Word<String>> initSuffixes = new ArrayList<>();

		initPrefixes.addAll(my_ot.getPrefixes());
		initSuffixes.addAll(my_ot.getSuffixes());

		int tpf = my_ot.getPrefixes().size();
		int tsf = my_ot.getSuffixes().size();

		logger.logEvent("Reused prefixes: " + tpf + "/" + tpi);
		logger.logEvent("Reused suffixes: " + tsf + "/" + tsi);

		// construct DL*M v1 instance
		ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
		builder.setAlphabet(mealyss.getInputAlphabet());
		builder.setOracle(mqOracle);
		builder.setInitialPrefixes(initPrefixes);
		builder.setInitialSuffixes(initSuffixes);
		builder.setCexHandler(handler);
		builder.setClosingStrategy(strategy);
		ExtensibleLStarMealy<String, Word<String>> learner = builder.create();

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	private static ExperimentAndLearner learningDLStarM_v2(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<? super MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy, File ot_file)
			throws IOException {

		System.out.println(ot_file.getPath());

		MyObservationTable my_ot = loadObservationTable(mealyss, ot_file);

		List<Word<String>> initPrefixes = new ArrayList<>(my_ot.getPrefixes());
		List<Word<String>> initSuffixes = new ArrayList<>(my_ot.getSuffixes());

		// construct DL*M v2 instance
		ExtensibleDLStarMealy<String, Word<String>> learner = new ExtensibleDLStarMealy<String, Word<String>>(
				mealyss.getInputAlphabet(), mqOracle, initPrefixes, initSuffixes, handler, strategy);

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	private static ExperimentAndLearner learningDLStarM_v3(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<? super MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy, File ot_file)
			throws IOException {

		MyObservationTable my_ot = loadObservationTable(mealyss, ot_file);

		// get alphabet of updated version
		Set<String> abc_updt = new HashSet<>(mealyss.getInputAlphabet());

		// get reused alphabet symbols (check first symbol of each S_ref word)
		Set<String> abc_reused = new HashSet<>();
		my_ot.getSuffixes().forEach(suff -> abc_reused.add(suff.getSymbol(0)));

		// find I_updt symbols that have been unreused
		boolean abc_unreused = abc_updt.removeAll(abc_reused);

		// if there is any unreused symbol from I_updt
		if (abc_unreused) {
			// then add unreused I_updt symbols to my_ot
			for (String symb : abc_updt) {
				Word<String> symb_w = Word.epsilon();
				symb_w = symb_w.append(symb);
				my_ot.getSuffixes().add(symb_w);
			}
		}

		List<Word<String>> initPrefixes = new ArrayList<>(my_ot.getPrefixes());
		List<Word<String>> initSuffixes = new ArrayList<>(my_ot.getSuffixes());

		// construct DL*M v3 instance
		ExtensibleDLStarMealy<String, Word<String>> learner = new ExtensibleDLStarMealy<String, Word<String>>(
				mealyss.getInputAlphabet(), mqOracle, initPrefixes, initSuffixes, handler, strategy);

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	private static ExperimentAndLearner learningDLStarM_v4(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<? super MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy, File ot_file)
			throws IOException {

		MyObservationTable my_ot = loadObservationTable(mealyss, ot_file);

		// get alphabet of updated version
		Set<String> abc_updt = new HashSet<>(mealyss.getInputAlphabet());

		// get reused alphabet symbols (check each S_ref member)
		Set<String> abc_reused = new HashSet<>();
		my_ot.getSuffixes().forEach(suff -> suff.forEach(symbol -> abc_reused.add(symbol)));

		// find I_updt symbols that have been unreused
		boolean abc_unreused = abc_updt.removeAll(abc_reused);

		// if there is any unreused symbol from I_updt
		if (abc_unreused) {
			// then add unreused I_updt symbols to my_ot
			for (String symb : abc_updt) {
				Word<String> symb_w = Word.epsilon();
				symb_w = symb_w.append(symb);
				my_ot.getSuffixes().add(symb_w);
			}
		}

		List<Word<String>> initPrefixes = new ArrayList<>(my_ot.getPrefixes());
		List<Word<String>> initSuffixes = new ArrayList<>(my_ot.getSuffixes());

		// construct DL*M v4 instance
		ExtensibleDLStarMealy<String, Word<String>> learner = new ExtensibleDLStarMealy<String, Word<String>>(
				mealyss.getInputAlphabet(), mqOracle, initPrefixes, initSuffixes, handler, strategy);

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	private static ExperimentAndLearner learningDLStarM_v5(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<? super MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy, File ot_file)
			throws IOException {

		MyObservationTable my_ot = loadObservationTable(mealyss, ot_file);

		ObservationTable<String, Word<Word<String>>> reval_ot = OTUtils.getInstance().revalidateObservationTable(my_ot,
				mqOracle, mealyss, true);

		List<Word<String>> initPrefixes = new ArrayList<>();
		List<Word<String>> initSuffixes = new ArrayList<>();

		initPrefixes.addAll(reval_ot.getShortPrefixes());
		initSuffixes.addAll(reval_ot.getSuffixes());

		Alphabet<String> pAlphabet = mealyss.getInputAlphabet();

		List<String> initSuffixesString = new ArrayList<>();
		for (Word<String> w : initSuffixes) {
			String wString = w.toString();
			initSuffixesString.add(wString);
		}

		for (String a1 : pAlphabet) {
			if (!initSuffixesString.contains(a1)) {
				Word<String> a2 = Word.fromLetter(a1);
				initSuffixes.add(a2);
			}
		}

		ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
		builder.setAlphabet(mealyss.getInputAlphabet());
		builder.setOracle(mqOracle);
		builder.setInitialPrefixes(initPrefixes);
		builder.setInitialSuffixes(initSuffixes);
		builder.setCexHandler(handler);
		builder.setClosingStrategy(strategy);
		ExtensibleLStarMealy<String, Word<String>> learner = builder.create();

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	private static ExperimentAndLearner learningDLStarM_v6(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle<String, Word<Word<String>>> mqOracle,
			EquivalenceOracle<? super MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle,
			ObservationTableCEXHandler<Object, Object> handler, ClosingStrategy<Object, Object> strategy, File ot_file)
			throws IOException {

		MyObservationTable my_ot = loadObservationTable(mealyss, ot_file);

		// get alphabet of updated version
		Set<String> abc_updt = new HashSet<>(mealyss.getInputAlphabet());

		// get reused alphabet symbols (check each S_ref member)
		Set<String> abc_reused = new HashSet<>();
		my_ot.getSuffixes().forEach(suff -> suff.forEach(symbol -> abc_reused.add(symbol)));

		// find I_updt symbols that have been unreused
		boolean abc_unreused = abc_updt.removeAll(abc_reused);

		// if there is any unreused symbol from I_updt
		if (abc_unreused) {
			// then add unreused I_updt symbols to my_ot
			for (String symb : abc_updt) {
				Word<String> symb_w = Word.epsilon();
				symb_w = symb_w.append(symb);
				my_ot.getSuffixes().add(symb_w);
			}
		}

		List<Word<String>> initPrefixes = new ArrayList<>(my_ot.getPrefixes());
		List<Word<String>> initSuffixes = new ArrayList<>(my_ot.getSuffixes());

		ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
		builder.setAlphabet(mealyss.getInputAlphabet());
		builder.setOracle(mqOracle);
		builder.setInitialPrefixes(initPrefixes);
		builder.setInitialSuffixes(initSuffixes);
		builder.setCexHandler(handler);
		builder.setClosingStrategy(strategy);
		ExtensibleLStarMealy<String, Word<String>> learner = builder.create();

		// The experiment will execute the main loop of active learning
		MealyExperiment<String, Word<String>> experiment = new MealyExperiment<String, Word<String>>(learner, eqOracle,
				mealyss.getInputAlphabet());

		ExperimentAndLearner pair = new ExperimentAndLearner(learner, experiment);
		return pair;
	}

	protected static MyObservationTable loadObservationTable(CompactMealy<String, Word<String>> mealyss, File the_ot)
			throws IOException {
		// create log
		LearnLogger logger = LearnLogger.getLogger(Infer_LearnLib.class);
		logger.logEvent("Reading OT: " + the_ot.getName());

		MyObservationTable my_ot = OTUtils.getInstance().readOT(the_ot, mealyss.getInputAlphabet());

		return my_ot;

	}

	private static int[] LearnProductFSM(File sul_file, File ot_file, String learningAlgorithm_1, File out_dir_file,
			CommandLine line_1, Random rnd_seed_1, int[] statistics_array, int saveOT, LearnLogger logger) {
		try {
			// set closing strategy
			ClosingStrategy<Object, Object> strategy = getClosingStrategy(line_1.getOptionValue(CLOS));

			// set CE processing approach
			ObservationTableCEXHandler<Object, Object> handler = getCEXHandler(line_1.getOptionValue(CEXH));

			// load mealy machine

			CompactMealy<String, Word<String>> mealyss = LoadMealy(sul_file);

//			System.out.println("Alphabet:\n" + mealyss.getInputAlphabet());

			logger.logEvent("\nSUL name: " + sul_file.getName());
			logger.logEvent("SUL dir: " + sul_file.getAbsolutePath());
			logger.logEvent("Output dir: " + out_dir_file);

			Utils.getInstance();
			// SUL simulator
			SUL<String, Word<String>> sulSim = new MealySimulatorSUL<>(mealyss, Utils.OMEGA_SYMBOL);

			//////////////////////////////////
			// Setup objects related to MQs //
			//////////////////////////////////

			// Counters for MQs
			StatisticSUL<String, Word<String>> mq_sym = new SymbolCounterSUL<>("MQ", sulSim);
			StatisticSUL<String, Word<String>> mq_rst = new ResetCounterSUL<>("MQ", mq_sym);

			// SUL for counting queries wraps sul
			SUL<String, Word<String>> mq_sul = mq_rst;

			// use caching to avoid duplicate queries
			if (line_1.hasOption(CACHE)) {
				// SULs for associating the IncrementalMealyBuilder 'mq_cbuilder' to MQs
				mq_sul = SULCache.createDAGCache(mealyss.getInputAlphabet(), mq_rst);
			}

			MembershipOracle<String, Word<Word<String>>> mqOracle = new SULOracle<String, Word<String>>(mq_sul);

			logger.logEvent("Cache: " + (line_1.hasOption(CACHE) ? "Y" : "N"));

			//////////////////////////////////
			// Setup objects related to EQs //
			//////////////////////////////////

			logger.logEvent("ClosingStrategy: " + strategy.toString());
			logger.logEvent("ObservationTableCEXHandler: " + line_1.getOptionValue(CEXH));

			// Counters for EQs
			StatisticSUL<String, Word<String>> eq_sym = new SymbolCounterSUL<>("EQ", sulSim);
			StatisticSUL<String, Word<String>> eq_rst = new ResetCounterSUL<>("EQ", eq_sym);

			// SUL for counting queries wraps sul
			SUL<String, Word<String>> eq_sul = eq_rst;

			// use caching to avoid duplicate queries
			if (line_1.hasOption(CACHE)) {
				// SULs for associating the IncrementalMealyBuilder 'cbuilder' to EQs
				eq_sul = SULCache.createDAGCache(mealyss.getInputAlphabet(), eq_rst);
			}

			EquivalenceOracle<MealyMachine<?, String, ?, Word<String>>, String, Word<Word<String>>> eqOracle = null;
			eqOracle = buildEqOracle(rnd_seed_1, line_1, logger, mealyss, eq_sul);

			ExperimentAndLearner experiment_pair = null;

			switch (learningAlgorithm_1) {
			case "lstar":
				logger.logConfig("Method: L*M");
				experiment_pair = learningLStarM(mealyss, mqOracle, eqOracle, handler, strategy);
				break;
			case "dlstar_v0":
				if (handler == ObservationTableCEXHandlers.CLASSIC_LSTAR)
					throw new Exception("DL*M requires " + ObservationTableCEXHandlers.RIVEST_SCHAPIRE + " CexH");
				logger.logConfig("Method: DL*M_v0");
				logger.logEvent("Revalidate OT");
				experiment_pair = learningDLStarM_v0(mealyss, mqOracle, eqOracle, handler, strategy, ot_file);
				// learning statistics
				logger.logEvent(
						"Reused queries [resets]: " + ((ResetCounterSUL) mq_rst).getStatisticalData().getCount());
				logger.logEvent(
						"Reused queries [symbols]: " + ((SymbolCounterSUL) mq_sym).getStatisticalData().getCount());
				break;
			case "dlstar_v1":
				if (handler == ObservationTableCEXHandlers.CLASSIC_LSTAR)
					throw new Exception("DL*M requires " + ObservationTableCEXHandlers.RIVEST_SCHAPIRE + " CexH");
				logger.logConfig("Method: DL*M_v1");
				logger.logEvent("Revalidate OT");
				experiment_pair = learningDLStarM_v1(mealyss, mqOracle, eqOracle, handler, strategy, ot_file);
				// learning statistics
				logger.logEvent(
						"Reused queries [resets]: " + ((ResetCounterSUL) mq_rst).getStatisticalData().getCount());
				logger.logEvent(
						"Reused queries [symbols]: " + ((SymbolCounterSUL) mq_sym).getStatisticalData().getCount());
				break;
			case "dlstar_v2":
				if (handler == ObservationTableCEXHandlers.CLASSIC_LSTAR)
					throw new Exception("DL*M requires " + ObservationTableCEXHandlers.RIVEST_SCHAPIRE + " CexH");
				logger.logConfig("Method: DL*M_v2");
				experiment_pair = learningDLStarM_v2(mealyss, mqOracle, eqOracle, handler, strategy, ot_file);
				break;
			case "dlstar_v3":
				if (handler == ObservationTableCEXHandlers.CLASSIC_LSTAR)
					throw new Exception("DL*M requires " + ObservationTableCEXHandlers.RIVEST_SCHAPIRE + " CexH");
				logger.logConfig("Method: DL*M_v3");
				experiment_pair = learningDLStarM_v3(mealyss, mqOracle, eqOracle, handler, strategy, ot_file);
				break;
			case "dlstar_v4":
				if (handler == ObservationTableCEXHandlers.CLASSIC_LSTAR)
					throw new Exception("DL*M requires " + ObservationTableCEXHandlers.RIVEST_SCHAPIRE + " CexH");
				logger.logConfig("Method: DL*M_v4");
				experiment_pair = learningDLStarM_v4(mealyss, mqOracle, eqOracle, handler, strategy, ot_file);
				break;
			case "dlstar_v5":
				if (handler == ObservationTableCEXHandlers.CLASSIC_LSTAR)
					throw new Exception("DL*M requires " + ObservationTableCEXHandlers.RIVEST_SCHAPIRE + " CexH");
				logger.logConfig("Method: DL*M_v5");
				experiment_pair = learningDLStarM_v5(mealyss, mqOracle, eqOracle, handler, strategy, ot_file);
				break;
			case "dlstar_v6":
				if (handler == ObservationTableCEXHandlers.CLASSIC_LSTAR)
					throw new Exception("DL*M requires " + ObservationTableCEXHandlers.RIVEST_SCHAPIRE + " CexH");
				logger.logConfig("Method: DL*M_v6");
				experiment_pair = learningDLStarM_v6(mealyss, mqOracle, eqOracle, handler, strategy, ot_file);
				break;
			default:
				throw new Exception("Invalid learning method selected: " + learningAlgorithm_1);
			}

			MealyExperiment experiment = experiment_pair.getExperiment();

			// turn on time profiling
			experiment.setProfile(true);

			// uncomment one of the following lines:
//			experiment.setLogModels(true);
			experiment.setLogOT(true);

			// run experiment
			experiment.run();

			// statistics array
			statistics_array[0] += experiment.getRounds().getCount();
			statistics_array[1] += ExtractValue(mq_rst.getStatisticalData().getSummary());
			statistics_array[2] += ExtractValue(mq_sym.getStatisticalData().getSummary());
			statistics_array[3] += ExtractValue(eq_rst.getStatisticalData().getSummary());
			statistics_array[4] += ExtractValue(eq_sym.getStatisticalData().getSummary());

			// learning statistics
			logger.logConfig("Rounds: " + experiment.getRounds().getCount());
			logger.logStatistic(mq_rst.getStatisticalData());
			logger.logStatistic(mq_sym.getStatisticalData());
			logger.logStatistic(eq_rst.getStatisticalData());
			logger.logStatistic(eq_sym.getStatisticalData());

			// profiling
			SimpleProfiler.logResults();

			MealyMachine finalHyp = (MealyMachine) experiment.getFinalHypothesis();

			logger.logConfig("Qsize: " + mealyss.getStates().size());
			logger.logConfig("Isize: " + mealyss.getInputAlphabet().size());

			boolean isEquiv = mealyss.getStates().size() == finalHyp.getStates().size();
			if (isEquiv) {
				logger.logConfig("Equivalent: OK");
			} else {
				logger.logConfig("Equivalent: NOK");
			}

			if (line_1.hasOption(SOT) && experiment_pair.getLearner() != null) {
				StringBuffer sb = new StringBuffer();
				sb.append("\n");
				new ObservationTableASCIIWriter<>().write(experiment_pair.getLearner().getObservationTable(), sb);
				logger.logEvent(sb.toString());
			}

			File ot_out_dir = null;
			if (line_1.hasOption(SOT2) && experiment_pair.getLearner() != null) {
				ot_out_dir = new File(line_1.getOptionValue(SOT2));
				OTUtils otUtils_1 = new OTUtils();

				String fileCompleteName = sul_file.getName();
				String fileName = "";
				int j = fileCompleteName.lastIndexOf('.');
				if (j >= 0) {
					fileName = fileCompleteName.substring(0, j);
				}

				if (saveOT == 1) {
					// save observation table
					String ot_name = "";
					if (fileName.endsWith("_text")) {
						ot_name = fileName.replace("_text", "_ot");
					} else {
						ot_name = fileName + "_ot";
					}
					File ot_file_2 = new File(ot_out_dir, ot_name);
					otUtils_1.writeOT(experiment_pair.getLearner().getObservationTable(), ot_file_2);

					// save observation FSM as a .txt file
					String learned_fsm_name = "";
					if (fileName.endsWith("_text")) {
						learned_fsm_name = fileName.replace("_text", "_learnedFsm") + ".txt";
						;
					} else {
						learned_fsm_name = fileName + "_learnedFsm.txt";
					}
					File learned_fsm_file = new File(ot_out_dir, learned_fsm_name);
					String header = "";
					FeaturedMealyUtils.getInstance().saveFSM_kiss(mealyss, learned_fsm_file, header);

					// save FSM as a dot file
					String learned_fsm_dot_name = "";
					if (fileName.endsWith("_text")) {
						learned_fsm_dot_name = ot_out_dir.toString() + "\\"
								+ fileName.replaceFirst("_text", "_learnedFsmDot.dot");
					} else {
						learned_fsm_dot_name = ot_out_dir.toString() + "\\" + fileName + "_learnedFsmDot.dot";
					}
					BufferedWriter bw = new BufferedWriter(new FileWriter(learned_fsm_dot_name));
					GraphDOT.write(mealyss, bw);
					bw.close();
				}

				System.out.println(Arrays.toString(statistics_array));
				System.out.println("[Rounds, MQ [Resets], MQ [Symbols], EQ [Resets], EQ [Symbols]]");
			}

		} catch (Exception exp) {
			System.err.println("Unexpected Exception");
			exp.printStackTrace();
		}
		return statistics_array;

	}

	private static int ExtractValue(String string_1) {
		// TODO Auto-generated method stub
		int value_1 = 0;
		int j = string_1.lastIndexOf(" ");
		String string_2 = "";
		if (j >= 0) {
			string_2 = string_1.substring(j + 1);
		}
		value_1 = Integer.parseInt(string_2);
		return value_1;
	}

	private static CompactMealy<String, Word<String>> LoadMealy(File fsm_file) {
		// TODO Auto-generated method stub
		InputModelDeserializer<String, CompactMealy<String, Word<String>>> parser_1 = DOTParsers
				.mealy(MEALY_EDGE_WORD_STR_PARSER);
		CompactMealy<String, Word<String>> mealy = null;
		String file_name = fsm_file.getName();
		if (file_name.endsWith("txt")) {
			try {
				mealy = Utils.getInstance().loadMealyMachine(fsm_file);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return mealy;
		} else if (file_name.endsWith("dot")) {
			try {
				mealy = parser_1.readModel(fsm_file).model;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return mealy;
		}

		return null;
	}
	
	private static Options createOptions() {
		// create the Options
		Options options = new Options();
		options.addOption(SUL1, true, "System Under Learning (SUL)_1");
		options.addOption(SUL2, true, "System Under Learning (SUL)_2");
		options.addOption(SOT, false, "Save observation table (OT)");
		options.addOption(SOT2, true, "Save observation table (OT) to the disk");
		options.addOption(HELP, false, "Shows help");
		options.addOption(OT, true, "Load observation table (OT)");
		options.addOption(OUT, true, "Set output directory");
		options.addOption(CLOS, true,
				"Set closing strategy." + "\nOptions: {" + String.join(", ", closingStrategiesAvailable) + "}");
		options.addOption(EQ, true,
				"Set equivalence query generator." + "\nOptions: {" + String.join(", ", eqMethodsAvailable) + "}");
		options.addOption(CEXH, true, "Set counter example (CE) processing method." + "\nOptions: {"
				+ String.join(", ", cexHandlersAvailable) + "}");
		options.addOption(CACHE, false, "Use caching.");
		options.addOption(LEARN, true,
				"Model learning algorithm." + "\nOptions: {" + String.join(", ", learningMethodsAvailable) + "}");
		options.addOption(SEED, true, "Seed used by the random generator");
		options.addOption(INFO, true, "Add extra information as string");
		options.addOption(FM, true, "Feature model");
		return options;
	}

}
