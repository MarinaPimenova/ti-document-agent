package com.wk.ti.controller;

import com.wk.ti.healthcheck.VersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;

    @GetMapping(value = "/rest/v1/version", produces = MediaType.TEXT_PLAIN_VALUE)
    public String getVersion() {
        return versionService.getVersion();
    }
}
