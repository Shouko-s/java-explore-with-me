package ru.practicum.statsserver;

import org.junit.jupiter.api.Test;
import ru.practicum.statsserver.exception.ErrorHandler;
import ru.practicum.statsserver.exception.ErrorResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ErrorHandlerTest {

    private final ErrorHandler handler = new ErrorHandler();

    @Test
    void handleIllegalArgument_returnsErrorResponseWithMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid input");
        ErrorResponse resp = handler.handleIllegalArgument(ex);

        assertNotNull(resp);
        assertEquals("Некорректный аргумент", resp.error());
        assertEquals("Invalid input", resp.description());
    }
}
