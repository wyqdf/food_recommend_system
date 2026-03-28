package com.foodrecommend.letmecook.service.impl;

import com.foodrecommend.letmecook.common.PageResult;
import com.foodrecommend.letmecook.common.exception.BadRequestException;
import com.foodrecommend.letmecook.common.exception.NotFoundException;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
            throw new BadRequestException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BadRequestException("用户名或密码错误");
        }

        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BadRequestException("账号已被禁用");
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
        Admin admin = requireAdmin(adminId);

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
        Admin admin = requireAdmin(adminId);

        if (!passwordEncoder.matches(request.getOldPassword(), admin.getPassword())) {
            throw new BadRequestException("旧密码错误");
        }

        ensurePasswordLength(request.getNewPassword(), "新密码长度至少6位");

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
        return requireManagedUser(id);
    }

    @Override
    @Transactional
    public UserDTO createUser(CreateUserRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BadRequestException("用户名不能为空");
        }

        User existUser = adminUserMapper.findByUsername(request.getUsername());
        if (existUser != null) {
            throw new BadRequestException("用户名已存在");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            User existEmail = adminUserMapper.findByEmail(request.getEmail());
            if (existEmail != null) {
                throw new BadRequestException("邮箱已存在");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus() != null ? request.getStatus() : 1);

        String password = request.getPassword();
        ensurePasswordLength(password, "密码长度至少6位");
        user.setPassword(passwordEncoder.encode(password));

        adminUserMapper.insertUser(user);

        return adminUserMapper.findUserById(user.getId());
    }

    @Override
    @Transactional
    public void updateUser(Integer id, UpdateUserRequestAdmin request) {
        requireManagedUser(id);

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            User existEmail = adminUserMapper.findByEmail(request.getEmail());
            if (existEmail != null && !existEmail.getId().equals(id)) {
                throw new BadRequestException("邮箱已存在");
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
        requireManagedUser(id);
        adminUserMapper.deleteUser(id);
    }

    @Override
    @Transactional
    public void batchDeleteUsers(Integer[] ids) {
        if (ids == null || ids.length == 0) {
            throw new BadRequestException("请选择要删除的用户");
        }
        adminUserMapper.batchDeleteUsers(ids);
    }

    @Override
    @Transactional
    public void updateUserStatus(Integer id, Integer status) {
        requireManagedUser(id);
        adminUserMapper.updateStatus(id, status);

        if (status == 0) {
            log.info("用户 {} 被禁用，其 token 将被加入黑名单", id);
        }
    }

    @Override
    @Transactional
    public void resetUserPassword(Integer id, String password) {
        requireManagedUser(id);
        ensurePasswordLength(password, "密码长度至少6位");

        String encodedPassword = passwordEncoder.encode(password);
        adminUserMapper.updatePassword(id, encodedPassword);
    }

    private Admin requireAdmin(Integer adminId) {
        Admin admin = adminMapper.findById(adminId);
        if (admin == null) {
            throw new NotFoundException("管理员不存在");
        }
        return admin;
    }

    private UserDTO requireManagedUser(Integer id) {
        UserDTO user = adminUserMapper.findUserById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return user;
    }

    private void ensurePasswordLength(String password, String message) {
        if (password == null || password.length() < 6) {
            throw new BadRequestException(message);
        }
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
        StatisticsDateRange range = resolveStatisticsDateRange(type, startTime, endTime);

        List<UserTrendDaily> trendData = filterByDateRange(
                statisticsSummaryMapper.getUserTrendByDateRange(range.startDate()),
                UserTrendDaily::getStatDate,
                range);
        if (hasData(trendData)) {
            dto.setTrend(buildTrendItems(
                    trendData,
                    UserTrendDaily::getStatDate,
                    UserTrendDaily::getNewUsersCount,
                    range));
        } else {
            List<Map<String, Object>> rawData = statisticsMapper.getUserTrendByDateRange(
                    range.startDateTime(),
                    range.endTimeExclusive());
            dto.setTrend(buildTrendItems(
                    convertToTrendPoints(rawData),
                    TrendPoint::date,
                    TrendPoint::count,
                    range));
        }

        return dto;
    }

    @Override
    public StatisticsDTO getRecipeStatistics(String type, String startTime, String endTime) {
        StatisticsDTO dto = new StatisticsDTO();
        StatisticsDateRange range = resolveStatisticsDateRange(type, startTime, endTime);

        List<RecipeTrendDaily> trendData = filterByDateRange(
                statisticsSummaryMapper.getRecipeTrendByDateRange(range.startDate()),
                RecipeTrendDaily::getStatDate,
                range);
        if (hasData(trendData)) {
            dto.setTrend(buildTrendItems(
                    trendData,
                    RecipeTrendDaily::getStatDate,
                    RecipeTrendDaily::getNewRecipesCount,
                    range));
        } else {
            List<Map<String, Object>> rawData = statisticsMapper.getRecipeTrendByDateRange(
                    range.startDateTime(),
                    range.endTimeExclusive());
            dto.setTrend(buildTrendItems(
                    convertToTrendPoints(rawData),
                    TrendPoint::date,
                    TrendPoint::count,
                    range));
        }

        List<CategoryDistributionSummary> categoryData = loadCategoryDistributionSnapshot(range);
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

        List<DifficultyDistributionSummary> difficultyData = loadDifficultyDistributionSnapshot(range);
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

        List<TopRecipesHourly> topRecipesData = loadTopRecipesSnapshot(range);
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
        StatisticsDateRange range = resolveStatisticsDateRange(type, startTime, endTime);

        List<CommentTrendDaily> trendData = filterByDateRange(
                statisticsSummaryMapper.getCommentTrendByDateRange(range.startDate()),
                CommentTrendDaily::getStatDate,
                range);
        if (hasData(trendData)) {
            dto.setCommentsTrend(buildTrendItems(
                    trendData,
                    CommentTrendDaily::getStatDate,
                    CommentTrendDaily::getNewCommentsCount,
                    range));
        } else {
            List<Map<String, Object>> rawData = statisticsMapper.getCommentTrendByDateRange(
                    range.startDateTime(),
                    range.endTimeExclusive());
            dto.setCommentsTrend(buildTrendItems(
                    convertToTrendPoints(rawData),
                    TrendPoint::date,
                    TrendPoint::count,
                    range));
        }

        List<TopCommentedRecipesHourly> topCommentedData = loadTopCommentedRecipesSnapshot(range);
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

    private StatisticsDateRange resolveStatisticsDateRange(String type, String startTime, String endTime) {
        StatisticsGranularity granularity = StatisticsGranularity.from(type);
        LocalDate explicitStart = parseStatisticsDate(startTime, "startTime");
        LocalDate explicitEnd = parseStatisticsDate(endTime, "endTime");

        LocalDate safeEnd = explicitEnd != null ? explicitEnd : LocalDate.now();
        LocalDate safeStart = explicitStart != null ? explicitStart : defaultStartDate(safeEnd, granularity);
        if (safeStart.isAfter(safeEnd)) {
            throw new BadRequestException("startTime 不能晚于 endTime");
        }

        return new StatisticsDateRange(
                granularity,
                safeStart,
                safeEnd,
                explicitStart != null || explicitEnd != null);
    }

    private LocalDate defaultStartDate(LocalDate endDate, StatisticsGranularity granularity) {
        return switch (granularity) {
            case DAILY -> endDate.minusDays(6);
            case WEEKLY -> endDate.minusWeeks(11);
            case MONTHLY -> endDate.minusMonths(11);
        };
    }

    private LocalDate parseStatisticsDate(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String value = rawValue.trim();
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return OffsetDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(value.replace(' ', 'T')).toLocalDate();
        } catch (DateTimeParseException ignored) {
        }

        throw new BadRequestException(fieldName + " 格式无效，请使用 ISO 8601 日期或日期时间");
    }

    private <T> List<T> filterByDateRange(List<T> data,
            Function<T, LocalDate> dateExtractor,
            StatisticsDateRange range) {
        if (data == null || data.isEmpty()) {
            return List.of();
        }

        List<T> filtered = new ArrayList<>(data.size());
        for (T entry : data) {
            LocalDate date = dateExtractor.apply(entry);
            if (date == null
                    || date.isBefore(range.startDate())
                    || date.isAfter(range.endDate())) {
                continue;
            }
            filtered.add(entry);
        }
        return filtered;
    }

    private <T> List<StatisticsDTO.TrendItem> buildTrendItems(
            List<T> data,
            Function<T, LocalDate> dateExtractor,
            Function<T, Integer> countExtractor,
            StatisticsDateRange range) {
        LinkedHashMap<LocalDate, Integer> bucketTotals = initializeTrendBuckets(range);
        if (data != null) {
            for (T entry : data) {
                LocalDate date = dateExtractor.apply(entry);
                if (date == null
                        || date.isBefore(range.startDate())
                        || date.isAfter(range.endDate())) {
                    continue;
                }
                LocalDate bucketStart = bucketStart(date, range.granularity());
                if (!bucketTotals.containsKey(bucketStart)) {
                    continue;
                }
                bucketTotals.put(bucketStart, bucketTotals.get(bucketStart) + safeInt(countExtractor.apply(entry)));
            }
        }

        List<StatisticsDTO.TrendItem> result = new ArrayList<>(bucketTotals.size());
        for (Map.Entry<LocalDate, Integer> entry : bucketTotals.entrySet()) {
            StatisticsDTO.TrendItem item = new StatisticsDTO.TrendItem();
            item.setDate(entry.getKey().toString());
            item.setCount(entry.getValue());
            result.add(item);
        }
        return result;
    }

    private LinkedHashMap<LocalDate, Integer> initializeTrendBuckets(StatisticsDateRange range) {
        LinkedHashMap<LocalDate, Integer> buckets = new LinkedHashMap<>();
        LocalDate cursor = bucketStart(range.startDate(), range.granularity());
        while (!cursor.isAfter(range.endDate())) {
            buckets.put(cursor, 0);
            cursor = nextBucket(cursor, range.granularity());
        }
        return buckets;
    }

    private LocalDate bucketStart(LocalDate date, StatisticsGranularity granularity) {
        return switch (granularity) {
            case DAILY -> date;
            case WEEKLY -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTHLY -> date.withDayOfMonth(1);
        };
    }

    private LocalDate nextBucket(LocalDate current, StatisticsGranularity granularity) {
        return switch (granularity) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
        };
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

    private List<CategoryDistributionSummary> loadCategoryDistributionSnapshot(StatisticsDateRange range) {
        if (range.hasExplicitTimeRange()) {
            List<CategoryDistributionSummary> snapshot = statisticsSummaryMapper.getCategoryDistributionByDate(range.endDate());
            if (hasData(snapshot)) {
                return snapshot;
            }
        }
        return statisticsSummaryMapper.getLatestCategoryDistribution();
    }

    private List<DifficultyDistributionSummary> loadDifficultyDistributionSnapshot(StatisticsDateRange range) {
        if (range.hasExplicitTimeRange()) {
            List<DifficultyDistributionSummary> snapshot = statisticsSummaryMapper.getDifficultyDistributionByDate(range.endDate());
            if (hasData(snapshot)) {
                return snapshot;
            }
        }
        return statisticsSummaryMapper.getLatestDifficultyDistribution();
    }

    private List<TopRecipesHourly> loadTopRecipesSnapshot(StatisticsDateRange range) {
        if (range.hasExplicitTimeRange()) {
            List<TopRecipesHourly> snapshot = statisticsSummaryMapper.getTopRecipesBefore(
                    range.endTimeExclusive().minusNanos(1),
                    10);
            if (hasData(snapshot)) {
                return snapshot;
            }
        }
        return statisticsSummaryMapper.getLatestTopRecipes(10);
    }

    private List<TopCommentedRecipesHourly> loadTopCommentedRecipesSnapshot(StatisticsDateRange range) {
        if (range.hasExplicitTimeRange()) {
            List<TopCommentedRecipesHourly> snapshot = statisticsSummaryMapper.getTopCommentedRecipesBefore(
                    range.endTimeExclusive().minusNanos(1),
                    10);
            if (hasData(snapshot)) {
                return snapshot;
            }
        }
        return statisticsSummaryMapper.getLatestTopCommentedRecipes(10);
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

    private List<TrendPoint> convertToTrendPoints(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return List.of();
        }

        List<TrendPoint> points = new ArrayList<>(data.size());
        for (Map<String, Object> item : data) {
            LocalDate date = MapValueUtils.getLocalDate(item, "date");
            if (date == null) {
                date = parseRawTrendDate(MapValueUtils.getString(item, "date"));
            }
            if (date == null) {
                continue;
            }
            points.add(new TrendPoint(date, MapValueUtils.getIntOrDefault(item, 0, "count")));
        }
        return points;
    }

    private LocalDate parseRawTrendDate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String value = rawValue.trim();
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ignored) {
        }
        if (value.length() >= 10) {
            try {
                return LocalDate.parse(value.substring(0, 10));
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
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
            topItem.setViewCount(MapValueUtils.getIntOrDefault(item, 0, "view_count"));
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

    private enum StatisticsGranularity {
        DAILY,
        WEEKLY,
        MONTHLY;

        private static StatisticsGranularity from(String rawType) {
            if (rawType == null || rawType.isBlank()) {
                return DAILY;
            }
            return switch (rawType.trim().toLowerCase()) {
                case "weekly", "week" -> WEEKLY;
                case "monthly", "month" -> MONTHLY;
                default -> DAILY;
            };
        }
    }

    private record StatisticsDateRange(
            StatisticsGranularity granularity,
            LocalDate startDate,
            LocalDate endDate,
            boolean hasExplicitTimeRange) {
        private LocalDateTime startDateTime() {
            return startDate.atStartOfDay();
        }

        private LocalDateTime endTimeExclusive() {
            return endDate.plusDays(1).atStartOfDay();
        }
    }

    private record TrendPoint(LocalDate date, int count) {
    }
}
