package ir.ac.ut.fml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import br.usp.icmc.labes.mealyInference.utils.MyObservationTable;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.words.Word;

public class ConvertToExcelFile2 {

	public static final String FILE = "file";
	public static final String OUT = "out";
	public static final String A = "a";
	public static final String N = "n";
	public static final String HELP = "help";

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
			if (!line.hasOption(FILE)) {
				throw new IllegalArgumentException("must provide a FILE");
			}
			int learning_method = 0;
			// learning_method 0: Non-adaptive, 1: Adaptive, 2: Both
			if (line.hasOption(A)) {
				learning_method = 1;
			} else if (line.hasOption(N)) {
				learning_method = 0;
			} else {
				learning_method = 2;
			}

//			File text_file = new File(line.getOptionValue(FILE));
			FileInputStream text_file = new FileInputStream(line.getOptionValue(FILE));
			File out_dir = new File(line.getOptionValue(OUT));

			String result = "";
			result = "Learning order,PL* rounds,PL* MQ resets,PL* MQ input symbols,PL* EQ resets,PL* EQ input symbols,PL* total resets,PL* total input symbols"
					+ "\n";

			BufferedReader br = new BufferedReader(new InputStreamReader(text_file));
			String line_1 = "";
			String line_2 = "";
			while ((line_1 = br.readLine()) != null) {
//				System.out.println("line:" + line_1.toString());
				if (line_1.toString().contains("order")) {
					System.out.println("a1 " + line_1);
					line_2 = line_1.replaceAll(".*\\:\s", "");
					line_2 = line_1.replaceAll(",", "\\;");
					line_2 = line_2 + ", ";
					System.out.println("a2 " + line_2);
				} else {
					line_2 = line_1;
					if (learning_method == 1) {
						if (line_1.toString().contains("Adaptive")) {
							System.out.println("Ahhhh " + line_2);
							line_2 = line_2.replaceAll(".*method\\:\s\\[", "");
							line_2 = line_2.replaceAll("\\]", "\n");
							System.out.println("Ahhhh " + line_2);
						}
					} else if (learning_method == 0) {
						if (line_1.toString().contains("Non")) {
							line_2 = line_2.replaceAll(".*method\\:\s\\[", "");
							line_2 = line_2.replaceAll("\\]", "\n");
						}
					} else if (learning_method == 2) {
						if (line_1.toString().contains("Adaptive")) {
							line_2 = line_2.replaceAll(".*method\\:\s\\[", "");
							line_2 = line_2.replaceAll("\\]", "\n");
						}
					}

				}
				line_2 = line_2.replaceAll(".*order\\:\s", "");
				line_2 = line_2.replaceAll(".*Non.*", "");
				line_2 = line_2.replaceAll("SUL.*", "");
				line_2 = line_2.replaceAll("2022-.*|SUL.*", "");

				if (!line_2.equals("")) {
					System.out.println("Added:" + line_2);
					result = result + line_2;
				}
			}
//			System.out.println("result:\n" + result);

//			String text_1 = Files.readString(text_file.toPath(), StandardCharsets.UTF_8);

			File result_file = new File(out_dir, "metrics_result.csv");
//			PrintWriter out = new PrintWriter(result_file);
//			out.println(result);
			FileWriter file_stream = new FileWriter(result_file);
			BufferedWriter out = new BufferedWriter(file_stream);
			out.write(result);
			out.close();
			System.out.println("result:\n" + result);

			System.out.println("Finished");

		} catch (Exception exp) {
			// automatically generate the help statement
			formatter.printHelp("Options:", options);
			System.err.println("Unexpected Exception");
			exp.printStackTrace();
		}
	}

	private static Options createOptions() {
		// create the Options
		Options options = new Options();
		options.addOption(FILE, true, "Input text file");
		options.addOption(OUT, true, "Set output directory");
		options.addOption(A, false, "Adaptive learning");
		options.addOption(N, false, "Non-adaptive learning");
		options.addOption(HELP, false, "Shows help");
		return options;
	}

}
