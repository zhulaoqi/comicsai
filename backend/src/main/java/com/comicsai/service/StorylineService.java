package com.comicsai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comicsai.common.exception.BusinessException;
import com.comicsai.common.exception.EntityNotFoundException;
import com.comicsai.mapper.GenerationConfigMapper;
import com.comicsai.mapper.StorylineMapper;
import com.comicsai.mapper.StorylineVersionMapper;
import com.comicsai.model.dto.GenerationConfigDTO;
import com.comicsai.model.dto.StorylineCreateDTO;
import com.comicsai.model.dto.StorylineQueryDTO;
import com.comicsai.model.dto.StorylineUpdateDTO;
import com.comicsai.model.entity.GenerationConfig;
import com.comicsai.model.entity.Storyline;
import com.comicsai.model.entity.StorylineVersion;
import com.comicsai.model.enums.StorylineStatus;
import com.comicsai.model.vo.PageVO;
import com.comicsai.model.vo.StorylineDetailVO;
import com.comicsai.model.vo.StorylineVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StorylineService {

    private final StorylineMapper storylineMapper;
    private final StorylineVersionMapper storylineVersionMapper;
    private final GenerationConfigMapper generationConfigMapper;
    private final ObjectMapper objectMapper;

    public StorylineService(StorylineMapper storylineMapper,
                            StorylineVersionMapper storylineVersionMapper,
                            GenerationConfigMapper generationConfigMapper,
                            ObjectMapper objectMapper) {
        this.storylineMapper = storylineMapper;
        this.storylineVersionMapper = storylineVersionMapper;
        this.generationConfigMapper = generationConfigMapper;
        this.objectMapper = objectMapper;
    }

    // ==================== Create ====================

    @Transactional
    public Long createStoryline(StorylineCreateDTO dto) {
        validateTemplateFields(dto.getGenre(), dto.getCharacterSettings(),
                dto.getWorldview(), dto.getPlotOutline());

        Storyline storyline = new Storyline();
        storyline.setTitle(dto.getTitle());
        storyline.setGenre(dto.getGenre());
        storyline.setContentType(dto.getContentType());
        storyline.setCharacterSettings(dto.getCharacterSettings());
        storyline.setWorldview(dto.getWorldview());
        storyline.setPlotOutline(dto.getPlotOutline());
        storyline.setStatus(StorylineStatus.DISABLED);
        storyline.setGeneratedCount(0);
        storyline.setCreatedAt(LocalDateTime.now());
        storyline.setUpdatedAt(LocalDateTime.now());

        storylineMapper.insert(storyline);
        return storyline.getId();
    }

    // ==================== Update with Version Snapshot ====================

    @Transactional
    public void updateStoryline(Long storylineId, StorylineUpdateDTO dto) {
        validateTemplateFields(dto.getGenre(), dto.getCharacterSettings(),
                dto.getWorldview(), dto.getPlotOutline());

        Storyline storyline = getStorylineById(storylineId);


        // Save current state as a version snapshot before updating
        saveVersionSnapshot(storyline);

        // Apply updates
        storyline.setTitle(dto.getTitle());
        storyline.setGenre(dto.getGenre());
        storyline.setContentType(dto.getContentType());
        storyline.setCharacterSettings(dto.getCharacterSettings());
        storyline.setWorldview(dto.getWorldview());
        storyline.setPlotOutline(dto.getPlotOutline());
        storyline.setUpdatedAt(LocalDateTime.now());

        storylineMapper.updateById(storyline);
    }

    // ==================== Status Toggle ====================

    @Transactional
    public void toggleStorylineStatus(Long storylineId, String status) {
        Storyline storyline = getStorylineById(storylineId);

        StorylineStatus targetStatus;
        if ("ENABLED".equalsIgnoreCase(status)) {
            targetStatus = StorylineStatus.ENABLED;
        } else if ("DISABLED".equalsIgnoreCase(status)) {
            targetStatus = StorylineStatus.DISABLED;
        } else {
            throw new BusinessException(400, "无效的状态值: " + status + "，仅支持 ENABLED 或 DISABLED");
        }

        storyline.setStatus(targetStatus);
        storyline.setUpdatedAt(LocalDateTime.now());
        storylineMapper.updateById(storyline);
    }

    // ==================== Query ====================

    public StorylineDetailVO getStorylineDetail(Long storylineId) {
        Storyline storyline = getStorylineById(storylineId);
        GenerationConfig config = getGenerationConfig(storylineId);
        return StorylineDetailVO.fromStoryline(storyline, config);
    }

    public PageVO<StorylineVO> getStorylines(StorylineQueryDTO query) {
        int page = query.getPage() != null && query.getPage() >= 1 ? query.getPage() : 1;
        int size = query.getSize() != null && query.getSize() >= 1 ? query.getSize() : 10;

        LambdaQueryWrapper<Storyline> wrapper = new LambdaQueryWrapper<>();
        if (query.getContentType() != null) {
            wrapper.eq(Storyline::getContentType, query.getContentType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Storyline::getStatus, query.getStatus());
        }
        if (query.getGenre() != null && !query.getGenre().isBlank()) {
            wrapper.like(Storyline::getGenre, query.getGenre());
        }
        wrapper.orderByDesc(Storyline::getCreatedAt);

        IPage<Storyline> pageResult = storylineMapper.selectPage(new Page<>(page, size), wrapper);
        List<StorylineVO> records = pageResult.getRecords().stream()
                .map(StorylineVO::fromStoryline)
                .toList();

        return new PageVO<>(records, pageResult.getTotal(), page, size);
    }

    // ==================== Generation Config ====================

    @Transactional
    public void configureGeneration(Long storylineId, GenerationConfigDTO dto) {
        // Verify storyline exists
        getStorylineById(storylineId);

        LambdaQueryWrapper<GenerationConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GenerationConfig::getStorylineId, storylineId);
        GenerationConfig existing = generationConfigMapper.selectOne(wrapper);

        if (existing != null) {
            // Update existing config
            existing.setTextProvider(dto.getTextProvider());
            existing.setTextModel(dto.getTextModel());
            existing.setImageProvider(dto.getImageProvider());
            existing.setImageModel(dto.getImageModel());
            existing.setTemperature(dto.getTemperature() != null ? dto.getTemperature() : 0.7);
            existing.setMaxTokens(dto.getMaxTokens() != null ? dto.getMaxTokens() : 2000);
            existing.setImageStyle(dto.getImageStyle());
            existing.setImageSize(dto.getImageSize());
            existing.setUpdatedAt(LocalDateTime.now());
            generationConfigMapper.updateById(existing);
        } else {
            // Create new config
            GenerationConfig config = new GenerationConfig();
            config.setStorylineId(storylineId);
            config.setTextProvider(dto.getTextProvider());
            config.setTextModel(dto.getTextModel());
            config.setImageProvider(dto.getImageProvider());
            config.setImageModel(dto.getImageModel());
            config.setTemperature(dto.getTemperature() != null ? dto.getTemperature() : 0.7);
            config.setMaxTokens(dto.getMaxTokens() != null ? dto.getMaxTokens() : 2000);
            config.setImageStyle(dto.getImageStyle());
            config.setImageSize(dto.getImageSize());
            config.setCreatedAt(LocalDateTime.now());
            config.setUpdatedAt(LocalDateTime.now());
            generationConfigMapper.insert(config);
        }
    }

    public GenerationConfig getGenerationConfig(Long storylineId) {
        LambdaQueryWrapper<GenerationConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GenerationConfig::getStorylineId, storylineId);
        return generationConfigMapper.selectOne(wrapper);
    }

    // ==================== Internal Helpers ====================

    public List<Storyline> getEnabledStorylines() {
        LambdaQueryWrapper<Storyline> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Storyline::getStatus, StorylineStatus.ENABLED);
        return storylineMapper.selectList(wrapper);
    }

    public Storyline getStorylineById(Long storylineId) {
        Storyline storyline = storylineMapper.selectById(storylineId);
        if (storyline == null) {
            throw new EntityNotFoundException("故事线", storylineId);
        }
        return storyline;
    }

    private void validateTemplateFields(String genre, String characterSettings,
                                        String worldview, String plotOutline) {
        if (genre == null || genre.isBlank()) {
            throw new BusinessException(400, "题材类型不能为空");
        }
        if (characterSettings == null || characterSettings.isBlank()) {
            throw new BusinessException(400, "角色设定不能为空");
        }
        if (worldview == null || worldview.isBlank()) {
            throw new BusinessException(400, "世界观描述不能为空");
        }
        if (plotOutline == null || plotOutline.isBlank()) {
            throw new BusinessException(400, "剧情大纲不能为空");
        }
    }

    private void saveVersionSnapshot(Storyline storyline) {
        // Determine next version number
        LambdaQueryWrapper<StorylineVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StorylineVersion::getStorylineId, storyline.getId());
        wrapper.orderByDesc(StorylineVersion::getVersionNumber);
        wrapper.last("LIMIT 1");

        StorylineVersion latestVersion = storylineVersionMapper.selectOne(wrapper);
        int nextVersionNumber = (latestVersion != null) ? latestVersion.getVersionNumber() + 1 : 1;

        // Serialize current storyline state to JSON
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(storyline);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "故事线快照序列化失败");
        }

        StorylineVersion version = new StorylineVersion();
        version.setStorylineId(storyline.getId());
        version.setVersionNumber(nextVersionNumber);
        version.setSnapshotJson(snapshotJson);
        version.setCreatedAt(LocalDateTime.now());

        storylineVersionMapper.insert(version);
    }
}
