# Local Find Canary 与 APK 联调计划

## 目标

复用已验证的路径处理方式，让 Chrome Canary 能加载当前仓库的 MV3 扩展，并安装包含后台/锁屏引导的 Android APK，完成真实 `/ping` 与控制链路验证。

## 当前证据

- 历史启动脚本引用的 `D:\local-find\chrome-extension` 已不存在，脚本已改为从当前仓库定位扩展。
- 当前 Google Chrome Canary 155.0.8056.0 会拒绝 `--load-extension` 和 `--disable-extensions-except`；旧启动参数不能作为验收路径。
- 通过 `--remote-debugging-pipe`、`--enable-unsafe-extension-debugging` 和 `Extensions.loadUnpacked`，Zero12 远程电脑已成功加载当前扩展。
- Canary popup 实测已读到手机 `/device-info`：设备 `MEIZU 21`，服务 `running`；未配对时按设计显示“请先在手机端开启配对模式”。
- Android 当前已安装包的亮屏后台与锁屏网络证据均通过：锁屏时 `mInputRestricted=true`、`/ping=200`，恢复后服务仍为 `running`。
- 当前源码 Debug/Release APK 已生成；用户已明确允许卸载重装，手机已卸载 Play 签名包并安装本地 Release APK（`versionCode=3`, `versionName=1.1.0`）。这清除了旧包的本地配对数据，随后已重新完成一次配对并保存控制端记录。
- 本轮新发现：配对令牌没有按天过期，但旧源码没有持久化“服务应保持运行”的意图，也没有 boot/update 恢复接收器；服务被停止后，配对仍存在但端口不可达。
- 本轮锁屏失败的直接证据是魅族系统在约 43 秒后记录 `am_freeze`；当时前台服务仍显示 `isForeground=true`，但进程被冻结后 8888 无法响应。为当前测试机加入 `dumpsys deviceidle whitelist` 后，锁屏 0/5/10/20/30/40 秒均保持 `/ping=200` 且 `isFrozen=0`。
- 最终本地 Release APK 已安装；锁屏状态下更新包后服务自动恢复（`isForeground=true`、`startRequested=true`、`/ping=200`），配对记录仍保留。
- Zero12 Chrome Canary 重新配对后，在手机锁屏并等待 45 秒后，真实 `stop-all` 命令成功；再次将已保存地址故意改为旧 `.225`，扩展按持久设备 ID 找回当前 `.226` 并成功执行命令。
- 强制结束进程后，用已保存的运行意图恢复服务，再退到后台并锁屏，8888 监听与远程 `stop-all` 均成功；这是诊断证据，不是用户日常操作要求。

## 实施与验收

1. 固化脚本的当前仓库路径、Canary 可执行文件和 CDP 加载流程。
2. 通过 CDP 打开扩展 popup，确认页面加载和 `/device-info` 请求可达。
3. 使用现有成功的 Gradle 发行版与可用依赖缓存编译 Android APK。
4. 在亮屏后台、锁屏和 Canary 控制端分别取得可复现证据。
5. 手机解锁后开启配对模式，完成请求、接受、保存 control token，再验证响铃/闪光和停止。
6. 修复服务运行意图、boot/update/task 生命周期和配对设备私有 `/24` 地址恢复；安装后重新验证亮屏后台、锁屏、服务停止后恢复和 Canary 控制。

## 边界

- 不提交、不发布、不修改账号或令牌。
- 不清理已有工作、报告或构建缓存。
- 新 APK 的设备证据已经在获得明确卸载授权并重新安装后单独记录；旧包的后台/锁屏证据不能冒充新 APK 证据。
- 地址扫描是同一私有 `/24` 的 best-effort 恢复，不对 Android 强制停止、OEM 后台冻结、网络隔离或跨网络可达性作保证。
- 本轮没有真实等待 24 小时；“隔天”只用锁屏超过 OEM 冻结阈值、更新后恢复和旧 DHCP 地址恢复作代理验证。魅族等设备仍必须完成系统电池/自启动/应用冻结设置。
