# EulCauInk

EulCauInk 是一款以 **Markdown（`.md`）** 为核心的本地笔记应用，强调 **结构化写作、轻量编辑与离线可用**。  
应用基于 Web 技术构建，并通过 Android WebView 封装为原生应用。

## 功能特性

- 本地 Markdown 笔记管理
- 即开即写，所见即所得的 Markdown 编辑体验
- 支持笔记拖拽排序
- 支持删除笔记
- 支持在 Markdown 中绘图与插入图片
- 支持多种超链接类型
- Android 原生应用封装，离线可用

## App 使用方法

### 1️. 创建与编辑笔记

- 打开应用后，可以 **新建 Markdown (`.md`) 文件**
- 点击笔记即可进入编辑界面
- 所有笔记均为本地文件，不依赖云端

### 2️. 笔记管理

- **拖拽排序**  
  长按笔记即可调整顺序
- **删除笔记**  
  可直接删除不再需要的 `.md` 文件

### 3️. Markdown 内容能力

在 Markdown 文档中，你可以：

- 正常书写 Markdown 语法
- 上传 / 下载 Markdown 文件
- 绘图 / 插入图片
- 插入超链接

#### 支持的链接类型

| 链接类型 | 是否支持 | 说明 |
| ------ | ------ | ------ |
| `http://` / `https://` | ✅ | 外部网页链接 |
| `#标题` | ✅ | 当前文档内部标题跳转 |
| `xxx.md` | ✅ | 跳转到其他 Markdown 文档 |
| `mailto:` | ✅ | 邮件链接 |
| `tel:` | ✅ | 电话链接 |
| `file://` | ❌ | 出于安全限制不支持 |

> **注意**：  
> 出于 Android WebView 的安全模型限制，`file://` 协议无法使用，请使用上述支持的链接形式。

## 技术架构概览

- **前端**：Vite + React + Tailwind CSS
- **Markdown 渲染**：`react-markdown` + `remark` / `rehype`
- **平台**：Android WebView
- **构建方式**：Vite build + 本地静态资源加载

## Web 源代码

Web 前端完整源码位于：

**GitHub 仓库** [EulCau/EulCauInk](https://github.com/EulCau/EulCauInk)

可以从其 [release](https://github.com/EulCau/EulcauInk/releases) 中下载构建好的静态资源

该仓库包含：

- 前端源码
- Vite 构建配置
- Markdown 编辑与渲染逻辑
- 静态网页构建方法与封装说明

## 已知限制与注意事项

- 本应用 **不适合作为在线网页部署**
- WebView 环境下：
  - CDN 资源需确保可访问
  - `file://` 链接受限
- release 模式下 WebView 行为比 debug 更严格，需注意资源路径与权限

## License

待定

## 致谢

- Markdown / React / Tailwind 社区
- chatGPT 与 aistudio 辅助开发
- Android WebView 开源生态

如果你在使用过程中发现问题或有改进建议，欢迎交流与反馈。
