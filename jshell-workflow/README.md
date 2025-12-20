# JShell Workflow Example

このプロジェクトは、POJO-actorフレームワークのワークフロー機能を使用して、JShellによる計算を実行する実例プログラムです。

## 概要

このサンプルは、POJO-actorの核心的な概念を実際に動作するコードで示します：

1. **普通のPOJOをアクターとして扱う** - `JShellCalculator`は特別なアクタークラスではなく、通常のJavaクラスです
2. **ワークフローによる制御** - XMLで定義したワークフローに従って、アクターのメソッドが順次実行されます
3. **疎結合な設計** - ビジネスロジック（POJO）とアクターシステムは完全に分離されています

## プロジェクト構成

```
jshell-workflow/
├── pom.xml                                        # Mavenプロジェクト定義
├── README.md                                      # このファイル
└── src/
    └── main/
        ├── java/com/example/workflow/
        │   ├── JShellCalculator.java              # ビジネスロジックを持つPOJO
        │   ├── JShellCalculatorActor.java         # アクターラッパー
        │   └── JShellWorkflowMain.java            # メインプログラム
        └── resources/
            └── jshell-calculation.xml             # ワークフロー定義
```

## 前提条件

- Java 21以降
- Apache Maven 3.6以降
- POJO-actor 3.0.0がローカルMavenリポジトリにインストール済み

## POJO-actorのインストール

まず、親プロジェクトのPOJO-actorをローカルMavenリポジトリにインストールします：

```bash
# POJO-actorプロジェクトのルートディレクトリで実行
cd ../POJO-actor
mvn clean install
```

## ビルド方法

```bash
# このディレクトリ（jshell-workflow/）で実行
mvn clean package
```

ビルドが成功すると、`target/`ディレクトリに以下のファイルが生成されます：

- `jshell-workflow-1.0.0.jar` - 通常のJAR
- `jshell-workflow-1.0.0-fat.jar` - すべての依存関係を含む実行可能JAR

## 実行方法

### 方法1: Fat JARを使用（推奨）

```bash
java -jar target/jshell-workflow-1.0.0-fat.jar
```

### 方法2: Mavenから直接実行

```bash
mvn exec:java -Dexec.mainClass="com.example.workflow.JShellWorkflowMain"
```

## 実行結果の例

```
============================================================
[メイン] JShellワークフローの実行を開始します
============================================================

[ステップ1] IIActorSystemを初期化
[メイン] アクターシステム作成完了

[ステップ2] JShellCalculatorインスタンスを作成
[JShellCalculator] インスタンス作成 - JShell初期化完了

[ステップ3] JShellCalculatorをアクターとしてラップ
[JShellCalculatorActor] アクター作成: jshellCalc

[ステップ4] アクターをシステムに登録
[メイン] アクター 'jshellCalc' を登録完了

[ステップ5] ワークフローXMLをロード
[メイン] ワークフローロード完了 - 3 行のワークフロー

[ステップ6] Interpreterでワークフローを実行
------------------------------------------------------------
[JShellCalculatorActor.callByActionName()] アクター名: jshellCalc, アクション: eval, 引数: 10 + 5
[JShellCalculator.callByActionName()] アクション: eval, 引数: 10 + 5
[JShellCalculator.evaluate()] 式を評価: 10 + 5
[JShellCalculator.evaluate()] 結果: 15

[JShellCalculatorActor.callByActionName()] アクター名: jshellCalc, アクション: eval, 引数: 15 * 2
[JShellCalculator.callByActionName()] アクション: eval, 引数: 15 * 2
[JShellCalculator.evaluate()] 式を評価: 15 * 2
[JShellCalculator.evaluate()] 結果: 30

[JShellCalculatorActor.callByActionName()] アクター名: jshellCalc, アクション: getLastResult, 引数:
[JShellCalculator.callByActionName()] アクション: getLastResult, 引数:
[JShellCalculator.getLastResult()] lastResult = 30
------------------------------------------------------------

[ステップ7] 実行結果
============================================================
[成功] ワークフロー実行成功!
[結果] 30
============================================================

[詳細] ワークフロー実行の流れ:
  1. Row 1: 10 + 5 を評価 → 結果: 15
  2. Row 2: 前の結果(15) * 2 を評価 → 結果: 30
  3. Row 3: 最終結果を取得 → 結果: 30

[ステップ8] リソースをクリーンアップ
[JShellCalculator.close()] JShellをクローズ
[メイン] クリーンアップ完了

============================================================
[メイン] プログラム終了
============================================================
```

## ワークフローの説明

`src/main/resources/jshell-calculation.xml`で定義されているワークフローは、3つのステップから構成されています：

### Row 1: 初期計算
```xml
<row id="1" name="初期計算">
    <action actorName="jshellCalc" actionName="eval" args="10 + 5" />
</row>
```
- JShellで `10 + 5` を評価
- 結果: `15`

### Row 2: 結果を2倍
```xml
<row id="2" name="結果を2倍">
    <action actorName="jshellCalc" actionName="eval" args="$jshellCalc * 2" />
</row>
```
- 前のステップの結果（15）を使用して `15 * 2` を評価
- `$jshellCalc`は前の行で同じアクターから返された値を参照
- 結果: `30`

### Row 3: 最終結果取得
```xml
<row id="3" name="最終結果取得">
    <action actorName="jshellCalc" actionName="getLastResult" args="" />
</row>
```
- 最後の計算結果を取得
- 結果: `30`

## コードの構造

### 1. JShellCalculator.java（POJO）

ビジネスロジックを持つ普通のJavaクラスです。アクターに関する知識は一切ありません。

```java
public class JShellCalculator implements CallableByActionName {
    private final JShell jshell;

    public String evaluate(String expression) {
        // JShellで式を評価
    }

    @Override
    public ActionResult callByActionName(String actionName, String args) {
        // ワークフローから呼び出されるメソッド
    }
}
```

### 2. JShellCalculatorActor.java（アクターラッパー）

POJOをアクターとして扱えるようにするラッパークラスです。

```java
public class JShellCalculatorActor extends IIActorRef<JShellCalculator> {
    public JShellCalculatorActor(String actorName,
                                 JShellCalculator calculator,
                                 IIActorSystem system) {
        super(actorName, calculator, system);
    }

    @Override
    public ActionResult callByActionName(String actionName, String args) {
        // 内部のPOJOに処理を委譲
        return this.object.callByActionName(actionName, args);
    }
}
```

### 3. JShellWorkflowMain.java（メイン）

アクターシステムを初期化し、ワークフローを実行するメインプログラムです。

```java
public class JShellWorkflowMain {
    public static void main(String[] args) {
        // 1. アクターシステム初期化
        IIActorSystem actorSystem = new IIActorSystem();

        // 2. POJOを作成
        JShellCalculator calculator = new JShellCalculator();

        // 3. アクターでラップして登録
        JShellCalculatorActor calcActor =
            new JShellCalculatorActor("jshellCalc", calculator, actorSystem);
        actorSystem.put("jshellCalc", calcActor);

        // 4. ワークフローをロードして実行
        MatrixCode matrixCode = MatrixCode.fromXML(xmlStream);
        Interpreter interpreter = new Interpreter(actorSystem);
        ActionResult result = interpreter.execCode(matrixCode);
    }
}
```

## 学習ポイント

このサンプルから、以下のPOJO-actorの重要な概念を学ぶことができます：

1. **POJOファースト**
   `JShellCalculator`は、通常のJavaクラスです。アクターフレームワークへの依存は`CallableByActionName`インターフェースのみです。

2. **アクターラッパーパターン**
   `IIActorRef<T>`を継承することで、任意のPOJOをアクターとして扱えるようになります。

3. **ワークフロー駆動**
   ビジネスロジックの実行順序はXMLで宣言的に定義します。コードを変更せずにワークフローを変更できます。

4. **文字列ベースの動的呼び出し**
   `callByActionName(String, String)`により、XMLから動的にメソッドを呼び出せます。

5. **実行モード**
   POJO-actor v3.0.0では、デフォルトでPOOL実行モード（Work-Stealing Pool）を使用します。

## ワークフローのカスタマイズ

`src/main/resources/jshell-calculation.xml`を編集することで、異なる計算を実行できます：

```xml
<!-- 例: 平方根の計算 -->
<row id="1" name="平方根計算">
    <action actorName="jshellCalc" actionName="eval" args="Math.sqrt(16)" />
</row>
```

## トラブルシューティング

### POJO-actorが見つからない

```
[ERROR] Failed to execute goal ... could not resolve dependencies ... com.scivicslab:POJO-actor:jar:3.0.0
```

**解決方法**: POJO-actorを先にインストールしてください
```bash
cd ../POJO-actor
mvn clean install
```

### JShellが見つからない

JShellはJava 9以降に含まれています。Java 21を使用していることを確認してください：
```bash
java -version
```

## 関連ドキュメント

- [POJO-actor 公式リポジトリ](https://github.com/scivicslab/POJO-actor)
- [ワークフロー実行モデル詳細](../../doc_SCIVICS002/docs/POJO-actor/WORKFLOW_EXECUTION_MODEL.md)
- [ワークフロー内部動作解説](../../doc_SCIVICS002/docs/POJO-actor/WORKFLOW_INTERNALS_JSHELL_EXAMPLE.md)

## ライセンス

Apache License 2.0
