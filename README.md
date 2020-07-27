# pankti-gen

This tool generates tests for instrumented methods (currently from PDFBox)

### Build, run

1. Clone PDFBox: `git@github.com:apache/pdfbox.git`
2. Replace CSV file path with path to [this](https://github.com/Deee92/journal/blob/master/data/pdfbox-pure-methods/one-method.csv) file
3. Replace XML file paths with paths to `org.apache.fontbox.cmap.CodespaceRange.isFullMatch-*.xml` files listed [here](https://github.com/Deee92/journal/tree/master/data/pdfbox-pure-methods)
4. Update `outputDirectory` in `PanktiGenMain`
5. Build pankti-gen: `mvn clean install`
6. Run: `java -jar target/pankti-gen-1.0-SNAPSHOT-jar-with-dependencies.jar /path/to/pdfbox/`
