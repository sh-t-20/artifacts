package br.usp.icmc.labes.mealyInference.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


import br.usp.icmc.labes.mealyInference.Infer_LearnLib;
import de.learnlib.datastructure.observationtable.DynamicDistinguishableStates;
import de.learnlib.datastructure.observationtable.GenericObservationTable;
import de.learnlib.datastructure.observationtable.ObservationTable;
import de.learnlib.datastructure.observationtable.Row;
import de.learnlib.util.statistics.SimpleProfiler;
import de.learnlib.algorithms.lstar.ce.ObservationTableCEXHandlers;
import de.learnlib.algorithms.lstar.closing.ClosingStrategies;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealy;
import de.learnlib.algorithms.lstar.mealy.ExtensibleLStarMealyBuilder;
import de.learnlib.api.SUL;
import de.learnlib.api.logging.LearnLogger;
import de.learnlib.api.oracle.MembershipOracle;
import net.automatalib.automata.transducers.impl.compact.CompactMealy;
import net.automatalib.commons.util.comparison.CmpUtil;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

public class OTUtils {


	private static final String WORD_DELIMITER = ";";
	private static final String SYMBOL_DELIMITER = ",";

	private static OTUtils instance;

	public OTUtils() { }

	public static OTUtils getInstance() {

		if(instance==null) instance = new OTUtils();

		return instance;
	}

	public void writeOT(ObservationTable<String, Word<Word<String>>> observationTable, File fout) throws IOException{

		FileWriter fw = new FileWriter(fout);

		StringBuilder sb = new StringBuilder();

		// write short prefixes
		sb.delete(0, sb.length());
		for (Word<String> word : observationTable.getShortPrefixes()) {
			if(!word.isEmpty()){
				for (int i = 0; i < word.length(); i++) {
					sb.append(word.getSymbol(i).toString());
					if(i!=word.length()-1) sb.append(SYMBOL_DELIMITER);
				}
			}
			sb.append(WORD_DELIMITER);
		}
//		fw.write(sb.toString());
//
//		// write long prefixes
//		sb.delete(0, sb.length());
//		for (Word<String> word : observationTable.getLongPrefixes()) {
//			if(!word.isEmpty()){
//				for (int i = 0; i < word.length(); i++) {
//					sb.append(word.getSymbol(i).toString());
//					if(i!=word.length()-1) sb.append(SYMBOL_DELIMITER);
//				}
//			}
//			sb.append(WORD_DELIMITER);
//		}
		sb.deleteCharAt(sb.length()-1);
		sb.append('\n');
		fw.write(sb.toString());

		// write suffixes
		sb.delete(0, sb.length());
		for (Word<String> word : observationTable.getSuffixes()) {
			if(!word.isEmpty()){
				for (int i = 0; i < word.length(); i++) {
					sb.append(word.getSymbol(i).toString());
					if(i!=word.length()-1) sb.append(SYMBOL_DELIMITER);
				}
			}
			sb.append(WORD_DELIMITER);
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append('\n');
		fw.write(sb.toString());
		fw.close();

	}
	
	public void writeOT(MyObservationTable observationTable, File fout) throws IOException{

		FileWriter fw = new FileWriter(fout);

		StringBuilder sb = new StringBuilder();

		// write prefixes
		sb.delete(0, sb.length());
		for (Word<String> word : observationTable.getPrefixes()) {
			if(!word.isEmpty()){
				for (int i = 0; i < word.length(); i++) {
					sb.append(word.getSymbol(i).toString());
					if(i!=word.length()-1) sb.append(SYMBOL_DELIMITER);
				}
			}
			sb.append(WORD_DELIMITER);
		}
		fw.write(sb.toString());

		// write suffixes
		sb.delete(0, sb.length());
		for (Word<String> word : observationTable.getSuffixes()) {
			if(!word.isEmpty()){
				for (int i = 0; i < word.length(); i++) {
					sb.append(word.getSymbol(i).toString());
					if(i!=word.length()-1) sb.append(SYMBOL_DELIMITER);
				}
			}
			sb.append(WORD_DELIMITER);
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append('\n');
		fw.write(sb.toString());
		fw.close();

	}

	public MyObservationTable readOT(File fin, Alphabet<String> abc) throws IOException{
		return readOT(fin, abc, false);		
	}
	public MyObservationTable readOT(File fin, Alphabet<String> abc, boolean projection) throws IOException{
		Map<String, String>  nameToSymbol  = generateNameToSymbolMap(abc); 

		Map<String,Word<String>> suf = new LinkedHashMap<>();
		List<Word<String>> pref= new ArrayList<>();

		BufferedReader fr = new BufferedReader(new FileReader(fin));

		String line = fr.readLine();
		boolean add;

		if(!line.isEmpty()){
			String[] words = line.split(WORD_DELIMITER);
			for (String prefixWord : words) {
				String[] symbolNames = prefixWord.split(SYMBOL_DELIMITER);
				Word<String> word = Word.epsilon();
				add=false;
				if (!prefixWord.isEmpty()) {
					for (String symbolName : symbolNames) {
						if(!nameToSymbol.containsKey(symbolName)){
							add = false;
							break;							
						}else{
							word = word.append(nameToSymbol.get(symbolName));
						}
						add = true;
					}
				}
				if(add) pref.add(word);
			}
		}

		line = fr.readLine();
		if(!line.isEmpty()){
			String[] words = line.split(WORD_DELIMITER);
			for (String suffixWord : words) {
				String[] symbolNames = suffixWord.split(SYMBOL_DELIMITER);
				Word<String> word = Word.epsilon();
				add = true;
				if (!suffixWord.isEmpty()) {
					for (String symbolName : symbolNames) {
						if(!nameToSymbol.containsKey(symbolName)){
							add = false;
							if(projection) {
								continue; // ignore invalid symbols
							}else {
								break; // trunk suffix at first invalid symbol
							}
						}
						word = word.append(nameToSymbol.get(symbolName));
						
					}
				}
				if(add&&!word.isEmpty()) suf.put(word.toString(),word);
			}
		}
		fr.close();

		MyObservationTable my_ot = new MyObservationTable(pref, suf.values());

		return my_ot;
	}

	private Map<String, String> generateNameToSymbolMap(Alphabet<String> abc) {
		Map<String, String> nameToSymbol = new HashMap<>(abc.size());

		for (String symbol : abc) {
			String symbolName = symbol.toString();
			if (nameToSymbol.containsKey(symbolName)) {
				throw new IllegalArgumentException(
						"Symbol name '" + symbolName + "' is used more than once in alphabet");
			}
			else {
				nameToSymbol.put(symbolName, symbol);
			}
		}

		return nameToSymbol;
	}
	
	private Map<String, Word<String>> generateNameToWordMap(List<? extends Word<String>> list) {
		Map<String, Word<String>> nameToSymbol = new HashMap<String, Word<String>>(list.size());

		for (Word<String> word : list) {
			String symbolName = word.toString();
			if (nameToSymbol.containsKey(symbolName)) {
				throw new IllegalArgumentException(
						"Symbol name '" + symbolName + "' is used more than once in alphabet");
			}
			else {
				nameToSymbol.put(symbolName, word);
			}
		}

		return nameToSymbol;
	}

	public ObservationTable<String, Word<Word<String>>> revalidateObservationTable(MyObservationTable myot, MembershipOracle<String, Word<Word<String>>>  oracle, CompactMealy<String, Word<String>> mealyss){
		return revalidateObservationTable(myot, oracle, mealyss, false);
	}
	
	public ObservationTable<String, Word<Word<String>>> revalidateObservationTable(MyObservationTable myot, MembershipOracle<String, Word<Word<String>>>  oracle, CompactMealy<String, Word<String>> mealyss, boolean usingLearner){
			LearnLogger logger = LearnLogger.getLogger(Infer_LearnLib.class);
			
			logger.logEvent("revalidateOT2: Begin");
			
			ObservationTable<String, Word<Word<String>>> gen_ot = null;
			
			if(usingLearner) {
				gen_ot = revalidateUsingLearner(mealyss, oracle, myot);
			}else {
				gen_ot = revalidateUsingOT(mealyss, oracle, myot);
			}
			
			
			List<Word<String>> t_initialShortPrefixes = new ArrayList<>();
			
			for (Row<String> row : gen_ot.getShortPrefixRows()) {
				t_initialShortPrefixes.add(row.getLabel());
			}

			// sort S_M for having EMPTY string at the first position and help on discarding redundant prefixes
	        Alphabet<String> abc = mealyss.getInputAlphabet();
			Collections.sort(t_initialShortPrefixes, new Comparator<Word<String>>() {
	            @Override
	            public int compare(Word<String> o1, Word<String> o2) { return CmpUtil.lexCompare(o1, o2, abc); }
	        });
			
			
			logger.logEvent("revalidateOT2: Started to search well-formed cover set");
			
			// set of observed outputs (helps to identify states reached using other prefixes)
	        Set<List<Word<Word<String>>>> t_observedOutputs = new HashSet<>(gen_ot.getShortPrefixes().size() * gen_ot.getSuffixes().size());
	        
	        // list to keep the outputs of each row for each query posed
	        List<Word<Word<String>>> t_outputs = null;
	        
	        // outputs obtained for all short rows included at the well-formed cover subset
	        Map<Word<String>,List<Word<Word<String>>>> observationMap = new HashMap<>();
	        
	        // PASS 1: Gradually add short prefix rows while finding an well-formed cover subset from initialSuffixes
	        for (int i = 0; i < t_initialShortPrefixes.size(); i++) {
				// row to be checked
	        	Word<String> sp = t_initialShortPrefixes.get(i);
	        	Row<String> row = gen_ot.getRow(sp);

	        	// new t_outputs to be included at the observationMap
	        	// concatenate outputs to compare with those previously observed
	        	t_outputs = new ArrayList<>(gen_ot.rowContents(row));

	        	// if NOT observed previously
	        	if(!t_observedOutputs.contains(t_outputs)){
					// Finally add sp to the set of short prefixes S_M
	        		observationMap.put(row.getLabel(),t_outputs);
	        		t_observedOutputs.add(t_outputs);
	        	}else if(i < t_initialShortPrefixes.size()){
	        		while (sp.isPrefixOf(t_initialShortPrefixes.get(i))){
	        			i++;
	        			if(i == t_initialShortPrefixes.size())  break;
	        		}
	        		i--;
	        	}
	        }
			logger.logEvent("revalidateOT2: Ended to search well-formed cover set");

			logger.logEvent("revalidateOT2: Started to copy well-formed cover set");
			myot.getPrefixes().clear();
			for (Word<String> key : t_initialShortPrefixes) {
				if(observationMap.keySet().contains(key)){
					myot.getPrefixes().add(key);
				}
			}
			logger.logEvent("revalidateOT2: Ended to copy well-formed cover set");

			
			logger.logEvent("revalidateOT2: Started to search experiment cover set");
			// find experiment cover
			List<Word<String>> suffixes = new ArrayList<>(gen_ot.getSuffixes());
			List<Integer> experimentCover = findExperimentCover(observationMap, suffixes,gen_ot);
			logger.logEvent("revalidateOT2: Ended to search experiment cover set");
			
			logger.logEvent("revalidateOT2: Started to copy experiment cover set");
			myot.getSuffixes().clear();
			for (Integer key : experimentCover) {
				myot.getSuffixes().add(suffixes.get(key));			
			}
			logger.logEvent("revalidateOT2: Ended to copy experiment cover set");
			

			logger.logEvent("revalidateOT2: End");
			return gen_ot;
		}

	private ObservationTable<String, Word<Word<String>>> revalidateUsingOT(CompactMealy<String, Word<String>> mealyss,
			MembershipOracle oracle, MyObservationTable myot) {
		
		LearnLogger logger = LearnLogger.getLogger(Infer_LearnLib.class);
		
		logger.logEvent("revalidate using GenericObservationTable: Begin");
		// Q: Why revalidate observation table using GenericObservationTable ?
		// A: It does not perform the steps for making the OT closed and consistent which are not required!!!
		GenericObservationTable<String, Word<Word<String>>> the_ot = new GenericObservationTable<>(mealyss.getInputAlphabet());
		
		SimpleProfiler.start("Learning");
		the_ot.initialize(myot.getPrefixes(), myot.getSuffixes(), oracle);
		SimpleProfiler.stop("Learning");
		logger.logEvent("revalidate using GenericObservationTable: Stop learning");
		//new ObservationTableASCIIWriter<>().write(the_ot, System.out);

		return the_ot;
	}

	private ObservationTable<String, Word<Word<String>>> revalidateUsingLearner(CompactMealy<String, Word<String>> mealyss, MembershipOracle<String, Word<Word<String>>> oracle, MyObservationTable myot) {
		LearnLogger logger = LearnLogger.getLogger(Infer_LearnLib.class);
		
		logger.logEvent("revalidate using Learner: Begin");
		// Q: Why revalidate observation table using ExtensibleLStarMealy ?
		// A: The OT has to be *well-formed* and *long prefixes may reach new states* !!!
		ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
		builder.setAlphabet(mealyss.getInputAlphabet());
		builder.setOracle(oracle);
		builder.setInitialPrefixes(myot.getPrefixes());
		builder.setInitialSuffixes(myot.getSuffixes());
		builder.setCexHandler(ObservationTableCEXHandlers.RIVEST_SCHAPIRE);
		builder.setClosingStrategy(ClosingStrategies.CLOSE_FIRST);
		
		ExtensibleLStarMealy<String, Word<String>> learner = builder.create();
		
		SimpleProfiler.start("Learning");
		learner.startLearning();
		SimpleProfiler.stop("Learning");
		logger.logEvent("revalidate using Learner: Stop learning");
		//new ObservationTableASCIIWriter<>().write(learner.getObservationTable(), System.out);

		return learner.getObservationTable();
	}
	
	private ObservationTable revalidateUsingOT(SUL<String, Word<String>> sul_sim, Alphabet<String> alphabet, 
			MembershipOracle oracle, MyObservationTable myot) {
		
		LearnLogger logger = LearnLogger.getLogger(Infer_LearnLib.class);
		
		logger.logEvent("revalidate using GenericObservationTable: Begin");
		// Q: Why revalidate observation table using GenericObservationTable ?
		// A: It does not perform the steps for making the OT closed and consistent which are not required!!!
		GenericObservationTable<String, Word<String>> the_ot = new GenericObservationTable<>(alphabet);
		
		SimpleProfiler.start("Learning");
		the_ot.initialize(myot.getPrefixes(), myot.getSuffixes(), oracle);
		SimpleProfiler.stop("Learning");
		logger.logEvent("revalidate using GenericObservationTable: Stop learning");
		//new ObservationTableASCIIWriter<>().write(the_ot, System.out);

		return the_ot;
	}

	private ObservationTable<String, Word<Word<String>>> revalidateUsingLearner(SUL<String, Word<String>> sul_sim, Alphabet<String> alphabet, MembershipOracle<String, Word<Word<String>>> oracle, MyObservationTable myot) {
		LearnLogger logger = LearnLogger.getLogger(Infer_LearnLib.class);
		
		logger.logEvent("revalidate using Learner: Begin");
		// Q: Why revalidate observation table using ExtensibleLStarMealy ?
		// A: The OT has to be *well-formed* and *long prefixes may reach new states* !!!
		ExtensibleLStarMealyBuilder<String, Word<String>> builder = new ExtensibleLStarMealyBuilder<String, Word<String>>();
		builder.setAlphabet(alphabet);
		builder.setOracle(oracle);
		builder.setInitialPrefixes(myot.getPrefixes());
		builder.setInitialSuffixes(myot.getSuffixes());
		builder.setCexHandler(ObservationTableCEXHandlers.RIVEST_SCHAPIRE);
		builder.setClosingStrategy(ClosingStrategies.CLOSE_FIRST);
		
		ExtensibleLStarMealy<String, Word<String>> learner = builder.create();
		
		SimpleProfiler.start("Learning");
		learner.startLearning();
		SimpleProfiler.stop("Learning");
		logger.logEvent("revalidate using Learner: Stop learning");
		//new ObservationTableASCIIWriter<>().write(learner.getObservationTable(), System.out);

		return learner.getObservationTable();
	}

	// find the experiment cover set using an approach similar to that for synchronizing trees
    List<Integer> findExperimentCover(Map<Word<String>, List<Word<Word<String>>>> observationMap, List<Word<String>> suffixes, ObservationTable<String, Word<Word<String>>> gen_ot){

        List<Integer> out = new ArrayList<>();

        if(observationMap.keySet().size()==1){
            for (int i = 0; i < suffixes.size(); i++) {
                out.add(i);
            }
            return out;
        }

        // keeps the set of distinguished states and the suffixes used to do it
        List<DynamicDistinguishableStates<String,Word<Word<String>>>> toAnalyze = new ArrayList<>();

        // set of nodes found (used to find previously visited states)
        Set<Set<Set<Word<String>>>> nodesFound = new HashSet<>();

        // creates the first DynamicDistinguishableStates w/all states undistinguished
        Set<Set<Word<String>>> diff_states = new HashSet<>();
        diff_states.add(observationMap.keySet());

        // no suffixes applied
        Set<Integer> eSubset = new HashSet<>();

        toAnalyze.add(new DynamicDistinguishableStates<>(observationMap, diff_states, eSubset));

        // current DynamicDistinguishableStates analyzed ( singleton is kept here )
        DynamicDistinguishableStates<String, Word<Word<String>>> item = toAnalyze.get(0);

        // the DynamicDistinguishableStates with the 'best' subset of E
        DynamicDistinguishableStates<String, Word<Word<String>>> best = toAnalyze.get(0);

        // ExperimentCover.find: Analysis begin"
        while (!toAnalyze.isEmpty()) {
            item = toAnalyze.remove(0);

            // Does item distinguish the largest number of states ?
            if(item.getDistinguishedStates().size()>best.getDistinguishedStates().size()) {
                // then keep it as the best option
                best = item;
            }

            // ExperimentCover.find: Singleton found!
            if(item.isSingleton()) {
                break; // Thus, stop here!!! :)
            }

            // get number of suffixes
            for (int sufIdx = 0; sufIdx < suffixes.size(); sufIdx++){
                if(item.getESubset().contains(sufIdx)) {
                    continue; // suffix already applied to this item
                }

                // new subset of states that may be distinguished by 'sufIdx'
                diff_states = new HashSet<>();

                // subset of suffixes (potential experiment cover)
                eSubset = new HashSet<>();

                Set<Set<Word<String>>> setOfPrefixes = item.getDistinguishedStates();
                for (Set<Word<String>> prefixes : setOfPrefixes) {
                    // maps the outputs to rows (used for keeping states equivalent given 'sufIdx')
                    Map<Integer,Set<Integer>> out2Rows = new TreeMap<>();
                    // look 'sufIdx' for each prefix
                    for (Word<String> pref : prefixes) {
                        Word<Word<String>> outStr = gen_ot.cellContents(gen_ot.getRow(pref), sufIdx);
                        // if outStr is new, then add sufIdx as an useful suffix
                        if(out2Rows.putIfAbsent(outStr.hashCode(), new HashSet<Integer>()) == null){
                            eSubset.add(sufIdx);
                        }
						out2Rows.get(outStr.hashCode()).add(((List<Row<String>>)gen_ot.getShortPrefixRows()).indexOf(gen_ot.getRow(pref)));
                    }
                    // the subsets of states that are distinguished by 'sufIdx'
                    for (Set<Integer> sset: out2Rows.values()) {
                        Set<Word<String>> sset_word = new HashSet<>();
                        for (Integer sset_item: sset) {
                            sset_word.add(((List<Row<String>>)gen_ot.getShortPrefixRows()).get(sset_item).getLabel());
                        }
                        diff_states.add(sset_word);
                    }

                }
                // if diff_states was previously visited, then discard! :(
                if(nodesFound.contains(diff_states)) continue;
                nodesFound.add(diff_states); // otherwise keep it!
                // create a new de.learnlib.datastructure.observationtable.DynamicDistinguishableStates
                DynamicDistinguishableStates new_diststates = new DynamicDistinguishableStates(observationMap);
                new_diststates.setDistinguishedStates(diff_states);
                // add previously applied suffixes to eSubset (i.e., { eSubset \cup 'sufIdx'}
                eSubset.addAll(item.getESubset());
                new_diststates.setESubset(eSubset);
                // add it to be analyzed later
                toAnalyze.add(new_diststates);
            }
        }
        // ExperimentCover.find: Analysis end!


        if(item.isSingleton()){
            // if item is singleton then return its suffixes
            out.addAll(item.getESubset());
        }else{
            // otherwise add the 'best' subset of E
            out.addAll(best.getESubset());
        }
        return out;
    }

	public static class ExperimentCover{
		
		private static ExperimentCover instance;
		
		private ExperimentCover(){}
		
		public static ExperimentCover getInstance() {
			if(instance==null) {
				instance = new ExperimentCover();
			}
			return instance;
		}

		// find the experiment cover set using an approach similar to that for synchronizing trees
		Set<Word<String>> find(ObservationTable<String, Word<Word<String>>> observationTable, Set<Row<String>> wellFormedCover){
			
			LearnLogger logger = LearnLogger.getLogger(Infer_LearnLib.class);
			
			// DistinguishableStates keeps the set of distinguished states and the suffixes used to that
			List<DistinguishableStates> toAnalyze = new ArrayList<>();
			
			// set of nodes found (used to find previously visited states)
			Set<Set<Set<Row<String>>>> nodesFound = new HashSet<>();

			// creates the first DistinguishableStates
			Set<Set<Row<String>>> diff_states = new HashSet<>();
			// all states undistinguished
			diff_states.add(wellFormedCover); 
			// no suffixes applied
			Set<Integer> eSubset = new HashSet<>();
			toAnalyze.add(new DistinguishableStates(observationTable, diff_states, eSubset));
			
			// current DistinguishableStates analyzed ( singleton is kept here )
			DistinguishableStates item = toAnalyze.get(0);
			
			// the DistinguishableStates with the 'best' subset of E 
			DistinguishableStates best = toAnalyze.get(0); 
			
			logger.logEvent("ExperimentCover.find: Analysis begin");
			while (!toAnalyze.isEmpty()) {
				item = toAnalyze.remove(0);
				if(item.getDistinguishedStates().size()>best.getDistinguishedStates().size()) {
					// (i.e., distinguish the largest number of states)
					best = item;
				}
				if(item.isSingleton()) {
					logger.logEvent("ExperimentCover.find: Singleton found!");
					break; // if is singleton stops here!!! :)
				}
				
				for (int sufIdx = 0; sufIdx < observationTable.getSuffixes().size(); sufIdx++){
					if(item.getESubset().contains(sufIdx)) {
						// suffix already applied at this item
						continue;
					}
					// new subset of states that may be distinguished by 'sufIdx' 
					diff_states = new HashSet<>(); eSubset = new HashSet<>();
					for (Set<Row<String>> prefixes : item.getDistinguishedStates()) {
						// maps the outputs to rows (used for keeping states equivalent given 'sufIdx') 
						Map<String,Set<Row<String>>> out2Rows = new TreeMap<>();
						// look 'sufIdx' for each prefix
						for (Row<String> pref : prefixes) {
							String outStr = observationTable.cellContents(pref, sufIdx).toString();
							// if outStr is new, then add sufIdx as an useful suffix
							if(out2Rows.putIfAbsent(outStr, new HashSet<>()) == null){
								eSubset.add(sufIdx);
							}
							out2Rows.get(outStr).add(pref);
						}
						// the subsets of states that are distinguished by 'sufIdx'
						diff_states.addAll(out2Rows.values());
					}
					// if diff_states was previously visited, then discard! :(
					if(nodesFound.contains(diff_states)) continue;
					nodesFound.add(diff_states); // otherwise keep it!
					// create a new DistinguishableStates 
					DistinguishableStates new_diststates = new DistinguishableStates(observationTable);
					new_diststates.setDistinguishedStates(diff_states);
					// add previously applied suffixes to eSubset (i.e., { eSubset \cup 'sufIdx'}  
					eSubset.addAll(item.getESubset());
					new_diststates.setESubset(eSubset);
					// add it to be analyzed later 
					toAnalyze.add(new_diststates);
				}
			}
			logger.logEvent("ExperimentCover.find: Analysis end");
			
			
			Set<Word<String>> out = new HashSet<>();
			if(item.isSingleton()){ // if item is singleton then return its suffixes
				for (Integer e_el : item.getESubset()) {
					out.add(observationTable.getSuffix(e_el));
				}
				
			}else{ // otherwise add the 'best' subset of E
				for (Integer e_el : best.getESubset()) {
					out.add(observationTable.getSuffix(e_el));
				}
			}
			return out;
		}
		
		public class DistinguishableStates{
			private ObservationTable<String, Word<Word<String>>> observationTable;
			private Set<Set<Row<String>>> distinguishedStates;
			private Set<Integer> eSubset;
			private boolean isSingleton;
			
			public DistinguishableStates(ObservationTable<String, Word<Word<String>>> ot, Set<Set<Row<String>>> states, Set<Integer> esubset) {
				this.observationTable = ot;
				this.distinguishedStates = states;
				this.eSubset = esubset;
				this.isSingleton = false;
				for (Set<Row<String>> set : this.distinguishedStates) {
					if(set.size()!=1){
						return;
					}
				}
				this.isSingleton = true;
			}
			
			public DistinguishableStates(ObservationTable<String, Word<Word<String>>> ot) {
				this.observationTable = ot;
			}
			
			public Set<Set<Row<String>>> getDistinguishedStates() {
				return distinguishedStates;
			}
			
			public Set<Integer> getESubset() {
				return eSubset;
			}
			
			public ObservationTable<String, Word<Word<String>>> getObservationTable() {
				return observationTable;
			}
			public boolean isSingleton() {
				return isSingleton;
			}
			
			public void setDistinguishedStates(Set<Set<Row<String>>> states) {
				this.distinguishedStates = states;
				for (Set<Row<String>> set : this.distinguishedStates) {
					if(set.size()!=1){
						return;
					}
				}
				this.isSingleton = true;
			}
			public void setESubset(Set<Integer> eSubset) {
				this.eSubset = eSubset;
			}
			@Override
			public boolean equals(Object obj) {
				if(obj !=null && obj instanceof DistinguishableStates){
					return this.distinguishedStates.equals(((DistinguishableStates) obj).distinguishedStates);
				}
				return false;
			}
			
			@Override
			public int hashCode() {
				StringBuffer sb = new StringBuffer(observationTable.toString().length());
				sb.append("{");
				for (Set<Row<String>> set : distinguishedStates) {
					sb.append("{");
					for (Row<String> row : set) {
						sb.append(row.getRowId());
						sb.append(",");
					}
					sb.append("}");
				}
				sb.append("}");
				return sb.toString().hashCode();
			}
			@Override
			public String toString() {
				StringBuffer sb = new StringBuffer(observationTable.toString().length());
				sb.append(observationTable.toString());
				sb.append('\n');
				sb.append("Distinguished states:\n");
				for (Set<Row<String>> set : distinguishedStates) {
					for (Row<String> row : set) {
						sb.append('\t');
						sb.append(row.getRowId());
					}
					sb.append('\n');
				}
				sb.append("Columns:");
				for (Integer intVal : eSubset) {
					sb.append('\t');
					sb.append(intVal);
				}
				sb.append('\n');
				return sb.toString();
			}
		}
		
	}
	
}