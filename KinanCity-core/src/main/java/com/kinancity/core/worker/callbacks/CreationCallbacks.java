package com.kinancity.core.worker.callbacks;

import com.kinancity.core.model.AccountCreation;

public interface CreationCallbacks {

	void onSuccess(AccountCreation accountCreation);

	void onTechnicalIssue(AccountCreation accountCreation);

	void onFailure(AccountCreation accountCreation);

}
