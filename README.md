# 中国古典诗词 (PoetryReader)

一款纯本地离线的安卓古典诗词阅读 App。基于开源项目 [chinese-poetry](https://github.com/liangleionline/chinese-poetry) 精选经典文集构建，原生 Kotlin + Jetpack Compose + Room。

## 数据

从 chinese-poetry 精选经典篇目预构建为带索引 + FTS5 的 SQLite 数据库（`poetry.db`，约 1.7 万条）：

| 分类 | 内容 |
|---|---|
| 唐诗 | 唐诗三百首、唐诗补录、水墨唐诗 |
| 宋词 | 宋词三百首、花间集、南唐词、纳兰性德 |
| 先秦 | 诗经、楚辞、论语、大学、中庸、孟子 |
| 蒙学 | 三字经、百家姓、千字文、弟子规、声律启蒙、增广贤文、朱子家训、千家诗、幼学琼林、文字蒙求、古文观止 |
| 其他 | 元曲、曹操诗集、幽梦影 |

## 功能

- 书架按文集分类浏览，上拉分页加载
- 全文检索（标题 / 正文 / 作者，LIKE 子串匹配）
- 沉浸式阅读页：字号、行距、浅色/深色切换、屏幕常亮
- 收藏、书签、自动记录阅读进度
- 数据完全本地离线，无需任何网络权限

## 技术栈

- Kotlin 1.9 + Jetpack Compose + Material3
- Room（用户数据：收藏/书签/进度，独立于诗词只读库）
- 诗词库 `poetry.db`：首次启动从 assets 拷贝到私有目录，只读使用

## 构建

```bash
# 需要 JDK 17 + Android SDK 34
./gradlew assembleRelease
# APK 输出：app/build/outputs/apk/release/app-release.apk
```

## 数据预处理

`tools/preprocess.py` 读取 chinese-poetry 精选 JSON，清洗去重并生成 `poetry.db`。

## 协议

- App 代码：MIT License
- 数据源：chinese-poetry 项目（MIT）
