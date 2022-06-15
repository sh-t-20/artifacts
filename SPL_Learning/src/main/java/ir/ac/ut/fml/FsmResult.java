package ir.ac.ut.fml;

import java.util.ArrayList;
import java.util.List;

import net.automatalib.automata.transducers.impl.compact.CompactMealy;

public class FsmResult {

	private List<CompactMealy> fsm_list = new ArrayList<>();
	private List<String> fsm_name_list = new ArrayList<>();
	
	public List<CompactMealy> getFsm_list() {
		return fsm_list;
	}
	public void setFsm_list(List<CompactMealy> fsm_list) {
		this.fsm_list = fsm_list;
	}
	public List<String> getFsm_name_list() {
		return fsm_name_list;
	}
	public void setFsm_name_list(List<String> fsm_name_list) {
		this.fsm_name_list = fsm_name_list;
	}
	
}
