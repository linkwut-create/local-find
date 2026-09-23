# 2026-09-23 落地成果证据 — LOCALFIND-LAND-RC-0923

## 0. 开工快照

- HEAD（开工前）：`b619d82`（master，07-22 后无提交）
- `git status --porcelain`（开工前）：76 项（19 个已跟踪文件改动，57 个新增/未跟踪）
- 备份：`robocopy` 全量复制到 `D:\AIProjects\cache\local-find-wip-backup-0923`（排除
  `android/.gradle-user-home`、`android/.tmp` 两个可重建缓存目录）
  实测：**29,703 目录 / 59,743 文件 / 7.22 GB，0 FAILED**（robocopy exit code 1 = 正常，表示有文件成功复制）

## 1. 未跟踪文件分类

| 类别 | 处置 | 依据 |
|---|---|---|
| 源码 / 测试 / 文档 | 进仓，分批提交（见第 3 节） | 09-14 两份计划的真实实现 |
| 截图 `android/*.png`、UI dump `android/*.xml`、APK `android/*.apk` | 不进仓，仅 `.gitignore` + 保留在备份 | 未做逐张视觉核对（是否含二维码/令牌），保守起见一律不进仓、不搬到 `docs/evidence/` |
| CDP/手动调试 json（`tools/cdp-*.json`、`tools/now-status*.json`、`tools/soak-*.json` 等 25 个文件） | 不进仓，仅 `.gitignore` + 保留在备份 | 敏感扫描见第 2 节 |
| `android/.gradle-user-home/`、`android/.tmp/` | 不进仓，`.gitignore` | 可重建的本机构建缓存 |

## 2. 敏感扫描（仅计数与字段名，未打印值）

对 25 个未进仓的调试 json 文件执行两轮扫描：

1. 键名包含 `token|secret|password|auth|credential`（含连字符变体）：**0 命中**。
2. 值为 32+ 位不透明字符串的字段：**21 个文件命中，字段名均为 `extensionId`（Chrome 扩展 ID，公开标识符）
   或 `targetId`（CDP 调试目标 ID）**，均非配对令牌/控制令牌。

结论：这批调试 json 实测**不含**配对/控制令牌，任务包里"可能含配对 token"的预判未成立，但仍按保守处置排除出仓。

另核查 `android/localfind-pairing-unlocked.xml` 的 UI 文本节点，命中 3 处含"令牌"字样，
均为按钮标签/说明文案（"重置令牌"、"令牌仅限局域网使用"等），**非令牌本身的值**。

## 3. 提交记录（均先过 `mcp__local-llm__local_review_diff commit_gate=true`，ok=true 后提交）

| 提交 | 内容 | 文件数 | +/- |
|---|---|---:|---|
| `a2e4fbc` | .gitignore：工作站缓存 + 09-14 调试产物排除 | 1 | +20 |
| `f92715d` | 生命周期恢复：ServiceRunState/ServiceRestartReceiver/onTaskRemoved | 5 | +158/-13 |
| `070205f` | 发现/身份验证：DiscoveryEndpointPolicy/IdentityBindingPolicy/UdpDiscoveryAnnouncer | 7 | +452/-38 |
| `e9bc2f3` | 令牌存储改按设备 ID 绑定 + HttpServerManager 生命周期加固 | 2 | +241/-45 |
| `3ea6718` | UI 文案改写 + 设备身份接线 | 2 | +35/-25 |
| `ae75f76` | Chrome 扩展 /24 地址恢复 + 本地发现桥 | 4 | +829/-6 |
| `d9d556b` | CDP 加载工具切到 Canary 支持的 pipe 流程 | 3 | +414/-21 |
| `3474c27` | 09-14 计划文档 + 产品范围定位 + 签名路径修复 | 5 | +187/-3 |
| `5f3ed01` | Android 16 复验与签名解决全过程文档 | 3 | +369/-36 |
| `3ed69f0` | Play 上传操作手册（含已完成的上传/送审记录） | 1 | +350 |
| `ea8c5f7` | 07-22 Android 16 本地执行规划书 | 1 | +964 |

**合计 11 笔**，单笔暂存 diff 均 ≤ 40000 字符（最大批次 F 约 34.7K）。评审与提交之间未插入任何文件写入。

## 4. 09-14 两份计划 vs 实际代码核对

| 计划条目 | 代码位置 | 核对结论 |
|---|---|---|
| 保存"应保持可找"运行意图，Activity/boot/update/task-removed 恢复，显式停止清意图 | `ServiceRunState`、`ServiceRestartReceiver`、`MainActivity`、`FindPhoneForegroundService.onTaskRemoved` | ✅ 一致，非空文档 |
| 扩展按持久设备 ID 在旧地址所在 `/24` 内恢复 | `chrome-extension/popup.js` `recoverSelectedDeviceAddress`/`discoverSelectedDeviceAddress`，`tools/local_find_discovery_bridge.cjs` | ✅ 一致，且比计划描述更完整（先查本地发现桥，再退化到扩展内 `/24` 扫描） |
| 中英文后台引导文案 | `LocalFindStrings.kt` | ✅ 已更新，含小米/华为/OPPO/魅族厂商设置 |
| Canary 加载方式改为 `--remote-debugging-pipe` + CDP | `tools/load_extension_cdp_pipe.cjs/js`、`tools/open_cws_screenshot_profile.ps1` | ✅ 一致 |

## 5. 新发现（未在计划/任务包中预判）

- Chrome 扩展 `manifest.json` 版本号仍是 `0.1.0`，与 Android `versionCode 3 / versionName 1.1.0` 不一致，
  需要在发布候选阶段对齐（属于任务包 §2.4 "manifest 版本与 Android 版本对齐"的具体缺口）。
- `android/app/build.gradle.kts` 的 keystore 路径此前用绝对路径，因 Java `.properties` 按 ISO-8859-1
  读取、本仓库目录名含中文会变乱码，已在本轮 `3474c27` 修复为 `rootProject.file(...)` 相对路径解析
  （历史证据见 `docs/android16/AUTOMATED_TEST_REPORT.md` §6.3、`FINAL_CLOSEOUT.md` 附录二 C）。
- `tools/load_extension_cdp_pipe.cjs` 与 `.js` 为字节级完全相同的重复文件，仅扩展名不同；
  按"不删除工作区文件"的红线原样落地，未做去重。
- `docs/android16/PLAY_UPLOAD_RUNBOOK.md`（07-22 之后、本会话开工之前的历史文档）显示
  **Play 正式版 `versionCode 3 / 1.1.0` 已于 2026-09-13 由账户持有人本人上传并提交送审**，
  与本任务包"不做"清单中的"不上传 Play"完全一致——本会话没有做、也不会做任何上传动作，
  这里只是如实记录已经发生过的历史事实。

## 6. 门禁基础设施插曲（对任务本身无影响）

开工阶段一度撞上 `local-llm-pipeline` 的 routectl 任务路由锁（会话继承了监工"只写文档"计划的哈希，
导致所有 Bash/PowerShell 被拒），已如实记录在 `_dispatch\status\LOCALFIND-LAND-RC-0923.md`、
`_dispatch\questions\LOCALFIND-LAND-RC-0923.md` 与协调线回执中；用户在自己的终端里手动
`routectl.py approve` 后解除。未使用任何绕过手段，未手改门禁状态文件。

## 7. 下一步（开发腿，进行中）

见任务包 §2：补单测（ServiceRunState）、全量 `testDebugUnitTest lintRelease assembleRelease bundleRelease`、
版本号核实、签名 AAB + 扩展 zip（版本对齐）+ CHANGELOG + `docs/release/1.1.0-readiness.md`、真机测试。
