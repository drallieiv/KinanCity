package com.kinancity.captcha.server;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.kinancity.captcha.server.dto.CaptchaJob;
import com.kinancity.captcha.server.errors.SolvingException;
import com.kinancity.captcha.server.errors.TaskNotFoundException;
import com.kinancity.core.captcha.antiCaptcha.dto.PtcCaptchaTask;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SolvingService {

	private Integer counter = 0;
	
	private int max = 10000000;

	private Map<Integer, CaptchaJob> jobs = new HashMap<>();

	public synchronized Integer addPtcCaptchaTask(PtcCaptchaTask task) {
		Integer taskId = nextAvailableJobId();
		CaptchaJob job = new CaptchaJob(taskId, task, null);
		jobs.put(taskId, job);
		return taskId;
	}

	public String getCaptchaFor(Integer taskId) throws SolvingException {
		CaptchaJob job = jobs.get(taskId);
		if (job == null) {
			throw new TaskNotFoundException();
		}
		return job.getSolution();
	}

	public void addSolutionToJob(Integer taskId, String solution) {
		jobs.get(taskId).setSolution(solution);
	}

	public void addSolutionToSite(String googleSiteKey, String solution) {
		CaptchaJob job = jobs.values().stream().filter(j -> j.getTask().getWebsiteKey().equals(googleSiteKey)).findFirst().orElse(null);
		if (job != null) {
			job.setSolution(solution);
		}
	}

	public void addSolution(String solution) {
		CaptchaJob job = jobs.values().stream().filter(j -> j.getSolution() == null).findFirst().orElse(null);
		if (job != null) {
			job.setSolution(solution);
		}
	}
	
	public long getNbJobsRemaining(){
		return jobs.values().stream().filter(j -> j.getSolution() == null).count();
	}
	
	public CaptchaJob getNextJob(){
		return jobs.values().stream().findFirst().orElse(null);
	}

	private Integer nextAvailableJobId() {
		log.debug("Get next available counter value");
		counter = (counter + 1)  % max;
		return counter;
	}
}
