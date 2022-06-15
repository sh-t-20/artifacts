package ir.ac.ut.fml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.Function;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.checkerframework.checker.nullness.qual.Nullable;

import br.usp.icmc.labes.mealyInference.Infer_LearnLib;
import br.usp.icmc.labes.mealyInference.utils.MyObservationTable;
import br.usp.icmc.labes.mealyInference.utils.OTUtils;
import br.usp.icmc.labes.mealyInference.utils.Utils;
import de.learnlib.api.logging.LearnLogger;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.commons.util.Pair;
import net.automatalib.serialization.InputModelDeserializer;
import net.automatalib.serialization.dot.DOTParsers;
import net.automatalib.visualization.VisualizationHelper.EdgeAttrs;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

public class OtFromFSM {

	public static final String FSM = "fsm";
	public static final String OUT = "out";
	public static final String HELP = "help";

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

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		// create the command line parser
		CommandLineParser parser = new BasicParser();
		Options options = createOptions();
		HelpFormatter formatter = new HelpFormatter();

		try {
			CommandLine line = parser.parse(options, args);

			if (line.hasOption(HELP)) {
				formatter.printHelp("Infer_LearnLib", options);
				System.exit(0);
			}
			if (!line.hasOption(FSM)) {
				throw new IllegalArgumentException("must provide an FSM");
			}

			File fsm_1 = new File(line.getOptionValue(FSM));
			File out_dir = new File(line.getOptionValue(OUT));

			CompactMealy<String, Word<String>> mealy_1 = LoadMealy(fsm_1);

			MyObservationTable ot_1 = getOT(mealy_1);
			
			System.out.println(ot_1.getPrefixes());
			System.out.println(ot_1.getSuffixes());

//			// Save observation table
//			String ot_name = "ot_file";
//			File ot_file = new File(out_dir, ot_name);
//			OTUtils otUtils_1 = new OTUtils();
//			otUtils_1.writeOT(ot, ot_file);

			System.out.println("\nFinished");

		} catch (Exception exp) {
			// automatically generate the help statement
			formatter.printHelp("Options:", options);
			System.err.println("Unexpected Exception");
			exp.printStackTrace();
		}
	}

	private static MyObservationTable getOT(CompactMealy<String, Word<String>> mealy) {
		// TODO Auto-generated method stub

		Alphabet<String> alphabet = mealy.getInputAlphabet();

		String ot_string = "";

		// Set the set of OT prefixes
		List<Word<String>> prefixes = new ArrayList<>();

		@Nullable
		int initial_state = mealy.getInitialState();
		Collection<Integer> fsm_states = mealy.getStates();

		int fsm_size = fsm_states.size();
		Queue<Integer> states_queue = new LinkedList<>();
		Queue<Integer> processed_states_queue = new LinkedList<>();
		states_queue.add(initial_state);
		int[] p_array = new int[fsm_size];
		Arrays.fill(p_array, -1);

		int[] shortest_distance_array = new int[fsm_size];
		Arrays.fill(shortest_distance_array, 0);

		String[] input_array = new String[fsm_size];
		Arrays.fill(input_array, null);

		while (states_queue.size() != 0) {
			int current_state = states_queue.remove();
			processed_states_queue.add(current_state);
			for (String a : alphabet) {
				int next_state = mealy.getTransition(current_state, a).getSuccId();
				if (!states_queue.contains(next_state) && !processed_states_queue.contains(next_state)) {
					p_array[next_state] = current_state;
					shortest_distance_array[next_state] = shortest_distance_array[current_state] + 1;
					input_array[next_state] = a;
					states_queue.add(next_state);
				}
			}
		}

		for (int s : fsm_states) {
			int s_copy = s;
			String prefix_string = null;
			String prefix_string_2 = null;
			if (p_array[s] == -1) {
				prefixes.add(Word.epsilon());

			} else {
				prefix_string = "";
				prefix_string_2 = "";
				int p = p_array[s];
				int depth = 0;
				while (p != -1) {
					prefix_string = " " + input_array[s] + prefix_string;
					int previous_s = s;
					s = p;
					p = p_array[s];
					if (p_array[s] == -1) {
						prefix_string_2 = " " + input_array[previous_s] + prefix_string_2;
					} else {
						prefix_string_2 = " ," + input_array[previous_s] + prefix_string_2;
					}

				}
				Word<String> prefix = Word.fromSymbols(prefix_string);
				prefixes.add(prefix);
				ot_string += prefix_string_2;
			}

			if (s_copy != fsm_size - 1) {
				ot_string += ";";
			}

		}
		ot_string += "\n";
//		System.out.println("Prefixes:\n" + prefixes + "\n");

		// Set the set of OT suffixes
		List<Word<String>> suffixes = new ArrayList<>();
		List<String> suffixesString = new ArrayList<>();
		for (Word<String> w : suffixes) {
			String wString = w.toString();
			suffixesString.add(wString);
		}
		int index = 0;
		for (String a1 : alphabet) {
			if (!suffixesString.contains(a1)) {
				Word<String> a2 = Word.fromLetter(a1);
				suffixes.add(a2);
				ot_string += a1;
				if (index != alphabet.size() - 1) {
					ot_string += " ;";
				}
			}
			index += 1;
		}

//		System.out.println("Suffixes:\n" + suffixes + "\n");
//		System.out.println(ot_string);

		MyObservationTable ot = new MyObservationTable(prefixes, suffixes);
		return ot;
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

	protected static MyObservationTable loadObservationTable(CompactMealy<String, Word<String>> mealyss, File the_ot)
			throws IOException {
		// create log
		LearnLogger logger = LearnLogger.getLogger(Infer_LearnLib.class);
		logger.logEvent("Reading OT: " + the_ot.getName());

		MyObservationTable my_ot = OTUtils.getInstance().readOT(the_ot, mealyss.getInputAlphabet());

		return my_ot;

	}

	private static Options createOptions() {
		// create the Options
		Options options = new Options();
		options.addOption(FSM, true, "Finite state machine");
		options.addOption(OUT, true, "Set output directory");
		options.addOption(HELP, false, "Shows help");
		return options;
	}
}
