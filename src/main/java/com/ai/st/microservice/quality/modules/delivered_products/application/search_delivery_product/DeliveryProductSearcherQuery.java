package com.ai.st.microservice.quality.modules.delivered_products.application.search_delivery_product;

import com.ai.st.microservice.quality.modules.shared.application.Roles;
import com.ai.st.microservice.quality.modules.shared.application.Query;

public final class DeliveryProductSearcherQuery implements Query {

    private final Long deliveryId;
    private final Long deliveryProductId;
    private final Roles role;
    private final Long entityCode;

    public DeliveryProductSearcherQuery(Long deliveryId, Long deliveryProductId, Roles role, Long entityCode) {
        this.deliveryId = deliveryId;
        this.deliveryProductId = deliveryProductId;
        this.role = role;
        this.entityCode = entityCode;
    }

    public Long deliveryProductId() {
        return deliveryProductId;
    }

    public Roles role() {
        return role;
    }

    public Long entityCode() {
        return entityCode;
    }

    public Long deliveryId() {
        return deliveryId;
    }
}
