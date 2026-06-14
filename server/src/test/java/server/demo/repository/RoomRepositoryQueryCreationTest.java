package server.demo.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import server.demo.entity.Room;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomRepositoryQueryCreationTest {

    @Test
    void creatingRepositoryProxy_shouldCreateRoomRepositoryQueries() {
        EntityManager entityManager = createEntityManager();
        JpaRepositoryFactory repositoryFactory = new JpaRepositoryFactory(entityManager);

        assertDoesNotThrow(() -> repositoryFactory.getRepository(RoomRepository.class));
    }

    @Test
    void roomTypeIdQueries_shouldUseRoomTypeAssociationPath() throws NoSuchMethodException {
        assertQueryUsesRoomTypeIdPath(
                RoomRepository.class.getMethod("findByStoreIdAndRoomTypeId", Long.class, Long.class)
        );
        assertQueryUsesRoomTypeIdPath(RoomRepository.class.getMethod("findByRoomTypeId", Long.class));
    }

    private void assertQueryUsesRoomTypeIdPath(Method method) {
        org.springframework.data.jpa.repository.Query query = method.getAnnotation(
                org.springframework.data.jpa.repository.Query.class
        );

        assertNotNull(query);
        assertTrue(query.value().contains("r.roomType.id = :roomTypeId"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private EntityManager createEntityManager() {
        EntityManager entityManager = mock(EntityManager.class);
        EntityManager validationEntityManager = mock(EntityManager.class);
        EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
        PersistenceUnitUtil persistenceUnitUtil = mock(PersistenceUnitUtil.class);
        Metamodel metamodel = createMetamodel();
        Query query = mock(Query.class);
        TypedQuery typedQuery = mock(TypedQuery.class);

        when(entityManager.getMetamodel()).thenReturn(metamodel);
        when(entityManager.getEntityManagerFactory()).thenReturn(entityManagerFactory);
        when(entityManager.getDelegate()).thenReturn(entityManager);
        when(entityManager.createQuery(anyString())).thenReturn(query);
        when(entityManager.createQuery(anyString(), any(Class.class))).thenReturn(typedQuery);
        when(entityManager.createQuery(any(CriteriaQuery.class))).thenReturn(typedQuery);

        when(validationEntityManager.getMetamodel()).thenReturn(metamodel);
        when(validationEntityManager.getEntityManagerFactory()).thenReturn(entityManagerFactory);
        when(validationEntityManager.createQuery(anyString())).thenReturn(query);
        when(validationEntityManager.createQuery(anyString(), any(Class.class))).thenReturn(typedQuery);
        when(validationEntityManager.createQuery(any(CriteriaQuery.class))).thenReturn(typedQuery);

        when(entityManagerFactory.getMetamodel()).thenReturn(metamodel);
        when(entityManagerFactory.getPersistenceUnitUtil()).thenReturn(persistenceUnitUtil);
        when(entityManagerFactory.createEntityManager()).thenReturn(validationEntityManager);
        when(entityManagerFactory.getProperties()).thenReturn(Map.of());

        return entityManager;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Metamodel createMetamodel() {
        Metamodel metamodel = mock(Metamodel.class);
        EntityType<Room> entityType = mock(EntityType.class);
        Type<Long> idType = mock(Type.class);
        SingularAttribute<Room, Long> idAttribute = mock(SingularAttribute.class);

        when(metamodel.managedType(Room.class)).thenReturn(entityType);
        when(metamodel.entity(Room.class)).thenReturn(entityType);
        when(metamodel.getManagedTypes()).thenReturn(Set.of(entityType));
        when(metamodel.getEntities()).thenReturn(Set.of(entityType));

        when(entityType.getJavaType()).thenReturn(Room.class);
        when(entityType.getName()).thenReturn("Room");
        when(entityType.getPersistenceType()).thenReturn(Type.PersistenceType.ENTITY);
        when(entityType.hasSingleIdAttribute()).thenReturn(true);
        when(entityType.hasVersionAttribute()).thenReturn(false);
        doReturn(idType).when(entityType).getIdType();
        doReturn(idAttribute).when(entityType).getId(Long.class);
        when(entityType.getIdClassAttributes()).thenReturn(Set.of());
        doReturn(Set.of(idAttribute)).when(entityType).getSingularAttributes();
        doReturn(Set.of(idAttribute)).when(entityType).getDeclaredSingularAttributes();
        doReturn(idAttribute).when(entityType).getSingularAttribute("id");
        doReturn(idAttribute).when(entityType).getSingularAttribute("id", Long.class);

        when(idType.getJavaType()).thenReturn(Long.class);
        when(idAttribute.getName()).thenReturn("id");
        when(idAttribute.getJavaType()).thenReturn(Long.class);
        when(idAttribute.isId()).thenReturn(true);
        when(idAttribute.isVersion()).thenReturn(false);

        return metamodel;
    }
}
