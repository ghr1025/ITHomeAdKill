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
- 唯一入口为 `XposedModule` / `onPackageReady`，元数据在 `META-INF/xposed/`；无 legacy `assets/xposed_init`、Manifest 模块声明或旧 API 82 依赖。
- 设置使用官方 API/service 102.0.0 的 RemotePreferences。UI 只在唯一 API102+ 且支持 Remote 的 service 连接时读写；无框架、多个可用框架或连接已更换时不保存到另一套本地后端。
- 宿主异步读取完整设置。缺字段、错类型或不可读均不进行净化和自动刷新；无手动 chmod、无导出设置 Provider、无自定义网络服务。
- 更改设置后返回或重新启动 IT之家。设置页只显示框架保存/读取结果，不伪装为宿主 Hook 激活探针。宿主只异步读取完整快照，listener 只是触发后台重读，不在 View Hook 中执行 Binder I/O。
- `RemotePreferences.Editor.commit()` 会先修改客户端内存再提交 Binder。失败后的相同内存快照不是持久化证明；同一 service 对象的 Store 确认状态必须跨页面保留。原方法异常必须通过 `Chain.proceed()` 原样传播，模块 observer 异常不得使宿主原调用丢失或重复。
- legacy → modern 的首次搬迁只接受应用私有 `files/legacy-settings-export.json` 的带类型旧配置导出；临时导出桥与宿主 APK/设备资料只保留在本地 `.hermes/`，不打进现代 APK。非空远端配置优先，不扫描框架目录，不读取过期 private 偏好覆盖。

## 真实设备验收矩阵

必须在 LSPosed 开启模块、仅选择 IT之家后验收真实安装 APK；单测和浏览器脚本模拟不等于实机通过。

- 广告/置顶/WebView推广保持默认行为，正常新闻复用后能恢复原高度。事件日历仅由官方设置控制，模块不再分类或折叠。
- 旧 schema1/2 完整配置迁移为 schema3：其他开关/分钟/writer 保留，revision 加一；schema1 额外移除 hide_event_calendar 并令 hide_article_rewards=false。缺键、坏类型和未来版本不得重置覆盖。
- 首页轮播开/关，检查无空白且正常新闻仍显示。
- 同一篇文章 AI 摘要开/关对照，正文/评论不受影响。精确 CSS 选择器仅用于文章模板。
- 文章评分只处理文章区域，不能误伤评论支持/反对或圈子评分。必须找确有评分的文章做 A/B。
- 同一篇文章打赏与红包区域开/关，检查完整父容器、红包子树及打赏人列表一起收起且无占位，关闭后恢复；正文和评论不变。未触发的红包样本不能宣称支付/弹出流程已测。
- 评论连接线已退役：设置页、宿主接入与绘制实现均不得保留。旧 schema3 中的 `comment_thread_lines` 忽略且不再写出，不重置其他合法字段；缺失或坏类型的保留字段仍拒绝读取。
- 自动刷新关闭、1/5/10分钟持久化；真实1分钟到期调用当前可见栏目的原listener。
- 切换栏目重新计时，非当前页面不刷新。文章页、好物/圈子页、失去焦点与后台均暂停，回前台不补跑。
- 浅/深色、窄窗口、重开设置和宿主，无崩溃。
- 所有规则独立关闭与总开关关闭后的 pass-through，不能把旧隐藏状态遗留到正常卡片。

不得为了测试上传 APK 至公开站点、清数据、清日志、改防火墙、启动额外服务或永久改变设备显示设置。
