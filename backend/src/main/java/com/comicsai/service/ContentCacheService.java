package com.comicsai.service;

import com.comicsai.model.enums.ContentType;
import com.comicsai.model.vo.ContentDetailVO;
import com.comicsai.model.vo.ContentVO;
import com.comicsai.model.vo.PageVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ContentCacheService {

    private static final Logger log = LoggerFactory.getLogger(ContentCacheService.class);

    private static final String HOME_PAGE_KEY = "content:home:page:";
    private static final String TYPE_LIST_KEY_PREFIX = "content:list:";
    private static final String DETAIL_KEY = "content:detail:";
    private static final String HOT_KEY = "content:hot";

    private static final long LIST_TTL_MINUTES = 10;
    private static final long DETAIL_TTL_MINUTES = 30;
    private static final long HOT_TTL_MINUTES = 30;
    private static final long EMPTY_TTL_MINUTES = 1;

    private final ContentService contentService;
    private final RedisTemplate<String, Object> redisTemplate;

    public ContentCacheService(ContentService contentService, RedisTemplate<String, Object> redisTemplate) {
        this.contentService = contentService;
        this.redisTemplate = redisTemplate;
    }

    @SuppressWarnings("unchecked")
    public PageVO<ContentVO> getPublishedContents(Integer page, Integer size, ContentType type) {
        String cacheKey = buildListCacheKey(page, type);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof PageVO<?>) {
                return (PageVO<ContentVO>) cached;
            }
        } catch (Exception e) {
            log.warn("Redis read failed for key {}, falling back to DB", cacheKey, e);
        }

        PageVO<ContentVO> result = contentService.getPublishedContents(page, size, type);

        try {
            long ttl = result.getRecords().isEmpty() ? EMPTY_TTL_MINUTES : LIST_TTL_MINUTES;
            redisTemplate.opsForValue().set(cacheKey, result, ttl, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis write failed for key {}", cacheKey, e);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public ContentDetailVO getContentDetail(Long contentId) {
        String cacheKey = DETAIL_KEY + contentId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof ContentDetailVO) {
                return (ContentDetailVO) cached;
            }
        } catch (Exception e) {
            log.warn("Redis read failed for key {}, falling back to DB", cacheKey, e);
        }

        ContentDetailVO result = contentService.getContentDetail(contentId);

        try {
            redisTemplate.opsForValue().set(cacheKey, result, DETAIL_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis write failed for key {}", cacheKey, e);
        }

        return result;
    }

    public PageVO<ContentVO> searchContents(String keyword, Integer page, Integer size) {
        // Search is not cached — keywords are too varied for effective caching
        return contentService.searchContents(keyword, page, size);
    }

    public void evictContentDetail(Long contentId) {
        try {
            redisTemplate.delete(DETAIL_KEY + contentId);
        } catch (Exception e) {
            log.warn("Redis evict failed for content detail {}", contentId, e);
        }
    }

    public void evictListCaches() {
        try {
            Set<String> homeKeys = redisTemplate.keys(HOME_PAGE_KEY + "*");
            if (homeKeys != null && !homeKeys.isEmpty()) {
                redisTemplate.delete(homeKeys);
            }
            Set<String> typeKeys = redisTemplate.keys(TYPE_LIST_KEY_PREFIX + "*");
            if (typeKeys != null && !typeKeys.isEmpty()) {
                redisTemplate.delete(typeKeys);
            }
        } catch (Exception e) {
            log.warn("Redis evict failed for list caches", e);
        }
    }

    public void evictAll(Long contentId) {
        evictContentDetail(contentId);
        evictListCaches();
    }

    private String buildListCacheKey(Integer page, ContentType type) {
        if (type != null) {
            return TYPE_LIST_KEY_PREFIX + type.getValue() + ":page:" + page;
        }
        return HOME_PAGE_KEY + page;
    }
}
