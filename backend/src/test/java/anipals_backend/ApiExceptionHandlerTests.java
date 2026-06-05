package anipals_backend;

import com.anipals.backend.config.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTests {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ErrorProbeController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void illegalArgumentExceptionReturnsCleanBadRequestMessage() throws Exception {
        mockMvc.perform(get("/error-probe").param("message", "Player UID not found."))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Player UID not found."));
    }

    @Test
    void tradeCanceledMessageReturnsCleanBadRequestMessage() throws Exception {
        mockMvc.perform(get("/error-probe").param("message", "Trade canceled."))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Trade canceled."));
    }

    @RestController
    private static class ErrorProbeController {

        @GetMapping("/error-probe")
        void error(@RequestParam String message) {
            throw new IllegalArgumentException(message);
        }
    }
}
