package com.wk.ti.document.service;

import com.wk.ti.document.entity.DocumentProjection;
import com.wk.ti.document.repository.DocumentRepository;
import com.wk.ti.document.entity.DocumentSection;
import com.wk.ti.document.repository.QuestionGenerationSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final QuestionGenerationSectionRepository sectionRepository;

    public List<DocumentProjection> getDocuments() {
        return documentRepository.findAll().stream()
                .map(entity -> new DocumentProjection(
                        entity.getId(), entity.getFilename()
                ))
                .toList();
    }

    public List<DocumentSection> getSections(Long documentId) {
        return sectionRepository.findByDocumentId(documentId);
    }
}
