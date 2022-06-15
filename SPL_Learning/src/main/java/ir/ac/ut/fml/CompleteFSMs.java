package ir.ac.ut.fml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.checkerframework.checker.nullness.qual.Nullable;

import br.usp.icmc.labes.mealyInference.utils.Utils;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.commons.util.Pair;
import net.automatalib.serialization.InputModelDeserializer;
import net.automatalib.serialization.dot.DOTParsers;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.visualization.VisualizationHelper.EdgeAttrs;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

public class CompleteFSMs {

	public static final String DIR = "dir";
	public static final String OUT = "out";

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

		try {
			// create the command line parser
			CommandLineParser parser = new BasicParser();

			// create the Options
			Options options = createOptions();

			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();

			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			File fsm_dir = new File(line.getOptionValue(DIR));
			File output_dir = new File(line.getOptionValue(OUT));

			File[] filesList = fsm_dir.listFiles();
			for (int i = 0; i < filesList.length; i++) {
				// The next line should be removed later
				File file_1 = filesList[i];
				CompactMealy<String, Word<String>> mealy_1 = LoadMealy(file_1);
				CompactMealy<String, Word<String>> mealy_2 = LoadMealy(file_1);
				Alphabet<String> alphabet = mealy_1.getInputAlphabet();
				for (Integer s : mealy_1.getStates()) {
					for (String a : alphabet) {
//							System.out.println(s + ", " + a + ":");
						if (mealy_1.getTransition(s, a) == null) {
//							For BCS FSMs, uncomment the output_string = "" line (and comment the next line)
//							String output_string = "";
							String output_string = "1";
							mealy_2.setTransition(s, a, s, Word.fromSymbols(output_string));
//								System.out.println("added transition:\n" + s + " " + a + "/ " + mealy_2.getTransition(s, a).getOutput() + " "
//										+ mealy_2.getTransition(s, a).getSuccId() + "\n");
						}
					}
				}
				String file_name = output_dir.toString() + "\\" + file_1.getName();
				BufferedWriter bw = new BufferedWriter(new FileWriter(file_name));
				GraphDOT.write(mealy_2, bw);
				bw.close();

			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getStackTrace()[0].getLineNumber());
		}

		System.out.println("\nFinished!");

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
		Options options = new Options();
		options.addOption(DIR, true, "Directory of the FSM files");
		options.addOption(OUT, true, "Set output directory");
		return options;
	}

}
