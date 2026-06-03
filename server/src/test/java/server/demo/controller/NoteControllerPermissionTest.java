package server.demo.controller;

import org.junit.jupiter.api.Test;
import server.demo.annotation.RequirePermission;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NoteControllerPermissionTest {

    @Test
    void notesStatistics_shouldRequireStatisticsPermission() throws Exception {
        Method method = NoteController.class.getMethod(
                "getNotesStatistics",
                LocalDate.class,
                LocalDate.class
        );

        RequirePermission permission = method.getAnnotation(RequirePermission.class);

        assertNotNull(permission);
        assertEquals(PermissionModule.STATISTICS, permission.module());
        assertEquals(PermissionAction.VIEW_STATS, permission.action());
    }
}

