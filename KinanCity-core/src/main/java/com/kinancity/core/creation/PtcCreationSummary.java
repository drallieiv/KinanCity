package com.kinancity.core.creation;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class PtcCreationSummary {

	private static final String LOG_FORMAT = "%s/%s account created in %s";

	private List<PtcCreationResult> results;

	private String errorMsg;

	private LocalTime startTime;
	private LocalTime endTime;

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
		return results.stream().filter(r -> ! r.isRescheduled()).count();
	}

	public String toString() {
		if (errorMsg != null) {
			return errorMsg;
		}

		return String.format(LOG_FORMAT, getNbSuccess(), getNbCreations(), getTimeInText());
	}

	private String getTimeInText() {

		StringBuilder sb = new StringBuilder();
		LocalDateTime tempDateTime = LocalDateTime.from(startTime);

		long hours = tempDateTime.until(endTime, ChronoUnit.HOURS);
		tempDateTime = tempDateTime.plusHours(hours);

		long minutes = tempDateTime.until(endTime, ChronoUnit.MINUTES);
		tempDateTime = tempDateTime.plusMinutes(minutes);

		long seconds = tempDateTime.until(endTime, ChronoUnit.SECONDS);

		if (hours > 0) {
			sb.append(hours).append("h ");
		}

		if (minutes > 0) {
			sb.append(minutes).append("m ");
		}

		sb.append(seconds).append("s ");

		return sb.toString();
	}

	public void setDuration(LocalTime startTime, LocalTime endTime) {
		this.startTime = startTime;
		this.endTime = endTime;

	}
}
