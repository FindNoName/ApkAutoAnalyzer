# MalDroid Analyzer

Android APK 静态分析工具，用于恶意软件检测研究。通过提取调用图指标、权限特征和 GLCM 纹理特征，生成可用于机器学习训练的特征数据集。

## 功能模块

| 模块 | 说明 |
|------|------|
| 调用图分析 | 基于 FlowDroid/Soot 构建方法调用图，计算内聚度与耦合度 |
| 权限分析 | 从 APK Manifest 提取权限声明，统计权限数量、分组及危险权限比例 |
| GLCM 特征提取 | 将 DEX 字节码可视化为灰度图像，提取对比度、相关性等纹理特征 |
| Excel 报告 | 将所有特征写入 Excel 文件，支持断点续分析（跳过已分析的 APK） |
| 调用图导出 | 以 GEXF 格式导出调用图，可用 Gephi 等工具可视化 |

## 环境要求

- Java 8+
- Maven 3.6+
- Android SDK（需要 `platforms/` 目录）

## 构建

```bash
mvn clean package -DskipTests
```

## 使用方法

```bash
java -jar target/maldroid-analyzer-1.0-SNAPSHOT.jar \
  <apkFolder> \
  <excelOutput> \
  <androidPlatforms> \
  <gexfOutputDir>
```

### 参数说明

| 参数 | 说明 | 默认值 |
|------|------|--------|
| `apkFolder` | 待分析的 APK 文件夹路径 | `apks/` |
| `excelOutput` | 输出 Excel 文件路径 | `output/results.xlsx` |
| `androidPlatforms` | Android SDK platforms 目录 | `$ANDROID_HOME/platforms` |
| `gexfOutputDir` | 调用图 GEXF 输出目录 | `output/callgraphs/` |

### 示例

```bash
# 使用默认值（需设置 ANDROID_HOME 环境变量）
java -jar target/maldroid-analyzer-1.0-SNAPSHOT.jar apks/

# 指定所有参数
java -jar target/maldroid-analyzer-1.0-SNAPSHOT.jar \
  /data/apks \
  /data/results.xlsx \
  /opt/android-sdk/platforms \
  /data/callgraphs
```

## 输出格式

Excel 文件每行对应一个 APK，包含以下列：

- APK 名称
- 内聚度（Cohesion）
- 直接耦合度（Direct Coupling）
- 间接耦合度（Indirect Coupling）
- 权限总数、权限分组数、危险权限比例
- GLCM 特征：对比度、相异性、同质性、能量、相关性、ASM

## 依赖

- [FlowDroid](https://github.com/secure-software-engineering/FlowDroid) 2.12.0 — Android 污点分析
- [Apache POI](https://poi.apache.org/) 5.0.0 — Excel 读写
- [apk-parser](https://github.com/hsiafan/apk-parser) 2.6.10 — APK Manifest 解析
- [gexf4j](https://github.com/francesco-ficarola/gexf4j) 1.0.0 — 调用图导出

## 项目结构

```
analyzer-core/
├── src/main/java/org/maldroid/
│   ├── MalDroidAnalyzer.java       # 主入口，批量处理 APK
│   ├── CallGraphAnalyzer.java      # 调用图分析
│   ├── PermissionAnalyzer.java     # 权限分析
│   ├── GlcmFeatureExtractor.java   # GLCM 特征提取
│   ├── ExcelReportWriter.java      # Excel 报告生成
│   └── CGExporter.java             # 调用图 GEXF 导出
├── src/main/resources/
│   └── AndroidCallbacks.txt        # Android 回调方法列表
└── pom.xml
```
