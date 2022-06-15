package ir.ac.ut.fml;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.prop4j.Node;

import be.vibes.fexpression.configuration.SimpleConfiguration;
import br.usp.icmc.labes.mealyInference.utils.Utils;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.commons.util.Pair;
import net.automatalib.serialization.InputModelDeserializer;
import net.automatalib.serialization.dot.DOTParsers;
import net.automatalib.visualization.VisualizationHelper.EdgeAttrs;
import net.automatalib.words.Word;
import uk.le.ac.fts.FtsUtils;

public class FsmFromFFSM {

	private static final String FFSM = "ffsm";
	private static final String CONF = "conf";
	public static final String OUT = "out";
	private static final String HELP = "h";

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

		CommandLineParser parser = new BasicParser();
		Options options = createOptions();
		HelpFormatter formatter = new HelpFormatter();

		try {
			CommandLine line = parser.parse(options, args);

			if (line.hasOption(HELP)) {
				formatter.printHelp("Infer_LearnLib", options);
				System.exit(0);
			}
			if (!line.hasOption(FFSM)) {
				throw new IllegalArgumentException("must provide an FFSM");
			}

			File ffsm_file = new File(line.getOptionValue(FFSM));
			String config_1 = line.getOptionValue(CONF);
			File out_dir = new File(line.getOptionValue(OUT));

			CompactMealy<String, Word<String>> ffsm_1 = LoadMealy(ffsm_file);
			
			SimpleConfiguration product_config = FtsUtils.getInstance().loadConfiguration(config_1);
			
			System.out.println("Finished");

		} catch (Exception exp) {
			// automatically generate the help statement
			formatter.printHelp("Options:", options);
			System.err.println("Unexpected Exception");
			exp.printStackTrace();
		}

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

	private static Node simplifyCondition(Node c_1) {
		// TODO Auto-generated method stub
		Node c_2 = c_1.toCNF();
		return c_2;
	}
	
	private static Options createOptions() {
		Options options = new Options();
		options.addOption(FFSM, true, "Featured Finite State Machine");
		options.addOption(CONF, true, "Product configuration");
		options.addOption(OUT, true, "Set output directory");
		options.addOption(HELP, false, "Help menu");
		return options;
	}

}
