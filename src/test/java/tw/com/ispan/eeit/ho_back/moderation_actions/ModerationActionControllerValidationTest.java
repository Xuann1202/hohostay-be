package tw.com.ispan.eeit.ho_back.moderation_actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.modelmapper.ModelMapper;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ModerationActionController.class)
public class ModerationActionControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Note: MockBean is deprecated in Spring Boot 3.4+ but remains functional
    // Suppress deprecation warnings as this is the standard test annotation for
    // @WebMvcTest
    @SuppressWarnings("removal")
    @MockBean
    private ModerationActionService service;

    @SuppressWarnings("removal")
    @MockBean
    private ModelMapper modelMapper;

    @Test
    void whenPostInvalid_thenReturnsValidationErrors() throws Exception {
        // missing required 'status' field -> should trigger validation error
        ModerationActionDTO dto = new ModerationActionDTO();
        dto.setReporterId(1L);
        dto.setReviewId(2L);
        dto.setReviewAuthorId(3L);
        dto.setModeratorId(4L);
        dto.setReason("test reason");
        // status intentionally left null

        mockMvc.perform(post("/api/moderation-actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists())
                .andExpect(jsonPath("$.message").exists());
    }
}
