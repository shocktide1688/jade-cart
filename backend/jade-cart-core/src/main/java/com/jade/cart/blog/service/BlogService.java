package com.jade.cart.blog.service;

import com.jade.cart.blog.entity.BlogPost;
import com.jade.cart.content.service.ContentService;
import com.jade.cart.catalog.service.HtmlSanitizer;
import com.jade.common.exception.BizException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 博客: CRUD + 受限 Markdown 渲染 (复用 ContentService 渲染器)
 */
@ApplicationScoped
public class BlogService {

    @Inject
    ContentService contentService;

    public List<BlogPost> posts() {
        return BlogPost.list("1=1 order by id desc");
    }

    @Transactional
    public BlogPost create(String slug, String title, String excerpt, String bodyMarkdown,
                           String bodyHtml, String coverPath, String seoDescription,
                           String seoKeywords, boolean publish) {
        String normalized = slug == null || slug.isBlank()
                ? slugify(title) + "-" + UUID.randomUUID().toString().substring(0, 6)
                : slug.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
        if (BlogPost.count("slug", normalized) > 0) {
            throw new BizException("slug 已存在: " + normalized);
        }
        if (title == null || title.isBlank()) {
            throw new BizException("标题不能为空");
        }
        BlogPost post = new BlogPost();
        post.slug = normalized;
        post.title = title;
        post.excerpt = excerpt;
        post.coverPath = coverPath;
        post.seoDescription = seoDescription;
        post.seoKeywords = seoKeywords;
        setBody(post, bodyMarkdown, bodyHtml);
        if (publish) {
            post.status = BlogPost.STATUS_ACTIVE;
            post.publishedAt = LocalDateTime.now();
        }
        post.persist();
        return post;
    }

    @Transactional
    public BlogPost update(Long id, String title, String excerpt, String bodyMarkdown,
                           String bodyHtml, String coverPath, String seoDescription,
                           String seoKeywords, String status) {
        BlogPost post = BlogPost.findById(id);
        if (post == null) {
            throw new BizException("文章不存在");
        }
        if (title != null && !title.isBlank()) {
            post.title = title;
        }
        if (excerpt != null) {
            post.excerpt = excerpt;
        }
        if (coverPath != null) {
            post.coverPath = coverPath.isBlank() ? null : coverPath;
        }
        if (seoDescription != null) {
            post.seoDescription = seoDescription;
        }
        if (seoKeywords != null) {
            post.seoKeywords = seoKeywords.isBlank() ? null : seoKeywords;
        }
        // 富文本编辑优先; markdown 兼容旧数据初始渲染
        if (bodyHtml != null) {
            post.bodyHtml = HtmlSanitizer.sanitize(bodyHtml);
            post.bodyMarkdown = null;
        } else if (bodyMarkdown != null) {
            setBody(post, bodyMarkdown);
        }
        if (status != null) {
            if (!BlogPost.STATUS_ACTIVE.equals(status) && !BlogPost.STATUS_DRAFT.equals(status)) {
                throw new BizException("非法状态");
            }
            // 草稿→发布: 记录发布时间
            if (BlogPost.STATUS_ACTIVE.equals(status) && post.publishedAt == null) {
                post.publishedAt = LocalDateTime.now();
            }
            post.status = status;
        }
        post.persist();
        return post;
    }

    @Transactional
    public void delete(Long id) {
        if (!BlogPost.deleteById(id)) {
            throw new BizException("文章不存在");
        }
    }

    private void setBody(BlogPost post, String bodyMarkdown) {
        post.bodyMarkdown = bodyMarkdown;
        post.bodyHtml = HtmlSanitizer.sanitize(contentService.renderMarkdown(bodyMarkdown));
    }

    /** 富文本优先: bodyHtml 非空走净化直存 (wangEditor); 否则走 markdown 渲染 */
    private void setBody(BlogPost post, String bodyMarkdown, String bodyHtml) {
        if (bodyHtml != null && !bodyHtml.isBlank()) {
            post.bodyHtml = HtmlSanitizer.sanitize(bodyHtml);
            post.bodyMarkdown = null;
        } else {
            setBody(post, bodyMarkdown);
        }
    }

    private String slugify(String s) {
        return (s == null ? "post" : s.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")).replaceAll("^-+|-+$", "");
    }
}
