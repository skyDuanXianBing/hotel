package server.demo.controller;

import org.junit.jupiter.api.Test;
import server.demo.annotation.RequirePermission;
import server.demo.dto.AnnouncementDTO;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnnouncementControllerPermissionTest {

    @Test
    void managementList_shouldRequireSettingsViewPermission() throws Exception {
        Method method = AnnouncementController.class.getMethod("listStoreAnnouncements");

        RequirePermission permission = method.getAnnotation(RequirePermission.class);

        assertNotNull(permission);
        assertEquals(PermissionModule.SETTINGS, permission.module());
        assertEquals(PermissionAction.VIEW_SETTINGS, permission.action());
    }

    @Test
    void managementMutations_shouldRequireModifyStoreSettingsPermission() throws Exception {
        assertModifyStoreSettingsPermission(AnnouncementController.class.getMethod(
                "createStoreAnnouncement",
                AnnouncementDTO.class
        ));
        assertModifyStoreSettingsPermission(AnnouncementController.class.getMethod(
                "updateStoreAnnouncement",
                Long.class,
                AnnouncementDTO.class
        ));
        assertModifyStoreSettingsPermission(AnnouncementController.class.getMethod(
                "disableStoreAnnouncement",
                Long.class
        ));
    }

    @Test
    void homeList_shouldKeepNormalAuthenticatedAccess() throws Exception {
        Method method = AnnouncementController.class.getMethod(
                "listHomeAnnouncements",
                String.class,
                Integer.class
        );

        RequirePermission permission = method.getAnnotation(RequirePermission.class);

        assertNull(permission);
    }

    private static void assertModifyStoreSettingsPermission(Method method) {
        RequirePermission permission = method.getAnnotation(RequirePermission.class);

        assertNotNull(permission);
        assertEquals(PermissionModule.SETTINGS, permission.module());
        assertEquals(PermissionAction.MODIFY_STORE_SETTINGS, permission.action());
    }
}
