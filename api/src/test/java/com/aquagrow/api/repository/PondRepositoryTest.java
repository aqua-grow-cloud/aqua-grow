package com.aquagrow.api.repository;

import com.aquagrow.api.model.Pond;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("PondRepository Tests")
class PondRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PondRepository pondRepository;

    private Pond testPond1;
    private Pond testPond2;

    @BeforeEach
    void setUp() {
        testPond1 = Pond.builder()
                .name("Test Pond 1")
                .tenantId("tenant-1")
                .area(100.0)
                .depth(2.5)
                .type("Concrete")
                .status("Active")
                .build();

        testPond2 = Pond.builder()
                .name("Test Pond 2")
                .tenantId("tenant-1")
                .area(150.0)
                .depth(3.0)
                .type("Earthen")
                .status("Inactive")
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve pond")
    void savePond_shouldPersistPond() {
        // When
        Pond savedPond = pondRepository.save(testPond1);

        // Then
        assertThat(savedPond).isNotNull();
        assertThat(savedPond.getId()).isNotNull();
        assertThat(savedPond.getName()).isEqualTo("Test Pond 1");
        assertThat(savedPond.getTenantId()).isEqualTo("tenant-1");
        assertThat(savedPond.getArea()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should find all ponds")
    void findAll_shouldReturnAllPonds() {
        // Given
        entityManager.persist(testPond1);
        entityManager.persist(testPond2);
        entityManager.flush();

        // When
        List<Pond> ponds = pondRepository.findAll();

        // Then
        assertThat(ponds).hasSize(2);
        assertThat(ponds).extracting(Pond::getName)
                .containsExactlyInAnyOrder("Test Pond 1", "Test Pond 2");
    }

    @Test
    @DisplayName("Should find pond by id")
    void findById_shouldReturnPond_whenExists() {
        // Given
        Pond savedPond = entityManager.persist(testPond1);
        entityManager.flush();

        // When
        Optional<Pond> foundPond = pondRepository.findById(savedPond.getId());

        // Then
        assertThat(foundPond).isPresent();
        assertThat(foundPond.get().getName()).isEqualTo("Test Pond 1");
        assertThat(foundPond.get().getId()).isEqualTo(savedPond.getId());
    }

    @Test
    @DisplayName("Should return empty when pond not found by id")
    void findById_shouldReturnEmpty_whenNotExists() {
        // When
        Optional<Pond> foundPond = pondRepository.findById("non-existent-id");

        // Then
        assertThat(foundPond).isEmpty();
    }

    @Test
    @DisplayName("Should find pond by name")
    void findByName_shouldReturnPond_whenExists() {
        // Given
        entityManager.persist(testPond1);
        entityManager.flush();

        // When
        Optional<Pond> foundPond = pondRepository.findByName("Test Pond 1");

        // Then
        assertThat(foundPond).isPresent();
        assertThat(foundPond.get().getName()).isEqualTo("Test Pond 1");
        assertThat(foundPond.get().getArea()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should return empty when pond not found by name")
    void findByName_shouldReturnEmpty_whenNotExists() {
        // When
        Optional<Pond> foundPond = pondRepository.findByName("Non-existent Pond");

        // Then
        assertThat(foundPond).isEmpty();
    }

    @Test
    @DisplayName("Should update pond")
    void updatePond_shouldPersistChanges() {
        // Given
        Pond savedPond = entityManager.persist(testPond1);
        entityManager.flush();
        entityManager.clear();

        // When
        savedPond.setName("Updated Pond Name");
        savedPond.setArea(200.0);
        savedPond.setStatus("Maintenance");
        Pond updatedPond = pondRepository.save(savedPond);

        // Then
        assertThat(updatedPond.getName()).isEqualTo("Updated Pond Name");
        assertThat(updatedPond.getArea()).isEqualTo(200.0);
        assertThat(updatedPond.getStatus()).isEqualTo("Maintenance");
    }

    @Test
    @DisplayName("Should delete pond")
    void deletePond_shouldRemovePond() {
        // Given
        Pond savedPond = entityManager.persist(testPond1);
        entityManager.flush();
        String pondId = savedPond.getId();

        // When
        pondRepository.delete(savedPond);
        entityManager.flush();

        // Then
        Optional<Pond> deletedPond = pondRepository.findById(pondId);
        assertThat(deletedPond).isEmpty();
    }

    @Test
    @DisplayName("Should delete pond by id")
    void deleteById_shouldRemovePond() {
        // Given
        Pond savedPond = entityManager.persist(testPond1);
        entityManager.flush();
        String pondId = savedPond.getId();

        // When
        pondRepository.deleteById(pondId);
        entityManager.flush();

        // Then
        Optional<Pond> deletedPond = pondRepository.findById(pondId);
        assertThat(deletedPond).isEmpty();
    }

    @Test
    @DisplayName("Should check if pond exists by id")
    void existsById_shouldReturnTrue_whenPondExists() {
        // Given
        Pond savedPond = entityManager.persist(testPond1);
        entityManager.flush();

        // When
        boolean exists = pondRepository.existsById(savedPond.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when pond does not exist")
    void existsById_shouldReturnFalse_whenPondNotExists() {
        // When
        boolean exists = pondRepository.existsById("non-existent-id");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should count all ponds")
    void count_shouldReturnCorrectCount() {
        // Given
        entityManager.persist(testPond1);
        entityManager.persist(testPond2);
        entityManager.flush();

        // When
        long count = pondRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }
}
