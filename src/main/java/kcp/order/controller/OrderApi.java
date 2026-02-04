package kcp.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kcp.order.controller.request.RegisterOrderReq;
import kcp.order.controller.request.UpdateOrderReq;
import kcp.order.controller.response.OrderResponse;
import kcp.common.response.PaginationResponse;
import kcp.order.service.dto.OrderCreateCmd;
import kcp.order.service.dto.OrderDetail;
import kcp.order.service.dto.OrderSearchCmd;
import kcp.order.service.entity.OrderStatus;
import kcp.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

import static org.springframework.util.StringUtils.hasText;

@Tag(name = "주문 api", description = "주문 api")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Validated
public class OrderApi {
    private final OrderService orderService;

    @Operation(summary = "주문(다건) 조회", description = "조회 조건을 바탕으로 주문을 페이지네이션하여 검색합니다.")
    @GetMapping
    public PaginationResponse<OrderResponse> getOrders(
        @Parameter(description = "주문 요청 일(시작)")
        @RequestParam(name = "orderAtSt", required = false) String orderAtSt,

        @Parameter(description = "주문 요청 일(끝)")
        @RequestParam(name = "orderAtEd", required = false) String orderAtEd,

        @Parameter(description = "주문 상품명")
        @RequestParam(name = "productName", required = false) String productName,

        @Parameter(description = "주문 상태")
        @RequestParam(name = "status", required = false) String status,

        @Parameter(description = "page number")
        @RequestParam(name = "page", required = false, defaultValue = "0") @Min(0) int pageNum,

        @Parameter(description = "Page size")
        @RequestParam(name = "size", required = false, defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        Page<OrderDetail> orders = orderService.getOrders(
            new OrderSearchCmd(
                hasText(orderAtSt) ? LocalDate.parse(orderAtSt).atStartOfDay() : null,
                hasText(orderAtEd) ? LocalDate.parse(orderAtEd).atStartOfDay() : null,
                productName,
                hasText(status) ? OrderStatus.of(status.split(",")): null
            ), PageRequest.of(pageNum, size)
        );

        return PaginationResponse.from(orders.map(OrderResponse::from));
    }

    @Operation(summary = "주문(단건) 조회", description = "주문을 id를 활용하여 한건 검색합니다.")
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(
        @PathVariable Long orderId
    ) {
        OrderDetail order = orderService.getOrder(orderId);
        return OrderResponse.from(order);
    }

    @Operation(summary = "주문 등록", description = "주문을 등록합니다.")
    @PostMapping
    public OrderResponse registerOrder(
        @Valid @RequestBody RegisterOrderReq body
    ) {
        OrderDetail order = orderService.registerOrder(new OrderCreateCmd(
            body.items().stream()
                .map(item -> new OrderCreateCmd.OrderItem(
                    item.productId(), item.count()
                )).toList()
        ));

        return OrderResponse.from(order);
    }

    @Operation(summary = "주문 수정", description = "주문 정보를 수정합니다.(현재 상태만 수정 가능합니다.)")
    @PostMapping("/{orderId}")
    public OrderResponse updateOrder(
        @PathVariable Long orderId,
        @Valid @RequestBody UpdateOrderReq body
    ) {
        OrderDetail order = orderService.changeStatus(orderId, body.orderStatus());
        return OrderResponse.from(order);
    }
}
