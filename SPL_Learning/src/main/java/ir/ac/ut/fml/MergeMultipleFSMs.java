package ir.ac.ut.fml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.checkerframework.checker.nullness.qual.Nullable;

import be.vibes.fexpression.Feature;
import be.vibes.fexpression.configuration.SimpleConfiguration;
import br.usp.icmc.labes.mealyInference.utils.Utils;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.automata.transducers.impl.compact.CompactMealyTransition;
import net.automatalib.commons.util.Pair;
import net.automatalib.serialization.InputModelData;
import net.automatalib.serialization.InputModelDeserializer;
import net.automatalib.serialization.dot.DOTParsers;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.visualization.VisualizationHelper.EdgeAttrs;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;
import net.automatalib.words.impl.Alphabets;
import uk.le.ac.fts.FtsUtils;

public class MergeMultipleFSMs {

	public static final String DIR = "dir";
	public static final String DIR2 = "dir2";
	public static final String OUT = "out";
	private static final String FM = "fm";

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

			File configs_dir = new File(line.getOptionValue(DIR));
			File fsm_dir = new File(line.getOptionValue(DIR2));
			File output_dir = new File(line.getOptionValue(OUT));

			File fm_file = new File(line.getOptionValue(FM));
			IFeatureModel feature_m = FeatureModelManager.load(fm_file.toPath()).getObject();
			List<String> all_model_features = new ArrayList<>();
			for (IFeature node : feature_m.getFeatures()) {
				if (node.getName().equals("TRUE"))
					continue;
				all_model_features.add(node.getName());
			}

			System.out.println("all features:" + all_model_features);
			String[] all_features_array = new String[all_model_features.size()];
			all_model_features.toArray(all_features_array);
			System.out.println("all features:" + Arrays.toString(all_features_array));

			File[] filesList = configs_dir.listFiles();
			for (int i = 0; i < filesList.length; i++) {
				File configFile = filesList[i];
				String config = configFile.getPath();

				String fileExtension = "";
				String fileName = configFile.getName();
				int j = fileName.lastIndexOf('.');
				if (j >= 0) {
					fileExtension = fileName.substring(j + 1);
				}

				if (fileExtension.equals("config")) {
					SimpleConfiguration config_i = FtsUtils.getInstance().loadConfiguration(config);

					Feature[] config_i_features = config_i.getFeatures();
					List<String> features_i = new ArrayList<>();
					for (Feature f : config_i_features) {
						features_i.add(f.toString());
					}

					System.out.println("\ni: " + i);
					System.out.println(features_i);

					List<CompactMealy> fsm_list = new ArrayList<>();
					List<String> fsm_name_list = new ArrayList<>();

					FsmResult fsm_result = CreateFSM_List_Basic(features_i, fsm_dir, all_features_array);
					fsm_list = fsm_result.getFsm_list();
					fsm_name_list = fsm_result.getFsm_name_list();
					System.out.println("FSM list size:" + fsm_list.size());
					System.out.println("FSM name list:" + fsm_name_list);

					if (fsm_list.size() < 11) {
						CompactMealy<String, Word<String>> m_1 = (CompactMealy<String, Word<String>>) fsm_list
								.toArray()[0];
						for (int k = 1; k < fsm_list.size(); k++) {
							CompactMealy<String, Word<String>> m_2 = (CompactMealy<String, Word<String>>) fsm_list
									.toArray()[k];
							CompactMealy<String, Word<String>> new_mealy = MergeFSMs(m_1, m_2);
							System.out.println(new_mealy.getStates().size());
							m_1 = new_mealy;
							if (k == (fsm_list.size() - 1)) {
								String fixedLengthString = convertTofixedLengthString(i + 1);
								File fsm_file = new File(output_dir, (fixedLengthString + "_fsm.dot"));
								BufferedWriter bw = new BufferedWriter(new FileWriter(fsm_file));
								GraphDOT.write(new_mealy, bw);
								bw.close();
							}
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getStackTrace()[0].getLineNumber());
		}

		System.out.println("\nFinished!");

	}

	private static CompactMealy<String, Word<String>> MergeFSMs(CompactMealy<String, Word<String>> mealy_1,
			CompactMealy<String, Word<String>> mealy_2) {
		// TODO Auto-generated method stub

		Collection<Integer> states_1 = mealy_1.getStates();
		Collection<Integer> states_2 = mealy_2.getStates();

		Alphabet<String> alphabet_1 = mealy_1.getInputAlphabet();
		Alphabet<String> alphabet_2 = mealy_2.getInputAlphabet();

		// Creating the alphabet of the merged FSM
		Alphabet<String> alphabet = Alphabets.fromCollection(MergeAlphabet(alphabet_1, alphabet_2));
//		System.out.println("alphabet: " + alphabet.toString());
		CompactMealy<String, Word<String>> mealy = new CompactMealy(alphabet);
//		System.out.println(mealy.getStates());

		int states_num = states_1.size() * states_2.size();
		int states_map[][] = new int[states_num][3];
		for (int[] array : states_map) {
			Arrays.fill(array, -1);
		}
		int merged_state = 0;
		states_map[merged_state][0] = states_map[merged_state][1] = states_map[merged_state][2] = 0;

		Queue<Integer> states_queue = new LinkedList<>();
		states_queue.add(merged_state);

		while (states_queue.size() != 0) {
			int current_state = states_queue.remove();
			mealy.addState();
//			System.out.println("\ncurrent state: " + current_state);
			int s_1 = states_map[current_state][1];
			int s_2 = states_map[current_state][2];
//			System.out.println("s_1: " + s_1);
//			System.out.println("s_2: " + s_2);

			for (String a : alphabet) {
//				System.out.println(a);

				int new_s_1 = -10;
				int new_s_2 = -10;

				Word<String> output_1 = null;
				Word<String> output_2 = null;

				@Nullable
				CompactMealyTransition<Word<String>> transition_1 = null;
				@Nullable
				CompactMealyTransition<Word<String>> transition_2 = null;

				if (alphabet_1.contains(a)) {
					transition_1 = mealy_1.getTransition(s_1, a);
				}
				if (alphabet_2.contains(a)) {
					transition_2 = mealy_2.getTransition(s_2, a);
				}
				if (transition_1 != null || transition_2 != null) {
					if (transition_1 != null && transition_2 != null) {
						output_1 = transition_1.getOutput();
						new_s_1 = transition_1.getSuccId();

						output_2 = transition_2.getOutput();
						new_s_2 = transition_2.getSuccId();

					} else if (transition_1 != null && transition_2 == null) {
						output_1 = transition_1.getOutput();
						new_s_1 = transition_1.getSuccId();

						new_s_2 = s_2;

					} else if (transition_1 == null && transition_2 != null) {
						new_s_1 = s_1;

						output_2 = transition_2.getOutput();
						new_s_2 = transition_2.getSuccId();

					}

					int equivalent_state = EquivalentState(new_s_1, new_s_2, states_map);
//					System.out.println("equivalent state:" + equivalent_state);

					String output_1_string = "";
					String output_2_string = "";
					if (output_1 != null) {
						output_1_string = output_1.toString();
					}
					if (output_2 != null) {
						output_2_string = output_2.toString();
					}
					List<String> output_1_list = new ArrayList<String>(Arrays.asList(output_1_string.split(",")));
					List<String> output_2_list = new ArrayList<String>(Arrays.asList(output_2_string.split(",")));
					List<String> output_list = new ArrayList<>();
					for (String string_1 : output_1_list) {
						if (!string_1.equals("")) {
							output_list.add(string_1);
						}
					}

					for (String string_1 : output_2_list) {
						if (!string_1.equals("") && !output_list.contains(string_1)) {
							output_list.add(string_1);
						}
					}

//					System.out.println("output list:" + output_list);
					String output_string = String.join(",", output_list);
					Word<String> output = Word.fromSymbols(output_string);
//					System.out.println("output:" + output);

					if (equivalent_state == -1) {
						merged_state += 1;
						mealy.setTransition(current_state, a, merged_state, output);
						states_queue.add(merged_state);
						states_map[merged_state][0] = merged_state;
						states_map[merged_state][1] = new_s_1;
						states_map[merged_state][2] = new_s_2;
					} else {
						mealy.setTransition(current_state, a, equivalent_state, output);
					}

//					System.out
//							.println(current_state + " " + a + "/ " + mealy.getTransition(current_state, a).getOutput()
//									+ " " + mealy.getTransition(current_state, a).getSuccId() + "\n");
				}

			}

		}

		mealy.setInitialState(0);
//		System.out.println("\n" + Arrays.deepToString(states_map));

		return mealy;
	}

	private static FsmResult CreateFSM_List_Basic(List<String> features_1, File fsm_dir_1,
			String[] all_features_array_1) {
		// TODO Auto-generated method stub
		FsmResult fsm_result = new FsmResult();
		List<CompactMealy> fsm_list_1 = new ArrayList<>();
		List<String> fsm_name_list_1 = new ArrayList<>();
		if (features_1.contains("Finger_Protection")) {
			String file_name_1 = "4_3_BCS_FP" + ".dot";
			CompactMealy<String, Word<String>> mealy_1 = LoadMealy_2(fsm_dir_1, file_name_1);
			fsm_list_1.add(mealy_1);
			fsm_name_list_1.add(file_name_1);
		}
//		if (features_1.contains("LED_Exterior_Mirror")) {
//			String[] file_names = { "4_12_BCS_LED_EMT", "4_13_MCS_LED_EML", "4_14_BCS_LED_EMB", "4_15_BCS_LED_EMR" };
//			for (String f : file_names) {
//				String file_name_1 = f + ".dot";
//				CompactMealy<String, Word<String>> mealy_1 = LoadMealy_2(fsm_dir_1, file_name_1);
//				fsm_list_1.add(mealy_1);
//				fsm_name_list_1.add(file_name_1);
//			}
//		}
		if (features_1.contains("Manual_Power_Window") && features_1.contains("Automatic_Power_Window")) {
			String file_name_1 = "BCS_PW_4" + ".dot";
			CompactMealy<String, Word<String>> mealy_1 = LoadMealy_2(fsm_dir_1, file_name_1);
			fsm_list_1.add(mealy_1);
			fsm_name_list_1.add(file_name_1);
		} else {
			if (features_1.contains("Manual_Power_Window")) {
				String file_name_1 = "4_1_BCS_MPW" + ".dot";
				CompactMealy<String, Word<String>> mealy_1 = LoadMealy_2(fsm_dir_1, file_name_1);
				fsm_list_1.add(mealy_1);
				fsm_name_list_1.add(file_name_1);
			}
			if (features_1.contains("Automatic_Power_Window")) {
				String file_name_1 = "4_2_BCS_APW" + ".dot";
				CompactMealy<String, Word<String>> mealy_1 = LoadMealy_2(fsm_dir_1, file_name_1);
				fsm_list_1.add(mealy_1);
				fsm_name_list_1.add(file_name_1);
			}
		}
//		if (features_1.contains("LED_Alarm_System")) {
//			String[] file_names = { "4_17_BCS_LED_AS_Active", "4_18_BCS_LED_AS_Alarm", "4_19_BCS_LED_AS_Alarm_D" };
//			for (String f : file_names) {
//				String file_name_1 = f + ".dot";
//				CompactMealy<String, Word<String>> mealy_1 = LoadMealy_2(fsm_dir_1, file_name_1);
//				fsm_list_1.add(mealy_1);
//				fsm_name_list_1.add(file_name_1);
//			}
//		}
		if (features_1.contains("LED_Power_Window")) {
			String file_name_1 = "";
			if (features_1.contains("Manual_Power_Window")) {
				file_name_1 = "4_10_BCS_LED_MPW" + ".dot";
				CompactMealy<String, Word<String>> mealy_1 = LoadMealy_2(fsm_dir_1, file_name_1);
				fsm_list_1.add(mealy_1);
				fsm_name_list_1.add(file_name_1);
			}
			if (features_1.contains("Automatic_Power_Window")) {
				file_name_1 = "4_11_BCS_LED_APW" + ".dot";
				CompactMealy<String, Word<String>> mealy_1 = LoadMealy_2(fsm_dir_1, file_name_1);
				fsm_list_1.add(mealy_1);
				fsm_name_list_1.add(file_name_1);
			}

		}
		if (features_1.contains("Central_Locking_System")) {
			String file_name_1 = "4_6_BCS_CLS" + ".dot";
			CompactMealy<String, Word<String>> mealy_1 = LoadMealy_2(fsm_dir_1, file_name_1);
			fsm_list_1.add(mealy_1);
			fsm_name_list_1.add(file_name_1);
		}
		if (features_1.contains("LED_Finger_Protection")) {
			String file_name_1 = "4_5_BCS_LED_FP" + ".dot";
			CompactMealy<String, Word<String>> mealy_1 = LoadMealy_2(fsm_dir_1, file_name_1);
			fsm_list_1.add(mealy_1);
			fsm_name_list_1.add(file_name_1);
		}
//		if (features_1.contains("Exterior_Mirror")) {
//			String file_name_1 = "4_7_BCS_EM" + ".dot";
//			CompactMealy<String, Word<String>> mealy_1 = LoadMealy_2(fsm_dir_1, file_name_1);
//			fsm_list_1.add(mealy_1);
//			fsm_name_list_1.add(file_name_1);
//		}
		if (features_1.contains("LED_Central_Locking_System")) {
			String file_name_1 = "4_9_BCS_LED_CLS" + ".dot";
			CompactMealy<String, Word<String>> mealy_1 = LoadMealy_2(fsm_dir_1, file_name_1);
			fsm_list_1.add(mealy_1);
			fsm_name_list_1.add(file_name_1);
		}
		if (features_1.contains("Alarm_System")) {
			String file_name_1 = "4_21_BCS_AS" + ".dot";
			CompactMealy<String, Word<String>> mealy_1 = LoadMealy_2(fsm_dir_1, file_name_1);
			fsm_list_1.add(mealy_1);
			fsm_name_list_1.add(file_name_1);
		}

		fsm_result.setFsm_list(fsm_list_1);
		fsm_result.setFsm_name_list(fsm_name_list_1);

		return fsm_result;
	}

	private static String convertTofixedLengthString(int productIndex) {
		// TODO Auto-generated method stub
		String productIndexString = Integer.toString(productIndex);
		String fixedLengthString = "00000".substring(productIndexString.length()) + productIndexString;
		return fixedLengthString;
	}

	private static int EquivalentState(int s1, int s2, int[][] states_map_1) {
		// TODO Auto-generated method stub
		int length_1 = states_map_1.length;
		for (int i = 0; i < length_1; i++) {
			if (states_map_1[i][0] != (-1)) {
				if (states_map_1[i][1] == s1 && states_map_1[i][2] == s2) {
					return i;
				}
			}
		}
		return -1;
	}

	private static Collection MergeAlphabet(Alphabet<String> a_1, Alphabet<String> a_2) {
		// TODO Auto-generated method stub
		Set<String> set_1 = new LinkedHashSet<>();
		set_1.addAll(a_2);
		set_1.addAll(a_1);
		List<String> list_1 = new ArrayList<>(set_1);
		return list_1;
	}

	private static CompactMealy<String, Word<String>> LoadMealy_2(File fsm_dir, String fsm_name) {
		// TODO Auto-generated method stub
		File fsm_file = new File(fsm_dir, fsm_name);
		CompactMealy<String, Word<String>> mealy = LoadMealy(fsm_file);
		return mealy;
	}

//	private static CompactMealy<String, String> LoadMealy(File fsm_file) {
//		// TODO Auto-generated method stub
//		InputModelDeserializer<String, CompactMealy<String, String>> parser_1 = DOTParsers.mealy();
//		InputModelData<String, CompactMealy<String, String>> machine_1;
//		try {
//			machine_1 = parser_1.readModel(fsm_file);
//			CompactMealy<String, String> mealy = machine_1.model;
//			return mealy;
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return null;
//	}

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
		options.addOption(DIR, true, "Directory of the config files");
		options.addOption(DIR2, true, "Directory of the FSM files");
		options.addOption(OUT, true, "Set output directory");
		options.addOption(FM, true, "Feature model");
		return options;
	}

}
