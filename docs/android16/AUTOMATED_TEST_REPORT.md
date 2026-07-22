# 自动化测试报告 — Android 16 / API 36

**执行时间**: 2026-07-22
**环境**: Windows 11, OpenJDK 21.0.10 (Android Studio JBR), Gradle 8.9

## 构建验证

| 检查项 | 状态 | 证据 |
|---|---|---|
| `gradlew clean` | ✅ PASS | BUILD SUCCESSFUL |
| `gradlew test` | ✅ PASS | 项目尚无单元测试 (NO-SOURCE)，无已有失败 |
| `gradlew lint` | ✅ PASS | 0 errors, 37 warnings（均为升级前已有弃用/代码风格警告） |
| `gradlew assembleDebug` | ✅ PASS | APK 输出: `app/build/outputs/apk/debug/app-debug.apk` |
| `gradlew bundleRelease` | ✅ PASS | AAB 输出: `app/build/outputs/bundle/release/app-release.aab` |

## Lint 警告详情 (37)

升级后 Lint 0 错误，37 警告均为升级前已有：

| 警告数 | 类型 |
|---|---|
| 4 | Deprecation（NsdManager host, resolveService, WIFI_MODE_FULL_HIGH_PERF, 未使用参数） |
| 33 | 其他（资源、布局、无障碍等） |

## 测试环境

| 项目 | 状态 |
|---|---|
| Android 16 模拟器 | NOT RUN — 本机未配置 |
| 真机测试 | NOT RUN — 需要用户操作 |

## 结果分类

| 类别 | 数量 |
|---|---|
| PASS | 5 |
| FAIL | 0 |
| BLOCKED | 0 |
| NOT RUN | 2 (模拟器/真机) |
