# pankti-gen

This tool generates tests for instrumented methods (currently from PDFBox)

### Build, run

1. Clone PDFBox: `git@github.com:apache/pdfbox.git`
2. Replace CSV file path with path to [this](https://github.com/Deee92/journal/blob/master/data/pdfbox-pure-methods/one-method.csv) file
3. Replace XML file paths with paths to `17-pdfbox-*.xml` files listed [here](https://github.com/Deee92/journal/tree/master/data/pdfbox-pure-methods)
4. Build pankti-gen: `mvn clean install`
5. Run: `java -jar target/pankti-gen-1.0-SNAPSHOT-jar-with-dependencies.jar /path/to/pdfbox/`
