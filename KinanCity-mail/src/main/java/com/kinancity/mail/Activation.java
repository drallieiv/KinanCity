package com.kinancity.mail;

import lombok.Data;

@Data
public class Activation {

	private String link;
	private String email;
	private String status;

	public Activation(String link, String email) {
		this.link = link;
		this.email = email;
	}

	public Activation(String link, String email, String status) {
		this.link = link;
		this.email = email;
		this.status = status;
	}

}
