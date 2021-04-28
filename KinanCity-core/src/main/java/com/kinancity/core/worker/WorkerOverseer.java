package com.kinancity.core.worker;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Overseer that has knows all workers and threads
 * 
 * @author drallieiv
 *
 */
@Getter
@Slf4j
public class WorkerOverseer implements UncaughtExceptionHandler {

	private ThreadGroup accountCreationWorkers = new ThreadGroup("accountCreation");

	private List<AccountCreationWorker> workers = new ArrayList<>();

	/**
	 * Add and start a worker
	 * 
	 * @param worker
	 */
	public void addWorker(AccountCreationWorker worker) {
		workers.add(worker);
		Thread thread = new Thread(accountCreationWorkers, worker, worker.getName());
		thread.setUncaughtExceptionHandler(this);
		thread.start();
	}

	/**
	 * Stop all worker threads
	 */
	public void stopAll() {
		workers.stream().forEach(w -> w.stop());
	}

	@Override
	public void uncaughtException(Thread tr, Throwable err) {
		log.error("Unexpected error with thread {}", tr, err);
	}

}
