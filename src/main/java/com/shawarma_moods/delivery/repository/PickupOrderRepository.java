package com.shawarma_moods.delivery.repository;

import com.shawarma_moods.delivery.model.PickupOrder;
import com.shawarma_moods.delivery.model.PickupStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PickupOrderRepository extends JpaRepository<PickupOrder, Long> {

    Optional<PickupOrder> findByWoocommerceOrderId(String woocommerceOrderId);
    List<PickupOrder> findByStatusOrderByCreatedAtDesc(PickupStatus status);
    List<PickupOrder> findAllByOrderByCreatedAtDesc();
}
