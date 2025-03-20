# Directory
SRC_DIR = src/cross
OUT_DIR = out
LIB_DIR = lib
LIBS = $(LIB_DIR)/gson-2.11.0.jar

# File principali
CLIENT_MAIN = cross.client.ClientMain
SERVER_MAIN = cross.server.ServerMain

# JAR output
CLIENT_JAR = ClientMain.jar
SERVER_JAR = ServerMain.jar

# Trova tutti i file Java nel progetto
SOURCES = $(shell find $(SRC_DIR) -name "*.java")
CLASSES = $(SOURCES:$(SRC_DIR)/%.java=$(OUT_DIR)/%.class)

# Regola predefinita
all: clean compile client server

# Compilazione dei file .java
compile:
	@mkdir -p $(OUT_DIR)
	@echo "Compilando i file Java..."
	javac -d $(OUT_DIR) -cp "$(LIBS)" $(SOURCES)

# Creazione del JAR del Client (Fat JAR)
client: compile
	@echo "Creando il JAR del Client..."
	@mkdir -p tmp_client
	@cp -r $(OUT_DIR)/* tmp_client
	@cd tmp_client && jar xf ../$(LIBS)
	@cd tmp_client && jar cvfe ../$(CLIENT_JAR) $(CLIENT_MAIN) .
	@rm -rf tmp_client
	@echo "Client JAR creato: $(CLIENT_JAR)"

# Creazione del JAR del Server (Fat JAR)
server: compile
	@echo "Creando il JAR del Server..."
	@mkdir -p tmp_server
	@cp -r $(OUT_DIR)/* tmp_server
	@cd tmp_server && jar xf ../$(LIBS)
	@cd tmp_server && jar cvfe ../$(SERVER_JAR) $(SERVER_MAIN) .
	@rm -rf tmp_server
	@echo "Server JAR creato: $(SERVER_JAR)"

# Pulizia dei file compilati
clean:
	@echo "Pulizia dei file compilati..."
	@rm -rf $(OUT_DIR) $(CLIENT_JAR) $(SERVER_JAR) tmp_client tmp_server
	@echo "Pulizia completata."