package com.smartgarden.backend.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A interface 3D (../intelligarden-web) é copiada para static/ no build e servida pelo backend, sem depender de CDN. */
@SpringBootTest
@AutoConfigureMockMvc
class InterfacePageTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void rootOpensThe3dPage() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>IntelliGarden 3D</title>")));
    }

    @Test
    void servesThreeJsAndFontsLocally() throws Exception {
        mvc.perform(get("/vendor/three/three.module.min.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/javascript"));
        mvc.perform(get("/vendor/three/addons/environments/RoomEnvironment.js")).andExpect(status().isOk());
        mvc.perform(get("/vendor/three/addons/lights/RectAreaLightUniformsLib.js")).andExpect(status().isOk());
        mvc.perform(get("/vendor/fonts/fonts.css")).andExpect(status().isOk());
        mvc.perform(get("/vendor/fonts/geist-latin.woff2")).andExpect(status().isOk());
    }
}
