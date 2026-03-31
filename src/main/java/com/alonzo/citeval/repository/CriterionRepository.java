package com.alonzo.citeval.repository;

import com.alonzo.citeval.model.entity.Criterion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CriterionRepository extends JpaRepository<Criterion, Long> {
}
