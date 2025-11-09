package ru.practicum.statsserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.statsdto.request.EndpointHitRequestDto;
import ru.practicum.statsdto.response.ViewStatsResponseDto;
import ru.practicum.statsserver.controller.StatsController;
import ru.practicum.statsserver.exception.ErrorHandler;
import ru.practicum.statsserver.service.EndpointHitService;

import static java.util.List.of;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StatsController.class)
@Import(ErrorHandler.class)
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EndpointHitService service;

    @Test
    void postHit_validDto_returnsOkAndCallsService() throws Exception {
        String json = """
            {
              "app": "ewm-service",
              "uri": "/events/1",
              "ip": "127.0.0.1",
              "timestamp": "2023-08-01 12:00:00"
            }
            """;

        mockMvc.perform(post("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isOk());

        verify(service, times(1)).hit(any(EndpointHitRequestDto.class));
    }

    @Test
    void postHit_invalidDto_missingRequiredFields_returnsBadRequest() throws Exception {
        String json = """
            {
              "uri": "/events/1",
              "ip": "127.0.0.1"
            }
            """;

        mockMvc.perform(post("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void getStats_validParams_callsServiceAndReturnsJsonArray() throws Exception {
        String start = "2023-01-01 00:00:00";
        String end = "2023-01-02 00:00:00";

        ViewStatsResponseDto dto = ViewStatsResponseDto.builder()
            .app("appA")
            .uri("/a")
            .hits(10L)
            .build();

        when(service.getStats(eq(start), eq(end), anyList(), eq(false))).thenReturn(of(dto));

        mockMvc.perform(get("/stats")
                .param("start", start)
                .param("end", end)
                .param("uris", "/a", "/b")
                .param("unique", "false"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].app", is("appA")))
            .andExpect(jsonPath("$[0].uri", is("/a")))
            .andExpect(jsonPath("$[0].hits", is(10)));

        verify(service, times(1)).getStats(eq(start), eq(end), anyList(), eq(false));
    }

    @Test
    void getStats_missingStartParam_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/stats")
                .param("end", "2023-01-02 00:00:00"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void getStats_whenServiceThrowsIllegalArgument_exceptionHandledByAdvice_returnsBadRequestWithBody() throws Exception {
        String start = "2023-01-03 00:00:00";
        String end = "2023-01-02 00:00:00";

        when(service.getStats(eq(start), eq(end), any(), eq(false)))
            .thenThrow(new IllegalArgumentException("End не может быть до или равен Start"));

        mockMvc.perform(get("/stats")
                .param("start", start)
                .param("end", end))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.error", is("Некорректный аргумент")))
            .andExpect(jsonPath("$.description", containsString("End не может быть")));
    }

    @Test
    void getStats_whenUrisEmpty_passesNullToService() throws Exception {
        String start = "2023-01-01 00:00:00";
        String end = "2023-01-02 00:00:00";

        when(service.getStats(eq(start), eq(end), isNull(), eq(false))).thenReturn(of());

        mockMvc.perform(get("/stats")
                .param("start", start)
                .param("end", end)
            )
            .andExpect(status().isOk());

        verify(service).getStats(eq(start), eq(end), isNull(), eq(false));
    }
}
