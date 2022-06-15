package br.usp.icmc.labes.mealyInference.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import net.automatalib.words.Word;

public class MyObservationTable{

	private List<Word<String>> prefixes; // S + SA
	private List<Word<String>> suffixes;  // E
	
	
	public MyObservationTable() {
		prefixes = new ArrayList<>();
		prefixes.add(Word.epsilon());
		
		suffixes = new ArrayList<>();
	}
	
	
	public MyObservationTable(Collection<Word<String>> pref, Collection<Word<String>> suf) {
		prefixes = new ArrayList<>();
		prefixes.add(Word.epsilon());
		
		for (Word<String> word : pref) {
			if(!word.isEmpty()){
				prefixes.add(word);
			}
		}
		
		suffixes = new ArrayList<>();
		for (Word<String> word : suf) {
			suffixes.add(word);
		}		 
	}
	
	public List<Word<String>> getPrefixes() {
		return prefixes;
	}
	
	public List<Word<String>> getSuffixes() {
		return suffixes;
	}
	
	@Override
	public String toString() {
		return "Prefixes ("+prefixes.size()+"):\t"+prefixes.toString()+"\n"+"Suffixes ("+suffixes.size()+"):\t"+suffixes.toString();
	}
}
