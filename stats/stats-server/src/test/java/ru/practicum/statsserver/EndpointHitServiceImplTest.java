package ru.practicum.statsserver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.statsdto.request.EndpointHitRequestDto;
import ru.practicum.statsdto.response.ViewStatsResponseDto;
import ru.practicum.statsserver.entity.EndpointHitEntity;
import ru.practicum.statsserver.mapper.StatsMapper;
import ru.practicum.statsserver.repository.EndpointHitRepository;
import ru.practicum.statsserver.service.impl.EndpointHitServiceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndpointHitServiceImplTest {

    @Mock
    private EndpointHitRepository repository;

    @Mock
    private StatsMapper mapper;

    @InjectMocks
    private EndpointHitServiceImpl service;

    @Captor
    private ArgumentCaptor<EndpointHitEntity> entityCaptor;

    @Test
    void hit_callsRepositorySaveWithMappedEntity() {
        EndpointHitRequestDto dto = EndpointHitRequestDto.builder()
            .app("ewm-service")
            .uri("/events/1")
            .ip("127.0.0.1")
            .timestamp("2023-08-01 12:00:00")
            .build();

        EndpointHitEntity mapped = EndpointHitEntity.builder()
            .app("ewm-service")
            .uri("/events/1")
            .ip("127.0.0.1")
            .timestamp(LocalDateTime.of(2023, 8, 1, 12, 0, 0))
            .build();

        when(mapper.buildEntity(dto)).thenReturn(mapped);

        service.hit(dto);

        verify(mapper).buildEntity(dto);
        verify(repository).save(entityCaptor.capture());
        EndpointHitEntity captured = entityCaptor.getValue();
        assertSame(mapped, captured, "Repository should be called with entity returned by mapper");
    }

    @Test
    void getStats_whenUniqueFalse_callsFindStatsAndReturnsList() {
        String start = "2023-01-01 00:00:00";
        String end = "2023-01-02 00:00:00";
        List<String> uris = List.of("/a", "/b");

        ViewStatsResponseDto dto = ViewStatsResponseDto.builder()
            .app("app")
            .uri("/a")
            .hits(5L)
            .build();

        when(repository.findStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(uris)))
            .thenReturn(List.of(dto));

        List<ViewStatsResponseDto> res = service.getStats(start, end, uris, false);

        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals("app", res.get(0).getApp());
        verify(repository, times(1)).findStats(any(LocalDateTime.class), any(LocalDateTime.class), eq(uris));
        verify(repository, never()).findStatsUnique(any(LocalDateTime.class), any(LocalDateTime.class), any());
    }

    @Test
    void getStats_whenUniqueTrue_callsFindStatsUniqueAndReturnsList() {
        String start = "2023-05-01 00:00:00";
        String end = "2023-05-03 00:00:00";
        List<String> uris = List.of("/x");

        ViewStatsResponseDto dto = ViewStatsResponseDto.builder()
            .app("appX")
            .uri("/x")
            .hits(2L)
            .build();

        when(repository.findStatsUnique(any(LocalDateTime.class), any(LocalDateTime.class), eq(uris)))
            .thenReturn(List.of(dto));

        List<ViewStatsResponseDto> res = service.getStats(start, end, uris, true);

        assertNotNull(res);
        assertEquals(1, res.size());
        assertEquals("/x", res.get(0).getUri());
        verify(repository, times(1)).findStatsUnique(any(LocalDateTime.class), any(LocalDateTime.class), eq(uris));
        verify(repository, never()).findStats(any(LocalDateTime.class), any(LocalDateTime.class), any());
    }

    @Test
    void getStats_whenUrisNull_passesNullToRepository() {
        String start = "2023-06-01 00:00:00";
        String end = "2023-06-02 00:00:00";
        List<String> uris = null;

        when(repository.findStats(any(LocalDateTime.class), any(LocalDateTime.class), isNull()))
            .thenReturn(emptyList());

        List<ViewStatsResponseDto> res = service.getStats(start, end, uris, false);

        assertNotNull(res);
        assertTrue(res.isEmpty());
        verify(repository).findStats(any(LocalDateTime.class), any(LocalDateTime.class), isNull());
    }

    @Test
    void getStats_whenUrisEmpty_passesNullToRepository() {
        String start = "2023-06-01 00:00:00";
        String end = "2023-06-02 00:00:01";
        List<String> uris = List.of();

        when(repository.findStats(any(LocalDateTime.class), any(LocalDateTime.class), isNull()))
            .thenReturn(emptyList());

        List<ViewStatsResponseDto> res = service.getStats(start, end, uris, false);

        assertNotNull(res);
        assertTrue(res.isEmpty());
        verify(repository).findStats(any(LocalDateTime.class), any(LocalDateTime.class), isNull());
    }

    @Test
    void getStats_invalidRange_throwsIllegalArgumentException_whenEndBeforeOrEqualStart() {
        String start = "2023-07-02 10:00:00";
        String endEqual = "2023-07-02 10:00:00";
        String endBefore = "2023-07-02 09:00:00";

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
            () -> service.getStats(start, endEqual, null, false));
        assertTrue(ex1.getMessage().contains("End"));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
            () -> service.getStats(start, endBefore, null, false));
        assertTrue(ex2.getMessage().contains("End"));

        verifyNoInteractions(repository);
    }

    @Test
    void getStats_whenUniqueTrue_andUrisNull_passesNullToFindStatsUnique() {
        String start = "2023-09-01 00:00:00";
        String end = "2023-09-02 00:00:00";
        List<String> uris = null;

        when(repository.findStatsUnique(any(LocalDateTime.class), any(LocalDateTime.class), isNull()))
            .thenReturn(emptyList());

        List<ViewStatsResponseDto> res = service.getStats(start, end, uris, true);

        assertNotNull(res);
        assertTrue(res.isEmpty());
        verify(repository, times(1)).findStatsUnique(any(LocalDateTime.class), any(LocalDateTime.class), isNull());
        verify(repository, never()).findStats(any(LocalDateTime.class), any(LocalDateTime.class), any());
    }

    @Test
    void getStats_whenUniqueTrue_andUrisEmpty_passesNullToFindStatsUnique() {
        String start = "2023-09-10 00:00:00";
        String end = "2023-09-11 01:00:00";
        List<String> uris = List.of();

        when(repository.findStatsUnique(any(LocalDateTime.class), any(LocalDateTime.class), isNull()))
            .thenReturn(emptyList());

        List<ViewStatsResponseDto> res = service.getStats(start, end, uris, true);

        assertNotNull(res);
        assertTrue(res.isEmpty());
        verify(repository, times(1)).findStatsUnique(any(LocalDateTime.class), any(LocalDateTime.class), isNull());
        verify(repository, never()).findStats(any(LocalDateTime.class), any(LocalDateTime.class), any());
    }

}
