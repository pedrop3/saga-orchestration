package com.learn.orchestrated.orchestrator.service.saga;

import static com.learn.orchestrated.orchestrator.service.enums.EventSourceEnum.*;
import static com.learn.orchestrated.orchestrator.service.enums.TopicsEnum.*;
import static com.learn.sagacommons.enums.SagaStatusEnum.*;

public final class SagaHandler {
    private SagaHandler() {
    }

    public static final Object[][] SAGA_HANDLER = {
            { ORCHESTRATOR, SUCCESS, PRODUCT_VALIDATION_SUCCESS },
            { ORCHESTRATOR, FAIL, FINISH_FAIL },

            { PRODUCT_VALIDATION_SERVICE, ROLLBACK, PRODUCT_VALIDATION_FAIL },
            { PRODUCT_VALIDATION_SERVICE, FAIL, FINISH_FAIL },
            { PRODUCT_VALIDATION_SERVICE, SUCCESS, PAYMENT_SUCCESS },

            { PAYMENT_SERVICE, ROLLBACK, PAYMENT_FAIL },
            { PAYMENT_SERVICE, FAIL, PRODUCT_VALIDATION_FAIL },
            { PAYMENT_SERVICE, SUCCESS, INVENTORY_SUCCESS },

            { INVENTORY_SERVICE, ROLLBACK, INVENTORY_FAIL },
            { INVENTORY_SERVICE, FAIL, PAYMENT_FAIL },
            { INVENTORY_SERVICE, SUCCESS, FINISH_SUCCESS }
    };

    public static final int EVENT_SOURCE_INDEX = 0;
    public static final int SAGA_STATUS_INDEX = 1;
    public static final int TOPIC_INDEX = 2;
}
