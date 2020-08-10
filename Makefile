#LOGISIM_JAR_FILE = c:\logisim\logisim-evolution-3.3.1.jar
#LOGISIM_JAR_FILE = c:\logisim\logisim-evolution-4.0.4hc.jar
LOGISIM_JAR_FILE = ..\logisim-generic-2.7.1.jar
MANIFEST_FILE = MANIFEST.MF
JAR_FILE = midikeyboard.jar
BIN_DIR = .\bin
SRC = src\Keyboard.java src\MyKeyboardLib.java

jar: classes
	jar cmf $(MANIFEST_FILE) $(JAR_FILE) -C $(BIN_DIR) . src README.md Makefile Makefile.unix MANIFEST.MF

classes:
	javac -nowarn -d $(BIN_DIR) -classpath $(LOGISIM_JAR_FILE) $(SRC)
