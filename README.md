# MalDroid Analyzer

MalDroid Analyzer is a static analysis tool for Android APK malware research. It traverses APK files in a directory tree, extracts multi-view features, and writes the results to an Excel dataset for downstream analysis or model training.

## Modules

| Module | Description |
|------|------|
| Call Graph Analysis | Builds a method call graph with FlowDroid/Soot and extracts cohesion, coupling, SCC, sensitive API, dynamic loading, and component-entry features |
| Permission Analysis | Extracts declared permissions and computes total, dangerous, and high-risk permission features |
| Manifest Analysis | Extracts Android component counts and manifest security attributes such as `debuggable`, `allowBackup`, and cleartext traffic |
| String Feature Extraction | Counts URLs, IPs, suspicious keywords, and high-entropy strings |
| DEX / GLCM Feature Extraction | Converts DEX byte streams into grayscale-image-derived texture statistics and entropy-related features |
| Excel Report | Writes features to an `.xlsx` file, supports resume, deduplicates rows, and skips APKs that were already analyzed |
| Sample Label Inference | Infers labels from parent folder names: `good -> 0`, `bad -> 1` |

## Requirements

- Java 8+
- Maven 3.6+
- Android SDK with a valid `platforms/` directory

## Build

```bash
mvn clean package -DskipTests
```

After packaging, the runnable shaded jar is:

```bash
target/maldroid-analyzer-1.0-SNAPSHOT.jar
```

## Usage

The current code does not read command-line arguments. Run it directly:

```bash
java -jar target/maldroid-analyzer-1.0-SNAPSHOT.jar
```

### Runtime Defaults

The startup parameters are currently fixed in `src/main/java/org/maldroid/MalDroidAnalyzer.java`:

| Setting | Current value |
|------|------|
| APK root directory | `D:/DaChuang/data_set/Androzoo` |
| Excel output | `output/results.xlsx` |
| Android platforms | `$ANDROID_HOME/platforms` when `ANDROID_HOME` is set, otherwise empty string |

Notes:

- The program recursively traverses subdirectories under the configured APK root.
- If the APK root directory does not exist, the program prints `Folder not found` and exits.
- If you need custom input or output paths, modify the constants in `MalDroidAnalyzer.java`.

## Current Startup Behavior

The main process performs the following steps for each APK:

1. Skip APKs that already exist in the Excel report by filename.
2. Infer the sample label from parent folder names (`good` or `bad`).
3. Run call-graph analysis.
4. Run permission analysis.
5. Run manifest analysis.
6. Run string feature extraction.
7. Run DEX / GLCM feature extraction.
8. Append one row to the Excel report.

## Output

Each Excel row corresponds to one APK and currently includes these feature groups:

- Identity: APK path, malicious label
- Call graph: cohesion, direct coupling, indirect coupling, method counts, edge count, SCC metrics, component-entry count
- Sensitive behavior: sensitive API type/count, reflection, dynamic load, native load, `Runtime.exec` count
- Permissions: total permissions, dangerous-permission ratio, high-risk permission count, category flags
- Manifest: activity/service/receiver/provider counts, debuggable, allowBackup, cleartext traffic, minSdk, targetSdk, custom permission count
- Strings: URL count, IP count, suspicious keyword count, high-entropy string count
- DEX / image statistics: DEX count, total size, GLCM mean/std features, byte entropy, gray statistics, edge density

The exact Excel headers are defined in `src/main/java/org/maldroid/ExcelReportWriter.java`.

## Notes On GEXF Export

The repository still contains `src/main/java/org/maldroid/CGExporter.java`, but the current `MalDroidAnalyzer` startup flow does not invoke it. A normal run does not emit GEXF call-graph files.

## Dependencies

- [FlowDroid](https://github.com/secure-software-engineering/FlowDroid) 2.12.0
- [Apache POI](https://poi.apache.org/) 5.0.0
- [apk-parser](https://github.com/hsiafan/apk-parser) 2.6.10
- [gexf4j](https://github.com/francesco-ficarola/gexf4j) 1.0.0

## Project Structure

```text
analyzer-core/
├── src/main/java/org/maldroid/
│   ├── MalDroidAnalyzer.java
│   ├── CallGraphAnalyzer.java
│   ├── PermissionAnalyzer.java
│   ├── ManifestAnalyzer.java
│   ├── StringFeatureExtractor.java
│   ├── GlcmFeatureExtractor.java
│   ├── ExcelReportWriter.java
│   ├── SampleLabelResolver.java
│   └── CGExporter.java
├── src/main/resources/
│   └── AndroidCallbacks.txt
├── pom.xml
└── README.md
```

---

# MalDroid Analyzer

MalDroid Analyzer 是一个面向 Android APK 恶意软件研究的静态分析工具。程序会递归遍历目录中的 APK，提取多视角特征，并将结果写入 Excel 数据集，便于后续统计分析或机器学习训练。

## 功能模块

| 模块 | 说明 |
|------|------|
| 调用图分析 | 基于 FlowDroid/Soot 构建方法调用图，提取内聚度、耦合度、SCC、敏感 API、动态加载、组件入口等特征 |
| 权限分析 | 提取权限声明，统计总权限数、危险权限数、高风险权限数及相关标记特征 |
| Manifest 分析 | 提取 Activity / Service / Receiver / Provider 数量以及 `debuggable`、`allowBackup`、明文流量等安全属性 |
| 字符串特征提取 | 统计 URL、IP、可疑关键词和高熵字符串数量 |
| DEX / GLCM 特征提取 | 将 DEX 字节流转换为灰度纹理统计，提取 GLCM、熵、灰度和边缘密度相关特征 |
| Excel 报告 | 将分析结果写入 `.xlsx` 文件，支持断点续跑、按文件名去重、跳过已分析 APK |
| 样本标签推断 | 根据父目录名推断标签：`good -> 0`，`bad -> 1` |

## 环境要求

- Java 8+
- Maven 3.6+
- Android SDK，且需要可用的 `platforms/` 目录

## 构建

```bash
mvn clean package -DskipTests
```

打包完成后，可执行的 fat jar 为：

```bash
target/maldroid-analyzer-1.0-SNAPSHOT.jar
```

## 启动方式

当前代码不会读取命令行参数，直接运行即可：

```bash
java -jar target/maldroid-analyzer-1.0-SNAPSHOT.jar
```

### 当前默认配置

启动参数目前写死在 `src/main/java/org/maldroid/MalDroidAnalyzer.java` 中：

| 配置项 | 当前值 |
|------|------|
| APK 根目录 | `D:/DaChuang/data_set/Androzoo` |
| Excel 输出路径 | `output/results.xlsx` |
| Android platforms 路径 | 若设置了 `ANDROID_HOME`，则为 `$ANDROID_HOME/platforms`；否则为空字符串 |

说明：

- 程序会递归遍历 APK 根目录下的所有子目录。
- 如果 APK 根目录不存在，程序会输出 `Folder not found` 后结束。
- 如果需要自定义输入输出路径，需要修改 `MalDroidAnalyzer.java` 中的常量。

## 当前执行流程

程序对每个 APK 依次执行：

1. 按文件名跳过已经写入 Excel 的 APK。
2. 根据父目录名 `good` / `bad` 推断样本标签。
3. 执行调用图分析。
4. 执行权限分析。
5. 执行 Manifest 分析。
6. 执行字符串特征提取。
7. 执行 DEX / GLCM 特征提取。
8. 将结果追加到 Excel。

## 输出内容

当前每个 APK 的 Excel 行主要包含以下几类字段：

- 基本信息：APK 路径、恶意标签
- 调用图特征：内聚度、直接/间接耦合、方法数、边数、SCC 指标、组件入口数
- 敏感行为特征：敏感 API 类型/次数、反射、动态加载、native 加载、`Runtime.exec` 次数
- 权限特征：总权限数、危险权限比例、高风险权限数、权限类别标记
- Manifest 特征：四大组件数量、`debuggable`、`allowBackup`、明文流量、`minSdk`、`targetSdk`、自定义权限数
- 字符串特征：URL 数、IP 数、可疑关键词数、高熵字符串数
- DEX / 图像统计特征：DEX 数量、总大小、GLCM 均值/标准差、字节熵、灰度统计、边缘密度

精确列名定义见 `src/main/java/org/maldroid/ExcelReportWriter.java`。

## 关于 GEXF 导出

仓库中仍保留 `src/main/java/org/maldroid/CGExporter.java`，但当前 `MalDroidAnalyzer` 的主流程并不会调用它，因此默认运行不会生成 GEXF 调用图文件。

## 依赖

- [FlowDroid](https://github.com/secure-software-engineering/FlowDroid) 2.12.0
- [Apache POI](https://poi.apache.org/) 5.0.0
- [apk-parser](https://github.com/hsiafan/apk-parser) 2.6.10
- [gexf4j](https://github.com/francesco-ficarola/gexf4j) 1.0.0

## 项目结构

```text
analyzer-core/
├── src/main/java/org/maldroid/
│   ├── MalDroidAnalyzer.java
│   ├── CallGraphAnalyzer.java
│   ├── PermissionAnalyzer.java
│   ├── ManifestAnalyzer.java
│   ├── StringFeatureExtractor.java
│   ├── GlcmFeatureExtractor.java
│   ├── ExcelReportWriter.java
│   ├── SampleLabelResolver.java
│   └── CGExporter.java
├── src/main/resources/
│   └── AndroidCallbacks.txt
├── pom.xml
└── README.md
```
