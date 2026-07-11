package server.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SmartLockPasscodeRecordLifecycleTest {
    @Test
    void jpaLifecycleContract_shouldMapNullableLongAndRunPrePersistCallback() throws Exception {
        Field submittedAtField = SmartLockPasscodeRecord.class.getDeclaredField("submittedAtEpochMs");
        Column column = submittedAtField.getAnnotation(Column.class);
        assertNotNull(column);
        assertEquals(Long.class, submittedAtField.getType());
        assertEquals("submitted_at_epoch_ms", column.name());
        assertEquals(true, column.nullable());

        Method callback = SmartLockPasscodeRecord.class.getDeclaredMethod("onCreate");
        assertNotNull(callback.getAnnotation(PrePersist.class));
        SmartLockPasscodeRecord record = new SmartLockPasscodeRecord();
        long submittedAtEpochMs = 1_782_016_800_000L;
        record.setSubmittedAtEpochMs(submittedAtEpochMs);

        invokeJpaLifecycle(record, PrePersist.class);

        assertEquals(submittedAtEpochMs, record.getSubmittedAtEpochMs());
        assertNotNull(record.getCreatedAt());
        assertNotNull(record.getUpdatedAt());
        assertEquals(record.getCreatedAt(), record.getUpdatedAt());
    }

    private void invokeJpaLifecycle(Object entity, Class<PrePersist> callbackType) throws Exception {
        for (Method method : entity.getClass().getDeclaredMethods()) {
            if (method.getAnnotation(callbackType) == null) {
                continue;
            }
            method.setAccessible(true);
            method.invoke(entity);
        }
    }
}
