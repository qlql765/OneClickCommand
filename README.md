# 一键指令 (OneClickCommand)

将多个 App 按顺序组合为一条「指令」，保存后生成桌面快捷方式。点击快捷方式即可按序自动启动所有 App，执行完毕后进程自动从后台消失。

## 功能特性

- ✅ 新建/编辑/删除指令，每条指令包含名称、主题色、按序排列的 App 列表
- ✅ 从已安装应用中选择要加入指令的 App，支持调整顺序与删除
- ✅ 为指令生成桌面快捷方式（图标为主题色圆形 + 闪电标识）
- ✅ 点击桌面快捷方式后按序启动所有 App，每个 App 间可配置延迟（默认 600ms）
- ✅ 执行完毕后服务自动停止，不留残留后台进程
- ✅ Material 3 设计，支持动态取色（Android 12+）

## 技术栈

| 项目 | 版本 |
|------|------|
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.10.01) |
| 主题 | Material 3 |
| 数据库 | Room 2.6.1 (KSP) |
| AGP | 8.10.1 |
| Gradle | 8.14.3 |
| compileSdk / targetSdk | 36 |
| minSdk | 26 (Android 8.0) |
| JDK | 17 |

## 项目结构

```
app/src/main/java/com/qile/oneclickcommand/
├── data/
│   ├── AppDatabase.kt              # Room 数据库入口
│   ├── entity/
│   │   ├── CommandEntity.kt        # 指令表（id, name, iconColor, createdAt, updatedAt）
│   │   └── ActionEntity.kt         # 动作表（id, commandId, orderIndex, packageName, appName, delayMs）
│   └── dao/
│       ├── CommandDao.kt           # 指令增删改查
│       └── ActionDao.kt            # 动作增删改查
├── executor/
│   └── CommandExecuteService.kt    # 前台服务：按序启动各 App，执行完自动 stopSelf
├── ui/
│   ├── MainActivity.kt             # 主界面：指令列表、新建、执行、删除、添加快捷方式
│   ├── MainViewModel.kt            # 主界面 ViewModel
│   ├── EditCommandActivity.kt      # 编辑/新建指令：名称、颜色、应用列表
│   ├── ExecuteActivity.kt          # 透明 Activity：接收快捷方式 Intent 并启动服务
│   └── theme/Theme.kt              # Material 3 主题
└── util/
    └── ShortcutHelper.kt           # 桌面快捷方式创建/移除
```

## 核心流程

```
点击桌面快捷方式
       │
       ▼
ExecuteActivity（透明、不进最近任务）
       │  读取 intent extra: executeCommandId
       ▼
CommandExecuteService.start(commandId)
       │  前台服务（specialUse 类型）
       │  按 orderIndex 顺序 startActivity 每个 App
       │  每步间隔 action.delayMs（默认 600ms）
       │  异常静默跳过
       ▼
执行完毕 → stopSelf() → 进程自动消失
```

## 构建

### 环境要求

- JDK 17（推荐 JetBrains Runtime 17/21）
- Android SDK（含 platform android-36）

### 构建命令

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease
```

Debug 包输出：`app/build/outputs/apk/debug/OneClickCommand-debug-1.0.0.apk`

Debug 包 applicationId 为 `com.qile.oneclickcommand.debug`（带 `.debug` 后缀）。

### 安装到设备

```bash
adb install -r app/build/outputs/apk/debug/OneClickCommand-debug-1.0.0.apk
# 启动
adb shell am start -n com.qile.oneclickcommand.debug/com.qile.oneclickcommand.ui.MainActivity
```

## 权限说明

| 权限 | 用途 |
|------|------|
| `QUERY_ALL_PACKAGES` | 读取已安装应用列表供选择 |
| `INSTALL_SHORTCUT` / `CREATE_SHORTCUT` | 创建桌面快捷方式 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | 指令执行前台服务 |
| `POST_NOTIFICATIONS` | Android 13+ 前台服务通知 |

## 使用说明

1. 打开 App，点击右下角「+ 新建指令」
2. 输入指令名称，选择主题色
3. 点击「+ 添加」选择要按序启动的 App，可调整顺序和延迟
4. 点击右上角「✓ 保存」
5. 在指令列表点击「⋯」→「添加到桌面」创建快捷方式
6. 点击桌面快捷方式即可一键启动所有 App

## License

MIT
