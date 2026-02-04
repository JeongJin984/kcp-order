package kcp.order.repository.predicate;

import com.querydsl.core.types.dsl.BooleanExpression;
import kcp.order.service.dto.OrderSearchCmd;
import lombok.NoArgsConstructor;

import static kcp.order.domain.order.entity.QOrderJpaEntity.orderJpaEntity;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class OrderSearchPredicate {
    public static BooleanExpression[] from(OrderSearchCmd command) {
        return new BooleanExpression[] {
            byOrderDateSt(command),
            byOrderDateEd(command),
            byStatus(command)
        };
    }

    private static BooleanExpression byOrderDateSt(OrderSearchCmd command) {
        return command.orderDateSt() == null ? null : orderJpaEntity.orderDate.after(command.orderDateSt());
    }

    private static BooleanExpression byOrderDateEd(OrderSearchCmd command) {
        return command.orderDateEd() == null ? null : orderJpaEntity.orderDate.before(command.orderDateEd());
    }

    private static BooleanExpression byStatus(OrderSearchCmd command) {
        return command.orderStatus() == null
            ? null : orderJpaEntity.status.in(command.orderStatus());
    }
}
