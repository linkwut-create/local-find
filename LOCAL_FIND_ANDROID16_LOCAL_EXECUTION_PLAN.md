# Local Find Android 16 / API 36 本地执行规划书

## 0. 使用方式

本规划书交给 Claude Code、OpenCode 或其他能够直接操作本机文件与终端的编程 Agent 执行。

**项目来源唯一以用户指定的本地文件夹为准。**

执行器必须先进入用户提供的 Local Find 本地项目目录，再读取、修改、构建和测试。不得把 GitHub、远程仓库、旧聊天记录或此前导出的文件当作当前项目状态的依据。

本规划书不要求执行器自行寻找项目路径。用户会在启动时直接进入正确目录，或明确提供本地路径。

---

# 一、任务目标

在不改变 Local Find 产品边界和核心协议的前提下，将 Android 应用升级到可面向 Android 16 / API 36 发布的状态。

最终目标：

```text
compileSdk = 36
targetSdk = 36
```

并完成：

1. 本地构建工具链兼容升级；
2. Android 16 相关代码兼容检查；
3. Debug APK 构建；
4. Release AAB 构建；
5. 单元测试、Lint 和基础静态检查；
6. 局域网、前台服务、扫码、通知和系统界面测试清单；
7. 更新记录和最终执行报告。

本次是兼容性维护，不是功能扩张。

---

# 二、唯一事实来源

## 2.1 项目事实来源

以下内容必须从本地项目文件中重新确认：

- 当前 `compileSdk`；
- 当前 `targetSdk`；
- 当前 `minSdk`；
- 当前 `versionCode`；
- 当前 `versionName`；
- Android Gradle Plugin 版本；
- Gradle Wrapper 版本；
- Kotlin 版本；
- Compose、CameraX、Ktor、AndroidX 等依赖版本；
- 本地 Git 分支和未提交修改；
- 当前构建是否成功；
- 当前测试是否成功；
- 本地签名配置是否存在。

即使规划书中提到某个历史版本，也不能直接采用，必须先检查本地文件。

## 2.2 不得作为项目状态依据的内容

不得以以下来源覆盖本地项目：

- GitHub 默认分支；
- GitHub Release；
- 远程分支；
- 此前聊天中的代码片段；
- 旧压缩包；
- 旧规划文档；
- 其他机器上的项目副本。

如本地项目与远程仓库不同，以本地项目为准，并在最终报告中记录差异，但不得自动同步或覆盖。

---

# 三、执行边界

## 3.1 允许执行

执行器可以：

- 读取整个本地项目；
- 搜索 Android、Gradle、Kotlin 和 Compose 相关文件；
- 执行本地 Git 只读命令；
- 在确认工作区安全后建立本地工作分支；
- 修改 Android 构建配置；
- 升级必要的 Gradle、AGP、Kotlin 或 AndroidX 依赖；
- 修改由 API 36 引起的兼容性代码；
- 执行 Gradle 构建、测试和 Lint；
- 执行 ADB 检查和兼容性测试命令；
- 生成本地测试报告；
- 创建本地提交。

## 3.2 禁止执行

未经用户明确授权，不得：

- `git pull`；
- `git fetch`；
- `git push`；
- 克隆或重新下载仓库；
- 以远程版本覆盖本地文件；
- 合并到主分支；
- 删除用户已有分支；
- 强制重置工作区；
- 自动执行 `git reset --hard`；
- 自动清理未跟踪文件；
- 修改 `applicationId`；
- 修改包名；
- 更换上传密钥或签名证书；
- 读取、打印、复制或上传密钥密码；
- 修改 Local Find 配对协议；
- 修改 Chrome 扩展通信协议；
- 增加云端服务、账户系统、定位追踪或互联网中继；
- 重构与 API 36 无关的模块；
- 发布到 Google Play；
- 声称未执行的真机测试已经通过。

## 3.3 云端边界

项目文件和构建日志以本地处理为主。

如执行器所使用的模型本身是云端模型，只允许发送完成当前任务所必需的最小上下文。以下内容不得进入模型上下文或外部服务：

- keystore；
- 密钥密码；
- `local.properties` 中的秘密值；
- 用户令牌；
- 配对令牌；
- 真实设备隐私信息；
- 与任务无关的本地文件。

---

# 四、阶段 0：进入本地项目并确认安全状态

## 4.1 确认当前目录

首先执行：

```powershell
Get-Location
git rev-parse --show-toplevel
git status --short
git branch --show-current
git log -1 --oneline
```

同时列出项目根目录中的主要文件：

```powershell
Get-ChildItem
```

确认至少存在 Android 工程目录或 Gradle 工程文件。

## 4.2 工作区处理规则

### 工作区干净

如 `git status --short` 无输出：

1. 记录当前分支和提交；
2. 检查是否已经存在本地 API 36 工作分支；
3. 如不存在，建立本地分支：

```powershell
git switch -c chore/android-16-api36-local
```

分支仅存在本地，不推送。

### 工作区存在未提交修改

不得自动 stash、提交、覆盖或删除。

必须：

1. 列出修改文件；
2. 判断这些修改是否与本次更新相关；
3. 停止执行；
4. 向用户报告；
5. 等待用户决定继续使用当前工作区、先提交，还是另建副本。

## 4.3 阶段输出

输出：

```text
项目本地路径：
当前分支：
当前提交：
工作区是否干净：
是否建立本地工作分支：
发现的主要模块：
下一阶段是否可以继续：
```

---

# 五、阶段 1：读取项目结构与当前配置

## 5.1 必查文件

根据实际项目结构查找并读取：

```text
settings.gradle
settings.gradle.kts
build.gradle
build.gradle.kts
gradle.properties
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.properties
app/build.gradle
app/build.gradle.kts
AndroidManifest.xml
proguard-rules.pro
local.properties
```

注意：

- `local.properties` 只能检查是否存在必要路径，不得输出秘密值；
- 签名配置只能检查变量名和是否完整，不得输出密码。

## 5.2 搜索关键代码

在 Android 源码中搜索：

```text
onBackPressed
OnBackPressedDispatcher
BackHandler
WindowCompat
setDecorFitsSystemWindows
enableEdgeToEdge
Scaffold
WindowInsets
statusBarsPadding
navigationBarsPadding
startForeground
startForegroundService
FOREGROUND_SERVICE
specialUse
POST_NOTIFICATIONS
NsdManager
NetworkServiceDiscovery
MulticastLock
WifiLock
WakeLock
ServerSocket
Socket
Ktor
Netty
usesCleartextTraffic
CAMERA
CameraX
BiometricPrompt
```

## 5.3 输出当前基线表

必须生成：

| 项目 | 本地当前值 | 文件位置 |
|---|---:|---|
| compileSdk |  |  |
| targetSdk |  |  |
| minSdk |  |  |
| versionCode |  |  |
| versionName |  |  |
| AGP |  |  |
| Gradle |  |  |
| Kotlin |  |  |
| Java |  |  |
| Compose |  |  |
| CameraX |  |  |
| Ktor |  |  |

## 5.4 阶段停止条件

如发现：

- 项目不是预期的 Local Find；
- Android 工程缺失；
- 构建脚本严重损坏；
- 本地文件与规划预期完全不同；
- 存在多个无法判断的 Android 应用模块；

则停止并向用户报告，不继续修改。

---

# 六、阶段 2：建立更新前构建基线

## 6.1 检查环境

执行：

```powershell
java -version
.\gradlew.bat --version
```

检查 Android SDK：

```powershell
$env:ANDROID_HOME
$env:ANDROID_SDK_ROOT
```

如本机安装了 `sdkmanager`：

```powershell
sdkmanager --list
```

确认：

- Java 版本；
- Gradle 能否启动；
- 当前 Android SDK 是否存在；
- API 36 SDK 是否已经安装；
- Build Tools 是否可用。

## 6.2 更新前构建

在修改任何文件之前执行：

```powershell
.\gradlew.bat clean
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
```

如项目路径不是当前目录，先进入实际 Android 工程目录。

## 6.3 记录结果

将结果写入：

```text
docs/android16/BASELINE_BUILD_REPORT.md
```

记录：

- 执行时间；
- 本地环境；
- 命令；
- 成功或失败；
- 原始错误摘要；
- 当前已存在的警告；
- 构建产物路径；
- 哪些错误在升级前已经存在。

## 6.4 基线失败规则

更新前构建失败时，不得直接把所有错误归因于 API 36。

先分类：

1. 环境缺失；
2. 依赖无法下载；
3. Gradle/Java 不兼容；
4. 原项目已有编译错误；
5. 测试已有失败；
6. 签名配置缺失；
7. 其他。

只修复继续升级所必需的基线问题。不得借机扩大重构范围。

---

# 七、阶段 3：确定最小升级组合

## 7.1 原则

优先采用最小变更集合：

1. 安装或确认 Android SDK 36；
2. 将 `compileSdk` 升到 36；
3. 将 `targetSdk` 升到 36；
4. 提高 `versionCode`；
5. 根据实际兼容要求升级 AGP 与 Gradle；
6. 只有出现明确问题时才升级 Kotlin、Compose 或其他依赖。

不得执行“全部依赖升级到最新版”。

## 7.2 版本决策顺序

依次判断：

```text
当前 AGP 是否支持 compileSdk 36
↓
如不支持，选择兼容 API 36 的稳定 AGP
↓
根据 AGP 选择对应 Gradle Wrapper
↓
检查当前 Kotlin 与新 AGP 是否兼容
↓
检查 Compose 编译器与 Kotlin 是否兼容
↓
只升级必要依赖
```

## 7.3 versionCode 规则

先读取本地 `versionCode`。

- 新 `versionCode` 必须大于本地当前值；
- 如用户本地保存有 Play 发布记录，需同时核对；
- 如无法确认 Play 上最新 `versionCode`，不得声称可以直接上传；
- 可以暂时设置一个本地候选值，但必须在发布前由用户确认。

`versionName` 建议采用兼容性维护版本，例如：

```text
1.1.0
```

但最终以本地项目已有版本策略为准。

## 7.4 阶段输出

在修改前先输出拟修改表：

| 文件 | 当前值 | 拟修改值 | 原因 |
|---|---|---|---|
|  |  |  |  |

完成此表后再开始修改。

---

# 八、阶段 4：实施构建配置升级

## 8.1 首轮修改

首轮只修改必要构建文件，包括可能的：

```text
app/build.gradle.kts
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.properties
gradle.properties
```

目标：

```text
compileSdk = 36
targetSdk = 36
versionCode > 当前值
```

## 8.2 每次修改后的验证

每次只改一个逻辑组，然后执行：

```powershell
.\gradlew.bat help
.\gradlew.bat assembleDebug
```

不得累计大量修改后才首次构建。

## 8.3 依赖升级控制

只有在错误明确指向版本不兼容时，才升级：

- Android Gradle Plugin；
- Gradle Wrapper；
- Kotlin；
- Compose Compiler 或 Compose BOM；
- AndroidX Core；
- Activity Compose；
- Lifecycle；
- CameraX；
- Ktor；
- Coroutines。

每次升级必须记录：

```text
旧版本：
新版本：
触发原因：
解决的具体错误：
是否引入新警告：
```

## 8.4 禁止顺手修改

不得在此阶段：

- 改 UI 文案；
- 重构服务类；
- 重命名包；
- 变更端口；
- 变更令牌格式；
- 重写网络协议；
- 删除旧功能；
- 改 Chrome 扩展。

---

# 九、阶段 5：Android 16 兼容性审查与必要修复

只有在 API 36 配置能够编译后，才进入本阶段。

## 9.1 Edge-to-edge 与系统栏

检查 Compose 页面：

- 顶部栏是否被状态栏遮挡；
- 底部按钮是否被导航栏遮挡；
- `Scaffold` 是否正确使用 `innerPadding`；
- 二维码和扫码界面是否处理系统栏；
- 手势导航和三键导航是否都可用；
- 横屏和大字体是否仍可操作。

原则：

- 优先使用 Compose 官方 Insets 机制；
- 不添加重复 Padding；
- 不为了视觉效果大规模重写 UI；
- 仅修复 API 36 下的实际遮挡问题。

## 9.2 Predictive Back

搜索旧返回处理。

检查：

- 主 Activity 返回；
- 对话框关闭；
- 扫码界面退出；
- 生物识别取消；
- 下拉菜单关闭；
- 返回桌面时前台服务是否保持。

如未使用自定义返回逻辑，先测试，不要无理由新增复杂返回框架。

## 9.3 前台服务

检查：

- `startForegroundService()` 后是否及时调用 `startForeground()`；
- `foregroundServiceType` 声明；
- `specialUse` 用途说明；
- 通知渠道；
- 通知权限拒绝后的行为；
- 服务停止时是否释放全部资源；
- 锁屏和后台状态下服务是否持续；
- Android 16 是否产生新的运行异常或 Lint 警告。

不得试图绕过系统后台限制。

## 9.4 局域网与 NSD

这是本项目最高风险项。

检查：

- Ktor/Netty 本地 HTTP Server；
- 监听地址；
- NSD 注册；
- NSD 发现；
- TCP 连接；
- Wi-Fi 切换；
- 手机 IP 变化；
- MulticastLock；
- WifiLock；
- 局域网权限或系统限制；
- 浏览器扩展连接；
- 手机控制手机。

必须区分：

```text
编译通过
模拟器通过
真机通过
尚未验证
```

不得把其中一种替代另一种。

## 9.5 通知权限

测试和检查：

- 首次授权；
- 拒绝授权；
- 永久拒绝；
- 从系统设置撤销；
- 通知关闭时启动前台服务；
- UI 是否给出准确提示。

## 9.6 CameraX 与二维码

检查：

- 相机权限；
- 扫码成功；
- 用户取消；
- 返回后相机释放；
- 横屏；
- 前后台切换；
- 设备无相机或相机被占用；
- CameraX 是否因 API 36 需要升级。

除非出现明确兼容问题，不升级整个图像依赖栈。

## 9.7 生物识别

检查：

- 指纹成功；
- 用户取消；
- 未录入生物识别；
- 仅设备凭据；
- 多次失败；
- 无生物识别硬件；
- 横屏和返回手势。

---

# 十、阶段 6：自动化构建与静态验证

完成必要修复后执行：

```powershell
.\gradlew.bat clean
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
.\gradlew.bat bundleRelease
```

如项目存在其他测试任务，也应先列出：

```powershell
.\gradlew.bat tasks
```

再执行与 Android 应用相关的测试。

## 10.1 结果分类

结果必须分为：

```text
PASS
FAIL
BLOCKED
NOT RUN
```

例如：

| 检查项 | 状态 | 证据 |
|---|---|---|
| Unit tests | PASS | Gradle 输出 |
| Lint | PASS / FAIL | 报告路径 |
| Debug APK | PASS | APK 路径 |
| Release AAB | PASS / BLOCKED | AAB 路径或签名问题 |
| Android 16 emulator | NOT RUN | 本机未安装模拟器 |
| Physical device | NOT RUN | 需要用户执行 |

## 10.2 不得伪造结果

- 没有 Android 16 模拟器时，写 `NOT RUN`；
- 没有真机时，写 `NOT RUN`；
- 缺少签名环境时，写 `BLOCKED`；
- 仅编译成功不能写“兼容性全部通过”。

---

# 十一、阶段 7：本地设备测试清单

本阶段可以由执行器辅助，但通常需要用户操作真机。

## 11.1 被寻找端

测试：

```text
启动服务
停止服务
重启服务器
锁屏后响铃
静音模式下响铃
勿扰模式下响铃
闪光灯常亮
闪光灯频闪
停止全部
重置令牌
开启配对
关闭配对
接受配对
拒绝配对
撤销控制器
```

## 11.2 Chrome 扩展控制

测试：

```text
已有配对继续可用
重新配对
手动 IP 连接
错误 IP
错误令牌
撤销令牌
手机离线
手机 IP 变化
响铃
闪光
停止全部
多设备切换
```

## 11.3 手机控制手机

测试：

```text
手机 A 发现手机 B
手机 A 与手机 B 配对
手机 A 控制手机 B
手机 B 锁屏
手机 B 静音
手机 B 切换 Wi-Fi
手机 B 重启服务
```

## 11.4 网络异常

测试：

```text
Wi-Fi 断开
Wi-Fi 重连
切换路由器
飞行模式
路由器重启
IP 变化
不同子网
公共 Wi-Fi 客户端隔离
IPv4 / IPv6 双栈
```

## 11.5 UI

测试：

```text
Android 16
Android 15 或更低版本
手势导航
三键导航
横屏
大字体
深色模式
扫码页返回
生物识别取消
```

测试结果写入：

```text
docs/android16/DEVICE_TEST_REPORT.md
```

---

# 十二、阶段 8：代码审查与变更控制

## 12.1 Git Diff 审查

执行：

```powershell
git status --short
git diff --stat
git diff
```

逐文件检查：

- 是否只有必要修改；
- 是否误改包名；
- 是否误改端口；
- 是否误改协议；
- 是否提交秘密；
- 是否出现自动格式化导致的大面积无关变化；
- 是否删除用户代码；
- 是否修改 Chrome 扩展；
- 是否引入未使用依赖。

## 12.2 本地模型审查

如用户本地模型工作流可用，建议：

1. `qwen3-coder-next-q8`：实施与普通构建修复；
2. `qwen3.5-35b-q8`：审查 diff；
3. `mistral-medium-3.5-128b`：仅在局域网、前台服务或复杂兼容问题上复核。

审查建议必须分类：

```text
接受
拒绝
暂缓
需要真机验证
```

模型建议不是事实，Gradle、ADB 和真机结果优先。

## 12.3 提交策略

建议最多三个本地提交：

```text
build: update Android toolchain for API 36
fix: address Android 16 compatibility issues
docs: record Android 16 validation results
```

不得自动合并主分支，不得推送远程。

---

# 十三、阶段 9：最终交付物

完成后必须生成：

```text
docs/android16/BASELINE_BUILD_REPORT.md
docs/android16/API36_CHANGELOG.md
docs/android16/AUTOMATED_TEST_REPORT.md
docs/android16/DEVICE_TEST_REPORT.md
docs/android16/FINAL_CLOSEOUT.md
```

## 13.1 FINAL_CLOSEOUT 必须包含

```text
1. 本地项目路径
2. 起始分支与起始提交
3. 最终本地分支与提交
4. 修改文件列表
5. 版本变更表
6. 构建工具链变更
7. 代码兼容性修复
8. 实际执行命令
9. 自动测试结果
10. 模拟器测试结果
11. 真机测试结果
12. 尚未验证事项
13. 签名与 AAB 状态
14. 是否使用云端模型
15. 是否访问远程仓库
16. 是否执行任何 push、merge 或发布
17. 后续由用户完成的步骤
```

---

# 十四、完成标准

只有以下条件满足后，才能标记“本地实施完成”：

```text
[ ] 本地项目身份已确认
[ ] 未以远程仓库覆盖本地项目
[ ] 更新前基线已记录
[ ] compileSdk = 36
[ ] targetSdk = 36
[ ] versionCode 已提高
[ ] Gradle 工具链兼容
[ ] Debug APK 构建成功
[ ] Release AAB 构建成功，或明确记录签名阻塞
[ ] 单元测试完成
[ ] Lint 完成
[ ] Git diff 已审查
[ ] 未修改 applicationId
[ ] 未修改配对协议
[ ] 未修改 Chrome 扩展协议
[ ] 未提交秘密
[ ] 自动测试与人工测试严格区分
[ ] 未合并主分支
[ ] 未推送远程
[ ] 未发布 Google Play
[ ] 最终报告已生成
```

“发布准备完成”还需要：

```text
[ ] 用户确认 Play 上最新 versionCode
[ ] Android 16 真机或模拟器测试
[ ] 至少一台真实 Android 手机测试
[ ] Chrome 扩展控制测试
[ ] 手机控制手机测试
[ ] 锁屏与静音响铃测试
[ ] Google Play 内部测试
[ ] Play 预发布报告检查
```

---

# 十五、强制停止条件

出现以下任一情况，执行器必须停止并报告：

1. 本地工作区有无法判断的未提交修改；
2. 项目目录不是预期项目；
3. 需要更换 applicationId；
4. 需要更换签名密钥；
5. 无法确认 versionCode；
6. API 36 升级需要大规模业务重构；
7. 局域网协议必须改变；
8. Chrome 扩展必须同步破坏性升级；
9. 发现秘密已进入版本控制；
10. 需要删除用户文件；
11. 需要强制重置 Git；
12. 需要访问远程仓库才能继续；
13. 构建错误与本次升级无关且范围较大；
14. 自动测试与真机表现冲突；
15. 无法证明修改没有破坏核心寻找功能。

---

# 十六、给执行器的启动指令

将以下内容连同本规划书交给 Claude Code 或其他本地执行器：

```text
你现在位于用户指定的 Local Find 本地项目目录。

严格按照《Local Find Android 16 / API 36 本地执行规划书》执行。

项目当前状态只能从本地文件和本地 Git 工作区读取。不要使用 GitHub 或任何远程仓库作为项目依据，不要 fetch、pull、push、clone、merge 或发布。

先执行阶段 0、阶段 1 和阶段 2：
1. 确认本地目录、Git 状态和项目结构；
2. 读取本地构建配置；
3. 建立更新前构建基线；
4. 生成基线报告；
5. 输出拟修改表。

在完成拟修改表之前，不要修改任何代码。

如工作区不干净、项目身份不明、签名或 versionCode 无法确认，立即停止并报告，不要自行处理。
```
