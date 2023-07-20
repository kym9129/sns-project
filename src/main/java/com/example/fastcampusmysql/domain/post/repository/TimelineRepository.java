package com.example.fastcampusmysql.domain.post.repository;

import com.example.fastcampusmysql.domain.post.entity.Timeline;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TimelineRepository {
    private final static String TABLE = "Timeline";
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private static final RowMapper<Timeline> ROW_MAPPER =
            (ResultSet resultSet, int rowNum)
                -> Timeline.builder()
                    .id(resultSet.getLong("id"))
                    .memberId(resultSet.getLong("memberId"))
                    .postId(resultSet.getLong("postId"))
                    .createdAt(resultSet.getObject("createdAt", LocalDateTime.class))
                    .build();

    public List<Timeline> findAllByMemberIdAndOrderByIdDesc(long memberId, int size) {
        String sql = String.format("""
                SELECT *
                FROM %s
                WHERE memberId = :memberId
                ORDER BY id DESC
                LIMIT :size
                """, TABLE);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("size", size);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public List<Timeline> findAllByLessThenIdAndMemberIdOrderByIdDesc(long id, long memberId, int size) {
        String sql = String.format("""
                SELECT *
                FROM %s
                WHERE memberId = :memberId
                  AND id < :id 
                ORDER BY id DESC
                LIMIT :size
                """, TABLE);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memberId", memberId)
                .addValue("id", id)
                .addValue("size", size);
        return namedParameterJdbcTemplate.query(sql, params, ROW_MAPPER);
    }

    public Timeline save(Timeline timeline) {
        if(timeline.getId() == null){
            return insert(timeline);
        }
        throw new UnsupportedOperationException("Timeline은 갱신을 지원하지 않습니다");
    }

    private Timeline insert(Timeline timeline){
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(namedParameterJdbcTemplate.getJdbcTemplate())
                .withTableName(TABLE)
                .usingGeneratedKeyColumns("id");
        SqlParameterSource params = new BeanPropertySqlParameterSource(timeline);
        Long id = simpleJdbcInsert.executeAndReturnKey(params).longValue();
        return Timeline.builder()
                .id(id)
                .memberId(timeline.getMemberId())
                .postId(timeline.getPostId())
                .createdAt(timeline.getCreatedAt())
                .build();
    }

    public void bulkInsert(List<Timeline> timelines) {
        String sql = String.format("""
                    INSERT INTO `%s` (memberId, postId, createdAt)
                    VALUES (:memberId, :postId, :createdAt)
                    """, TABLE);

        SqlParameterSource[] params = timelines
                .stream()
                .map(BeanPropertySqlParameterSource::new)
                .toArray(SqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(sql, params);
    }
}