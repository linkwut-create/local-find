# Android 16 / API 36 升级变更记录

**日期**: 2026-07-22
**分支**: `chore/android-16-api36-local`
**起始提交**: `bc5f0fa`

## 版本变更

| 项目 | 旧值 | 新值 | 文件 |
|---|---|---|---|
| compileSdk | 35 | **36** | `android/app/build.gradle.kts` |
| targetSdk | 35 | **36** | `android/app/build.gradle.kts` |
| versionCode | 2 | **3** | `android/app/build.gradle.kts` |
| versionName | 1.0.1 | **1.1.0** | `android/app/build.gradle.kts` |

## 构建工具链

AGP / Gradle / Kotlin / Compose 均未升级，当前版本与 compileSdk 36 编译兼容。

| 工具 | 版本 | 是否变更 |
|---|---|---|
| AGP | 8.7.3 | 未变 |
| Gradle | 8.9 | 未变 |
| Kotlin | 1.9.22 | 未变 |
| Compose BOM | 2023.10.01 | 未变 |
| Compose Compiler | 1.5.8 | 未变 |
| CameraX | 1.5.3 | 未变 |
| Ktor | 2.3.8 | 未变 |

## 代码兼容性修复

### CameraX ExperimentalGetImage Lint 修复

**文件**: `android/app/src/main/java/io/github/linkwutcreate/localfind/ui/MainScreen.kt`

- 添加 `import androidx.camera.core.ExperimentalGetImage`
- `QrScannerScreen` 函数添加 `@OptIn(ExperimentalGetImage::class)` 注解
- `imageProxy.image` 调用处添加局部 `@OptIn` 注解
- 新建 `android/app/lint.xml` 配置项目级 Lint opt-in

### 构建配置补充

**文件**: `android/gradle.properties`

- 添加 `android.overridePathCheck=true`（路径含中文字符，AGP Windows 限制）
- 添加 `android.suppressUnsupportedCompileSdk=36`（AGP 8.7.3 未正式测试 compileSdk 36，构建已验证通过）

## Android 16 兼容性审查摘要

| 领域 | 结论 | 说明 |
|---|---|---|
| Edge-to-edge | ⚠️ 待真机验证 | Scaffold 使用 innerPadding，需在 Android 16 真机上确认无遮挡 |
| Predictive Back | ✅ 无需修改 | 项目无自定义返回处理 |
| 前台服务 | ✅ 兼容 | 已使用 `specialUse` 类型，通知渠道完整 |
| NSD/局域网 | ⚠️ 存在弃用 API | `host` getter 和 `resolveService` 已弃用，功能仍正常 |
| 通知权限 | ✅ 兼容 | POST_NOTIFICATIONS 运行时权限已实现 |
| CameraX/扫码 | ✅ 已修复 | ExperimentalGetImage opt-in 已标注 |
| 生物识别 | ✅ 兼容 | BiometricPrompt 用法标准 |
| WakeLock/WifiLock | ✅ 兼容 | 使用 PARTIAL_WAKE_LOCK，WIFI_MODE_FULL_HIGH_PERF 已弃用但功能正常 |
| usesCleartextTraffic | ✅ 兼容 | 本地 HTTP 通信必需 |
