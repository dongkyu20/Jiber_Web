package com.jiber.backend.news.controller;

import com.jiber.backend.news.dto.NewsFeedResponse;
import com.jiber.backend.news.dto.NewsSearchRequest;
import com.jiber.backend.news.service.NewsService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping
    public NewsFeedResponse searchNews(@Valid @ParameterObject @ModelAttribute NewsSearchRequest request) {
        return newsService.search(request);
    }
}
