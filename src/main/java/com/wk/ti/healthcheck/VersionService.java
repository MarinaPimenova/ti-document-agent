package com.wk.ti.healthcheck;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VersionService {
    private final String version;
    private final String name;

    public VersionService(@Value("${info.app.version}") String version,
                          @Value("${info.app.name}") String name) {
        this.version = version;
        this.name = name;
    }

    public String getVersion() {
        return name + " : " + version;
    }

}
