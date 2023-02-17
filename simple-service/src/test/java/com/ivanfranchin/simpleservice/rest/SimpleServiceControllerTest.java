package com.ivanfranchin.simpleservice.rest;

import com.ivanfranchin.simpleservice.security.WebSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimpleServiceController.class)
@Import(WebSecurityConfig.class)
class SimpleServiceControllerTest {

    private static final String API_PUBLIC = "/api/public";
    private static final String API_PRIVATE = "/api/private";

    private static final MediaType MEDIA_TYPE_TEXT_PLAIN_UTF8 = MediaType.valueOf("text/plain;charset=UTF-8");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetPublicString() throws Exception {
        ResultActions resultActions = mockMvc.perform(get(API_PUBLIC))
                .andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MEDIA_TYPE_TEXT_PLAIN_UTF8))
                .andExpect(content().string("It is public."));
    }

    @Test
    void testGetPrivateStringWithoutAuthentication() throws Exception {
        ResultActions resultActions = mockMvc.perform(get(API_PRIVATE))
                .andDo(print());

        resultActions.andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(value = "ivan")
    void testGetPrivateStringWithValidCredentials() throws Exception {
        ResultActions resultActions = mockMvc.perform(get(API_PRIVATE))
                .andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(content().contentType(MEDIA_TYPE_TEXT_PLAIN_UTF8))
                .andExpect(content().string("ivan, it is private."));
    }
}