package ru.practicum.statsserver;

import org.junit.jupiter.api.Test;
import ru.practicum.statsdto.response.ViewStatsResponseDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ViewStatsResponseDtoTest {

    @Test
    void builder_and_getters_work() {
        ViewStatsResponseDto dto = ViewStatsResponseDto.builder()
            .app("app")
            .uri("/u")
            .hits(5L)
            .build();

        assertNotNull(dto);
        assertEquals("app", dto.getApp());
        assertEquals("/u", dto.getUri());
        assertEquals(5L, dto.getHits());
    }

    @Test
    void default_constructor() {
        ViewStatsResponseDto dto = new ViewStatsResponseDto();
        assertNotNull(dto);
    }
}
