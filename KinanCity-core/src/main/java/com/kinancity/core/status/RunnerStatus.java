package com.kinancity.core.status;

public enum RunnerStatus {
	/**
	 * Has Nothing to do
	 */
	IDLE,
	/**
	 * Doing something
	 */
	RUNNING,
	/**
	 * Has stopped, and will never do anything again
	 */
	STOP,
	/**
	 * Temporarily stopped but may start running again
	 */
	PAUSED;
}
