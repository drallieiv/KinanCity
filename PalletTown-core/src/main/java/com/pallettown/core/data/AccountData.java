package com.pallettown.core.data;

import lombok.Data;

@Data
public class AccountData implements Cloneable{
	 
	public String username;
	
	public String email;
	
	public String password;

	public AccountData clone() {
		AccountData clonedData = new AccountData();
		clonedData.username = this.username;
		clonedData.email = this.email;
		clonedData.password = this.password;
        return clonedData;
    }
}
