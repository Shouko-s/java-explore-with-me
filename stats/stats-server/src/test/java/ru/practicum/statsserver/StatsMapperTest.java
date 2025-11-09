package ru.practicum.statsserver;

import org.junit.jupiter.api.Test;
import ru.practicum.statsdto.request.EndpointHitRequestDto;
import ru.practicum.statsserver.entity.EndpointHitEntity;
import ru.practicum.statsserver.mapper.StatsMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class StatsMapperTest {

    private final StatsMapper mapper = new StatsMapper();

    @Test
    void buildEntity_validDto_mapsFieldsCorrectly() {
        String timestamp = "2023-08-01 12:34:56";
        EndpointHitRequestDto dto = EndpointHitRequestDto.builder()
            .app("ewm-service")
            .uri("/events/1")
            .ip("127.0.0.1")
            .timestamp(timestamp)
            .build();

        EndpointHitEntity entity = mapper.buildEntity(dto);

        assertNotNull(entity);
        assertEquals("ewm-service", entity.getApp());
        assertEquals("/events/1", entity.getUri());
        assertEquals("127.0.0.1", entity.getIp());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime expected = LocalDateTime.parse(timestamp, fmt);
        assertEquals(expected, entity.getTimestamp());
    }

    @Test
    void buildEntity_invalidTimestamp_throwsException() {
        EndpointHitRequestDto dto = EndpointHitRequestDto.builder()
            .app("ewm-service")
            .uri("/events/1")
            .ip("127.0.0.1")
            .timestamp("invalid-timestamp")
            .build();

        assertThrows(RuntimeException.class, () -> mapper.buildEntity(dto));
    }
}
