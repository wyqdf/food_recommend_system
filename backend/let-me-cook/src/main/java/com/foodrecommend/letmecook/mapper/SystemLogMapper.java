package com.foodrecommend.letmecook.mapper;

import com.foodrecommend.letmecook.entity.SystemLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SystemLogMapper {

    @Insert("INSERT INTO operation_logs (admin_id, action, target_type, target_id, detail, ip, user_agent) " +
            "VALUES (#{adminId}, #{operation}, #{module}, #{targetId}, #{content}, #{ip}, #{userAgent})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SystemLog log);

    @Select("<script>" +
            "SELECT " +
            "  ol.id AS id, " +
            "  ol.admin_id AS adminId, " +
            "  COALESCE(a.username, '未知管理员') AS adminName, " +
            "  ol.action AS operation, " +
            "  ol.target_type AS module, " +
            "  ol.target_id AS targetId, " +
            "  ol.detail AS content, " +
            "  ol.ip AS ip, " +
            "  ol.user_agent AS userAgent, " +
            "  ol.create_time AS createTime " +
            "FROM operation_logs ol " +
            "LEFT JOIN admins a ON a.id = ol.admin_id " +
            "<where>" +
            "  <if test='adminId != null'>" +
            "    AND ol.admin_id = #{adminId} " +
            "  </if>" +
            "  <if test='adminName != null and adminName != \"\"'>" +
            "    AND a.username LIKE CONCAT('%', #{adminName}, '%') " +
            "  </if>" +
            "  <if test='module != null and module != \"\"'>" +
            "    AND ol.target_type = #{module} " +
            "  </if>" +
            "  <if test='operation != null and operation != \"\"'>" +
            "    AND ol.action = #{operation} " +
            "  </if>" +
            "  <if test='startTime != null and startTime != \"\"'>" +
            "    AND ol.create_time &gt;= #{startTime} " +
            "  </if>" +
            "  <if test='endTime != null and endTime != \"\"'>" +
            "    AND ol.create_time &lt;= #{endTime} " +
            "  </if>" +
            "</where>" +
            "ORDER BY ol.create_time DESC, ol.id DESC " +
            "LIMIT #{offset}, #{limit}" +
            "</script>")
    List<SystemLog> findByCondition(
            @Param("adminId") Integer adminId,
            @Param("adminName") String adminName,
            @Param("module") String module,
            @Param("operation") String operation,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    @Select("<script>" +
            "SELECT COUNT(*) " +
            "FROM operation_logs ol " +
            "LEFT JOIN admins a ON a.id = ol.admin_id " +
            "<where>" +
            "  <if test='adminId != null'>" +
            "    AND ol.admin_id = #{adminId} " +
            "  </if>" +
            "  <if test='adminName != null and adminName != \"\"'>" +
            "    AND a.username LIKE CONCAT('%', #{adminName}, '%') " +
            "  </if>" +
            "  <if test='module != null and module != \"\"'>" +
            "    AND ol.target_type = #{module} " +
            "  </if>" +
            "  <if test='operation != null and operation != \"\"'>" +
            "    AND ol.action = #{operation} " +
            "  </if>" +
            "  <if test='startTime != null and startTime != \"\"'>" +
            "    AND ol.create_time &gt;= #{startTime} " +
            "  </if>" +
            "  <if test='endTime != null and endTime != \"\"'>" +
            "    AND ol.create_time &lt;= #{endTime} " +
            "  </if>" +
            "</where>" +
            "</script>")
    long countByCondition(
            @Param("adminId") Integer adminId,
            @Param("adminName") String adminName,
            @Param("module") String module,
            @Param("operation") String operation,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime
    );

    @Select("SELECT " +
            "  ol.id AS id, " +
            "  ol.admin_id AS adminId, " +
            "  COALESCE(a.username, '未知管理员') AS adminName, " +
            "  ol.action AS operation, " +
            "  ol.target_type AS module, " +
            "  ol.target_id AS targetId, " +
            "  ol.detail AS content, " +
            "  ol.ip AS ip, " +
            "  ol.user_agent AS userAgent, " +
            "  ol.create_time AS createTime " +
            "FROM operation_logs ol " +
            "LEFT JOIN admins a ON a.id = ol.admin_id " +
            "WHERE ol.id = #{id}")
    SystemLog findById(@Param("id") Integer id);

    @Delete("DELETE FROM operation_logs WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    @Delete("<script>" +
            "DELETE FROM operation_logs WHERE id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "  #{id}" +
            "</foreach>" +
            "</script>")
    int batchDeleteByIds(@Param("ids") List<Integer> ids);

    @Delete("DELETE FROM operation_logs WHERE create_time < #{beforeTime}")
    int deleteBeforeTime(@Param("beforeTime") String beforeTime);

    @Select("SELECT DISTINCT target_type FROM operation_logs WHERE target_type IS NOT NULL AND target_type != '' ORDER BY target_type ASC")
    List<String> findDistinctModules();

    @Select("SELECT DISTINCT action FROM operation_logs WHERE action IS NOT NULL AND action != '' ORDER BY action ASC")
    List<String> findDistinctOperations();
}
