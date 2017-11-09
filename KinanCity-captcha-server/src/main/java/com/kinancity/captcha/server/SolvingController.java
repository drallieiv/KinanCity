package com.kinancity.captcha.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class SolvingController {

	private SolvingService solvingService;

	@Autowired
	public SolvingController(SolvingService solvingService) {
		this.solvingService = solvingService;
	}

	@RequestMapping(path = "/captcha/solve", method = RequestMethod.POST)
	public void simpleSolve(@RequestBody String solution) {
		solvingService.addSolution(solution);
	}

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
