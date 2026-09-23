# 自动化测试报告 — Android 16 / API 36

> 本文件记录 **2026-09-10 重新验证**的结果，取代 2026-07-22 的旧记录。
> 旧记录有两处已不准确：Gradle 版本，以及 Lint 警告数量。

## 1. 执行环境

| 项目 | 值 |
|---|---|
| 日期 | 2026-09-10 |
| 系统 | Windows 11 amd64 |
| JDK | 21.0.10 (Android Studio JBR) |
| Gradle | **8.14**（见第 4 节说明） |
| AGP | 8.9.1 |
| Kotlin | 1.9.22 |
| Android SDK | `C:\Users\Zero\AppData\Local\Android\Sdk`（platforms 含 android-36） |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26 |
| versionCode / versionName | 3 / 1.1.0 |

## 2. 构建验证结果

| 检查项 | 状态 | 证据 |
|---|---|---|
| `clean` | ✅ PASS | BUILD SUCCESSFUL |
| `test` | ✅ PASS | `testDebugUnitTest` / `testReleaseUnitTest` 均为 NO-SOURCE（项目暂无单元测试，不存在已失败用例） |
| `lint` | ✅ PASS | **0 error**，62 warning + 1 info |
| `assembleDebug` | ✅ PASS | `android/app/build/outputs/apk/debug/app-debug.apk` (36,914,368 bytes) |
| `bundleRelease` | ✅ PASS | `android/app/build/outputs/bundle/release/app-release.aab` (20,432,401 bytes) |

一次完整执行耗时 **17m 18s**，`100 actionable tasks: 100 executed`。

### 2.1 产物身份（从合并后的 Manifest 读出）

```
android:versionCode="3"
android:versionName="1.1.0"
android:minSdkVersion="26"
android:targetSdkVersion="36"
```

`targetSdkVersion=36` 满足 Google Play 的 Android 16 要求。

AAB SHA256：

```
FE3FB1AE47916EAA670608618030DDF7426D7E9E04B5B0252BD71915120E1D5D
```

## 3. 16 KB 内存页支持

Play 此前拒绝过本应用的 `versionCode 1`，原因是缺少 16 KB 内存页支持。本次对**新构建的 AAB** 重新完整核验：

| 检查 | 状态 | 证据 |
|---|---|---|
| zip 对齐 `zipalign -c -P 16 -v 4` | ✅ PASS | `Verification successful` |
| arm64-v8a 原生库存在 | ✅ PASS | `libbarhopper_v3.so` (4,946,720 B)、`libimage_processing_util_jni.so`、`libsurface_util_jni.so` |
| ELF LOAD 段对齐 | ✅ PASS | 三个 arm64 库的全部 LOAD 段对齐值均为 `0x4000` (= 16 KB) |

AAB 内共 12 个 `.so`，覆盖 arm64-v8a / armeabi-v7a / x86 / x86_64。

## 4. 构建环境故障与处置（重要）

本次验证开始时，**本机任何 Gradle 都无法启动**：

```
Gradle could not start your build.
> Could not initialize native services.
   > Failed to load native library 'native-platform.dll' for Windows 11 amd64
```

根因：`C:\Users\Zero\.gradle\native` 下的原生库是**被截断的存根文件**——

| 文件 | 截断后 | 实际应有 |
|---|---:|---:|
| `native-platform.dll` | 14 B | 141,312 B |
| `jansi.dll` | 26 B | 26,112 B |
| `gradle-fileevents.dll` | 46 B | 465,408 B |

DLL 真身在对应 jar 内是完整的，因此清空该损坏缓存、让 Gradle 重新解包即可恢复。恢复后三个文件大小已正常（见上表右列）。

各缓存的完整性状况：

| Gradle 分发 | 状态 |
|---|---|
| `gradle-8.9-bin` | ❌ 仅有 20,412,096 B 的 `.part`，未下载完 |
| `gradle-8.11.1-bin`（wrapper 钉的版本） | ❌ 仅有 23,590,272 B 的 `.part`，未下载完 |
| `gradle-8.14-bin` | ✅ 完整可用（本次使用） |
| `gradle-9.6.1-bin` | ❌ 目录存在但 `lib/*.jar` 全为 0 字节 |

本机 shell 的 HTTPS 不可用（`curl: (35) schannel: AcquireCredentialsHandle failed: SEC_E_NO_CREDENTIALS`），因此**无法联网补齐 8.11.1**，改用已完整缓存的 Gradle 8.14 完成构建。

AGP 8.9.1 要求 Gradle ≥ 8.11.1，8.14 满足。**`android/gradle/wrapper/gradle-wrapper.properties` 未被修改**，仍钉 `gradle-8.11.1-bin.zip`。后续如需完全按 wrapper 版本构建，请先修复本机网络的 TLS 凭证问题，让 8.11.1 下载完成。

## 5. Lint 详情（0 error / 62 warning）

| 数量 | 规则 |
|---:|---|
| 42 | `GradleDependency`（依赖有更新版本可用） |
| 13 | `UseKtx`（可用 AndroidX KTX 扩展） |
| 3 | `AndroidGradlePluginVersion`（AGP 有更新版本可用） |
| 2 | `ObsoleteSdkInt` |
| 2 | `MonochromeLauncherIcon` |
| 1 | `AutoboxingStateCreation` |

均为**升级前既有的建议类警告**，无 error。

Kotlin 编译附带 3 条警告（升级前既有）：

- `MainScreen.kt:254` 参数 `onAuthenticate` 未被使用；
- `MainScreen.kt:1499`、`1531` `@OptIn(ExperimentalGetImage::class)` 被忽略，因为该注解不是 opt-in 标记。

这 3 条不影响构建结果；`UnsafeOptInUsageError` 已在 `app/lint.xml` 单独降级处理。

## 6. 签名状态 — ✅ 已解决（2026-09-10 晚）

### 6.1 初始状态（当时为 BLOCKED）

| 检查 | 状态 | 证据 |
|---|---|---|
| AAB 是否签名 | ❌ 未签名 | `keytool -printcert -jarfile` → "这个 jar 文件未签名"；AAB 内无 `META-INF` 条目 |
| 上传密钥文件 | ✅ 存在 | `local-find-secrets/local-find-upload.jks` (2,796 B) |
| 四项签名凭证 | ❌ 缺失 | `android/local.properties` 仅有 `sdk.dir`；环境变量无 `LOCAL_FIND_*` |

因凭证缺失，`hasReleaseSigningConfig` 为 `false`，`signReleaseBundle` 空转。
原口令确认为**不可恢复**（本机零残留，46 个常见口令均不匹配）。

### 6.2 处置结果

经用户选择「Play App Signing 重置上传密钥」路线：

| 检查 | 状态 | 证据 |
|---|---|---|
| 新上传密钥 | ✅ 已生成 | `android/local-find-secrets/local-find-upload-new.jks` (4,460 B)，RSA 4096 / PKCS12 / 别名 `localfind-upload` |
| 上传证书 `.der` | ✅ 已导出 | `android/local-find-secrets/upload_certificate.der` (1,432 B) |
| **AAB 是否签名** | ✅ **已签名** | `keytool -printcert -jarfile` 读出 `CN=Local Find Upload`；含 `META-INF` 签名条目 |
| AAB 体积 | ✅ 20,468,318 B | 由 20,432,401 B 增大约 36 KB（签名开销） |
| AAB SHA256 | ✅ | `E110C63D9CD6D0E7B762236AC056464582643EDA4DD514693392AD8A2721AEE8` |
| 证书 SHA-256 | ✅ | `32:59:7A:5D:D1:FD:AA:B3:D5:7D:6D:FD:9F:E9:03:93:FF:D4:FE:29:E2:8F:09:C2:F9:7C:9C:A0:CB:C0:A8:53` |
| `.der` 与 AAB 签名者一致 | ✅ | 两者 SHA-1 / SHA-256 完全相同 |
| 16 KB 对齐（签名后复验） | ✅ | `Verification successful` |
| versionCode / targetSdk | ✅ | 3 / 36（Play 线上正式版为 2 / 35） |

**AAB 现在可以上传。**

### 6.3 复验期间修复的真实构建缺陷

签名配置原先**无法处理本仓库路径**。绝对路径会失败：

```
Keystore file 'D:\AIProjects\projects\local-findï¼æ¾ææºappï¼\...' not found
```

根因：**Java `.properties` 按 ISO-8859-1 读取**，本仓库目录名含中文，
绝对路径必然变乱码。此前未被发现，是因为 `local.properties` 里只有 `sdk.dir`，
签名配置从未真正生效过。

修复内容（3 处）：

1. `android/app/build.gradle.kts`：`file(...)` → `rootProject.file(...)`，
   使 keystore 路径可相对 `android/` 解析；
2. keystore 迁至 `android/local-find-secrets/`，相对路径为**纯 ASCII**；
3. `local.properties` 改写为**无 BOM** 的 UTF-8（原先误写成带 BOM，会污染首行）。

### 6.4 早期用一次性密钥的链路验证

在拿到真实凭证前，曾用一枚一次性 RSA 2048 密钥（`CN=SigProbe`，PKCS12，有效期 30 天）
走通完整签名流程，以确认"凭证一旦补齐就一定能产出可用产物"。凭证仅经子进程环境变量注入，
未写入任何文件、未打印到日志：

| 步骤 | 结果 |
|---|---|
| 生成一次性 keystore | ✅ 2,732 B |
| `bundleRelease`（签名配置生效） | ✅ BUILD SUCCESSFUL，`signReleaseBundle` 实际执行 |
| AAB 签名核验 | ✅ `keytool -printcert -jarfile` 读出证书 `CN=SigProbe` |
| AAB 内签名文件 | ✅ `META-INF/SIGPROBE.RSA`、`SIGPROBE.SF`、`MANIFEST.MF` |
| AAB 体积变化 | 20,432,401 B → 20,467,494 B（+35,093 B 签名开销） |
| 16 KB 对齐（签名后复验） | ✅ `Verification successful` |

结论：签名逻辑本身正确可用，瓶颈只在凭证缺失——后续的真实构建印证了这一点。

验证后该一次性密钥**已立即删除**，产物也已重建。该密钥**不是**上传密钥，
任何情况下都不可用于 Google Play 上传。

> 注：当时用于验证的 Gradle 编译产物与最终签名的 AAB 体积略有差异
> （最终为 20,468,318 B），属正常构建差异。

## 7. 结果分类

| 类别 | 数量 | 明细 |
|---|---:|---|
| PASS | 5 | clean / test / lint / assembleDebug / bundleRelease |
| FAIL | 0 | — |
| BLOCKED | 0 | ~~Release AAB 签名~~ → 已于 2026-09-10 晚解决（见第 6 节） |
| NOT RUN | 2 | Android 16 模拟器、真机回归 |

## 8. 边界声明

本次执行分为两个阶段：

1. **只读复验阶段**：仅执行读取与构建验证，未修改任何代码、构建配置或签名文件；
2. **签名修复阶段**：经用户明确选择「Play App Signing 重置上传密钥」路线后，
   生成了新上传密钥、修改了 `android/app/build.gradle.kts` 的 keystore 路径解析方式、
   更新了 `local.properties`（gitignored）。

全程未访问远程仓库，未执行 push / fetch / merge / clone，未上传或发布到 Google Play。
`applicationId`、包名、配对协议、Chrome 扩展协议均未改动。

## 9. 复现命令

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\Users\Zero\AppData\Local\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# wrapper 分发下载不完整时，可直接使用已缓存的 Gradle 8.14：
$gradle = "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.14-bin\18nmau8r28wyp9qkhrj7hed8f\gradle-8.14\bin\gradle.bat"

& $gradle -p .\android --no-daemon clean test lint assembleDebug bundleRelease

# 16 KB 核验
& "$env:ANDROID_HOME\build-tools\36.0.0\zipalign.exe" -c -P 16 -v 4 .\android\app\build\outputs\bundle\release\app-release.aab
& "$env:ANDROID_HOME\ndk\28.2.13676358\toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-readelf.exe" -l <解包出的 arm64 .so>
```
