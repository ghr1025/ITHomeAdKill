# ITHomeAdKill 开发与设备验收

## Git

保留上游历史，使用独立分支开发，通过 `git remote -v` 核对上游。APK、反编译材料、私有计划和设备截图不提交。不要将任何 IT之家 APK 放入本仓库或 PR。

## Mac 构建

要求 JDK 21、Android SDK Platform 37.2、可用的 Android build-tools。`minSdk=26` 与 `targetSdk=34` 保持不变；compile SDK 高于目标系统不意味着要求 Android 37 运行。

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew testDebugUnitTest lintDebug assembleDebug
```

如果使用 Homebrew 的 keg-only JDK21，且未向系统注册，单次构建可用：

```sh
JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Gradle Wrapper 与分发包固定并校验 SHA256。Miuix 固定 `0.9.5-beta03`，使用其真实 Android AAR，不从 rolling `main` 取代码。AGP9 内置 Kotlin，Compose 编译插件与 Kotlin版本保持一致。

构建产物：`app/build/outputs/apk/debug/app-debug.apk`。Debug APK 由本机 Android debug identity 签名，不是上游 release 签名。未经设备签名兼容确认，不要假设可以覆盖其他人的安装。

## 设置与宿主

- 打开设置 Activity 初始化完整偏好。新功能默认关闭，原有净化选项默认开启。
- 模块只注入 `com.ruanmei.ithome` 主进程，不注入推送和 channel 进程。
- 设置使用 LSPosed 官方 legacy 共享偏好扩展，要求 API93+。模块 UI 没有可用扩展时只保存本地私有设置，不能据此宣称宿主已读取。
- 宿主异步读取完整设置。缺字段、错类型或不可读均不进行净化和自动刷新；无手动 chmod、无导出设置 Provider、无自定义网络服务。
- 更改设置后返回或重新启动 IT之家。设置页仅显示本机保存/读取结果，不伪装为 LSPosed 激活探针。

## 真实设备验收矩阵

必须在 LSPosed 开启模块、仅选择 IT之家后验收真实安装 APK；单测和浏览器脚本模拟不等于实机通过。

- 广告/置顶/WebView推广保持默认行为，正常新闻复用后能恢复原高度。事件日历仅由官方设置控制，模块不再分类或折叠。
- 旧 schema1 完整配置迁移为 schema2：其他开关/分钟/writer 保留，revision 加一，移除 hide_event_calendar，新 hide_article_rewards=false；损坏或未知版本不得重置覆盖。
- 首页轮播开/关，检查无空白且正常新闻仍显示。
- 同一篇文章 AI 摘要开/关对照，正文/评论不受影响。精确 CSS 选择器仅用于文章模板。
- 文章评分只处理文章区域，不能误伤评论支持/反对或圈子评分。必须找确有评分的文章做 A/B。
- 同一篇文章打赏与红包区域开/关，检查完整父容器、红包子树及打赏人列表一起收起且无占位，关闭后恢复；正文和评论不变。未触发的红包样本不能宣称支付/弹出流程已测。
- 自动刷新关闭、1/5/10分钟持久化；真实1分钟到期调用当前可见栏目的原listener。
- 切换栏目重新计时，非当前页面不刷新。文章页、好物/圈子页、失去焦点与后台均暂停，回前台不补跑。
- 浅/深色、窄窗口、重开设置和宿主，无崩溃。
- 所有规则独立关闭与总开关关闭后的 pass-through，不能把旧隐藏状态遗留到正常卡片。

不得为了测试上传 APK 至公开站点、清数据、清日志、改防火墙、启动额外服务或永久改变设备显示设置。
