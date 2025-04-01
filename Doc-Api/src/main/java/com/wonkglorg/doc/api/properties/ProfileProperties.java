package com.wonkglorg.doc.api.properties;

import com.wonkglorg.doc.api.DocApiApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.print.Doc;

import static com.wonkglorg.doc.api.DocApiApplication.DEV_MODE;

@Component
public class ProfileProperties{
	
	public enum Profile{
		PROD("prod"),
		TEST("test"),
		DEPLOYMENT("deployment");
		
		private final String profile;
		
		Profile(String profile) {
			this.profile = profile;
		}
		
		public String getProfile() {
			return profile;
		}
	}
	
	private Profile activeProfile = Profile.TEST;
	
	private final Environment env;
	
	@Autowired
	public ProfileProperties(Environment env) {
		this.env = env;
		for(String profile : env.getActiveProfiles()){
			if(Profile.PROD.getProfile().equals(profile)){
				activeProfile = Profile.PROD;
				DEV_MODE = false;
				return;
			} else if(Profile.TEST.getProfile().equals(profile)){
				DEV_MODE = true;
				activeProfile = Profile.TEST;
				return;
			} else if(Profile.DEPLOYMENT.getProfile().equals(profile)){
				DEV_MODE = true;
				activeProfile = Profile.DEPLOYMENT;
				return;
			}
		}
	}
	
	public Profile getActiveProfile() {
		return activeProfile;
	}
	
	public boolean isMemoryDatabase() {
		return activeProfile == Profile.TEST || activeProfile == Profile.DEPLOYMENT;
	}
}

