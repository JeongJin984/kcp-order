package kcp.order.service;

import jakarta.persistence.EntityManager;
import kcp.order.service.entity.OrderItemJpaEntity;
import kcp.order.service.entity.OrderJpaEntity;
import kcp.order.service.entity.OrderStatus;
import kcp.order.service.repository.OrderRepository;
import kcp.product.repository.jpa.CategoryJpaRepository;
import kcp.product.service.entity.CategoryJpaEntity;
import kcp.product.service.entity.ProductJpaEntity;
import kcp.product.service.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrderServiceConcurrencyTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryJpaRepository categoryJpaRepository;
    @Autowired
    private EntityManager em; // 영속성 컨텍스트 강제 반영을 위해 추가

    @Test
    @DisplayName("여러 주문이 동시에 동일한 상품의 재고를 차감할 때 정합성이 유지되어야 한다")
    void changeStatus_concurrency_productStock() throws InterruptedException {
        // given
        int threadCount = 10;
        CategoryJpaEntity category = categoryJpaRepository.save(CategoryJpaEntity.createTestEmptyCategory("categoryA"));
        ProductJpaEntity product = ProductJpaEntity.createProduct("Concurrency Product", new BigDecimal("1000"), 100, List.of(category));
        productRepository.saveAndFlush(product); // DB에 즉시 반영
        Long productId = product.getId();

        List<Long> orderIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            OrderItemJpaEntity orderItem = OrderItemJpaEntity.createOrderItem(product, product.getPrice(), 1);
            OrderJpaEntity order = OrderJpaEntity.createOrder(List.of(orderItem));
            order.updateOrderStatus(OrderStatus.ACCEPTED);
            orderIds.add(orderRepository.saveAndFlush(order).getId());
        }
        em.clear(); // 영속성 컨텍스트를 비워 스레드들이 최신 DB 데이터를 조회하도록 함

        // when
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        for (Long orderId : orderIds) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    orderService.changeStatus(orderId, OrderStatus.COMPLETED);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        // then
        ProductJpaEntity updatedProduct = productRepository.findById(productId).orElseThrow();

        // 1. 모든 스레드가 성공했는지 확인
        assertThat(successCount.get()).isEqualTo(threadCount);
        // 2. 최종 재고가 정확히 차감되었는지 확인 (100 - 10 = 90)
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(90);
    }

    @Test
    @DisplayName("동일한 주문에 대해 동시에 상태 변경을 요청할 경우 한 번만 처리되어야 한다")
    void changeStatus_concurrency_sameOrder() throws InterruptedException {
        // given
        int threadCount = 5;
        CategoryJpaEntity category = categoryJpaRepository.save(CategoryJpaEntity.createTestEmptyCategory("categoryAB"));
        ProductJpaEntity product = ProductJpaEntity.createProduct("Same Order", new BigDecimal("1000"), 10, List.of(category));
        productRepository.saveAndFlush(product);
        Long productId = product.getId();

        OrderItemJpaEntity orderItem = OrderItemJpaEntity.createOrderItem(product, product.getPrice(), 1);
        OrderJpaEntity order = OrderJpaEntity.createOrder(List.of(orderItem));
        order.updateOrderStatus(OrderStatus.ACCEPTED);
        Long orderId = orderRepository.saveAndFlush(order).getId();
        em.clear();

        // when
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderService.changeStatus(orderId, OrderStatus.COMPLETED);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                    // 중복 요청 시 발생할 예외 (예: IllegalStateException 등)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        OrderJpaEntity updatedOrder = orderRepository.findById(orderId).orElseThrow();
        ProductJpaEntity updatedProduct = productRepository.findById(productId).orElseThrow();

        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        // 성공 횟수는 서비스 로직에 따라 다르겠지만, 재고 차감은 반드시 1번만 일어나야 함
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(9);
    }
}