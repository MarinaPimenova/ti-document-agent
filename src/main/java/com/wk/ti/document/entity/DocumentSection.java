package com.wk.ti.document.entity;

@SuppressWarnings("unused")
public interface DocumentSection {
    Long getDocumentId();
    String getFilename();
    Long getSectionId();
    Integer getSectionNumber();
    String getContent();
    Integer getStartPage();
    Integer getEndPage();
}
