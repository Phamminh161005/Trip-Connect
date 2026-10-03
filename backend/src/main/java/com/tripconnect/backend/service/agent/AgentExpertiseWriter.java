package com.tripconnect.backend.service.agent;

import com.tripconnect.backend.entity.AgentServiceArea;
import com.tripconnect.backend.entity.AgentSpecialty;
import com.tripconnect.backend.entity.Location;
import com.tripconnect.backend.entity.TourCategory;
import com.tripconnect.backend.entity.User;
import com.tripconnect.backend.exception.ResourceNotFoundException;
import com.tripconnect.backend.repository.AgentServiceAreaRepository;
import com.tripconnect.backend.repository.AgentSpecialtyRepository;
import com.tripconnect.backend.repository.LocationRepository;
import com.tripconnect.backend.repository.TourCategoryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Ghi khu vực phụ trách + loại hình tour thế mạnh của Agent (dùng khi đăng ký và khi Agent tự sửa). */
@Component
@RequiredArgsConstructor
public class AgentExpertiseWriter {

    private final AgentServiceAreaRepository serviceAreaRepository;
    private final AgentSpecialtyRepository specialtyRepository;
    private final LocationRepository locationRepository;
    private final TourCategoryRepository tourCategoryRepository;
    private final EntityManager entityManager;

    /** Thay toàn bộ khu vực + loại hình cũ bằng danh sách mới. */
    public void replace(User agent, List<Long> locationIds, List<Long> categoryIds) {
        // Load trước để báo lỗi id không tồn tại TRƯỚC khi xóa dữ liệu cũ
        List<Location> locations = loadLocations(locationIds);
        List<TourCategory> categories = loadCategories(categoryIds);

        serviceAreaRepository.deleteByAgentId(agent.getId());
        specialtyRepository.deleteByAgentId(agent.getId());
        // Bulk delete bỏ qua persistence context -> flush + clear
        entityManager.flush();
        entityManager.clear();

        User agentRef = entityManager.getReference(User.class, agent.getId());
        saveAll(agentRef, locations, categories);
    }

    /** Ghi lần đầu (Agent mới đăng ký, chưa có dữ liệu cũ). */
    public void create(User agent, List<Long> locationIds, List<Long> categoryIds) {
        saveAll(agent, loadLocations(locationIds), loadCategories(categoryIds));
    }

    private void saveAll(User agent, List<Location> locations, List<TourCategory> categories) {
        serviceAreaRepository.saveAll(locations.stream().map(location -> {
            AgentServiceArea area = new AgentServiceArea();
            area.setAgent(agent);
            area.setLocation(location);
            return area;
        }).toList());

        specialtyRepository.saveAll(categories.stream().map(category -> {
            AgentSpecialty specialty = new AgentSpecialty();
            specialty.setAgent(agent);
            specialty.setCategory(category);
            return specialty;
        }).toList());
    }

    private List<Location> loadLocations(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<Location> locations = locationRepository.findAllById(distinctIds);
        if (locations.size() != distinctIds.size()) {
            throw new ResourceNotFoundException("Có địa điểm không tồn tại");
        }
        return locations;
    }

    private List<TourCategory> loadCategories(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> distinctIds = ids.stream().distinct().toList();
        List<TourCategory> categories = tourCategoryRepository.findAllById(distinctIds);
        if (categories.size() != distinctIds.size()) {
            throw new ResourceNotFoundException("Có loại hình tour không tồn tại");
        }
        return categories;
    }
}
