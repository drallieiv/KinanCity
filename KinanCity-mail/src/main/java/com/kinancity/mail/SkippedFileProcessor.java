package com.kinancity.mail;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import com.kinancity.mail.activator.LinkActivator;
import com.kinancity.mail.activator.QueueLinkActivator;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@Slf4j
public class SkippedFileProcessor {

	private LinkActivator activator;

	private String filePath;

	private ArrayList<String> processStatus = new ArrayList<String>(Arrays.asList(FileLogger.SKIPPED));

	public SkippedFileProcessor(LinkActivator activator, String filePath) {
		this.activator = activator;
		this.filePath = filePath;
	}

	public void process() {
		try {
			Path path = Paths.get(filePath);
			BufferedReader reader = Files.newBufferedReader(path);

			String line;
			while ((line = reader.readLine()) != null) {
				// CSV or not ?
				if (line.contains(";")) {
					Activation activation = FileLogger.fromLog(line);
					if (processStatus.contains(activation.getStatus().toUpperCase())) {
						activator.activateLink(activation);
					}
				} else {
					activator.activateLink(new Activation(line, ""));
				}
			}

			if (activator instanceof QueueLinkActivator) {
				QueueLinkActivator.class.cast(activator).setStop(true);
			}

		} catch (IOException e) {
			log.error("Error opening file {}", filePath);
		}
	}

}
