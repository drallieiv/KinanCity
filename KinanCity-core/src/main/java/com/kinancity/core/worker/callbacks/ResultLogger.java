package com.kinancity.core.worker.callbacks;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultLogger {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static String COMMENT_PREFIX = "# ";

	private Writer resultLogWriter;

	public ResultLogger(Writer writer) {
		this.resultLogWriter = writer;
	}

	public void logStart() {
		try {
			resultLogWriter.write(new StringBuilder().append(COMMENT_PREFIX)
					.append("Batch creation start at : ").append(nowStr()).append("\n").toString());
			resultLogWriter.flush();
		} catch (IOException e) {
			logger.error("Could not write to log file");
		}
	}

	public void logEnd() {
		try {
			resultLogWriter.write(new StringBuilder().append(COMMENT_PREFIX)
					.append("Batch creation end at : ").append(nowStr()).append("\n").toString());
			resultLogWriter.flush();
		} catch (IOException e) {
			logger.debug("Could not write to log file");
		}
	}
	
	public void logLine(String msg) {
		try {
			resultLogWriter.write(new StringBuilder().append(msg).append("\n").toString());
			resultLogWriter.flush();
		} catch (IOException e) {
			logger.debug("Could not write to log file");
		}
	}

	public void logComment(String msg) {
		try {
			resultLogWriter.write(new StringBuilder().append(COMMENT_PREFIX).append(msg).append("\n").toString());
			resultLogWriter.flush();
		} catch (IOException e) {
			logger.debug("Could not write to log file");
		}
	}
	
	public void close() {
		try {
			resultLogWriter.close();
		} catch (IOException e) {
			logger.debug("Could not write to log file");
		}
	}

	/**
	 * Current date time as string
	 * 
	 * @return
	 */
	private String nowStr() {
		return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace("T", " ");
	}

}
