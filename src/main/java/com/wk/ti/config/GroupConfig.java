package com.wk.ti.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "group")
@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupConfig {
    private List<String> admin;
}
