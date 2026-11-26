package com.aquagrow.api.repository;

import com.aquagrow.api.model.SalaryPayment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("SalaryPaymentRepository Tests")
class SalaryPaymentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SalaryPaymentRepository salaryPaymentRepository;

    private SalaryPayment payment1;
    private SalaryPayment payment2;

    @BeforeEach
    void setUp() {
        payment1 = SalaryPayment.builder()
                .employeeId("emp-001")
                .date(LocalDate.of(2024, 1, 31))
                .baseSalary(5000.0)
                .advanceDeduction(500.0)
                .bonus(200.0)
                .netAmount(4700.0)
                .month("2024-01")
                .tenantId("tenant-1")
                .build();

        payment2 = SalaryPayment.builder()
                .employeeId("emp-001")
                .date(LocalDate.of(2024, 2, 29))
                .baseSalary(5000.0)
                .advanceDeduction(0.0)
                .bonus(500.0)
                .netAmount(5500.0)
                .month("2024-02")
                .tenantId("tenant-1")
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve salary payment")
    void savePayment_shouldPersistPayment() {
        // When
        SalaryPayment savedPayment = salaryPaymentRepository.save(payment1);

        // Then
        assertThat(savedPayment).isNotNull();
        assertThat(savedPayment.getId()).isNotNull();
        assertThat(savedPayment.getEmployeeId()).isEqualTo("emp-001");
        assertThat(savedPayment.getBaseSalary()).isEqualTo(5000.0);
        assertThat(savedPayment.getNetAmount()).isEqualTo(4700.0);
    }

    @Test
    @DisplayName("Should find all salary payments")
    void findAll_shouldReturnAllPayments() {
        // Given
        entityManager.persist(payment1);
        entityManager.persist(payment2);
        entityManager.flush();

        // When
        List<SalaryPayment> payments = salaryPaymentRepository.findAll();

        // Then
        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(SalaryPayment::getMonth)
                .containsExactlyInAnyOrder("2024-01", "2024-02");
    }

    @Test
    @DisplayName("Should find payment by id")
    void findById_shouldReturnPayment_whenExists() {
        // Given
        SalaryPayment savedPayment = entityManager.persist(payment1);
        entityManager.flush();

        // When
        Optional<SalaryPayment> foundPayment = salaryPaymentRepository.findById(savedPayment.getId());

        // Then
        assertThat(foundPayment).isPresent();
        assertThat(foundPayment.get().getEmployeeId()).isEqualTo("emp-001");
        assertThat(foundPayment.get().getMonth()).isEqualTo("2024-01");
    }

    @Test
    @DisplayName("Should return empty when payment not found by id")
    void findById_shouldReturnEmpty_whenNotExists() {
        // When
        Optional<SalaryPayment> foundPayment = salaryPaymentRepository.findById("non-existent-id");

        // Then
        assertThat(foundPayment).isEmpty();
    }

    @Test
    @DisplayName("Should find payments by employee ID")
    void findByEmployeeId_shouldReturnAllPaymentsForEmployee() {
        // Given
        entityManager.persist(payment1);
        entityManager.persist(payment2);
        SalaryPayment payment3 = SalaryPayment.builder()
                .employeeId("emp-002")
                .date(LocalDate.of(2024, 1, 31))
                .baseSalary(6000.0)
                .month("2024-01")
                .tenantId("tenant-1")
                .build();
        entityManager.persist(payment3);
        entityManager.flush();

        // When
        List<SalaryPayment> payments = salaryPaymentRepository.findByEmployeeId("emp-001");

        // Then
        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(SalaryPayment::getEmployeeId)
                .containsOnly("emp-001");
    }

    @Test
    @DisplayName("Should find payments by month")
    void findByMonth_shouldReturnAllPaymentsForMonth() {
        // Given
        entityManager.persist(payment1);
        entityManager.persist(payment2);
        SalaryPayment payment3 = SalaryPayment.builder()
                .employeeId("emp-002")
                .date(LocalDate.of(2024, 1, 31))
                .baseSalary(6000.0)
                .month("2024-01")
                .tenantId("tenant-1")
                .build();
        entityManager.persist(payment3);
        entityManager.flush();

        // When
        List<SalaryPayment> payments = salaryPaymentRepository.findByMonth("2024-01");

        // Then
        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(SalaryPayment::getMonth)
                .containsOnly("2024-01");
    }

    @Test
    @DisplayName("Should find payment by employee ID and month")
    void findByEmployeeIdAndMonth_shouldReturnPayment_whenExists() {
        // Given
        entityManager.persist(payment1);
        entityManager.persist(payment2);
        entityManager.flush();

        // When
        Optional<SalaryPayment> foundPayment = salaryPaymentRepository.findByEmployeeIdAndMonth("emp-001", "2024-01");

        // Then
        assertThat(foundPayment).isPresent();
        assertThat(foundPayment.get().getEmployeeId()).isEqualTo("emp-001");
        assertThat(foundPayment.get().getMonth()).isEqualTo("2024-01");
        assertThat(foundPayment.get().getNetAmount()).isEqualTo(4700.0);
    }

    @Test
    @DisplayName("Should return empty when payment not found by employee ID and month")
    void findByEmployeeIdAndMonth_shouldReturnEmpty_whenNotExists() {
        // When
        Optional<SalaryPayment> foundPayment = salaryPaymentRepository.findByEmployeeIdAndMonth("emp-999", "2024-12");

        // Then
        assertThat(foundPayment).isEmpty();
    }

    @Test
    @DisplayName("Should find payments between dates")
    void findByDateBetween_shouldReturnPaymentsInRange() {
        // Given
        entityManager.persist(payment1);
        entityManager.persist(payment2);
        SalaryPayment payment3 = SalaryPayment.builder()
                .employeeId("emp-003")
                .date(LocalDate.of(2024, 3, 31))
                .baseSalary(7000.0)
                .month("2024-03")
                .tenantId("tenant-1")
                .build();
        entityManager.persist(payment3);
        entityManager.flush();

        // When
        List<SalaryPayment> payments = salaryPaymentRepository.findByDateBetween(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 2, 29)
        );

        // Then
        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(SalaryPayment::getMonth)
                .containsExactlyInAnyOrder("2024-01", "2024-02");
    }

    @Test
    @DisplayName("Should update salary payment")
    void updatePayment_shouldPersistChanges() {
        // Given
        SalaryPayment savedPayment = entityManager.persist(payment1);
        entityManager.flush();
        entityManager.clear();

        // When
        savedPayment.setBonus(1000.0);
        savedPayment.setNetAmount(5500.0);
        SalaryPayment updatedPayment = salaryPaymentRepository.save(savedPayment);

        // Then
        assertThat(updatedPayment.getBonus()).isEqualTo(1000.0);
        assertThat(updatedPayment.getNetAmount()).isEqualTo(5500.0);
    }

    @Test
    @DisplayName("Should delete salary payment")
    void deletePayment_shouldRemovePayment() {
        // Given
        SalaryPayment savedPayment = entityManager.persist(payment1);
        entityManager.flush();
        String paymentId = savedPayment.getId();

        // When
        salaryPaymentRepository.delete(savedPayment);
        entityManager.flush();

        // Then
        Optional<SalaryPayment> deletedPayment = salaryPaymentRepository.findById(paymentId);
        assertThat(deletedPayment).isEmpty();
    }

    @Test
    @DisplayName("Should count all payments")
    void count_shouldReturnCorrectCount() {
        // Given
        entityManager.persist(payment1);
        entityManager.persist(payment2);
        entityManager.flush();

        // When
        long count = salaryPaymentRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should check if payment exists by employee ID and month")
    void existsByEmployeeIdAndMonth_shouldReturnTrue_whenExists() {
        // Given
        entityManager.persist(payment1);
        entityManager.flush();

        // When
        boolean exists = salaryPaymentRepository.existsByEmployeeIdAndMonth("emp-001", "2024-01");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when payment does not exist by employee ID and month")
    void existsByEmployeeIdAndMonth_shouldReturnFalse_whenNotExists() {
        // When
        boolean exists = salaryPaymentRepository.existsByEmployeeIdAndMonth("emp-999", "2024-12");

        // Then
        assertThat(exists).isFalse();
    }
}
