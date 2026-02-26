package ru.kuzmich.objectmapperproject.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.kuzmich.objectmapperproject.model.Order;
import ru.kuzmich.objectmapperproject.model.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerCustomerId(Long customerId);

    List<Order> findByOrderStatus(OrderStatus status);

    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);
}
