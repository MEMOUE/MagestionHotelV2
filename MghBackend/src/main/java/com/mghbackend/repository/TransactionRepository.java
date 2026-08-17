package com.mghbackend.repository;

import com.mghbackend.entity.Transaction;
import com.mghbackend.enums.StatutTransaction;
import com.mghbackend.enums.TypeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByHotelIdOrderByDateTransactionDesc(Long hotelId);

    List<Transaction> findByHotelIdAndTypeOrderByDateTransactionDesc(Long hotelId, TypeTransaction type);

    List<Transaction> findByHotelIdAndStatutOrderByDateTransactionDesc(Long hotelId, StatutTransaction statut);

    List<Transaction> findByHotelIdAndDateTransactionBetweenOrderByDateTransactionAsc(
            Long hotelId, LocalDateTime debut, LocalDateTime fin);

    long countByHotelId(Long hotelId);
}