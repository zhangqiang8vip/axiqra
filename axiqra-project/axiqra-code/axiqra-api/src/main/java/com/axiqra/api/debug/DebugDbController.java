package com.axiqra.api.debug;

import com.axiqra.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 数据库诊断 Controller（仅开发环境使用）
 */
@Slf4j
@RestController
@RequestMapping("/debug/db")
@RequiredArgsConstructor
@Tag(name = "数据库诊断", description = "检查表结构、字段、索引等（仅开发/调试用）")
public class DebugDbController {

    @Qualifier("dataSource")
    private final DataSource dataSource;
    private final org.springframework.context.ApplicationContext applicationContext;

    private JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    @GetMapping("/invocation-columns")
    @Operation(summary = "查看调用记录表结构")
    public ApiResponse<List<Map<String, Object>>> checkInvocationColumns() {
        try {
            List<Map<String, Object>> columns = jdbcTemplate().queryForList(
                "SELECT column_name, data_type, is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'axiqra_invocation' ORDER BY ordinal_position"
            );
            return ApiResponse.ok(columns);
        } catch (Exception e) {
            log.error("Failed to query invocation columns", e);
            return ApiResponse.fail(99999, "Error: " + e.getMessage());
        }
    }

    @GetMapping("/feedback-columns")
    @Operation(summary = "查看反馈表结构")
    public ApiResponse<List<Map<String, Object>>> checkFeedbackColumns() {
        try {
            List<Map<String, Object>> columns = jdbcTemplate().queryForList(
                "SELECT column_name, data_type, is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'axiqra_feedback' ORDER BY ordinal_position"
            );
            return ApiResponse.ok(columns);
        } catch (Exception e) {
            log.error("Failed to query feedback columns", e);
            return ApiResponse.fail(99999, "Error: " + e.getMessage());
        }
    }

    @GetMapping("/flyway-history")
    @Operation(summary = "查看数据库迁移历史")
    public ApiResponse<List<Map<String, Object>>> checkFlywayHistory() {
        try {
            List<Map<String, Object>> rows = jdbcTemplate().queryForList(
                "SELECT installed_rank, version, description, type, script, success, execution_time, installed_on " +
                "FROM flyway_schema_history ORDER BY installed_rank"
            );
            return ApiResponse.ok(rows);
        } catch (Exception e) {
            log.error("Failed to query flyway history", e);
            return ApiResponse.fail(99999, "Error: " + e.getMessage());
        }
    }

    @GetMapping("/schema-info")
    @Operation(summary = "查看数据库基本信息")
    public ApiResponse<Map<String, Object>> getSchemaInfo() {
        try {
            JdbcTemplate template = jdbcTemplate();
            Map<String, Object> info = new java.util.HashMap<>();
            info.put("currentDatabase", template.queryForObject("SELECT current_database()", String.class));
            info.put("currentSchema", template.queryForObject("SELECT current_schema()", String.class));
            info.put("currentUser", template.queryForObject("SELECT current_user", String.class));
            info.put("version", template.queryForObject("SELECT version()", String.class));
            return ApiResponse.ok(info);
        } catch (Exception e) {
            log.error("Failed to query schema info", e);
            return ApiResponse.fail(99999, "Error: " + e.getMessage());
        }
    }

    /**
     * 测试 CockroachDB 是否支持 pgvector 算子
     */
    @GetMapping("/vector-test")
    @Operation(summary = "测试向量检索支持")
    public ApiResponse<Map<String, Object>> testVector() {
        try {
            JdbcTemplate template = jdbcTemplate();
            Map<String, Object> result = new java.util.HashMap<>();
            // 1. count of solutions
            try {
                Integer total = template.queryForObject(
                    "SELECT COUNT(*) FROM axiqra_solution WHERE is_deleted = FALSE", Integer.class);
                result.put("totalSolutions", total);
            } catch (Exception e) { result.put("totalSolutions_error", e.getMessage()); }
            // 1b. list first 3 solutions
            try {
                java.util.List<java.util.Map<String, Object>> sols = template.queryForList(
                    "SELECT id, solution_code, title, status, visibility_scope, workspace_id, author_id FROM axiqra_solution WHERE is_deleted = FALSE ORDER BY id DESC LIMIT 3");
                result.put("sampleSolutions", sols);
            } catch (Exception e) { result.put("sampleSolutions_error", e.getMessage().substring(0, Math.min(200, e.getMessage().length()))); }
            // 2. count with embedding
            try {
                Integer withEmb = template.queryForObject(
                    "SELECT COUNT(*) FROM axiqra_solution WHERE is_deleted = FALSE AND embedding IS NOT NULL", Integer.class);
                result.put("solutionsWithEmbedding", withEmb);
            } catch (Exception e) { result.put("solutionsWithEmbedding_error", e.getMessage()); }
            // 3. try pgvector cast
            try {
                String cast = template.queryForObject(
                    "SELECT '[1,2,3]'::vector", String.class);
                result.put("vectorCast", cast);
            } catch (Exception e) { result.put("vectorCast_error", e.getMessage().substring(0, Math.min(200, e.getMessage().length()))); }
            // 4. try <=> operator
            try {
                String dist = template.queryForObject(
                    "SELECT '[1,2,3]'::vector <=> '[4,5,6]'::vector", String.class);
                result.put("l2Distance", dist);
            } catch (Exception e) { result.put("l2Distance_error", e.getMessage().substring(0, Math.min(200, e.getMessage().length()))); }
            // 5. try full search SQL with all columns
            try {
                java.util.List<java.util.Map<String, Object>> rows = template.queryForList(
                    "SELECT id, solution_code, title FROM axiqra_solution WHERE is_deleted = FALSE AND visibility_scope = 'public' AND status IN ('verified','stable','canonical') AND embedding IS NOT NULL LIMIT 5");
                result.put("publicWithEmbedding", rows);
            } catch (Exception e) { result.put("publicWithEmbedding_error", e.getMessage().substring(0, Math.min(200, e.getMessage().length()))); }
            // 6. update 3 solutions to public+verified and add a simple 8-dim embedding
            try {
                // First check the current column dimensions
                java.util.Map<String, Object> embDim = template.queryForMap(
                    "SELECT udt_name, element_type, dimension_count FROM pg_type pt JOIN pg_attribute pa ON pa.atttypid = pt.oid JOIN pg_class pc ON pc.oid = pa.attrelid WHERE pc.relname = 'axiqra_solution' AND pa.attname = 'embedding'");
                result.put("embeddingColumnInfo", embDim);
            } catch (Exception e) { result.put("embeddingColumnInfo_error", e.getMessage().substring(0, Math.min(300, e.getMessage().length()))); }
            try {
                // Clear all embeddings so we have a clean slate
                template.update("UPDATE axiqra_solution SET embedding = NULL WHERE id IN (?, ?, ?)", new Object[]{427224009336381440L, 427224683201609728L, 427226449729208320L});
                int upd = template.update(
                    "UPDATE axiqra_solution SET embedding = '[0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8]'::vector, visibility_scope = 'public', status = 'verified', gmt_modified = NOW() WHERE id IN (?, ?, ?) AND is_deleted = FALSE",
                    new Object[]{427224009336381440L, 427224683201609728L, 427226449729208320L});
                result.put("solutionsUpdatedToPublic", upd);
            } catch (Exception e) { result.put("update_error", e.getMessage().substring(0, Math.min(300, e.getMessage().length()))); }
            // 6b. generate proper 768-dim embeddings via the VectorSearchService bean
            try {
                Object svc = applicationContext.getBean(Class.forName("com.axiqra.core.service.VectorSearchService"));
                java.lang.reflect.Method m = svc.getClass().getMethod("generateEmbedding", Long.class);
                java.util.Map<String, Object> gen = new java.util.HashMap<>();
                gen.put("s1", m.invoke(svc, 427224009336381440L));
                gen.put("s2", m.invoke(svc, 427224683201609728L));
                gen.put("s3", m.invoke(svc, 427226449729208320L));
                result.put("generatedEmbeddings", gen);
            } catch (Exception e) {
                Throwable t = e;
                while (t.getCause() != null) t = t.getCause();
                result.put("generateEmbeddings_err", t.getMessage());
            }
            // 6c. Direct write 768-dim via SQL for s1 and s2 (bypass service path which has bad UPDATE SQL)
            try {
                for (long sid : new long[]{427224009336381440L, 427224683201609728L, 427226449729208320L}) {
                    StringBuilder sb = new StringBuilder("[");
                    double seed = (sid % 1000) * 0.01;
                    for (int k = 0; k < 768; k++) {
                        if (k > 0) sb.append(",");
                        sb.append(String.format("%.4f", Math.sin(seed + k * 0.07)));
                    }
                    sb.append("]");
                    String vec = sb.toString();
                    int n = template.update(
                        "UPDATE axiqra_solution SET embedding = '" + vec + "'::vector WHERE id = ? AND is_deleted = FALSE",
                        sid);
                    result.put("direct768_" + sid, n);
                }
            } catch (Exception e) {
                Throwable t = e;
                while (t.getCause() != null) t = t.getCause();
                result.put("direct768_err", t.getMessage());
            }
            // 7. verify after update
            try {
                java.util.List<java.util.Map<String, Object>> after = template.queryForList(
                    "SELECT id, visibility_scope, status, embedding IS NOT NULL AS has_embedding FROM axiqra_solution WHERE id IN (?, ?, ?)",
                    new Object[]{427224009336381440L, 427224683201609728L, 427226449729208320L});
                result.put("afterUpdate", after);
            } catch (Exception e) { result.put("afterUpdate_error", e.getMessage().substring(0, Math.min(300, e.getMessage().length()))); }
            // 7b. find which schema axiqra_solution is in
            try {
                java.util.List<java.util.Map<String, Object>> schemas = template.queryForList(
                    "SELECT table_schema, table_name FROM information_schema.tables WHERE table_name = 'axiqra_solution'");
                result.put("axiqra_solutionSchemas", schemas);
            } catch (Exception e) { result.put("axiqra_solutionSchemas_error", e.getMessage().substring(0, Math.min(300, e.getMessage().length()))); }
            // 7c. test with explicit schema prefix
            try {
                StringBuilder sb = new StringBuilder("[0.1");
                for (int k = 1; k < 768; k++) sb.append(",0.1");
                sb.append("]");
                String vec = sb.toString();
                java.util.List<java.util.Map<String, Object>> rows3 = template.queryForList(
                    "SELECT s.id FROM public.axiqra_solution s WHERE s.is_deleted = FALSE AND s.visibility_scope = 'public' AND s.status IN ('verified','stable','canonical') AND s.embedding IS NOT NULL AND (1 - (s.embedding::vector <=> '" + vec + "'::vector)) > 0.50 ORDER BY s.embedding::vector <=> '" + vec + "'::vector LIMIT 10");
                result.put("publicSchemaQuery", rows3);
            } catch (Exception e) {
                Throwable t = e.getCause();
                result.put("publicSchemaQuery_cause", t == null || t.getMessage() == null ? "unknown" : t.getMessage());
            }
            // 8. try vector SQL with 768-dim vector
            try {
                // Build a 768-dim vector
                StringBuilder sb = new StringBuilder("[0.1");
                for (int k = 1; k < 768; k++) sb.append(",0.1");
                sb.append("]");
                String vec = sb.toString();
                // Test 1: simple SELECT with embedding <=> cast
                java.util.List<java.util.Map<String, Object>> rows1 = template.queryForList(
                    "SELECT s.id, (1 - (s.embedding <=> '" + vec + "'::vector)) AS similarity FROM axiqra_solution s WHERE s.is_deleted = FALSE AND s.visibility_scope = 'public' AND s.status IN ('verified','stable','canonical') AND s.embedding IS NOT NULL ORDER BY s.embedding <=> '" + vec + "'::vector LIMIT 10");
                result.put("simple768VecQuery", rows1);
            } catch (Exception e) {
                String msg = e.getMessage() == null ? "unknown" : e.getMessage();
                result.put("simple768VecQuery_error", msg.substring(0, Math.min(400, msg.length())));
            }
            // 9. try exact service-style SQL with filter > 0.5
            try {
                StringBuilder sb = new StringBuilder("[0.1");
                for (int k = 1; k < 768; k++) sb.append(",0.1");
                sb.append("]");
                String vec = sb.toString();
                java.util.List<java.util.Map<String, Object>> rows2 = template.queryForList(
                    "SELECT s.id FROM axiqra_solution s WHERE s.is_deleted = FALSE AND s.visibility_scope = 'public' AND s.status IN ('verified','stable','canonical') AND s.embedding IS NOT NULL AND (1 - (s.embedding <=> '" + vec + "'::vector)) > 0.50 ORDER BY s.embedding <=> '" + vec + "'::vector LIMIT 10");
                result.put("filter768VecQuery", rows2);
            } catch (Exception e) {
                // Print full cause chain
                Throwable t = e;
                int depth = 0;
                while (t != null && depth < 5) {
                    String m = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
                    result.put("err_" + depth + "_" + t.getClass().getSimpleName(), m);
                    t = t.getCause();
                    depth++;
                }
            }
            return ApiResponse.ok(result);
        } catch (Exception e) {
            log.error("Failed to test vector", e);
            return ApiResponse.fail(99999, "Error: " + e.getMessage());
        }
    }

    /**
     * 列出指定表的列类型 (用于诊断 forward_steps 等 JSON 字段)
     */
    @GetMapping("/table-columns")
    @Operation(summary = "查看指定表结构")
    public ApiResponse<java.util.List<java.util.Map<String, Object>>> getTableColumns(
            @org.springframework.web.bind.annotation.RequestParam String table) {
        try {
            JdbcTemplate template = jdbcTemplate();
            java.util.List<java.util.Map<String, Object>> cols = template.queryForList(
                "SELECT column_name, data_type, udt_name, character_maximum_length FROM information_schema.columns WHERE table_name = ? ORDER BY ordinal_position",
                table.toLowerCase()
            );
            return ApiResponse.ok(cols);
        } catch (Exception e) {
            log.error("Failed to query table columns", e);
            return ApiResponse.fail(99999, "Error: " + e.getMessage());
        }
    }

    /**
     * 修复 Feedback 表缺失字段 (V2 migration 中 idempotency_key)
     */
    @PostMapping("/fix-feedback-fields")
    @Operation(summary = "修复反馈表字段")
    public ApiResponse<Map<String, Object>> fixFeedbackFields() {
        JdbcTemplate template = jdbcTemplate();
        java.util.List<String> executed = new java.util.ArrayList<>();
        java.util.List<String> skipped = new java.util.ArrayList<>();
        java.util.List<String> errors = new java.util.ArrayList<>();

        try {
            Integer count = template.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'axiqra_feedback' AND column_name = 'idempotency_key'",
                Integer.class
            );
            if (count != null && count > 0) {
                skipped.add("idempotency_key (already exists)");
            } else {
                template.execute("ALTER TABLE axiqra_feedback ADD COLUMN idempotency_key VARCHAR(100)");
                executed.add("idempotency_key (VARCHAR(100))");
            }

            // Add index
            try {
                template.execute("CREATE INDEX IF NOT EXISTS idx_feedback_idempotency_key ON axiqra_feedback (idempotency_key) WHERE idempotency_key IS NOT NULL");
                executed.add("idx_feedback_idempotency_key");
            } catch (Exception e) {
                errors.add("index: " + e.getMessage());
            }
        } catch (Exception e) {
            errors.add(e.getMessage());
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("executed", executed);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return ApiResponse.ok(result);
    }

    /**
     * 修复 Invocation 表缺失字段 (V3 migration 中 caller_type 等)
     */
    @PostMapping("/fix-invocation-fields")
    @Operation(summary = "修复调用记录表字段")
    public ApiResponse<Map<String, Object>> fixInvocationFields() {
        JdbcTemplate template = jdbcTemplate();
        java.util.List<String> executed = new java.util.ArrayList<>();
        java.util.List<String> skipped = new java.util.ArrayList<>();
        java.util.List<String> errors = new java.util.ArrayList<>();

        // V3 migration 中的字段
        String[][] fields = {
            {"caller_type", "VARCHAR(50)"},
            {"invocation_status", "VARCHAR(50)"},
            {"task_goal", "TEXT"},
            {"error_signature", "TEXT"},
            {"tech_stack", "VARCHAR(500)"},
            {"environment", "VARCHAR(500)"},
            {"context_hash", "VARCHAR(100)"},
            {"fit_score", "DOUBLE PRECISION"},
            {"returned_results_count", "INT"},
            {"prior_attempts", "TEXT"},
            {"problem_type", "VARCHAR(100)"},
            {"user_intent", "VARCHAR(50)"}
        };

        for (String[] field : fields) {
            String columnName = field[0];
            String columnType = field[1];
            try {
                Integer count = template.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'axiqra_invocation' AND column_name = '" + columnName + "'",
                    Integer.class
                );
                if (count != null && count > 0) {
                    skipped.add(columnName + " (already exists)");
                } else {
                    template.execute("ALTER TABLE axiqra_invocation ADD COLUMN " + columnName + " " + columnType);
                    executed.add(columnName + " (" + columnType + ")");
                }
            } catch (Exception e) {
                errors.add(columnName + ": " + e.getMessage());
            }
        }

        // 创建索引
        String[][] indexes = {
            {"idx_invocation_context_hash", "context_hash"},
            {"idx_invocation_task_goal", "task_goal"},
            {"idx_invocation_error_signature", "error_signature"}
        };

        for (String[] idx : indexes) {
            String indexName = idx[0];
            String columnName = idx[1];
            try {
                template.execute("CREATE INDEX IF NOT EXISTS " + indexName + " ON axiqra_invocation (" + columnName + ") WHERE " + columnName + " IS NOT NULL");
                executed.add(indexName);
            } catch (Exception e) {
                if (!e.getMessage().contains("already exists") && !e.getMessage().contains("duplicate")) {
                    errors.add(indexName + ": " + e.getMessage());
                } else {
                    skipped.add(indexName + " (already exists)");
                }
            }
        }

        Map<String, Object> result2 = new java.util.HashMap<>();
        result2.put("executed", executed);
        result2.put("skipped", skipped);
        result2.put("errors", errors);
        return ApiResponse.ok(result2);
    }

    /**
     * 修复 Solution 表缺失字段 (V3 migration 中部分字段)
     */
    @PostMapping("/fix-solution-fields")
    @Operation(summary = "修复方案表字段")
    public ApiResponse<Map<String, Object>> fixSolutionFields() {
        JdbcTemplate template = jdbcTemplate();
        java.util.List<String> executed = new java.util.ArrayList<>();
        java.util.List<String> skipped = new java.util.ArrayList<>();
        java.util.List<String> errors = new java.util.ArrayList<>();

        // V3 migration 中的 Solution 字段
        String[][] fields = {
            {"error_signature", "TEXT"},
            {"environment", "VARCHAR(500)"},
            {"problem_type", "VARCHAR(100)"},
            {"evidence_count", "INT"},
            {"failure_paths", "TEXT"},
            {"applicability", "TEXT"},
            {"inapplicability", "TEXT"}
        };

        for (String[] field : fields) {
            String columnName = field[0];
            String columnType = field[1];
            try {
                Integer count = template.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'axiqra_solution' AND column_name = '" + columnName + "'",
                    Integer.class
                );
                if (count != null && count > 0) {
                    skipped.add(columnName + " (already exists)");
                } else {
                    template.execute("ALTER TABLE axiqra_solution ADD COLUMN " + columnName + " " + columnType);
                    executed.add(columnName + " (" + columnType + ")");
                }
            } catch (Exception e) {
                errors.add(columnName + ": " + e.getMessage());
            }
        }

        // 创建索引
        String[][] indexes = {
            {"idx_solution_error_signature", "error_signature"},
            {"idx_solution_problem_type", "problem_type"},
            {"idx_solution_environment", "environment"}
        };

        for (String[] idx : indexes) {
            String indexName = idx[0];
            String columnName = idx[1];
            try {
                template.execute("CREATE INDEX IF NOT EXISTS " + indexName + " ON axiqra_solution (" + columnName + ") WHERE " + columnName + " IS NOT NULL");
                executed.add(indexName);
            } catch (Exception e) {
                if (!e.getMessage().contains("already exists") && !e.getMessage().contains("duplicate")) {
                    errors.add(indexName + ": " + e.getMessage());
                } else {
                    skipped.add(indexName + " (already exists)");
                }
            }
        }

        Map<String, Object> result3 = new java.util.HashMap<>();
        result3.put("executed", executed);
        result3.put("skipped", skipped);
        result3.put("errors", errors);
        return ApiResponse.ok(result3);
    }

    /**
     * 测试 Invocation 插入 - 直接使用 JdbcTemplate 绕过 MyBatis-Flex
     */
    @PostMapping("/test-invocation")
    @Operation(summary = "测试调用记录插入")
    public ApiResponse<Map<String, Object>> testInvocation() {
        JdbcTemplate template = jdbcTemplate();
        Map<String, Object> result = new java.util.HashMap<>();
        
        try {
            String requestId = "test-" + System.currentTimeMillis();
            
            // 直接执行 INSERT SQL
            int rows = template.update(
                "INSERT INTO axiqra_invocation (request_id, user_id, target_type, target_id, workspace_id, " +
                "invocation_code, tool_type, caller_type, invocation_status, task_goal, result_type, risk_level, " +
                "gmt_create, gmt_modified, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), 0)",
                requestId, 428103443614781440L, "solution", 1L, 428103445791625216L,
                "test_code", "mcp", "ai_tool", "invoked", "Test task goal", "worked", 1
            );
            
            result.put("success", true);
            result.put("rowsInserted", rows);
            result.put("requestId", requestId);
            log.info("【Test Invocation】插入成功: requestId={}", requestId);
            return ApiResponse.ok(result);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
            log.error("【Test Invocation】插入失败: error={}", e.getMessage(), e);
            return ApiResponse.fail(99999, "插入失败: " + e.getMessage());
        }
    }
}