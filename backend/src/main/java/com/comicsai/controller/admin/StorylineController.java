package com.comicsai.controller.admin;

import com.comicsai.common.ApiResponse;
import com.comicsai.model.dto.GenerationConfigDTO;
import com.comicsai.model.dto.StorylineCreateDTO;
import com.comicsai.model.dto.StorylineQueryDTO;
import com.comicsai.model.dto.StorylineUpdateDTO;
import com.comicsai.model.entity.GenerationConfig;
import com.comicsai.model.entity.Storyline;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.enums.StorylineStatus;
import com.comicsai.model.vo.GenerationConfigVO;
import com.comicsai.model.vo.PageVO;
import com.comicsai.model.vo.StorylineDetailVO;
import com.comicsai.model.vo.StorylineVO;
import com.comicsai.service.ContentGeneratorService;
import com.comicsai.service.StorylineService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/storylines")
public class StorylineController {

    private final StorylineService storylineService;
    private final ContentGeneratorService contentGeneratorService;

    public StorylineController(StorylineService storylineService,
                               ContentGeneratorService contentGeneratorService) {
        this.storylineService = storylineService;
        this.contentGeneratorService = contentGeneratorService;
    }

    @GetMapping
    public ApiResponse<PageVO<StorylineVO>> listStorylines(
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(required = false) StorylineStatus status,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        StorylineQueryDTO query = new StorylineQueryDTO();
        query.setContentType(contentType);
        query.setStatus(status);
        query.setGenre(genre);
        query.setPage(page);
        query.setSize(size);
        PageVO<StorylineVO> result = storylineService.getStorylines(query);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> createStoryline(@Valid @RequestBody StorylineCreateDTO dto) {
        Long id = storylineService.createStoryline(dto);
        return ApiResponse.success(Map.of("id", id));
    }

    @GetMapping("/{id}")
    public ApiResponse<StorylineDetailVO> getStorylineDetail(@PathVariable Long id) {
        StorylineDetailVO detail = storylineService.getStorylineDetail(id);
        return ApiResponse.success(detail);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateStoryline(@PathVariable Long id,
                                              @Valid @RequestBody StorylineUpdateDTO dto) {
        storylineService.updateStoryline(id, dto);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> toggleStorylineStatus(@PathVariable Long id,
                                                    @RequestBody Map<String, String> body) {
        String status = body.get("status");
        storylineService.toggleStorylineStatus(id, status);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/generation-config")
    public ApiResponse<Void> configureGeneration(@PathVariable Long id,
                                                  @Valid @RequestBody GenerationConfigDTO dto) {
        storylineService.configureGeneration(id, dto);
        return ApiResponse.success();
    }

    @GetMapping("/{id}/generation-config")
    public ApiResponse<GenerationConfigVO> getGenerationConfig(@PathVariable Long id) {
        GenerationConfig config = storylineService.getGenerationConfig(id);
        return ApiResponse.success(GenerationConfigVO.fromEntity(config));
    }

    @PostMapping("/{id}/generate")
    public ApiResponse<Void> triggerGeneration(@PathVariable Long id) {
        Storyline storyline = storylineService.getStorylineById(id);
        contentGeneratorService.generateContentAsync(storyline);
        return ApiResponse.success("生成任务已提交，请稍后刷新查看结果", null);
    }
}
