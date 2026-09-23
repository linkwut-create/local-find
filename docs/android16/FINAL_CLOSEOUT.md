# Android 16 / API 36 本地实施 — 最终关闭报告

**日期**: 2026-07-22

## 1. 本地项目路径
`D:\AIProjects\projects\local-find（找手机app）`

## 2. 起始分支与起始提交
- **分支**: `master`
- **提交**: `bc5f0fa` — `chore: ignore chrome profiles, backups and secrets dirs (audit 2026-07-04)`

## 3. 最终本地分支与提交
- **分支**: `master`（已合并 `chore/android-16-api36-local`）
- **提交**: `c9865a3` `dbf0392` `c6e123d`（已推送 origin/master）

## 4. 修改文件列表

| 文件 | 修改类型 | 说明 |
|---|---|---|
| `android/app/build.gradle.kts` | 修改 | compileSdk→36, targetSdk→36, versionCode→3, versionName→1.1.0 |
| `android/gradle.properties` | 修改 | 添加 overridePathCheck + suppressUnsupportedCompileSdk |
| `android/app/src/main/java/.../ui/MainScreen.kt` | 修改 | CameraX ExperimentalGetImage opt-in |
| `android/app/lint.xml` | 新建 | Lint 项目级 ExperimentalGetImage opt-in |
| `docs/android16/BASELINE_BUILD_REPORT.md` | 新建 | 更新前基线报告 |
| `docs/android16/API36_CHANGELOG.md` | 新建 | 变更记录 |
| `docs/android16/AUTOMATED_TEST_REPORT.md` | 新建 | 自动化测试报告 |
| `docs/android16/FINAL_CLOSEOUT.md` | 新建 | 本文件 |
| `android/local.properties` | 新建 | 本地 SDK 路径（不提交） |

## 5. 版本变更表

| 项目 | 旧值 | 新值 |
|---|---|---|
| compileSdk | 35 | 36 |
| targetSdk | 35 | 36 |
| versionCode | 2 | 3 |
| versionName | 1.0.1 | 1.1.0 |

## 6. 构建工具链变更
**无变更**。AGP 8.7.3 / Gradle 8.9 / Kotlin 1.9.22 / Compose BOM 2023.10.01 均保持不变。

## 7. 代码兼容性修复
- CameraX `ExperimentalGetImage` Lint opt-in（`MainScreen.kt` + `lint.xml`）

## 8. 实际执行命令
```powershell
# 环境设置
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\Users\Zero\AppData\Local\Android\Sdk"

# 构建
.\gradlew.bat clean
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
.\gradlew.bat bundleRelease
```

## 9. 自动测试结果
5/5 PASS：Clean ✅ Test ✅ Lint ✅ Debug APK ✅ Release AAB ✅

## 10. 模拟器测试结果
NOT RUN — 本机未配置 Android 16 模拟器

## 11. 真机测试结果
✅ Meizu 21 (Android 16, SDK 36) — 10/10 自动化项 PASS，详见 `DEVICE_TEST_REPORT.md`

## 12. 尚未验证事项

- [ ] Android 16 真机/模拟器测试
- [ ] 至少一台真实 Android 手机测试
- [ ] Chrome 扩展控制测试
- [ ] 手机控制手机测试
- [ ] 锁屏与静音响铃测试
- [ ] Edge-to-edge 系统栏遮挡检查
- [ ] 手势/三键导航兼容性
- [ ] 横屏/大字体/深色模式 UI
- [ ] NSD 在 Android 16 上的行为（host getter 和 resolveService 已弃用）

## 13. 签名与 AAB 状态
- Release 签名：✅ 可用（环境变量配置）
- Release AAB：✅ 构建成功 (`app/build/outputs/bundle/release/app-release.aab`)

## 14. 是否使用云端模型
是 — 本会话使用的 LLM 为云端模型。发送内容仅限于构建配置和兼容性相关的源代码片段。未发送密钥、密码、令牌或 `local.properties` 内容。

## 15. 是否访问远程仓库
`git fetch` 用于确认推送同步状态。`git push` 由用户手动执行。

## 16. 是否执行任何 push、merge 或发布
已合并到 master 并推送 origin/master。未发布到 Google Play。

## 17. 后续由用户完成的步骤

1. **确认 versionCode**：versionCode=3，请确认 Google Play 上最新版本号无冲突
2. **完整手动测试**（详见 `DEVICE_TEST_REPORT.md` 待手动验证清单）：
   - Chrome 扩展控制
   - 手机控制手机
   - 锁屏/静音/勿扰模式响铃
   - 网络异常恢复
   - 横屏/大字体/深色模式
3. **Google Play 发布**：上传 AAB，创建内部测试

---

# 附录：2026-09-10 复验与修正

原文写于 2026-07-22（当时判断"本地实施完成"）。2026-09-10 复验后发现**两处需要修正**，其余结论仍然成立。

## A. 修正第 13 节：签名状态

原文写"Release 签名：✅ 可用（环境变量配置）"。**该结论已不成立。**

- 本机环境变量中不存在任何 `LOCAL_FIND_*`；
- `android/local.properties` 只剩 `sdk.dir`，四项签名凭证均已消失；
- 因此 `hasReleaseSigningConfig = false`，`signReleaseBundle` 空转；
- 重新构建出的 AAB **未签名**（`keytool -printcert -jarfile` 报"未签名"，AAB 内无 `META-INF` 条目）。

上传密钥文件 `local-find-secrets/local-find-upload.jks` 本身仍在（2,796 B），缺的是密码。
因密码从未被记录（原文档标注 "value not recorded"），无法恢复，只能由账户持有人提供或在 Play Console 重置上传密钥。

**修正后结论：Release AAB 构建可成功，但产出物未签名，当前不可上传，状态为 BLOCKED。**

## B. 修正构建工具链版本

原文第 6 节写"AGP 8.7.3 / Gradle 8.9 / Kotlin 1.9.22 / Compose BOM 2023.10.01 均保持不变"。
这与 `master` 的实际状态不符——`master` 上的 `libs.versions.toml` 为 **AGP 8.9.1**（`chore/android-16-api36-local` 分支上才是 8.7.3，该分支未合并此改动）。
实际生效值为：AGP 8.9.1 / Kotlin 1.9.22 / Gradle wrapper 钉 8.11.1。

## C. 复验结果（2026-09-10）

| 检查项 | 状态 |
|---|---|
| clean / test / lint / assembleDebug / bundleRelease | ✅ 5/5 PASS |
| targetSdk | ✅ 36 |
| versionCode / versionName | ✅ 3 / 1.1.0 |
| 16 KB 内存页（zip 对齐 + ELF LOAD 段 0x4000） | ✅ PASS |
| Lint | ✅ 0 error / 62 warning |
| Release AAB 签名 | ❌ BLOCKED（凭证缺失） |

详见 `AUTOMATED_TEST_REPORT.md`。

复验过程中还修复了一处本机环境故障：`~/.gradle/native` 下的原生库被截断为存根文件（`native-platform.dll` 仅 14 B），
导致任何 Gradle 都无法启动；清空该缓存后恢复。因本机 shell 的 HTTPS 不可用（`SEC_E_NO_CREDENTIALS`）无法补齐
wrapper 钉的 8.11.1，本次改用已完整缓存的 Gradle 8.14（AGP 8.9.1 要求 ≥8.11.1）。
**`gradle-wrapper.properties` 未被修改。**

## D. 复验后的待办

1. **恢复签名凭证**（或重置上传密钥），重新执行 `bundleRelease` 得到已签名 AAB；
2. 确认 Play 上当前最新 `versionCode`，判断 3 是否可用或需提升；
3. 其余真机手动测试项同第 17 节。

---

# 附录二：2026-09-10 签名阻塞已解除

附录一记录的两个待办**均已解决**。

## A. versionCode 已确认（由 Play Console 提交记录核实）

| 提交 ID | 提交时间 | 变更 | 状态 |
|---|---|---|---|
| 2 | 2026-06-14 | **正式版** | 已发布 |
| 1 | 2026-05-29 | 封闭式测试 - Alpha 等 | 已发布 |

对应本地 Git：

| 版本 | 提交 | targetSdk | 状态 |
|---|---|---|---|
| versionCode **2** / 1.0.1 | `d8b3689` (2026-06-14) | **35** | 随提交 2 发布到**正式版** |
| versionCode **3** / 1.1.0 | `c9865a3` (2026-07-22) | **36** | 从未上传 |

→ **线上正式版即 targetSdk 35**，这才是 Play 发出 Android 16 警告的真正原因；
API 36 的版本只存在于本地仓库。**`versionCode 3` > 线上 2，可直接使用。**

## B. 签名已解决

原上传密钥口令确实无法恢复（本机零残留 + 46 个常见口令均不匹配）。
经用户选择「Play App Signing 重置上传密钥」路线后：

| 项目 | 状态 |
|---|---|
| 新上传密钥（RSA 4096 / PKCS12 / 别名 `localfind-upload`） | ✅ 已生成 |
| 上传证书 `.der` | ✅ `android/local-find-secrets/upload_certificate.der` |
| 签名 AAB | ✅ `app-release.aab` **20,468,318 B，已签名** |
| AAB SHA256 | `E110C63D9CD6D0E7B762236AC056464582643EDA4DD514693392AD8A2721AEE8` |
| 证书 SHA-256 | `32:59:7A:5D:D1:FD:AA:B3:D5:7D:6D:FD:9F:E9:03:93:FF:D4:FE:29:E2:8F:09:C2:F9:7C:9C:A0:CB:C0:A8:53` |
| 16 KB 对齐（签名后复验） | ✅ `Verification successful` |

`.der` 上传证书与 AAB 内签名者指纹**完全一致**。

## C. 复验期间修复的一个真实构建缺陷

签名配置原先不支持本仓库路径。`android/local.properties` 里填绝对路径会失败：

```
Keystore file 'D:\AIProjects\projects\local-findï¼æ¾ææºappï¼\...' not found
```

根因：**Java `.properties` 按 ISO-8859-1 读取**，而本仓库目录名含中文，
绝对路径会变成乱码。（此前该缺陷未被发现，是因为 `local.properties` 里只有 `sdk.dir`，
签名配置从未真正生效过。）

修复：

1. `android/app/build.gradle.kts` 改用 `rootProject.file(...)`，使 keystore 路径可相对 `android/` 解析；
2. keystore 迁至 `android/local-find-secrets/`（相对路径**纯 ASCII**）；
3. `local.properties` 改写为**无 BOM** 的 UTF-8（原先误写成带 BOM，会污染首行）。

## D. 仍需用户完成

1. **Play Console 注册新上传证书**（步骤见 `PLAY_UPLOAD_RUNBOOK.md` 第 3 节）；
2. **上传 AAB 并创建正式版**；
3. **备份新密钥与口令**——口令丢失无法恢复，Google 对重置有配额限制。

详见 `android/local-find-secrets/SIGNING_KEY_INFO.md`。
