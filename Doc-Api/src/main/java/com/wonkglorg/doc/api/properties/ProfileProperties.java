package com.wonkglorg.doc.api.properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import static com.wonkglorg.doc.api.DocApiApplication.DEV_MODE;

@Component
public class ProfileProperties {

    public enum Profile {
        PROD("prod"),
        TEST("test"),
        DEV("dev"),
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
        for (String profile : env.getActiveProfiles()) {
            if (Profile.PROD.getProfile().equals(profile)) {
                activeProfile = Profile.PROD;
                DEV_MODE = false;
                return;
            } else if (Profile.TEST.getProfile().equals(profile)) {
                activeProfile = Profile.TEST;
                return;
            } else if (Profile.DEPLOYMENT.getProfile().equals(profile)) {
                activeProfile = Profile.DEPLOYMENT;
                return;
            } else if (Profile.DEV.getProfile().equals(profile)) {
                activeProfile = Profile.DEV;
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

