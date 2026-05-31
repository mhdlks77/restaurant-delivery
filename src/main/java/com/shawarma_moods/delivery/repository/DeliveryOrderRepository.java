package com.shawarma_moods.delivery.repository;

import com.shawarma_moods.delivery.model.DeliveryOrder;
import com.shawarma_moods.delivery.model.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {

    Optional<DeliveryOrder> findByWoocommerceOrderId(String woocommerceOrderId);
    Optional<DeliveryOrder> findByUberDeliveryId(String uberDeliveryId);

}
