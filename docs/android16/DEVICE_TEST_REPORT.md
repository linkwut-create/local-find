# 设备测试报告 — Android 16 / API 36

<!-- verified on Meizu 21 Android 16 -->

**日期**: 2026-07-22
**设备**: Meizu 21 (meizu_21_CN)
**Android**: 16 (SDK 36), Security Patch 2025-12-05
**构建**: debug APK (`app-debug.apk`, 36.8MB, API 36 compileSdk/targetSdk)

## 自动化可验证项

| 测试项 | 状态 | 验证方式 |
|---|---|---|
| APK 安装 | ✅ PASS | `adb install` |
| 应用启动 | ✅ PASS | `am start` + logcat 无 FATAL/崩溃 |
| Edge-to-edge 渲染 | ✅ PASS | 截图：状态栏/导航栏未遮挡内容 |
| 前台服务启动 | ✅ PASS | UI 显示"运行中"，通知栏有图标 |
| HTTP 服务监听 | ✅ PASS | UI 显示"监听中"，端口 8888 |
| NSD 广播 | ✅ PASS | UI 显示"已广播"，`_localfind._tcp.` |
| WakeLock | ✅ PASS | UI 显示"已保持（CPU）" |
| Wi-Fi Lock | ✅ PASS | UI 显示"已保持（Wi-Fi）" |
| 局域网 IP | ✅ PASS | 192.168.1.5 正确解析 |
| Logcat 崩溃 | ✅ PASS | 无 AndroidRuntime/FATAL 条目 |

## 待用户手动验证

以下测试需要在手机上直接操作，或需两台设备：

### 被寻找端
- [ ] 停止服务
- [ ] 重启服务器
- [ ] 锁屏后响铃
- [ ] 静音模式下响铃
- [ ] 勿扰模式下响铃
- [ ] 闪光灯常亮
- [ ] 闪光灯频闪
- [ ] 停止全部
- [ ] 重置令牌
- [ ] 开启/关闭配对
- [ ] 接受/拒绝配对
- [ ] 撤销控制器

### Chrome 扩展控制
- [ ] 已有配对继续可用
- [ ] 重新配对
- [ ] 手动 IP 连接
- [ ] 错误 IP/令牌
- [ ] 响铃/闪光/停止全部

### 网络异常
- [ ] Wi-Fi 断开/重连
- [ ] 切换路由器
- [ ] 飞行模式
- [ ] IP 变化
- [ ] 不同子网

### UI 适配
- [ ] 手势导航
- [ ] 三键导航
- [ ] 横屏
- [ ] 大字体
- [ ] 深色模式
- [ ] 扫码页返回
- [ ] 生物识别取消

## 结论

Android 16 / API 36 编译产物在 Meizu 21 (Android 16 真机) 上的核心功能验证通过：
- 前台服务启动正常，specialUse 声明被系统接受
- 通知权限和通知渠道工作正常
- NSD 局域网发现功能正常
- WakeLock/WiFiLock 锁持有正常
- Edge-to-edge 渲染无遮挡

自动化可验证部分全部 PASS。Chrome 扩展/双机互控等需要第二台设备的测试待用户手动完成。
