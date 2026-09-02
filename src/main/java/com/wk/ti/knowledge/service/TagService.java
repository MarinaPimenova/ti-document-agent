package com.wk.ti.knowledge.service;

import com.wk.ti.knowledge.entity.Tag;
import com.wk.ti.knowledge.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private List<Tag> tags;

    public String tags() {
        if (tags == null || tags.isEmpty()) {
            tags = tagRepository.findAll();
        }

        return String.join(",", tags.stream().map(Tag::getTag).toList());
    }

    public List<Tag> findByTagIn(List<String> tags) {
        return tagRepository.findByTagIn(tags);
    }

    public List<Tag> getDefaultTags() {
        return tagRepository.findByTagIn(List.of("General"));
    }
}
