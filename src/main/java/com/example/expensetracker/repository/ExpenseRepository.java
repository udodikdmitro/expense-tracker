package com.example.expensetracker.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.expensetracker.model.Expense;

// Spring Data generates the implementation at startup: CRUD methods come from JpaRepository,
// and the query for each findBy... method is derived from its name.
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByDateBetweenOrderByDateDescIdDesc(LocalDate from, LocalDate to);

    List<Expense> findByCategoryAndDateBetweenOrderByDateDescIdDesc(String category, LocalDate from, LocalDate to);

    // category == null means "all categories"
    @Query("""
            select coalesce(sum(e.amount), 0)
            from Expense e
            where e.date between :from and :to
              and (:category is null or e.category = :category)
            """)
    BigDecimal sumAmount(@Param("category") String category,
                         @Param("from") LocalDate from,
                         @Param("to") LocalDate to);
}
