# 更新前构建基线报告

<!-- touched -->

**执行时间**: 2026-07-22

## 本地环境

| 项目 | 值 |
|---|---|
| Java | OpenJDK 21.0.10 (Android Studio JBR) |
| JAVA_HOME | C:\Program Files\Android\Android Studio\jbr |
| ANDROID_HOME | C:\Users\Zero\AppData\Local\Android\Sdk |
| SDK Platforms | android-34, android-35, android-35-ext14, android-35-ext15, android-36.1 |
| Build Tools | 34.0.0, 36.1.0, 37.0.0 |
| Gradle Wrapper | 8.9 |

## 当前版本配置

| 项目 | 值 | 文件 |
|---|---|---|
| compileSdk | 35 | app/build.gradle.kts |
| targetSdk | 35 | app/build.gradle.kts |
| minSdk | 26 | app/build.gradle.kts |
| versionCode | 2 | app/build.gradle.kts |
| versionName | 1.0.1 | app/build.gradle.kts |
| AGP | 8.7.3 | gradle/libs.versions.toml |
| Kotlin | 1.9.22 | gradle/libs.versions.toml |
| Compose BOM | 2023.10.01 | gradle/libs.versions.toml |
| Compose Compiler | 1.5.8 | app/build.gradle.kts |
| Java Target | 17 | app/build.gradle.kts |

## 构建结果

### 命令执行

```powershell
# 环境变量设置
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\Users\Zero\AppData\Local\Android\Sdk"

# gradle.properties 修改（路径中文字符）
android.overridePathCheck=true
```

| 命令 | 状态 | 备注 |
|---|---|---|
| `gradlew clean` | ✅ PASS | |
| `gradlew test` | ✅ PASS | 项目尚无单元测试 (NO-SOURCE) |
| `gradlew lint` | ❌ FAIL | 1 error, 37 warnings（详见下文） |
| `gradlew assembleDebug` | ✅ PASS | |
| `gradlew bundleRelease` | ✅ PASS | 签名配置可用 |

### Lint 失败详情

**错误 (1)**:
- `UnsafeOptInUsageError` @ `MainScreen.kt:1529` — `imageProxy.image` 未标记 `@OptIn(ExperimentalGetImage::class)`
- 这是升级前已有问题，与 API 36 无关

**警告 (37)**，主要包括:
- 4 个 deprecation 警告（`NsdDiscoveryManager` host getter、resolveService、`WIFI_MODE_FULL_HIGH_PERF`、未使用参数）
- 33 个其他 lint 警告

### 升级前已有问题分类

1. **Lint 错误 (1)**: CameraX ExperimentalGetImage opt-in — 可在本次升级中修复
2. **弃用 API (3)**: NsdManager host getter, resolveService, WIFI_MODE_FULL_HIGH_PERF — 可能受 API 36 影响
3. **SDK XML version warning**: build-tools 与 SDK 版本不完全匹配 — 不影响构建

## 构建产物

| 产物 | 路径 |
|---|---|
| Debug APK | `android/app/build/outputs/apk/debug/app-debug.apk` |
| Release AAB | `android/app/build/outputs/bundle/release/app-release.aab` |

## 签名状态

- Release 签名配置：✅ 可用（环境变量 `LOCAL_FIND_UPLOAD_*` 已配置）
- 密码：未输出（合规）
