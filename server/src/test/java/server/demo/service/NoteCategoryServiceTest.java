package server.demo.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import server.demo.context.StoreContext;
import server.demo.context.StoreContextHolder;
import server.demo.entity.NoteCategory;
import server.demo.repository.NoteCategoryRepository;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteCategoryServiceTest {

    @AfterEach
    void tearDown() {
        StoreContextHolder.clear();
    }

    @Test
    void updateCategoriesOrder_shouldSaveExistingEntities() {
        NoteCategoryRepository repository = Mockito.mock(NoteCategoryRepository.class);
        NoteCategoryService service = new NoteCategoryService();
        inject(service, "noteCategoryRepository", repository);
        StoreContextHolder.setContext(new StoreContext(1L, 7L, "admin"));

        NoteCategory existingIncome = new NoteCategory();
        existingIncome.setId(101L);
        existingIncome.setStoreId(7L);
        existingIncome.setName("房费");
        existingIncome.setType("income");
        existingIncome.setDisplayOrder(0);

        NoteCategory existingExpense = new NoteCategory();
        existingExpense.setId(102L);
        existingExpense.setStoreId(7L);
        existingExpense.setName("水电");
        existingExpense.setType("expense");
        existingExpense.setDisplayOrder(1);

        when(repository.findById(101L)).thenReturn(Optional.of(existingIncome));
        when(repository.findById(102L)).thenReturn(Optional.of(existingExpense));
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        NoteCategory updateIncome = new NoteCategory();
        updateIncome.setId(101L);
        updateIncome.setDisplayOrder(1);
        NoteCategory updateExpense = new NoteCategory();
        updateExpense.setId(102L);
        updateExpense.setDisplayOrder(0);

        List<NoteCategory> result = service.updateCategoriesOrder(List.of(updateIncome, updateExpense));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NoteCategory>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        List<NoteCategory> saved = captor.getValue();

        assertEquals(2, result.size());
        assertEquals(2, saved.size());
        assertSame(existingIncome, saved.get(0));
        assertSame(existingExpense, saved.get(1));
        assertNotNull(saved.get(0).getName());
        assertNotNull(saved.get(1).getName());
        assertEquals(1, saved.get(0).getDisplayOrder());
        assertEquals(0, saved.get(1).getDisplayOrder());
    }

    @Test
    void updateCategoriesOrder_shouldRejectOtherStoreCategory() {
        NoteCategoryRepository repository = Mockito.mock(NoteCategoryRepository.class);
        NoteCategoryService service = new NoteCategoryService();
        inject(service, "noteCategoryRepository", repository);
        StoreContextHolder.setContext(new StoreContext(1L, 7L, "admin"));

        NoteCategory existing = new NoteCategory();
        existing.setId(201L);
        existing.setStoreId(8L);
        existing.setName("房费");
        existing.setType("income");
        existing.setDisplayOrder(0);

        when(repository.findById(201L)).thenReturn(Optional.of(existing));

        NoteCategory update = new NoteCategory();
        update.setId(201L);
        update.setDisplayOrder(1);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.updateCategoriesOrder(List.of(update)));
        assertEquals("无权限修改此分类: 201", exception.getMessage());
        verify(repository, never()).saveAll(anyList());
    }

    private static void inject(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
