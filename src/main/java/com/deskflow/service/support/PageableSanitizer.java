package com.deskflow.service.support;

import com.deskflow.exception.BusinessRuleException;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PageableSanitizer {

    private static final Set<String> TICKET_SORT_FIELDS = Set.of(
            "id", "title", "status", "priority", "category", "createdAt", "updatedAt"
    );
    private static final Set<String> COMMENT_SORT_FIELDS = Set.of("id", "createdAt");
    private static final Set<String> USER_SORT_FIELDS = Set.of(
            "id", "name", "email", "role", "active", "createdAt", "updatedAt"
    );
    private static final int MAX_PAGE_SIZE = 100;

    public Pageable forTickets(Pageable pageable) {
        return sanitize(pageable, TICKET_SORT_FIELDS,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    public Pageable forComments(Pageable pageable) {
        return sanitize(pageable, COMMENT_SORT_FIELDS,
                Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
    }

    public Pageable forUsers(Pageable pageable) {
        return sanitize(pageable, USER_SORT_FIELDS,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    private Pageable sanitize(Pageable pageable, Set<String> allowedFields, Sort defaultSort) {
        List<Sort.Order> orders = pageable.getSort().stream()
                .map(order -> {
                    if (!allowedFields.contains(order.getProperty())) {
                        throw new BusinessRuleException(
                                "Campo de ordenação inválido: " + order.getProperty()
                        );
                    }
                    return order;
                })
                .toList();

        Sort sort = orders.isEmpty() ? defaultSort : Sort.by(orders);
        int page = Math.max(pageable.getPageNumber(), 0);
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        return PageRequest.of(page, size, sort);
    }
}
