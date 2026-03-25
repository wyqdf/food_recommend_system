package com.foodrecommend.letmecook.service.impl;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.dto.admin.*;
import com.foodrecommend.letmecook.entity.*;
import com.foodrecommend.letmecook.mapper.AdminMapper;
import com.foodrecommend.letmecook.mapper.AdminUserMapper;
import com.foodrecommend.letmecook.mapper.StatisticsMapper;
import com.foodrecommend.letmecook.mapper.StatisticsSummaryMapper;
import com.foodrecommend.letmecook.service.AdminService;
import com.foodrecommend.letmecook.service.TokenBlacklistService;
import com.foodrecommend.letmecook.util.JwtUtil;
import com.foodrecommend.letmecook.util.MapValueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final AdminUserMapper adminUserMapper;
    private final StatisticsMapper statisticsMapper;
    private final StatisticsSummaryMapper statisticsSummaryMapper;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public AdminLoginResponse login(AdminLoginRequest request, String ip) {
        Admin admin = adminMapper.findByUsername(request.getUsername());
        if (admin == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        adminMapper.updateLoginInfo(admin.getId(), new Date(), ip);

        String token = jwtUtil.generateAdminToken(admin.getId(), admin.getRole());

        AdminLoginResponse response = new AdminLoginResponse();
        response.setToken(token);

        AdminInfo adminInfo = new AdminInfo();
        adminInfo.setId(admin.getId());
        adminInfo.setUsername(admin.getUsername());
        adminInfo.setEmail(admin.getEmail());
        adminInfo.setRole(admin.getRole());
        adminInfo.setStatus(admin.getStatus());
        response.setAdmin(adminInfo);

        return response;
    }

    @Override
    public AdminProfileDTO getProfile(Integer adminId) {
        Admin admin = adminMapper.findById(adminId);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }

        AdminProfileDTO dto = new AdminProfileDTO();
        dto.setId(admin.getId());
        dto.setUsername(admin.getUsername());
        dto.setEmail(admin.getEmail());
        dto.setRole(admin.getRole());
        dto.setStatus(admin.getStatus());
        dto.setLastLoginTime(admin.getLastLoginTime() != null ? admin.getLastLoginTime().toString() : null);
        return dto;
    }

    @Override
    public void updatePassword(Integer adminId, ChangePasswordRequest request) {
        Admin admin = adminMapper.findById(adminId);
        if (admin == null) {
            throw new RuntimeException("管理员不存在");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), admin.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new RuntimeException("新密码长度至少6位");
        }

        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        adminMapper.updatePassword(adminId, encodedPassword);
    }

    @Override
    public PageResult<UserDTO> getUsers(int page, int pageSize, String keyword, Integer status) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 200);
        int offset = (safePage - 1) * safePageSize;
        long total = adminUserMapper.countUsers(keyword, status);
        if (total <= 0) {
            return new PageResult<>(List.of(), 0, safePage, safePageSize);
        }

        List<Integer> userIds = adminUserMapper.findUserIds(keyword, status, offset, safePageSize);
        if (userIds == null || userIds.isEmpty()) {
            return new PageResult<>(List.of(), total, safePage, safePageSize);
        }

        List<UserDTO> pageUsers = adminUserMapper.findUsersByIds(userIds);
        Map<Integer, UserDTO> userMap = new HashMap<>();
        for (UserDTO user : pageUsers) {
            if (user != null && user.getId() != null) {
                userMap.put(user.getId(), user);
            }
        }

        Map<Integer, Integer> favoritesCountMap = toCountMap(adminUserMapper.countFavoritesByUserIds(userIds));
        Map<Integer, Integer> commentsCountMap = toCountMap(adminUserMapper.countCommentsByUserIds(userIds));

        List<UserDTO> list = new ArrayList<>(userIds.size());
        for (Integer userId : userIds) {
            UserDTO user = userMap.get(userId);
            if (user == null) {
                continue;
            }
            user.setFavoritesCount(favoritesCountMap.getOrDefault(userId, 0));
            user.setCommentsCount(commentsCountMap.getOrDefault(userId, 0));
            list.add(user);
        }
        return new PageResult<>(list, total, safePage, safePageSize);
    }

    @Override
    public UserDTO getUserById(Integer id) {
        UserDTO user = adminUserMapper.findUserById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return user;
    }

    @Override
    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new RuntimeException("用户名不能为空");
        }

        User existUser = adminUserMapper.findByUsername(request.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            User existEmail = adminUserMapper.findByEmail(request.getEmail());
            if (existEmail != null) {
                throw new RuntimeException("邮箱已存在");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        String password = request.getPassword();
        if (password == null || password.length() < 6) {
            throw new RuntimeException("密码长度至少6位");
        }
        user.setPassword(passwordEncoder.encode(password));

        adminUserMapper.insertUser(user);

        return adminUserMapper.findUserById(user.getId());
    }

    @Override
    @Transactional
    public void updateUser(Integer id, UpdateUserRequestAdmin request) {
        UserDTO existUser = adminUserMapper.findUserById(id);
        if (existUser == null) {
            throw new RuntimeException("用户不存在");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            User existEmail = adminUserMapper.findByEmail(request.getEmail());
            if (existEmail != null && !existEmail.getId().equals(id)) {
                throw new RuntimeException("邮箱已存在");
            }
        }

        User user = new User();
        user.setId(id);
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());

        adminUserMapper.updateUser(user);
    }

    @Override
    @Transactional
    public void deleteUser(Integer id) {
        UserDTO existUser = adminUserMapper.findUserById(id);
        if (existUser == null) {
            throw new RuntimeException("用户不存在");
        }
        adminUserMapper.deleteUser(id);
    }

    @Override
    @Transactional
    public void batchDeleteUsers(Integer[] ids) {
        if (ids == null || ids.length == 0) {
            throw new RuntimeException("请选择要删除的用户");
        }
        adminUserMapper.batchDeleteUsers(ids);
    }

    @Override
    @Transactional
    public void updateUserStatus(Integer id, Integer status) {
        UserDTO existUser = adminUserMapper.findUserById(id);
        if (existUser == null) {
            throw new RuntimeException("用户不存在");
        }
        adminUserMapper.updateStatus(id, status);

        if (status == 0) {
            log.info("用户 {} 被禁用，其 token 将被加入黑名单", id);
        }
    }

    @Override
    @Transactional
    public void resetUserPassword(Integer id, String password) {
        UserDTO existUser = adminUserMapper.findUserById(id);
        if (existUser == null) {
            throw new RuntimeException("用户不存在");
        }

        if (password == null || password.length() < 6) {
            throw new RuntimeException("密码长度至少6位");
        }

        String encodedPassword = passwordEncoder.encode(password);
        adminUserMapper.updatePassword(id, encodedPassword);
    }

    @Override
    public StatisticsOverviewDTO getOverview() {
        // 优先从汇总表读取数据，提升查询性能
        StatisticsOverview summary = statisticsSummaryMapper.getLatestOverview();

        StatisticsOverviewDTO dto = new StatisticsOverviewDTO();

        if (summary != null && summary.getStatDate() != null && summary.getStatDate().equals(LocalDate.now())) {
            // 如果汇总表有今日数据，直接使用
            dto.setTotalUsers(summary.getTotalUsers());
            dto.setTotalRecipes(summary.getTotalRecipes());
            dto.setTotalCategories(summary.getTotalCategories());
            dto.setTotalComments(summary.getTotalComments());
            dto.setTodayViews(summary.getTodayViews());
            dto.setTodayNewUsers(summary.getTodayNewUsers());
            dto.setTodayNewRecipes(summary.getTodayNewRecipes());
        } else {
            // 如果汇总表没有今日数据，直接查询数据库（首次使用或数据缺失时）
            dto.setTotalUsers(statisticsMapper.countTotalUsers());
            dto.setTotalRecipes(statisticsMapper.countTotalRecipes());
            dto.setTotalCategories(statisticsMapper.countTotalCategories());
            dto.setTotalComments(statisticsMapper.countTotalComments());
            dto.setTodayViews(statisticsMapper.countTodayViews());
            dto.setTodayNewUsers(statisticsMapper.countTodayNewUsers());
            dto.setTodayNewRecipes(statisticsMapper.countTodayNewRecipes());
        }

        return dto;
    }

    @Override
    public StatisticsDTO getUserStatistics(String type, String startTime, String endTime) {
        StatisticsDTO dto = new StatisticsDTO();

        // 优先从汇总表读取近 7 天数据
        List<UserTrendDaily> trendData = statisticsSummaryMapper.getLatestUserTrend();

        if (hasData(trendData)) {
            dto.setTrend(buildTrendItems(trendData, UserTrendDaily::getStatDate, UserTrendDaily::getNewUsersCount));
        } else {
            // 如果汇总表没有数据，使用原始查询
            List<Map<String, Object>> rawData = statisticsMapper.getUserTrend();
            dto.setTrend(convertToTrendList(rawData));
        }

        return dto;
    }

    @Override
    public StatisticsDTO getRecipeStatistics(String type, String startTime, String endTime) {
        StatisticsDTO dto = new StatisticsDTO();

        // 食谱趋势
        List<RecipeTrendDaily> trendData = statisticsSummaryMapper.getLatestRecipeTrend();
        if (hasData(trendData)) {
            dto.setTrend(buildTrendItems(trendData, RecipeTrendDaily::getStatDate, RecipeTrendDaily::getNewRecipesCount));
        } else {
            List<Map<String, Object>> rawData = statisticsMapper.getRecipeTrend();
            dto.setTrend(convertToTrendList(rawData));
        }

        // 分类分布
        List<CategoryDistributionSummary> categoryData = statisticsSummaryMapper.getLatestCategoryDistribution();
        if (hasData(categoryData)) {
            dto.setCategoryDistribution(buildDistributionItems(
                categoryData,
                CategoryDistributionSummary::getCategoryName,
                CategoryDistributionSummary::getRecipeCount
            ));
        } else {
            List<Map<String, Object>> rawData = statisticsMapper.getCategoryDistribution();
            dto.setCategoryDistribution(convertToDistributionList(rawData));
        }

        // 难度分布
        List<DifficultyDistributionSummary> difficultyData = statisticsSummaryMapper.getLatestDifficultyDistribution();
        if (hasData(difficultyData)) {
            dto.setDifficultyDistribution(buildDistributionItems(
                difficultyData,
                DifficultyDistributionSummary::getDifficultyName,
                DifficultyDistributionSummary::getRecipeCount
            ));
        } else {
            List<Map<String, Object>> rawData = statisticsMapper.getDifficultyDistribution();
            dto.setDifficultyDistribution(convertToDistributionList(rawData));
        }

        // 热门食谱 Top 10
        List<TopRecipesHourly> topRecipesData = statisticsSummaryMapper.getLatestTopRecipes(10);
        if (hasData(topRecipesData)) {
            dto.setTopRecipes(buildTopRecipeItems(
                topRecipesData,
                TopRecipesHourly::getRecipeId,
                TopRecipesHourly::getRecipeTitle,
                TopRecipesHourly::getViewCount,
                TopRecipesHourly::getLikeCount
            ));
        } else {
            List<Map<String, Object>> rawData = statisticsMapper.getTopRecipes(10);
            dto.setTopRecipes(convertToTopRecipeList(rawData));
        }

        return dto;
    }

    @Override
    public StatisticsDTO getCommentStatistics(String type, String startTime, String endTime) {
        StatisticsDTO dto = new StatisticsDTO();

        // 评论趋势
        List<CommentTrendDaily> trendData = statisticsSummaryMapper.getLatestCommentTrend();
        if (hasData(trendData)) {
            dto.setCommentsTrend(buildTrendItems(trendData, CommentTrendDaily::getStatDate, CommentTrendDaily::getNewCommentsCount));
        } else {
            List<Map<String, Object>> rawData = statisticsMapper.getCommentTrend();
            dto.setCommentsTrend(convertToTrendList(rawData));
        }

        // 热门评论食谱 Top 10
        List<TopCommentedRecipesHourly> topCommentedData = statisticsSummaryMapper.getLatestTopCommentedRecipes(10);
        if (hasData(topCommentedData)) {
            dto.setTopCommentedRecipes(buildTopCommentedItems(
                topCommentedData,
                TopCommentedRecipesHourly::getRecipeId,
                TopCommentedRecipesHourly::getRecipeTitle,
                TopCommentedRecipesHourly::getCommentCount
            ));
        } else {
            List<Map<String, Object>> rawData = statisticsMapper.getTopCommentedRecipes(10);
            dto.setTopCommentedRecipes(convertToTopCommentedList(rawData));
        }

        return dto;
    }

    private boolean hasData(List<?> data) {
        return data != null && !data.isEmpty();
    }

    private <T> List<StatisticsDTO.TrendItem> buildTrendItems(
            List<T> data,
            Function<T, LocalDate> dateExtractor,
            Function<T, Integer> countExtractor) {
        List<StatisticsDTO.TrendItem> result = new ArrayList<>(data.size());
        for (T entry : data) {
            StatisticsDTO.TrendItem item = new StatisticsDTO.TrendItem();
            LocalDate date = dateExtractor.apply(entry);
            item.setDate(date != null ? date.toString() : null);
            item.setCount(safeInt(countExtractor.apply(entry)));
            result.add(item);
        }
        return result;
    }

    private <T> List<StatisticsDTO.DistributionItem> buildDistributionItems(
            List<T> data,
            Function<T, String> nameExtractor,
            Function<T, Integer> valueExtractor) {
        List<StatisticsDTO.DistributionItem> result = new ArrayList<>(data.size());
        for (T entry : data) {
            StatisticsDTO.DistributionItem item = new StatisticsDTO.DistributionItem();
            item.setName(nameExtractor.apply(entry));
            item.setValue(safeInt(valueExtractor.apply(entry)));
            result.add(item);
        }
        return result;
    }

    private <T> List<StatisticsDTO.TopRecipeItem> buildTopRecipeItems(
            List<T> data,
            Function<T, Integer> idExtractor,
            Function<T, String> titleExtractor,
            Function<T, Integer> viewCountExtractor,
            Function<T, Integer> likeCountExtractor) {
        List<StatisticsDTO.TopRecipeItem> result = new ArrayList<>(data.size());
        for (T entry : data) {
            StatisticsDTO.TopRecipeItem item = new StatisticsDTO.TopRecipeItem();
            item.setId(safeInt(idExtractor.apply(entry)));
            item.setTitle(titleExtractor.apply(entry));
            item.setViewCount(safeInt(viewCountExtractor.apply(entry)));
            item.setLikeCount(safeInt(likeCountExtractor.apply(entry)));
            result.add(item);
        }
        return result;
    }

    private <T> List<StatisticsDTO.TopCommentedItem> buildTopCommentedItems(
            List<T> data,
            Function<T, Integer> idExtractor,
            Function<T, String> titleExtractor,
            Function<T, Integer> commentCountExtractor) {
        List<StatisticsDTO.TopCommentedItem> result = new ArrayList<>(data.size());
        for (T entry : data) {
            StatisticsDTO.TopCommentedItem item = new StatisticsDTO.TopCommentedItem();
            item.setId(safeInt(idExtractor.apply(entry)));
            item.setTitle(titleExtractor.apply(entry));
            item.setCommentCount(safeInt(commentCountExtractor.apply(entry)));
            result.add(item);
        }
        return result;
    }

    private int safeInt(Integer value) {
        return value != null ? value : 0;
    }

    private Map<Integer, Integer> toCountMap(List<Map<String, Object>> rows) {
        Map<Integer, Integer> countMap = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            return countMap;
        }
        for (Map<String, Object> row : rows) {
            Integer userId = MapValueUtils.getInt(row, "userId", "user_id");
            if (userId == null) {
                continue;
            }
            countMap.put(userId, MapValueUtils.getIntOrDefault(row, 0, "total", "count"));
        }
        return countMap;
    }

    private List<StatisticsDTO.TrendItem> convertToTrendList(List<Map<String, Object>> data) {
        List<StatisticsDTO.TrendItem> result = new ArrayList<>();
        if (data == null || data.isEmpty()) {
            return result;
        }
        for (Map<String, Object> item : data) {
            StatisticsDTO.TrendItem trendItem = new StatisticsDTO.TrendItem();
            LocalDate date = MapValueUtils.getLocalDate(item, "date");
            trendItem.setDate(date != null ? date.toString() : MapValueUtils.getString(item, "date"));
            trendItem.setCount(MapValueUtils.getIntOrDefault(item, 0, "count"));
            result.add(trendItem);
        }
        return result;
    }

    private List<StatisticsDTO.DistributionItem> convertToDistributionList(List<Map<String, Object>> data) {
        List<StatisticsDTO.DistributionItem> result = new ArrayList<>();
        if (data == null || data.isEmpty()) {
            return result;
        }
        for (Map<String, Object> item : data) {
            StatisticsDTO.DistributionItem distItem = new StatisticsDTO.DistributionItem();
            distItem.setName(MapValueUtils.getString(item, "name", "category_name", "difficulty_name"));
            distItem.setValue(MapValueUtils.getIntOrDefault(item, 0, "value", "recipe_count"));
            result.add(distItem);
        }
        return result;
    }

    private List<StatisticsDTO.TopRecipeItem> convertToTopRecipeList(List<Map<String, Object>> data) {
        List<StatisticsDTO.TopRecipeItem> result = new ArrayList<>();
        if (data == null || data.isEmpty()) {
            return result;
        }
        for (Map<String, Object> item : data) {
            StatisticsDTO.TopRecipeItem topItem = new StatisticsDTO.TopRecipeItem();
            topItem.setId(MapValueUtils.getIntOrDefault(item, 0, "id", "recipe_id"));
            topItem.setTitle(MapValueUtils.getString(item, "title", "recipe_title"));
            topItem.setViewCount(MapValueUtils.getIntOrDefault(item, 0, "view_count", "rating_count"));
            topItem.setLikeCount(MapValueUtils.getIntOrDefault(item, 0, "like_count"));
            result.add(topItem);
        }
        return result;
    }

    private List<StatisticsDTO.TopCommentedItem> convertToTopCommentedList(List<Map<String, Object>> data) {
        List<StatisticsDTO.TopCommentedItem> result = new ArrayList<>();
        if (data == null || data.isEmpty()) {
            return result;
        }
        for (Map<String, Object> item : data) {
            StatisticsDTO.TopCommentedItem topItem = new StatisticsDTO.TopCommentedItem();
            topItem.setId(MapValueUtils.getIntOrDefault(item, 0, "id", "recipe_id"));
            topItem.setTitle(MapValueUtils.getString(item, "title", "recipe_title"));
            topItem.setCommentCount(MapValueUtils.getIntOrDefault(item, 0, "comment_count", "commentCount"));
            result.add(topItem);
        }
        return result;
    }
}
