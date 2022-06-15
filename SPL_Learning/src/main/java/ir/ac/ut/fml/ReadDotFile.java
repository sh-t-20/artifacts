package ir.ac.ut.fml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.io.Files;

import be.vibes.fexpression.Feature;
import be.vibes.fexpression.configuration.SimpleConfiguration;
import br.usp.icmc.labes.mealyInference.utils.Utils;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.automata.transducers.impl.compact.CompactMealyTransition;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import uk.le.ac.fts.FtsUtils;

import net.automatalib.serialization.InputModelDeserializer;
import net.automatalib.serialization.dot.DOTParsers;
import net.automatalib.serialization.InputModelData;

public class ReadDotFile {
	public static final String DIR = "dir";

	public static void main(String[] args) {
		// TODO Auto-generated method stub

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

			File dir_1 = new File(line.getOptionValue(DIR));

			File[] filesList = dir_1.listFiles();

			for (int i = 0; i < filesList.length; i++) {
				File file_1 = filesList[i];
				String file_name = file_1.getPath();

				String fileExtension = "";
				String fileName = file_1.getName();
				int j = fileName.lastIndexOf('.');
				if (j >= 0) {
					fileExtension = fileName.substring(j + 1);
				}

				if (fileExtension.equals("dot")) {
					System.out.println(fileName);
					InputModelDeserializer<String, CompactMealy<String, String>> parser_1 = DOTParsers.mealy();
					InputModelData<String, CompactMealy<String, String>> machine_1 = parser_1.readModel(file_1);
					CompactMealy<String, String> mealy_1 = machine_1.model;
					System.out.println(mealy_1.getStates());
					Collection<Integer> states = mealy_1.getStates();
					System.out.println(mealy_1.getIntInitialState());
					Alphabet<String> alphabet = mealy_1.getInputAlphabet();
					System.out.println(alphabet.toString());
					for (Integer s : states) {
						for (String a : alphabet) {
							@Nullable
							CompactMealyTransition<String> transition = mealy_1.getTransition(s, a);
							if (transition != null) {
								System.out.println(s.toString() + "  " + a.toString() + "  " + transition.getOutput()
										+ "  " + transition.getSuccId());
							}
						}

					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Finished!");

	}

	private static Options createOptions() {
		Options options = new Options();
		options.addOption(DIR, true, "Directory of the config files");
		return options;
	}
}
