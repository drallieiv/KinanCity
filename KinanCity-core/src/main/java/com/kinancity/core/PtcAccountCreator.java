package com.kinancity.core;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.captcha.CaptchaProvider;
import com.kinancity.api.model.AccountData;
import com.kinancity.core.generator.AccountGenerator;
import com.kinancity.core.model.AccountCreation;
import com.kinancity.core.proxy.ProxyManager;
import com.kinancity.core.scheduling.AccountCreationQueue;
import com.kinancity.core.status.RunnerStatus;
import com.kinancity.core.worker.AccountCreationWorker;
import com.kinancity.core.worker.AccountCreationWorkerFactory;
import com.kinancity.core.worker.WorkerOverseer;
import com.kinancity.core.worker.callbacks.CreationCallbacks;
import com.kinancity.core.worker.callbacks.ResultLogger;
import com.kinancity.core.worker.callbacks.SaveOrRetryCallbacks;

import lombok.Getter;

@Getter
public class PtcAccountCreator {
	private Logger logger = LoggerFactory.getLogger(getClass());

	private Configuration config;

	private int initialCount = 0;

	// how fast should the status check, in ms.
	private long statusPollingRate = 500;

	// Queue of account creation to process
	private AccountCreationQueue queue = new AccountCreationQueue();

	// Final place for accountCreation successfully processed
	private List<AccountCreation> done = new ArrayList<>();

	// Final place for accountCreation that failed
	private List<AccountCreation> failed = new ArrayList<>();

	private WorkerOverseer workerOverseer = new WorkerOverseer();

	private AccountCreationWorkerFactory accountCreationWorkerFactory = new AccountCreationWorkerFactory();

	public PtcAccountCreator(Configuration config) {
		this.config = config;
	}

	public void start() {
		// Add All creation in a queue
		scheduleCreations();

		setupWorkers();

		showProgress();
	}

	/**
	 * Prepare the creation of all Accounts requested
	 */
	private void scheduleCreations() {
		AccountGenerator generator = config.getAccountGenerator();

		while (generator.hasNext()) {
			AccountData account = generator.next();
			queue.add(new AccountCreation(account));
		}

		initialCount = queue.size();

		logger.info("{} account creations added in queue", queue.size());
	}

	/**
	 * Add the number of workers defined in configuration
	 */
	private void setupWorkers() {

		/**
		 * Live Logger for results
		 */
		ResultLogger resultLogger = config.getResultLogger();
		resultLogger.logStart();

		// Callback for creation
		CreationCallbacks callbacks = new SaveOrRetryCallbacks(queue, done, failed, resultLogger);
		
		// Captcha Provider from config
		CaptchaProvider catpchaProvider = config.getCaptchaProvider();
		
		// Proxy Manager from config
		ProxyManager proxyManager = config.getProxyManager();

		// Start multiple workers that will consume the queue
		for (int i = 0; i < config.getNbThreads(); i++) {
			AccountCreationWorker worker = accountCreationWorkerFactory.createWorker(queue, catpchaProvider, proxyManager, callbacks);
			worker.setDryRun(config.isDryRun());
			worker.setDumpResult(config.getDumpResult());
			workerOverseer.addWorker(worker);
		}

		logger.info("{} worker thread added", config.getNbThreads());
	}

	/**
	 * Display progress, will run until everything is done
	 */
	private void showProgress() {
		int lastCount = 0;
		while (!hasFinished()) {
			int count = done.size() + failed.size();
			if (count != lastCount) {
				logger.info("Batch creation progress : {}/{} done with {} failures", done.size() + failed.size(), initialCount, failed.size());
				lastCount = count;
			}

			try {
				Thread.sleep(statusPollingRate);
			} catch (InterruptedException e) {
				logger.warn("Account Creator interrupted");
			}
		}
		logger.info("Batch creation progress : {}/{} done with {} failures", done.size() + failed.size(), initialCount, failed.size());

		// End Logs
		ResultLogger resultLogger = config.getResultLogger();
		resultLogger.logEnd();
		resultLogger.logComment(String.format("%s success, %s errors", done.size(), failed.size()));
		resultLogger.close();

		// Do something more
		workerOverseer.stopAll();
	}

	private boolean hasFinished() {
		return nbInQueue() + nbProcessing() == 0;
	}

	private int nbInQueue() {
		return queue.size();
	}

	private long nbProcessing() {
		return workerOverseer.getWorkers().stream().filter(w -> w.getStatus() == RunnerStatus.RUNNING).count();
	}
}
