package com.kinancity.captcha.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kinancity.captcha.server.dto.CaptchaJob;
import com.kinancity.captcha.server.dto.JobStats;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class SolvingController {

	private SolvingService solvingService;

	@Autowired
	public SolvingController(SolvingService solvingService) {
		this.solvingService = solvingService;
	}

	@CrossOrigin
	@RequestMapping(path = "/captcha/jobs/stats", method = RequestMethod.GET)
	public JobStats getJobs() {
		return new JobStats(solvingService.getNbJobsRemaining());
	}

	@CrossOrigin
	@RequestMapping(path = "/captcha/solve", method = RequestMethod.POST)
	public void simpleSolve(@RequestParam String token) {
		solvingService.addSolution(token);
	}

	@CrossOrigin
	@RequestMapping(path = "/captcha/next", method = RequestMethod.GET)
	public ResponseEntity<CaptchaJob> getNextJob() {
		CaptchaJob job = solvingService.getNextJob();
		if (job == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} else {
			return new ResponseEntity<>(job, HttpStatus.OK);
		}
	}

}
