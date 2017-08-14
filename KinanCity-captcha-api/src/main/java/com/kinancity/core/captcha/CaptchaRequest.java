package com.kinancity.core.captcha;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class CaptchaRequest implements Serializable {

	private static final long serialVersionUID = -4747076215124299774L;

	private static final long SLEEP_TIME = 500;

	private String response;

	private String username;

	private LocalDateTime creationTime;

	public CaptchaRequest(String username) {
		this.username = username;
		this.creationTime = LocalDateTime.now();
	}

	public String getResponse() {
		try {
			while (response == null) {
				Thread.sleep(SLEEP_TIME);
			}
			return response;
		} catch (InterruptedException e) {
			return null;
		}
	}

	@Override
	public String toString() {
		return "CaptchaRequest [" + (response != null ? "response=" + response + ", " : "") + (username != null ? "username=" + username + ", " : "") + (creationTime != null ? "creationTime=" + creationTime : "") + "]";
	}

}
