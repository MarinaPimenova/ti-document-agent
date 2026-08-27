package com.wk.ti.controller;

import com.wk.ti.healthcheck.VersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VersionControllerTest {

    @Mock
    private VersionService versionService;

    @InjectMocks
    private VersionController versionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(versionController).build();
    }

    @Test
    void shouldReturnResponseEntityWithHttpStatus200() throws Exception {
        given(versionService.getVersion()).willReturn("app: 1.1.1");

        mockMvc.perform(MockMvcRequestBuilders.get("/rest/v1/version"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("app: 1.1.1"));
    }
}