package ir.ac.ut.fml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
import net.automatalib.commons.util.Pair;
import net.automatalib.serialization.InputModelDeserializer;
import net.automatalib.serialization.dot.DOTParsers;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.visualization.VisualizationHelper.EdgeAttrs;
import net.automatalib.words.Word;
import uk.le.ac.fts.FtsUtils;

public class CopyFSMs {

	// Finds FSMs of all configurations available in a folder and copies them in the
	// same folder with a name related to the corresponding configuration file.
	private static final String HELP = "h";
	public static final String FSM = "fsm";
	public static final String DIR = "dir";
	private static final String FM = "fm";
	public static final String PNAME = "pname";

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

			if (line.hasOption(HELP) || !line.hasOption(FSM) || !line.hasOption(DIR)) {
				formatter.printHelp("CreateFSMs", options);
				System.exit(0);
			}
			
			File fm_file = new File(line.getOptionValue(FM));
			IFeatureModel feature_m = FeatureModelManager.load(fm_file.toPath()).getObject();
			List<String> all_model_features = new ArrayList<>();
			for (IFeature node : feature_m.getFeatures()) {
				if (node.getName().equals("TRUE"))
					continue;
				all_model_features.add(node.getName());
			}
			System.out.println(all_model_features);
			
			String p_name = new String(line.getOptionValue(PNAME));
			String[] f_list_1 = new String[100];
			String[] f_list_2 = new String[100];
			
			switch(p_name) {
			case "ws":
				f_list_1 = new String[] {"sLow", "wLow", "sHigh", "wHigh", "PermWiper"};
				f_list_2 = new String[] {"sL", "wL", "sH", "wH", "pW"};
				break;
			case "vm":
				f_list_1 = new String[] {"TEA", "COF", "CAP", "DOL", "EUR", "TON"};
				f_list_2 = new String[] {"TEA", "COF", "CAP", "DOL", "EUR", "TON"};
				break;
			case "agm":
				f_list_1 = all_model_features.toArray(new String[0]);
				f_list_2 = all_model_features.toArray(new String[0]);
			default:
				break;
			}
			
			
			List<String> all_features = Arrays.asList(f_list_2);

			File fsm_dir = new File(line.getOptionValue(FSM));

			File configs_dir = new File(line.getOptionValue(DIR));

			File[] configFilesList = configs_dir.listFiles();
			File[] fsmFilesList = fsm_dir.listFiles();
			
			for (int i = 0; i < configFilesList.length; i++) {
				File configFile = configFilesList[i];
				String config = configFile.getPath();

				String fileExtension = "";
				String fileName = configFile.getName();
				int j = fileName.lastIndexOf('.');
				if (j >= 0) {
					fileExtension = fileName.substring(j + 1);
				}

				if (fileExtension.equals("config")) {
//					System.out.println(config);
					SimpleConfiguration config_i = FtsUtils.getInstance().loadConfiguration(config);
					Feature[] config_i_features = config_i.getFeatures();
					List<String> features_i = new ArrayList<>();
					for (Feature f : config_i_features) {
						features_i.add(f.toString());
					}
					
					System.out.println("i: " + i);
					System.out.println(features_i);
					
					for (int s = 0; s < features_i.size(); s++) {
						String f_1 = features_i.get(s);
						String f_2 = getFeatureName(f_1, f_list_1, f_list_2);
						features_i.set(s, f_2);
					}
					
					System.out.println(features_i);
					
					for (int k = 0; k < fsmFilesList.length; k++) {
						File fsmFile = fsmFilesList[k];
						String fsmFileName = fsmFile.getName();
						if(fsmFileName.endsWith("txt")) {
							BufferedReader br = new BufferedReader(new FileReader(fsmFile));
							String line_text = br.readLine();
							String[] features = line_text.split("\t");
//							System.out.println(Arrays.toString(features));
							
							List<String> features_k = new ArrayList<>();
							for (String f: features) {
								if (Arrays.asList(f_list_2).contains(f) && !f.contains("not")) {
									features_k.add(f);
								}
							}
//							System.out.println(features_k);
							double similarity_score =  ConfigurationSimilarity(features_i, features_k, all_features);
							if (similarity_score == 1) {
								System.out.println(fsmFileName);
								File new_file = new File(configFile.toString().replace(".config", "_text.txt"));
								Files.copy(fsmFile, new_file);
								
								CompactMealy<String, Word<String>> mealy_1 = LoadMealy(fsmFile);
								// save fsm.dot
								String s_fsm = configFile.toString().replace(".config", "_fsm.dot");
								BufferedWriter bw = new BufferedWriter(new FileWriter(s_fsm));
								GraphDOT.write(mealy_1, bw);
								bw.close();
								break;
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

	private static double ConfigurationSimilarity(List<String> features_i_1, List<String> features_j_1,
			List<String> all_features_1) {
		// TODO Auto-generated method stub
		List<String> intersection_i_j = Intersection(features_i_1, features_j_1);

		List<String> all_minus_i = Difference(all_features_1, features_i_1);

		List<String> all_minus_j = Difference(all_features_1, features_j_1);

		List<String> intersection_remained = Intersection(all_minus_i, all_minus_j);

		double similarity = (intersection_i_j.size() + intersection_remained.size()) / ((double) all_features_1.size());
//		System.out.println(similarity);

		return similarity;
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

	private static List<String> Difference(List<String> list_1, List<String> list_2) {
		List<String> difference_list = new ArrayList<>();
		for (String f : list_1) {
			if (!list_2.contains(f)) {
				difference_list.add(f);
			}
		}
		return difference_list;
	}

	private static String getFeatureName(String featureName_1, String[] feature_list_1, String[] feature_list_2) {
		// TODO Auto-generated method stub
		String featureName_2 = "";
		for (int i = 0; i < feature_list_1.length; i++) {
			if (feature_list_1[i].equals(featureName_1)) {
				featureName_2 = feature_list_2[i];
			}
		}
		return featureName_2;
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
		options.addOption(HELP, false, "Help menu");
		options.addOption(FSM, true, "Directory containing all FSMs (FSMs of all possible products)");
		options.addOption(DIR, true, "Directory of the config files");
		options.addOption(FM, true, "Feature model");
		options.addOption(PNAME, true, "Project name");
		return options;
	}

}
