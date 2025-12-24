package com.aquagrow.api.dto;

public class OrderMessageResponse {
    private String message;
    private String whatsappLink;
    private String supplierPhone;

    public OrderMessageResponse() {
    }

    public OrderMessageResponse(String message, String whatsappLink, String supplierPhone) {
        this.message = message;
        this.whatsappLink = whatsappLink;
        this.supplierPhone = supplierPhone;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getWhatsappLink() {
        return whatsappLink;
    }

    public void setWhatsappLink(String whatsappLink) {
        this.whatsappLink = whatsappLink;
    }

    public String getSupplierPhone() {
        return supplierPhone;
    }

    public void setSupplierPhone(String supplierPhone) {
        this.supplierPhone = supplierPhone;
    }
}
