package com.ai.st.microservice.quality.modules.shared.infrastructure.persistence.entities;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "deliveries", schema = "quality")
public class DeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, length = 20, unique = true)
    private String code;

    @Column(name = "municipality_name", nullable = false)
    private String municipalityName;

    @Column(name = "department_name", nullable = false)
    private String departmentName;

    @Column(name = "municipality_code", nullable = false, length = 8)
    private String municipalityCode;

    @Column(name = "manager_name", nullable = false)
    private String managerName;

    @Column(name = "manager_code", nullable = false)
    private Long managerCode;

    @Column(name = "operator_name", nullable = false)
    private String operatorName;

    @Column(name = "operator_code", nullable = false)
    private Long operatorCode;

    @Column(name = "user_code", nullable = false)
    private Long userCode;

    @Column(name = "observations", nullable = false)
    private String observations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_status_id", referencedColumnName = "id", nullable = false)
    private DeliveryStatusEntity deliveryStatus;

    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "status_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date statusAt;

    @Column(name = "final_comments", length = 1000)
    private String finalComments;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL)
    private List<DeliveredProductEntity> products = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMunicipalityCode() {
        return municipalityCode;
    }

    public void setMunicipalityCode(String municipalityCode) {
        this.municipalityCode = municipalityCode;
    }

    public Long getManagerCode() {
        return managerCode;
    }

    public void setManagerCode(Long managerCode) {
        this.managerCode = managerCode;
    }

    public Long getOperatorCode() {
        return operatorCode;
    }

    public void setOperatorCode(Long operatorCode) {
        this.operatorCode = operatorCode;
    }

    public Long getUserCode() {
        return userCode;
    }

    public void setUserCode(Long userCode) {
        this.userCode = userCode;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public DeliveryStatusEntity getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(DeliveryStatusEntity deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<DeliveredProductEntity> getProducts() {
        return products;
    }

    public void setProducts(List<DeliveredProductEntity> products) {
        this.products = products;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMunicipalityName() {
        return municipalityName;
    }

    public void setMunicipalityName(String municipalityName) {
        this.municipalityName = municipalityName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Date getStatusAt() {
        return statusAt;
    }

    public void setStatusAt(Date statusAt) {
        this.statusAt = statusAt;
    }

    public String getFinalComments() {
        return finalComments;
    }

    public void setFinalComments(String finalComments) {
        this.finalComments = finalComments;
    }
}
