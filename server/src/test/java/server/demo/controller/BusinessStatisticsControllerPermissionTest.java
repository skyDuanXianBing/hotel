package server.demo.controller;

import org.junit.jupiter.api.Test;
import server.demo.annotation.RequirePermission;
import server.demo.enums.PermissionAction;
import server.demo.enums.PermissionModule;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BusinessStatisticsControllerPermissionTest {

    @Test
    void revenueSummary_shouldRequireFinancialDataPermission() throws Exception {
        Method method = BusinessStatisticsController.class.getMethod(
                "getRevenueSummary",
                LocalDate.class,
                LocalDate.class
        );

        RequirePermission permission = method.getAnnotation(RequirePermission.class);

        assertNotNull(permission);
        assertEquals(PermissionModule.SENSITIVE, permission.module());
        assertEquals(PermissionAction.VIEW_FINANCIAL_DATA, permission.action());
    }

    @Test
    void businessSummary_shouldKeepStatisticsPermission() throws Exception {
        Method method = BusinessStatisticsController.class.getMethod(
                "getBusinessSummary",
                LocalDate.class,
                LocalDate.class
        );

        RequirePermission permission = method.getAnnotation(RequirePermission.class);

        assertNotNull(permission);
        assertEquals(PermissionModule.STATISTICS, permission.module());
        assertEquals(PermissionAction.VIEW_STATS, permission.action());
    }
}

