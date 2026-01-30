package com.aquagrow.api.service;

import com.aquagrow.api.exception.BusinessException;
import com.aquagrow.api.model.Employee;
import com.aquagrow.api.model.SalaryPayment;
import com.aquagrow.api.repository.EmployeeRepository;
import com.aquagrow.api.repository.SalaryPaymentRepository;
import com.aquagrow.api.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalaryPaymentService Tests")
class SalaryPaymentServiceTest {

    @Mock
    private SalaryPaymentRepository salaryPaymentRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private SalaryPaymentService salaryPaymentService;

    private Employee testEmployee;
    private SalaryPayment testPayment;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("tenant-1");

        testEmployee = Employee.builder()
                .id("emp-001")
                .name("John Doe")
                .salary(5000.0)
                .advanceTaken(500.0)
                .tenantId("tenant-1")
                .status("Active")
                .build();

        testPayment = SalaryPayment.builder()
                .id("pay-001")
                .employeeId("emp-001")
                .date(LocalDate.of(2024, 1, 31))
                .baseSalary(5000.0)
                .advanceDeduction(500.0)
                .bonus(200.0)
                .netAmount(4700.0)
                .month("2024-01")
                .tenantId("tenant-1")
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should calculate net amount correctly")
    void calculateNetAmount_shouldReturnCorrectAmount() {
        // Given
        double baseSalary = 5000.0;
        double advanceDeduction = 500.0;
        double bonus = 200.0;

        // When
        double netAmount = salaryPaymentService.calculateNetAmount(baseSalary, advanceDeduction, bonus);

        // Then
        assertThat(netAmount).isEqualTo(4700.0);
    }

    @Test
    @DisplayName("Should calculate net amount with zero advance and bonus")
    void calculateNetAmount_shouldHandleZeroValues() {
        // Given
        double baseSalary = 5000.0;
        double advanceDeduction = 0.0;
        double bonus = 0.0;

        // When
        double netAmount = salaryPaymentService.calculateNetAmount(baseSalary, advanceDeduction, bonus);

        // Then
        assertThat(netAmount).isEqualTo(5000.0);
    }

    @Test
    @DisplayName("Should calculate net amount with null advance and bonus as zero")
    void calculateNetAmount_shouldHandleNullValues() {
        // Given
        double baseSalary = 5000.0;

        // When
        double netAmount = salaryPaymentService.calculateNetAmount(baseSalary, null, null);

        // Then
        assertThat(netAmount).isEqualTo(5000.0);
    }

    @Test
    @DisplayName("Should create salary payment successfully")
    void createSalaryPayment_shouldCreatePayment_whenValid() {
        // Given
        when(employeeRepository.findById("emp-001")).thenReturn(Optional.of(testEmployee));
        when(salaryPaymentRepository.existsByEmployeeIdAndMonth("emp-001", "2024-01")).thenReturn(false);
        when(salaryPaymentRepository.save(any(SalaryPayment.class))).thenReturn(testPayment);

        // When
        SalaryPayment created = salaryPaymentService.createSalaryPayment(
                "emp-001",
                LocalDate.of(2024, 1, 31),
                5000.0,
                500.0,
                200.0
        );

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getEmployeeId()).isEqualTo("emp-001");
        assertThat(created.getNetAmount()).isEqualTo(4700.0);
        verify(salaryPaymentRepository).save(any(SalaryPayment.class));
    }

    @Test
    @DisplayName("Should throw exception when employee not found")
    void createSalaryPayment_shouldThrowException_whenEmployeeNotFound() {
        // Given
        when(employeeRepository.findById("emp-999")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> salaryPaymentService.createSalaryPayment(
                "emp-999",
                LocalDate.of(2024, 1, 31),
                5000.0,
                500.0,
                200.0
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Employee not found");
    }

    @Test
    @DisplayName("Should throw exception when payment already exists for month")
    void createSalaryPayment_shouldThrowException_whenDuplicatePayment() {
        // Given
        when(employeeRepository.findById("emp-001")).thenReturn(Optional.of(testEmployee));
        when(salaryPaymentRepository.existsByEmployeeIdAndMonth("emp-001", "2024-01")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> salaryPaymentService.createSalaryPayment(
                "emp-001",
                LocalDate.of(2024, 1, 31),
                5000.0,
                500.0,
                200.0
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Salary already paid");
    }

    @Test
    @DisplayName("Should set tenant ID when creating payment")
    void createSalaryPayment_shouldSetTenantId() {
        // Given
        when(employeeRepository.findById("emp-001")).thenReturn(Optional.of(testEmployee));
        when(salaryPaymentRepository.existsByEmployeeIdAndMonth("emp-001", "2024-01")).thenReturn(false);
        when(salaryPaymentRepository.save(any(SalaryPayment.class))).thenAnswer(invocation -> {
            SalaryPayment payment = invocation.getArgument(0);
            assertThat(payment.getTenantId()).isEqualTo("tenant-1");
            return payment;
        });

        // When
        salaryPaymentService.createSalaryPayment(
                "emp-001",
                LocalDate.of(2024, 1, 31),
                5000.0,
                500.0,
                200.0
        );

        // Then
        verify(salaryPaymentRepository).save(any(SalaryPayment.class));
    }

    @Test
    @DisplayName("Should reset employee advance after payment")
    void createSalaryPayment_shouldResetEmployeeAdvance() {
        // Given
        when(employeeRepository.findById("emp-001")).thenReturn(Optional.of(testEmployee));
        when(salaryPaymentRepository.existsByEmployeeIdAndMonth("emp-001", "2024-01")).thenReturn(false);
        when(salaryPaymentRepository.save(any(SalaryPayment.class))).thenReturn(testPayment);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        salaryPaymentService.createSalaryPayment(
                "emp-001",
                LocalDate.of(2024, 1, 31),
                5000.0,
                500.0,
                200.0
        );

        // Then
        verify(employeeRepository).save(argThat(emp -> emp.getAdvanceTaken() == 0.0));
    }

    @Test
    @DisplayName("Should get employee salary history")
    void getEmployeeSalaryHistory_shouldReturnAllPayments() {
        // Given
        SalaryPayment payment1 = SalaryPayment.builder()
                .employeeId("emp-001")
                .month("2024-01")
                .netAmount(4700.0)
                .build();
        SalaryPayment payment2 = SalaryPayment.builder()
                .employeeId("emp-001")
                .month("2024-02")
                .netAmount(5200.0)
                .build();

        when(salaryPaymentRepository.findByEmployeeId("emp-001"))
                .thenReturn(Arrays.asList(payment1, payment2));

        // When
        List<SalaryPayment> history = salaryPaymentService.getEmployeeSalaryHistory("emp-001");

        // Then
        assertThat(history).hasSize(2);
        assertThat(history).extracting(SalaryPayment::getMonth)
                .containsExactly("2024-01", "2024-02");
    }

    @Test
    @DisplayName("Should get payments for specific month")
    void getPaymentsForMonth_shouldReturnAllPaymentsForMonth() {
        // Given
        SalaryPayment payment1 = SalaryPayment.builder()
                .employeeId("emp-001")
                .month("2024-01")
                .build();
        SalaryPayment payment2 = SalaryPayment.builder()
                .employeeId("emp-002")
                .month("2024-01")
                .build();

        when(salaryPaymentRepository.findByMonth("2024-01"))
                .thenReturn(Arrays.asList(payment1, payment2));

        // When
        List<SalaryPayment> payments = salaryPaymentService.getPaymentsForMonth("2024-01");

        // Then
        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(SalaryPayment::getMonth)
                .containsOnly("2024-01");
    }

    @Test
    @DisplayName("Should format month from date correctly")
    void formatMonth_shouldReturnYearMonthFormat() {
        // Given
        LocalDate date = LocalDate.of(2024, 1, 15);

        // When
        String month = salaryPaymentService.formatMonth(date);

        // Then
        assertThat(month).isEqualTo("2024-01");
    }

    @Test
    @DisplayName("Should format month from date with double-digit month")
    void formatMonth_shouldHandleDoubleDigitMonth() {
        // Given
        LocalDate date = LocalDate.of(2024, 12, 31);

        // When
        String month = salaryPaymentService.formatMonth(date);

        // Then
        assertThat(month).isEqualTo("2024-12");
    }
}
