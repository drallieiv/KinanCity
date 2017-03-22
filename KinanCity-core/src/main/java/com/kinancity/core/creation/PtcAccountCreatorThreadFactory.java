package com.kinancity.core.creation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.lang.math.RandomUtils;

import lombok.Getter;
import lombok.Setter;

public class PtcAccountCreatorThreadFactory implements ThreadFactory {

	@Getter
	@Setter
	private List<String> trainerNames = Arrays.asList("Ash","Misty","Brock","Tracey","May","Max","Dawn","Iris","Cilan","Serena","Clemont","Bonnie","Lana","Mallow","Lillie","Sophocles", "Kiawe");
	
	private int position = -1;
	
	private String getNextName(){
		if(position < 0){
			position = RandomUtils.nextInt(trainerNames.size());
			Collections.shuffle(trainerNames);
		}
		
		return trainerNames.get(position++);
	}
	
	@Override
	public Thread newThread(Runnable arg0) {
		return new Thread(arg0, getNextName());
	}

}
