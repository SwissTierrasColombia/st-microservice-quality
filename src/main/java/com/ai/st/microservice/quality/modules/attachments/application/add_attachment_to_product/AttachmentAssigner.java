package com.ai.st.microservice.quality.modules.attachments.application.add_attachment_to_product;

import com.ai.st.microservice.quality.modules.attachments.domain.exceptions.AttachmentTypeNotSupportedToProduct;
import com.ai.st.microservice.quality.modules.attachments.domain.exceptions.AttachmentUnsupported;
import com.ai.st.microservice.quality.modules.attachments.domain.exceptions.NumberAttachmentsExceeded;
import com.ai.st.microservice.quality.modules.attachments.domain.contracts.DeliveryProductAttachmentRepository;
import com.ai.st.microservice.quality.modules.attachments.domain.DeliveryProductAttachment;
import com.ai.st.microservice.quality.modules.attachments.domain.DeliveryProductAttachmentDate;
import com.ai.st.microservice.quality.modules.attachments.domain.DeliveryProductAttachmentObservations;
import com.ai.st.microservice.quality.modules.attachments.domain.DeliveryProductAttachmentUUID;
import com.ai.st.microservice.quality.modules.attachments.domain.document.DeliveryProductDocumentAttachment;
import com.ai.st.microservice.quality.modules.attachments.domain.document.DocumentUrl;
import com.ai.st.microservice.quality.modules.attachments.domain.ftp.*;
import com.ai.st.microservice.quality.modules.attachments.domain.xtf.*;

import com.ai.st.microservice.quality.modules.delivered_products.domain.DeliveryProductStatusId;
import com.ai.st.microservice.quality.modules.delivered_products.domain.exceptions.DeliveryProductNotFound;
import com.ai.st.microservice.quality.modules.deliveries.domain.Delivery;
import com.ai.st.microservice.quality.modules.deliveries.domain.DeliveryId;
import com.ai.st.microservice.quality.modules.delivered_products.domain.contracts.DeliveryProductRepository;
import com.ai.st.microservice.quality.modules.deliveries.domain.contracts.DeliveryRepository;
import com.ai.st.microservice.quality.modules.deliveries.domain.exceptions.*;
import com.ai.st.microservice.quality.modules.delivered_products.domain.DeliveryProduct;
import com.ai.st.microservice.quality.modules.delivered_products.domain.DeliveryProductId;

import com.ai.st.microservice.quality.modules.products.domain.Product;
import com.ai.st.microservice.quality.modules.products.domain.ProductId;
import com.ai.st.microservice.quality.modules.products.domain.contracts.ProductRepository;

import com.ai.st.microservice.quality.modules.shared.application.CommandUseCase;
import com.ai.st.microservice.quality.modules.shared.domain.OperatorCode;
import com.ai.st.microservice.quality.modules.shared.domain.Service;
import com.ai.st.microservice.quality.modules.shared.domain.contracts.DateTime;
import com.ai.st.microservice.quality.modules.shared.domain.contracts.ILIMicroservice;
import com.ai.st.microservice.quality.modules.shared.domain.contracts.ILIOldMicroservice;
import com.ai.st.microservice.quality.modules.shared.domain.contracts.StoreFile;

import java.util.UUID;

@Service
public final class AttachmentAssigner implements CommandUseCase<AttachmentAssignerCommand> {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryProductRepository deliveryProductRepository;
    private final DeliveryProductAttachmentRepository attachmentRepository;
    private final ProductRepository productRepository;
    private final DateTime dateTime;
    private final StoreFile storeFile;
    private final ILIMicroservice iliMicroservice;
    private final ILIOldMicroservice iliOldMicroservice;

    private final static int MAXIMUM_ATTACHMENTS_PER_PRODUCT = 5;

    public AttachmentAssigner(DeliveryProductAttachmentRepository attachmentRepository,
            DeliveryRepository deliveryRepository, DeliveryProductRepository deliveryProductRepository,
            ProductRepository productRepository, DateTime dateTime, StoreFile storeFile,
            ILIMicroservice iliMicroservice, ILIOldMicroservice iliOldMicroservice) {
        this.attachmentRepository = attachmentRepository;
        this.productRepository = productRepository;
        this.dateTime = dateTime;
        this.storeFile = storeFile;
        this.iliMicroservice = iliMicroservice;
        this.deliveryRepository = deliveryRepository;
        this.deliveryProductRepository = deliveryProductRepository;
        this.iliOldMicroservice = iliOldMicroservice;
    }

    @Override
    public void handle(AttachmentAssignerCommand command) {

        DeliveryId deliveryId = new DeliveryId(command.deliveryId());
        DeliveryProductId deliveryProductId = new DeliveryProductId(command.deliveryProductId());
        OperatorCode operatorCode = new OperatorCode(command.operatorCode());

        DeliveryProduct deliveryProduct = verifyPermissions(deliveryId, deliveryProductId, operatorCode);

        DeliveryProductAttachment attachment = handleAttachment(command.attachment(), deliveryId, deliveryProductId,
                deliveryProduct.productId());

        attachmentRepository.save(attachment);

        changeProductStatusToPending(deliveryProductId);
    }

    private DeliveryProduct verifyPermissions(DeliveryId deliveryId, DeliveryProductId deliveryProductId,
            OperatorCode operatorCode) {

        // verify delivery exists
        Delivery delivery = deliveryRepository.search(deliveryId);
        if (delivery == null) {
            throw new DeliveryNotFound();
        }

        // verify delivery product exists
        DeliveryProduct deliveryProduct = deliveryProductRepository.search(deliveryProductId);
        if (deliveryProduct == null) {
            throw new DeliveryProductNotFound();
        }

        // verify owner of the delivery
        if (!delivery.deliveryBelongToOperator(operatorCode)) {
            throw new UnauthorizedToSearchDelivery();
        }

        // verify status of the delivery
        if (!delivery.isDraft() && !delivery.isInRemediation()) {
            throw new UnauthorizedToModifyDelivery(
                    "No se puede agregar adjuntos, porque el estado de la entrega no lo permite.");
        }

        // verify status of the delivery product
        if (deliveryProduct.isAccepted()) {
            throw new UnauthorizedToModifyDelivery("No se puede agregar adjuntos, porque el producto ya fue aceptado.");
        }

        // verify count attachments per product
        long count = attachmentRepository.findByDeliveryProductId(deliveryProductId).size() + 1;
        if (count > MAXIMUM_ATTACHMENTS_PER_PRODUCT) {
            throw new NumberAttachmentsExceeded(MAXIMUM_ATTACHMENTS_PER_PRODUCT);
        }

        return deliveryProduct;
    }

    private DeliveryProductAttachment handleAttachment(AttachmentAssignerCommand.Attachment attachment,
            DeliveryId deliveryId, DeliveryProductId deliveryProductId, ProductId productId) {

        if (!productIsXTF(productId) && attachment.isXTF()) {
            throw new AttachmentTypeNotSupportedToProduct();
        }

        DeliveryProductAttachmentDate attachmentDate = new DeliveryProductAttachmentDate(dateTime.now());

        if (attachment instanceof AttachmentAssignerCommand.XTFAttachment) {
            return handleXTFAttachment((AttachmentAssignerCommand.XTFAttachment) attachment, deliveryId,
                    deliveryProductId, attachmentDate);
        }

        if (attachment instanceof AttachmentAssignerCommand.FTPAttachment) {
            return handleFTPAttachment((AttachmentAssignerCommand.FTPAttachment) attachment, deliveryProductId,
                    attachmentDate);
        }

        if (attachment instanceof AttachmentAssignerCommand.DocumentAttachment) {
            return handleDocumentAttachment((AttachmentAssignerCommand.DocumentAttachment) attachment, deliveryId,
                    deliveryProductId, attachmentDate);
        }

        throw new AttachmentUnsupported();
    }

    private DeliveryProductXTFAttachment handleXTFAttachment(AttachmentAssignerCommand.XTFAttachment attachment,
            DeliveryId deliveryId, DeliveryProductId deliveryProductId, DeliveryProductAttachmentDate attachmentDate) {

        XTFVersion version = new XTFVersion(attachment.version());

        String namespace = buildNamespace(deliveryId);
        String pathUrl = storeFile.storeFilePermanently(attachment.bytes(), attachment.extension(), namespace);

        DeliveryProductAttachmentUUID identifierUUID = new DeliveryProductAttachmentUUID(UUID.randomUUID().toString());

        if (version.isOldVersion()) {
            iliOldMicroservice.sendToValidation(identifierUUID, pathUrl, false, false);
        } else {
            iliMicroservice.sendToValidation(identifierUUID, pathUrl, false, false);
        }

        return DeliveryProductXTFAttachment.create(identifierUUID,
                new DeliveryProductAttachmentObservations(attachment.observations()), deliveryProductId, attachmentDate,
                new XTFValid(null), new XTFUrl(pathUrl), version, new XTFStatus(XTFStatus.Status.IN_VALIDATION));
    }

    private DeliveryProductFTPAttachment handleFTPAttachment(AttachmentAssignerCommand.FTPAttachment attachment,
            DeliveryProductId deliveryProductId, DeliveryProductAttachmentDate attachmentDate) {
        return DeliveryProductFTPAttachment.create(new DeliveryProductAttachmentUUID(UUID.randomUUID().toString()),
                new DeliveryProductAttachmentObservations(attachment.observations()), deliveryProductId, attachmentDate,
                new FTPDomain(attachment.domain()), new FTPPort(attachment.port()),
                new FTPUsername(attachment.username()), new FTPPassword(attachment.password()));
    }

    private DeliveryProductDocumentAttachment handleDocumentAttachment(
            AttachmentAssignerCommand.DocumentAttachment attachment, DeliveryId deliveryId,
            DeliveryProductId deliveryProductId, DeliveryProductAttachmentDate attachmentDate) {

        String namespace = buildNamespace(deliveryId);
        String pathUrl = storeFile.storeFilePermanently(attachment.bytes(), attachment.extension(), namespace);

        return DeliveryProductDocumentAttachment.create(new DeliveryProductAttachmentUUID(UUID.randomUUID().toString()),
                new DeliveryProductAttachmentObservations(attachment.observations()), deliveryProductId, attachmentDate,
                new DocumentUrl(pathUrl));
    }

    private String buildNamespace(DeliveryId deliveryId) {
        return String.format("/entregas/%d", deliveryId.value());
    }

    private boolean productIsXTF(ProductId productId) {
        Product product = productRepository.search(productId);
        if (product != null) {
            return product.isConfiguredAsXTF();
        }
        return false;
    }

    private void changeProductStatusToPending(DeliveryProductId deliveryProductId) {
        deliveryProductRepository.changeStatus(deliveryProductId,
                DeliveryProductStatusId.fromValue(DeliveryProductStatusId.PENDING));
    }

}
