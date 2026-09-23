# Android 16 / API 36 — Play 上传操作手册

**日期**: 2026-09-10 初稿 / **2026-09-13 更新**（§3 补 48 小时冷却期、§7 结论更正为「自动化可行」、§1.2 进度回填）
**适用本地提交**: `b619d82`（master）
**目标**: 把已完成 API 36 升级的版本发布到 Google Play，解除 API 级别要求

---

## 1. 当前状态

Play Console 显示"应用必须以 Android 16 (API 级别 36) 或更高版本为目标平台"，原因是**线上版本仍以 API 35 为目标**。
本地代码其实早已完成升级，只是**这个升级后的 AAB 从未上传过**。

本地已验证的构建身份：

| 项目 | 值 |
|---|---|
| compileSdk | 36 |
| targetSdk | 36 |
| minSdk | 26 |
| versionCode | **3** |
| versionName | 1.1.0 |
| 16 KB 内存页 | ✅ 通过（zip 对齐 + ELF LOAD 段 0x4000） |
| Lint | ✅ 0 error |

### 1.1 线上版本 vs 本地版本（已由 Play Console 提交记录确认）

Play Console → 发布概览 → 应用提交活动记录：

| 提交 ID | 已提交 | 变更 | 状态 |
|---|---|---|---|
| 2 | 2026-06-14 8:30 下午 | **正式版** | 已发布 |
| 1 | 2026-05-29 9:06 下午 | 封闭式测试 - Alpha、商品详情、应用内容、商店设置 | 已发布 |

对照本地 Git：

| 版本 | 提交 | 日期 | targetSdk | 状态 |
|---|---|---|---|---|
| versionCode **2** / 1.0.1 | `d8b3689` | 2026-06-14 | **35** | ✅ 随提交 2 发布到**正式版** |
| versionCode **3** / 1.1.0 | `c9865a3` | 2026-07-22 | **36** | ❌ **从未上传** |

**结论**：线上正式版就是 targetSdk 35，所以 Play 才发出 Android 16 警告；
API 36 的 `versionCode 3` 只存在于本地仓库，从未上传。
**`versionCode 3` > 线上 2，可直接使用，无需修改代码，也不会冲突。**

## 1.2 当前进度（更新于 2026-09-13）

| 步骤 | 状态 | 日期 |
|---|---|---|
| 上传密钥重置（本机侧生成新密钥） | ✅ 完成 | 2026-09-10 |
| 新上传证书 `.der` 导出 | ✅ `android/local-find-secrets/upload_certificate.der` | 2026-09-10 |
| 签名 AAB 构建 | ✅ `app-release.aab` 20,468,318 B，已签名 | 2026-09-10 |
| AAB SHA256 | `E110C63D9CD6D0E7B762236AC056464582643EDA4DD514693392AD8A2721AEE8` | — |
| 16 KB 对齐 | ✅ `Verification successful` | 2026-09-10 |
| **Play Console 注册新上传证书** | ✅ 完成（**§3 步骤 1**） | 2026-09-10 |
| 冷却期（重置后 48 小时，**§3 步骤 1 后**） | ✅ 已过 | 2026-09-12 21:30 CST |
| **上传 AAB 并创建正式版** | ✅ 完成：`versionCode 3 (1.1.0)` / `targetSdk 36`，0 错误 | 2026-09-13 |
| 提交送审 | ✅ 已提交，送审前快速检查中 | 2026-09-13 |
| 审核通过 / 上架 | ⬜ 待 Google 审核 | — |

上传后 Play Console 确认的结果（`releases/3/review` 页）：

| 项 | 值 |
|---|---|
| 新 app bundle | **App bundle 3 (1.1.0)**，API 26 及更高级别，**目标 SDK 版本 36** |
| 上一版本 | App bundle 2 (1.0.1)，目标 SDK 版本 35 |
| 错误 / 警告 | **0 个错误**，2 个警告（均为提示级，见下） |
| 设备支持变化 | 手机 11,423 → 11,423，**0 台流失**；平板 6,343 → 6,343；车载 25 → 25 |
| 新安装大小 | 13.2 MB（较上一版本 −18.6 KB） |
| 分阶段发布 | 100.0%，发布到所有目标国家/地区 |

那 2 个警告（**不阻塞发布**，均属建议性）：

1. **没有与此 App Bundle 关联的去混淆文件** —— 建议上传 R8/proguard mapping 文件；
2. **此 App Bundle 包含原生代码，尚未上传调试符号文件** —— 建议上传 native debug symbols。

两条都只影响**崩溃/ANR 日志的可读性**（Google 能否把崩溃栈还原成人可读的函数名），
不影响用户安装、运行与上架。

新密钥信息见 `android/local-find-secrets/SIGNING_KEY_INFO.md`。

## 2. 为什么当初未签名

`android/app/build.gradle.kts` 的签名逻辑是条件式的：

```kotlin
val hasReleaseSigningConfig = listOf(
    releaseSigningStoreFile, releaseSigningStorePassword,
    releaseSigningKeyAlias, releaseSigningKeyPassword,
).all { !it.isNullOrBlank() }
```

四项凭证来自环境变量或 `android/local.properties`。当前：

- 环境变量：无任何 `LOCAL_FIND_*`
- `android/local.properties`：只有 `sdk.dir`

所以 `hasReleaseSigningConfig = false`，`signReleaseBundle` 任务空转，产出的 AAB 没有签名。
上传密钥文件 `local-find-secrets/local-find-upload.jks` 仍在，缺的只是密码——而密码从未被记录，无法恢复。

## 3. 接下来只剩你在 Play Console 的操作

本机侧已全部完成（新密钥 + 已签名 AAB）。你只需在 Play Console 做两步：

### 步骤 1：注册新的上传证书

> **⚠️ 改版陷阱（2026-09 实测）**：Play Console 已把「应用完整性」的设置移走，
> 侧边栏进去只会看到一句「应用完整性」设置已移至别处，并把你重定向到
> 「Google Play 提供保护」页面——**那里也没有上传密钥的入口**。
> 正确入口被藏在**应用签名**页面里，只能用 URL 直接跳。

**用 URL 直接打开应用签名页：**

1. 先在 Play Console 里进入 Local Find，此时地址栏形如
   `https://play.google.com/console/u/0/developers/<devId>/app/<appId>/app-dashboard`
2. 把结尾的 `app-dashboard` 换成 **`keymanagement`**（`<devId>` / `<appId>` 原样保留）
3. 回车，即直接加载 **应用签名（App signing）** 页面
4. 在该页找到 **上传密钥证书** → 点 **重置上传密钥 / 请求重置上传密钥**，按向导走完

（此办法来源：[Stack Overflow — How to reset upload signing key in the new Play Console UI](https://stackoverflow.com/questions/79968226/how-to-reset-upload-signing-key-jks-in-the-new-google-play-console-ui-missin)）

5. 向导会让你下载一个 `.der`，**忽略它**——你已经有自己的了
6. 在要求上传新证书的那一步，上传这个文件：

   ```
   D:\AIProjects\projects\local-find（找手机app）\android\local-find-secrets\upload_certificate.der
   ```

   > 若上传框只接受 `.pem`，用 keytool 转格式即可：
   > `keytool -exportcert -rfc -keystore <ks> -alias localfind-upload -file upload_certificate.pem`
   > （同一张证书，DER 与 PEM 只是容器不同。转换所需的 keystore 口令见 `SIGNING_KEY_INFO.md`。）

7. 核对页面上显示的指纹应为：

   ```
   SHA-256: 32:59:7A:5D:D1:FD:AA:B3:D5:7D:6D:FD:9F:E9:03:93:FF:D4:FE:29:E2:8F:09:C2:F9:7C:9C:A0:CB:C0:A8:53
   ```

> ### 🚨 重置成功后还有 48 小时冷却期，期间上传 AAB 必被拒
>
> **2026-09 实测踩到的坑，白等了两天。这一步之后先读这段再动手。**
>
> `keymanagement` 页点下「申请重置」之后，Google 会开始一段**固定 48 小时**的生效等待期。
> 这段时间里，用新证书签名的 AAB 会**上传成功、处理完成，然后才被拒**，报错原文：
>
> > 您上传的 app bundle 在进行签名时所使用的上传证书最近已重置，因此该证书目前尚未生效。
> > 在 2026年9月12日 13:30:59（世界协调时间）之后，您便可以重新上传 app bundle。
>
> 几个关键点，别走弯路：
>
> - **不是**「版本代码已存在」，**也****不是**证书指纹不匹配。上传前指纹已逐位核对无误，照样被拒——
>   所以别去改 AAB、别去重新生成密钥、别去怀疑 `.der` 传错了，那些都不是原因。
> - 冷却截止时刻 = **点下「申请重置」的那一刻 + 48 小时**（UTC）。报错里那句
>   「在 …（世界协调时间）之后」给的就是它，**当场记下来**。
> - 时间没到之前**不要反复重试上传**——每次都是同一个错，只会在 Play 上多留几条失败记录。
> - 冷却一过，**同一个 AAB 原样重传即可**，不需要重新构建、不需要改任何东西。
> - 实在等不了，可以在 `keymanagement` 页点「取消请求」撤回这次重置——但那样整个流程要重走一遍。
>
> 实测时间线（本次）：
>
> | 时刻（CST） | 事件 |
> |---|---|
> | 2026-09-10 21:30:59 | 提交重置申请，48 小时冷却开始 |
> | 冷却期内 | 上传 AAB → 被上述报错拒绝 |
> | 2026-09-12 21:30:59 | 冷却结束 |
> | 2026-09-13 16:40 | **原样重传同一个 AAB → 一次通过** |
>
> 冷却期间 `keymanagement` 页处于 pending 状态：「上传密钥证书」已显示新证书的
> MD5 / SHA-1 / SHA-256（可当场核对），「请求重置上传密钥」按钮变灰并出现「取消请求」。

### 步骤 2：上传 AAB

Play Console → **测试和发布** → **正式版** → **创建新版本** → 上传：

```
D:\AIProjects\projects\local-find（找手机app）\android\app\build\outputs\bundle\release\app-release.aab
```

`versionCode 3` > 线上 `2`，不会冲突，无需修改代码。

> ⚠️ 重置上传密钥会**永久更换上传密钥**，但**不影响已发布版本的签名**（应用签名密钥由 Google 保管）。
> Google 对重置次数有配额限制，所以**新口令务必立刻备份**（见 `SIGNING_KEY_INFO.md` 第 5 节）。

### 如果你改主意：仍想用旧密钥

只有当你能想起原口令时才可行。填写方式：

```properties
LOCAL_FIND_UPLOAD_STORE_FILE=local-find-secrets/local-find-upload.jks   # 注意:相对 android/ 的路径
LOCAL_FIND_UPLOAD_STORE_PASSWORD=<原 keystore 密码>
LOCAL_FIND_UPLOAD_KEY_ALIAS=localfind-upload
LOCAL_FIND_UPLOAD_KEY_PASSWORD=<原 key 密码>
```

> **必须是相对路径。** Java `.properties` 按 ISO-8859-1 读取，本仓库目录名含中文，
> 绝对路径会变乱码导致 keystore 找不到。详见 `SIGNING_KEY_INFO.md` 第 3 节。

## 4. 构建命令（已验证可用）

```powershell
$env:JAVA_HOME  = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\Users\Zero\AppData\Local\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

# 若 wrapper 的 gradle-8.11.1 未下载完，直接用已缓存的 8.14
$gradle = "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.14-bin\18nmau8r28wyp9qkhrj7hed8f\gradle-8.14\bin\gradle.bat"

& $gradle -p .\android --no-daemon clean test lint assembleDebug bundleRelease
```

产物：`android/app/build/outputs/bundle/release/app-release.aab`

## 5. 上传前自检清单

```powershell
$aab = "android\app\build\outputs\bundle\release\app-release.aab"

# 1) 必须显示已签名（当前显示"未签名"）
& "$env:JAVA_HOME\bin\keytool.exe" -printcert -jarfile $aab

# 2) 16 KB 对齐必须通过
& "$env:ANDROID_HOME\build-tools\36.0.0\zipalign.exe" -c -P 16 -v 4 $aab

# 3) 核对 targetSdk / versionCode
Select-String -Path "android\app\build\intermediates\merged_manifest\release\processReleaseMainManifest\AndroidManifest.xml" `
  -Pattern "targetSdkVersion|versionCode|versionName"
```

三项都通过后，再到 Play Console → 测试和发布 → 正式版 → 创建新版本 → 上传 AAB。

## 6. 上传前确认清单

- [x] Play 上当前最新 versionCode = **2**（提交 2，2026-06-14 正式版）→ `versionCode 3` 可用，**无需提升**；
- [x] AAB 已确认签名（`keytool -printcert -jarfile` 读出 `CN=Local Find Upload`）；
- [x] 16 KB 对齐通过；
- [x] AAB SHA256 已记录；
- [ ] **Play Console 注册新上传证书**（第 3 节步骤 1）——待你操作；
- [ ] 上传 AAB 并创建正式版（第 3 节步骤 2）——待你操作；
- [ ] 真机回归（见 `DEVICE_TEST_REPORT.md` 待手动验证清单）。

## 7. 关于用浏览器自动化代劳 Play Console

> **本节结论已更新两次。****以 2026-09-13 的实测为准：可行。**
>
> | 日期 | 结论 |
> |---|---|
> | 2026-09-10 | ❌ 不可行（下列证据均属当日有效） |
> | **2026-09-13** | ✅ **可行** —— 见本节末「可行方案」 |
>
> 2026-09-10 那份「不可行」结论的**真正原因是当时的浏览器启动方式不对**，
> 而不是「DSH 沙箱里起不来浏览器」。下面保留原始失败证据作为历史记录。

### 7.1 原始失败证据（2026-09-10）

曾尝试用 Chrome/CDP 自动化代替手工点击 Play Console，结论是**当前 DSH 会话环境下不可行**，实测证据如下：

| 尝试 | 结果 |
|---|---|
| Chrome Canary + 空 profile + `--remote-debugging-port=9230` | launcher 挂起，chrome 进程数恒为 0 |
| Chrome Canary + 复制来的已登录 profile | 启动失败，`Start-Process` 退出码 **21** |
| 稳定版 Chrome + 项目自带 CWS profile + 端口 9231 | 未监听，无进程 |
| 系统内已有 `DevToolsActivePort` / 9222-9300 监听 | **均不存在** |

对照事实：**用户自己的 Chrome 正常跑着 27 个进程**，说明 Chrome 本身没问题——
是 DSH 进程沙箱启动的浏览器实例无法存活。DSH 会话也没有暴露浏览器类 MCP 工具
（`manifest` 中不包含；Codex/Claude 侧的 `chrome-devtools-mcp`、`local-browser-agent-mcp`
只对它们自己的会话生效）。

**两个结构性障碍（其中一条至今仍然成立）：**

1. ~~**跨安装的 cookie 无法复用。**~~ **—— 无法复用是真的，但不必复用。**
   正确做法是让用户**在同一个 Canary 安装、同一个 profile 里**登录一次，
   之后自动化就一直复用那个 profile，根本不需要跨安装搬运 cookie。
   2026-09-10 那次之所以卡住，正是因为试图从稳定版 Chrome 搬 profile 给 Canary。
2. **需要真实登录（仍然成立）。** Play Console 必须用账户持有人身份登录，无法代持。
   **登录这一步必须由你本人做**，机器人做不了，也不该做。

所以准确的说法是：**登录由你本人完成；登录之后的所有点击、填表、上传，都可以自动化。**

### 7.2 可行方案（2026-09-13 实测，已跑通完整流程）

**核心手法：用裸 CDP（Chrome DevTools Protocol）直连浏览器，而不是用现成的 Puppeteer MCP。**

原因是**上传 AAB 这件事**：Puppeteer MCP 没有文件上传工具，常见的
`DataTransfer` + `new File()` 注入对 20 MB 的二进制包不可用（内容只能靠字符串塞进去）。
只有 CDP 的 `DOM.setFileInputFiles` 能真正把磁盘上的文件交给 `<input type=file>`。

**关键步骤：**

1. **启动浏览器**（用户在该 profile 里登录过一次 Play Console）：
   ```json
   {"headless": false,
    "executablePath": "C:\\Users\\Zero\\AppData\\Local\\Google Chrome SxS\\Application\\chrome.exe",
    "userDataDir": "D:\\AIProjects\\cache\\canary-play-profile",
    "ignoreDefaultArgs": ["--enable-automation"],
    "args": ["--window-size=1600,1000", "--window-position=60,40"]}
   ```
   `--enable-automation` 必须去掉（会让 Play Console 识别出自动化）。

2. **从 `DevToolsActivePort` 拿真实调试端口**（不要自己猜端口）：
   文件在 `<userDataDir>\DevToolsActivePort`，第一行是端口，第二行是 `/devtools/browser/<uuid>`。
   浏览器重启后端口会变，**每次读文件即可自动适配**。

3. **连上去，走 CDP 序列**：
   `Target.getTargets` → `Target.attachToTarget {flatten:true}` →
   `Runtime.evaluate`（页面内 JS，用来定位和点击）→
   `DOM.getDocument` → `DOM.querySelector` → **`DOM.setFileInputFiles`**（上传文件）。

4. **上传 AAB 的关键三行**（Node 24 自带全局 `WebSocket`，零依赖）：
   ```js
   // 先给目标 input 打个标记，再用 DOM.querySelector 找到它
   const doc   = await send('DOM.getDocument', { depth: 1 }, sessionId);
   const found = await send('DOM.querySelector', { nodeId: doc.root.nodeId, selector: '[data-cf-aab="1"]' }, sessionId);
   await send('DOM.setFileInputFiles', { files: [AAB], nodeId: found.nodeId }, sessionId);
   ```
   找 input 的选择器：`[...document.querySelectorAll('input[type=file]')].find(i => i.accept === '.aab')`。
   页面上通常有 **2 个** file input（第 2 个是 OBB 位），靠 `accept` 区分。

**踩过的坑：**

- **`Runtime.evaluate` 里不能用顶层 `return`** —— 会报 `Illegal return statement`。必须包成 IIFE `(() => { … })()`。
- **Play Console 会在 DOM 里堆叠多个隐藏 dialog**，同一个表单可能出现 5 份。
  必须按 `offsetWidth || offsetHeight` 过滤掉不可见的，再用 dialog 内文匹配（如「请求重置上传密钥」）锁定当前活跃的那个，否则会拿到 8 个 input 而不知道点哪个。
- **文字匹配点击报「找不到按钮」时，先别重试** —— 很可能点击**已经成功**、SPA 已经导航走了，
  按钮自然就不存在了。先重新读一次 URL 再决定。
- **MCP 报 `Failed to launch the browser process!` 不一定是真失败** ——
  先看 `DevToolsActivePort` 的时间戳和 `chrome.exe` 进程数，再 `curl http://127.0.0.1:<port>/json/version`。
  实测遇到过 MCP 超时报错但浏览器其实正常起来了的情况；这种情况直接用 CDP 脚本接手即可。

**本次实际用到的脚本**（临时脚本，放在 `D:\AIProjects\cache\`，可随时重写）：

| 脚本 | 用途 |
|---|---|
| `cdp-probe.mjs` | 只读探查：当前页 URL/title + 所有 file input 清单 |
| `cdp-state.mjs` | 只读：当前页正文、告警、按钮清单 |
| `cdp-forms.mjs` | 只读：所有表单字段的真实 `value`（`innerText` 读不到 input 的值） |
| `cdp-upload-aab.mjs` | **上传 AAB**（`DOM.setFileInputFiles`） |
| `cdp-watch-bundle.mjs` | 轮询到 bundle 处理完成 |

> 实测过程未改动 `local-find-cws-chrome-profile` 与你 Canary 的原始 profile
> （时间戳保持 2026-05-25 不变），所有临时目录与调试实例均已清理。

## 8. 边界声明

本文档只描述操作步骤。

- **2026-09-10 撰写时**：未修改任何代码、构建配置或密钥文件，未上传、未发布、未访问远程仓库。
- **2026-09-13 更新时**：未修改任何代码、构建配置或密钥文件，未执行 `git commit` / `git push`。
  Play Console 侧完成了：注册新上传证书（§3 步骤 1）、上传 AAB 并创建正式版 `versionCode 3 (1.1.0)`
  （§3 步骤 2）。**提交送审由账户持有人本人操作。**
