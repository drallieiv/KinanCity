package com.kinancity.core.creation;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class PtcCreationSummary {

	private static final String LOG_FORMAT = "%s/%s account created in %s";

	private List<PtcCreationResult> results;

	private String errorMsg;

	// Total generation time in seconds
	private long duration;

	public PtcCreationSummary(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public PtcCreationSummary() {
		results = new ArrayList<>();
	}

	public void add(PtcCreationResult result) {
		results.add(result);
	}

	public long getNbSuccess() {
		return results.stream().filter(r -> r.isSuccess()).count();
	}

	public long getNbCreations() {
		return results.size();
	}

	public String toString() {
		if (errorMsg != null) {
			return errorMsg;
		}

		return String.format(LOG_FORMAT, getNbSuccess(), getNbCreations(), getTimeInText());
	}

	private String getTimeInText() {

		StringBuilder sb = new StringBuilder();
		long nbHours = Math.floorDiv(duration, 3600);
		if (nbHours > 0) {
			sb.append(nbHours).append("h ");
		}

		long nbMinutes = Math.floorDiv(duration % 3600, 60);
		if (nbHours > 0) {
			sb.append(nbMinutes).append("m ");
		}

		long nbSeconds = duration % 60;
		sb.append(nbSeconds).append("s ");

		return sb.toString();
	}

	public void setDuration(LocalTime startTime, LocalTime endTime) {
		duration = ChronoUnit.SECONDS.between(startTime, endTime);
	}
}
