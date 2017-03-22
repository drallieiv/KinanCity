package com.kinancity.core.creation;

import java.util.ArrayList;
import java.util.List;

public class PtcCreationSummary {

	private static final String LOG_FORMAT = "%s/%s account created";
	
	private List<PtcCreationResult> results;
	
	private String errorMsg;

	public PtcCreationSummary(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	
	public PtcCreationSummary() {
		results = new ArrayList<>();
	}

	public void add(PtcCreationResult result){
		results.add(result);
	}

	public long getNbSuccess() {
		return results.stream().filter(r -> r.isSuccess()).count();
	}

	public long getNbCreations() {
		return results.size();
	}

	public String toString() {
		if(errorMsg != null){
			return errorMsg;
		}
		return String.format(LOG_FORMAT, getNbSuccess(), getNbCreations());
	}
}
