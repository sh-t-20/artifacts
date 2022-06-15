package ir.ac.ut.fml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.serialization.dot.GraphDOT;
import net.automatalib.words.Word;
import uk.le.ac.ffsm.FeaturedMealyUtils;
import uk.le.ac.fts.FtsUtils;
import be.vibes.ts.FeaturedTransitionSystem;
import be.vibes.ts.SimpleProjection;
import be.vibes.ts.TransitionSystem;
import be.vibes.ts.io.dot.TransitionSystemDotPrinter;
import be.vibes.ts.io.xml.XmlLoaders;
import be.vibes.fexpression.configuration.SimpleConfiguration;
import net.automatalib.util.automata.minimizer.hopcroft.HopcroftMinimization;

public class CreateFSMs {

	// Creates FSMs for all configurations available in a folder using the FTS file
	private static final String FTS = "fts";
	private static final String HELP = "h";
	public static final String DIR = "dir";

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

			if (line.hasOption(HELP) || !line.hasOption(FTS) || !line.hasOption(DIR)) {
				formatter.printHelp("CreateFSMs", options);
				System.exit(0);
			}
			
			String s_fts =
					// "Benchmark_SPL/minepump/fts/minepump.fts" ;
					line.getOptionValue(FTS);
			
			// load the fts
			File f_fts = new File(s_fts);
			FeaturedTransitionSystem fts = XmlLoaders.loadFeaturedTransitionSystem(f_fts);
			System.out.println(fts.getStatesCount());
			
			File configs_dir = new File(line.getOptionValue(DIR));
			
			File[] filesList = configs_dir.listFiles();
			for (int i = 0; i < filesList.length; i++) {
				File configFile = filesList[i];
				String config = configFile.getPath();
				
				String fileExtension = "";
				String fileName = configFile.getName();
				int j = fileName.lastIndexOf('.');
				if (j >= 0) { fileExtension = fileName.substring(j+1); }
				
				if (fileExtension.equals("config")) {
//					System.out.println(config);
					SimpleConfiguration product = FtsUtils.getInstance().loadConfiguration(config);
					TransitionSystem lts = SimpleProjection.getInstance().project(fts, product);
					CompactMealy<String, Word<String>> mealy = FtsUtils.getInstance().lts2fsm(lts);
					mealy = HopcroftMinimization.minimizeMealy(mealy);
					
					String r = "." + fileExtension + "$";
					
					// save lts.dot
					String s_lts = config.replaceFirst(r, "_lts.dot");
					new TransitionSystemDotPrinter(lts, new PrintStream(new File(s_lts))).printDot();
	
					// save fsm.dot
					String s_fsm = config.replaceFirst(r, "_fsm.dot");
					BufferedWriter bw = new BufferedWriter(new FileWriter(s_fsm));
					GraphDOT.write(mealy, bw);
					bw.close();
	
					// save .txt file
					String s_text = config.replaceFirst(r, "_text.txt");
					String header = FtsUtils.getInstance().simpleConfigurationToString(product);
					FeaturedMealyUtils.getInstance().saveFSM_kiss(mealy, new File(s_text), header);
				 }
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Finished!");
	}

	private static Options createOptions() {
		Options options = new Options();
		options.addOption(FTS, true, "Featured transition system");
		options.addOption(HELP, false, "Help menu");
		options.addOption(DIR, true, "Directory of the config files");
		return options;
	}
}
