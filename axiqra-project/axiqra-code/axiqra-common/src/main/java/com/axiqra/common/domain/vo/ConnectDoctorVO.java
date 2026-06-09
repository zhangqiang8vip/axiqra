package com.axiqra.common.domain.vo;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Connect doctor 检查结果
 */
@Data
@NoArgsConstructor
public class ConnectDoctorVO {

    private String status;
    private int passedChecks;
    private int totalChecks;
    private List<DoctorCheckItemVO> checks;

    @Data
    @NoArgsConstructor
    @Builder
    public static class DoctorCheckItemVO {
        private String code;
        private String description;
        private boolean passed;
        private String detail;

        public DoctorCheckItemVO(String code, String description, boolean passed, String detail) {
            this.code = code;
            this.description = description;
            this.passed = passed;
            this.detail = detail;
        }
    }

    @Builder
    public ConnectDoctorVO(String status, int passedChecks, int totalChecks, List<DoctorCheckItemVO> checks) {
        this.status = status;
        this.passedChecks = passedChecks;
        this.totalChecks = totalChecks;
        this.checks = checks;
    }
}
