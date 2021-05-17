package com.ai.st.microservice.quality.modules.shared.domain.contracts;

import com.ai.st.microservice.quality.modules.deliveries.domain.products.attachments.DeliveryProductAttachmentUUID;

public interface ILIMicroservice {

    void sendToValidation(DeliveryProductAttachmentUUID attachmentUUID, String pathFile, boolean skipGeometryValidation, boolean skipErrors);

}
