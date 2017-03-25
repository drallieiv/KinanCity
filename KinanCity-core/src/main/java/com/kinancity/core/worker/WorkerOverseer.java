package com.kinancity.core.worker;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * Overseer that has knows all workers and threads
 * 
 * @author drallieiv
 *
 */
@Getter
public class WorkerOverseer {

	private ThreadGroup accountCreationWorkers = new ThreadGroup("accountCreation");

	private List<AccountCreationWorker> workers = new ArrayList<>();

	/**
	 * Add and start a worker
	 * 
	 * @param worker
	 */
	public void addWorker(AccountCreationWorker worker) {
		workers.add(worker);
		new Thread(accountCreationWorkers, worker, worker.getName()).start();
	}

	/**
	 * Stop all worker threads
	 */
	public void stopAll() {
		workers.stream().forEach(w -> w.stop());
	}

}
