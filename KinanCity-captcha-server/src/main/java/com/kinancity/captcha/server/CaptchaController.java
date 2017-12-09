package com.kinancity.captcha.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.kinancity.captcha.server.errors.SolvingException;
import com.kinancity.core.captcha.antiCaptcha.dto.AbstractTaskDto;
import com.kinancity.core.captcha.antiCaptcha.dto.BalanceRequest;
import com.kinancity.core.captcha.antiCaptcha.dto.BalanceResponse;
import com.kinancity.core.captcha.antiCaptcha.dto.CatpchaSolutionDto;
import com.kinancity.core.captcha.antiCaptcha.dto.CreateTaskRequest;
import com.kinancity.core.captcha.antiCaptcha.dto.CreateTaskResponse;
import com.kinancity.core.captcha.antiCaptcha.dto.PtcCaptchaTask;
import com.kinancity.core.captcha.antiCaptcha.dto.TaskResultRequest;
import com.kinancity.core.captcha.antiCaptcha.dto.TaskResultResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class CaptchaController {

	private SolvingService solvingService;

	@Autowired
	public CaptchaController(SolvingService solvingService) {
		this.solvingService = solvingService;
	}

	@RequestMapping(path = "/captcha/balance", method = RequestMethod.POST)
	public BalanceResponse getBalance(@RequestBody BalanceRequest request) {
		log.debug("get Balance Called");

		BalanceResponse response = new BalanceResponse();

		Double balance = 999D;
		response.setBalance(balance);

		return response;
	}

	@RequestMapping(path = "/captcha/submit", method = RequestMethod.POST)
	public CreateTaskResponse addRequest(@RequestBody CreateTaskRequest request) {
		log.debug("add Request Called");

		AbstractTaskDto task = request.getTask();
		if (task instanceof PtcCaptchaTask) {
			PtcCaptchaTask captchaTask = PtcCaptchaTask.class.cast(task);
			Integer taskId = solvingService.addPtcCaptchaTask(captchaTask);

			CreateTaskResponse response = new CreateTaskResponse();
			response.setTaskId(taskId);
			return response;
		} else {
			CreateTaskResponse response = new CreateTaskResponse();
			response.setErrorCode("999");
			response.setErrorDescription("Unknown task type");
			return response;
		}
	}

	@RequestMapping(path = "/captcha/retrieive", method = RequestMethod.POST)
	public TaskResultResponse getResult(@RequestBody TaskResultRequest request) {
		log.debug("get Result Called");

		TaskResultResponse response = new TaskResultResponse();

		try {
			String captcha = solvingService.getCaptchaFor(Integer.parseInt(request.getTaskId()));

			if (captcha != null) {
				response.setStatus(TaskResultResponse.READY);
				CatpchaSolutionDto solution = new CatpchaSolutionDto();
				solution.setGRecaptchaResponse(captcha);
				response.setSolution(solution);
			} else {
				response.setStatus(TaskResultResponse.PROCESSING);
			}
		} catch (SolvingException e) {
			response.setStatus(TaskResultResponse.READY);
			response.setErrorId(1);
			response.setErrorDescription(e.getMessage());
			response.setErrorCode("FAILED");
		}

		return response;
	}
}
